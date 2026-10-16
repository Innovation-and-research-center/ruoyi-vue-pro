package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;

/** executionId 是精确会签根，不是节点编号；排队成员尚无 taskId。 */
@Data
public class BpmTaskWithdrawTargetRespVO {
    private String executionId;
    private String nodeId;
    private String nodeName;
    private Long userId;
    private String nickname;
    private String taskId;
    private boolean queued;
    private boolean withdrawable;
    private String disabledReason;
}
