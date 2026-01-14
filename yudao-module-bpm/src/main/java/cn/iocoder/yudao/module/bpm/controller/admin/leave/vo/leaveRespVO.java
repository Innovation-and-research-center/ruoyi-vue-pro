package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 假期申请审批 Response VO")
@Data
@ExcelIgnoreUnannotated
public class LeaveRespVO {

    @Schema(description = "假期申请审批内码", requiredMode = Schema.RequiredMode.REQUIRED, example = "29970")
    @ExcelProperty("假期申请审批内码")
    private Long id;

    @Schema(description = "申请时间")
    @ExcelProperty("申请时间")
    private LocalDateTime applyDate;

    @Schema(description = "开始时间(请假时间段中最小的时间)")
    @ExcelProperty("开始时间(请假时间段中最小的时间)")
    private LocalDateTime qxjStartDate;

    @Schema(description = "结束时间(请假时间段中最大的时间)")
    @ExcelProperty("结束时间(请假时间段中最大的时间)")
    private LocalDateTime qxjEndDate;

    @Schema(description = "请（休）假种类", example = "1")
    @ExcelProperty("请（休）假种类")
    private Integer qxjType;

    @Schema(description = "事假理由", example = "不好")
    @ExcelProperty("事假理由")
    private String sjReason;

    @Schema(description = "共计天数")
    @ExcelProperty("共计天数")
    private BigDecimal totalTs;

    @Schema(description = "共计天数")
    private String filepath;

    private String deptName;




}