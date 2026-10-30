package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.ManagementService;
import org.flowable.bpmn.model.*;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.condition.ConditionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import java.util.*;

/** 来源关联按目标成员传递，在目标实例初始化时消费；不以节点名或时间推测来源。 */
public final class BpmDispatchSources {
    public static final String SOURCES = "bpm_dispatch_sources";
    public static final String OPTIONS = "bpm_dispatch_options";
    public static final String BRANCH = "bpm_dispatch_branch";
    public static final String NEW_MEMBERS = "bpm_dispatch_new_members";
    public static final String LEDGER_PREFIX = "bpm_dispatch_ledger_";
    /** 恢复分支保存节点到来源链的映射；对应来源任务上保存链快照，不能从表单传入。 */
    public static final String RESTORED_FROM = "bpm_dispatch_restored_from";
    private static final java.util.regex.Pattern MANUAL_ROUTE = java.util.regex.Pattern.compile(
            "\\$\\{\\s*(?:variables:get\\(\\s*['\"]?(?:select_node_\\d+|next_node\\d+)['\"]?\\s*\\)|(?:select_node_\\d+|next_node\\d+))\\s*==\\s*['\"]([^'\"]+)['\"]\\s*}");
    private static final String PREFIX = "bpm_dispatch_pending_";
    private BpmDispatchSources() {}

    public static Set<String> restoredSources(Object context, String nodeId) {
        Set<String> result = new LinkedHashSet<>();
        Object sources = context instanceof Map ? ((Map<?, ?>) context).get(nodeId) : null;
        if (sources instanceof Collection) ((Collection<?>) sources).forEach(id -> result.add(id.toString()));
        return result;
    }

    /** 恢复的来源重新选人时，未撤销的旧派发（包含已办）仍属已接收；已撤销人员可重新选择。 */
    public static Map<String, Set<Long>> retainedRecipients(RuntimeService runtime, String instanceId,
                                                           String executionId, String nodeId) {
        Map<String, Set<Long>> result = new LinkedHashMap<>();
        if (executionId == null) return result;
        Set<String> sources = restoredSources(runtime.getVariable(executionId, RESTORED_FROM), nodeId);
        for (String source : sources) {
            Object ledger = runtime.getVariableLocal(instanceId, LEDGER_PREFIX + source);
            if (!(ledger instanceof Map)) continue;
            ((Map<?, ?>) ledger).forEach((target, users) -> {
                if (!(users instanceof Collection)) return;
                Set<Long> members = result.computeIfAbsent(target.toString(), key -> new LinkedHashSet<>());
                for (Object user : (Collection<?>) users) members.add(Long.valueOf(user.toString()));
            });
        }
        return result;
    }
    public static boolean isInternal(String key) {
        return key != null && key.startsWith("bpm_dispatch_");
    }
    /** 不允许业务表单携带内部来源标记；复制后过滤，避免改动调用方原始变量。 */
    public static Map<String, Object> businessVariables(Map<String, Object> input) {
        Map<String, Object> result = input == null ? new HashMap<>() : new HashMap<>(input);
        result.keySet().removeIf(BpmDispatchSources::isInternal);
        return result;
    }
    private static String key(String node, Object user) {
        return PREFIX + node.length() + "_" + node + "_" + user;
    }

    /** 仅撤销尚未消费的精确来源，保留其他会签成员对相同目标/人员的选择。 */
    public static void withdrawPending(org.flowable.engine.impl.persistence.entity.ExecutionEntity scope,
                                       String sourceTaskId, UserTask sourceNode) {
        Map<String, Set<String>> removed = new LinkedHashMap<>();
        Map<String, Set<String>> retained = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : new LinkedHashMap<>(scope.getVariablesLocal()).entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith(PREFIX) || !(entry.getValue() instanceof Collection)) continue;
            int separator = name.indexOf('_', PREFIX.length());
            if (separator < 0) continue;
            int length = Integer.parseInt(name.substring(PREFIX.length(), separator));
            String node = name.substring(separator + 1, separator + 1 + length);
            String user = name.substring(separator + 2 + length);
            Set<String> sources = new LinkedHashSet<>();
            ((Collection<?>) entry.getValue()).forEach(id -> sources.add(id.toString()));
            if (sources.remove(sourceTaskId)) {
                if (sources.isEmpty()) {
                    scope.removeVariableLocal(name);
                    scope.removeVariableLocal("bpm_task_sms_pending_" + length + "_" + node + "_" + user);
                    removed.computeIfAbsent(node, ignored -> new LinkedHashSet<>()).add(user);
                } else scope.setVariableLocal(name, sources);
            }
            if (!sources.isEmpty()) retained.computeIfAbsent(node, ignored -> new LinkedHashSet<>()).add(user);
        }
        String selectionsKey = cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants
                .PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES;
        Object value = scope.getVariableLocal(selectionsKey);
        if (value instanceof Map) {
            Map<String, Object> selections = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((node, users) -> selections.put(node.toString(), users));
            removed.forEach((node, users) -> {
                Object selected = selections.get(node);
                if (!(selected instanceof Collection)) return;
                List<Object> remaining = new ArrayList<>((Collection<?>) selected);
                remaining.removeIf(user -> users.contains(user.toString())
                        || users.contains("*") && !retained.getOrDefault(node, Collections.emptySet()).contains(user.toString()));
                if (remaining.isEmpty()) selections.remove(node); else selections.put(node, remaining);
            });
            scope.setVariableLocal(selectionsKey, selections);
        }
        // 只重置当前节点的人工出线，业务条件及其他分支变量保持原值。
        Map<String, Set<String>> routeTargets = new LinkedHashMap<>();
        for (SequenceFlow flow : sourceNode.getOutgoingFlows()) {
            if (flow.getConditionExpression() == null) continue;
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(?:select_node_\\d+|next_node\\d+)\\b")
                    .matcher(flow.getConditionExpression());
            Set<String> targets = new LinkedHashSet<>();
            collectUnconditionalTargets(flow.getTargetFlowElement(), targets, new HashSet<>());
            while (matcher.find()) routeTargets.computeIfAbsent(matcher.group(), ignored -> new LinkedHashSet<>()).addAll(targets);
        }
        routeTargets.forEach((route, targets) -> {
            if (!Collections.disjoint(targets, removed.keySet()) && Collections.disjoint(targets, retained.keySet()))
                scope.setVariableLocal(route, null);
        });
    }
    public static void prepare(RuntimeService runtime, String executionId, String sourceTaskId,
                               Map<String, ? extends Collection<Long>> recipients) {
        if (recipients == null) return;
        recipients.forEach((node, users) -> {
            if (users == null) return;
            for (Long user : users) {
                String key = key(node, user);
                Object previous = runtime.getVariableLocal(executionId, key);
                Set<String> sources = new LinkedHashSet<>();
                if (previous instanceof Collection) for (Object value : (Collection<?>) previous) sources.add(value.toString());
                sources.add(sourceTaskId);
                runtime.setVariableLocal(executionId, key, sources);
            }
        });
    }
    /** 仅沿本次条件成立的出线记录来源，不为未走分支留下待消费的来源标记。 */
    public static Set<String> selectedTargets(ManagementService management, String executionId, FlowNode source) {
        return management.executeCommand(context -> {
            DelegateExecution execution = CommandContextUtil.getExecutionEntityManager().findById(executionId);
            Set<String> result = new LinkedHashSet<>();
            collect(source, execution, result, new HashSet<>(), false);
            return result;
        });
    }
    private static void collect(FlowNode node, DelegateExecution execution, Set<String> result, Set<String> visited, boolean appendOptions) {
        if (!visited.add(node.getId())) return;
        String defaultId = node instanceof Activity ? ((Activity) node).getDefaultFlow()
                : node instanceof Gateway ? ((Gateway) node).getDefaultFlow() : null;
        List<SequenceFlow> selected = new ArrayList<>();
        SequenceFlow fallback = null;
        for (SequenceFlow flow : node.getOutgoingFlows()) {
            if (Objects.equals(defaultId, flow.getId())) { fallback = flow; continue; }
            if (node instanceof ParallelGateway
                    || (appendOptions && node instanceof UserTask && isOptionalManualRoute(flow))
                    || ConditionUtil.hasTrueCondition(flow, execution)) {
                selected.add(flow);
                if (node instanceof ExclusiveGateway) break;
            }
        }
        if (selected.isEmpty() && fallback != null) selected.add(fallback);
        for (SequenceFlow flow : selected) {
            FlowElement target = flow.getTargetFlowElement();
            if (target instanceof UserTask) result.add(target.getId());
            else if (target instanceof Gateway) collect((FlowNode) target, execution, result, visited, appendOptions);
        }
    }

    /** 人工可重新选择的简单流向；发送选项不应被上次选择或撤回留下的空值过滤。 */
    public static boolean isOptionalManualRoute(SequenceFlow flow) {
        if (flow.getConditionExpression() == null) return false;
        java.util.regex.Matcher matcher = MANUAL_ROUTE.matcher(flow.getConditionExpression().trim());
        // 上诉属于新的业务阶段，不能通过追加绕过原结果选择。
        return matcher.matches() && !"appeal".equals(matcher.group(1));
    }

    /** 只补充模型明确声明的人工选择出线，不重新评估任何业务条件。 */
    public static Set<String> optionalManualTargets(FlowNode source) {
        Set<String> result = new LinkedHashSet<>();
        for (SequenceFlow flow : source.getOutgoingFlows()) {
            if (isOptionalManualRoute(flow)) collectUnconditionalTargets(flow.getTargetFlowElement(), result, new HashSet<>());
        }
        return result;
    }

    private static void collectUnconditionalTargets(FlowElement element, Set<String> result, Set<String> visited) {
        if (element == null || !visited.add(element.getId())) return;
        if (element instanceof UserTask) {
            result.add(element.getId());
        } else if (element instanceof Gateway && !(element instanceof ExclusiveGateway)) {
            for (SequenceFlow flow : ((Gateway) element).getOutgoingFlows()) {
                if (flow.getConditionExpression() == null || flow.getConditionExpression().trim().isEmpty()) {
                    collectUnconditionalTargets(flow.getTargetFlowElement(), result, visited);
                }
            }
        }
    }

    /** 在原办理事务中冻结可追加目标；后续不再用变化后的全局业务变量重新判路由。 */
    public static void recordOptions(ManagementService management, org.flowable.engine.TaskService tasks,
                                     String taskId, String executionId, FlowNode source) {
        Set<String> options = management.executeCommand(context -> {
            DelegateExecution execution = CommandContextUtil.getExecutionEntityManager().findById(executionId);
            Set<String> result = new LinkedHashSet<>();
            collect(source, execution, result, new HashSet<>(), true);
            return result;
        });
        tasks.setVariableLocal(taskId, OPTIONS, options);
    }

    /** 每个已办来源独立保存实际派发人员，目标结束后仍可防止重复追加。 */
    public static void remember(DelegateExecution execution, String sourceTaskId, String nodeId, Collection<Long> users) {
        org.flowable.engine.impl.persistence.entity.ExecutionEntity process =
                CommandContextUtil.getExecutionEntityManager().findById(execution.getProcessInstanceId());
        String key = LEDGER_PREFIX + sourceTaskId;
        @SuppressWarnings("unchecked")
        Map<String, Set<Long>> previous = (Map<String, Set<Long>>) process.getVariableLocal(key);
        Map<String, Set<Long>> ledger = previous == null ? new LinkedHashMap<>() : new LinkedHashMap<>(previous);
        Set<Long> members = new LinkedHashSet<>(ledger.getOrDefault(nodeId, Collections.emptySet()));
        members.addAll(users); ledger.put(nodeId, members);
        process.forceUpdate(); process.setVariableLocal(key, ledger);
    }

    /** 只移除已验证仍未完成的派发关系；正常完成的账本记录永远保留。 */
    @SuppressWarnings("unchecked")
    public static void forget(DelegateExecution execution, String sourceTaskId, String nodeId, Long userId) {
        org.flowable.engine.impl.persistence.entity.ExecutionEntity process =
                CommandContextUtil.getExecutionEntityManager().findById(execution.getProcessInstanceId());
        String key = LEDGER_PREFIX + sourceTaskId;
        Map<String, Set<Long>> previous = (Map<String, Set<Long>>) process.getVariableLocal(key);
        Map<String, Set<Long>> ledger = previous == null ? new LinkedHashMap<>() : new LinkedHashMap<>(previous);
        Set<Long> users = new LinkedHashSet<>(ledger.getOrDefault(nodeId, Collections.emptySet()));
        users.remove(userId);
        if (users.isEmpty()) ledger.remove(nodeId); else ledger.put(nodeId, users);
        process.forceUpdate(); process.setVariableLocal(key, ledger);
    }

    /** 新分支的指定名单只消费一次，不沿用流程级残留的会签集合。 */
    @SuppressWarnings("unchecked")
    public static Set<Long> takeNewMembers(DelegateExecution execution) {
        Object pending = execution.getVariable(NEW_MEMBERS);
        if (!(pending instanceof Map)) return null;
        Map<String, Collection<Long>> map = (Map<String, Collection<Long>>) pending;
        Collection<Long> users = map.get(execution.getCurrentActivityId());
        if (users == null) return null;
        Map<String, Collection<Long>> remaining = new LinkedHashMap<>(map);
        remaining.remove(execution.getCurrentActivityId());
        if (remaining.isEmpty()) execution.removeVariable(NEW_MEMBERS);
        else execution.setVariable(NEW_MEMBERS, remaining);
        return new LinkedHashSet<>(users);
    }

    public static void prepareDefault(RuntimeService runtime, String executionId, String sourceTaskId, String node) {
        String key = key(node, "*");
        Object previous = runtime.getVariableLocal(executionId, key);
        Set<String> sources = new LinkedHashSet<>();
        if (previous instanceof Collection) for (Object value : (Collection<?>) previous) sources.add(value.toString());
        sources.add(sourceTaskId);
        runtime.setVariableLocal(executionId, key, sources);
    }

    public static void capture(DelegateExecution execution, Collection<Long> users) {
        Map<String, Set<String>> members = new LinkedHashMap<>();
        String defaultKey = key(execution.getCurrentActivityId(), "*");
        Object defaults = execution.getVariable(defaultKey);
        for (Long user : users) {
            String key = key(execution.getCurrentActivityId(), user);
            Object pending = execution.hasVariable(key) ? execution.getVariable(key) : defaults;
            if (pending instanceof Collection) {
                Set<String> sources = new LinkedHashSet<>();
                for (Object source : (Collection<?>) pending) sources.add(source.toString());
                members.put(String.valueOf(user), sources);
                execution.removeVariable(key);
            }
        }
        execution.removeVariable(defaultKey);
        execution.setVariableLocal(SOURCES, members);
        members.forEach((user, sources) -> sources.forEach(source -> {
            if (!"null".equals(user)) remember(execution, source, execution.getCurrentActivityId(),
                    Collections.singleton(Long.valueOf(user)));
        }));
    }
    public static boolean contains(Object value, String taskId) {
        if (!(value instanceof Map)) return false;
        return ((Map<?, ?>) value).values().stream()
                .anyMatch(sources -> sources instanceof Collection && ((Collection<?>) sources).contains(taskId));
    }
}
