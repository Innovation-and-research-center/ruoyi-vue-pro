package cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyKzDO;

@Schema(description = "管理后台 - 行政复议新增/修改 Request VO")
@Data
public class XzfySaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "5236")
    private Long id;

    @Schema(description = "备用主键", example = "20927")
    private String xmGuid;

    @Schema(description = "来文号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "来文号不能为空")
    private String swWh;

    @Schema(description = "来文机关", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "来文机关不能为空")
    private String swJg;

    @Schema(description = "来文日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来文日期不能为空")
    private LocalDateTime swRq;

    @Schema(description = "申请人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "申请人不能为空")
    private String sqr;

    @Schema(description = "被申请人", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "被申请人不能为空")
    private String bsqr;

    @Schema(description = "第三人")
    private String dsr;

    @Schema(description = "土地坐落", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "土地坐落不能为空")
    private String tdZl;

    @Schema(description = "案件分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "案件分类不能为空")
    private String lb1;

    @Schema(description = "涉及事项", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "涉及事项不能为空")
    private String lb2;

    @Schema(description = "案件类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "案件类型不能为空")
    private String lb3;

    @Schema(description = "复议请求")
    private String fyNr;

    @Schema(description = "承办人")
    private String cbr;

    @Schema(description = "承办日期")
    private LocalDateTime cbRq;

    @Schema(description = "送复议机关日期")
    private LocalDateTime sfyjgRq;

    @Schema(description = "行政区（街道、村）")
    private String xzq;

    @Schema(description = "监督监管")
    private Short issupervise;

    @Schema(description = "办理时限")
    private LocalDateTime zhubandate;

    @Schema(description = "是否已寄件提醒")
    private Short mailTip;

    @Schema(description = "流程实例的编号", example = "14032")
    private String processInstanceId;

    @Schema(description = "行政复议扩展")
    private XzfyKzDO xzfyKz;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Schema(description = "下一节点审批人", example = "assist")
    private Map<String, List<Long>> nextNodeAssignees;

}