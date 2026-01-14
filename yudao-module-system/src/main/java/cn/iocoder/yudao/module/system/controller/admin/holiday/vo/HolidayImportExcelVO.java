package cn.iocoder.yudao.module.system.controller.admin.holiday.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HolidayImportExcelVO {
    @ExcelProperty("日期")
    @NotEmpty(message = "假日日日期不能为空")
    private String settingDate;

    @ExcelProperty("是否是工作日")
    @NotNull
    @Min(0)
    @Max(1)
    private Short isworkday;

    @ExcelProperty("假日描述")
    private String   holiDesc;
}
