package cn.iocoder.yudao.module.bpm.dal.dataobject.xzss;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 行政诉讼 DO
 *
 * @author 管理员
 */
@TableName("t_xzss_list")
@KeySequence("t_xzss_list_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XzssDO extends BaseDO {

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
     * 来文号
     */
    private String swWh;
    /**
     * 来文机关
     */
    private String swJg;
    /**
     * 收文日期
     */
    private LocalDateTime swRq;
    /**
     * 原告
     */
    private String sqr;
    /**
     * 被告
     */
    private String bsqr;
    /**
     * 第三人
     */
    private String dsr;
    /**
     * 土地坐落
     */
    private String tdZl;
    /**
     * 诉讼类型：1一审，2二审，3再审，如果多次，继续记录
     */
    private Short ssLx;
    /**
     * 复议项目主键
     */
    private String fyGuid;
    /**
     * 复议案号
     */
    private String fyAh;
    /**
     * 上一审项目主键
     */
    private String ssGuid;
    /**
     * 上一审案号
     */
    private String ssAh;
    /**
     * 类别一
     */
    private String lb1;
    /**
     * 类别二
     */
    private String lb2;
    /**
     * 类别三
     */
    private String lb3;
    /**
     * 类别四
     */
    private String lb4;
    /**
     * 类别五
     */
    private String lb5;
    /**
     * 复议请求
     */
    private String ssNr;
    /**
     * 承办人
     */
    private String cbr;
    /**
     * 承办日期
     */
    private LocalDateTime cbRq;
    /**
     * 送法院日期
     */
    private LocalDateTime sfyjgRq;
    /**
     * 上诉人
     */
    private String ssr;
    /**
     * 被上诉人
     */
    private String bssr;
    /**
     * 再审申请人
     */
    private String zssqr;
    /**
     * 再审被申请人
     */
    private String zsbsqr;
    /**
     * 监督监管
     */
    private Short issupervise;
    /**
     * 办理时限
     */
    private LocalDateTime zhubandate;
    /**
     * 诉讼内容
     */
    private String ssnr;
    /**
     * 是否已寄件提醒
     */
    private Short mailTip;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;

    private Short status;

    private String cancelReason;

}