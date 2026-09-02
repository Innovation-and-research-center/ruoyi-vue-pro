package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 调整指定部门内用户排序 Request VO")
@Data
public class UserUpdateDeptSortReqVO {

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "部门编号不能为空")
    private Long deptId;

    @Schema(description = "按目标顺序排列的用户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "用户编号列表不能为空")
    private List<Long> userIds;

}
