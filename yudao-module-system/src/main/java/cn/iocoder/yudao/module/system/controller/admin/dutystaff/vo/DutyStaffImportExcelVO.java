package cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DutyStaffImportExcelVO {
    @ExcelProperty("日期")
    @NotEmpty(message = "值班日期不能为空")
    private String dutyDate;

    @ExcelProperty("值周领导")
    @NotEmpty(message = "值周领导不能为空")
    private String  leader;

    @ExcelProperty("值班人员")
    @NotEmpty(message = "值班人员不能为空")
    private String   staff;
}
