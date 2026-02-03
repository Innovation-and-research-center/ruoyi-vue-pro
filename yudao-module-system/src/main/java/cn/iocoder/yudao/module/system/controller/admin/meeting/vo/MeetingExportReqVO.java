package cn.iocoder.yudao.module.system.controller.admin.meeting.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;
@Schema(description = "管理后台 - 会议室导出 Request VO")
@Data
public class MeetingExportReqVO {
    @Schema(description = "年份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "年份不能为空")
    private Integer year;

    @Schema(description = "月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "月份不能为空")
    private Integer month;
}
