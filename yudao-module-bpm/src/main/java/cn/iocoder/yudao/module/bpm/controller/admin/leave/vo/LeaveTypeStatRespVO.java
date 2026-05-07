package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveTypeStatRespVO {
    @Schema(description = "请假类型(字典值)", example = "1")
    private String qxjType;

    @Schema(description = "请假次数", example = "3")
    private Integer leaveCount;

    @Schema(description = "总共天数", example = "5.5")
    private BigDecimal totalDays;
}
