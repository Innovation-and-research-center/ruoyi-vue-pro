package cn.iocoder.yudao.module.system.controller.admin.userdept.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Schema(description = "管理后台 - 赋予用户关联部门 Request VO")
@Data
public class UserAssignDeptReqVO {
    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "部门ID集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Long> deptIds;
}
