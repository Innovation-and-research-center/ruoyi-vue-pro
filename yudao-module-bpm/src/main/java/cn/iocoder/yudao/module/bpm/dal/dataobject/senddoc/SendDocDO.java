package cn.iocoder.yudao.module.bpm.dal.dataobject.senddoc;

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
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 发文 DO
 *
 * @author 管理员
 */
@TableName("t_send_doc")
@KeySequence("t_send_doc_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendDocDO extends BaseDO {

    /**
     * 发文表主键
     */
    @TableId
    private Long id;
    /**
     * 发文草稿内容
     */
    private byte[] sendDocDraft;
    /**
     * 发文结果内容
     */
    private byte[] sendDocResult;
    /**
     * 公文性质
     */
    private String docProperty;
    /**
     * 主送机关
     */
    private String primarySendDept;
    /**
     * 抄送机关
     */
    private String copySendDept;
    /**
     * 抄报机关
     */
    private String reportSendDept;
    /**
     * 抄送机关
     */
    private String deliverSendDept;
    /**
     * 发文号
     */
    private String sendDocNumber;
    /**
     * 发文日期
     */
    private LocalDateTime sendTime;
    /**
     * 签印份数
     */
    private String signPrintCount;
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
     * 文号
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
     * 发文单位
     */
    private String sendDept;
    /**
     * 拟稿人
     */
    private String draftPerson;
    /**
     * 拟稿时间
     */
    private LocalDateTime draftDate;
    /**
     * 事业单位拟稿人意见
     */
    private String draftIdea;
    /**
     * 拟稿人联系电话
     */
    private String contactPhone;
    /**
     * 最终公文名称
     */
    private String resultFileName;
    /**
     * 二级分类
     */
    private Long docSecondClass;
    /**
     * 一级分类
     */
    private Long docClass;
    /**
     * 办件时限
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
     * 是否发布到内网
     */
    private Short releasetoin;
    /**
     * 最近修改发布人
     */
    private String updatereleaseman;
    /**
     * 最近修改发布时间
     */
    private LocalDateTime updatereleasedate;
    /**
     * 内部传阅
     */
    private String releasetoother;
    /**
     * 收文内码(收文转发文时用)
     */
    private Long receiveDocId;
    /**
     * 正文稿子签出人
     */
    private Long checkoutUserId;
    /**
     * 签出时间
     */
    private LocalDateTime checkoutTime;
    /**
     * 办公室秘书意见
     */
    private String secretaryidea;
    /**
     * 办公室秘书
     */
    private String secretaryideaPerson;
    /**
     * 办公室秘书意见日期
     */
    private LocalDateTime secretaryideaDate;
    /**
     * 办公室主任意见
     */
    private String directoridea;
    /**
     * 办公室主任
     */
    private String directorideaPerson;
    /**
     * 办公室主任意见日期
     */
    private LocalDateTime directorideaDate;
    /**
     * 能否查询
     */
    private Short couldquery;
    /**
     * 表单页面
     */
    private String vdformpage;
    /**
     * 能否访问
     */
    private Short defaultAccess;
    /**
     * 打印条形码
     */
    private Short printbarcode;
    /**
     * 信息公开
     */
    private String inforelease;
    /**
     * 不予公开理由
     */
    private String noreleasecause;
    /**
     * 附件路径
     */
    private String attachFilePath;
    /**
     * 判断正文草稿，审批搞，套红正文是否存在
     */
    private String filetag;
    /**
     * 拟稿单位
     */
    private String draftDept;
    /**
     * 主办部门
     */
    private String responsibleDept;
    /**
     * 是否导出
     */
    private Short isexported;
    /**
     * 是否可以导出
     */
    private Short iscanexport;
    /**
     * 自定义编号
     */
    private String customnumber;
    /**
     * 拟稿人用户ID
     */
    private Long draftPersonUserid;
    /**
     * 是否需要TIF文件
     */
    private Short ishavetif;
    /**
     * 公文交换主送机关
     */
    private String exPrimarySendDept;
    /**
     * 公文交换抄送机关
     */
    private String exCopySendDept;
    /**
     * 是否行政规范性文件
     */
    private String ispolicynorm;
    /**
     * 法制机构意见
     */
    private String fzjgyjTz;
    /**
     * 法制机构意见用户
     */
    private String fzjgyjTzPerson;
    /**
     * 法制机构意见日期
     */
    private LocalDateTime fzjgyjTzDate;
    /**
     * 法制编号2
     */
    private String customnumber2;
    /**
     * 法制编号3
     */
    private String customnumber3;
    /**
     * 会签部门
     */
    private String hqdept;
    /**
     * 会签人员姓名
     */
    private String hqname;
    /**
     * 会签意见
     */
    private String hqidea;
    /**
     * 会签时间
     */
    private LocalDateTime hqdate;
    /**
     * 签发人
     */
    private String issuedName;
    /**
     * 校对
     */
    private String proofreader;
    /**
     * 校对日期
     */
    private LocalDateTime proofreaderDate;
    /**
     * 印制
     */
    private String yinzhiren;
    /**
     * 用印
     */
    private String yongyinren;
    /**
     * 分发
     */
    private String fenfaren;
    /**
     * 文件类型 1党务 2政务
     */
    private Short docType;
    /**
     * 发文审批内容
     */
    private byte[] sendDocNotion;
    /**
     * 公文范围
     */
    private String docRange;
    /**
     * 是否是联合发文
     */
    private String isunion;
    /**
     * 联合发文单位
     */
    private String uniondepts;
    /**
     * 联合发文单位意见
     */
    private String uniondeptscomments;
    /**
     * 是否强制交换pdf正文
     */
    private Short forceexchangepdf;
    /**
     * 是否上传了word套红正文
     */
    private Short isuploadwordformal;
    /**
     * 打字员
     */
    private String typist;
    /**
     * 打印日期
     */
    private LocalDateTime typistDate;
    /**
     * word套红正文上传时间
     */
    private LocalDateTime thwordupdate;
    /**
     * 厅/局长审批意见
     */
    private String fugleidea;
    /**
     * 厅/局意见人
     */
    private String fuglename;
    /**
     * 厅/局意见时间
     */
    private LocalDateTime fugledate;
    /**
     * 发文组织
     */
    private String organize;
    /**
     * 发文单位
     */
    private Long sendUnitid;
    /**
     * 发送状态 0为拟发 1为已发 2为退回
     */
    private Short sendStatus;
    /**
     * 受理文号
     */
    private String acceptNumber;
    /**
     * 受理申请时间
     */
    private LocalDateTime acceptStarttime;
    /**
     * 受理到期时间
     */
    private LocalDateTime acceptEndtime;
    /**
     * 受理延期后到期时间
     */
    private LocalDateTime acceptDelaytime;
    /**
     * 是否属于规范性文件,是1，否2
     */
    private Short isNormativeDocument;
    /**
     * 是否重要
     */
    private Short ifimportant;
    /**
     * 办公室秘书落实意见
     */
    private String secretaryimplementidea;
    /**
     * 办公室秘书
     */
    private String secretaryimplementideaPerson;
    /**
     * 办公室秘书落实意见日期
     */
    private LocalDateTime secretaryimplementideaDate;
    /**
     * 最小知悉范围
     */
    private Short miniknow;
    /**
     * 省厅便函选择类型结果
     */
    private String selecttype;
    /**
     * 是否已归档,1已归档，否则未归档
     */
    private Short ifpigeonhold;
    /**
     * 归档号
     */
    private String pigeonholeNum;
    /**
     * 归档序号
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