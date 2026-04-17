package cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo;

import cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo.XzssRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 行政复议 Response VO")
@Data
@ExcelIgnoreUnannotated
public class XzfyRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "5236")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "来文号")
    @ExcelProperty("来文号")
    private String swWh;

    @Schema(description = "来文机关")
    @ExcelProperty("来文机关")
    private String swJg;

    @Schema(description = "来文日期")
    @ExcelProperty("来文日期")
    private LocalDateTime swRq;

    @Schema(description = "申请人")
    @ExcelProperty("申请人")
    private String sqr;

    @Schema(description = "被申请人")
    @ExcelProperty("被申请人")
    private String bsqr;

    @Schema(description = "第三人")
    @ExcelProperty("第三人")
    private String dsr;

    @Schema(description = "土地坐落")
    @ExcelProperty("土地坐落")
    private String tdZl;

    @Schema(description = "类别一")
    @ExcelProperty("类别一")
    private String lb1;

    @Schema(description = "类别二")
    @ExcelProperty("类别二")
    private String lb2;

    @Schema(description = "类别三")
    @ExcelProperty("类别三")
    private String lb3;

    @Schema(description = "承办人")
    @ExcelProperty("承办人")
    private String cbr;

    @Schema(description = "承办日期")
    @ExcelProperty("承办日期")
    private LocalDateTime cbRq;

    @Schema(description = "送复议机关日期")
    @ExcelProperty("送复议机关日期")
    private LocalDateTime sfyjgRq;

    @Schema(description = "行政区（街道、村）")
    @ExcelProperty("行政区（街道、村）")
    private String xzq;

    @Schema(description = "监督监管")
    @ExcelProperty("监督监管")
    private Short issupervise;

    @Schema(description = "办理时限")
    @ExcelProperty("办理时限")
    private LocalDateTime zhubandate;

    @Schema(description = "是否已寄件提醒")
    @ExcelProperty("是否已寄件提醒")
    private Short mailTip;

    @Schema(description = "流程实例的编号", example = "14032")
    @ExcelProperty("流程实例的编号")
    private String processInstanceId;

    @Schema(description = "备用主键")
    @ExcelProperty("备用主键")
    private String xmGuid;

    @Schema(description = "关联的行政诉讼列表")
    private List<XzssRespVO> xzssList;

    private Short status;

    private String cancelReason;

}