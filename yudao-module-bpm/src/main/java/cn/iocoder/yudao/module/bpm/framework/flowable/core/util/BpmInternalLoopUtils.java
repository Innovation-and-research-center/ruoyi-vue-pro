package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants;
import org.flowable.bpmn.model.*;

import java.util.HashSet;
import java.util.Set;

/** 内循环任务的单出口流转规则，供选项查询和提交校验共用。 */
public final class BpmInternalLoopUtils {

    private BpmInternalLoopUtils() {
    }

    public static boolean canContinueToSingleTarget(BpmnModel model, FlowElement source) {
        if (model == null || model.getMainProcess() == null || !(source instanceof UserTask)) {
            return false;
        }
        String processKey = model.getMainProcess().getId();
        if (!BpmTaskKeyConstants.XZFY.equals(processKey) && !BpmTaskKeyConstants.XZSS.equals(processKey)) {
            return false;
        }
        Set<String> visited = new HashSet<>();
        FlowNode current = (FlowNode) source;
        while (visited.add(current.getId())) {
            if (current.getOutgoingFlows().size() != 1) {
                return false;
            }
            SequenceFlow flow = current.getOutgoingFlows().get(0);
            if (StrUtil.isNotBlank(flow.getConditionExpression())) {
                return false;
            }
            FlowElement target = flow.getTargetFlowElement();
            if (target == null) {
                target = model.getFlowElement(flow.getTargetRef());
            }
            if (target instanceof UserTask || target instanceof EndEvent) {
                return !visited.contains(target.getId());
            }
            if (!(target instanceof Gateway)) {
                return false;
            }
            current = (FlowNode) target;
        }
        return false;
    }
}
