package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskBatchApproveReqVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmDispatchSources;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmProcessOperationVersion;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import org.flowable.bpmn.model.*;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BpmRestoredDispatchTest {
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final Task task = mock(Task.class);
    private final BpmTaskServiceImpl service = new BpmTaskServiceImpl();
    private final BpmnModel model = new BpmnModel();
    private final UserTask target = new UserTask();

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        when(task.getExecutionId()).thenReturn("restored");
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getTaskDefinitionKey()).thenReturn("director");
        when(runtime.getVariable("restored", BpmDispatchSources.RESTORED_FROM))
                .thenReturn(Collections.singletonMap("director", Collections.singleton("oldTask")));
        when(runtime.getVariableLocal("instance", BpmDispatchSources.LEDGER_PREFIX + "oldTask"))
                .thenReturn(Collections.singletonMap("target", new LinkedHashSet<>(Arrays.asList(4L, 6L))));
        target.setId("target"); target.setName("主办科室");
        ExtensionElement strategy = new ExtensionElement(); strategy.setName("candidateStrategy"); strategy.setElementText("55");
        target.addExtensionElement(strategy);
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("process"); process.addFlowElement(target); model.addProcess(process);
    }

    @Test
    void retainedMembersRejectedButWithdrawnAndNewRecipientsAllowed() {
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 4L));
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 5L, 6L));
        assertDoesNotThrow(() -> validate(Collections.singleton("target"), 5L, 8L));
    }

    @Test
    void unselectedRouteDoesNotBlockOtherDispatchOrCompletion() {
        assertDoesNotThrow(() -> validate(Collections.emptySet(), 4L));
    }

    @Test
    void automaticRouteCannotRedispatchRetainedMembersThroughForgedSelection() {
        target.getExtensionElements().clear();
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target")));
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 8L));
        when(runtime.getVariableLocal("instance", BpmDispatchSources.LEDGER_PREFIX + "oldTask"))
                .thenReturn(Collections.emptyMap());
        assertDoesNotThrow(() -> validate(Collections.singleton("target")));
    }

    @Test
    void ordinaryTaskWithoutRecoveryContextKeepsExistingRules() {
        when(runtime.getVariable("restored", BpmDispatchSources.RESTORED_FROM)).thenReturn(null);
        assertDoesNotThrow(() -> validate(Collections.singleton("target"), 4L));
    }

    @Test
    void returnedTaskCanSelectOtherRoutesWhileRetainedPeopleStayProtected() {
        when(runtime.getVariable("restored", BpmDispatchSources.RESTORED_FROM)).thenReturn(null);
        when(runtime.getVariable("restored", BpmTaskReturnService.SCOPE))
                .thenReturn(Collections.singletonMap("director", "oldRoute"));
        when(runtime.getVariable("restored", BpmTaskReturnService.RETAINED_SOURCES))
                .thenReturn(Collections.singletonMap("director", Collections.singleton("oldTask")));
        assertDoesNotThrow(() -> validate(Collections.singleton("target"), 8L));
        cn.iocoder.yudao.framework.common.exception.ServiceException error = assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> validate(Collections.singleton("target"), 4L));
        assertEquals("发送失败：该环节（主办科室）仍有已接收人员，请勿重复选择", error.getMessage());
        assertDoesNotThrow(() -> validate(Collections.emptySet()));
        target.getExtensionElements().clear();
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 8L));
    }

    private void validate(Set<String> selected, Long... users) {
        service.validateRestoredDispatch(task, model, selected,
                Collections.singletonMap("target", Arrays.asList(users)));
    }

    @Nested
    class BatchCompletion {
        private ProcessEngine engine;
        private BpmTaskServiceImpl service;

        @BeforeEach
        void setUp() {
            StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
            config.setJdbcUrl("jdbc:h2:mem:batch-end-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
            config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
            config.setAsyncExecutorActivate(false);
            engine = config.buildProcessEngine();
            service = new BpmTaskServiceImpl();
            BpmModelService models = mock(BpmModelService.class);
            when(models.getBpmnModelByDefinitionId(anyString())).thenAnswer(call ->
                    engine.getRepositoryService().getBpmnModel(call.getArgument(0)));
            BpmProcessInstanceService instances = mock(BpmProcessInstanceService.class);
            when(instances.getProcessInstance(anyString())).thenAnswer(call -> engine.getRuntimeService()
                    .createProcessInstanceQuery().processInstanceId(call.getArgument(0)).includeProcessVariables().singleResult());
            ReflectionTestUtils.setField(service, "modelService", models);
            ReflectionTestUtils.setField(service, "processInstanceService", instances);
            ReflectionTestUtils.setField(service, "taskService", engine.getTaskService());
            ReflectionTestUtils.setField(service, "runtimeService", engine.getRuntimeService());
            ReflectionTestUtils.setField(service, "historyService", engine.getHistoryService());
            ReflectionTestUtils.setField(service, "managementService", engine.getManagementService());
        }

        @AfterEach
        void close() {
            if (engine != null) engine.close();
        }

        @Test
        void batchCompletionSetsRealEndConditionAndKeepsParallelHostInBothVersions() {
            for (boolean legacy : new boolean[]{true, false}) {
                String instance = start("${variables:get(select_node_136) == 'end'}", legacy, false);
                Task leader = task(instance, "leader");
                Task host = task(instance, "host");
                submit(leader.getId());
                assertNull(task(instance, "leader"));
                assertEquals(host.getId(), task(instance, "host").getId());
                assertNull(task(instance, "dispatch"));
                assertNotNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
                assertEquals(2, engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(leader.getId())
                        .includeTaskLocalVariables().singleResult().getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
            }
        }

        @Test
        void staleTransferChoiceOnRestoredBranchDoesNotCreateAnotherHostTask() {
            String instance = start("${variables:get(select_node_136) == 'end'}", false, false);
            Task leader = task(instance, "leader");
            Task host = task(instance, "host");
            String root = engine.getRuntimeService().createExecutionQuery().executionId(leader.getExecutionId())
                    .singleResult().getParentId();
            String branch = engine.getRuntimeService().createExecutionQuery().executionId(root).singleResult().getParentId();
            engine.getRuntimeService().setVariableLocal(branch, BpmDispatchSources.BRANCH, true);
            engine.getRuntimeService().setVariableLocal(branch, "select_node_134", "head");
            engine.getRuntimeService().setVariableLocal(host.getExecutionId(), "select_node_134", "head");
            submit(leader.getId());
            assertNull(task(instance, "dispatch"));
            assertEquals(host.getId(), task(instance, "host").getId());
            assertEquals("head", engine.getRuntimeService().getVariableLocal(host.getExecutionId(), "select_node_134"));
        }

        @Test
        void unconditionalCompletionStillWorks() {
            String instance = start(null, true, false);
            submit(task(instance, "leader").getId());
            assertNull(task(instance, "leader"));
            assertNotNull(task(instance, "host"));
            assertNull(task(instance, "dispatch"));
        }

        @Test
        void manualCompletionSupportsQuotedKeysAndDifferentRouteVariables() {
            for (String condition : Arrays.asList("${variables:get('select_node_136') == 'end'}",
                    "${variables:get(next_node42) == 'finish'}")) {
                String instance = start(condition, false, false);
                submit(task(instance, "leader").getId());
                assertNull(task(instance, "leader"));
                assertNotNull(task(instance, "host"));
                assertNull(task(instance, "dispatch"));
            }
        }

        @Test
        void finishingOneCountersignMemberKeepsTheOtherLeaderTask() {
            String instance = start("${variables:get(select_node_136) == 'end'}", false, false);
            Task leader = task(instance, "leader");
            org.flowable.engine.runtime.Execution added = engine.getRuntimeService().addMultiInstanceExecution(
                    "leader", instance, Collections.singletonMap("leaderUser", 10L));
            String second = engine.getTaskService().createTaskQuery().executionId(added.getId()).singleResult().getId();
            submit(leader.getId());
            assertEquals(second, task(instance, "leader").getId());
            assertNotNull(task(instance, "host"));
            submit(second);
            assertNull(task(instance, "leader"));
            assertNotNull(task(instance, "host"));
        }

        @Test
        void conditionalGatewayPathCannotBeMistakenForUnconditionalCompletion() {
            String instance = start("${variables:get(select_node_136) == 'end'}", false, false);
            Task leader = task(instance, "leader");
            String definition = engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance)
                    .singleResult().getProcessDefinitionId();
            BpmnModel model = engine.getRepositoryService().getBpmnModel(definition);
            ((SequenceFlow) model.getFlowElement("join_end")).setConditionExpression("${amount > 100}");
            assertThrows(ServiceException.class, () -> submit(leader.getId()));
            assertEquals(leader.getId(), task(instance, "leader").getId());
        }

        @Test
        void businessConditionIsNotRewrittenToForceCompletion() {
            String instance = start("${amount > 100}", false, false);
            Task leader = task(instance, "leader");
            assertThrows(ServiceException.class, () -> submit(leader.getId()));
            assertEquals(leader.getId(), task(instance, "leader").getId());
            assertEquals(1, engine.getRuntimeService().getVariable(instance, "amount"));
        }

        @Test
        void laterIneligibleTaskRollsBackEntireBatch() {
            String instance = start("${variables:get(select_node_136) == 'end'}", false, true);
            Task leader = task(instance, "leader");
            Task host = task(instance, "host");
            assertThrows(ServiceException.class, () -> submit(leader.getId(), host.getId()));
            assertEquals(leader.getId(), task(instance, "leader").getId());
            assertEquals(host.getId(), task(instance, "host").getId());
        }

        private void submit(String... ids) {
            BpmTaskBatchApproveReqVO req = new BpmTaskBatchApproveReqVO();
            req.setIds(Arrays.asList(ids));
            // 同一引擎事务覆盖整批操作，模拟业务入口的事务回滚边界。
            engine.getManagementService().executeCommand(context -> { service.batchApproveTaskIfEnd(10L, req); return null; });
        }

        private Task task(String instance, String node) {
            return engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(node).singleResult();
        }

        private String start(String endCondition, boolean legacy, boolean hostNeedsAnotherTask) {
            BpmnModel model = new BpmnModel();
            org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
            process.setId("batchReceive"); model.addProcess(process);
            StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
            ParallelGateway fork = new ParallelGateway(); fork.setId("fork"); process.addFlowElement(fork);
            InclusiveGateway join = new InclusiveGateway(); join.setId("join"); process.addFlowElement(join);
            EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
            for (String id : Arrays.asList("leader", "host", "dispatch")) {
                UserTask node = new UserTask(); node.setId(id); node.setName(id);
                node.setAssignee("leader".equals(id) ? "${leaderUser}" : "host".equals(id) && !hostNeedsAnotherTask ? "20" : "10");
                if ("leader".equals(id)) {
                    MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
                    mi.setInputDataItem("${leaderUsers}"); mi.setElementVariable("leaderUser");
                    mi.setCompletionCondition("${nrOfCompletedInstances >= nrOfInstances}");
                    node.setLoopCharacteristics(mi);
                }
                if ("dispatch".equals(id)) {
                    ExtensionElement strategy = new ExtensionElement(); strategy.setName("candidateStrategy"); strategy.setElementText("55");
                    strategy.setNamespace("http://flowable.org/bpmn"); strategy.setNamespacePrefix("flowable");
                    node.addExtensionElement(strategy);
                }
                process.addFlowElement(node);
            }
            flow(process, "start", "fork", null);
            flow(process, "fork", "leader", null);
            flow(process, "fork", "host", null);
            flow(process, "leader", "join", endCondition);
            flow(process, "leader", "dispatch", "${variables:get(select_node_134) == 'head'}");
            flow(process, "dispatch", "join", null);
            flow(process, "host", hostNeedsAnotherTask ? "dispatch" : "join", null);
            flow(process, "join", "end", null);
            engine.getRepositoryService().createDeployment().addBpmnModel("batch.bpmn20.xml", model).deploy();
            Map<String, Object> variables = new HashMap<>();
            variables.put("leaderUsers", Collections.singletonList(10L)); variables.put("amount", 1);
            variables.put("select_node_136", null); variables.put("select_node_134", null);
            if (!legacy) variables.put(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT);
            return engine.getRuntimeService().startProcessInstanceByKey("batchReceive", variables).getId();
        }

        private void flow(org.flowable.bpmn.model.Process process, String source, String target, String condition) {
            SequenceFlow flow = new SequenceFlow(source, target); flow.setId(source + "_" + target);
            flow.setConditionExpression(condition); process.addFlowElement(flow);
        }
    }
}
