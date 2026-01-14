package cn.iocoder.yudao.module.bpm.controller.admin.senddoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 发文 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SendDocRespVO {

    @Schema(description = "发文表主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "4356")
    @ExcelProperty("发文表主键")
    private Long id;

    @Schema(description = "发文草稿内容")
    @ExcelProperty("发文草稿内容")
    private byte[] sendDocDraft;

    @Schema(description = "发文结果内容")
    @ExcelProperty("发文结果内容")
    private byte[] sendDocResult;

    @Schema(description = "公文性质")
    @ExcelProperty("公文性质")
    private String docProperty;

    @Schema(description = "主送机关")
    @ExcelProperty("主送机关")
    private String primarySendDept;

    @Schema(description = "抄送机关")
    @ExcelProperty("抄送机关")
    private String copySendDept;

    @Schema(description = "抄报机关")
    @ExcelProperty("抄报机关")
    private String reportSendDept;

    @Schema(description = "抄送机关")
    @ExcelProperty("抄送机关")
    private String deliverSendDept;

    @Schema(description = "发文号")
    @ExcelProperty("发文号")
    private String sendDocNumber;

    @Schema(description = "发文日期")
    @ExcelProperty("发文日期")
    private LocalDateTime sendTime;

    @Schema(description = "签印份数", example = "6692")
    @ExcelProperty("签印份数")
    private String signPrintCount;

    @Schema(description = "保管期限")
    @ExcelProperty("保管期限")
    private String keepTerm;

    @Schema(description = "项目内码", example = "11492")
    @ExcelProperty("项目内码")
    private String projectId;

    @Schema(description = "主题")
    @ExcelProperty("主题")
    private String subject;

    @Schema(description = "紧急程度")
    @ExcelProperty("紧急程度")
    private String urgencyDegree;

    @Schema(description = "机密程度")
    @ExcelProperty("机密程度")
    private String secretDegree;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "文号")
    @ExcelProperty("文号")
    private Long docSequence;

    @Schema(description = "年份")
    @ExcelProperty("年份")
    private String year;

    @Schema(description = "主题词")
    @ExcelProperty("主题词")
    private String keyWords;

    @Schema(description = "发文单位")
    @ExcelProperty("发文单位")
    private String sendDept;

    @Schema(description = "拟稿人")
    @ExcelProperty("拟稿人")
    private String draftPerson;

    @Schema(description = "拟稿时间")
    @ExcelProperty("拟稿时间")
    private LocalDateTime draftDate;

    @Schema(description = "事业单位拟稿人意见")
    @ExcelProperty("事业单位拟稿人意见")
    private String draftIdea;

    @Schema(description = "拟稿人联系电话")
    @ExcelProperty("拟稿人联系电话")
    private String contactPhone;

    @Schema(description = "最终公文名称", example = "赵六")
    @ExcelProperty("最终公文名称")
    private String resultFileName;

    @Schema(description = "二级分类")
    @ExcelProperty("二级分类")
    private Long docSecondClass;

    @Schema(description = "一级分类")
    @ExcelProperty("一级分类")
    private Long docClass;

    @Schema(description = "办件时限")
    @ExcelProperty("办件时限")
    private LocalDateTime dealTimelimit;

    @Schema(description = "是否发布到外网")
    @ExcelProperty("是否发布到外网")
    private Short releasetoout;

    @Schema(description = "是否发布到专网")
    @ExcelProperty("是否发布到专网")
    private Short releasetospecial;

    @Schema(description = "是否发布到内网")
    @ExcelProperty("是否发布到内网")
    private Short releasetoin;

    @Schema(description = "最近修改发布人")
    @ExcelProperty("最近修改发布人")
    private String updatereleaseman;

    @Schema(description = "最近修改发布时间")
    @ExcelProperty("最近修改发布时间")
    private LocalDateTime updatereleasedate;

    @Schema(description = "内部传阅")
    @ExcelProperty("内部传阅")
    private String releasetoother;

    @Schema(description = "收文内码(收文转发文时用)", example = "30642")
    @ExcelProperty("收文内码(收文转发文时用)")
    private Long receiveDocId;

    @Schema(description = "正文稿子签出人", example = "1285")
    @ExcelProperty("正文稿子签出人")
    private Long checkoutUserId;

    @Schema(description = "签出时间")
    @ExcelProperty("签出时间")
    private LocalDateTime checkoutTime;

    @Schema(description = "办公室秘书意见")
    @ExcelProperty("办公室秘书意见")
    private String secretaryidea;

    @Schema(description = "办公室秘书")
    @ExcelProperty("办公室秘书")
    private String secretaryideaPerson;

    @Schema(description = "办公室秘书意见日期")
    @ExcelProperty("办公室秘书意见日期")
    private LocalDateTime secretaryideaDate;

    @Schema(description = "办公室主任意见")
    @ExcelProperty("办公室主任意见")
    private String directoridea;

    @Schema(description = "办公室主任")
    @ExcelProperty("办公室主任")
    private String directorideaPerson;

    @Schema(description = "办公室主任意见日期")
    @ExcelProperty("办公室主任意见日期")
    private LocalDateTime directorideaDate;

    @Schema(description = "能否查询")
    @ExcelProperty("能否查询")
    private Short couldquery;

    @Schema(description = "表单页面")
    @ExcelProperty("表单页面")
    private String vdformpage;

    @Schema(description = "能否访问")
    @ExcelProperty("能否访问")
    private Short defaultAccess;

    @Schema(description = "打印条形码")
    @ExcelProperty("打印条形码")
    private Short printbarcode;

    @Schema(description = "信息公开")
    @ExcelProperty("信息公开")
    private String inforelease;

    @Schema(description = "不予公开理由")
    @ExcelProperty("不予公开理由")
    private String noreleasecause;

    @Schema(description = "附件路径")
    @ExcelProperty("附件路径")
    private String attachFilePath;

    @Schema(description = "判断正文草稿，审批搞，套红正文是否存在")
    @ExcelProperty("判断正文草稿，审批搞，套红正文是否存在")
    private String filetag;

    @Schema(description = "拟稿单位")
    @ExcelProperty("拟稿单位")
    private String draftDept;

    @Schema(description = "主办部门")
    @ExcelProperty("主办部门")
    private String responsibleDept;

    @Schema(description = "是否导出")
    @ExcelProperty("是否导出")
    private Short isexported;

    @Schema(description = "是否可以导出")
    @ExcelProperty("是否可以导出")
    private Short iscanexport;

    @Schema(description = "自定义编号")
    @ExcelProperty("自定义编号")
    private String customnumber;

    @Schema(description = "拟稿人用户ID", example = "30718")
    @ExcelProperty("拟稿人用户ID")
    private Long draftPersonUserid;

    @Schema(description = "是否需要TIF文件")
    @ExcelProperty("是否需要TIF文件")
    private Short ishavetif;

    @Schema(description = "公文交换主送机关")
    @ExcelProperty("公文交换主送机关")
    private String exPrimarySendDept;

    @Schema(description = "公文交换抄送机关")
    @ExcelProperty("公文交换抄送机关")
    private String exCopySendDept;

    @Schema(description = "是否行政规范性文件")
    @ExcelProperty("是否行政规范性文件")
    private String ispolicynorm;

    @Schema(description = "法制机构意见")
    @ExcelProperty("法制机构意见")
    private String fzjgyjTz;

    @Schema(description = "法制机构意见用户")
    @ExcelProperty("法制机构意见用户")
    private String fzjgyjTzPerson;

    @Schema(description = "法制机构意见日期")
    @ExcelProperty("法制机构意见日期")
    private LocalDateTime fzjgyjTzDate;

    @Schema(description = "法制编号2")
    @ExcelProperty("法制编号2")
    private String customnumber2;

    @Schema(description = "法制编号3")
    @ExcelProperty("法制编号3")
    private String customnumber3;

    @Schema(description = "会签部门")
    @ExcelProperty("会签部门")
    private String hqdept;

    @Schema(description = "会签人员姓名", example = "赵六")
    @ExcelProperty("会签人员姓名")
    private String hqname;

    @Schema(description = "会签意见")
    @ExcelProperty("会签意见")
    private String hqidea;

    @Schema(description = "会签时间")
    @ExcelProperty("会签时间")
    private LocalDateTime hqdate;

    @Schema(description = "签发人", example = "李四")
    @ExcelProperty("签发人")
    private String issuedName;

    @Schema(description = "校对")
    @ExcelProperty("校对")
    private String proofreader;

    @Schema(description = "校对日期")
    @ExcelProperty("校对日期")
    private LocalDateTime proofreaderDate;

    @Schema(description = "印制")
    @ExcelProperty("印制")
    private String yinzhiren;

    @Schema(description = "用印")
    @ExcelProperty("用印")
    private String yongyinren;

    @Schema(description = "分发")
    @ExcelProperty("分发")
    private String fenfaren;

    @Schema(description = "文件类型 1党务 2政务", example = "2")
    @ExcelProperty("文件类型 1党务 2政务")
    private Short docType;

    @Schema(description = "发文审批内容")
    @ExcelProperty("发文审批内容")
    private byte[] sendDocNotion;

    @Schema(description = "公文范围")
    @ExcelProperty("公文范围")
    private String docRange;

    @Schema(description = "是否是联合发文")
    @ExcelProperty("是否是联合发文")
    private String isunion;

    @Schema(description = "联合发文单位")
    @ExcelProperty("联合发文单位")
    private String uniondepts;

    @Schema(description = "联合发文单位意见")
    @ExcelProperty("联合发文单位意见")
    private String uniondeptscomments;

    @Schema(description = "是否强制交换pdf正文")
    @ExcelProperty("是否强制交换pdf正文")
    private Short forceexchangepdf;

    @Schema(description = "是否上传了word套红正文")
    @ExcelProperty("是否上传了word套红正文")
    private Short isuploadwordformal;

    @Schema(description = "打字员")
    @ExcelProperty("打字员")
    private String typist;

    @Schema(description = "打印日期")
    @ExcelProperty("打印日期")
    private LocalDateTime typistDate;

    @Schema(description = "word套红正文上传时间")
    @ExcelProperty("word套红正文上传时间")
    private LocalDateTime thwordupdate;

    @Schema(description = "厅/局长审批意见")
    @ExcelProperty("厅/局长审批意见")
    private String fugleidea;

    @Schema(description = "厅/局意见人", example = "李四")
    @ExcelProperty("厅/局意见人")
    private String fuglename;

    @Schema(description = "厅/局意见时间")
    @ExcelProperty("厅/局意见时间")
    private LocalDateTime fugledate;

    @Schema(description = "发文组织")
    @ExcelProperty("发文组织")
    private String organize;

    @Schema(description = "发文单位", example = "26029")
    @ExcelProperty("发文单位")
    private Long sendUnitid;

    @Schema(description = "发送状态 0为拟发 1为已发 2为退回", example = "1")
    @ExcelProperty("发送状态 0为拟发 1为已发 2为退回")
    private Short sendStatus;

    @Schema(description = "受理文号")
    @ExcelProperty("受理文号")
    private String acceptNumber;

    @Schema(description = "受理申请时间")
    @ExcelProperty("受理申请时间")
    private LocalDateTime acceptStarttime;

    @Schema(description = "受理到期时间")
    @ExcelProperty("受理到期时间")
    private LocalDateTime acceptEndtime;

    @Schema(description = "受理延期后到期时间")
    @ExcelProperty("受理延期后到期时间")
    private LocalDateTime acceptDelaytime;

    @Schema(description = "是否属于规范性文件,是1，否2")
    @ExcelProperty("是否属于规范性文件,是1，否2")
    private Short isNormativeDocument;

    @Schema(description = "是否重要")
    @ExcelProperty("是否重要")
    private Short ifimportant;

    @Schema(description = "办公室秘书落实意见")
    @ExcelProperty("办公室秘书落实意见")
    private String secretaryimplementidea;

    @Schema(description = "办公室秘书")
    @ExcelProperty("办公室秘书")
    private String secretaryimplementideaPerson;

    @Schema(description = "办公室秘书落实意见日期")
    @ExcelProperty("办公室秘书落实意见日期")
    private LocalDateTime secretaryimplementideaDate;

    @Schema(description = "最小知悉范围")
    @ExcelProperty("最小知悉范围")
    private Short miniknow;

    @Schema(description = "省厅便函选择类型结果", example = "2")
    @ExcelProperty("省厅便函选择类型结果")
    private String selecttype;

    @Schema(description = "是否已归档,1已归档，否则未归档")
    @ExcelProperty("是否已归档,1已归档，否则未归档")
    private Short ifpigeonhold;

    @Schema(description = "归档号")
    @ExcelProperty("归档号")
    private String pigeonholeNum;

    @Schema(description = "归档序号")
    @ExcelProperty("归档序号")
    private Long pigeonholeSeq;

    @Schema(description = "归档结果")
    @ExcelProperty("归档结果")
    private String pigeonholeResult;

    @Schema(description = "流程实例的编号", example = "29028")
    @ExcelProperty("流程实例的编号")
    private String processInstanceId;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}