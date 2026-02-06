package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;
import java.util.Date;


@Data
public class BpmTaskFlowTaskNodeRespVO {
    /** 节点ID (Task ID) */
    private String taskId;

    /** 节点名称 */
    private String taskName;

    /** 办理人 ID */
    private String assignee;

    /** 办理人名称 (需要关联查用户表，这里暂存ID或名称) */
    private String assigneeName;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 耗时 (毫秒) */
    private Long duration;

    /** 节点类型 (用于前端区分是 人工审批 还是 开始节点) */
    private String activityType;

    private Integer status;
}
