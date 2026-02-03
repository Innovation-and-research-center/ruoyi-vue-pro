package cn.iocoder.yudao.module.system.controller.admin.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 会议记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MeetingRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "28198")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "会议室主键", example = "26608")
    @ExcelProperty("会议室主键")
    private Long meetingRoomId;

    @Schema(description = "记录时间")
    @ExcelProperty("记录时间")
    private LocalDateTime recordTime;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private LocalDateTime endTime;

    @Schema(description = "预约人ID", example = "1458")
    @ExcelProperty("预约人ID")
    private Long userId;

    @Schema(description = "预约人姓名", example = "王五")
    @ExcelProperty("预约人姓名")
    private String staffName;

    @Schema(description = "预约人部门")
    @ExcelProperty("预约人部门")
    private String department;

    @Schema(description = "预约人电话")
    @ExcelProperty("预约人电话")
    private String telephone;

    @Schema(description = "参会人数")
    @ExcelProperty("参会人数")
    private Integer attendNumber;

    @Schema(description = "会议摘要")
    @ExcelProperty("会议摘要")
    private String meetingAbstract;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}