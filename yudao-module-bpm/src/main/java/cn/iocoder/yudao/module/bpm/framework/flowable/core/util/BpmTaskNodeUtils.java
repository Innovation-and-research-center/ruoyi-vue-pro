package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import org.flowable.bpmn.model.*;
import java.util.ArrayList;
import java.util.List;

public class BpmTaskNodeUtils {

    /**
     * 获取指定节点之后的下一级用户任务列表
     * 支持穿透排他网关、并行网关、连线
     */
    public static List<UserTask> getNextUserTasks(FlowElement source) {
        List<UserTask> result = new ArrayList<>();
        if (source == null) return result;
        recursiveFindNext(source, result);
        return result;
    }

    private static void recursiveFindNext(FlowElement current, List<UserTask> result) {
        List<SequenceFlow> outgoingFlows = null;

        if (current instanceof FlowNode) {
            outgoingFlows = ((FlowNode) current).getOutgoingFlows();
        }

        if (outgoingFlows == null) return;

        for (SequenceFlow flow : outgoingFlows) {
            FlowElement target = flow.getTargetFlowElement();

            // 1. 如果是用户任务，且之前没添加过（防止环路），加入结果
            if (target instanceof UserTask) {
                if (!result.contains(target)) {
                    result.add((UserTask) target);
                }
                // 找到任务后停止当前分支的深入，因为我们只找“下一级”
            }
            // 2. 如果是网关(排他/并行) 或 普通连线节点，继续递归寻找
            else if (target instanceof Gateway || target instanceof SequenceFlow) {
                recursiveFindNext(target, result);
            }
            // 注意：EndEvent 会自然结束递归
        }
    }

    // 获取 StartEvent (保持不变)
    public static StartEvent getStartEvent(BpmnModel model) {
        org.flowable.bpmn.model.Process process = model.getMainProcess();
        for (FlowElement element : process.getFlowElements()) {
            if (element instanceof StartEvent) {
                return (StartEvent) element;
            }
        }
        return null;
    }
}
