package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

/** 单任务退回：新分支先创建，当前成员再退出；不移动会签根，不改兄弟变量。 */
@Service
public class BpmTaskReturnService {
    public static final String RELEASED_PREFIX = "bpm_dispatch_return_released_";
    public static final String RETAINED_SOURCES = "bpm_dispatch_return_sources";
    // 兼容既有恢复任务：该键仅用于定位保留人员，不再限制发送流向。
    public static final String SCOPE = "bpm_dispatch_return_scope";
    public static final String RETAINED_ROOTS = "bpm_dispatch_return_retained_roots";
    public static final String RETAINED_PREFIX = "bpm_dispatch_return_retained_";
    public static final String TARGET = "bpm_dispatch_return_target";
    public static final String RETURNED = "bpm_dispatch_returned_count";
    private static final String PATH = "RETURNABLE_TASK_PATH";
    private static final String BATCH = "RETURN_BRANCH_CONTEXT";
    private static final String REGISTER_SOURCE = "DIRECT_REGISTER_SOURCE";
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;
    @Resource private RepositoryService repositoryService;
    @Resource private ManagementService managementService;
    @Resource private TaskService flowableTaskService;
    @Resource private AdminUserApi adminUserApi;

    private Task current(Long userId, String taskId) {
        Task task = flowableTaskService.createTaskQuery().taskId(taskId).active().singleResult();
        if (task == null) throw exception(TASK_NOT_EXISTS);
        if (userId != null && !userId.toString().equals(task.getAssignee()))
            throw exception(TASK_RETURN_INVALID, "只能退回本人正在办理的任务");
        if (task.getParentTaskId() != null || task.getDelegationState() == org.flowable.task.api.DelegationState.PENDING)
            throw exception(TASK_RETURN_INVALID, "请先完成委派或加签处理");
        return task;
    }

    /** 只按流程实例启动时的固定版本分流；部署前无标记的实例始终使用旧版。 */
    public String mode(Long userId, String taskId) {
        return managementService.executeCommand(command -> mode(current(userId, taskId)));
    }

    private String mode(Task task) {
        return BpmProcessOperationVersion.isCurrent(runtimeService.getVariableLocal(task.getProcessInstanceId(), BpmProcessOperationVersion.KEY))
                ? "SINGLE" : "LEGACY";
    }

    private void requireLegacyReady(Task task) {
        if (managementService.createJobQuery().processInstanceId(task.getProcessInstanceId()).count() > 0
                || managementService.createDeadLetterJobQuery().processInstanceId(task.getProcessInstanceId()).count() > 0)
            throw exception(TASK_RETURN_INVALID, "流程还有未完成的异步任务，请处理后重试整体退回");
    }

    private List<BpmTaskReturnTargetRespVO> legacyTargets(Task task) {
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowElement current = model.getFlowElement(task.getTaskDefinitionKey());
        Set<String> finished = historyService.createHistoricTaskInstanceQuery().processInstanceId(task.getProcessInstanceId())
                .finished().includeTaskLocalVariables().list().stream()
                .filter(h -> !BpmTaskStatusEnum.CANCEL.getStatus().equals(h.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS)))
                .map(HistoricTaskInstance::getTaskDefinitionKey).collect(Collectors.toSet());
        Map<String, BpmTaskReturnTargetRespVO> result = new LinkedHashMap<>();
        for (UserTask node : BpmnModelUtils.getPreviousUserTaskList(current, null, null)) {
            if (!finished.contains(node.getId()) || !BpmnModelUtils.isSequentialReachable(current, node, null)) continue;
            BpmTaskReturnTargetRespVO row = new BpmTaskReturnTargetRespVO();
            row.setTaskDefinitionKey(node.getId()); row.setName(node.getName()); row.setReturnMode("LEGACY");
            row.setAssignees(Collections.emptyList()); result.put(node.getId(), row);
        }
        return new ArrayList<>(result.values());
    }

    /** 与旧撤回相同：保护命令先退出，原变更命令的 agenda 才能执行完再查询新任务。 */
    @Transactional(rollbackFor = Exception.class)
    public void executeLegacy(Long userId, BpmTaskReturnReqVO request, Runnable operation) {
        managementService.executeCommand(command -> {
            Task task = current(userId, request.getId());
            if (!"LEGACY".equals(mode(task)) || request.getExpectedMode() != null && !"LEGACY".equals(request.getExpectedMode()))
                throw exception(TASK_RETURN_INVALID, "退回方式已变化，请刷新后重新确认");
            requireLegacyReady(task);
            if (request.getReturnAssignees() != null && !request.getReturnAssignees().isEmpty())
                throw exception(TASK_RETURN_INVALID, "旧任务仅支持整体退回，不能指定单人");
            if (legacyTargets(task).stream().noneMatch(target -> target.getTaskDefinitionKey().equals(request.getTargetTaskDefinitionKey())))
                throw exception(TASK_RETURN_INVALID, "目标不在当前可退回的历史节点中");
            ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
            if (process == null || process.isSuspended() || process.isEnded()) throw exception(TASK_RETURN_INVALID, "流程已结束或挂起");
            process.forceUpdate();
            return null;
        });
        operation.run();
    }

    /** 不读取流程根上的混合路径；追加分支缺少路径时，从精确派发来源恢复上下文。 */
    private Map<String, Object> context(Task task) {
        Map<String, Object> values = new HashMap<>();
        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
        boolean inspectedDispatchSource = false;
        while (execution != null && !execution.isProcessInstanceType()) {
            for (String key : Arrays.asList(PATH, BATCH)) {
                if (!values.containsKey(key) && execution.hasVariableLocal(key)) values.put(key, execution.getVariableLocal(key));
            }
            Object origins = execution.getVariableLocal(BpmDispatchSources.SOURCES);
            Object ids = origins instanceof Map ? ((Map<?, ?>) origins).get(task.getAssignee()) : null;
            boolean directSource = !inspectedDispatchSource && ids instanceof Collection && !((Collection<?>) ids).isEmpty();
            if (ids instanceof Collection && !((Collection<?>) ids).isEmpty()) inspectedDispatchSource = true;
            if (ids instanceof Collection && ((Collection<?>) ids).size() == 1) {
                HistoricTaskInstance source = historyService.createHistoricTaskInstanceQuery()
                        .taskId(((Collection<?>) ids).iterator().next().toString()).includeTaskLocalVariables().singleResult();
                if (source != null) for (String key : Arrays.asList(PATH, BATCH)) {
                    if (!values.containsKey(key) && source.getTaskLocalVariables().containsKey(key))
                        values.put(key, source.getTaskLocalVariables().get(key));
                }
                // 登记专用提交不经过普通 approveTask 的路径载体，仅从本任务可核验的直接来源恢复。
                // 不按人员、时间或全流程历史猜测登记轮次；最近的派发来源不符时也不沿祖先扩大范围。
                if (directSource && source != null && source.getEndTime() != null && source.getAssignee() != null
                        && task.getProcessInstanceId().equals(source.getProcessInstanceId())
                        && BpmTaskStatusEnum.APPROVE.getStatus().equals(source.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS))
                        && flowableTaskService.getVariableLocal(task.getId(), BpmInternalDispatch.SOURCE) == null
                        && BpmRegisterTaskUtils.isRegisterTask(repositoryService.getBpmnModel(task.getProcessDefinitionId())
                                .getFlowElement(source.getTaskDefinitionKey()))) {
                    if (path(values).isEmpty()) values.put(PATH, Collections.singletonList(source.getTaskDefinitionKey()));
                    values.put(REGISTER_SOURCE, Collections.singletonMap(source.getTaskDefinitionKey(), source.getId()));
                }
            }
            execution = execution.getParent();
        }
        return values;
    }

    private List<String> path(Map<String, Object> context) {
        Object path = context.get(PATH);
        if (!(path instanceof Collection)) return new ArrayList<>();
        return ((Collection<?>) path).stream().map(Object::toString).collect(Collectors.toList());
    }

    /** 只沿 taskId 指针追溯同环节的真实前手，绝不按办理人或时间拼接。 */
    private List<HistoricTaskInstance> internalAncestors(Task task) {
        List<HistoricTaskInstance> result = new ArrayList<>();
        Object previous = flowableTaskService.getVariableLocal(task.getId(), BpmInternalDispatch.SOURCE);
        Set<String> visited = new HashSet<>(); visited.add(task.getId());
        while (previous != null && visited.add(previous.toString())) {
            HistoricTaskInstance history = historyService.createHistoricTaskInstanceQuery().taskId(previous.toString()).includeTaskLocalVariables().singleResult();
            if (history == null || history.getEndTime() == null || history.getAssignee() == null
                    || !task.getProcessInstanceId().equals(history.getProcessInstanceId())
                    || !task.getTaskDefinitionKey().equals(history.getTaskDefinitionKey())
                    || !BpmTaskStatusEnum.APPROVE.getStatus().equals(history.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS))) break;
            result.add(history); previous = history.getTaskLocalVariables().get(BpmInternalDispatch.SOURCE);
        }
        return result;
    }

    private Map<String, List<HistoricTaskInstance>> histories(Task task) {
        Map<String, Object> context = context(task);
        List<String> path = path(context);
        Map<?, ?> batches = context.get(BATCH) instanceof Map ? (Map<?, ?>) context.get(BATCH) : Collections.emptyMap();
        Map<?, ?> registerSources = context.get(REGISTER_SOURCE) instanceof Map ? (Map<?, ?>) context.get(REGISTER_SOURCE) : Collections.emptyMap();
        BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        Map<String, List<HistoricTaskInstance>> result = new LinkedHashMap<>();
        List<HistoricTaskInstance> ancestors = internalAncestors(task);
        if (!ancestors.isEmpty()) result.put(task.getTaskDefinitionKey(), ancestors);
        List<HistoricTaskInstance> history = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).finished().includeTaskLocalVariables().list();
        for (String node : path) {
            if (node.equals(task.getTaskDefinitionKey()) || !(model.getFlowElement(node) instanceof UserTask)) continue;
            if (!BpmnModelUtils.isSequentialReachable(model.getFlowElement(task.getTaskDefinitionKey()), model.getFlowElement(node), null)) continue;
            Object batch = batches.get(node);
            List<HistoricTaskInstance> matches = history.stream().filter(h -> node.equals(h.getTaskDefinitionKey())
                    && (registerSources.get(node) == null || registerSources.get(node).equals(h.getId()))
                    && h.getAssignee() != null && BpmTaskStatusEnum.APPROVE.getStatus().equals(h.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS))
                    && (batch == null || h.getTaskLocalVariables().get(BATCH) instanceof Map
                    && batch.equals(((Map<?, ?>) h.getTaskLocalVariables().get(BATCH)).get(node))))
                    .collect(Collectors.toList());
            // 无批次的历史只允许实际路径内唯一一次办理，不用时间窗口猜测轮次。
            if (!matches.isEmpty() && (batch != null || matches.size() == 1)) result.put(node, matches);
        }
        return result;
    }

    public List<BpmTaskReturnTargetRespVO> targets(Long userId, String taskId) {
        return managementService.executeCommand(command -> {
            Task task = current(userId, taskId);
            if ("LEGACY".equals(mode(task))) {
                requireLegacyReady(task);
                return legacyTargets(task);
            }
            BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            List<BpmTaskReturnTargetRespVO> result = new ArrayList<>();
            histories(task).forEach((node, history) -> {
                BpmTaskReturnTargetRespVO target = new BpmTaskReturnTargetRespVO();
                target.setReturnMode("SINGLE"); target.setTaskDefinitionKey(node); target.setName(model.getFlowElement(node).getName());
                Set<Long> users = history.stream().map(h -> Long.valueOf(h.getAssignee())).collect(Collectors.toCollection(LinkedHashSet::new));
                List<BpmTaskReturnTargetRespVO.Candidate> candidates = new ArrayList<>();
                for (AdminUserRespDTO user : adminUserApi.getUserList(users)) {
                    if (!Integer.valueOf(0).equals(user.getStatus())) continue;
                    BpmTaskReturnTargetRespVO.Candidate candidate = new BpmTaskReturnTargetRespVO.Candidate();
                    candidate.setId(user.getId()); candidate.setName(user.getNickname()); candidates.add(candidate);
                }
                if (node.equals(task.getTaskDefinitionKey())) {
                    Map<Long, String> names = candidates.stream().collect(Collectors.toMap(BpmTaskReturnTargetRespVO.Candidate::getId, BpmTaskReturnTargetRespVO.Candidate::getName));
                    candidates.clear();
                    for (int index = 0; index < history.size(); index++) {
                        HistoricTaskInstance ancestor = history.get(index); Long id = Long.valueOf(ancestor.getAssignee());
                        if (!names.containsKey(id)) continue;
                        BpmTaskReturnTargetRespVO.Candidate candidate = new BpmTaskReturnTargetRespVO.Candidate();
                        candidate.setId(id); candidate.setTaskId(ancestor.getId());
                        candidate.setName(names.get(id) + (index == 0 ? "（上一手）" : index == history.size() - 1 ? "（最初办理人）" : "（前序办理人）"));
                        candidates.add(candidate);
                    }
                }
                target.setAssignees(candidates);
                if (!candidates.isEmpty()) result.add(target);
            });
            return result;
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void returnTask(Long userId, BpmTaskReturnReqVO request) {
        managementService.executeCommand(command -> {
            Task task = current(userId, request.getId());
            if (!"SINGLE".equals(mode(task)) || request.getExpectedMode() != null && !"SINGLE".equals(request.getExpectedMode()))
                throw exception(TASK_RETURN_INVALID, "退回方式已变化，请刷新后重新确认");
            ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
            if (process == null || process.isSuspended() || process.isEnded()) throw exception(TASK_RETURN_INVALID, "流程已结束或挂起");
            process.forceUpdate();
            List<HistoricTaskInstance> histories = histories(task).get(request.getTargetTaskDefinitionKey());
            if (histories == null) throw exception(TASK_RETURN_INVALID, "目标不在当前任务可核验的历史路径和轮次中");
            if (request.getTargetTaskId() != null) histories = histories.stream().filter(h -> request.getTargetTaskId().equals(h.getId())).collect(Collectors.toList());
            if (histories.isEmpty()) throw exception(TASK_RETURN_INVALID, "指定的历史任务不属于当前退回路径");
            Set<Long> eligible = histories.stream().map(h -> Long.valueOf(h.getAssignee())).collect(Collectors.toSet());
            List<Long> selected = request.getReturnAssignees();
            if (selected == null || selected.isEmpty()) {
                if (eligible.size() != 1) throw exception(TASK_RETURN_INVALID, "请选择一名退回办理人");
                selected = new ArrayList<>(eligible);
            }
            if (selected.size() != 1 || !eligible.contains(selected.get(0))) throw exception(TASK_RETURN_INVALID, "只能选择本次历史节点的一名办理人");
            Long assignee = selected.get(0);
            adminUserApi.validateUser(assignee);
            BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            UserTask target = (UserTask) model.getFlowElement(request.getTargetTaskDefinitionKey());
            ExecutionEntity child = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
            ExecutionEntity root = child.getParent();
            boolean multi = root != null && root.isMultiInstanceRoot();
            if (target.getSubProcess() != null || ((UserTask) child.getCurrentFlowElement()).getSubProcess() != null)
                throw exception(TASK_RETURN_INVALID, "暂不支持跨子流程退回");
            if (multi && !(root.getCurrentFlowElement() instanceof UserTask
                    && (((UserTask) root.getCurrentFlowElement()).getBehavior() instanceof BpmParallelMultiInstanceBehavior
                    || ((UserTask) root.getCurrentFlowElement()).getBehavior() instanceof BpmSequentialMultiInstanceBehavior)))
                throw exception(TASK_RETURN_INVALID, "不支持当前会签执行类型");
            Map<String, Object> context = context(task);
            List<String> path = path(context);
            boolean internalReturn = target.getId().equals(task.getTaskDefinitionKey());
            HistoricTaskInstance internalTarget = null;
            if (internalReturn) {
                List<HistoricTaskInstance> matches = histories.stream().filter(h -> assignee.toString().equals(h.getAssignee())).collect(Collectors.toList());
                if (matches.size() != 1) throw exception(TASK_RETURN_INVALID, "同一办理人有多次内循环记录，请选择具体退回任务");
                internalTarget = matches.get(0);
                context = new HashMap<>(internalTarget.getTaskLocalVariables());
                path = path(context);
                path.removeIf(target.getId()::equals);
                path.add(target.getId());
            }
            final List<String> returnPath = path;
            int targetIndex = path.indexOf(target.getId());
            Map<String, String> scope = new LinkedHashMap<>();
            Object inheritedScope = child.getVariable(SCOPE);
            if (inheritedScope instanceof Map) ((Map<?, ?>) inheritedScope).forEach((key, value) -> scope.put(key.toString(), value.toString()));
            List<String> segment = new ArrayList<>(path.subList(targetIndex, path.size()));
            if (!segment.get(segment.size() - 1).equals(task.getTaskDefinitionKey())) segment.add(task.getTaskDefinitionKey());
            for (int i = 0; i + 1 < segment.size(); i++) scope.put(segment.get(i), segment.get(i + 1));
            // 恢复分支独立存在，D/E 即使退到同一人同一节点也不合并。
            ExecutionEntity branch = CommandContextUtil.getExecutionEntityManager().createChildExecution(process);
            branch.setActive(false); branch.setScope(false); branch.setCurrentFlowElement(BpmnModelUtils.getEndEvent(model));
            branch.setVariableLocal(BpmDispatchSources.BRANCH, true);
            List<HistoricTaskInstance> opinionSources = histories.stream()
                    .filter(h -> assignee.toString().equals(h.getAssignee())).collect(Collectors.toList());
            if (opinionSources.size() == 1)
                branch.setVariableLocal(BpmRestoredOpinion.KEY, BpmRestoredOpinion.draft(opinionSources.get(0)));
            branch.setVariableLocal(SCOPE, scope);
            Map<String, Set<String>> retainedSources = new LinkedHashMap<>();
            Object inheritedSources = child.getVariable(RETAINED_SOURCES);
            if (inheritedSources instanceof Map) ((Map<?, ?>) inheritedSources).forEach((key, value) -> {
                if (value instanceof Collection) retainedSources.put(key.toString(), ((Collection<?>) value).stream()
                        .map(Object::toString).collect(Collectors.toCollection(LinkedHashSet::new)));
            });
            histories(task).forEach((key, records) -> retainedSources.computeIfAbsent(key, ignored -> new LinkedHashSet<>())
                    .addAll(records.stream().map(HistoricTaskInstance::getId).collect(Collectors.toSet())));
            branch.setVariableLocal(RETAINED_SOURCES, retainedSources);
            branch.setVariableLocal(TARGET, target.getId());
            if (internalTarget != null) {
                Map<String, Object> inherited = new LinkedHashMap<>();
                inherited.put(BpmInternalDispatch.SOURCE, internalTarget.getTaskLocalVariables().get(BpmInternalDispatch.SOURCE));
                BpmInternalDispatch.prepareContext(branch, target.getId(), assignee, inherited);
            }
            Map<String, String> retainedRoots = new LinkedHashMap<>();
            Object inheritedRoots = child.getVariable(RETAINED_ROOTS);
            if (inheritedRoots instanceof Map) ((Map<?, ?>) inheritedRoots).forEach((key, value) -> retainedRoots.put(key.toString(), value.toString()));
            if (multi && (internalReturn || segment.size() >= 2)) {
                retainedRoots.put(internalReturn ? target.getId() : segment.get(segment.size() - 2), root.getId());
                Object previous = process.getVariableLocal(RETAINED_PREFIX + root.getId());
                Set<Long> retained = new LinkedHashSet<>();
                Object members = previous != null ? previous : root.getVariableLocal(FlowableUtils.formatExecutionCollectionVariable(task.getTaskDefinitionKey()));
                if (members instanceof Collection) ((Collection<?>) members).forEach(id -> { if (id != null) retained.add(Long.valueOf(id.toString())); });
                retained.remove(Long.valueOf(task.getAssignee()));
                if (internalReturn) {
                    for (HistoricTaskInstance ancestor : internalAncestors(task)) {
                        retained.remove(Long.valueOf(ancestor.getAssignee()));
                        if (ancestor.getId().equals(internalTarget.getId())) break;
                    }
                }
                process.setVariableLocal(RETAINED_PREFIX + root.getId(), retained);
            }
            branch.setVariableLocal(RETAINED_ROOTS, retainedRoots);
            branch.setVariableLocal(PATH, new ArrayList<>(path.subList(0, targetIndex)));
            Map<String, Object> batches = new LinkedHashMap<>();
            if (context.get(BATCH) instanceof Map) ((Map<?, ?>) context.get(BATCH)).forEach((key, value) -> {
                if (returnPath.subList(0, targetIndex).contains(key.toString())) batches.put(key.toString(), value);
            });
            branch.setVariableLocal(BATCH, batches);
            branch.setVariableLocal(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, new LinkedHashMap<>());
            branch.setVariableLocal(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME, new LinkedHashMap<>());
            java.util.regex.Pattern manual = java.util.regex.Pattern.compile("\\b(?:select_node_\\d+|next_node\\d+)\\b");
            for (SequenceFlow flow : model.getMainProcess().findFlowElementsOfType(SequenceFlow.class, true)) {
                if (flow.getConditionExpression() == null) continue;
                java.util.regex.Matcher matcher = manual.matcher(flow.getConditionExpression());
                while (matcher.find()) branch.setVariableLocal(matcher.group(), null);
            }
            branch.setVariableLocal(BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap(target.getId(), Collections.singleton(assignee)));
            branch.setVariableLocal(String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, target.getId()), true);
            BpmTaskSmsPolicy.prepareDefault(runtimeService, branch.getId(), target.getId(), Boolean.TRUE.equals(request.getSendSms()));
            ExecutionEntity entry = CommandContextUtil.getExecutionEntityManager().createChildExecution(branch);
            entry.setCurrentFlowElement(target);
            CommandContextUtil.getAgenda().planContinueProcessOperation(entry);

            String description = "退回【" + task.getName() + "】至【" + target.getName() + "】"
                    + adminUserApi.getUser(assignee).getNickname() + "；原因：" + request.getReason();
            flowableTaskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.RETURN.getStatus());
            flowableTaskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON, request.getReason());
            flowableTaskService.addComment(task.getId(), task.getProcessInstanceId(), "4", description);
            if (multi) exitMember(root, child, task, model);
            else runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                    .moveExecutionToActivityId(child.getId(), BpmnModelUtils.getEndEvent(model).getId()).changeState();
            return null;
        });
    }

    private void exitMember(ExecutionEntity root, ExecutionEntity child, Task task, BpmnModel model) {
        root.forceUpdate();
        Object origins = root.getVariableLocal(BpmDispatchSources.SOURCES);
        Object sources = origins instanceof Map ? ((Map<?, ?>) origins).get(task.getAssignee()) : null;
        if (sources instanceof Collection) for (Object source : (Collection<?>) sources) {
            ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(task.getProcessInstanceId());
            Object previous = process.getVariableLocal(RELEASED_PREFIX + source);
            Map<String, Set<Long>> released = new LinkedHashMap<>();
            if (previous instanceof Map) ((Map<?, ?>) previous).forEach((key, value) -> {
                if (value instanceof Collection) released.put(key.toString(), ((Collection<?>) value).stream()
                        .map(id -> Long.valueOf(id.toString())).collect(Collectors.toCollection(LinkedHashSet::new)));
            });
            released.computeIfAbsent(task.getTaskDefinitionKey(), ignored -> new LinkedHashSet<>()).add(Long.valueOf(task.getAssignee()));
            process.setVariableLocal(RELEASED_PREFIX + source, released);
        }
        int completed = number(root, "nrOfCompletedInstances") + 1;
        int returned = number(root, RETURNED) + 1;
        UserTask node = (UserTask) root.getCurrentFlowElement();
        // 退回计为已结束，历史仍为 RETURN；不在这里计算或签通过条件。
        CommandContextUtil.getExecutionEntityManager().deleteChildExecutions(child, "单任务退回", false);
        CommandContextUtil.getExecutionEntityManager().deleteExecutionAndRelatedData(child, "单任务退回", false);
        root.setVariableLocal("nrOfCompletedInstances", completed);
        root.setVariableLocal(RETURNED, returned);
        BpmTaskSmsPolicy.removeMembers(root, node.getId(), Collections.singleton(Long.valueOf(task.getAssignee())));
        if (completed == number(root, "nrOfInstances")) {
            if (completed == returned + number(root, BpmInternalDispatch.HANDOFFS)) {
                runtimeService.createChangeActivityStateBuilder().processInstanceId(task.getProcessInstanceId())
                        .moveExecutionToActivityId(root.getId(), BpmnModelUtils.getEndEvent(model).getId()).changeState();
            } else if (node.getBehavior() instanceof BpmParallelMultiInstanceBehavior) {
                ((BpmParallelMultiInstanceBehavior) node.getBehavior()).completeAfterWithdrawal(root);
            } else {
                ((BpmSequentialMultiInstanceBehavior) node.getBehavior()).completeAfterWithdrawal(root);
            }
        } else if (node.getLoopCharacteristics().isSequential()) {
            ExecutionEntity next = CommandContextUtil.getExecutionEntityManager().createChildExecution(root);
            next.setCurrentFlowElement(node);
            ((BpmSequentialMultiInstanceBehavior) node.getBehavior()).continueSequentialMultiInstance(next, completed, root);
        }
    }

    private static int number(ExecutionEntity root, String key) {
        Object value = root.getVariableLocal(key); return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /** 退回后原会签仍可追加/内循环，新增成员也不能在重办分支重复派发。 */
    public static void retainAddedMembers(RuntimeService runtime, String instance, String root, Collection<Long> added) {
        Object value = runtime.getVariableLocal(instance, RETAINED_PREFIX + root);
        if (!(value instanceof Collection)) return;
        Set<Long> users = ((Collection<?>) value).stream().map(id -> Long.valueOf(id.toString())).collect(Collectors.toCollection(LinkedHashSet::new));
        users.addAll(added);
        runtime.setVariableLocal(instance, RETAINED_PREFIX + root, users);
    }

    public static Set<Long> retainedUsers(RuntimeService runtime, String instance, String execution, String node) {
        Set<Long> result = new LinkedHashSet<>();
        if (execution == null) return result;
        Object roots = runtime.getVariable(execution, RETAINED_ROOTS);
        Object root = roots instanceof Map ? ((Map<?, ?>) roots).get(node) : null;
        if (root == null) return result;
        Object users = runtime.getVariableLocal(instance, RETAINED_PREFIX + root);
        if (users instanceof Collection) ((Collection<?>) users).forEach(id -> result.add(Long.valueOf(id.toString())));
        return result;
    }

    /** 恢复全部原有流向，仅禁止重复派发仍保留的人员。 */
    public static Map<String, Set<Long>> retainedRecipients(RuntimeService runtime, String instance, String execution, String node) {
        return retainedRecipients(runtime, null, instance, execution, node);
    }

    public static Map<String, Set<Long>> retainedRecipients(RuntimeService runtime, HistoryService history,
                                                           String instance, String execution, String node) {
        Map<String, Set<Long>> result = new LinkedHashMap<>();
        if (execution == null) return result;
        Set<String> sources = BpmDispatchSources.restoredSources(runtime.getVariable(execution, RETAINED_SOURCES), node);
        Object roots = runtime.getVariable(execution, RETAINED_ROOTS);
        // 已生成的恢复任务没有新快照时，从其明确关联的原会签历史来源读取，不按时间猜测。
        if (sources.isEmpty() && history != null && roots instanceof Map && ((Map<?, ?>) roots).get(node) != null) {
            org.flowable.variable.api.history.HistoricVariableInstance origins = history.createHistoricVariableInstanceQuery()
                    .executionId(((Map<?, ?>) roots).get(node).toString()).variableName(BpmDispatchSources.SOURCES).singleResult();
            if (origins != null && origins.getValue() instanceof Map) {
                Set<String> candidates = new LinkedHashSet<>();
                ((Map<?, ?>) origins.getValue()).values().forEach(value -> {
                    if (value instanceof Collection) ((Collection<?>) value).forEach(id -> candidates.add(id.toString()));
                });
                for (String candidate : candidates) {
                    HistoricTaskInstance source = history.createHistoricTaskInstanceQuery().taskId(candidate).singleResult();
                    if (source != null && instance.equals(source.getProcessInstanceId()) && node.equals(source.getTaskDefinitionKey())) sources.add(candidate);
                }
            }
        }
        Map<String, String> rootTargets = new LinkedHashMap<>();
        addRetainedRoot(rootTargets, roots, runtime.getVariable(execution, SCOPE), node);
        for (String source : sources) {
            if (history != null) {
                HistoricTaskInstance historic = history.createHistoricTaskInstanceQuery().taskId(source).includeTaskLocalVariables().singleResult();
                if (historic != null && instance.equals(historic.getProcessInstanceId())) {
                    Map<String, Object> locals = historic.getTaskLocalVariables();
                    addRetainedRoot(rootTargets, locals.get(RETAINED_ROOTS), locals.get(SCOPE), node);
                }
            }
            Object ledger = runtime.getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source);
            Object released = runtime.getVariableLocal(instance, RELEASED_PREFIX + source);
            if (ledger instanceof Map) ((Map<?, ?>) ledger).forEach((target, users) -> {
                if (users instanceof Collection) {
                    Set<Long> members = result.computeIfAbsent(target.toString(), ignored -> new LinkedHashSet<>());
                    Object excluded = released instanceof Map ? ((Map<?, ?>) released).get(target) : null;
                    Set<Long> absent = excluded instanceof Collection ? ((Collection<?>) excluded).stream()
                            .map(id -> Long.valueOf(id.toString())).collect(Collectors.toSet()) : Collections.emptySet();
                    ((Collection<?>) users).forEach(user -> {
                        Long id = Long.valueOf(user.toString()); if (!absent.contains(id)) members.add(id);
                    });
                }
            });
        }
        // 每轮快照仅替换其自身会签的成员，不能用最新一轮的空集合覆盖前几轮保留人员。
        Map<String, Set<Long>> snapshots = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rootTargets.entrySet()) {
            Object retained = runtime.getVariableLocal(instance, RETAINED_PREFIX + entry.getKey());
            if (!(retained instanceof Collection)) continue;
            Object members = null;
            String collection = FlowableUtils.formatExecutionCollectionVariable(entry.getValue());
            if (history != null) {
                org.flowable.variable.api.history.HistoricVariableInstance variable = history.createHistoricVariableInstanceQuery()
                        .executionId(entry.getKey()).variableName(collection).singleResult();
                if (variable != null) members = variable.getValue();
            } else if (runtime.createExecutionQuery().executionId(entry.getKey()).singleResult() != null) {
                members = runtime.getVariableLocal(entry.getKey(), collection);
            }
            Set<Long> recipients = result.computeIfAbsent(entry.getValue(), ignored -> new LinkedHashSet<>());
            if (members instanceof Collection) ((Collection<?>) members).forEach(id -> recipients.remove(Long.valueOf(id.toString())));
            Set<Long> snapshot = snapshots.computeIfAbsent(entry.getValue(), ignored -> new LinkedHashSet<>());
            ((Collection<?>) retained).forEach(id -> snapshot.add(Long.valueOf(id.toString())));
        }
        snapshots.forEach((target, users) -> result.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).addAll(users));
        return result;
    }

    private static void addRetainedRoot(Map<String, String> result, Object roots, Object scope, String node) {
        Object root = roots instanceof Map ? ((Map<?, ?>) roots).get(node) : null;
        if (root == null) return;
        Object target = scope instanceof Map ? ((Map<?, ?>) scope).get(node) : null;
        result.put(root.toString(), target == null ? node : target.toString());
    }

    public static String retainedTarget(RuntimeService runtime, String executionId, String node) {
        Object scope = runtime.getVariable(executionId, SCOPE);
        Object next = scope instanceof Map ? ((Map<?, ?>) scope).get(node) : null;
        return next == null ? null : next.toString();
    }
}
