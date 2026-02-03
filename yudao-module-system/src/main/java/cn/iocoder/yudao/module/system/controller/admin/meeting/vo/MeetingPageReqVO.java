package cn.iocoder.yudao.module.system.controller.admin.meeting.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 会议记录分页 Request VO")
@Data
public class MeetingPageReqVO extends PageParam {

    @Schema(description = "会议室主键", example = "26608")
    private Long meetingRoomId;

    @Schema(description = "记录时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] recordTime;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] endTime;

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

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}