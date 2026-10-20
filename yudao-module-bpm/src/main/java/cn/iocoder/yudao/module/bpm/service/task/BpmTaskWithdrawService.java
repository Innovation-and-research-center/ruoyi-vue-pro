package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.impl.bpmn.behavior.MultiInstanceActivityBehavior;
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

/** 按来源和精确会签实例撤回未完成成员；禁止按时间或节点名猜测派发关系。 */
@Service
public class BpmTaskWithdrawService {
    private static final String RESTORED = "bpm_dispatch_restored_";
    private static final String CANCEL_REASON = "来源办理人撤回派发";
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;
    @Resource private RepositoryService repositoryService;
    @Resource private ManagementService managementService;
    @Resource private TaskService flowableTaskService;
    @Resource private BpmProcessDefinitionService processDefinitionService;
    @Resource private AdminUserApi adminUserApi;

    private HistoricTaskInstance source(Long userId, String taskId) {
        HistoricTaskInstance source = historyService.createHistoricTaskInstanceQuery().taskId(taskId)
                .includeTaskLocalVariables().singleResult();
        if (source == null || source.getEndTime() == null || !String.valueOf(userId).equals(source.getAssignee())
                || !BpmTaskStatusEnum.APPROVE.getStatus().equals(source.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS))) {
            throw exception(TASK_WITHDRAW_FAIL_TASK_NOT_EXISTS);
        }
        if (runtimeService.createProcessInstanceQuery().processInstanceId(source.getProcessInstanceId()).active().singleResult() == null)
            throw exception(TASK_WITHDRAW_FAIL_PROCESS_NOT_RUNNING);
        cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO info =
                processDefinitionService.getProcessDefinitionInfo(source.getProcessDefinitionId());
        if (info == null || !Boolean.TRUE.equals(info.getAllowWithdrawTask())) throw exception(TASK_WITHDRAW_FAIL_NOT_ALLOW);
        return source;
    }

    public List<BpmTaskWithdrawTargetRespVO> targets(Long userId, String taskId) {
        HistoricTaskInstance source = source(userId, taskId);
        List<BpmTaskWithdrawTargetRespVO> result = managementService.executeCommand(context -> { requireMemberMode(source); requirePartialAllowed(source); return inspect(source); });
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserList(result.stream()
                .map(BpmTaskWithdrawTargetRespVO::getUserId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(AdminUserRespDTO::getId, user -> user, (a, b) -> a));
        result.forEach(row -> row.setNickname(users.containsKey(row.getUserId())
                ? users.get(row.getUserId()).getNickname() : "人员 " + row.getUserId()));
        result.sort(Comparator.comparing((BpmTaskWithdrawTargetRespVO row) -> !row.getNodeName().contains("主办"))
                .thenComparing(BpmTaskWithdrawTargetRespVO::getNodeId).thenComparing(BpmTaskWithdrawTargetRespVO::getUserId));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<BpmTaskWithdrawTargetRespVO> inspect(HistoricTaskInstance source) {
        List<BpmTaskWithdrawTargetRespVO> result = new ArrayList<>();
        BpmnModel model = repositoryService.getBpmnModel(source.getProcessDefinitionId());
        List<Task> tasks = flowableTaskService.createTaskQuery().processInstanceId(source.getProcessInstanceId()).active().list();
        for (ExecutionEntity root : CommandContextUtil.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(source.getProcessInstanceId())) {
            Object originValue = root.getVariableLocal(BpmDispatchSources.SOURCES);
            if (!root.isMultiInstanceRoot() || !BpmDispatchSources.contains(originValue, source.getId())) continue;
            FlowElement element = model.getFlowElement(root.getActivityId());
            if (!(element instanceof UserTask)) continue;
            UserTask node = (UserTask) element;
            List<Long> members = members(root);
            List<ExecutionEntity> children = children(root);
            Map<String, Set<String>> origins = (Map<String, Set<String>>) originValue;
            boolean consistent = members.size() == number(root, "nrOfInstances")
                    && members.stream().allMatch(user -> user != null && !origins.getOrDefault(user.toString(), Collections.emptySet()).isEmpty());
            int completed = number(root, "nrOfCompletedInstances");
            for (int index = 0; index < members.size(); index++) {
                Long user = members.get(index);
                if (user == null || !origins.getOrDefault(user.toString(), Collections.emptySet()).contains(source.getId())) continue;
                ExecutionEntity child = children.stream().filter(e -> Objects.equals(user, memberId(e, node.getId())) && e.isActive()).findFirst().orElse(null);
                Task task = child == null ? null : tasks.stream().filter(t -> child.getId().equals(t.getExecutionId())
                        && user.toString().equals(t.getAssignee())).findFirst().orElse(null);
                boolean queued = node.getLoopCharacteristics().isSequential() && index >= completed && child == null;
                BpmTaskWithdrawTargetRespVO row = new BpmTaskWithdrawTargetRespVO();
                row.setExecutionId(root.getId()); row.setNodeId(node.getId()); row.setNodeName(node.getName() == null ? node.getId() : node.getName()); row.setUserId(user);
                row.setTaskId(task == null ? null : task.getId()); row.setQueued(queued);
                // 当前成员任务不存在/转办/执行中、或旧内循环集合计数不一致时不猜测撤回。
                row.setWithdrawable(consistent && !root.isSuspended() && (queued || task != null));
                row.setDisabledReason(row.isWithdrawable() ? null : consistent ? "已办理或任务状态已变化" : "会签成员计数或来源记录不完整，暂不能撤回");
                result.add(row);
            }
        }
        Object value = runtimeService.getVariableLocal(source.getProcessInstanceId(), BpmDispatchSources.LEDGER_PREFIX + source.getId());
        if (value instanceof Map) ((Map<String, Set<Long>>) value).forEach((nodeId, users) -> {
            FlowElement element = model.getFlowElement(nodeId);
            for (Long user : users) {
                if (result.stream().anyMatch(row -> row.getNodeId().equals(nodeId) && row.getUserId().equals(user))) continue;
                BpmTaskWithdrawTargetRespVO row = new BpmTaskWithdrawTargetRespVO();
                row.setExecutionId(""); row.setNodeId(nodeId); row.setNodeName(element == null || element.getName() == null ? nodeId : element.getName());
                row.setUserId(user); row.setDisabledReason("目标已结束或尚未生成可撤回的会签实例"); result.add(row);
            }
        });
        return result;
    }

    /** 以本次派发的持久标记分流，不以流程创建时间判断，也不在新版失败后降级。 */
    public String mode(Long userId, String taskId) {
        HistoricTaskInstance source = source(userId, taskId);
        if (!memberMode(source)) return "LEGACY";
        return partialAllowed(source) ? "MEMBERS" : "ALL";
    }

    private boolean partialAllowed(HistoricTaskInstance source) {
        return BpmTaskDispatchPolicy.allowsMemberOperations(repositoryService.getBpmnModel(source.getProcessDefinitionId()), source.getTaskDefinitionKey());
    }

    private void requirePartialAllowed(HistoricTaskInstance source) {
        if (!partialAllowed(source)) throw exception(TASK_WITHDRAW_INVALID, "只有收文主任拟办节点允许按人员撤回，当前节点只能全部撤回");
    }

    private boolean memberMode(HistoricTaskInstance source) {
        if (source.getTaskLocalVariables().containsKey(BpmDispatchSources.OPTIONS)
                || runtimeService.hasVariableLocal(source.getProcessInstanceId(), BpmDispatchSources.LEDGER_PREFIX + source.getId())
                || runtimeService.hasVariableLocal(source.getProcessInstanceId(), RESTORED + source.getId())) return true;
        return runtimeService.createVariableInstanceQuery().processInstanceId(source.getProcessInstanceId())
                .excludeTaskVariables().variableNameLike("bpm_dispatch_%").list().stream().anyMatch(variable ->
                        BpmDispatchSources.SOURCES.equals(variable.getName()) && BpmDispatchSources.contains(variable.getValue(), source.getId())
                        || variable.getName().startsWith("bpm_dispatch_pending_") && variable.getValue() instanceof Collection
                        && ((Collection<?>) variable.getValue()).contains(source.getId()));
    }

    private void requireMemberMode(HistoricTaskInstance source) {
        if (!memberMode(source)) throw exception(TASK_WITHDRAW_INVALID, "该任务使用原版整体撤回，请刷新后重新操作");
    }

    /** 原逻辑清理流程级变量，因此混有新版派发时不能进入；提交时重新核验并锁定流程。 */
    @Transactional(rollbackFor = Exception.class)
    public void executeLegacy(Long userId, String taskId, Runnable operation) {
        managementService.executeCommand(context -> {
            HistoricTaskInstance source = source(userId, taskId);
            if (memberMode(source)) throw exception(TASK_WITHDRAW_INVALID, "该任务已接入新版派发，请刷新后重新确认撤回方式");
            boolean mixed = runtimeService.createVariableInstanceQuery().processInstanceId(source.getProcessInstanceId())
                    .excludeTaskVariables().variableNameLike("bpm_dispatch_%").list().stream().anyMatch(variable -> {
                        Object value = variable.getValue();
                        // 新引擎也会给尚无来源的旧根写空 SOURCES；空集合不代表新版派发。
                        if (BpmDispatchSources.SOURCES.equals(variable.getName())) return value instanceof Map && !((Map<?, ?>) value).isEmpty();
                        if (variable.getName().startsWith("bpm_dispatch_pending_")) return value instanceof Collection && !((Collection<?>) value).isEmpty();
                        return variable.getName().startsWith(BpmDispatchSources.LEDGER_PREFIX)
                                || variable.getName().startsWith(RESTORED)
                                || BpmDispatchSources.BRANCH.equals(variable.getName()) && Boolean.TRUE.equals(value)
                                || BpmDispatchSources.NEW_MEMBERS.equals(variable.getName()) && value instanceof Map && !((Map<?, ?>) value).isEmpty();
                    });
            if (mixed) throw exception(TASK_WITHDRAW_INVALID, "该流程已混有新版派发或追加记录，不能使用原版整体撤回，以免影响其他分支");
            ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(source.getProcessInstanceId());
            if (process == null || process.isSuspended() || process.isEnded()) throw exception(TASK_WITHDRAW_FAIL_PROCESS_NOT_RUNNING);
            process.forceUpdate();
            return null;
        });
        // 仍在同一 Spring 事务中，但先结束校验命令。原代码依赖 changeState/addMultiInstanceExecution
        // 返回时 agenda 已创建任务，再查询并设置办理人；不能把它们嵌在同一个外层引擎命令里。
        operation.run();
    }

    /** 新版整体撤回仍按来源逐人校验；旧任务由业务入口调用原版实现。 */
    @Transactional(rollbackFor = Exception.class)
    public void withdrawAll(Long userId, String sourceTaskId) {
        BpmTaskWithdrawReqVO request = new BpmTaskWithdrawReqVO(); request.setSourceTaskId(sourceTaskId);
        executeWithdrawal(userId, request, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long userId, BpmTaskWithdrawReqVO request) {
        executeWithdrawal(userId, request, false);
    }

    private void executeWithdrawal(Long userId, BpmTaskWithdrawReqVO request, boolean all) {
        if (request == null || (!all && (request.getMembers() == null || request.getMembers().isEmpty() || request.getMembers().size() > 200)))
            throw exception(TASK_WITHDRAW_INVALID, "请选择需要撤回的未完成成员");
        HistoricTaskInstance source = source(userId, request.getSourceTaskId());
        if (!all) requirePartialAllowed(source);
        managementService.executeCommand(context -> {
            ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(source.getProcessInstanceId());
            if (process == null || process.isSuspended() || process.isEnded()) throw exception(TASK_WITHDRAW_FAIL_PROCESS_NOT_RUNNING);
            process.forceUpdate();
            if (process.getVariableLocal(RESTORED + source.getId()) != null) throw exception(TASK_WITHDRAW_INVALID, "该来源已撤回并恢复待办，请刷新");
            requireMemberMode(source);
            if (!(process.getVariableLocal(BpmDispatchSources.LEDGER_PREFIX + source.getId()) instanceof Map))
                throw exception(TASK_WITHDRAW_INVALID, "新版派发记录尚未完整生成，请稍后重试，不能降级为原版撤回");
            boolean pending = runtimeService.createVariableInstanceQuery().processInstanceId(source.getProcessInstanceId())
                    .excludeTaskVariables().variableNameLike("bpm_dispatch_pending_%").list().stream()
                    .anyMatch(variable -> variable.getValue() instanceof Collection && ((Collection<?>) variable.getValue()).contains(source.getId()));
            if (pending) throw exception(TASK_WITHDRAW_INVALID, "本次派发尚有异步任务生成中，请稍后重试");
            List<BpmTaskWithdrawTargetRespVO> allowed = inspect(source);
            if (all) {
                // 非主任节点只能完整拉回本次派发，不能把“全撤”变成仅撤剩余可撤人员。
                if (!partialAllowed(source) && allowed.stream().anyMatch(row -> !row.isWithdrawable()))
                    throw exception(TASK_WITHDRAW_INVALID, "下游已有人员办理或目标状态不允许，不能全部撤回");
                request.setMembers(allowed.stream().filter(BpmTaskWithdrawTargetRespVO::isWithdrawable).map(row -> {
                    BpmTaskWithdrawReqVO.Member member = new BpmTaskWithdrawReqVO.Member();
                    member.setExecutionId(row.getExecutionId()); member.setUserId(row.getUserId()); return member;
                }).collect(Collectors.toList()));
                if (request.getMembers().isEmpty()) throw exception(TASK_WITHDRAW_INVALID, "没有可全部撤回的派发任务");
            }
            Map<String, Set<Long>> selected = new LinkedHashMap<>();
            List<BpmTaskWithdrawTargetRespVO> chosen = new ArrayList<>();
            for (BpmTaskWithdrawReqVO.Member member : request.getMembers()) {
                if (member == null || member.getUserId() == null || member.getExecutionId() == null)
                    throw exception(TASK_WITHDRAW_INVALID, "撤回目标不能为空");
                BpmTaskWithdrawTargetRespVO row = allowed.stream().filter(r -> r.isWithdrawable()
                        && member.getExecutionId().equals(r.getExecutionId()) && member.getUserId().equals(r.getUserId())).findFirst()
                        .orElseThrow(() -> exception(TASK_WITHDRAW_INVALID, "成员已办理或不属于本次派发，请刷新后重试"));
                if (!selected.computeIfAbsent(member.getExecutionId(), key -> new LinkedHashSet<>()).add(member.getUserId()))
                    throw exception(TASK_WITHDRAW_INVALID, "不能重复选择同一成员");
                chosen.add(row);
            }
            // 全批校验后再修改；一个无效成员使整个撤回回滚。
            List<ExecutionEntity> emptyRoots = new ArrayList<>();
            for (Map.Entry<String, Set<Long>> entry : selected.entrySet()) {
                ExecutionEntity root = CommandContextUtil.getExecutionEntityManager().findById(entry.getKey());
                removeMembers(source, root, entry.getValue(), chosen, emptyRoots);
            }
            Object remaining = process.getVariableLocal(BpmDispatchSources.LEDGER_PREFIX + source.getId());
            if (((Map<?, ?>) remaining).isEmpty()) restoreSource(source, process);
            // 先恢复来源再结束空目标，避免最后一个目标的取消误结束整个流程。
            for (ExecutionEntity root : emptyRoots) cancelRoot(root);
            writeComment(source, userId, request.getReason(), chosen);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void removeMembers(HistoricTaskInstance source, ExecutionEntity root, Set<Long> requested,
                               List<BpmTaskWithdrawTargetRespVO> chosen, List<ExecutionEntity> emptyRoots) {
        root.forceUpdate();
        UserTask node = (UserTask) root.getCurrentFlowElement();
        if (!(node.getBehavior() instanceof BpmParallelMultiInstanceBehavior) && !(node.getBehavior() instanceof BpmSequentialMultiInstanceBehavior))
            throw exception(TASK_WITHDRAW_INVALID, "当前目标不支持精确会签撤回");
        Map<String, Set<String>> origins = new LinkedHashMap<>((Map<String, Set<String>>) root.getVariableLocal(BpmDispatchSources.SOURCES));
        Set<Long> removed = new LinkedHashSet<>();
        for (Long user : requested) {
            Set<String> sources = new LinkedHashSet<>(origins.get(user.toString())); sources.remove(source.getId());
            if (sources.isEmpty()) { origins.remove(user.toString()); removed.add(user); }
            else origins.put(user.toString(), sources);
            BpmDispatchSources.forget(root, source.getId(), node.getId(), user);
        }
        root.setVariableLocal(BpmDispatchSources.SOURCES, origins);
        if (removed.isEmpty()) return;
        int completed = number(root, "nrOfCompletedInstances");
        List<Long> members = members(root); members.removeAll(removed);
        List<ExecutionEntity> activeChildren = children(root);
        boolean removedCurrent = false;
        for (ExecutionEntity child : activeChildren) {
            if (!child.isActive() || !removed.contains(memberId(child, node.getId()))) continue;
            removedCurrent = true;
            for (BpmTaskWithdrawTargetRespVO row : chosen) {
                if (root.getId().equals(row.getExecutionId()) && row.getTaskId() != null && row.getUserId().equals(memberId(child, node.getId()))) {
                    flowableTaskService.setVariableLocal(row.getTaskId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.CANCEL.getStatus());
                    flowableTaskService.setVariableLocal(row.getTaskId(), BpmnVariableConstants.TASK_VARIABLE_REASON, CANCEL_REASON);
                }
            }
            CommandContextUtil.getExecutionEntityManager().deleteChildExecutions(child, CANCEL_REASON, false);
            CommandContextUtil.getExecutionEntityManager().deleteExecutionAndRelatedData(child, CANCEL_REASON, false);
        }
        root.setVariableLocal(FlowableUtils.formatExecutionCollectionVariable(node.getId()), new LinkedHashSet<>(members));
        root.setVariableLocal("nrOfInstances", members.size());
        // 清掉被撤回成员尚未消费的短信参数，不影响其他成员。
        BpmTaskSmsPolicy.removeMembers(root, node.getId(), removed);
        if (members.isEmpty()) { emptyRoots.add(root); return; }
        MultiInstanceActivityBehavior behavior = (MultiInstanceActivityBehavior) node.getBehavior();
        boolean finished = completed > 0 && (completed == members.size() || behavior.completionConditionSatisfied(root));
        if (finished) {
            if (behavior instanceof BpmParallelMultiInstanceBehavior) ((BpmParallelMultiInstanceBehavior) behavior).completeAfterWithdrawal(root);
            else ((BpmSequentialMultiInstanceBehavior) behavior).completeAfterWithdrawal(root);
        } else if (node.getLoopCharacteristics().isSequential() && removedCurrent) {
            ExecutionEntity next = CommandContextUtil.getExecutionEntityManager().createChildExecution(root);
            next.setCurrentFlowElement(node);
            // 已办前缀保留，当前/排队撤回后从相同下标继续，不把撤回算成通过。
            ((BpmSequentialMultiInstanceBehavior) behavior).continueSequentialMultiInstance(next, completed, root);
        }
    }

    private void restoreSource(HistoricTaskInstance source, ExecutionEntity process) {
        BpmnModel model = repositoryService.getBpmnModel(source.getProcessDefinitionId());
        FlowElement element = model.getFlowElement(source.getTaskDefinitionKey());
        if (!(element instanceof UserTask) || ((UserTask) element).getSubProcess() != null)
            throw exception(TASK_WITHDRAW_INVALID, "当前来源不能恢复独立待办");
        UserTask node = (UserTask) element;
        ExecutionEntity branch = CommandContextUtil.getExecutionEntityManager().createChildExecution(process);
        branch.setActive(false); branch.setScope(false); branch.setCurrentFlowElement(BpmnModelUtils.getEndEvent(model));
        branch.setVariableLocal(BpmDispatchSources.BRANCH, true);
        // 恢复原任务的退回路径上下文；不继承流程级其他分支的路径。
        for (String key : Arrays.asList("RETURN_BRANCH_CONTEXT", "RETURNABLE_TASK_PATH")) {
            Object value = source.getTaskLocalVariables().get(key);
            branch.setVariableLocal(key, value instanceof Map ? new LinkedHashMap<>((Map<?, ?>) value)
                    : value instanceof Collection ? new ArrayList<>((Collection<?>) value) : null);
        }
        branch.setVariableLocal(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, new LinkedHashMap<>());
        branch.setVariableLocal(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME, new LinkedHashMap<>());
        java.util.regex.Pattern route = java.util.regex.Pattern.compile("\\b(?:select_node_\\d+|next_node\\d+)\\b");
        for (SequenceFlow flow : model.getMainProcess().findFlowElementsOfType(SequenceFlow.class, true)) {
            if (flow.getConditionExpression() == null) continue;
            java.util.regex.Matcher matcher = route.matcher(flow.getConditionExpression());
            while (matcher.find()) branch.setVariableLocal(matcher.group(), null);
        }
        if (!BpmRegisterTaskUtils.isRegisterTask(node)) branch.setVariableLocal(BpmDispatchSources.NEW_MEMBERS,
                Collections.singletonMap(node.getId(), new LinkedHashSet<>(Collections.singleton(Long.valueOf(source.getAssignee())))));
        // 来源恢复不主动发送短信；本次撤回没有提供新的短信发送选择。
        BpmTaskSmsPolicy.prepareDefault(runtimeService, branch.getId(), node.getId(), false);
        ExecutionEntity entry = CommandContextUtil.getExecutionEntityManager().createChildExecution(branch);
        entry.setCurrentFlowElement(node);
        process.setVariableLocal(RESTORED + source.getId(), branch.getId());
        CommandContextUtil.getAgenda().planContinueProcessOperation(entry);
    }

    private void cancelRoot(ExecutionEntity root) {
        // 无人完成且全撤：取消目标，不执行目标的出线和“审批通过”逻辑。
        BpmnModel model = repositoryService.getBpmnModel(root.getProcessDefinitionId());
        EndEvent end = BpmnModelUtils.getEndEvent(model);
        if (end == null) throw exception(TASK_WITHDRAW_INVALID, "模型缺少结束节点");
        runtimeService.createChangeActivityStateBuilder().processInstanceId(root.getProcessInstanceId())
                .moveExecutionToActivityId(root.getId(), end.getId()).changeState();
    }

    private void writeComment(HistoricTaskInstance source, Long userId, String reason, List<BpmTaskWithdrawTargetRespVO> chosen) {
        Set<Long> ids = chosen.stream().map(BpmTaskWithdrawTargetRespVO::getUserId).collect(Collectors.toSet());
        Map<Long, String> names = adminUserApi.getUserList(ids).stream().collect(Collectors.toMap(AdminUserRespDTO::getId, AdminUserRespDTO::getNickname));
        String description = "撤回派发：" + chosen.stream().map(row -> "【" + row.getNodeName() + "】" + names.getOrDefault(row.getUserId(), row.getUserId().toString()))
                .collect(Collectors.joining("、")) + (reason == null || reason.trim().isEmpty() ? "" : "；原因：" + reason);
        org.flowable.engine.impl.persistence.entity.CommentEntityManager manager = CommandContextUtil.getProcessEngineConfiguration().getCommentEntityManager();
        org.flowable.engine.impl.persistence.entity.CommentEntity comment = manager.create();
        comment.setTaskId(source.getId()); comment.setProcessInstanceId(source.getProcessInstanceId()); comment.setUserId(userId.toString());
        comment.setType("10"); comment.setAction("AddComment"); comment.setTime(CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime());
        comment.setMessage(description.length() > 160 ? description.substring(0, 160) + "..." : description); comment.setFullMessage(description); manager.insert(comment);
    }

    private static int number(ExecutionEntity root, String name) { return ((Number) root.getVariableLocal(name)).intValue(); }
    private static List<Long> members(ExecutionEntity root) {
        Object value = root.getVariableLocal(FlowableUtils.formatExecutionCollectionVariable(root.getActivityId()));
        if (!(value instanceof Collection)) return Collections.emptyList();
        return ((Collection<?>) value).stream().map(v -> v == null ? null : Long.valueOf(v.toString())).collect(Collectors.toList());
    }
    private static List<ExecutionEntity> children(ExecutionEntity root) {
        return CommandContextUtil.getExecutionEntityManager().findChildExecutionsByParentExecutionId(root.getId());
    }
    private static Long memberId(ExecutionEntity child, String node) {
        Object value = child.getVariableLocal(FlowableUtils.formatExecutionCollectionElementVariable(node));
        return value == null ? null : Long.valueOf(value.toString());
    }
}
