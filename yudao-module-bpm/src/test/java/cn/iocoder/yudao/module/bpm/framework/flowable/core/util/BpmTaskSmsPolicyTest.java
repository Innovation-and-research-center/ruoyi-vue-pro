package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** 使用真实引擎及项目的自定义会签行为，验证通知选择的持久化和消费边界。 */
class BpmTaskSmsPolicyTest {
    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        BpmTaskCandidateInvoker candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            DelegateExecution execution = call.getArgument(0);
            switch (execution.getCurrentActivityId()) {
                case "source": return new LinkedHashSet<>(Arrays.asList(1L, 2L, 3L));
                case "target": return new LinkedHashSet<>(Arrays.asList(4L, 5L, 6L));
                default: return new LinkedHashSet<>(Collections.singletonList(7L));
            }
        });
        BpmActivityBehaviorFactory factory = new BpmActivityBehaviorFactory();
        factory.setTaskCandidateInvoker(candidates);
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:sms-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setAsyncExecutorActivate(false);
        config.setActivityBehaviorFactory(factory);
        engine = config.buildProcessEngine();
    }

    @AfterEach
    void tearDown() {
        if (engine != null) engine.close();
    }

    @Test
    void delayedParallelRecipientsKeepEachApproversChoiceAndDefaultDoesNotLeak() {
        String instance = start(false, false);
        finishSource(instance, 1L, 4L, true);
        finishSource(instance, 2L, 5L, false);
        assertEquals(0, engine.getTaskService().createTaskQuery().taskDefinitionKey("target").count());
        finishSource(instance, 3L, 6L, true);
        assertPolicy(instance, "4", true);
        assertPolicy(instance, "5", false);
        assertPolicy(instance, "6", true);
        assertTrue(engine.getRuntimeService().getVariables(instance).keySet().stream()
                .noneMatch(k -> k.startsWith("bpm_task_sms_pending_")));
        for (Task task : engine.getTaskService().createTaskQuery().processInstanceId(instance).list()) {
            engine.getTaskService().complete(task.getId());
        }
        assertPolicy(instance, "7", true); // 下一次分派未提供选择，兼容发送。
    }

    @Test
    void sequentialMembersKeepOriginalChoiceWhileEarlierMembersSubmitOtherChoices() {
        String instance = start(true, false);
        finishSource(instance, 1L, 4L, false);
        finishSource(instance, 2L, 5L, false);
        finishSource(instance, 3L, 6L, true);
        assertPolicy(instance, "4", false);
        Task first = task(instance, "4");
        Execution member = engine.getRuntimeService().createExecutionQuery().executionId(first.getExecutionId()).singleResult();
        // 当前人提交下一步选择，不改变尚未轮到的目标成员短信选择。
        BpmTaskSmsPolicy.prepare(engine.getRuntimeService(), member.getParentId(),
                Collections.singletonMap("after", Collections.singletonList(7L)), true);
        engine.getTaskService().complete(first.getId());
        assertPolicy(instance, "5", false);
        engine.getTaskService().complete(task(instance, "5").getId());
        assertPolicy(instance, "6", true);
    }

    @Test
    void asyncCreationReadsPersistedSelectionsAndProcessInstancesAreIsolated() {
        String first = start(false, true);
        String second = start(false, true);
        for (long i = 1; i <= 3; i++) {
            finishSource(first, i, i + 3, false);
            finishSource(second, i, i + 3, true);
        }
        engine.getManagementService().createJobQuery().list().forEach(job ->
                engine.getManagementService().executeJob(job.getId()));
        for (int i = 4; i <= 6; i++) {
            assertPolicy(first, String.valueOf(i), false);
            assertPolicy(second, String.valueOf(i), true);
        }
    }

    private void finishSource(String instance, long user, long recipient, Boolean sms) {
        Task task = task(instance, String.valueOf(user));
        RuntimeService runtime = engine.getRuntimeService();
        Execution member = runtime.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        Execution root = runtime.createExecutionQuery().executionId(member.getParentId()).singleResult();
        BpmTaskSmsPolicy.prepare(runtime, root.getParentId(),
                Collections.singletonMap("target", Collections.singletonList(recipient)), sms);
        engine.getTaskService().complete(task.getId());
    }

    @Test
    void asyncRoleRecipientsUseDefaultChoiceWithoutExplicitUserSelection() {
        String instance = start(false, true);
        BpmTaskSmsPolicy.prepareDefault(engine.getRuntimeService(), instance, "target", false);
        for (long user = 1; user <= 3; user++) {
            engine.getTaskService().complete(task(instance, String.valueOf(user)).getId());
        }
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        engine.getManagementService().createJobQuery().processInstanceId(instance).list().forEach(job ->
                engine.getManagementService().executeJob(job.getId()));
        for (int user = 4; user <= 6; user++) {
            assertPolicy(instance, String.valueOf(user), false);
        }
        for (Task task : engine.getTaskService().createTaskQuery().processInstanceId(instance).list()) {
            engine.getTaskService().complete(task.getId());
        }
        assertPolicy(instance, "7", true);
    }

    private Task task(String instance, String user) {
        return engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee(user).singleResult();
    }

    private void assertPolicy(String instance, String user, boolean expected) {
        Task task = task(instance, user);
        assertNotNull(task);
        assertEquals(expected, engine.getTaskService().getVariableLocal(task.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
    }

    private String start(boolean sequential, boolean async) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("sms");
        model.addProcess(process);
        StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
        for (String id : Arrays.asList("source", "target", "after")) {
            UserTask task = new UserTask(); task.setId(id); task.setName(id);
            MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
            mi.setInputDataItem("${users}");
            mi.setElementVariable("user");
            mi.setSequential("target".equals(id) && sequential);
            mi.setCompletionCondition("${nrOfCompletedInstances >= nrOfInstances}");
            task.setLoopCharacteristics(mi);
            task.setAsynchronous("target".equals(id) && async);
            process.addFlowElement(task);
        }
        EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
        String[] ids = {"start", "source", "target", "after", "end"};
        for (int i = 0; i < ids.length - 1; i++) {
            SequenceFlow flow = new SequenceFlow(ids[i], ids[i + 1]);
            flow.setId("f" + i); process.addFlowElement(flow);
        }
        engine.getRepositoryService().createDeployment().addBpmnModel("sms.bpmn20.xml", model).deploy();
        return engine.getRuntimeService().startProcessInstanceByKey("sms").getId();
    }
}
