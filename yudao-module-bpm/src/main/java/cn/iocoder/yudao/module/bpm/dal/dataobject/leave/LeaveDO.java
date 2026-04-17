package cn.iocoder.yudao.module.bpm.dal.dataobject.leave;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 假期申请审批 DO
 *
 * @author 芋道源码
 */
@TableName("bpm_leave")
@KeySequence("bpm_leave_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDO extends BaseDO {

    /**
     * 假期申请审批内码
     */
    @TableId
    private Long id;
    /**
     * 申请时间
     */
    private LocalDateTime applyDate;
    /**
     * 开始时间(请假时间段中最小的时间)
     */
    private LocalDateTime qxjStartDate;
    /**
     * 结束时间(请假时间段中最大的时间)
     */
    private LocalDateTime qxjEndDate;
    /**
     * 请（休）假种类
     */
    private Integer qxjType;
    /**
     * 事假理由
     */
    private String sjReason;
    /**
     * 共计天数
     */
    private BigDecimal totalTs;
    /**
     * 所在单位（科室）意见
     */
    private String unitOpinion;
    /**
     * 所在单位（科室）意见审核人
     */
    private String unitOpinionShr;
    /**
     * 所在单位（科室）意见审核日期
     */
    private LocalDateTime unitOpinionDate;
    /**
     * 局办公室意见
     */
    private String jbgsOpinion;
    /**
     * 局办公室意见审核人
     */
    private String jbgsOpinionShr;
    /**
     * 局办公室意见审核日期
     */
    private LocalDateTime jbgsOpinionDate;
    /**
     * 副局长意见
     */
    private String fjzOpinion;
    /**
     * 副局长意见审核人
     */
    private String fjzOpinionShr;
    /**
     * 副局长意见审核日期
     */
    private LocalDateTime fjzOpinionDate;
    /**
     * 常务副局长意见
     */
    private String cwfjzOpinion;
    /**
     * 常务副局长意见审核人
     */
    private String cwfjzOpinionShr;
    /**
     * 常务副局长意见审核日期
     */
    private LocalDateTime cwfjzOpinionDate;
    /**
     * 局长意见
     */
    private String jzOpinion;
    /**
     * 局长意见审核人
     */
    private String jzOpinionShr;
    /**
     * 局长意见审核日期
     */
    private LocalDateTime jzOpinionDate;
    /**
     * 审批状态
     */
    private Short spzt;
    /**
     * 申请用户
     */
    private Integer userid;
    /**
     * 销假申请时间
     */
    private LocalDateTime xjDate;
    /**
     * 销假总天数
     */
    private BigDecimal xjTs;
    /**
     * 销假备注
     */
    private String xjBz;
    /**
     * 销假审批状态
     */
    private Short xjSpzt;
    /**
     * 销假审批意见
     */
    private String xjOption;
    /**
     * 销假意见审核人
     */
    private String xjOpinionShr;
    /**
     * 请假时间描述（当请休假时间修改的时候进行更新）
     */
    private String dateDesc;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;
    /**
     * 文件地址
     */
    private String filepath;

    private String reason;


}