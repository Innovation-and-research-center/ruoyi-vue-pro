package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_REGISTER_TASK;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.BUSINESS_NODE_TYPE;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.BUSINESS_NODE_TYPE_REGISTER;

/**
 * 通用登记任务工具。
 *
 * <p>新流程通过 {@code business_node_type=register} 标识登记节点；旧收文流程继续兼容固定节点 ID，
 * 避免影响存量流程实例。</p>
 */
public final class BpmRegisterTaskUtils {

    private static final String PROPERTIES_ELEMENT = "properties";
    private static final String PROPERTY_ELEMENT = "property";
    private static final String PROPERTY_NAME_ATTRIBUTE = "name";
    private static final String PROPERTY_VALUE_ATTRIBUTE = "value";

    private BpmRegisterTaskUtils() {
    }

    /** 退回登记重新提交时，选人及人工流向写入本分支；传入的业务变量副本会剥离这些字段。 */
    public static String prepareSubmitScope(org.flowable.engine.RuntimeService runtime,
                                           org.flowable.task.api.Task task, Map<String, Object> variables) {
        String instance = task.getProcessInstanceId();
        if (!BpmProcessOperationVersion.isCurrent(runtime.getVariableLocal(instance, BpmProcessOperationVersion.KEY))) return instance;
        String executionId = task.getExecutionId();
        while (executionId != null && !instance.equals(executionId)) {
            if (Boolean.TRUE.equals(runtime.getVariableLocal(executionId, BpmDispatchSources.BRANCH))) {
                Map<String, Object> routing = new java.util.HashMap<>();
                variables.entrySet().removeIf(entry -> {
                    if (entry.getKey().matches("(?:select_node_\\d+|next_node\\d+)")
                            || cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants
                            .PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES.equals(entry.getKey())) {
                        routing.put(entry.getKey(), entry.getValue()); return true;
                    }
                    return false;
                });
                runtime.setVariablesLocal(executionId, routing);
                return executionId;
            }
            org.flowable.engine.runtime.Execution execution = runtime.createExecutionQuery().executionId(executionId).singleResult();
            executionId = execution == null ? null : execution.getParentId();
        }
        return instance;
    }

    public static boolean isRegisterTask(DelegateExecution execution) {
        return execution != null && isRegisterTask(execution.getCurrentFlowElement());
    }

    public static boolean isRegisterTask(BpmnModel bpmnModel, String activityId) {
        return isRegisterTask(BpmnModelUtils.getFlowElementById(bpmnModel, activityId));
    }

    public static boolean isRegisterTask(FlowElement flowElement) {
        if (flowElement == null) {
            return false;
        }
        // 兼容已经发布的旧收文流程。
        if (isLegacyReceiveRegisterTask(flowElement)) {
            return true;
        }
        return StrUtil.equals(BUSINESS_NODE_TYPE_REGISTER,
                getProperty(flowElement, BUSINESS_NODE_TYPE));
    }

    public static boolean isLegacyReceiveRegisterTask(FlowElement flowElement) {
        return flowElement != null && StrUtil.equals(RECEIVE_REGISTER_TASK, flowElement.getId());
    }

    public static Set<Long> calculateCandidateUserIds(DelegateExecution execution,
                                                       BpmTaskCandidateInvoker taskCandidateInvoker) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        Set<Long> userIds = calculateConfiguredCandidateUserIds(execution, taskCandidateInvoker);
        if (CollUtil.isNotEmpty(userIds)) {
            return userIds;
        }
        return calculateLegacyCandidateUserIds(flowElement);
    }

    public static Set<Long> calculateCandidateUserIds(BpmnModel bpmnModel, String activityId,
                                                       BpmTaskCandidateInvoker taskCandidateInvoker,
                                                       Long startUserId, String processDefinitionId,
                                                       Map<String, Object> processVariables) {
        FlowElement flowElement = BpmnModelUtils.getFlowElementById(bpmnModel, activityId);
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(flowElement);
        Set<Long> userIds = strategy != null
                ? taskCandidateInvoker.calculateUsersByActivity(bpmnModel, activityId,
                startUserId, processDefinitionId, processVariables)
                : Collections.emptySet();
        if (CollUtil.isNotEmpty(userIds)) {
            return userIds;
        }
        return calculateLegacyCandidateUserIds(flowElement);
    }

    /** 读取 flowable:properties/flowable:property 中的自定义属性。 */
    public static String getProperty(FlowElement flowElement, String propertyName) {
        if (flowElement == null || StrUtil.isBlank(propertyName)) {
            return null;
        }
        List<ExtensionElement> wrappers = flowElement.getExtensionElements().get(PROPERTIES_ELEMENT);
        if (CollUtil.isEmpty(wrappers)) {
            return null;
        }
        for (ExtensionElement wrapper : wrappers) {
            List<ExtensionElement> properties = wrapper.getChildElements().get(PROPERTY_ELEMENT);
            if (CollUtil.isEmpty(properties)) {
                continue;
            }
            for (ExtensionElement property : properties) {
                if (StrUtil.equals(propertyName, property.getAttributeValue(null, PROPERTY_NAME_ATTRIBUTE))) {
                    return property.getAttributeValue(null, PROPERTY_VALUE_ATTRIBUTE);
                }
            }
        }
        return null;
    }

    private static Set<Long> calculateConfiguredCandidateUserIds(DelegateExecution execution,
                                                                  BpmTaskCandidateInvoker taskCandidateInvoker) {
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(execution.getCurrentFlowElement());
        return strategy != null ? taskCandidateInvoker.calculateUsersByTask(execution) : Collections.emptySet();
    }

    private static Set<Long> calculateLegacyCandidateUserIds(FlowElement flowElement) {
        if (isLegacyReceiveRegisterTask(flowElement)) {
            return BpmReceiveRegisterTaskUtils.calculateCandidateUserIds(flowElement);
        }
        throw new IllegalStateException("登记节点未配置有效办理人："
                + (flowElement != null ? flowElement.getName() : "未知节点"));
    }

}
