package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.bpmn.model.UserTask;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPEND_INVALID;

/** 内循环按具体来源任务留痕；与普通派发共用成员来源账本。 */
public final class BpmInternalDispatch {
    public static final String SOURCE = "internal_source_task_id";
    public static final String META = "bpm_dispatch_internal_context";
    public static final String HANDOFFS = "bpm_dispatch_internal_handoffs";
    public static final String PENDING = "bpm_dispatch_internal_completing";
    private BpmInternalDispatch() {}

    @SuppressWarnings("unchecked")
    public static void prepareContext(DelegateExecution scope, String node, Long user, Map<String, Object> context) {
        Object previous = scope.getVariableLocal(META);
        Map<String, Map<String, Map<String, Object>>> all = previous instanceof Map ? new LinkedHashMap<>((Map<String, Map<String, Map<String, Object>>>) previous) : new LinkedHashMap<>();
        Map<String, Map<String, Object>> users = new LinkedHashMap<>(all.getOrDefault(node, Collections.emptyMap()));
        users.put(user.toString(), new LinkedHashMap<>(context)); all.put(node, users); scope.setVariableLocal(META, all);
    }

    @SuppressWarnings("unchecked")
    public static void capture(DelegateExecution execution, TaskEntity task, Long user) {
        if (user == null) return;
        ExecutionEntity owner = (ExecutionEntity) execution;
        while (owner != null && !owner.isProcessInstanceType()) {
            Object value = owner.getVariableLocal(META);
            Map<?, ?> nodes = value instanceof Map ? (Map<?, ?>) value : Collections.emptyMap();
            Object users = nodes.get(execution.getCurrentActivityId());
            Object context = users instanceof Map ? ((Map<?, ?>) users).get(user.toString()) : null;
            if (context instanceof Map) {
                ((Map<String, Object>) context).forEach((key, item) -> {
                    if (item == null) return;
                    task.setVariableLocal(key, item);
                    if (!SOURCE.equals(key)) execution.setVariableLocal(key, item);
                });
                Map<String, Map<String, Map<String, Object>>> updated = new LinkedHashMap<>((Map<String, Map<String, Map<String, Object>>>) value);
                Map<String, Map<String, Object>> rest = new LinkedHashMap<>((Map<String, Map<String, Object>>) users);
                rest.remove(user.toString()); updated.put(execution.getCurrentActivityId(), rest); owner.setVariableLocal(META, updated);
                return;
            }
            owner = owner.getParent();
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeContext(DelegateExecution root, String node, Collection<Long> removed) {
        Object value = root.getVariableLocal(META);
        if (!(value instanceof Map)) return;
        Map<String, Map<String, Map<String, Object>>> all = new LinkedHashMap<>((Map<String, Map<String, Map<String, Object>>>) value);
        Map<String, Map<String, Object>> users = new LinkedHashMap<>(all.getOrDefault(node, Collections.emptyMap()));
        removed.forEach(id -> users.remove(id.toString()));
        if (users.isEmpty()) all.remove(node); else all.put(node, users);
        if (all.isEmpty()) root.removeVariableLocal(META); else root.setVariableLocal(META, all);
    }

    @SuppressWarnings("unchecked")
    public static void add(ManagementService management, RuntimeService runtime, Task source, String rootId,
                           Collection<Long> requested, Boolean sendSms, Map<String, Object> context) {
        management.executeCommand(command -> {
            ExecutionEntity root = CommandContextUtil.getExecutionEntityManager().findById(rootId);
            if (root == null || !root.isMultiInstanceRoot()) throw exception(TASK_APPEND_INVALID, "内循环会签实例已变化");
            root.forceUpdate();
            UserTask node = (UserTask) root.getCurrentFlowElement();
            String key = FlowableUtils.formatExecutionCollectionVariable(node.getId());
            Object value = root.getVariableLocal(key);
            if (!(value instanceof Collection)) throw exception(TASK_APPEND_INVALID, "内循环成员集合不完整");
            Set<Long> members = new LinkedHashSet<>(); ((Collection<?>) value).forEach(id -> members.add(Long.valueOf(id.toString())));
            Set<Long> added = new LinkedHashSet<>(requested);
            if (added.size() != requested.size() || added.isEmpty() || added.stream().anyMatch(members::contains))
                throw exception(TASK_APPEND_INVALID, "所选人员已参与本轮办理，不能重复派发；需要重办请使用退回");
            int count = ((Number) root.getVariableLocal("nrOfInstances")).intValue();
            if (members.size() != count) throw exception(TASK_APPEND_INVALID, "内循环成员计数不一致，请先核查历史数据");
            Object originsValue = root.getVariableLocal(BpmDispatchSources.SOURCES);
            Map<String, Set<String>> origins = originsValue instanceof Map ? new LinkedHashMap<>((Map<String, Set<String>>) originsValue) : new LinkedHashMap<>();
            Map<String, Object> snapshot = new LinkedHashMap<>(context); snapshot.put(SOURCE, source.getId());
            for (Long id : added) {
                origins.put(id.toString(), Collections.singleton(source.getId())); prepareContext(root, node.getId(), id, snapshot);
            }
            members.addAll(added); root.setVariableLocal(key, members); root.setVariableLocal("nrOfInstances", members.size());
            root.setVariableLocal(BpmDispatchSources.SOURCES, origins);
            BpmDispatchSources.remember(root, source.getId(), node.getId(), added);
            BpmTaskSmsPolicy.prepare(runtime, rootId, Collections.singletonMap(node.getId(), added), sendSms);
            Object old = root.getVariableLocal(HANDOFFS);
            root.setVariableLocal(HANDOFFS, (old instanceof Number ? ((Number) old).intValue() : 0) + 1);
            root.setVariableLocal(PENDING, true);
            if (!node.getLoopCharacteristics().isSequential()) {
                for (Long id : added) {
                    ExecutionEntity child = CommandContextUtil.getExecutionEntityManager().createChildExecution(root);
                    child.setCurrentFlowElement(node);
                    child.setVariableLocal(FlowableUtils.formatExecutionCollectionElementVariable(node.getId()), id);
                    CommandContextUtil.getAgenda().planContinueMultiInstanceOperation(child, root, count++);
                }
            }
            return null;
        });
    }
}
