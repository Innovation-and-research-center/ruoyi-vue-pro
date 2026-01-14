package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 外出请假补假 Response VO")
@Data
@ExcelIgnoreUnannotated
public class TimeExplainRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15124")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "人员编号", example = "16761")
    @ExcelProperty("人员编号")
    private Long userId;

    @Schema(description = "人员姓名", example = "王五")
    @ExcelProperty("人员姓名")
    private String userName;

    @Schema(description = "登记时间")
    @ExcelProperty("登记时间")
    private LocalDateTime checkDate;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime checkBegin;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private LocalDateTime checkEnd;

    @Schema(description = "审核状态（0审批中 1审核完毕 2删除）", example = "2")
    @ExcelProperty(value = "审核状态（0审批中 1审核完毕 2删除）", converter = DictConvert.class)
    @DictFormat("system_user_sex") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Long status;

    @Schema(description = "请假天数")
    @ExcelProperty("请假天数")
    private BigDecimal days;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    private String deptName;

    private String reason;

}