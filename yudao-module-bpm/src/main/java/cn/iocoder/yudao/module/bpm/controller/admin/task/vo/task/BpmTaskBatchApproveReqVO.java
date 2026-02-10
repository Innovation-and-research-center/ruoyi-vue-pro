package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 批量办结任务 Request VO")
@Data
public class BpmTaskBatchApproveReqVO {

    @Schema(description = "任务编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"task1\", \"task2\"]")
    @NotEmpty(message = "任务编号列表不能为空")
    private List<String> ids;

    @Schema(description = "审批意见", example = "批量办结")
    private String reason;
}