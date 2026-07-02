package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BpmProcessInstanceUnifiedRespVO {
    private String id;                 // 办件编号 (ProcessInstanceId)
    private String name;               // 办件名称 (Process Name)
    private String category;           // 办件类型 (Process Definition Category/Name)
    private String startUserNickname;  // 发起人
    private String startDeptName;      // 来文单位 (发起人部门)
    private LocalDateTime createTime;  // 开始日期
    private LocalDateTime endTime;     // 办结日期
    private Integer status;            // 办件状态
    private String processDefinitionKey;
    private String sourceUnit;
    private String urgencyDegree;
    private String deadlineDate;
    private Integer processResult;
    private Integer processStatus;

    private String processReason;

    /**
     * 是否历史迁移数据。历史数据来自 hist_wf，不进入 Flowable 表。
     */
    private Boolean isHistory;

    /**
     * 历史业务项目内码，用于跳转历史详情和关联业务表。
     */
    private String projectId;

    /**
     * 数据来源：flowable / history。
     */
    private String sourceType;

    // --- 运行时信息 (仅进行中流程有) ---
    private String currTaskName;       // 在办环节 (当前任务节点名称)
    private String currTaskAssignee;   // 在办人员 (当前办理人姓名)
}
