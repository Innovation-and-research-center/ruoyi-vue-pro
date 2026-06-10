package cn.iocoder.yudao.module.bpm.dal.dataobject.confflow;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;

/**
 * 会议报告单 DO
 *
 * @author 芋道源码
 */
@TableName("t_confflow")
@KeySequence("t_confflow_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfflowDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 办件GUID
     */
    private String docGuid;
    /**
     * 办件类型
     */
    private String docType;
    /**
     * 项目ID
     */
    private String projectId;
    /**
     * 申请人ID
     */
    private Long userId;
    /**
     * 申请人
     */
    private String userName;
    /**
     * 申请人部门ID
     */
    private Long deptId;
    /**
     * 申请人部门
     */
    private String deptName;
    /**
     * 申请日期
     */
    private LocalDateTime applyDate;
    /**
     * 会议时间
     */
    private LocalDateTime startDate;
    /**
     * 会议名称
     */
    private String title;
    /**
     * 会议类型
     */
    private String confType;
    /**
     * 提议内容
     */
    private String content;
    /**
     * 备注
     */
    private String remark;
    /**
     * 会议地点
     */
    private String venue;
    /**
     * 召集单位及召集人
     */
    private String joinUnit;
    /**
     * 我局参会科室
     */
    private String offerUnit;
    /**
     * 我局参会人员
     */
    private String offerPerson;
    /**
     * 参会人员会议情况及建议
     */
    private String situation;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;
    /**
     * 附件路径
     */
    private String attachFilePath;
    private Short status;

    private String cancelReason;

}
