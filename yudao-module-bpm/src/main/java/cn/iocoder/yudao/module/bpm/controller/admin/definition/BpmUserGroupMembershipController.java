package cn.iocoder.yudao.module.bpm.controller.admin.definition;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupMembershipService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupService;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ROLE_NOT_EXISTS;

@Tag(name = "管理后台 - 用户角色与用户组")
@RestController
@RequestMapping("/bpm/user-group/membership")
public class BpmUserGroupMembershipController {

    @Resource
    private BpmUserGroupMembershipService membershipService;
    @Resource
    private BpmUserGroupService groupService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private AdminUserService userService;

    @GetMapping("/user")
    @Operation(summary = "查看用户的角色和用户组及成员来源")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserMembershipResp> getUserMembership(@RequestParam("userId") Long userId) {
        checkUser(userId);
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        Set<Long> groupIds = membershipService.getGroupIdsByUser(userId);
        Map<Long, Set<Long>> roleSources = membershipService.getRoleSourcesByUser(userId);
        Set<Long> manualGroups = membershipService.getManualGroupIdsByUser(userId);
        Map<Long, String> roleNames = roleIds.isEmpty() ? Collections.emptyMap() : roleService.getRoleList(roleIds)
                .stream().collect(Collectors.toMap(RoleDO::getId, RoleDO::getName));
        Map<Long, String> groupNames = groupIds.isEmpty() ? Collections.emptyMap() : groupService.getUserGroupList(groupIds)
                .stream().collect(Collectors.toMap(BpmUserGroupDO::getId, BpmUserGroupDO::getName));
        UserMembershipResp resp = new UserMembershipResp();
        resp.setUserId(userId);
        resp.setRoleCount(roleIds.size());
        resp.setGroupCount(groupIds.size());
        resp.setRoles(roleIds.stream().map(id -> new IdName(id, roleNames.get(id))).collect(Collectors.toList()));
        List<GroupMembership> groups = new ArrayList<>();
        for (Long id : groupIds) {
            GroupMembership group = new GroupMembership();
            group.setId(id);
            group.setName(groupNames.get(id));
            group.setManual(manualGroups.contains(id));
            group.setRoleIds(roleSources.getOrDefault(id, Collections.emptySet()));
            groups.add(group);
        }
        resp.setGroups(groups);
        return success(resp);
    }

    @PutMapping("/user-manual-groups")
    @Operation(summary = "从用户入口维护手动加入的用户组")
    @PreAuthorize("@ss.hasPermission('bpm:user-group:update')")
    public CommonResult<Boolean> updateUserManualGroups(@Valid @RequestBody UserManualGroupsReq req) {
        checkUser(req.getUserId());
        Set<Long> newGroupIds = new HashSet<>(req.getGroupIds());
        newGroupIds.removeAll(membershipService.getManualGroupIdsByUser(req.getUserId()));
        groupService.validUserGroups(newGroupIds);
        membershipService.replaceManualGroups(req.getUserId(), req.getGroupIds());
        return success(true);
    }

    @GetMapping("/role-groups")
    @Operation(summary = "查看角色映射的用户组")
    @PreAuthorize("@ss.hasPermission('bpm:user-group:query')")
    public CommonResult<Set<Long>> getRoleGroups(@RequestParam("roleId") Long roleId) {
        return success(membershipService.getMappedGroupIds(roleId));
    }

    @PutMapping("/role-groups")
    @Operation(summary = "配置角色映射的用户组并同步现有用户")
    @PreAuthorize("@ss.hasPermission('bpm:user-group:update')")
    public CommonResult<Boolean> updateRoleGroups(@Valid @RequestBody RoleGroupsReq req) {
        if (roleService.getRole(req.getRoleId()) == null) {
            throw exception(ROLE_NOT_EXISTS);
        }
        Set<Long> newGroupIds = new HashSet<>(req.getGroupIds());
        newGroupIds.removeAll(membershipService.getMappedGroupIds(req.getRoleId()));
        groupService.validUserGroups(newGroupIds);
        membershipService.replaceRoleMappings(req.getRoleId(), req.getGroupIds());
        return success(true);
    }

    private void checkUser(Long userId) {
        if (userService.getUser(userId) == null) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    @Data
    public static class UserManualGroupsReq {
        @NotNull
        private Long userId;
        @NotNull
        private Set<Long> groupIds;
    }

    @Data
    public static class RoleGroupsReq {
        @NotNull
        private Long roleId;
        @NotNull
        private Set<Long> groupIds;
    }

    @Data
    public static class UserMembershipResp {
        private Long userId;
        private Integer roleCount;
        private Integer groupCount;
        private List<IdName> roles;
        private List<GroupMembership> groups;
    }

    @Data
    public static class IdName {
        private final Long id;
        private final String name;
    }

    @Data
    public static class GroupMembership {
        private Long id;
        private String name;
        private boolean manual;
        private Set<Long> roleIds;
    }
}
