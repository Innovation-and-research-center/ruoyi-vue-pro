package cn.iocoder.yudao.module.system.controller.admin.holiday.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 节假日 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HolidayRespVO {

    @Schema(description = "节假日内码", requiredMode = Schema.RequiredMode.REQUIRED, example = "170")
    @ExcelProperty("节假日内码")
    private Long id;

    @Schema(description = "设置日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("设置日期")
    private LocalDateTime settingDate;

    @Schema(description = "是否是工作日", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "是否是工作日", converter = DictConvert.class)
    @DictFormat("infra_boolean_int") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Short isworkday;

    @Schema(description = "假日描述")
    @ExcelProperty("假日描述")
    private String holiDesc;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}