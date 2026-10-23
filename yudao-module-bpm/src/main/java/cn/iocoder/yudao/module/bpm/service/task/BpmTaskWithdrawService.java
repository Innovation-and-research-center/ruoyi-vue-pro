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
        List<BpmTaskWithdrawTargetRespVO> result = managementService.executeCommand(context -> {
            requireMemberMode(source); requirePartialAllowed(source);
            return inspect(source, withdrawalSources(source).keySet());
        });
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
    private List<BpmTaskWithdrawTargetRespVO> inspect(HistoricTaskInstance source, Set<String> sourceIds) {
        List<BpmTaskWithdrawTargetRespVO> result = new ArrayList<>();
        BpmnModel model = repositoryService.getBpmnModel(source.getProcessDefinitionId());
        List<Task> tasks = flowableTaskService.createTaskQuery().processInstanceId(source.getProcessInstanceId()).active().list();
        for (ExecutionEntity root : CommandContextUtil.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(source.getProcessInstanceId())) {
            Object originValue = root.getVariableLocal(BpmDispatchSources.SOURCES);
            if (!root.isMultiInstanceRoot() || sourceIds.stream().noneMatch(id -> BpmDispatchSources.contains(originValue, id))) continue;
            FlowElement element = model.getFlowElement(root.getActivityId());
            if (!(element instanceof UserTask)) continue;
            UserTask node = (UserTask) element;
            List<Long> members = members(root);
            List<ExecutionEntity> children = children(root);
            Map<String, Set<String>> origins = (Map<String, Set<String>>) originValue;
            // 旧会签缺少全员来源时，只允许有新内循环标记的本环节发送人撤回其精确派发。
            boolean trackedInternal = root.getVariableLocal(BpmInternalDispatch.HANDOFFS) instanceof Number
                    && node.getId().equals(source.getTaskDefinitionKey());
            boolean consistent = members.size() == number(root, "nrOfInstances")
                    && members.stream().allMatch(id -> id != null && (trackedInternal || origins.containsKey(id.toString())));
            int completed = number(root, "nrOfCompletedInstances");
            for (int index = 0; index < members.size(); index++) {
                Long user = members.get(index);
                if (user == null || Collections.disjoint(origins.getOrDefault(user.toString(), Collections.emptySet()), sourceIds)) continue;
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
        for (String sourceId : sourceIds) {
            Object value = runtimeService.getVariableLocal(source.getProcessInstanceId(), BpmDispatchSources.LEDGER_PREFIX + sourceId);
            if (value instanceof Map) ((Map<String, Set<Long>>) value).forEach((nodeId, users) -> {
                FlowElement element = model.getFlowElement(nodeId);
                for (Long user : users) {
                    if (result.stream().anyMatch(row -> row.getNodeId().equals(nodeId) && row.getUserId().equals(user))) continue;
                    BpmTaskWithdrawTargetRespVO row = new BpmTaskWithdrawTargetRespVO();
                    row.setExecutionId(""); row.setNodeId(nodeId); row.setNodeName(element == null || element.getName() == null ? nodeId : element.getName());
                    row.setUserId(user); row.setDisabledReason("目标已结束或尚未生成可撤回的会签实例"); result.add(row);
                }
            });
        }
        return result;
    }

    /** 仅聚合有明确恢复关系的本人同节点已办，不把同名节点、相邻时间或其他办理人的任务并入。 */
    private Map<String, HistoricTaskInstance> withdrawalSources(HistoricTaskInstance source) {
        Map<String, HistoricTaskInstance> result = new LinkedHashMap<>();
        result.put(source.getId(), source);
        if (!partialAllowed(source)) return result;
        List<HistoricTaskInstance> candidates = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(source.getProcessInstanceId()).taskDefinitionKey(source.getTaskDefinitionKey())
                .includeTaskLocalVariables().list().stream()
                .filter(task -> task.getEndTime() != null && Objects.equals(source.getAssignee(), task.getAssignee())
                        && BpmTaskStatusEnum.APPROVE.getStatus().equals(task.getTaskLocalVariables()
                        .get(BpmnVariableConstants.TASK_VARIABLE_STATUS))).collect(Collectors.toList());
        Set<String> related = sourceLineage(source);
        boolean expanded;
        do {
            expanded = false;
            for (HistoricTaskInstance task : candidates) {
                if (result.containsKey(task.getId())) continue;
                Set<String> lineage = sourceLineage(task);
                if (Collections.disjoint(related, lineage)) continue;
                result.put(task.getId(), task);
                related.addAll(lineage);
                expanded = true;
            }
        } while (expanded);
        return result;
    }

    private Set<String> sourceLineage(HistoricTaskInstance task) {
        Set<String> result = new LinkedHashSet<>();
        result.add(task.getId());
        Object value = task.getTaskLocalVariables().get(BpmDispatchSources.RESTORED_FROM);
        if (value instanceof Collection) ((Collection<?>) value).forEach(id -> result.add(id.toString()));
        return result;
    }

    /** 按流程实例启动时的固定版本分流，新版失败不降级。 */
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
        return BpmProcessOperationVersion.isCurrent(runtimeService.getVariableLocal(source.getProcessInstanceId(), BpmProcessOperationVersion.KEY));
    }

    private void requireMemberMode(HistoricTaskInstance source) {
        if (!memberMode(source)) throw exception(TASK_WITHDRAW_INVALID, "该任务使用原版整体撤回，请刷新后重新操作");
    }

    /** 按流程实例的固定版本进入原规则；提交时重新核验并锁定流程。 */
    @Transactional(rollbackFor = Exception.class)
    public void executeLegacy(Long userId, String taskId, Runnable operation) {
        managementService.executeCommand(context -> {
            HistoricTaskInstance source = source(userId, taskId);
            if (memberMode(source)) throw exception(TASK_WITHDRAW_INVALID, "该任务已接入新版派发，请刷新后重新确认撤回方式");
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
            boolean director = partialAllowed(source);
            if (!director && process.getVariableLocal(RESTORED + source.getId()) != null)
                throw exception(TASK_WITHDRAW_INVALID, "该来源已撤回并恢复待办，请刷新");
            requireMemberMode(source);
            Map<String, HistoricTaskInstance> sources = withdrawalSources(source);
            Set<String> sourceIds = sources.keySet();
            if (sourceIds.stream().noneMatch(id -> process.getVariableLocal(BpmDispatchSources.LEDGER_PREFIX + id) instanceof Map))
                throw exception(TASK_WITHDRAW_INVALID, "新版派发记录尚未完整生成，请稍后重试，不能降级为原版撤回");
            boolean pending = runtimeService.createVariableInstanceQuery().processInstanceId(source.getProcessInstanceId())
                    .excludeTaskVariables().variableNameLike("bpm_dispatch_pending_%").list().stream()
                    .anyMatch(variable -> variable.getValue() instanceof Collection
                            && !Collections.disjoint((Collection<?>) variable.getValue(), sourceIds));
            if (pending) throw exception(TASK_WITHDRAW_INVALID, "本次派发尚有异步任务生成中，请稍后重试");
            List<BpmTaskWithdrawTargetRespVO> allowed = inspect(source, sourceIds);
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
            Map<String, List<BpmTaskWithdrawTargetRespVO>> chosenBySource = new LinkedHashMap<>();
            for (BpmTaskWithdrawReqVO.Member member : request.getMembers()) {
                if (member == null || member.getUserId() == null || member.getExecutionId() == null)
                    throw exception(TASK_WITHDRAW_INVALID, "撤回目标不能为空");
                BpmTaskWithdrawTargetRespVO row = allowed.stream().filter(r -> r.isWithdrawable()
                        && member.getExecutionId().equals(r.getExecutionId()) && member.getUserId().equals(r.getUserId())).findFirst()
                        .orElseThrow(() -> exception(TASK_WITHDRAW_INVALID, "成员已办理或不属于本次派发，请刷新后重试"));
                if (!selected.computeIfAbsent(member.getExecutionId(), key -> new LinkedHashSet<>()).add(member.getUserId()))
                    throw exception(TASK_WITHDRAW_INVALID, "不能重复选择同一成员");
                chosen.add(row);
                ExecutionEntity root = CommandContextUtil.getExecutionEntityManager().findById(row.getExecutionId());
                @SuppressWarnings("unchecked")
                Map<String, Set<String>> origins = (Map<String, Set<String>>) root.getVariableLocal(BpmDispatchSources.SOURCES);
                for (String id : origins.getOrDefault(row.getUserId().toString(), Collections.emptySet())) {
                    if (!sourceIds.contains(id)) continue;
                    Object ledger = process.getVariableLocal(BpmDispatchSources.LEDGER_PREFIX + id);
                    Object recorded = ledger instanceof Map ? ((Map<?, ?>) ledger).get(row.getNodeId()) : null;
                    if (!(recorded instanceof Collection) || !((Collection<?>) recorded).contains(row.getUserId()))
                        throw exception(TASK_WITHDRAW_INVALID, "派发来源记录不完整，请刷新后重试");
                    chosenBySource.computeIfAbsent(id, key -> new ArrayList<>()).add(row);
                }
            }
            // 全批校验后再修改；一个无效成员使整个撤回回滚。
            // 主任每次成功撤回都回到拟办；先恢复，避免最后一个目标结束时提前结束流程。
            if (!director || sources.values().stream().noneMatch(related -> hasRestoredSourceTask(related, process)))
                restoreSource(source, process);
            List<ExecutionEntity> emptyRoots = new ArrayList<>();
            for (Map.Entry<String, Set<Long>> entry : selected.entrySet()) {
                ExecutionEntity root = CommandContextUtil.getExecutionEntityManager().findById(entry.getKey());
                removeMembers(sourceIds, root, entry.getValue(), chosen, emptyRoots);
            }
            Object remaining = process.getVariableLocal(BpmDispatchSources.LEDGER_PREFIX + source.getId());
            if (!director && !((Map<?, ?>) remaining).isEmpty())
                throw exception(TASK_WITHDRAW_INVALID, "派发记录未完整撤回，请刷新重试");
            // 先恢复来源再结束空目标，避免最后一个目标的取消误结束整个流程。
            for (ExecutionEntity root : emptyRoots) cancelRoot(root);
            chosenBySource.forEach((id, rows) -> writeComment(sources.get(id), userId, request.getReason(), rows));
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void removeMembers(Set<String> sourceIds, ExecutionEntity root, Set<Long> requested,
                               List<BpmTaskWithdrawTargetRespVO> chosen, List<ExecutionEntity> emptyRoots) {
        root.forceUpdate();
        UserTask node = (UserTask) root.getCurrentFlowElement();
        if (!(node.getBehavior() instanceof BpmParallelMultiInstanceBehavior) && !(node.getBehavior() instanceof BpmSequentialMultiInstanceBehavior))
            throw exception(TASK_WITHDRAW_INVALID, "当前目标不支持精确会签撤回");
        Map<String, Set<String>> origins = new LinkedHashMap<>((Map<String, Set<String>>) root.getVariableLocal(BpmDispatchSources.SOURCES));
        Set<Long> removed = new LinkedHashSet<>();
        for (Long user : requested) {
            Set<String> sources = new LinkedHashSet<>(origins.get(user.toString()));
            Set<String> removedSources = new LinkedHashSet<>(sources); removedSources.retainAll(sourceIds);
            sources.removeAll(removedSources);
            if (sources.isEmpty()) { origins.remove(user.toString()); removed.add(user); }
            else origins.put(user.toString(), sources);
            for (String id : removedSources) BpmDispatchSources.forget(root, id, node.getId(), user);
        }
        root.setVariableLocal(BpmDispatchSources.SOURCES, origins);
        if (removed.isEmpty()) return;
        int completed = number(root, "nrOfCompletedInstances");
        List<Long> members = members(root); members.removeAll(removed);
        ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(root.getProcessInstanceId());
        Object retained = process.getVariableLocal(BpmTaskReturnService.RETAINED_PREFIX + root.getId());
        if (retained instanceof Collection) {
            Set<Object> remainingReturn = new LinkedHashSet<>((Collection<?>) retained); remainingReturn.removeAll(removed);
            process.setVariableLocal(BpmTaskReturnService.RETAINED_PREFIX + root.getId(), remainingReturn);
        }
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
        BpmInternalDispatch.removeContext(root, node.getId(), removed);
        if (members.isEmpty()) { emptyRoots.add(root); return; }
        MultiInstanceActivityBehavior behavior = (MultiInstanceActivityBehavior) node.getBehavior();
        Object returnedValue = root.getVariableLocal(BpmTaskReturnService.RETURNED);
        int returned = returnedValue instanceof Number ? ((Number) returnedValue).intValue() : 0;
        Object handoffs = root.getVariableLocal(BpmInternalDispatch.HANDOFFS);
        returned += handoffs instanceof Number ? ((Number) handoffs).intValue() : 0;
        boolean finished = completed > 0 && (completed == members.size()
                || completed > returned && behavior.completionConditionSatisfied(root));
        if (finished && completed == returned) {
            emptyRoots.add(root);
        } else if (finished) {
            if (behavior instanceof BpmParallelMultiInstanceBehavior) ((BpmParallelMultiInstanceBehavior) behavior).completeAfterWithdrawal(root);
            else ((BpmSequentialMultiInstanceBehavior) behavior).completeAfterWithdrawal(root);
        } else if (node.getLoopCharacteristics().isSequential() && removedCurrent) {
            ExecutionEntity next = CommandContextUtil.getExecutionEntityManager().createChildExecution(root);
            next.setCurrentFlowElement(node);
            // 已办前缀保留，当前/排队撤回后从相同下标继续，不把撤回算成通过。
            ((BpmSequentialMultiInstanceBehavior) behavior).continueSequentialMultiInstance(next, completed, root);
        }
    }

    private boolean hasRestoredSourceTask(HistoricTaskInstance source, ExecutionEntity process) {
        Object previousBranch = process.getVariableLocal(RESTORED + source.getId());
        for (Task task : flowableTaskService.createTaskQuery().processInstanceId(source.getProcessInstanceId())
                .taskDefinitionKey(source.getTaskDefinitionKey()).includeTaskLocalVariables().list()) {
            Object lineage = task.getTaskLocalVariables().get(BpmDispatchSources.RESTORED_FROM);
            if (lineage instanceof Collection && ((Collection<?>) lineage).contains(source.getId())) return true;
            // 兼容本次修改前已恢复、尚无来源链的待办。
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(task.getExecutionId());
            while (execution != null && !execution.isProcessInstanceType()) {
                if (execution.getId().equals(previousBranch)) return true;
                execution = execution.getParent();
            }
        }
        return false;
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
        branch.setVariableLocal(BpmRestoredOpinion.KEY, BpmRestoredOpinion.draft(source));
        if (source.getTaskLocalVariables().get(BpmInternalDispatch.SOURCE) != null) {
            BpmInternalDispatch.prepareContext(branch, node.getId(), Long.valueOf(source.getAssignee()),
                    Collections.singletonMap(BpmInternalDispatch.SOURCE, source.getTaskLocalVariables().get(BpmInternalDispatch.SOURCE)));
        }
        Set<String> lineage = new LinkedHashSet<>();
        Object previous = source.getTaskLocalVariables().get(BpmDispatchSources.RESTORED_FROM);
        if (previous instanceof Collection) ((Collection<?>) previous).forEach(id -> lineage.add(id.toString()));
        lineage.add(source.getId());
        // 原来源在恢复任务重新发送后仍可继续撤回。带上已产生的恢复轮次，避免漏掉那些轮次的保留派发。
        Set<String> ancestors = new LinkedHashSet<>(lineage);
        for (HistoricTaskInstance related : historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(source.getProcessInstanceId()).taskDefinitionKey(source.getTaskDefinitionKey())
                .includeTaskLocalVariables().list()) {
            Object context = related.getTaskLocalVariables().get(BpmDispatchSources.RESTORED_FROM);
            if (context instanceof Collection && ((Collection<?>) context).stream().anyMatch(ancestors::contains)) {
                lineage.add(related.getId());
                ((Collection<?>) context).forEach(id -> lineage.add(id.toString()));
            }
        }
        branch.setVariableLocal(BpmDispatchSources.RESTORED_FROM,
                Collections.singletonMap(source.getTaskDefinitionKey(), lineage));
        // 恢复原任务的退回路径上下文；不继承流程级其他分支的路径。
        for (String key : Arrays.asList("RETURN_BRANCH_CONTEXT", "RETURNABLE_TASK_PATH", BpmTaskReturnService.SCOPE, BpmTaskReturnService.RETAINED_ROOTS, BpmTaskReturnService.RETAINED_SOURCES)) {
            Object value = source.getTaskLocalVariables().get(key);
            branch.setVariableLocal(key, value instanceof Map ? new LinkedHashMap<>((Map<?, ?>) value)
                    : value instanceof Collection ? new ArrayList<>((Collection<?>) value) : null);
        }
        // 同环节内循环撤回后，原会签的其他成员仍属于原实例，恢复任务不能重复派发给他们。
        for (ExecutionEntity original : CommandContextUtil.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(source.getProcessInstanceId())) {
            if (!original.isMultiInstanceRoot() || !node.getId().equals(original.getActivityId())
                    || !BpmDispatchSources.contains(original.getVariableLocal(BpmDispatchSources.SOURCES), source.getId())) continue;
            Map<String, String> retainedRoots = new LinkedHashMap<>();
            Object inherited = branch.getVariableLocal(BpmTaskReturnService.RETAINED_ROOTS);
            if (inherited instanceof Map) ((Map<?, ?>) inherited).forEach((key, value) -> retainedRoots.put(key.toString(), value.toString()));
            retainedRoots.put(node.getId(), original.getId());
            branch.setVariableLocal(BpmTaskReturnService.RETAINED_ROOTS, retainedRoots);
            Object existing = process.getVariableLocal(BpmTaskReturnService.RETAINED_PREFIX + original.getId());
            Set<Long> retained = new LinkedHashSet<>();
            if (existing instanceof Collection) ((Collection<?>) existing).forEach(id -> retained.add(Long.valueOf(id.toString())));
            else retained.addAll(members(original));
            retained.remove(Long.valueOf(source.getAssignee()));
            process.setVariableLocal(BpmTaskReturnService.RETAINED_PREFIX + original.getId(), retained);
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
