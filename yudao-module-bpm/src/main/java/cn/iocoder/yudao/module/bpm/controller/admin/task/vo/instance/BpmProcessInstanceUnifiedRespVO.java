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

    // --- 运行时信息 (仅进行中流程有) ---
    private String currTaskName;       // 在办环节 (当前任务节点名称)
    private String currTaskAssignee;   // 在办人员 (当前办理人姓名)
}
