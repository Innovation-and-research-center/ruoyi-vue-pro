package cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 会议报告单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ConfflowRespVO {
    @Schema(description = "会议记录id", requiredMode = Schema.RequiredMode.REQUIRED, example = "29970")
    @ExcelProperty("会议记录 id")
    private Long id;

    @Schema(description = "申请人ID", example = "10425")
    @ExcelProperty("申请人ID")
    private Long userId;

    @Schema(description = "申请人", example = "李四")
    @ExcelProperty("申请人")
    private String userName;

    @Schema(description = "申请人部门ID", example = "15842")
    @ExcelProperty("申请人部门ID")
    private Long deptId;

    @Schema(description = "申请人部门", example = "李四")
    @ExcelProperty("申请人部门")
    private String deptName;

    @Schema(description = "申请日期")
    @ExcelProperty("申请日期")
    private LocalDateTime applyDate;

    @Schema(description = "会议时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("会议时间")
    private LocalDateTime startDate;

    @Schema(description = "会议名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("会议名称")
    private String title;

    @Schema(description = "提议内容")
    @ExcelProperty("提议内容")
    private String content;

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "会议地点", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("会议地点")
    private String venue;

    @Schema(description = "我局参会科室")
    @ExcelProperty("我局参会科室")
    private String offerUnit;

    @Schema(description = "我局参会人员")
    @ExcelProperty("我局参会人员")
    private String offerPerson;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    private Long creator;

    @Schema(description = "附件路径")
    private String attachFilePath;

    @Schema(description = "参会人员会议情况及建议")
    private String situation;

    @Schema(description = "召集单位及召集人")
    private String joinUnit;

    private Short status;

    private String cancelReason;

    @Schema(description = "附件列表")
    private List<ConfflowAttachRespVO> fileList;

}