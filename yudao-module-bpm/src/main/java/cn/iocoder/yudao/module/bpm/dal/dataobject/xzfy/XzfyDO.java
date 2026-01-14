package cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy;

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
 * 行政复议 DO
 *
 * @author 管理员
 */
@TableName("t_xzfy_list")
@KeySequence("t_xzfy_list_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XzfyDO extends BaseDO {

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
     * 来文日期
     */
    private LocalDateTime swRq;
    /**
     * 申请人
     */
    private String sqr;
    /**
     * 被申请人
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
     * 复议请求
     */
    private String fyNr;
    /**
     * 承办人
     */
    private String cbr;
    /**
     * 承办日期
     */
    private LocalDateTime cbRq;
    /**
     * 送复议机关日期
     */
    private LocalDateTime sfyjgRq;
    /**
     * 行政区（街道、村）
     */
    private String xzq;
    /**
     * 监督监管
     */
    private Short issupervise;
    /**
     * 办理时限
     */
    private LocalDateTime zhubandate;
    /**
     * 是否已寄件提醒
     */
    private Short mailTip;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;


}