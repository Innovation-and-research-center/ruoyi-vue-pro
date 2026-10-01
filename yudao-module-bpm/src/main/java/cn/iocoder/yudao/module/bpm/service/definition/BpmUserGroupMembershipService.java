package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 手动成员持久化；角色成员由当前角色关系与映射实时计算。 */
@Service
public class BpmUserGroupMembershipService {

    @Resource
    private JdbcTemplate jdbc;

    private Long tenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }

    public Set<Long> getUserIds(Collection<Long> groupIds) {
        Map<Long, Set<Long>> byGroup = getUsersByGroups(groupIds);
        Set<Long> result = new LinkedHashSet<>();
        byGroup.values().forEach(result::addAll);
        return result;
    }

    public Map<Long, Set<Long>> getUsersByGroups(Collection<Long> groupIds) {
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        if (groupIds == null || groupIds.isEmpty()) {
            return result;
        }
        String placeholders = placeholders(groupIds.size());
        List<Object> args = args(groupIds);
        jdbc.query("SELECT m.group_id, m.user_id FROM bpm_user_group_member m "
                + "JOIN system_users u ON u.id = m.user_id AND u.tenant_id = m.tenant_id AND u.deleted = 0 "
                + "WHERE m.tenant_id = ? AND m.group_id IN (" + placeholders + ")", rs -> {
            result.computeIfAbsent(rs.getLong(1), ignored -> new LinkedHashSet<>()).add(rs.getLong(2));
        }, args.toArray());
        jdbc.query("SELECT map.group_id, ur.user_id FROM bpm_user_group_role_map map "
                + "JOIN system_user_role ur ON ur.role_id = map.role_id AND ur.tenant_id = map.tenant_id AND ur.deleted = 0 "
                + "JOIN system_role r ON r.id = map.role_id AND r.tenant_id = map.tenant_id AND r.deleted = 0 AND r.status = 0 "
                + "JOIN system_users u ON u.id = ur.user_id AND u.tenant_id = ur.tenant_id AND u.deleted = 0 "
                + "WHERE map.tenant_id = ? AND map.group_id IN (" + placeholders + ")", rs -> {
            result.computeIfAbsent(rs.getLong(1), ignored -> new LinkedHashSet<>()).add(rs.getLong(2));
        }, args.toArray());
        return result;
    }

    public Set<Long> getGroupIdsByUser(Long userId) {
        Set<Long> result = getManualGroupIdsByUser(userId);
        result.addAll(jdbc.queryForList("SELECT DISTINCT map.group_id FROM bpm_user_group_role_map map "
                + "JOIN system_user_role ur ON ur.role_id = map.role_id AND ur.tenant_id = map.tenant_id AND ur.deleted = 0 "
                + "JOIN system_role r ON r.id = map.role_id AND r.tenant_id = map.tenant_id AND r.deleted = 0 AND r.status = 0 "
                + "JOIN bpm_user_group g ON g.id = map.group_id AND g.tenant_id = map.tenant_id AND g.deleted = 0 "
                + "WHERE map.tenant_id = ? AND ur.user_id = ?", Long.class, tenantId(), userId));
        return result;
    }

    public Set<Long> getManualGroupIdsByUser(Long userId) {
        return new LinkedHashSet<>(jdbc.queryForList("SELECT m.group_id FROM bpm_user_group_member m "
                + "JOIN bpm_user_group g ON g.id = m.group_id AND g.tenant_id = m.tenant_id AND g.deleted = 0 "
                + "WHERE m.tenant_id = ? AND m.user_id = ?", Long.class, tenantId(), userId));
    }

    public Set<Long> getManualUserIdsByGroup(Long groupId) {
        return new LinkedHashSet<>(jdbc.queryForList("SELECT user_id FROM bpm_user_group_member "
                + "WHERE tenant_id = ? AND group_id = ?", Long.class, tenantId(), groupId));
    }

    public Map<Long, Set<Long>> getRoleSourcesByUser(Long userId) {
        Map<Long, Set<Long>> result = new LinkedHashMap<>();
        jdbc.query("SELECT map.group_id, map.role_id FROM bpm_user_group_role_map map "
                + "JOIN system_user_role ur ON ur.role_id = map.role_id AND ur.tenant_id = map.tenant_id AND ur.deleted = 0 "
                + "JOIN system_role r ON r.id = map.role_id AND r.tenant_id = map.tenant_id AND r.deleted = 0 AND r.status = 0 "
                + "JOIN bpm_user_group g ON g.id = map.group_id AND g.tenant_id = map.tenant_id AND g.deleted = 0 "
                + "WHERE map.tenant_id = ? AND ur.user_id = ?", rs -> {
            result.computeIfAbsent(rs.getLong(1), ignored -> new LinkedHashSet<>()).add(rs.getLong(2));
        }, tenantId(), userId);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceManualMembers(Long groupId, Set<Long> userIds) {
        lockGroup(groupId);
        Set<Long> desired = userIds == null ? Collections.emptySet() : userIds;
        validateUsers(desired);
        Set<Long> existing = getManualUserIdsByGroup(groupId);
        for (Long id : existing) {
            if (!desired.contains(id)) {
                jdbc.update("DELETE FROM bpm_user_group_member WHERE tenant_id = ? AND group_id = ? AND user_id = ?",
                        tenantId(), groupId, id);
            }
        }
        for (Long id : desired) {
            if (!existing.contains(id)) {
                jdbc.update("INSERT INTO bpm_user_group_member (tenant_id, group_id, user_id) VALUES (?, ?, ?)",
                        tenantId(), groupId, id);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceManualGroups(Long userId, Set<Long> groupIds) {
        Set<Long> desired = groupIds == null ? Collections.emptySet() : groupIds;
        Set<Long> existing = getManualGroupIdsByUser(userId);
        Set<Long> affected = new HashSet<>(desired);
        affected.addAll(existing);
        List<Long> ordered = new ArrayList<>(affected);
        Collections.sort(ordered);
        for (Long groupId : ordered) {
            lockGroup(groupId);
            if (desired.contains(groupId) && !existing.contains(groupId)) {
                jdbc.update("INSERT INTO bpm_user_group_member (tenant_id, group_id, user_id) VALUES (?, ?, ?)",
                        tenantId(), groupId, userId);
            } else if (!desired.contains(groupId) && existing.contains(groupId)) {
                jdbc.update("DELETE FROM bpm_user_group_member WHERE tenant_id = ? AND group_id = ? AND user_id = ?",
                        tenantId(), groupId, userId);
            }
        }
    }

    public Set<Long> getMappedGroupIds(Long roleId) {
        return new LinkedHashSet<>(jdbc.queryForList("SELECT group_id FROM bpm_user_group_role_map "
                + "WHERE tenant_id = ? AND role_id = ?", Long.class, tenantId(), roleId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceRoleMappings(Long roleId, Set<Long> groupIds) {
        jdbc.queryForObject("SELECT id FROM system_role WHERE tenant_id = ? AND id = ? AND deleted = 0 FOR UPDATE",
                Long.class, tenantId(), roleId);
        List<Long> ordered = new ArrayList<>(groupIds);
        Collections.sort(ordered);
        ordered.forEach(this::lockGroup);
        jdbc.update("DELETE FROM bpm_user_group_role_map WHERE tenant_id = ? AND role_id = ?", tenantId(), roleId);
        for (Long groupId : ordered) {
            jdbc.update("INSERT INTO bpm_user_group_role_map (tenant_id, role_id, group_id) VALUES (?, ?, ?)",
                    tenantId(), roleId, groupId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId) {
        lockGroup(groupId);
        jdbc.update("DELETE FROM bpm_user_group_member WHERE tenant_id = ? AND group_id = ?", tenantId(), groupId);
        jdbc.update("DELETE FROM bpm_user_group_role_map WHERE tenant_id = ? AND group_id = ?", tenantId(), groupId);
    }

    private void lockGroup(Long groupId) {
        jdbc.queryForObject("SELECT id FROM bpm_user_group WHERE tenant_id = ? AND id = ? AND deleted = 0 FOR UPDATE",
                Long.class, tenantId(), groupId);
    }

    private void validateUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<Object> params = args(userIds);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM system_users WHERE tenant_id = ? "
                + "AND deleted = 0 AND id IN (" + placeholders(userIds.size()) + ")",
                Integer.class, params.toArray());
        if (count == null || count != userIds.size()) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    private static String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private List<Object> args(Collection<Long> ids) {
        List<Object> result = new ArrayList<>();
        result.add(tenantId());
        result.addAll(ids);
        return result;
    }
}
