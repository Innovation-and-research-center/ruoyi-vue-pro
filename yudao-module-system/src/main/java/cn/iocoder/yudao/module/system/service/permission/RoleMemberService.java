package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.RoleMemberRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

@Service
public class RoleMemberService {
    @Resource private RoleMapper roleMapper;
    @Resource private UserRoleMapper userRoleMapper;
    @Resource private AdminUserService userService;
    @Resource private DeptService deptService;

    public List<RoleMemberRespVO> list(Long roleId) {
        if (roleMapper.selectById(roleId) == null) throw exception(ROLE_NOT_EXISTS);
        Set<Long> ids = userRoleMapper.selectListByRoleIds(Collections.singleton(roleId)).stream()
                .map(UserRoleDO::getUserId).collect(Collectors.toSet());
        if (ids.isEmpty()) return Collections.emptyList();
        // 包含已停用的成员，允许管理员清理其角色关联。
        List<AdminUserDO> users = userService.getUserList(ids);
        Map<Long, DeptDO> depts = deptService.getDeptMap(users.stream().map(AdminUserDO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Comparator<AdminUserDO> memberOrder = Comparator
                .comparing((AdminUserDO user) -> {
                    DeptDO dept = depts.get(user.getDeptId());
                    return dept == null ? null : dept.getSort();
                }, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AdminUserDO::getDeptId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AdminUserDO::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AdminUserDO::getId);
        return users.stream().sorted(memberOrder).map(user -> {
            RoleMemberRespVO member = new RoleMemberRespVO();
            member.setId(user.getId());
            member.setUsername(user.getUsername());
            member.setNickname(user.getNickname());
            member.setStatus(user.getStatus());
            DeptDO dept = depts.get(user.getDeptId());
            member.setDeptName(dept == null ? null : dept.getName());
            return member;
        }).collect(Collectors.toList());
    }

    @DSTransactional
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void update(Long roleId, Long userId, boolean assigned) {
        // 串行化同一角色的成员维护，防止重复添加；查询保留租户和逻辑删除过滤。
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getId, roleId).last("FOR UPDATE"));
        if (role == null) throw exception(ROLE_NOT_EXISTS);
        AdminUserDO user = userService.getUser(userId);
        if (user == null) throw exception(USER_NOT_EXISTS);
        if (assigned) {
            if (!CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()))
                throw exception(ROLE_IS_DISABLE, role.getName());
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus()))
                throw exception(USER_IS_DISABLE, user.getNickname());
            boolean exists = userRoleMapper.selectListByUserId(userId).stream()
                    .anyMatch(item -> roleId.equals(item.getRoleId()));
            if (!exists) {
                UserRoleDO member = new UserRoleDO();
                member.setRoleId(roleId);
                member.setUserId(userId);
                userRoleMapper.insert(member);
            }
        } else {
            // 只删除当前角色，不覆盖用户的其他角色，不删除用户账号。
            userRoleMapper.deleteListByUserIdAndRoleIdIds(userId, Collections.singleton(roleId));
        }
    }
}
