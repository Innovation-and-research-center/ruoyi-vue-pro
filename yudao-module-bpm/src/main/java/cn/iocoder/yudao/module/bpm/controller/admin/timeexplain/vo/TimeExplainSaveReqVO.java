package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 外出请假补假新增/修改 Request VO")
@Data
public class TimeExplainSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15124")
    private Long id;

    @Schema(description = "登记时间")
    private LocalDateTime checkDate;

    @Schema(description = "开始时间")
    private LocalDateTime checkBegin;

    @Schema(description = "开始时段",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时段不能为空")
    private String startPeriod;

    @Schema(description = "结束时间")
    private LocalDateTime checkEnd;

    @Schema(description = "结束时段",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时段不能为空")
    private String endPeriod;

    @Schema(description = "原因说明", example = "不好")
    private String reason;

    @Schema(description = "审核状态（0审批中 1审核完毕 2删除）", example = "2")
    private Long status;

    @Schema(description = "请假天数")
    private BigDecimal days;

    @Schema(description = "文件路径")
    private String filepath;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    /**
     * 出发地
     */
    @Schema(description = "出发地")
    private String startPlace;
    /**
     * 目的地
     */
    @Schema(description = "目的地")
    private String endPlace;

    @Schema(description = "下一节点审批人", example = "assist")
    private Map<String, List<Long>> nextNodeAssignees;



}