package cn.iocoder.yudao.module.bpm.service.definition;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.framework.test.core.util.RandomUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.definition.BpmUserGroupMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.USER_GROUP_NOT_EXISTS;

/**
 * {@link BpmUserGroupServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import({BpmUserGroupServiceImpl.class, BpmUserGroupMembershipService.class, BpmUserGroupServiceTest.JdbcConfig.class})
public class BpmUserGroupServiceTest extends BaseDbUnitTest {

    @TestConfiguration
    public static class JdbcConfig {
        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

    @BeforeEach
    public void setTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void clearTenant() {
        TenantContextHolder.clear();
    }

    @Resource
    private BpmUserGroupServiceImpl userGroupService;

    @Resource
    private BpmUserGroupMapper userGroupMapper;
    @Resource
    private BpmUserGroupMembershipService membershipService;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testCreateUserGroup_success() {
        // 准备参数
        BpmUserGroupSaveReqVO reqVO = RandomUtils.randomPojo(BpmUserGroupSaveReqVO.class);
        reqVO.setUserIds(Collections.singleton(11L));
        jdbcTemplate.update("INSERT INTO system_users (id, tenant_id, deleted) VALUES (11, 1, 0)");

        // 调用
        Long userGroupId = userGroupService.createUserGroup(reqVO);
        // 断言
        Assertions.assertNotNull(userGroupId);
        // 校验记录的属性是否正确
        BpmUserGroupDO userGroup = userGroupService.getUserGroup(userGroupId);
        AssertUtils.assertPojoEquals(reqVO, userGroup);
    }

    @Test
    public void testUpdateUserGroup_success() {
        // mock 数据
        BpmUserGroupDO dbUserGroup = RandomUtils.randomPojo(BpmUserGroupDO.class);
        userGroupMapper.insert(dbUserGroup);// @Sql: 先插入出一条存在的数据
        // 准备参数
        BpmUserGroupSaveReqVO reqVO = RandomUtils.randomPojo(BpmUserGroupSaveReqVO.class, o -> {
            o.setId(dbUserGroup.getId()); // 设置更新的 ID
        });
        reqVO.setUserIds(Collections.singleton(11L));
        jdbcTemplate.update("INSERT INTO system_users (id, tenant_id, deleted) VALUES (11, 1, 0)");

        // 调用
        userGroupService.updateUserGroup(reqVO);
        // 校验是否更新正确
        BpmUserGroupDO userGroup = userGroupService.getUserGroup(reqVO.getId()); // 获取最新的
        AssertUtils.assertPojoEquals(reqVO, userGroup);
    }

    @Test
    public void testUpdateUserGroup_notExists() {
        // 准备参数
        BpmUserGroupSaveReqVO reqVO = RandomUtils.randomPojo(BpmUserGroupSaveReqVO.class);

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> userGroupService.updateUserGroup(reqVO), USER_GROUP_NOT_EXISTS);
    }

    @Test
    public void testDeleteUserGroup_success() {
        // mock 数据
        BpmUserGroupDO dbUserGroup = RandomUtils.randomPojo(BpmUserGroupDO.class);
        userGroupMapper.insert(dbUserGroup);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbUserGroup.getId();

        // 调用
        userGroupService.deleteUserGroup(id);
       // 校验数据不存在了
       Assertions.assertNull(userGroupMapper.selectById(id));
    }

    @Test
    public void testDeleteUserGroup_notExists() {
        // 准备参数
        Long id = RandomUtils.randomLongId();

        // 调用, 并断言异常
        AssertUtils.assertServiceException(() -> userGroupService.deleteUserGroup(id), USER_GROUP_NOT_EXISTS);
    }

    @Test
    public void testGetUserGroupPage() {
       // mock 数据
       BpmUserGroupDO dbUserGroup = RandomUtils.randomPojo(BpmUserGroupDO.class, o -> { // 等会查询到
           o.setName("芋道源码");
           o.setStatus(CommonStatusEnum.ENABLE.getStatus());
           o.setCreateTime(buildTime(2021, 11, 11));
       });
       userGroupMapper.insert(dbUserGroup);
       // 测试 name 不匹配
       userGroupMapper.insert(cloneIgnoreId(dbUserGroup, o -> o.setName("芋道")));
       // 测试 status 不匹配
       userGroupMapper.insert(cloneIgnoreId(dbUserGroup, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
       // 测试 createTime 不匹配
       userGroupMapper.insert(cloneIgnoreId(dbUserGroup, o -> o.setCreateTime(buildTime(2021, 12, 12))));
       // 准备参数
       BpmUserGroupPageReqVO reqVO = new BpmUserGroupPageReqVO();
       reqVO.setName("源码");
       reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
       reqVO.setCreateTime((new LocalDateTime[]{buildTime(2021, 11, 10),buildTime(2021, 11, 12)}));

       // 调用
       PageResult<BpmUserGroupDO> pageResult = userGroupService.getUserGroupPage(reqVO);
       // 断言
       Assertions.assertEquals(1, pageResult.getTotal());
       Assertions.assertEquals(1, pageResult.getList().size());
       AssertUtils.assertPojoEquals(dbUserGroup, pageResult.getList().get(0), "userIds");
    }

    @Test
    public void testRoleRevocationPreservesManualMember() {
        BpmUserGroupSaveReqVO req = new BpmUserGroupSaveReqVO();
        req.setName("中层审批");
        req.setDescription("测试");
        req.setStatus(CommonStatusEnum.ENABLE.getStatus());
        req.setUserIds(Collections.singleton(11L));
        jdbcTemplate.update("INSERT INTO system_users (id, tenant_id, deleted) VALUES (11, 1, 0)");
        Long groupId = userGroupService.createUserGroup(req);

        jdbcTemplate.update("INSERT INTO system_role (id, tenant_id, deleted) VALUES (99, 1, 0)");
        jdbcTemplate.update("INSERT INTO system_user_role (user_id, role_id, tenant_id, deleted) VALUES (11, 99, 1, 0)");
        jdbcTemplate.update("INSERT INTO system_users (id, tenant_id, deleted) VALUES (22, 1, 0)");
        jdbcTemplate.update("INSERT INTO system_user_role (user_id, role_id, tenant_id, deleted) VALUES (22, 99, 1, 0)");

        membershipService.replaceRoleMappings(99L, Collections.singleton(groupId));
        Assertions.assertEquals(Collections.singleton(99L), membershipService.getRoleSourcesByUser(11L).get(groupId));
        Assertions.assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(11L, 22L)),
                userGroupService.getUserGroup(groupId).getUserIds());
        jdbcTemplate.update("DELETE FROM system_user_role WHERE user_id = 11 AND role_id = 99");
        jdbcTemplate.update("DELETE FROM system_user_role WHERE user_id = 22 AND role_id = 99");
        Assertions.assertEquals(Collections.singleton(11L), userGroupService.getUserGroup(groupId).getUserIds());
        Assertions.assertEquals(Collections.singleton(11L), membershipService.getManualUserIdsByGroup(groupId));
        Assertions.assertEquals(Collections.emptyMap(), membershipService.getRoleSourcesByUser(11L));
    }

}
