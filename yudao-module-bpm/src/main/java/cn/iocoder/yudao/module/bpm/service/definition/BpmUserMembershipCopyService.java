package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserMembershipCopyReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.USER_GROUP_COPY_SOURCE_CHANGED;

@Service
public class BpmUserMembershipCopyService {
    @Resource private AdminUserService userService;
    @Resource private RoleService roleService;
    @Resource private PermissionService permissionService;
    @Resource private RoleMapper roleMapper;
    @Resource private UserRoleMapper userRoleMapper;
    @Resource private BpmUserGroupService groupService;
    @Resource private BpmUserGroupMembershipService membershipService;

    // 角色 Mapper 与用户组 JDBC 写入共用 Spring 事务，避免混用独立的多数据源事务。
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, allEntries = true,
            condition = "!#req.roleIds.isEmpty()")
    public void copy(BpmUserMembershipCopyReqVO req) {
        if (req.getTargetUserIds().contains(req.getSourceUserId())) throw exception(USER_ROLE_COPY_SELF);
        if (userService.getUser(req.getSourceUserId()) == null) throw exception(USER_NOT_EXISTS);
        userService.validateUserList(req.getTargetUserIds());
        // 与角色成员维护保持相同锁定顺序。先锁定角色，再验证来源关系，最后锁用户组。
        for (Long roleId : new TreeSet<>(req.getRoleIds())) {
            if (roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getId, roleId)
                    .last("FOR UPDATE")) == null) throw exception(ROLE_NOT_EXISTS);
        }
        if (!permissionService.getUserRoleIdListByUserId(req.getSourceUserId()).containsAll(req.getRoleIds()))
            throw exception(USER_ROLE_COPY_SOURCE_CHANGED);
        if (!membershipService.getGroupIdsByUser(req.getSourceUserId()).containsAll(req.getGroupIds()))
            throw exception(USER_GROUP_COPY_SOURCE_CHANGED);
        roleService.validateRoleList(req.getRoleIds());
        groupService.validUserGroups(req.getGroupIds());

        if (!req.getRoleIds().isEmpty()) {
            for (Long userId : new TreeSet<>(req.getTargetUserIds())) {
                Set<Long> existing = userRoleMapper.selectListByUserId(userId).stream()
                        .map(UserRoleDO::getRoleId).collect(Collectors.toSet());
                for (Long roleId : new TreeSet<>(req.getRoleIds())) {
                    if (existing.contains(roleId)) continue;
                    UserRoleDO row = new UserRoleDO(); row.setUserId(userId); row.setRoleId(roleId);
                    userRoleMapper.insert(row);
                }
            }
        }
        for (Long groupId : new TreeSet<>(req.getGroupIds())) {
            // 角色已自动带来的成员关系不重复写成手动关联。
            membershipService.appendMissingMembers(groupId, req.getTargetUserIds());
        }
    }
}
