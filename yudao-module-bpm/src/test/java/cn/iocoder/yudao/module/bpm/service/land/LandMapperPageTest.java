package cn.iocoder.yudao.module.bpm.service.land;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandPageReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandMapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 实际执行分页 SQL，验证筛选与逻辑删除、租户条件共同生效。 */
class LandMapperPageTest {
    private SqlSessionFactory factory;

    @BeforeEach
    void prepare() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:land-page-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE bpm_land (id bigint primary key, project_name varchar(200), "
                    + "location varchar(500), dept_opinion varchar(4000), apply_date timestamp, user_id bigint, "
                    + "dept_id bigint, start_unit varchar(100), start_user_name varchar(100), dept_name varchar(100), "
                    + "process_instance_id varchar(100), status int, filepath varchar(1000), "
                    + "creator varchar(64), create_time timestamp, updater varchar(64), update_time timestamp, "
                    + "deleted smallint default 0, tenant_id bigint default 1)");
            statement.execute("INSERT INTO bpm_land (id, project_name, location, start_user_name, status, apply_date, tenant_id, deleted) VALUES "
                    + "(1, '项目甲', '坐落甲', '登记员甲', 1, '2026-10-01 00:00:00', 1, 0),"
                    + "(2, '项目乙', '坐落甲', '登记员甲', 1, '2026-10-29 23:59:59', 1, 0),"
                    + "(3, '项目甲', '坐落甲', '登记员甲', 2, '2026-10-15 12:00:00', 1, 0),"
                    + "(4, '项目甲', '坐落甲', '登记员甲', 1, '2026-10-15 12:00:00', 2, 0),"
                    + "(5, '项目甲', '坐落甲', '登记员甲', 1, '2026-10-15 12:00:00', 1, 1),"
                    + "(6, '其他项目', '其他坐落', '其他人', 1, '2026-09-01 00:00:00', 1, 0),"
                    + "(7, '作废项目', '坐落甲', '登记员甲', 5, '2026-10-15 12:00:00', 1, 0)");
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("land-test", new JdbcTransactionFactory(), source));
        configuration.setMapUnderscoreToCamelCase(true);
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(1L);
            }
        }));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(LandMapper.class);
        factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    @Test
    void filtersIncludeDateBoundariesAndExcludeOtherTenantsAndDeletedRecords() {
        try (SqlSession session = factory.openSession()) {
            LandPageReqVO request = new LandPageReqVO().setProjectName("项目").setLocation("坐落甲")
                    .setStartUserName("登记员").setStatus(1).setApplyDate(new LocalDateTime[]{
                            LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 29, 23, 59, 59)});
            request.setPageSize(1);
            PageResult<LandDO> first = session.getMapper(LandMapper.class).selectPage(request);
            assertEquals(2L, first.getTotal());
            assertEquals(2L, first.getList().get(0).getId());
            request.setPageNo(2);
            assertEquals(1L, session.getMapper(LandMapper.class).selectPage(request).getList().get(0).getId());
            request.setPageNo(3);
            PageResult<LandDO> empty = session.getMapper(LandMapper.class).selectPage(request);
            assertEquals(2L, empty.getTotal());
            assertTrue(empty.getList().isEmpty());
        }
    }

    @Test
    void cancelStatusEventCannotOverwriteInvalidatedBusiness() {
        try (SqlSession session = factory.openSession()) {
            LandMapper mapper = session.getMapper(LandMapper.class);
            mapper.updateById(new LandDO().setId(1L).setProcessInstanceId("process-1").setStatus(5));
            LandServiceImpl service = new LandServiceImpl();
            org.springframework.test.util.ReflectionTestUtils.setField(service, "landMapper", mapper);
            service.updateStatus(1L, "process-1", 4);
            assertEquals(5, mapper.selectById(1L).getStatus());
            mapper.updateById(new LandDO().setId(2L).setProcessInstanceId("process-2"));
            service.updateStatus(2L, "process-2", 2);
            assertEquals(2, mapper.selectById(2L).getStatus());
        }
    }

    @Test
    void resetFiltersReturnsOnlyCurrentTenantActiveRecordsInStableOrder() {
        try (SqlSession session = factory.openSession()) {
            PageResult<LandDO> page = session.getMapper(LandMapper.class).selectPage(new LandPageReqVO());
            assertEquals(4L, page.getTotal());
            assertEquals(java.util.Arrays.asList(6L, 3L, 2L, 1L), page.getList().stream()
                    .map(LandDO::getId).collect(Collectors.toList()));
            PageResult<LandDO> none = session.getMapper(LandMapper.class)
                    .selectPage(new LandPageReqVO().setProjectName("不存在的项目"));
            assertEquals(0L, none.getTotal());
            assertTrue(none.getList().isEmpty());
        }
    }
}
