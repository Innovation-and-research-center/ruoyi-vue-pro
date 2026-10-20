package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants;
import org.flowable.bpmn.model.BpmnModel;

/** 只有收文主任拟办来源允许追加和按人员撤回；目标节点、办理人职务和显示名称不参与授权。 */
public final class BpmTaskDispatchPolicy {
    private BpmTaskDispatchPolicy() {}

    public static boolean allowsMemberOperations(String processKey, String taskKey) {
        return BpmTaskKeyConstants.RECEIVE.equals(processKey)
                && BpmTaskKeyConstants.RECEIVE_DIRECTOR_TASK.equals(taskKey);
    }

    public static boolean allowsMemberOperations(BpmnModel model, String taskKey) {
        return model != null && model.getMainProcess() != null
                && allowsMemberOperations(model.getMainProcess().getId(), taskKey);
    }
}
