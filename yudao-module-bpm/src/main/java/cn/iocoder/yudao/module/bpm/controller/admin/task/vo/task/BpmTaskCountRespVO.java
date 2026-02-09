package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BpmTaskCountRespVO {
    @Schema(description = "待办任务数量")
    private Long todoCount;

    @Schema(description = "已办任务数量")
    private Long doneCount;

    @Schema(description = "总数量 (待办 + 已办)")
    private Long totalCount;
}
