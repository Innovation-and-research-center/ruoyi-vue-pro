package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserMembershipCopyReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmUserMembershipCopyTest {
    private JdbcTemplate jdbc;
    private BpmUserMembershipCopyService service;
    private BpmUserGroupMembershipService membership;
    private BpmUserGroupService groups;
    private AdminUserService users;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:copy_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE system_users(id BIGINT, tenant_id BIGINT, deleted INT)");
        jdbc.execute("CREATE TABLE system_role(id BIGINT, tenant_id BIGINT, deleted INT, status INT)");
        jdbc.execute("CREATE TABLE system_user_role(user_id BIGINT, role_id BIGINT, tenant_id BIGINT, deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE bpm_user_group(id BIGINT, tenant_id BIGINT, deleted INT, status INT)");
        jdbc.execute("CREATE TABLE bpm_user_group_member(tenant_id BIGINT, group_id BIGINT, user_id BIGINT, UNIQUE(tenant_id,group_id,user_id))");
        jdbc.execute("CREATE TABLE bpm_user_group_role_map(tenant_id BIGINT, group_id BIGINT, role_id BIGINT)");
        jdbc.update("INSERT INTO system_users VALUES (1,1,0),(2,1,0),(3,1,0),(4,2,0)");
        jdbc.update("INSERT INTO system_role VALUES (10,1,0,0),(99,1,0,0)");
        jdbc.update("INSERT INTO system_user_role(user_id,role_id,tenant_id) VALUES (1,10,1),(2,99,1)");
        jdbc.update("INSERT INTO bpm_user_group VALUES (20,1,0,0),(30,1,0,0),(40,1,0,0),(50,2,0,0)");
        jdbc.update("INSERT INTO bpm_user_group_member VALUES (1,20,1),(1,40,2),(2,50,4)");
        jdbc.update("INSERT INTO bpm_user_group_role_map VALUES (1,30,10)");
        membership = spy(new BpmUserGroupMembershipService());
        ReflectionTestUtils.setField(membership, "jdbc", jdbc);
        PermissionService permissions = mock(PermissionService.class);
        when(permissions.getUserRoleIdListByUserId(1L)).thenReturn(Collections.singleton(10L));
        users = mock(AdminUserService.class);
        when(users.getUser(1L)).thenReturn(new AdminUserDO());
        RoleMapper roles = mock(RoleMapper.class);
        when(roles.selectOne(any(Wrapper.class))).thenReturn(new RoleDO());
        UserRoleMapper userRoles = mock(UserRoleMapper.class);
        when(userRoles.selectListByUserId(anyLong())).thenAnswer(invocation -> jdbc.query(
                "SELECT role_id FROM system_user_role WHERE user_id=? AND tenant_id=1 AND deleted=0",
                (rs, index) -> { UserRoleDO row = new UserRoleDO(); row.setRoleId(rs.getLong(1)); return row; },
                (Long) invocation.getArgument(0)));
        when(userRoles.insert(any(UserRoleDO.class))).thenAnswer(invocation -> {
            UserRoleDO row = invocation.getArgument(0);
            return jdbc.update("INSERT INTO system_user_role(user_id,role_id,tenant_id) VALUES (?,?,1)", row.getUserId(), row.getRoleId());
        });
        groups = mock(BpmUserGroupService.class);
        BpmUserMembershipCopyService target = new BpmUserMembershipCopyService();
        ReflectionTestUtils.setField(target, "userService", users);
        ReflectionTestUtils.setField(target, "roleService", mock(RoleService.class));
        ReflectionTestUtils.setField(target, "permissionService", permissions);
        ReflectionTestUtils.setField(target, "roleMapper", roles);
        ReflectionTestUtils.setField(target, "userRoleMapper", userRoles);
        ReflectionTestUtils.setField(target, "groupService", groups);
        ReflectionTestUtils.setField(target, "membershipService", membership);
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        service = (BpmUserMembershipCopyService) factory.getProxy();
    }

    @AfterEach
    void cleanup() { TenantContextHolder.clear(); }

    @Test
    void combinedCopyPreservesExistingAndDoesNotConvertInheritedGroupsToManual() {
        BpmUserMembershipCopyReqVO request = request(ids(10L), ids(20L,30L));
        service.copy(request);
        service.copy(request); // 重复提交保持幂等。
        assertEquals(1, count("SELECT COUNT(*) FROM system_user_role WHERE user_id=2 AND role_id=10"));
        assertEquals(1, count("SELECT COUNT(*) FROM system_user_role WHERE user_id=2 AND role_id=99"));
        assertEquals(ids(20L,40L), membership.getManualGroupIdsByUser(2L));
        assertEquals(ids(20L,30L,40L), membership.getGroupIdsByUser(2L));
        assertEquals(1, count("SELECT COUNT(*) FROM bpm_user_group_member WHERE tenant_id=2"));
    }

    @Test
    void groupsCanBeCopiedWithoutRolesIncludingAnInheritedSourceGroup() {
        service.copy(request(Collections.emptySet(), ids(20L,30L)));
        assertEquals(ids(20L,30L,40L), membership.getManualGroupIdsByUser(2L));
        assertEquals(0, count("SELECT COUNT(*) FROM system_user_role WHERE user_id=2 AND role_id=10"));
    }

    @Test
    void rolesCanBeCopiedWithoutExplicitGroupSelection() {
        service.copy(request(ids(10L), Collections.emptySet()));
        assertEquals(ids(30L,40L), membership.getGroupIdsByUser(2L));
        assertEquals(ids(40L), membership.getManualGroupIdsByUser(2L));
    }

    @Test
    void groupFailureRollsBackRolesAndEarlierGroupWrites() {
        doThrow(new IllegalStateException("simulated write failure")).when(membership).appendMissingMembers(30L, ids(2L));
        assertThrows(IllegalStateException.class, () -> service.copy(request(ids(10L), ids(20L,30L))));
        assertEquals(0, count("SELECT COUNT(*) FROM system_user_role WHERE user_id=2 AND role_id=10"));
        assertEquals(ids(40L), membership.getManualGroupIdsByUser(2L));
    }

    @Test
    void cannotCopyForeignOrUnownedGroups() {
        assertThrows(RuntimeException.class, () -> service.copy(request(ids(10L), ids(50L))));
        assertEquals(0, count("SELECT COUNT(*) FROM system_user_role WHERE user_id=2 AND role_id=10"));
    }

    @Test
    void invalidTargetAndDisabledGroupAreRejectedBeforeWrites() {
        doThrow(new IllegalArgumentException("disabled user")).when(users).validateUserList(ids(2L));
        assertThrows(IllegalArgumentException.class, () -> service.copy(request(ids(10L), ids(20L))));
        doNothing().when(users).validateUserList(ids(2L));
        doThrow(new IllegalArgumentException("disabled group")).when(groups).validUserGroups(ids(20L));
        assertThrows(IllegalArgumentException.class, () -> service.copy(request(ids(10L), ids(20L))));
        assertEquals(0, count("SELECT COUNT(*) FROM system_user_role WHERE user_id=2 AND role_id=10"));
    }

    @Test
    void validatesAtLeastOneSelectionAndAllowsGroupsOnly() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(request(Collections.emptySet(), Collections.emptySet())).isEmpty());
            assertTrue(factory.getValidator().validate(request(Collections.emptySet(), ids(20L))).isEmpty());
        }
    }

    @Test
    void endpointChecksRoleAndGroupPermissionsSeparately() throws Exception {
        String expression = cn.iocoder.yudao.module.bpm.controller.admin.definition.BpmUserGroupMembershipController.class
                .getMethod("copy", BpmUserMembershipCopyReqVO.class)
                .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
        org.springframework.expression.spel.support.StandardEvaluationContext context = new org.springframework.expression.spel.support.StandardEvaluationContext();
        context.setBeanResolver((ctx, name) -> new TestPermissions());
        org.springframework.expression.Expression predicate = new org.springframework.expression.spel.standard.SpelExpressionParser().parseExpression(expression);
        context.setVariable("req", request(ids(10L), Collections.emptySet()));
        assertEquals(Boolean.TRUE, predicate.getValue(context, Boolean.class));
        context.setVariable("req", request(Collections.emptySet(), ids(20L)));
        assertEquals(Boolean.FALSE, predicate.getValue(context, Boolean.class));
        context.setVariable("req", request(ids(10L), ids(20L)));
        assertEquals(Boolean.FALSE, predicate.getValue(context, Boolean.class));
    }

    public static class TestPermissions {
        public boolean hasPermission(String permission) { return "system:permission:assign-user-role".equals(permission); }
        public boolean hasAnyPermissions(String... permissions) { return Arrays.stream(permissions).anyMatch(this::hasPermission); }
    }

    private BpmUserMembershipCopyReqVO request(Set<Long> roleIds, Set<Long> groupIds) {
        BpmUserMembershipCopyReqVO req = new BpmUserMembershipCopyReqVO();
        req.setSourceUserId(1L); req.setTargetUserIds(ids(2L)); req.setRoleIds(roleIds); req.setGroupIds(groupIds);
        return req;
    }
    private Set<Long> ids(Long... values) { return new HashSet<>(Arrays.asList(values)); }
    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
}
