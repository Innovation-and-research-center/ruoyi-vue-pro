package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 收文保存 Response VO")
@Data
@Accessors(chain = true)
public class ReceiveDocSaveRespVO {

    @Schema(description = "收文 id", requiredMode = Schema.RequiredMode.REQUIRED, example = "29970")
    private Long id;

    @Schema(description = "流程实例编号", example = "receive_2026062200003")
    private String processInstanceId;

    @Schema(description = "来文登记任务编号")
    private String taskId;

}
