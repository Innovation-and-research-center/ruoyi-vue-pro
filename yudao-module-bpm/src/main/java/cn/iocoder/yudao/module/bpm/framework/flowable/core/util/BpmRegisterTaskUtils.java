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
