package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.RoleMemberRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.RoleMemberUpdateReqVO;
import cn.iocoder.yudao.module.system.service.permission.RoleMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/system/permission")
@Validated
@Tag(name = "管理后台 - 角色成员")
public class RoleMemberController {
    @Resource private RoleMemberService roleMemberService;

    @GetMapping("/role-members")
    @Operation(summary = "查看角色成员")
    @PreAuthorize("@ss.hasAnyPermissions('system:role:query', 'system:permission:assign-user-role')")
    public CommonResult<List<RoleMemberRespVO>> list(@RequestParam("roleId") @Positive Long roleId) {
        return success(roleMemberService.list(roleId));
    }

    @PutMapping("/role-member")
    @Operation(summary = "添加或移除角色成员")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Boolean> update(@Valid @RequestBody RoleMemberUpdateReqVO request) {
        roleMemberService.update(request.getRoleId(), request.getUserId(), request.getAssigned());
        return success(true);
    }
}
