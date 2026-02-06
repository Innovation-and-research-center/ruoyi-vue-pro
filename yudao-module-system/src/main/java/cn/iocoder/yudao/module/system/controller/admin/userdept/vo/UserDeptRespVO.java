package cn.iocoder.yudao.module.system.controller.admin.userdept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 用户部门关联 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UserDeptRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21160")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21641")
    @ExcelProperty("用户ID")
    private Long userId;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "25906")
    @ExcelProperty("部门ID")
    private Long deptId;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}