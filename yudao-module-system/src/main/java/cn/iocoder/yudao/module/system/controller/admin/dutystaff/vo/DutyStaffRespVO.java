package cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 值班 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DutyStaffRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "值班日期")
    @ExcelProperty("值班日期")
    private LocalDateTime dutyDate;

    @Schema(description = "人员类型", example = "2")
    @ExcelProperty(value = "人员类型", converter = DictConvert.class)
    @DictFormat("task_type") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String staffType;

    @Schema(description = "人员ID", example = "14288")
    @ExcelProperty("人员ID")
    private Long userId;

    @Schema(description = "人员姓名", example = "王五")
    @ExcelProperty("人员姓名")
    private String staffName;

    @Schema(description = "提醒次数", example = "19333")
    @ExcelProperty("提醒次数")
    private Long smsCount;

    @Schema(description = "部门名称", example = "科技部")
    @ExcelProperty("部门名称")
    private  String deptName;

}