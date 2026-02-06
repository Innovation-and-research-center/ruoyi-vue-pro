package cn.iocoder.yudao.module.system.controller.admin.userdept.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 用户部门关联新增/修改 Request VO")
@Data
public class UserDeptSaveReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21160")
    private Long id;

    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "21641")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "25906")
    @NotNull(message = "部门ID不能为空")
    private Long deptId;

}