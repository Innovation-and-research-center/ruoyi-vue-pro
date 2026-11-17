package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@Schema(description = "管理后台 - 添加或移除角色成员")
public class RoleMemberUpdateReqVO {
    @NotNull @Positive
    private Long roleId;
    @NotNull @Positive
    private Long userId;
    @NotNull
    @Schema(description = "true 添加，false 移除")
    private Boolean assigned;
}
