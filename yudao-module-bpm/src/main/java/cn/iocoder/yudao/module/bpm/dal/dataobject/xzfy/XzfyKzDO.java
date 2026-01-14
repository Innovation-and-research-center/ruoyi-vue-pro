package cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy;

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
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 行政复议扩展 DO
 *
 * @author 管理员
 */
@TableName("t_xzfy_kz")
@KeySequence("t_xzfy_kz_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XzfyKzDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 备用主键
     */
    private String xmGuid;
    /**
     * 复议决定日期
     */
    private LocalDateTime fyjdRq;
    /**
     * 复议结果：0撤销，1维持
     */
    private Short fyJg;
    /**
     * 转业务科室日期
     */
    private LocalDateTime zksRq;
    /**
     * 意见书文号
     */
    private String yjsWh;
    /**
     * 意见书日期
     */
    private LocalDateTime yjsRq;
    /**
     * 行政赔偿，单位：元
     */
    private BigDecimal xzPc;
    /**
     * 执行情况
     */
    private String zxQk;
    /**
     * 是否装订：0否，1是
     */
    private Short sfZd;
    /**
     * 装订人
     */
    private String zdr;
    /**
     * 装订日期
     */
    private LocalDateTime zdRq;
    /**
     * 是否移交：0否，1是
     */
    private Short sfYj;
    /**
     * 移交人
     */
    private String yjr;
    /**
     * 移交日期
     */
    private LocalDateTime yjRq;
    /**
     * 备注
     */
    private String bz;
    /**
     * 意见书内容
     */
    private String yjsNr;
    /**
     * 听证日期
     */
    private LocalDateTime tzRq;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;

}