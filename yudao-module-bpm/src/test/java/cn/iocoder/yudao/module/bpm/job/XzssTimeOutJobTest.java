package cn.iocoder.yudao.module.bpm.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XzssTimeOutJobTest {
    enum Stage {
        NORMAL("Activity_0b1nu1k", "Activity_06bpq99", "Activity_16wkjql", "Activity_1q3wpn8",
                "Activity_0xurhyv", "Gateway_1cwnri4", "Activity_01ukxlz", "transactorUserId", 99L, "过期系统自动完成"),
        APPEAL("Activity_1489xi0", "Activity_03i9nb5", "Activity_1j3mjl5", "Activity_0c22of2",
                "Activity_1g5bzru", "Gateway_0trntwd", "Activity_0gfzyss", "lawsuitUserId", 100L, "上诉阶段过期系统自动完成");

        final String dispatch, head, assist, dept, legal, join, target, userVariable, reason;
        final long user;

        Stage(String dispatch, String head, String assist, String dept, String legal, String join,
              String target, String userVariable, long user, String reason) {
            this.dispatch = dispatch; this.head = head; this.assist = assist; this.dept = dept;
            this.legal = legal; this.join = join; this.target = target;
            this.userVariable = userVariable; this.user = user; this.reason = reason;
        }

        String success(int count) {
            return (this == APPEAL ? "上诉阶段" : "") + "成功跳转 " + count + " 个实例";
        }
    }

    private static final String RESULT = "Activity_1rmxz5k";
    private static ProcessEngine engine;
    private static String oldDefinition, newDefinition;
    private XzssMapper mapper;
    private JobHandler normalJob, appealJob;

    @BeforeAll
    static void createEngine() {
        BpmTaskCandidateInvoker candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            DelegateExecution execution = call.getArgument(0);
            for (Stage stage : Stage.values()) {
                if (stage.target.equals(execution.getCurrentActivityId())) {
                    if (stage.target.equals(execution.getVariable("failJumpAt"))) {
                        throw new IllegalStateException("模拟后续办理创建失败");
                    }
                    return new LinkedHashSet<>(Collections.singletonList(
                            ((Number) execution.getVariable(stage.userVariable)).longValue()));
                }
                if (stage.dispatch.equals(execution.getCurrentActivityId())) {
                    return new LinkedHashSet<>(Collections.singletonList(1L));
                }
            }
            return new LinkedHashSet<>(RESULT.equals(execution.getCurrentActivityId())
                    ? Collections.singletonList(1L) : Arrays.asList(2L, 3L));
        });
        BpmActivityBehaviorFactory factory = new BpmActivityBehaviorFactory();
        factory.setTaskCandidateInvoker(candidates);
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:xzss-timeout-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
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
        mapper = mock(XzssMapper.class);
        normalJob = new XzssTimeOutJob();
        appealJob = new XzssAppealTimeOutJob();
        for (JobHandler job : Arrays.asList(normalJob, appealJob)) {
            ReflectionTestUtils.setField(job, "xzssMapper", mapper);
            ReflectionTestUtils.setField(job, "runtimeService", engine.getRuntimeService());
            ReflectionTestUtils.setField(job, "taskService", engine.getTaskService());
            ReflectionTestUtils.setField(job, "managementService", engine.getManagementService());
        }
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
        engine.getRuntimeService().createProcessInstanceQuery().list().forEach(
                instance -> engine.getRuntimeService().deleteProcessInstance(instance.getId(), "test cleanup"));
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void oldInstancesStillTimeoutAtEveryOriginalNodeAfterNewVersionIsDeployed(Stage stage) throws Exception {
        for (String node : Arrays.asList(stage.head, stage.assist, stage.dept)) {
            String instance = start(false, stage, node);
            assertEquals(oldDefinition, engine.getRuntimeService().createProcessInstanceQuery()
                    .processInstanceId(instance).singleResult().getProcessDefinitionId());
            List<Task> previous = tasks(instance);
            assertEquals(2, previous.size());
            assertEquals(stage.success(1), execute(stage, instance));
            assertOnlyTarget(stage, instance);
            assertTimeoutReasons(stage, previous);
            assertEquals(stage.success(0), execute(stage, instance));
            assertOnlyTarget(stage, instance);
        }
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void optionalNewNodeAloneOrAlongsideMultipleBranchesCreatesOnlyOneTarget(Stage stage) throws Exception {
        for (String[] selected : new String[][]{{stage.legal}, {stage.dept}, {stage.dept, stage.legal},
                {stage.head, stage.assist, stage.dept, stage.legal}}) {
            String instance = start(true, stage, selected);
            List<Task> previous = tasks(instance);
            assertEquals(stage.success(1), execute(stage, instance));
            assertOnlyTarget(stage, instance);
            assertTimeoutReasons(stage, previous);
        }
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void timeoutCollectsGatewayWaitingBranchesInBothCompletionOrders(Stage stage) throws Exception {
        for (String finished : Arrays.asList(stage.dept, stage.legal)) {
            String instance = start(true, stage, stage.dept, stage.legal);
            complete(instance, finished);
            assertEquals(1, waitingCount(stage, instance));
            List<Task> previous = tasks(instance);
            assertEquals(stage.success(1), execute(stage, instance));
            assertOnlyTarget(stage, instance);
            assertTimeoutReasons(stage, previous);
            assertEquals(0, waitingCount(stage, instance));
            assertEquals(stage.success(0), execute(stage, instance));
            complete(instance, stage.target);
            if (stage == Stage.NORMAL) {
                assertEquals(RESULT, tasks(instance).get(0).getTaskDefinitionKey());
                assertEquals(stage.success(0), execute(stage, instance));
                complete(instance, RESULT);
                assertEquals(Stage.APPEAL.dispatch, tasks(instance).get(0).getTaskDefinitionKey());
            } else {
                assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
            }
        }
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void normalCompletionWaitsOnlyForSelectedBranchesAndForEveryMember(Stage stage) {
        for (String[] selected : new String[][]{{stage.legal}, {stage.dept},
                {stage.dept, stage.legal}, {stage.legal, stage.dept}}) {
            String instance = start(true, stage, selected);
            for (int i = 0; i < selected.length; i++) {
                String node = selected[i];
                Task first = tasks(instance).stream().filter(task -> node.equals(task.getTaskDefinitionKey())).findFirst().get();
                engine.getTaskService().complete(first.getId());
                assertEquals(0, engine.getTaskService().createTaskQuery()
                        .processInstanceId(instance).taskDefinitionKey(stage.target).count());
                complete(instance, selected[i]);
                if (i < selected.length - 1) assertEquals(0, engine.getTaskService().createTaskQuery()
                        .processInstanceId(instance).taskDefinitionKey(stage.target).count());
            }
            assertOnlyTarget(stage, instance);
        }
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void otherStageJobDoesNotCloseOrCommentOnCurrentStage(Stage stage) throws Exception {
        String instance = start(true, stage, stage.dept, stage.legal);
        List<Task> previous = tasks(instance);
        Stage other = stage == Stage.NORMAL ? Stage.APPEAL : Stage.NORMAL;
        assertEquals(other.success(0), execute(other, instance));
        assertUnchanged(instance, previous);
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void missingStageSpecificUserDoesNotUseTheOtherStagesPreset(Stage stage) throws Exception {
        String instance = start(true, stage, stage.legal);
        engine.getRuntimeService().removeVariable(instance, stage.userVariable);
        List<Task> previous = tasks(instance);
        assertEquals(stage.success(0), execute(stage, instance));
        assertUnchanged(instance, previous);
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void targetCreationFailureRollsBackCommentsAndStatusAndCanBeRetried(Stage stage) throws Exception {
        String instance = start(true, stage, stage.dept, stage.legal);
        complete(instance, stage.dept);
        List<Task> previous = tasks(instance);
        engine.getRuntimeService().setVariable(instance, "failJumpAt", stage.target);
        assertEquals(stage.success(0), execute(stage, instance));
        assertUnchanged(instance, previous);
        assertEquals(1, waitingCount(stage, instance));
        engine.getRuntimeService().removeVariable(instance, "failJumpAt");
        assertEquals(stage.success(1), execute(stage, instance));
        assertOnlyTarget(stage, instance);
    }

    @ParameterizedTest @EnumSource(Stage.class)
    void emptyCandidateListAndNonTargetTenantDoNotChangeTasks(Stage stage) throws Exception {
        String instance = start(true, stage, stage.legal);
        List<Task> previous = tasks(instance);
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        assertEquals("成功，无处理数据", job(stage).execute(""));
        assertUnchanged(instance, previous);
        TenantContextHolder.setTenantId(2L);
        assertEquals("跳过非目标租户", execute(stage, instance));
        assertUnchanged(instance, previous);
    }

    private JobHandler job(Stage stage) { return stage == Stage.NORMAL ? normalJob : appealJob; }

    private String execute(Stage stage, String instance) throws Exception {
        XzssDO doc = new XzssDO();
        doc.setProcessInstanceId(instance);
        doc.setSwRq(LocalDateTime.now().minusDays(30));
        doc.setSfyjgRq(LocalDateTime.now().minusDays(1));
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(doc));
        return job(stage).execute("");
    }

    private void assertOnlyTarget(Stage stage, String instance) {
        assertEquals(1, tasks(instance).size());
        assertEquals(stage.target, tasks(instance).get(0).getTaskDefinitionKey());
        assertEquals(String.valueOf(stage.user), tasks(instance).get(0).getAssignee());
    }

    private void assertTimeoutReasons(Stage stage, List<Task> previous) {
        previous.forEach(task -> {
            Map<String, Object> vars = engine.getHistoryService().createHistoricTaskInstanceQuery()
                    .taskId(task.getId()).includeTaskLocalVariables().singleResult().getTaskLocalVariables();
            assertEquals(2, vars.get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
            assertEquals(stage.reason, vars.get(BpmnVariableConstants.TASK_VARIABLE_REASON));
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

    private static void complete(String instance, String node) {
        tasks(instance).stream().filter(task -> node.equals(task.getTaskDefinitionKey()))
                .forEach(task -> engine.getTaskService().complete(task.getId()));
    }

    private static long waitingCount(Stage stage, String instance) {
        return engine.getRuntimeService().createExecutionQuery().processInstanceId(instance).list().stream()
                .filter(execution -> stage.join.equals(execution.getActivityId())).count();
    }

    private static String start(boolean modern, Stage stage, String... selected) {
        Map<String, Object> vars = new HashMap<>();
        for (Stage s : Stage.values()) {
            vars.put(s.userVariable, s.user);
            for (String id : Arrays.asList(s.head, s.assist, s.dept, s.legal)) vars.put("select_" + id, false);
        }
        for (String id : selected) vars.put("select_" + id, true);
        if (stage == Stage.APPEAL) vars.put("select_" + Stage.NORMAL.dept, true);
        String instance = engine.getRuntimeService().startProcessInstanceById(
                modern ? newDefinition : oldDefinition, vars).getId();
        complete(instance, Stage.NORMAL.dispatch);
        if (stage == Stage.APPEAL) {
            complete(instance, Stage.NORMAL.dept);
            complete(instance, Stage.NORMAL.target);
            complete(instance, RESULT);
            complete(instance, Stage.APPEAL.dispatch);
        }
        return instance;
    }

    private static String deploy(boolean modern) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("oa_lawsuit"); model.addProcess(process);
        StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
        EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
        addTask(process, RESULT, false);
        for (Stage stage : Stage.values()) {
            InclusiveGateway join = new InclusiveGateway(); join.setId(stage.join); process.addFlowElement(join);
            List<String> ids = new ArrayList<>(Arrays.asList(stage.dispatch, stage.head, stage.assist, stage.dept, stage.target));
            if (modern) ids.add(stage.legal);
            for (String id : ids) addTask(process, id, stage == Stage.NORMAL && id.equals(stage.dispatch));
            for (String id : modern ? Arrays.asList(stage.head, stage.assist, stage.dept, stage.legal)
                    : Arrays.asList(stage.head, stage.assist, stage.dept)) {
                flow(process, stage.dispatch, id, "${select_" + id + "}");
            }
            flow(process, stage.head, stage.assist, null);
            flow(process, stage.assist, stage.dept, null);
            flow(process, stage.dept, stage.join, null);
            if (modern) flow(process, stage.legal, stage.join, null);
            flow(process, stage.join, stage.target, null);
        }
        flow(process, "start", Stage.NORMAL.dispatch, null);
        flow(process, Stage.NORMAL.target, RESULT, null);
        flow(process, RESULT, Stage.APPEAL.dispatch, null);
        flow(process, Stage.APPEAL.target, "end", null);
        String deployment = engine.getRepositoryService().createDeployment()
                .addBpmnModel("xzss.bpmn20.xml", model).deploy().getId();
        return engine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment).singleResult().getId();
    }

    private static void addTask(org.flowable.bpmn.model.Process process, String id, boolean sequential) {
        UserTask task = new UserTask(); task.setId(id); task.setName(id);
        MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
        mi.setSequential(sequential);
        if (sequential) mi.setLoopCardinality("1");
        mi.setInputDataItem("${coll_userList}");
        mi.setCompletionCondition("${nrOfCompletedInstances >= nrOfInstances}");
        task.setLoopCharacteristics(mi); process.addFlowElement(task);
    }

    private static void flow(org.flowable.bpmn.model.Process process, String from, String to, String condition) {
        SequenceFlow flow = new SequenceFlow(from, to);
        flow.setId(from + "_" + to); flow.setConditionExpression(condition); process.addFlowElement(flow);
    }
}
