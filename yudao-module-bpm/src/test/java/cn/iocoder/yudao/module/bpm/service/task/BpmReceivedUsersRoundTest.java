package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmReceivedUsersRoundTest {

    @Test
    void completedRecipientsFromBeforeReturnAreExcluded() {
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 100), new Date(200)));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 250), new Date(200)));
    }

    @Test
    void runningAndCompletedRecipientsInThisRoundRemainVisible() {
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(1, 200), new Date(200)));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 201), new Date(200)));
    }

    @Test
    void canceledAndReturnedTasksRemainExcludedEvenInThisRound() {
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(4, 300), new Date(200)));
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(5, 300), new Date(200)));
    }

    @Test
    void legacyMissingStatusIsAllowedButMissingCreationTimeCannotEstablishCurrentRound() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getCreateTime()).thenReturn(new Date(300));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(task, new Date(200)));
        when(task.getCreateTime()).thenReturn(null);
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(task, new Date(200)));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(task, null));
    }

    @Test
    void multiInstanceRoundUsesParentActivityStartAndIgnoresOtherBranches() {
        BpmProcessInstanceServiceImpl service = new BpmProcessInstanceServiceImpl();
        RuntimeService runtime = mock(RuntimeService.class);
        HistoryService history = mock(HistoryService.class);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "historyService", history);

        Task current = mock(Task.class);
        when(current.getCreateTime()).thenReturn(new Date(400));
        when(current.getExecutionId()).thenReturn("child");
        when(current.getProcessInstanceId()).thenReturn("process");
        when(current.getTaskDefinitionKey()).thenReturn("source");
        Execution execution = mock(Execution.class);
        when(execution.getParentId()).thenReturn("mi-root");
        ExecutionQuery executionQuery = mock(ExecutionQuery.class);
        when(runtime.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId("child")).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);

        HistoricActivityInstanceQuery activityQuery = mock(HistoricActivityInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
        List<HistoricActivityInstance> activities = Arrays.asList(
                activity("other-branch", 100), activity("child", 400), activity("mi-root", 200));
        when(activityQuery.list()).thenReturn(activities);

        Date start = ReflectionTestUtils.invokeMethod(service, "getSendRoundStartTime", current);
        assertEquals(new Date(200), start);
        // 本轮会签的其他人员已发送的任务，即使早于当前子任务创建，也应保留。
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 300), start));
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 150), start));
        verify(activityQuery).processInstanceId("process");
        verify(activityQuery).activityId("source");
        verify(activityQuery).unfinished();

        when(activityQuery.list()).thenReturn(Collections.emptyList());
        assertEquals(new Date(400), ReflectionTestUtils.invokeMethod(service, "getSendRoundStartTime", current));
    }

    private static HistoricTaskInstance recipient(int status, long createdAt) {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getTaskLocalVariables()).thenReturn(Collections.singletonMap(BpmnVariableConstants.TASK_VARIABLE_STATUS, status));
        when(task.getCreateTime()).thenReturn(new Date(createdAt));
        return task;
    }

    private static HistoricActivityInstance activity(String executionId, long startedAt) {
        HistoricActivityInstance activity = mock(HistoricActivityInstance.class);
        when(activity.getExecutionId()).thenReturn(executionId);
        when(activity.getStartTime()).thenReturn(new Date(startedAt));
        return activity;
    }


    @Test
    void joinedQueryPreservesRecipientsAndLocalVariablesWithoutPerTaskQueries() {
        QueryCounter counter = new QueryCounter();
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:received-query-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setAsyncExecutorActivate(false);
        config.setCustomMybatisInterceptors(Collections.singletonList(counter));
        ProcessEngine engine = config.buildProcessEngine();
        try {
            engine.getRepositoryService().createDeployment().addString("recipients.bpmn20.xml", model()).deploy();
            String instance = engine.getRuntimeService().startProcessInstanceByKey("recipients",
                    Collections.singletonMap("members", IntStream.rangeClosed(1, 20)
                            .mapToObj(String::valueOf).collect(Collectors.toList()))).getId();
            List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(instance).list();
            for (Task task : tasks) {
                Map<String, Object> locals = new HashMap<>();
                locals.put("status", 1);
                locals.put("testMarker", task.getId());
                engine.getTaskService().setVariablesLocal(task.getId(), locals);
            }
            Task completed = tasks.stream().filter(task -> "bulk".equals(task.getTaskDefinitionKey())).findFirst().get();
            engine.getTaskService().setOwner(completed.getId(), "98");
            engine.getTaskService().complete(completed.getId());
            Set<String> selectedKeys = new HashSet<>(Arrays.asList("bulk", "empty"));

            counter.statements.clear();
            List<HistoricTaskInstance> original = engine.getHistoryService().createHistoricTaskInstanceQuery()
                    .processInstanceId(instance).includeTaskLocalVariables().list();
            Map<String, Set<Long>> expectedLinks = new HashMap<>();
            Map<String, Map<String, Object>> expectedLocals = new HashMap<>();
            for (HistoricTaskInstance task : original) {
                if (!selectedKeys.contains(task.getTaskDefinitionKey())) continue;
                expectedLinks.put(task.getId(), links(engine.getHistoryService().getHistoricIdentityLinksForTask(task.getId())));
                expectedLocals.put(task.getId(), task.getTaskLocalVariables());
            }
            int originalQueryCount = counter.statements.size();

            counter.statements.clear();
            List<HistoricTaskInstance> optimized = engine.getHistoryService().createHistoricTaskInstanceQuery()
                    .processInstanceId(instance).taskDefinitionKeys(selectedKeys)
                    .includeTaskLocalVariables().includeIdentityLinks().list();
            assertEquals(21, optimized.size(), "保留20个会签任务和一个没有人员关联的任务，排除其他节点");
            for (HistoricTaskInstance task : optimized) {
                assertEquals(expectedLinks.get(task.getId()), BpmProcessInstanceServiceImpl.getReceivedUserIds(task));
                assertEquals(expectedLocals.get(task.getId()), task.getTaskLocalVariables());
            }
            assertTrue(optimized.stream().anyMatch(task -> task.getId().equals(completed.getId()) && task.getEndTime() != null));
            assertEquals(43, originalQueryCount, "原API每个任务先验证任务存在，再查人员关联，共1+21*2次查询");
            assertEquals(1, counter.statements.size(), "加载和读取所有任务的关联时，不应额外查询数据库");
        } finally {
            engine.close();
        }
    }

    private static Set<Long> links(List<? extends IdentityLinkInfo> links) {
        return links.stream().filter(link -> link.getUserId() != null).map(link -> Long.valueOf(link.getUserId()))
                .collect(Collectors.toSet());
    }

    private static String model() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" "
                + "xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"test\">"
                + "<process id=\"recipients\" isExecutable=\"true\">"
                + "<startEvent id=\"start\"/><parallelGateway id=\"fork\"/><endEvent id=\"end\"/>"
                + "<userTask id=\"bulk\" flowable:assignee=\"${member}\" flowable:candidateUsers=\"99\">"
                + "<multiInstanceLoopCharacteristics isSequential=\"false\" flowable:collection=\"members\" "
                + "flowable:elementVariable=\"member\"/></userTask>"
                + "<userTask id=\"empty\"/><userTask id=\"unrelated\"/>"
                + "<sequenceFlow id=\"startFork\" sourceRef=\"start\" targetRef=\"fork\"/>"
                + Arrays.asList("bulk", "empty", "unrelated").stream().map(id ->
                        "<sequenceFlow id=\"to" + id + "\" sourceRef=\"fork\" targetRef=\"" + id + "\"/>"
                                + "<sequenceFlow id=\"from" + id + "\" sourceRef=\"" + id + "\" targetRef=\"end\"/>")
                        .collect(Collectors.joining())
                + "</process></definitions>";
    }

    @Intercepts(@Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
    public static class QueryCounter implements Interceptor {
        final List<String> statements = new ArrayList<>();

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            statements.add(((MappedStatement) invocation.getArgs()[0]).getId());
            return invocation.proceed();
        }
    }
}
