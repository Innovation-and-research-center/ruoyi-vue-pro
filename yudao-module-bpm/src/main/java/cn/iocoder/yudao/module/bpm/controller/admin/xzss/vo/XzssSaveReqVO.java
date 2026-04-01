package cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo;

import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssKzDO;

@Schema(description = "管理后台 - 行政诉讼新增/修改 Request VO")
@Data
public class XzssSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1284")
    private Long id;

    @Schema(description = "备用主键", example = "18350")
    private String xmGuid;

    @Schema(description = "来文号")
    private String swWh;

    @Schema(description = "来文机关")
    private String swJg;

    @Schema(description = "收文日期")
    private LocalDateTime swRq;

    @Schema(description = "原告")
    private String sqr;

    @Schema(description = "被告")
    private String bsqr;

    @Schema(description = "第三人")
    private String dsr;

    @Schema(description = "土地坐落")
    private String tdZl;

    @Schema(description = "诉讼类型：1一审，2二审，3再审，如果多次，继续记录")
    private Short ssLx;

    @Schema(description = "复议项目主键", example = "28061")
    private String fyGuid;

    @Schema(description = "复议案号")
    private String fyAh;

    @Schema(description = "上一审项目主键", example = "7215")
    private String ssGuid;

    @Schema(description = "上一审案号")
    private String ssAh;

    @Schema(description = "类别一")
    private String lb1;

    @Schema(description = "类别二")
    private String lb2;

    @Schema(description = "类别三")
    private String lb3;

    @Schema(description = "类别四")
    private String lb4;

    @Schema(description = "类别五")
    private String lb5;

    @Schema(description = "复议请求")
    private String ssNr;

    @Schema(description = "承办人")
    private String cbr;

    @Schema(description = "承办日期")
    private LocalDateTime cbRq;

    @Schema(description = "送法院日期")
    private LocalDateTime sfyjgRq;

    @Schema(description = "上诉人")
    private String ssr;

    @Schema(description = "被上诉人")
    private String bssr;

    @Schema(description = "再审申请人")
    private String zssqr;

    @Schema(description = "再审被申请人")
    private String zsbsqr;

    @Schema(description = "监督监管")
    private Short issupervise;

    @Schema(description = "办理时限")
    private LocalDateTime zhubandate;

    @Schema(description = "诉讼内容")
    private String ssnr;

    @Schema(description = "是否已寄件提醒")
    private Short mailTip;

    @Schema(description = "流程实例的编号", example = "30539")
    private String processInstanceId;

    @Schema(description = "行政诉讼拓展")
    private XzssKzDO xzssKz;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Schema(description = "下一节点审批人", example = "assist")
    private Map<String, List<Long>> nextNodeAssignees;

    @Schema(description = "附件列表")
    private List<CommentAttachDO> fileList;

}