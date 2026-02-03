package cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;


@Data
public class BookingSaveReqVO {
    private Long id; // 如果是修改

    @NotNull(message = "会议室不能为空")
    private Long meetingRoomId;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-02-03 11:00:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    // 序列化成JSON时（虽然是ReqVO，但如果作为返回值或者是Feign调用时有用）
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-02-03 11:30:00")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND, timezone = "GMT+8")
    private LocalDateTime endTime;

    private String meetingAbstract; // 会议摘要
    private Integer attendNumber; // 人数
}
