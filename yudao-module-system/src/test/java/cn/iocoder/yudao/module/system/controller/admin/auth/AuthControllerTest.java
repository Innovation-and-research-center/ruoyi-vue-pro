package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AuthController controller;
    @Mock
    private AdminUserService userService;
    @Mock
    private DeptService deptService;
    @Mock
    private PermissionService permissionService;
    @Mock
    private RoleService roleService;
    @Mock
    private MenuService menuService;

    private AdminUserDO user;

    @BeforeEach
    void setUp() {
        SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(20L), new MockHttpServletRequest());
        user = new AdminUserDO().setId(20L).setDeptId(30L);
        when(userService.getUser(20L)).thenReturn(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        DataPermissionContextHolder.clear();
    }

    @Test
    void selfOnlyUserReceivesOwnDepartmentWithoutChangingRoleScope() {
        mockOwnDepartment();
        when(permissionService.getUserRoleIdListByUserId(20L)).thenAnswer(invocation -> {
            assertNull(DataPermissionContextHolder.get());
            return Collections.singleton(1L);
        });
        RoleDO role = new RoleDO().setId(1L).setCode("ordinary")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(roleService.getRoleList(Collections.singleton(1L)))
                .thenReturn(new ArrayList<>(Collections.singletonList(role)));
        when(permissionService.getRoleMenuListByRoleId(Collections.singleton(1L)))
                .thenReturn(Collections.emptySet());
        when(menuService.getMenuList(Collections.emptySet())).thenReturn(Collections.emptyList());
        when(menuService.filterDisableMenus(Collections.emptyList())).thenReturn(Collections.emptyList());

        AuthPermissionInfoRespVO result = controller.getPermissionInfo().getData();

        assertEquals("测试部门", result.getUser().getDeptName());
        assertEquals(Collections.singleton("ordinary"), result.getRoles());
        assertTrue(result.getPermissions().isEmpty());
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void userWithoutRolesStillReceivesDepartment() {
        mockOwnDepartment();
        when(permissionService.getUserRoleIdListByUserId(20L)).thenReturn(Collections.emptySet());

        AuthPermissionInfoRespVO result = controller.getPermissionInfo().getData();

        assertEquals("测试部门", result.getUser().getDeptName());
        assertTrue(result.getRoles().isEmpty());
        verifyNoInteractions(roleService, menuService);
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void userWithoutDepartmentDoesNotQueryDepartment() {
        user.setDeptId(null);
        assertEquals("", controller.getPermissionInfo().getData().getUser().getDeptName());
        verifyNoInteractions(deptService);
    }

    @Test
    void deletedDepartmentReturnsEmptyName() {
        assertEquals("", controller.getPermissionInfo().getData().getUser().getDeptName());
        verify(deptService).getDept(30L);
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void departmentQueryFailureRestoresDataPermission() {
        when(deptService.getDept(30L)).thenThrow(new IllegalStateException("查询失败"));
        assertThrows(IllegalStateException.class, () -> controller.getPermissionInfo());
        assertNull(DataPermissionContextHolder.get());
        verifyNoInteractions(permissionService);
    }

    private void mockOwnDepartment() {
        when(deptService.getDept(30L)).thenAnswer(invocation -> {
            // 模拟仅本人权限下，普通部门查询被过滤的情况。
            if (DataPermissionContextHolder.get() == null || DataPermissionContextHolder.get().enable()) {
                return null;
            }
            return new DeptDO().setId(30L).setName("测试部门");
        });
    }
}
