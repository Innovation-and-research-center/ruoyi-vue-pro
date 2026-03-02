package cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc;

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
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 收文 DO
 *
 * @author 芋道
 */
@TableName("bpm_receive_doc")
@KeySequence("bpm_receive_doc_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveDocDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 收文草稿内容
     */
    private byte[] receiveDocDraft;
    /**
     * 单位类别
     *
     * 枚举 {@link TODO receive_class 对应的类}
     */
    private String docClass;
    /**
     * 来文单位
     *
     * 枚举 {@link TODO agency_name 对应的类}
     */
    private String sendDept;
    /**
     * 来文字号
     */
    private String sendDocNumber;
    /**
     * 发文日期
     */
    private LocalDateTime sendTime;
    /**
     * 收文编号
     */
    private String receiveDocNumber;
    /**
     * 收文日期
     */
    private LocalDateTime receiveTime;
    /**
     * 签收份数
     */
    private String signReceiveCount;
    /**
     * 保管期限
     */
    private String keepTerm;
    /**
     * 项目内码
     */
    private String projectId;
    /**
     * 主题
     */
    private String subject;
    /**
     * 紧急程度
     *
     * 枚举 {@link TODO emergency_degree 对应的类}
     */
    private String urgencyDegree;
    /**
     * 机密程度
     */
    private String secretDegree;
    /**
     * 备注
     */
    private String remark;
    /**
     * 件号
     */
    private Long docSequence;
    /**
     * 年份
     */
    private String year;
    /**
     * 主题词
     */
    private String keyWords;
    /**
     * 结果文件名称
     */
    private String resultFileName;
    /**
     * 文件类别
     *
     * 枚举 {@link TODO doc_class 对应的类}
     */
    private String docSecondClass;
    /**
     * 收文审批意见
     */
    private byte[] receiveDocNotion;
    /**
     * 收文业务类型
     */
    private String recBizType;
    /**
     * 是否为规范性文件
     */
    private Short canonicalFile;
    /**
     * 办理时限
     */
    private LocalDateTime dealTimelimit;
    /**
     * 是否发布到外网
     */
    private Short releasetoout;
    /**
     * 是否发布到专网
     */
    private Short releasetospecial;
    /**
     * 内部传阅
     */
    private String releasetoother;
    /**
     * 主办部门
     */
    private String responsibleDept;
    /**
     * 办公室主任意见
     */
    private String directoridea;
    /**
     * 能否查询
     */
    private Short couldquery;
    /**
     * 表单页面
     */
    private String vdformpage;
    /**
     * 是否默认访问许可
     */
    private Short defaultAccess;
    /**
     * 主任意见人
     */
    private String directorname;
    /**
     * 主任意见时间
     */
    private LocalDateTime directordate;
    /**
     * 厅/局长审批意见
     */
    private String fugleidea;
    /**
     * 附件路径
     */
    private String attachFilePath;
    /**
     * 签收部门
     */
    private String receiveDept;
    /**
     * 签收人
     */
    private String receiver;
    /**
     * 是否全局学习
     */
    private Short isallstudy;
    /**
     * 厅/局意见人
     */
    private String fuglename;
    /**
     * 厅/局意见时间
     */
    private LocalDateTime fugledate;
    /**
     * 处理结果
     */
    private String dealResult;
    /**
     * 公文范围
     */
    private String docRange;
    /**
     * 办理期限(天)
     */
    private Short workdays;
    /**
     * 是否回复并审结
     */
    private Short isResponseSettled;
    /**
     * 是否进行监管
     */
    private Short issupervise;
    /**
     * 主办办结时间
     */
    private LocalDateTime zhubandate;
    /**
     * 协办办结时间
     */
    private LocalDateTime xiebandate;
    /**
     * 转自收文ID
     */
    private String fromrecid;
    /**
     * 承办人/信息录入人
     */
    private String takername;
    /**
     * 承办人意见
     */
    private String takeridea;
    /**
     * 承办人意见时间
     */
    private LocalDateTime takerdate;
    /**
     * 公文主键
     */
    private Long exchangedocid;
    /**
     * 发文内码(发文转收文时用)
     */
    private Long sendDocId;
    /**
     * 是否重要
     */
    private Short ifimportant;
    /**
     * 经办人
     */
    private String draftPerson;
    /**
     * 经办时间
     */
    private LocalDateTime draftDate;
    /**
     * 经办人意见
     */
    private String draftIdea;
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
     * 厅/局长审批意见
     */
    private String leaderIdea;
    /**
     * 厅/局长
     */
    private String leaderPerson;
    /**
     * 厅/局长审批日期
     */
    private LocalDateTime leaderDate;
    /**
     * 是否要归档,1不归档，否则归档
     */
    private Short notpigeonhold;
    /**
     * 是否已归档,1已归档，否则未归档
     */
    private Short ifpigeonhold;
    /**
     * 归档号
     */
    private String pigeonholeNum;
    /**
     * 归档顺序号
     */
    private Long pigeonholeSeq;
    /**
     * 归档结果
     */
    private String pigeonholeResult;
    /**
     * 流程实例的编号
     */
    private String processInstanceId;


}