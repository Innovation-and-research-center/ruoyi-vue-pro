package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.bpmn.model.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.api.history.HistoricTaskInstance;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_WITHDRAW_INVALID;

/** 存量派发的只读解析。只接受任务级批次/直接路径证据，或无歧义的唯一历史来源。 */
public final class BpmLegacyDispatchResolver {
    private static final String CONTEXT = "RETURN_BRANCH_CONTEXT";
    private static final String PATH = "RETURNABLE_TASK_PATH";
    private BpmLegacyDispatchResolver() {}

    public static class Snapshot {
        public final Map<String, List<Long>> members = new LinkedHashMap<>();
        public final Map<String, Map<String, Set<String>>> origins = new LinkedHashMap<>();
        public final Map<String, Set<Long>> ledger = new LinkedHashMap<>();
    }

    public static Snapshot resolve(HistoricTaskInstance source, BpmnModel model,
                                   List<ExecutionEntity> executions, HistoryService history) {
        Snapshot result = new Snapshot();
        // 旧来源没有完整派发账本，异步执行尚未落地时不能据当前任务推断派发已经全部生成。
        org.flowable.engine.ManagementService management = CommandContextUtil.getProcessEngineConfiguration().getManagementService();
        if (management.createJobQuery().processInstanceId(source.getProcessInstanceId()).count() > 0
                || management.createDeadLetterJobQuery().processInstanceId(source.getProcessInstanceId()).count() > 0)
            ambiguous("旧流程尚有异步任务待执行，请完成后再撤回");
        FlowElement element = model.getFlowElement(source.getTaskDefinitionKey());
        if (!(element instanceof UserTask) || ((UserTask) element).getSubProcess() != null) return result;
        Set<String> targetIds = new LinkedHashSet<>();
        next((FlowNode) element, targetIds, new HashSet<>());
        if (targetIds.isEmpty()) return result;
        List<HistoricTaskInstance> all = history.createHistoricTaskInstanceQuery()
                .processInstanceId(source.getProcessInstanceId()).includeTaskLocalVariables().list();
        String batch = batch(source, source.getTaskDefinitionKey());
        boolean uniqueSource = all.stream().filter(t -> source.getTaskDefinitionKey().equals(t.getTaskDefinitionKey())).count() == 1;
        boolean noJumps = all.stream().noneMatch(t -> t.getDeleteReason() != null
                || Arrays.asList(3, 4, 5).contains(t.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS)));
        for (String nodeId : targetIds) {
            FlowElement target = model.getFlowElement(nodeId);
            if (!(target instanceof UserTask) || ((UserTask) target).getLoopCharacteristics() == null) continue;
            List<HistoricTaskInstance> nodeHistory = all.stream().filter(t -> nodeId.equals(t.getTaskDefinitionKey())).collect(Collectors.toList());
            List<ExecutionEntity> roots = executions.stream().filter(e -> e.isMultiInstanceRoot() && nodeId.equals(e.getActivityId())).collect(Collectors.toList());
            // 比来源完成早创建的任务不可能是此次转出；不使用毫秒窗口近似匹配。
            List<HistoricTaskInstance> possible = nodeHistory.stream().filter(t -> !t.getCreateTime().before(source.getEndTime())).collect(Collectors.toList());
            if (possible.isEmpty()) {
                if (nodeHistory.isEmpty() && !roots.isEmpty()) ambiguous("目标尚未生成历史任务，请稍后重试");
                continue;
            }
            Set<String> predecessors = new LinkedHashSet<>(); previous((FlowNode) target, predecessors, new HashSet<>());
            boolean graphProvesSource = uniqueSource && noJumps && !predecessors.contains(nodeId)
                    && roots.size() <= 1 && all.stream().filter(t -> predecessors.contains(t.getTaskDefinitionKey()) && t.getEndTime() != null)
                    .allMatch(t -> t.getId().equals(source.getId()))
                    && nodeHistory.size() == possible.size();
            List<HistoricTaskInstance> matched = new ArrayList<>();
            for (HistoricTaskInstance task : possible) {
                String taskBatch = batch(task, source.getTaskDefinitionKey());
                String predecessor = predecessor(task);
                boolean exact = batch != null && batch.equals(taskBatch) && source.getTaskDefinitionKey().equals(predecessor);
                // 有明确的其他批次/直接前驱，不使用唯一来源兜底覆盖它。
                boolean other = batch != null && taskBatch != null && !batch.equals(taskBatch)
                        || predecessor != null && !source.getTaskDefinitionKey().equals(predecessor);
                if (exact || !other && graphProvesSource) matched.add(task);
                else if (!other) ambiguous("旧任务存在多个可能来源或缺少批次路径记录，不能自动撤回");
            }
            if (matched.isEmpty()) continue;
            if (matched.stream().map(HistoricTaskInstance::getAssignee).distinct().count() != matched.size())
                ambiguous("旧目标存在重复办理轮次，无法唯一确认原次派发");
            if (matched.stream().anyMatch(t -> t.getTaskLocalVariables().containsKey("internal_source_task_id") || t.getDeleteReason() != null))
                ambiguous("旧目标包含内循环、取消或退回记录，无法完整还原派发");
            Set<Long> sent = result.ledger.computeIfAbsent(nodeId, key -> new LinkedHashSet<>());
            for (HistoricTaskInstance task : matched) {
                if (task.getAssignee() == null) ambiguous("旧目标尚未分配办理人");
                sent.add(Long.valueOf(task.getAssignee()));
            }
            Set<String> matchedIds = matched.stream().map(HistoricTaskInstance::getId).collect(Collectors.toSet());
            Set<String> coveredActive = new HashSet<>();
            int associatedRoots = 0;
            for (ExecutionEntity root : roots) {
                List<ExecutionEntity> children = executions.stream().filter(e -> root.getId().equals(e.getParentId())).collect(Collectors.toList());
                Set<String> childIds = children.stream().map(ExecutionEntity::getId).collect(Collectors.toSet());
                List<HistoricTaskInstance> rootHistory = nodeHistory.stream().filter(t -> childIds.contains(t.getExecutionId())).collect(Collectors.toList());
                if (rootHistory.stream().noneMatch(t -> matchedIds.contains(t.getId()))) continue;
                if (++associatedRoots > 1 || rootHistory.isEmpty() || rootHistory.stream().anyMatch(t -> !matchedIds.contains(t.getId())))
                    ambiguous("同一目标存在多轮或混合来源，不能自动合并");
                Object existing = root.getVariableLocal(BpmDispatchSources.SOURCES);
                if (existing instanceof Map && !((Map<?, ?>) existing).isEmpty()) ambiguous("已有来源记录与旧记录混合，不能自动覆盖");
                List<Long> members = collection(root, roots.size() == 1);
                validateMembers((UserTask) target, root, members, rootHistory, children);
                Map<String, Set<String>> origins = new LinkedHashMap<>();
                for (Long user : members) origins.put(user.toString(), new LinkedHashSet<>(Collections.singleton(source.getId())));
                result.members.put(root.getId(), members); result.origins.put(root.getId(), origins);
                sent.addAll(members); // 包含串行排队但尚未生成任务的成员
                rootHistory.stream().filter(t -> t.getEndTime() == null).forEach(t -> coveredActive.add(t.getId()));
            }
            if (matched.stream().anyMatch(t -> t.getEndTime() == null && !coveredActive.contains(t.getId())))
                ambiguous("旧任务无法定位到唯一运行会签实例");
        }
        return result;
    }

    private static void validateMembers(UserTask node, ExecutionEntity root, List<Long> members,
                                        List<HistoricTaskInstance> tasks, List<ExecutionEntity> children) {
        int count = number(root, "nrOfInstances"), completed = number(root, "nrOfCompletedInstances");
        if (members.isEmpty() || members.contains(null) || new HashSet<>(members).size() != members.size() || members.size() != count)
            ambiguous("旧会签人员集合与计数不一致");
        Set<Long> actual = tasks.stream().map(t -> Long.valueOf(t.getAssignee())).collect(Collectors.toSet());
        long finished = tasks.stream().filter(t -> t.getEndTime() != null).count();
        if (finished != completed || tasks.stream().anyMatch(t -> t.getOwner() != null && !Objects.equals(t.getOwner(), t.getAssignee())))
            ambiguous("旧会签办理记录或转办信息不完整");
        if (!node.getLoopCharacteristics().isSequential()) {
            if (tasks.size() != count || !actual.equals(new HashSet<>(members))) ambiguous("旧并行任务与派发名单不一致");
        } else {
            if (tasks.size() != completed + 1 || completed >= count || !actual.equals(new HashSet<>(members.subList(0, completed + 1))))
                ambiguous("旧串行任务队列无法确认");
        }
        for (HistoricTaskInstance task : tasks) {
            if (task.getEndTime() != null) continue;
            ExecutionEntity child = children.stream().filter(e -> e.getId().equals(task.getExecutionId())).findFirst().orElse(null);
            Object user = child == null ? null : child.getVariableLocal(FlowableUtils.formatExecutionCollectionElementVariable(node.getId()));
            if (user == null || !task.getAssignee().equals(user.toString())) ambiguous("旧任务已转办或成员执行变量不完整");
            if (node.getLoopCharacteristics().isSequential() && !task.getAssignee().equals(members.get(completed).toString()))
                ambiguous("旧串行任务当前办理人与队列不一致");
        }
    }

    private static List<Long> collection(ExecutionEntity root, boolean uniqueRoot) {
        String key = FlowableUtils.formatExecutionCollectionVariable(root.getActivityId());
        Object value = root.getVariableLocal(key);
        // 旧行为曾把集合写在父作用域。只在没有同名运行实例时读取，再核验实际任务和计数。
        if (!(value instanceof Collection) && uniqueRoot) value = root.getVariable(key);
        if (!(value instanceof Collection)) ambiguous("缺少旧会签人员集合");
        return ((Collection<?>) value).stream().map(user -> user == null ? null : Long.valueOf(user.toString())).collect(Collectors.toList());
    }
    private static int number(ExecutionEntity root, String key) {
        Object value = root.getVariableLocal(key);
        if (!(value instanceof Number)) ambiguous("缺少旧会签人数");
        return ((Number) value).intValue();
    }
    private static String batch(HistoricTaskInstance task, String node) {
        Object context = task.getTaskLocalVariables().get(CONTEXT);
        Object value = context instanceof Map ? ((Map<?, ?>) context).get(node) : null;
        return value == null ? null : value.toString();
    }
    private static String predecessor(HistoricTaskInstance task) {
        Object value = task.getTaskLocalVariables().get(PATH);
        if (!(value instanceof List) || ((List<?>) value).isEmpty()) return null;
        List<?> path = (List<?>) value;
        int index = path.size() - 1;
        if (task.getTaskDefinitionKey().equals(path.get(index).toString())) index--;
        return index < 0 ? null : path.get(index).toString();
    }
    private static void next(FlowNode node, Set<String> targets, Set<String> visited) {
        if (!visited.add(node.getId())) return;
        for (SequenceFlow flow : node.getOutgoingFlows()) {
            FlowElement target = flow.getTargetFlowElement();
            if (target instanceof UserTask) targets.add(target.getId());
            else if (target instanceof Gateway) next((FlowNode) target, targets, visited);
        }
    }
    private static void previous(FlowNode node, Set<String> sources, Set<String> visited) {
        if (!visited.add(node.getId())) return;
        for (SequenceFlow flow : node.getIncomingFlows()) {
            FlowElement source = flow.getSourceFlowElement();
            if (source instanceof UserTask) sources.add(source.getId());
            else if (source instanceof Gateway) previous((FlowNode) source, sources, visited);
        }
    }
    private static void ambiguous(String message) { throw exception(TASK_WITHDRAW_INVALID, message); }

    /** 只在提交事务中使用；预览阶段不写运行时或历史变量。 */
    public static void install(HistoricTaskInstance source, Snapshot snapshot) {
        ExecutionEntity process = CommandContextUtil.getExecutionEntityManager().findById(source.getProcessInstanceId());
        process.forceUpdate();
        snapshot.origins.forEach((id, origins) -> {
            ExecutionEntity root = CommandContextUtil.getExecutionEntityManager().findById(id);
            root.forceUpdate(); root.setVariableLocal(BpmDispatchSources.SOURCES, origins);
            root.setVariableLocal(FlowableUtils.formatExecutionCollectionVariable(root.getActivityId()), new LinkedHashSet<>(snapshot.members.get(id)));
        });
        process.setVariableLocal(BpmDispatchSources.LEDGER_PREFIX + source.getId(), snapshot.ledger);
    }
}
