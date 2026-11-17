package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleMemberServiceTest {
    private final RoleMemberService service = new RoleMemberService();
    private final RoleMapper roles = mock(RoleMapper.class);
    private final UserRoleMapper members = mock(UserRoleMapper.class);
    private final AdminUserService users = mock(AdminUserService.class);
    private final DeptService depts = mock(DeptService.class);
    private RoleDO role;
    private AdminUserDO user;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "roleMapper", roles);
        ReflectionTestUtils.setField(service, "userRoleMapper", members);
        ReflectionTestUtils.setField(service, "userService", users);
        ReflectionTestUtils.setField(service, "deptService", depts);
        role = new RoleDO(); role.setId(10L); role.setStatus(0);
        user = new AdminUserDO(); user.setId(20L); user.setStatus(0); user.setNickname("测试成员");
        when(roles.selectOne(any(Wrapper.class))).thenReturn(role);
        when(roles.selectById(10L)).thenReturn(role);
        when(users.getUser(20L)).thenReturn(user);
    }

    @Test
    void addingMemberPreservesOtherRoles() {
        when(members.selectListByUserId(20L)).thenReturn(Collections.singletonList(link(99L)));
        service.update(10L, 20L, true);
        ArgumentCaptor<UserRoleDO> added = ArgumentCaptor.forClass(UserRoleDO.class);
        verify(members).insert(added.capture());
        assertEquals(Long.valueOf(10), added.getValue().getRoleId());
        assertEquals(Long.valueOf(20), added.getValue().getUserId());
        verify(members, never()).deleteListByUserId(anyLong());
        verify(members, never()).deleteListByUserIdAndRoleIdIds(anyLong(), anyCollection());
    }

    @Test
    void duplicateAdditionIsIdempotent() {
        when(members.selectListByUserId(20L)).thenReturn(Collections.singletonList(link(10L)));
        service.update(10L, 20L, true);
        verify(members, never()).insert(any(UserRoleDO.class));
    }

    @Test
    void removingDisabledMemberOnlyDeletesTheSelectedRole() {
        role.setStatus(1); user.setStatus(1);
        service.update(10L, 20L, false);
        verify(members).deleteListByUserIdAndRoleIdIds(20L, Collections.singleton(10L));
        verify(members, never()).deleteListByUserId(anyLong());
    }

    @Test
    void disabledUserOrRoleCannotBeAdded() {
        user.setStatus(1);
        assertThrows(ServiceException.class, () -> service.update(10L, 20L, true));
        user.setStatus(0); role.setStatus(1);
        assertThrows(ServiceException.class, () -> service.update(10L, 20L, true));
        verifyNoInteractions(members);
    }

    @Test
    void missingOrInaccessibleRoleAndUserCauseNoWrites() {
        when(roles.selectOne(any(Wrapper.class))).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.update(10L, 20L, true));
        when(roles.selectOne(any(Wrapper.class))).thenReturn(role);
        when(users.getUser(20L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.update(10L, 20L, false));
        verifyNoInteractions(members);
    }

    @Test
    void listingIncludesDisabledMembersAndHandlesMissingDepartment() {
        user.setStatus(1);
        when(members.selectListByRoleIds(Collections.singleton(10L))).thenReturn(Collections.singletonList(link(10L)));
        when(users.getUserList(Collections.singleton(20L))).thenReturn(Collections.singletonList(user));
        when(depts.getDeptMap(anyCollection())).thenReturn(Collections.emptyMap());
        assertEquals(Integer.valueOf(1), service.list(10L).get(0).getStatus());
        assertNull(service.list(10L).get(0).getDeptName());
    }

    @Test
    void emptyRoleDoesNotQueryAllUsers() {
        when(members.selectListByRoleIds(Collections.singleton(10L))).thenReturn(Collections.emptyList());
        assertTrue(service.list(10L).isEmpty());
        verifyNoInteractions(users);
    }

    @Test
    void membersFollowDepartmentAndUserOrderWithMissingValuesLast() {
        DeptDO firstDept = new DeptDO(); firstDept.setId(1L); firstDept.setSort(1);
        DeptDO secondDept = new DeptDO(); secondDept.setId(2L); secondDept.setSort(2);
        Map<Long, DeptDO> departments = new HashMap<>();
        departments.put(1L, firstDept); departments.put(2L, secondDept);
        AdminUserDO first = orderedUser(40L, 1L, 1L);
        AdminUserDO second = orderedUser(30L, 1L, 2L);
        AdminUserDO unsorted = orderedUser(25L, 1L, null);
        AdminUserDO otherDept = orderedUser(10L, 2L, 1L);
        AdminUserDO noDept = orderedUser(1L, null, null);
        when(members.selectListByRoleIds(Collections.singleton(10L))).thenReturn(Collections.singletonList(link(10L)));
        when(users.getUserList(anyCollection())).thenReturn(Arrays.asList(noDept, otherDept, unsorted, second, first));
        when(depts.getDeptMap(anyCollection())).thenReturn(departments);
        assertEquals(Arrays.asList(40L, 30L, 25L, 10L, 1L), service.list(10L).stream()
                .map(item -> item.getId()).collect(Collectors.toList()));
    }

    private AdminUserDO orderedUser(Long id, Long deptId, Long sort) {
        AdminUserDO user = new AdminUserDO();
        user.setId(id); user.setDeptId(deptId); user.setSort(sort); user.setStatus(0);
        return user;
    }

    private UserRoleDO link(Long roleId) {
        UserRoleDO link = new UserRoleDO(); link.setRoleId(roleId); link.setUserId(20L); return link;
    }
}
