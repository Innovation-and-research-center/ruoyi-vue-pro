package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskCountRespVO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmTaskSortMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmTaskCountTest {

    @Test
    void shouldExcludeCancelledInvalidAndStartTasksAndIncludeLegacyDoneTasks() throws Exception {
        UnpooledDataSource dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:task_counts_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE ACT_HI_TASKINST (ID_ VARCHAR, ASSIGNEE_ VARCHAR, "
                    + "PROC_INST_ID_ VARCHAR, TASK_DEF_KEY_ VARCHAR, END_TIME_ TIMESTAMP)");
            statement.execute("CREATE TABLE ACT_HI_VARINST (TASK_ID_ VARCHAR, PROC_INST_ID_ VARCHAR, "
                    + "NAME_ VARCHAR, LONG_ BIGINT, TEXT_ VARCHAR)");
            statement.execute("CREATE SCHEMA hist_wf");
            statement.execute("CREATE TABLE hist_wf.actinst (user_id BIGINT, end_date TIMESTAMP)");
            String[] businessTables = {"bpm_receive_doc", "bpm_leave", "t_time_explain", "t_confflow",
                    "t_xzfy_list", "t_xzss_list", "t_send_doc"};
            for (String table : businessTables) {
                String statusColumn = "bpm_leave".equals(table) ? "spzt" : "status";
                statement.execute("CREATE TABLE " + table + " (process_instance_id VARCHAR, " + statusColumn + " INT)");
                statement.execute("INSERT INTO " + table + " VALUES ('invalid-" + table + "', 5)");
                statement.execute("INSERT INTO ACT_HI_TASKINST VALUES ('task-" + table + "', '7', "
                        + "'invalid-" + table + "', 'review', TIMESTAMP '2026-01-01 10:00:00')");
            }
            statement.execute("INSERT INTO ACT_HI_TASKINST VALUES "
                    + "('valid1', '7', 'p1', 'review', TIMESTAMP '2026-01-01 10:00:00'), "
                    + "('valid2', '7', 'p2', 'review', TIMESTAMP '2026-01-01 10:00:00'), "
                    + "('cancelled', '7', 'p3', 'review', TIMESTAMP '2026-01-01 10:00:00'), "
                    + "('invalid', '7', 'p4', 'review', TIMESTAMP '2026-01-01 10:00:00'), "
                    + "('start', '7', 'p5', 'StartUserNode', TIMESTAMP '2026-01-01 10:00:00'), "
                    + "('other', '8', 'p6', 'review', TIMESTAMP '2026-01-01 10:00:00'), "
                    + "('pending', '7', 'p7', 'review', NULL)");
            // 同时覆盖数字和文本两种状态存储方式。
            statement.execute("INSERT INTO ACT_HI_VARINST VALUES "
                    + "('cancelled', 'p3', 'TASK_STATUS', 4, NULL), "
                    + "(NULL, 'p4', 'PROCESS_STATUS', NULL, '5')");
            statement.execute("INSERT INTO hist_wf.actinst VALUES "
                    + "(7, TIMESTAMP '2026-01-01 10:00:00'), "
                    + "(8, TIMESTAMP '2026-01-01 10:00:00'), (7, NULL)");
        }

        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
        String resource = "mapper/task/BpmTaskSortMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        try (SqlSession session = new SqlSessionFactoryBuilder().build(configuration).openSession()) {
            BpmTaskServiceImpl service = new BpmTaskServiceImpl();
            ReflectionTestUtils.setField(service, "taskSortMapper", session.getMapper(BpmTaskSortMapper.class));
            TaskService taskService = mock(TaskService.class);
            TaskQuery todoQuery = mock(TaskQuery.class, RETURNS_SELF);
            when(taskService.createTaskQuery()).thenReturn(todoQuery);
            when(todoQuery.count()).thenReturn(2L);
            ReflectionTestUtils.setField(service, "taskService", taskService);

            BpmTaskCountRespVO count = service.getTaskCount(7L);
            assertEquals(3L, count.getDoneCount());
            assertEquals(2L, count.getTodoCount());
            assertEquals(5L, count.getTotalCount());

            when(todoQuery.count()).thenReturn(0L);
            BpmTaskCountRespVO empty = service.getTaskCount(999L);
            assertEquals(0L, empty.getDoneCount());
            assertEquals(0L, empty.getTotalCount());
        }
    }
}
