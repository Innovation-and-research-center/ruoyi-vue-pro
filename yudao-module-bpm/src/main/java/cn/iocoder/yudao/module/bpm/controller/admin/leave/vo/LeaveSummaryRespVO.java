package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "管理后台 - 请假统计 Response VO")
public class LeaveSummaryRespVO {
    @Schema(description = "用户ID", example = "1024")
    private Long userId;

    @Schema(description = "用户昵称", example = "芋道源码")
    private String nickname;

    @Schema(description = "部门名称", example = "研发部")
    private String deptName;

    @Schema(description = "请假类型（字典值）", example = "1")
    private Integer qxjType;

    @Schema(description = "共计天数", example = "5.5")
    private BigDecimal totalDays;

    @Schema(description = "审批中的天数", example = "2.0")
    private BigDecimal runningDays; // 【新增字段】用于接收审批中的天数

    @Schema(description = "请假次数", example = "3")
    private Integer count;
}
