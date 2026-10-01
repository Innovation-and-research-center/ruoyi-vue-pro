package cn.iocoder.yudao.module.bpm.controller.admin.task;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessViewScopeService;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/bpm/process-view-scope")
@Validated
public class BpmProcessViewScopeController {

    @Resource
    private BpmProcessViewScopeService service;
    @Resource
    private RoleApi roleApi;

    @GetMapping("/role-keys")
    @Operation(summary = "获取角色可查看的办件类型")
    @PreAuthorize("@ss.hasPermission('bpm:category:update')")
    public CommonResult<List<String>> getRoleKeys(@RequestParam("roleId") Long roleId) {
        return success(service.getRoleKeys(roleId));
    }

    @PutMapping("/role-keys")
    @Operation(summary = "设置角色可查看的办件类型")
    @PreAuthorize("@ss.hasPermission('bpm:category:update')")
    public CommonResult<Boolean> setRoleKeys(@Valid @RequestBody SaveReqVO reqVO) {
        roleApi.validRoleList(Collections.singleton(reqVO.getRoleId()));
        service.setRoleKeys(reqVO.getRoleId(), reqVO.getProcessDefinitionKeys());
        return success(true);
    }

    @Data
    public static class SaveReqVO {
        @NotNull
        private Long roleId;
        @NotNull
        private List<@NotBlank @Size(max = 128) String> processDefinitionKeys;
    }
}
