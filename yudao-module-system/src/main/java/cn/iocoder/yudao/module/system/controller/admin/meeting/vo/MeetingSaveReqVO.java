package cn.iocoder.yudao.module.system.controller.admin.meeting.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会议记录新增/修改 Request VO")
@Data
public class MeetingSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "28198")
    private Long id;

    @Schema(description = "会议室主键", example = "26608")
    private Long meetingRoomId;

    @Schema(description = "记录时间")
    private LocalDateTime recordTime;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "预约人ID", example = "1458")
    private Long userId;

    @Schema(description = "预约人姓名", example = "王五")
    private String staffName;

    @Schema(description = "预约人部门")
    private String department;

    @Schema(description = "预约人电话")
    private String telephone;

    @Schema(description = "参会人数")
    private Integer attendNumber;

    @Schema(description = "会议摘要")
    private String meetingAbstract;

}