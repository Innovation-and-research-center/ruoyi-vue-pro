package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 请假保存 Response VO")
@Data
@Accessors(chain = true)
public class LeaveSaveRespVO {

    @Schema(description = "请假申请 id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "流程实例编号")
    private String processInstanceId;

    @Schema(description = "请假登记任务编号")
    private String taskId;

}
