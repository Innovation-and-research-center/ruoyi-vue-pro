package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.dal.mysql.processInstance.BpmProcessViewScopeMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;

@Service
public class BpmProcessViewScopeService {

    @Resource
    private BpmProcessViewScopeMapper mapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;

    public List<String> getRoleKeys(Long roleId) {
        return mapper.selectKeysByRoleId(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setRoleKeys(Long roleId, Collection<String> keys) {
        mapper.deleteByRoleId(roleId);
        keys.stream().distinct().forEach(key -> mapper.insert(roleId, key));
    }

    public List<RoleDO> getEnabledUserRoles(Long userId) {
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserIdFromCache(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleService.getRoleListFromCache(roleIds).stream()
                .filter(Objects::nonNull)
                .filter(role -> ENABLE.getStatus().equals(role.getStatus()))
                .collect(Collectors.toList());
    }

    public Set<String> getRoleKeys(Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(mapper.selectKeysByRoleIds(roleIds));
    }
}
