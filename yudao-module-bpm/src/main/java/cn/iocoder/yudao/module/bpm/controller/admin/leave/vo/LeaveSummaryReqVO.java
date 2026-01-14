package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@Schema(description = "管理后台 - 请假统计 Request VO")
public class LeaveSummaryReqVO {

    @Schema(description = "年份，必填用于计算时间范围", example = "2023")
    private Integer year;

    @Schema(description = "月份，选填。不填则统计全年", example = "10")
    private Integer month;

    @Schema(description = "部门ID，选填", example = "100")
    private Long deptId;

    @Schema(description = "用户ID，选填", example = "1")
    private Long userId;

    @Schema(description = "请假类型，选填", example = "1")
    private Integer qxjType;

    // --- 下面的字段由后端 Service 计算后填入，前端不需要传 ---

    @Schema(hidden = true)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime beginTime;

    @Schema(hidden = true)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;
}