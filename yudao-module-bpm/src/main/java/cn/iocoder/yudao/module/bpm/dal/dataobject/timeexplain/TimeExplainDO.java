package cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 外出请假补假 DO
 *
 * @author 管理员
 */
@TableName("t_time_explain")
@KeySequence("t_time_explain_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeExplainDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * GUID
     */
    private String timeexplainGuid;
    /**
     * 项目编号
     */
    private String projectId;
    /**
     * 流程内码ID
     */
    private String proinstId;
    /**
     * 流程内码ID
     */
    private String actinstId;
    /**
     * 人员编号
     */
    private Long userId;
    /**
     * 人员姓名
     */
    private String userName;
    /**
     * 所属部门
     */
    private String deptment;
    /**
     * 登记时间
     */
    private LocalDateTime checkDate;
    /**
     * 开始时间
     */
    private LocalDateTime checkBegin;
    /**
     * 结束时间
     */
    private LocalDateTime checkEnd;
    /**
     * 考勤一级分类
     */
    private String firstType;
    /**
     * 考勤二级分类
     */
    private String secondType;
    /**
     * 原因说明
     */
    private String reason;
    /**
     * 科室负责人意见
     */
    private String deptDirectorIdea;
    /**
     * 科室负责人
     */
    private String deptDirector;
    /**
     * 负责人意见填写日期
     */
    private LocalDateTime deptDirectorDate;
    /**
     * 分管领导意见
     */
    private String chargeDirectorIdea;
    /**
     * 分管领导
     */
    private String chargeDirector;
    /**
     * 分管领导意见填写日期
     */
    private LocalDateTime chargeDirectorDate;
    /**
     * 主要负责人意见
     */
    private String coreDirectorIdea;
    /**
     * 主要负责人
     */
    private String coreDirector;
    /**
     * 主要负责人意见填写日期
     */
    private LocalDateTime coreDirectorDate;
    /**
     * 审核状态（0审批中 1审核完毕 2删除）
     *
     * 枚举 {@link TODO system_user_sex 对应的类}
     */
    private Long status;
    /**
     * 局长意见
     */
    private String juzDirectorIdea;
    /**
     * 局长(副局长)
     */
    private String juzDirector;
    /**
     * 局长(副)意见填写日期
     */
    private LocalDateTime juzDirectorDate;
    /**
     * 局办意见
     */
    private String jubDirectorIdea;
    /**
     * 局办负责人
     */
    private String jubDirector;
    /**
     * 局办意见填写日期
     */
    private LocalDateTime jubDirectorDate;
    /**
     * 职务
     */
    private String position;
    /**
     * 工龄
     */
    private String seniority;
    /**
     * 对应的销假的Projectid
     */
    private String xiaojiaProjectid;
    /**
     * 销假意见
     */
    private String xiaojiaIdea;
    /**
     * 销假审核人
     */
    private String xiaojiaDirector;
    /**
     * 销假审核日期
     */
    private LocalDateTime xiaojiaDate;
    /**
     * 销假说明
     */
    private String xiaojiaMsg;
    /**
     * 请假天数
     */
    private BigDecimal days;
    /**
     * 出国
     */
    private BigDecimal goabroad;
    /**
     * 出发地
     */
    private String startPlace;
    /**
     * 目的地
     */
    private String endPlace;
    /**
     * 参加工作时间
     */
    private LocalDateTime workDate;
    /**
     * 探望对象
     */
    private String visitObject;
    /**
     * 原请假项目编号
     */
    private String oldProjectId;
    /**
     * 原开始时间
     */
    private LocalDateTime oldCheckBegin;
    /**
     * 原结束时间
     */
    private LocalDateTime oldCheckEnd;
    /**
     * 出生年月
     */
    private LocalDateTime birthday;
    /**
     * 年份
     */
    private Short year;
    /**
     * 流水号
     */
    private Long serialNumber;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;
    /**
     * 文件路径
     */
    private String filepath;


    private String cancelReason;

}