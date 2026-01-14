package cn.iocoder.yudao.module.system.controller.admin.holiday.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 节假日新增/修改 Request VO")
@Data
public class HolidaySaveReqVO {

    @Schema(description = "节假日内码", requiredMode = Schema.RequiredMode.REQUIRED, example = "170")
    private Long id;

    @Schema(description = "设置日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "设置日期不能为空")
    private LocalDateTime settingDate;

    @Schema(description = "是否是工作日", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否是工作日不能为空")
    private Short isworkday;

    @Schema(description = "假日描述")
    private String holiDesc;

}