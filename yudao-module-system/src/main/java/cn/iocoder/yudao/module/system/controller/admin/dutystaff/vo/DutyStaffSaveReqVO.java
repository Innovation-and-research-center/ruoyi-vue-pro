package cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 值班新增/修改 Request VO")
@Data
public class DutyStaffSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "32069")
    private Long id;

    @Schema(description = "值班日期")
    @NotNull(message = "值班日期不能为空")
    private LocalDateTime dutyDate;

    @Schema(description = "人员类型", example = "2")
    @NotEmpty(message = "人员类型不能为空")
    private String staffType;

    @Schema(description = "人员ID", example = "14288")
    private Long userId;

    @Schema(description = "人员姓名", example = "王五")
    @NotEmpty(message = "人员姓名不能为空")
    private String staffName;

    @Schema(description = "提醒次数", example = "19333")
    private Long smsCount;

}