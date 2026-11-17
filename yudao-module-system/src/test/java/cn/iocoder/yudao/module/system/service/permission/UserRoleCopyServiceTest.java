package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionCopyUserRolesReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRoleCopyServiceTest {
    private final UserRoleCopyService service = new UserRoleCopyService();
    private final AdminUserService users = mock(AdminUserService.class);
    private final RoleService roles = mock(RoleService.class);
    private final PermissionService permissions = mock(PermissionService.class);
    private final RoleMemberService members = mock(RoleMemberService.class);

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "userService", users);
        ReflectionTestUtils.setField(service, "roleService", roles);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
        ReflectionTestUtils.setField(service, "roleMemberService", members);
        when(users.getUser(1L)).thenReturn(new AdminUserDO());
        when(permissions.getUserRoleIdListByUserId(1L)).thenReturn(ids(10L, 20L, 30L));
    }

    @Test
    void copiesSelectedRolesToAllTargetsInStableOrderWithoutReplacingRoles() {
        service.copy(1L, ids(3L, 2L), ids(20L, 10L));
        InOrder order = inOrder(users, roles, members);
        order.verify(users).getUser(1L);
        order.verify(users).validateUserList(ids(2L, 3L));
        order.verify(roles).validateRoleList(ids(10L, 20L));
        order.verify(members).update(10L, 2L, true);
        order.verify(members).update(10L, 3L, true);
        order.verify(members).update(20L, 2L, true);
        order.verify(members).update(20L, 3L, true);
        verifyNoMoreInteractions(members);
        verify(permissions, never()).assignUserRole(anyLong(), anySet());
    }

    @Test
    void cannotInjectRolesNotOwnedBySourceOrCopyStaleSelection() {
        assertThrows(ServiceException.class, () -> service.copy(1L, ids(2L), ids(999L)));
        verifyNoInteractions(members);
    }

    @Test
    void sourceMustExistAndCannotBeATarget() {
        assertThrows(ServiceException.class, () -> service.copy(1L, ids(1L), ids(10L)));
        when(users.getUser(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.copy(1L, ids(2L), ids(10L)));
        verifyNoInteractions(members);
    }

    @Test
    void validatesEveryTargetAndRoleBeforeAnyWrite() {
        doThrow(exception(USER_IS_DISABLE, "测试成员")).when(users).validateUserList(ids(2L, 3L));
        assertThrows(ServiceException.class, () -> service.copy(1L, ids(2L, 3L), ids(10L)));
        verifyNoInteractions(members);
        reset(users);
        when(users.getUser(1L)).thenReturn(new AdminUserDO());
        doThrow(exception(ROLE_IS_DISABLE, "测试角色")).when(roles).validateRoleList(ids(10L));
        assertThrows(ServiceException.class, () -> service.copy(1L, ids(2L), ids(10L)));
        verifyNoInteractions(members);
    }

    @Test
    void writeFailurePropagatesForBatchTransactionRollback() {
        doThrow(exception(USER_NOT_EXISTS)).when(members).update(10L, 3L, true);
        assertThrows(ServiceException.class, () -> service.copy(1L, ids(2L, 3L, 4L), ids(10L)));
        verify(members).update(10L, 2L, true);
        verify(members, never()).update(10L, 4L, true);
    }

    @Test
    void validatesEmptyInvalidAndOversizedRequests() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            PermissionCopyUserRolesReqVO request = new PermissionCopyUserRolesReqVO();
            assertFalse(factory.getValidator().validate(request).isEmpty());
            request.setSourceUserId(1L); request.setRoleIds(ids(10L)); request.setTargetUserIds(ids(2L));
            assertTrue(factory.getValidator().validate(request).isEmpty());
            request.setTargetUserIds(ids(-1L, null));
            assertFalse(factory.getValidator().validate(request).isEmpty());
            Set<Long> targets = new HashSet<>();
            for (long i = 2; i <= 52; i++) targets.add(i);
            request.setTargetUserIds(targets);
            assertFalse(factory.getValidator().validate(request).isEmpty());
        }
    }

    private Set<Long> ids(Long... ids) { return new HashSet<>(Arrays.asList(ids)); }
}
