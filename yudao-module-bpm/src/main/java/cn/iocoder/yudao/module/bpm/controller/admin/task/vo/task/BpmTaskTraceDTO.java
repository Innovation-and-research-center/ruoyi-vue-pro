package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BpmTaskTraceDTO {
    /** 当前任务信息 */
    private BpmTaskFlowTaskNodeRespVO currentTask;

    /** 来源任务列表 (可能来自并行的多条线，或者驳回) */
    private List<BpmTaskFlowTaskNodeRespVO> previousTasks;

    /** 去向任务列表 (如果是并行网关，会有多个) */
    private List<BpmTaskFlowTaskNodeRespVO> nextTasks;

    private LocalDateTime processStartTime;

    /** 流程发起人 ID */
    private String startUserId;
}
