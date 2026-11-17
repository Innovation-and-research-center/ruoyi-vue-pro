package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.Set;

@Data
@Schema(description = "管理后台 - 将选中的来源角色追加给其他用户")
public class PermissionCopyUserRolesReqVO {
    @NotNull @Positive
    private Long sourceUserId;

    @NotEmpty @Size(max = 50)
    private Set<@NotNull @Positive Long> targetUserIds;

    @NotEmpty @Size(max = 100)
    private Set<@NotNull @Positive Long> roleIds;
}
