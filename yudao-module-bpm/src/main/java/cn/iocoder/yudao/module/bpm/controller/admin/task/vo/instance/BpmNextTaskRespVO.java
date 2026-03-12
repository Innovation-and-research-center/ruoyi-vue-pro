package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ConditionResult;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BpmNextTaskRespVO {
    /** 目标任务定义的 Key (id) */
    private String taskDefKey;

    /** 目标任务名称 */
    private String taskName;

    /** 只有满足该条件表达式时，才会走到该节点 (例如 ${auditStatus == 1}) */
    private ConditionResult conditionExpression;

    /** 目标节点的拓展属性 (Map形式) */
    private Map<String, String> extensionProperties;

    private List<UserSimpleRespVO> candidateUsers2;

    private String taskType;

    private List<BpmUserGroupRespVO> candidateUsers;

    private String flowName;

    private Integer flowSort;

    /** 已经设置的任务人员 ID 集合 */
    private List<Long> assignedUserIds;

    /** 已经设置的任务人员详细信息 */
    private List<UserSimpleRespVO> assignedUsers;
}




