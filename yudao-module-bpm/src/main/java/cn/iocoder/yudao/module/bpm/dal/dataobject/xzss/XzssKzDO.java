package cn.iocoder.yudao.module.bpm.dal.dataobject.xzss;

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
 * 行政诉讼拓展 DO
 *
 * @author 管理员
 */
@TableName("t_xzss_kz")
@KeySequence("t_xzss_kz_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XzssKzDO extends BaseDO {

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
     * 裁定判决日期
     */
    private LocalDateTime cdpjRq;
    /**
     * 建议函日期
     */
    private LocalDateTime jyhRq;
    /**
     * 建议函内容
     */
    private String jyhNr;
    /**
     * 转业务科室日期
     */
    private LocalDateTime zksRq;
    /**
     * 判决结果
     */
    private String pjJg;
    /**
     * 裁定结果
     */
    private String cdJg;
    /**
     * 行政赔偿（单位：元）
     */
    private BigDecimal xzPc;
    /**
     * 执行情况
     */
    private String zxQk;
    /**
     * 装订人
     */
    private String zdr;
    /**
     * 装订日期
     */
    private LocalDateTime zdRq;
    /**
     * 装订情况
     */
    private String zdQk;
    /**
     * 移交人
     */
    private String yjr;
    /**
     * 移交日期
     */
    private LocalDateTime yjRq;
    /**
     * 移交情况
     */
    private String yjQk;
    /**
     * 备注
     */
    private String bz;
    /**
     * 开庭日期
     */
    private LocalDateTime ktRq;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;

}