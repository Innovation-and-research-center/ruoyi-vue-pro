package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO.ActivityNode;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;

/** 一次恢复的意见草稿，只由目标执行流消费，不能传播到后续环节。 */
public final class BpmRestoredOpinion {
    public static final String KEY = "bpm_dispatch_restored_opinion";
    public static final String SOURCE_TASK = "bpm_dispatch_opinion_source_task";
    private BpmRestoredOpinion() {}

    /** 兼容已存在的恢复任务：仅使用持久化的来源集合，不按人、环节、时间猜测来源。 */
    public static void enrichView(List<HistoricTaskInstance> history, List<ActivityNode> nodes) {
        Map<String, HistoricTaskInstance> byId = history.stream().collect(Collectors.toMap(
                HistoricTaskInstance::getId, task -> task, (a, b) -> a));
        for (ActivityNode node : nodes) {
            if (node.getTasks() == null) continue;
            node.getTasks().forEach(view -> {
                HistoricTaskInstance current = byId.get(view.getId());
                if (current == null || current.getTaskLocalVariables() == null) return;
                Map<String, Object> variables = current.getTaskLocalVariables();
                Collection<?> candidates;
                Object explicit = variables.get(SOURCE_TASK);
                if (explicit instanceof String) candidates = Collections.singleton(explicit);
                else {
                    // 老数据只兼容可核验的内循环恢复：须同时有历史来源集合和相同的直接转入任务。
                    if (!(variables.get(BpmInternalDispatch.SOURCE) instanceof String)) return;
                    Object retained = variables.get("bpm_dispatch_return_sources");
                    Object sources = retained instanceof Map ? ((Map<?, ?>) retained).get(current.getTaskDefinitionKey()) : null;
                    if (!(sources instanceof Collection)) return;
                    candidates = (Collection<?>) sources;
                }
                List<HistoricTaskInstance> matches = candidates.stream().map(id -> byId.get(String.valueOf(id)))
                        .filter(Objects::nonNull).filter(source -> !source.getId().equals(current.getId())
                                && source.getEndTime() != null
                                && Objects.equals(source.getProcessInstanceId(), current.getProcessInstanceId())
                                && Objects.equals(source.getTaskDefinitionKey(), current.getTaskDefinitionKey())
                                && current.getAssignee() != null && current.getAssignee().equals(source.getAssignee())
                                && (explicit instanceof String || source.getTaskLocalVariables() != null
                                && Objects.equals(variables.get(BpmInternalDispatch.SOURCE),
                                        source.getTaskLocalVariables().get(BpmInternalDispatch.SOURCE))))
                        .collect(Collectors.toList());
                if (matches.size() != 1) return;
                HistoricTaskInstance source = matches.get(0);
                view.setRestoredFromTaskId(source.getId());
                if (current.getEndTime() == null && !variables.containsKey(BpmnVariableConstants.TASK_VARIABLE_REASON)
                        && source.getTaskLocalVariables() != null
                        && BpmTaskStatusEnum.APPROVE.getStatus().equals(source.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS))) {
                    Object reason = source.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_REASON);
                    if (reason instanceof String) view.setReason((String) reason);
                }
            });
        }
    }

    public static Map<String, Object> draft(HistoricTaskInstance source) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("node", source.getTaskDefinitionKey());
        value.put("sourceTaskId", source.getId());
        value.put("assignee", source.getAssignee());
        value.put("reason", source.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_REASON));
        return value;
    }

    public static void copy(TaskService tasks, HistoricTaskInstance source, Task target) {
        if (!source.getTaskDefinitionKey().equals(target.getTaskDefinitionKey())
                || !String.valueOf(source.getAssignee()).equals(target.getAssignee())) return;
        if (!tasks.hasVariableLocal(target.getId(), SOURCE_TASK))
            tasks.setVariableLocal(target.getId(), SOURCE_TASK, source.getId());
        Object reason = source.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_REASON);
        if (reason instanceof String && !((String) reason).trim().isEmpty()
                && !tasks.hasVariableLocal(target.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON))
            tasks.setVariableLocal(target.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON, reason);
    }

    public static void capture(DelegateExecution execution, TaskEntity task) {
        Object value = execution.getVariable(KEY);
        if (!(value instanceof Map)) return;
        Map<?, ?> draft = (Map<?, ?>) value;
        if (!task.getTaskDefinitionKey().equals(draft.get("node"))
                || !String.valueOf(task.getAssignee()).equals(draft.get("assignee"))) return;
        if (draft.get("sourceTaskId") instanceof String && !task.hasVariableLocal(SOURCE_TASK))
            task.setVariableLocal(SOURCE_TASK, draft.get("sourceTaskId"));
        Object reason = draft.get("reason");
        if (reason instanceof String && !((String) reason).trim().isEmpty()
                && !task.hasVariableLocal(BpmnVariableConstants.TASK_VARIABLE_REASON)) {
            task.setVariableLocal(BpmnVariableConstants.TASK_VARIABLE_REASON, reason);
        }
        execution.removeVariable(KEY);
    }
}
