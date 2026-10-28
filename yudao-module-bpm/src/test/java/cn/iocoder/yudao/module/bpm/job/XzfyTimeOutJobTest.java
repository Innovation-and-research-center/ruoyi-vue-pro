package cn.iocoder.yudao.module.bpm.job;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzfy.XzfyMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XzfyTimeOutJobTest {
    private static final String DISPATCH = "Activity_1kq2pj8";
    private static final String HEAD = "Activity_176l096";
    private static final String ASSIST = "Activity_0f53oqt";
    private static final String DEPT = "Activity_0c9a0ni";
    private static final String LEGAL = "Activity_1h0vaw7";
    private static final String JOIN = "Gateway_0mwzqw4";
    private static final String TARGET = "Activity_1tghx6r";
    private static final String RESULT = "Activity_0ju01aa";
    private static ProcessEngine engine;
    private static String oldDefinition;
    private static String newDefinition;
    private XzfyTimeOutJob job;
    private XzfyMapper mapper;
    private HolidayService holidays;

    @BeforeAll
    static void createEngine() {
        BpmTaskCandidateInvoker candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            DelegateExecution execution = call.getArgument(0);
            if (TARGET.equals(execution.getCurrentActivityId())) {
                if (Boolean.TRUE.equals(execution.getVariable("failJump"))) {
                    throw new IllegalStateException("模拟后续任务创建失败");
                }
                return new LinkedHashSet<>(Collections.singletonList(
                        ((Number) execution.getVariable("transactorUserId")).longValue()));
            }
            return new LinkedHashSet<>(DISPATCH.equals(execution.getCurrentActivityId())
                    || RESULT.equals(execution.getCurrentActivityId())
                    ? Collections.singletonList(1L) : Arrays.asList(2L, 3L));
        });
        BpmActivityBehaviorFactory factory = new BpmActivityBehaviorFactory();
        factory.setTaskCandidateInvoker(candidates);
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:xzfy-timeout-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setAsyncExecutorActivate(false);
        config.setActivityBehaviorFactory(factory);
        engine = config.buildProcessEngine();
        oldDefinition = deploy(false);
        newDefinition = deploy(true);
    }

    @AfterAll
    static void closeEngine() {
        if (engine != null) engine.close();
    }

    @BeforeEach
    void setUp() {
        job = new XzfyTimeOutJob();
        mapper = mock(XzfyMapper.class);
        holidays = mock(HolidayService.class);
        ReflectionTestUtils.setField(job, "xzfyMapper", mapper);
        ReflectionTestUtils.setField(job, "holidayService", holidays);
        ReflectionTestUtils.setField(job, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(job, "taskService", engine.getTaskService());
        ReflectionTestUtils.setField(job, "managementService", engine.getManagementService());
        when(holidays.addWorkingDays(any(), eq(10))).thenReturn(LocalDateTime.now().minusDays(1));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
        engine.getRuntimeService().createProcessInstanceQuery().list().forEach(
                instance -> engine.getRuntimeService().deleteProcessInstance(instance.getId(), "test cleanup"));
    }

    @Test
    void oldInstancesStillTimeoutAtEachOriginalNodeAfterNewVersionIsDeployed() throws Exception {
        for (String node : Arrays.asList(HEAD, ASSIST, DEPT)) {
            String instance = start(false, node);
            assertEquals(oldDefinition, engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance).singleResult().getProcessDefinitionId());
            List<Task> previous = tasks(instance);
            assertEquals(2, previous.size());
            assertEquals("成功跳转 1 个实例", execute(instance));
            assertOnlyTarget(instance);
            assertTimeoutReasons(previous);
            assertEquals("成功跳转 0 个实例", execute(instance));
            assertOnlyTarget(instance);
        }
    }

    @Test
    void optionalLegalBranchCanTimeoutAloneOrTogetherWithOtherBranches() throws Exception {
        for (String[] selected : new String[][]{{LEGAL}, {DEPT}, {DEPT, LEGAL}, {HEAD, ASSIST, DEPT, LEGAL}}) {
            String instance = start(true, selected);
            List<Task> previous = tasks(instance);
            assertEquals("成功跳转 1 个实例", execute(instance));
            assertOnlyTarget(instance);
            assertTimeoutReasons(previous);
        }
    }

    @Test
    void timeoutAlsoCollectsTheBranchAlreadyWaitingAtTheGateway() throws Exception {
        for (String finished : Arrays.asList(DEPT, LEGAL)) {
            String instance = start(true, DEPT, LEGAL);
            complete(instance, finished);
            assertEquals(1, waitingCount(instance));
            List<Task> remaining = tasks(instance);
            assertEquals("成功跳转 1 个实例", execute(instance));
            assertOnlyTarget(instance);
            assertTimeoutReasons(remaining);
            assertEquals(0, waitingCount(instance));
            complete(instance, TARGET);
            assertEquals(RESULT, tasks(instance).get(0).getTaskDefinitionKey());
            assertEquals("成功跳转 0 个实例", execute(instance));
            complete(instance, RESULT);
            assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
        }
    }

    @Test
    void normalCompletionWaitsOnlyForSelectedBranches() {
        for (boolean selected : new boolean[]{false, true}) {
            String instance = start(true, selected ? new String[]{DEPT, LEGAL} : new String[]{DEPT});
            complete(instance, DEPT);
            if (selected) {
                assertEquals(0, engine.getTaskService().createTaskQuery()
                        .processInstanceId(instance).taskDefinitionKey(TARGET).count());
                complete(instance, LEGAL);
            }
            assertOnlyTarget(instance);
        }
    }

    @Test
    void missingPresetUserDoesNotCloseTasksOrWriteTimeoutOpinions() throws Exception {
        String instance = start(true, LEGAL);
        engine.getRuntimeService().removeVariable(instance, "transactorUserId");
        List<Task> previous = tasks(instance);
        assertEquals("成功跳转 0 个实例", execute(instance));
        assertUnchanged(instance, previous);
    }

    @Test
    void jumpFailureRollsBackCommentsAndStatusAndCanBeRetried() throws Exception {
        String instance = start(true, DEPT, LEGAL);
        complete(instance, DEPT);
        List<Task> previous = tasks(instance);
        engine.getRuntimeService().setVariable(instance, "failJump", true);
        assertEquals("成功跳转 0 个实例", execute(instance));
        assertUnchanged(instance, previous);
        assertEquals(1, waitingCount(instance));
        engine.getRuntimeService().setVariable(instance, "failJump", false);
        assertEquals("成功跳转 1 个实例", execute(instance));
        assertOnlyTarget(instance);
    }

    @Test
    void doesNotTimeoutBeforeDeadlineOrAtDispatchOrOutsideTenant() throws Exception {
        String instance = start(true, LEGAL);
        List<Task> previous = tasks(instance);
        when(holidays.addWorkingDays(any(), eq(10))).thenReturn(LocalDateTime.now().plusDays(1));
        assertEquals("成功跳转 0 个实例", execute(instance));
        assertUnchanged(instance, previous);
        when(holidays.addWorkingDays(any(), eq(10))).thenReturn(LocalDateTime.now().minusDays(1));
        String dispatch = engine.getRuntimeService().startProcessInstanceById(newDefinition).getId();
        assertEquals("成功跳转 0 个实例", execute(dispatch));
        assertEquals(DISPATCH, tasks(dispatch).get(0).getTaskDefinitionKey());
        TenantContextHolder.setTenantId(2L);
        assertEquals("跳过非目标租户", execute(instance));
        assertUnchanged(instance, previous);
    }

    private String execute(String instance) throws Exception {
        XzfyDO doc = new XzfyDO();
        doc.setProcessInstanceId(instance);
        doc.setSwRq(LocalDateTime.now().minusDays(30));
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(doc));
        return job.execute("");
    }

    private void assertOnlyTarget(String instance) {
        assertEquals(1, tasks(instance).size());
        assertEquals(TARGET, tasks(instance).get(0).getTaskDefinitionKey());
        assertEquals("99", tasks(instance).get(0).getAssignee());
    }

    private void assertTimeoutReasons(List<Task> previous) {
        previous.forEach(task -> {
            Map<String, Object> vars = engine.getHistoryService().createHistoricTaskInstanceQuery()
                    .taskId(task.getId()).includeTaskLocalVariables().singleResult().getTaskLocalVariables();
            assertEquals(2, vars.get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
            assertEquals("超时系统自动完成", vars.get(BpmnVariableConstants.TASK_VARIABLE_REASON));
            assertEquals(1, engine.getTaskService().getTaskComments(task.getId()).size());
        });
    }

    private void assertUnchanged(String instance, List<Task> previous) {
        assertEquals(previous.stream().map(Task::getId).collect(Collectors.toSet()),
                tasks(instance).stream().map(Task::getId).collect(Collectors.toSet()));
        previous.forEach(task -> {
            assertNull(engine.getTaskService().getVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON));
            assertTrue(engine.getTaskService().getTaskComments(task.getId()).isEmpty());
        });
    }

    private static List<Task> tasks(String instance) {
        return engine.getTaskService().createTaskQuery().processInstanceId(instance).list();
    }

    private static long waitingCount(String instance) {
        // activityId(...) 查询仅返回 active 执行，会漏掉包容网关的 inactive 等待分支。
        return engine.getRuntimeService().createExecutionQuery().processInstanceId(instance).list().stream()
                .filter(e -> JOIN.equals(e.getActivityId())).count();
    }

    private static void complete(String instance, String node) {
        tasks(instance).stream().filter(task -> node.equals(task.getTaskDefinitionKey()))
                .forEach(task -> engine.getTaskService().complete(task.getId()));
    }

    private static String start(boolean modern, String... selected) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("transactorUserId", 99L);
        for (String id : Arrays.asList(HEAD, ASSIST, DEPT, LEGAL)) vars.put("select_" + id, false);
        for (String id : selected) vars.put("select_" + id, true);
        String instance = engine.getRuntimeService().startProcessInstanceById(
                modern ? newDefinition : oldDefinition, vars).getId();
        complete(instance, DISPATCH);
        return instance;
    }

    private static String deploy(boolean modern) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("oa_review"); model.addProcess(process);
        StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
        EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
        InclusiveGateway join = new InclusiveGateway(); join.setId(JOIN); process.addFlowElement(join);
        List<String> ids = new ArrayList<>(Arrays.asList(DISPATCH, HEAD, ASSIST, DEPT, TARGET, RESULT));
        if (modern) ids.add(LEGAL);
        for (String id : ids) {
            UserTask task = new UserTask(); task.setId(id); task.setName(id);
            MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
            mi.setInputDataItem("${coll_userList}");
            mi.setCompletionCondition("${nrOfCompletedInstances >= nrOfInstances}");
            task.setLoopCharacteristics(mi);
            process.addFlowElement(task);
        }
        flow(process, "start", DISPATCH, null);
        for (String id : modern ? Arrays.asList(HEAD, ASSIST, DEPT, LEGAL) : Arrays.asList(HEAD, ASSIST, DEPT)) {
            flow(process, DISPATCH, id, "${select_" + id + "}");
        }
        flow(process, HEAD, DEPT, null);
        flow(process, ASSIST, DEPT, null);
        flow(process, DEPT, JOIN, null);
        if (modern) flow(process, LEGAL, JOIN, null);
        flow(process, JOIN, TARGET, null);
        flow(process, TARGET, RESULT, null);
        flow(process, RESULT, "end", null);
        String deployment = engine.getRepositoryService().createDeployment()
                .addBpmnModel("xzfy.bpmn20.xml", model).deploy().getId();
        return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment).singleResult().getId();
    }

    private static void flow(org.flowable.bpmn.model.Process process, String from, String to, String condition) {
        SequenceFlow flow = new SequenceFlow(from, to);
        flow.setId(from + "_" + to);
        flow.setConditionExpression(condition);
        process.addFlowElement(flow);
    }
}
