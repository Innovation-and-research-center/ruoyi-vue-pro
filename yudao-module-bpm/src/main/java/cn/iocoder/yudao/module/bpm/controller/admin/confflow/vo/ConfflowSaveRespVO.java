package cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 会议报告单保存 Response VO")
@Data
@Accessors(chain = true)
public class ConfflowSaveRespVO {

    @Schema(description = "会议报告单 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "流程实例编号")
    private String processInstanceId;

    @Schema(description = "科室登记任务编号")
    private String taskId;

}
