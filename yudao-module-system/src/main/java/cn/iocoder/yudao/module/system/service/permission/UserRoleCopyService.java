package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.Set;
import java.util.TreeSet;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

@Service
public class UserRoleCopyService {
    @Resource private AdminUserService userService;
    @Resource private RoleService roleService;
    @Resource private PermissionService permissionService;
    @Resource private RoleMemberService roleMemberService;

    /** 整批复制在同一事务内；只追加缺失关联，不替换目标用户已有角色。 */
    @DSTransactional
    public void copy(Long sourceUserId, Set<Long> targetUserIds, Set<Long> roleIds) {
        if (targetUserIds.contains(sourceUserId)) throw exception(USER_ROLE_COPY_SELF);
        if (userService.getUser(sourceUserId) == null) throw exception(USER_NOT_EXISTS);
        Set<Long> sourceRoles = permissionService.getUserRoleIdListByUserId(sourceUserId);
        if (!sourceRoles.containsAll(roleIds)) throw exception(USER_ROLE_COPY_SOURCE_CHANGED);
        // 全量校验通过后再写入，拒绝跨租户、已删除、停用的用户或角色。
        userService.validateUserList(targetUserIds);
        roleService.validateRoleList(roleIds);
        // 成员维护按角色加锁，固定获取顺序，避免两批复制交叉锁定不同角色。
        for (Long roleId : new TreeSet<>(roleIds)) {
            for (Long targetUserId : new TreeSet<>(targetUserIds)) {
                // 复用幂等添加和用户角色缓存失效，已有角色自动跳过。
                roleMemberService.update(roleId, targetUserId, true);
            }
        }
    }
}
