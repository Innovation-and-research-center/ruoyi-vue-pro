package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandSaveRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.service.land.LandServiceImpl;
import cn.iocoder.yudao.module.bpm.service.task.BpmRegisterTaskServiceImpl;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskReturnService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES;
import static cn.iocoder.yudao.module.bpm.service.land.LandServiceImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 真实引擎解析带 coll_userList 的登记 XML，不由调用方预置集合变量。 */
class BpmRegisterMultiInstanceTest {
    private ProcessEngine engine;
    private BpmTaskCandidateInvoker candidates;
    private BpmRegisterTaskServiceImpl registers;

    @BeforeEach
    void prepare() {
        candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUsersByTask(any())).thenReturn(new LinkedHashSet<>(Arrays.asList(7L, 8L)));
        BpmActivityBehaviorFactory factory = new BpmActivityBehaviorFactory();
        factory.setTaskCandidateInvoker(candidates);
        StandaloneInMemProcessEngineConfiguration config = new StandaloneInMemProcessEngineConfiguration();
        config.setJdbcUrl("jdbc:h2:mem:register-mi-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        config.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        config.setAsyncExecutorActivate(false);
        config.setActivityBehaviorFactory(factory);
        engine = config.buildProcessEngine();
        registers = new BpmRegisterTaskServiceImpl();
        ReflectionTestUtils.setField(registers, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(registers, "flowableTaskService", engine.getTaskService());
        ReflectionTestUtils.setField(registers, "repositoryService", engine.getRepositoryService());
        ReflectionTestUtils.setField(registers, "managementService", engine.getManagementService());
    }

    @AfterEach
    void close() {
        if (engine != null) engine.close();
    }

    @Test
    void sequentialRegistrationWithCardinalityStartsClaimsAndCompletesWithoutCollection() {
        verifyRegistration(true, true);
    }

    @Test
    void sequentialRegistrationWithoutCardinalityInitializesCollectionOnEveryInstance() {
        verifyRegistration(true, false);
    }

    @Test
    void parallelRegistrationStillCreatesOneSharedCandidateTask() {
        verifyRegistration(false, false);
    }

    @Test
    void landSaveReachesActualFlowableRegistrationInsteadOfMockingProcessCreation() {
        deploy(true, true);
        LandServiceImpl landService = new LandServiceImpl();
        LandMapper mapper = mock(LandMapper.class);
        doAnswer(call -> { ((LandDO) call.getArgument(0)).setId(10L); return 1; })
                .when(mapper).insert(any(LandDO.class));
        BpmProcessInstanceApi processes = mock(BpmProcessInstanceApi.class);
        when(processes.createProcessInstance(eq(7L), any())).thenAnswer(call -> {
            BpmProcessInstanceCreateReqDTO request = call.getArgument(1);
            return engine.getRuntimeService().startProcessInstanceByKey(request.getProcessDefinitionKey(),
                    request.getBusinessKey(), request.getVariables()).getId();
        });
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L).setNickname("测试登记员"));
        ReflectionTestUtils.setField(landService, "landMapper", mapper);
        ReflectionTestUtils.setField(landService, "landAttachMapper", mock(LandAttachMapper.class));
        ReflectionTestUtils.setField(landService, "userApi", users);
        ReflectionTestUtils.setField(landService, "processInstanceApi", processes);
        ReflectionTestUtils.setField(landService, "registerTaskService", registers);
        ReflectionTestUtils.setField(landService, "taskService", engine.getTaskService());
        ReflectionTestUtils.setField(landService, "runtimeService", engine.getRuntimeService());

        LandSaveRespVO result = landService.save(7L,
                new LandSaveReqVO().setProjectName("测试用途认定").setLocation("测试坐落"));
        Task task = engine.getTaskService().createTaskQuery().processInstanceId(result.getProcessInstanceId()).singleResult();
        assertEquals(result.getTaskId(), task.getId());
        assertEquals(REGISTER_NODE, task.getTaskDefinitionKey());
        assertEquals("7", task.getAssignee());
        assertFalse(engine.getRuntimeService().getVariables(result.getProcessInstanceId()).containsKey("coll_userList"));
        assertEquals("测试用途认定", engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(result.getProcessInstanceId()).singleResult().getName());

        when(mapper.selectForUpdate(10L)).thenReturn(new LandDO().setId(10L).setUserId(7L)
                .setProcessInstanceId(result.getProcessInstanceId()));
        landService.update(7L, new LandSaveReqVO().setId(10L).setProjectName("  修改后项目  ").setLocation("测试坐落"));
        assertEquals("修改后项目", engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(result.getProcessInstanceId()).singleResult().getName());
        assertEquals("修改后项目", engine.getHistoryService().createHistoricProcessInstanceQuery()
                .processInstanceId(result.getProcessInstanceId()).singleResult().getName());
        assertEquals("修改后项目", engine.getRuntimeService().getVariable(result.getProcessInstanceId(), "PROCESS_CUSTOM_NAME"));
    }

    @Test
    void registrationReturnFindsExactSourceAndSupportsRepeatedResubmission() {
        deploy(true, true);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            org.flowable.engine.delegate.DelegateExecution execution = call.getArgument(0);
            return new LinkedHashSet<>(Collections.singletonList(REGISTER_NODE.equals(execution.getCurrentActivityId()) ? 7L : 8L));
        });
        String instance = engine.getRuntimeService().startProcessInstanceByKey(PROCESS_KEY,
                Collections.singletonMap(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT)).getId();
        assertEquals(1, registers.claim(7L, instance));
        BpmTaskReturnService returns = returnService();
        for (int round = 0; round < 2; round++) {
            assertEquals(1, registers.completeOnSubmit(7L, instance,
                    Collections.singletonMap(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES,
                            Collections.singletonMap(MAIN_NODE, Collections.singletonList(8L))), false));
            Task main = engine.getTaskService().createTaskQuery().processInstanceId(instance)
                    .taskDefinitionKey(MAIN_NODE).singleResult();
            java.util.List<cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnTargetRespVO> targets = returns.targets(8L, main.getId());
            assertEquals(1, targets.size(), "主办应能找到本轮真实来源登记，而不是没有可退回节点");
            assertEquals(REGISTER_NODE, targets.get(0).getTaskDefinitionKey());
            assertEquals(7L, targets.get(0).getAssignees().get(0).getId());
            cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO request =
                    new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskReturnReqVO();
            request.setId(main.getId()); request.setTargetTaskDefinitionKey(REGISTER_NODE);
            request.setExpectedMode("SINGLE"); request.setReturnAssignees(Collections.singletonList(7L)); request.setReason("测试退回登记");
            returns.returnTask(8L, request);
            Task register = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
            assertEquals(REGISTER_NODE, register.getTaskDefinitionKey());
            assertEquals("7", register.getAssignee());
        }
        assertEquals(1, registers.completeOnSubmit(7L, instance,
                Collections.singletonMap(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES,
                        Collections.singletonMap(MAIN_NODE, Collections.singletonList(8L))), false));
        Task main = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getTaskService().complete(main.getId());
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).count());
    }

    @Test
    void registrationReturnDoesNotGuessMissingForeignOrUnrelatedDispatchSources() {
        deploy(true, true);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            org.flowable.engine.delegate.DelegateExecution execution = call.getArgument(0);
            return new LinkedHashSet<>(Collections.singletonList(REGISTER_NODE.equals(execution.getCurrentActivityId()) ? 7L : 8L));
        });
        List<String> instances = new ArrayList<>();
        List<String> registerIds = new ArrayList<>();
        for (int round = 0; round < 2; round++) {
            String instance = engine.getRuntimeService().startProcessInstanceByKey(PROCESS_KEY,
                    Collections.singletonMap(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT)).getId();
            instances.add(instance);
            assertEquals(1, registers.claim(7L, instance));
            registerIds.add(engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult().getId());
            registers.completeOnSubmit(7L, instance,
                    Collections.singletonMap(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES,
                            Collections.singletonMap(MAIN_NODE, Collections.singletonList(8L))), false);
        }
        Task main = engine.getTaskService().createTaskQuery().processInstanceId(instances.get(0)).singleResult();
        BpmTaskReturnService returns = returnService();
        assertEquals(1, returns.targets(8L, main.getId()).size());
        // 最近来源无效时，不能越过它借用父执行流里另一个来源。
        engine.getRuntimeService().setVariableLocal(main.getExecutionId(), BpmDispatchSources.SOURCES,
                Collections.singletonMap("8", Collections.singletonList("missing-source")));
        assertTrue(returns.targets(8L, main.getId()).isEmpty());
        // 其他流程中同名登记、同一个办理人，也不能作为当前流程的退回目标。
        engine.getRuntimeService().setVariableLocal(main.getExecutionId(), BpmDispatchSources.SOURCES,
                Collections.singletonMap("8", Collections.singletonList(registerIds.get(1))));
        assertTrue(returns.targets(8L, main.getId()).isEmpty());
        engine.getRuntimeService().removeVariableLocal(main.getExecutionId(), BpmDispatchSources.SOURCES);
        String root = engine.getRuntimeService().createExecutionQuery().executionId(main.getExecutionId()).singleResult().getParentId();
        engine.getRuntimeService().setVariableLocal(root, BpmDispatchSources.SOURCES, Collections.emptyMap());
        assertTrue(returns.targets(8L, main.getId()).isEmpty(), "有登记历史但无派发来源时，不能按时间或人员猜测");
        assertEquals(main.getId(), engine.getTaskService().createTaskQuery().processInstanceId(instances.get(0)).singleResult().getId());
    }

    private BpmTaskReturnService returnService() {
        BpmTaskReturnService returns = new BpmTaskReturnService();
        ReflectionTestUtils.setField(returns, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(returns, "historyService", engine.getHistoryService());
        ReflectionTestUtils.setField(returns, "repositoryService", engine.getRepositoryService());
        ReflectionTestUtils.setField(returns, "managementService", engine.getManagementService());
        ReflectionTestUtils.setField(returns, "flowableTaskService", engine.getTaskService());
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUser(anyLong())).thenAnswer(call -> new AdminUserRespDTO().setId(call.getArgument(0))
                .setStatus(0).setNickname("测试登记人"));
        when(users.getUserList(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0)).stream()
                .map(id -> new AdminUserRespDTO().setId(id).setStatus(0).setNickname("测试登记人"))
                .collect(java.util.stream.Collectors.toList()));
        ReflectionTestUtils.setField(returns, "adminUserApi", users);
        return returns;
    }

    @Test
    void withdrawCompletedMainBeforePeerFinishesRestoresOriginalMeeting() {
        verifyWaitingWithdrawal(false);
    }

    @Test
    void withdrawWaitingMainPreservesOtherCompletedMembersSelections() {
        verifyWaitingWithdrawal(true);
    }

    private void verifyWaitingWithdrawal(boolean anotherCompleted) {
        String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"test\">"
                + "<process id=\"waiting_withdraw\" isExecutable=\"true\"><startEvent id=\"start\"/>"
                + "<sequenceFlow id=\"s\" sourceRef=\"start\" targetRef=\"" + MAIN_NODE + "\"/>"
                + "<userTask id=\"" + MAIN_NODE + "\" name=\"主办科室\"><multiInstanceLoopCharacteristics flowable:collection=\"${users}\">"
                + "<completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition>"
                + "</multiInstanceLoopCharacteristics></userTask>"
                + "<sequenceFlow id=\"next\" sourceRef=\"" + MAIN_NODE + "\" targetRef=\"staff\">"
                + "<conditionExpression>${variables:get('next_node1') == 'staff'}</conditionExpression></sequenceFlow>"
                + "<userTask id=\"staff\"><multiInstanceLoopCharacteristics flowable:collection=\"${users}\"/></userTask>"
                + "<sequenceFlow id=\"e\" sourceRef=\"staff\" targetRef=\"end\"/><endEvent id=\"end\"/></process></definitions>";
        engine.getRepositoryService().createDeployment().addString("waiting.bpmn20.xml", xml).deploy();
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            org.flowable.engine.delegate.DelegateExecution execution = call.getArgument(0);
            if (MAIN_NODE.equals(execution.getCurrentActivityId()))
                return new LinkedHashSet<>(anotherCompleted ? Arrays.asList(7L, 8L, 9L) : Arrays.asList(7L, 8L));
            Map<String, Collection<Long>> selections = (Map<String, Collection<Long>>) execution.getVariable(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
            return new LinkedHashSet<>(selections.get("staff"));
        });
        String instance = engine.getRuntimeService().startProcessInstanceByKey("waiting_withdraw",
                Collections.singletonMap(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT)).getId();
        Task first = waitingTask(instance, "7");
        Task peer = waitingTask(instance, anotherCompleted ? "9" : "8");
        String root = engine.getRuntimeService().createExecutionQuery().executionId(first.getExecutionId()).singleResult().getParentId();
        completeWaitingMain(first, Arrays.asList(20L, 21L));
        if (anotherCompleted) completeWaitingMain(waitingTask(instance, "8"), Arrays.asList(21L, 22L));
        cn.iocoder.yudao.module.bpm.service.task.BpmTaskWithdrawService withdrawals = new cn.iocoder.yudao.module.bpm.service.task.BpmTaskWithdrawService();
        ReflectionTestUtils.setField(withdrawals, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(withdrawals, "historyService", engine.getHistoryService());
        ReflectionTestUtils.setField(withdrawals, "repositoryService", engine.getRepositoryService());
        ReflectionTestUtils.setField(withdrawals, "managementService", engine.getManagementService());
        ReflectionTestUtils.setField(withdrawals, "flowableTaskService", engine.getTaskService());
        cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions = mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);
        cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO info = new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO();
        info.setAllowWithdrawTask(true);
        when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);
        ReflectionTestUtils.setField(withdrawals, "processDefinitionService", definitions);
        AdminUserApi withdrawUsers = mock(AdminUserApi.class);
        when(withdrawUsers.getUserList(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0))
                .stream().map(id -> new AdminUserRespDTO().setId(id).setNickname("测试人员"))
                .collect(java.util.stream.Collectors.toList()));
        ReflectionTestUtils.setField(withdrawals, "adminUserApi", withdrawUsers);

        assertThrows(RuntimeException.class, () -> withdrawals.withdrawAll(99L, first.getId()));
        withdrawals.withdrawAll(7L, first.getId());
        Task restored = waitingTask(instance, "7");
        assertEquals(first.getExecutionId(), restored.getExecutionId());
        assertEquals(peer.getId(), waitingTask(instance, peer.getAssignee()).getId());
        assertEquals(anotherCompleted ? 3 : 2, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertEquals(anotherCompleted ? 1 : 0, engine.getRuntimeService().getVariableLocal(root, "nrOfCompletedInstances"));
        assertEquals(2, engine.getRuntimeService().getVariableLocal(root, "nrOfActiveInstances"));
        assertEquals("原意见", engine.getTaskService().getVariableLocal(restored.getId(), cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_REASON));
        assertEquals(Boolean.FALSE, engine.getTaskService().getVariableLocal(restored.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertThrows(RuntimeException.class, () -> withdrawals.withdrawAll(7L, first.getId()));
        Map<?, ?> selected = (Map<?, ?>) engine.getRuntimeService().getVariable(instance, PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
        assertEquals(anotherCompleted ? Arrays.asList(21L, 22L) : null, selected.get("staff"));
        if (!anotherCompleted) assertNull(engine.getRuntimeService().getVariable(instance, "next_node1"));
        // 发送窗口读取历史流程变量；显式 null 或旧选择都不能把人工流向隐藏。
        for (Task current : Arrays.asList(restored, peer)) {
            Map<String, Object> variables = engine.getHistoryService().createHistoricProcessInstanceQuery()
                    .processInstanceId(instance).includeProcessVariables().singleResult().getProcessVariables();
            assertTrue(sendOptions((org.flowable.bpmn.model.FlowNode) engine.getRepositoryService()
                    .getBpmnModel(current.getProcessDefinitionId()).getFlowElement(MAIN_NODE), variables)
                    .stream().anyMatch(option -> "staff".equals(option.getTaskDefKey())),
                    "撤回后本人和其他主办都应仍能选择转出流向");
        }
        completeWaitingMain(restored, Collections.singletonList(23L));
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("staff").count());
        withdrawals.withdrawAll(7L, restored.getId());
        assertEquals(peer.getId(), waitingTask(instance, peer.getAssignee()).getId());
        Task finalMain = waitingTask(instance, "7");
        completeWaitingMain(finalMain, Collections.singletonList(25L));
        completeWaitingMain(peer, Collections.singletonList(24L));
        Set<String> staff = engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("staff").list()
                .stream().map(Task::getAssignee).collect(java.util.stream.Collectors.toSet());
        assertEquals(new HashSet<>(anotherCompleted ? Arrays.asList("21", "22", "25", "24") : Arrays.asList("25", "24")), staff);
        // 全员办完后撤回，只拉回本人派发，其他主办的接收人员继续办理。
        withdrawals.withdrawAll(7L, finalMain.getId());
        assertNotNull(waitingTask(instance, "7"));
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee("25").count());
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee("24").count());
        completeWaitingMain(waitingTask(instance, "7"), Collections.singletonList(26L));
        for (Task task : engine.getTaskService().createTaskQuery().processInstanceId(instance).list()) engine.getTaskService().complete(task.getId());
        assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).count());
    }

    private Task waitingTask(String instance, String user) {
        return engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(MAIN_NODE).taskAssignee(user).singleResult();
    }

    @Test
    void manualSendOptionsIgnoreOldChoiceButKeepBusinessConditions() {
        org.flowable.bpmn.model.UserTask source = new org.flowable.bpmn.model.UserTask();
        source.setId("main");
        String[] conditions = {"${variables:get('next_node1') == 'center'}",
                "${select_node_2 == 'assist'}", "${amount > 100}", "${select_node_3 == 'appeal'}"};
        String[] targets = {"center", "assist", "business", "appeal"};
        for (int i = 0; i < conditions.length; i++) {
            org.flowable.bpmn.model.UserTask target = new org.flowable.bpmn.model.UserTask();
            target.setId(targets[i]); target.setName(targets[i]);
            org.flowable.bpmn.model.SequenceFlow flow = new org.flowable.bpmn.model.SequenceFlow();
            flow.setConditionExpression(conditions[i]); flow.setTargetFlowElement(target);
            source.getOutgoingFlows().add(flow);
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("next_node1", null); variables.put("select_node_2", "old");
        variables.put("amount", 10); variables.put("select_node_3", null);
        assertEquals(new HashSet<>(Arrays.asList("center", "assist")), sendOptions(source, variables).stream()
                .map(cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO::getTaskDefKey)
                .collect(java.util.stream.Collectors.toSet()));
    }

    private List<cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO> sendOptions(
            org.flowable.bpmn.model.FlowNode source, Map<String, Object> variables) {
        List<cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO> options = new ArrayList<>();
        ReflectionTestUtils.invokeMethod(new cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceServiceImpl(),
                "analyzeOutgoingFlows", source, options, null, variables);
        return options;
    }

    @Test
    void completedMainCanWithdrawWhileDirectCenterDispatchWaitsAtInclusiveJoin() {
        verifyJoinWithdrawal(false, false);
    }

    @Test
    void waitingJoinDoesNotAllowWithdrawalOfActualAsyncDispatch() {
        verifyJoinWithdrawal(true, false);
    }

    @Test
    void waitingJoinDoesNotAllowWithdrawalAfterRecipientCompletes() {
        verifyJoinWithdrawal(false, true);
    }

    private void verifyJoinWithdrawal(boolean asyncStaff, boolean recipientCompleted) {
        String xml = "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"test\">"
                + "<process id=\"join_withdraw\" isExecutable=\"true\"><startEvent id=\"start\"/>"
                + "<sequenceFlow id=\"s\" sourceRef=\"start\" targetRef=\"" + MAIN_NODE + "\"/>";
        for (String node : Arrays.asList(MAIN_NODE, "assist", "staff", "center")) {
            xml += "<userTask id=\"" + node + "\" name=\"" + node + "\""
                    + (asyncStaff && "staff".equals(node) ? " flowable:async=\"true\"" : "")
                    + "><multiInstanceLoopCharacteristics flowable:collection=\"${users}\">"
                    + "<completionCondition>${nrOfCompletedInstances == nrOfInstances}</completionCondition></multiInstanceLoopCharacteristics></userTask>";
        }
        for (String node : Arrays.asList("assist", "staff", "center")) {
            int key = Arrays.asList("center", "assist", "staff").indexOf(node) + 1;
            xml += "<sequenceFlow id=\"to_" + node + "\" sourceRef=\"" + MAIN_NODE + "\" targetRef=\"" + ("center".equals(node) ? "join" : node) + "\">"
                    + "<conditionExpression>${variables:get('next_node" + key + "') == '" + node + "'}</conditionExpression></sequenceFlow>";
        }
        xml += "<inclusiveGateway id=\"join\"/><sequenceFlow id=\"a\" sourceRef=\"assist\" targetRef=\"join\"/>"
                + "<sequenceFlow id=\"p\" sourceRef=\"staff\" targetRef=\"join\"/><sequenceFlow id=\"c\" sourceRef=\"join\" targetRef=\"center\"/>"
                + "<endEvent id=\"end\"/><sequenceFlow id=\"e\" sourceRef=\"center\" targetRef=\"end\"/></process></definitions>";
        engine.getRepositoryService().createDeployment().addString("join-withdraw.bpmn20.xml", xml).deploy();
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            org.flowable.engine.delegate.DelegateExecution execution = call.getArgument(0);
            if (MAIN_NODE.equals(execution.getCurrentActivityId())) return new LinkedHashSet<>(Arrays.asList(7L, 8L));
            Map<String, Collection<Long>> selected = (Map<String, Collection<Long>>) execution.getVariable(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
            return new LinkedHashSet<>(selected.get(execution.getCurrentActivityId()));
        });
        String instance = engine.getRuntimeService().startProcessInstanceByKey("join_withdraw",
                Collections.singletonMap(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT)).getId();
        Task a = waitingTask(instance, "7"), b = waitingTask(instance, "8");
        Map<String, List<Long>> selected = new LinkedHashMap<>();
        selected.put("assist", Arrays.asList(20L, 21L)); selected.put("staff", Collections.singletonList(30L));
        selected.put("center", Collections.singletonList(40L));
        engine.getRuntimeService().setVariableLocal(instance, PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, selected);
        engine.getRuntimeService().setVariableLocal(instance, "next_node1", "center");
        engine.getRuntimeService().setVariableLocal(instance, "next_node2", "assist");
        engine.getRuntimeService().setVariableLocal(instance, "next_node3", "staff");
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, a.getId(), selected);
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, b.getId(), Collections.singletonMap("assist", Arrays.asList(20L, 21L)));
        for (Task main : Arrays.asList(a, b)) {
            engine.getTaskService().setVariableLocal(main.getId(), cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
            engine.getTaskService().complete(main.getId());
        }
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("center").count());
        cn.iocoder.yudao.module.bpm.service.task.BpmTaskWithdrawService withdrawals = new cn.iocoder.yudao.module.bpm.service.task.BpmTaskWithdrawService();
        ReflectionTestUtils.setField(withdrawals, "runtimeService", engine.getRuntimeService());
        ReflectionTestUtils.setField(withdrawals, "historyService", engine.getHistoryService());
        ReflectionTestUtils.setField(withdrawals, "repositoryService", engine.getRepositoryService());
        ReflectionTestUtils.setField(withdrawals, "managementService", engine.getManagementService());
        ReflectionTestUtils.setField(withdrawals, "flowableTaskService", engine.getTaskService());
        cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions = mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);
        cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO info = new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO();
        info.setAllowWithdrawTask(true); when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);
        ReflectionTestUtils.setField(withdrawals, "processDefinitionService", definitions);
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUserList(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0)).stream()
                .map(id -> new AdminUserRespDTO().setId(id).setNickname("测试人员")).collect(java.util.stream.Collectors.toList()));
        ReflectionTestUtils.setField(withdrawals, "adminUserApi", users);
        Set<String> assistTaskIds = engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("assist")
                .list().stream().map(Task::getId).collect(java.util.stream.Collectors.toSet());
        if (asyncStaff || recipientCompleted) {
            if (recipientCompleted) engine.getTaskService().complete(engine.getTaskService().createTaskQuery()
                    .processInstanceId(instance).taskAssignee("20").singleResult().getId());
            RuntimeException failure = assertThrows(RuntimeException.class, () -> withdrawals.withdrawAll(7L, a.getId()));
            assertTrue(failure.getMessage().contains(asyncStaff ? "状态不允许" : "下游已有人员办理"), failure.getMessage());
            assertNull(waitingTask(instance, "7"));
            assertTrue(engine.getRuntimeService().createVariableInstanceQuery().processInstanceId(instance)
                    .variableNameLike("bpm_dispatch_pending_%").list().stream()
                    .anyMatch(variable -> variable.getValue() instanceof Collection && ((Collection<?>) variable.getValue()).contains(a.getId())));
            return;
        }
        withdrawals.withdrawAll(7L, a.getId());
        assertNotNull(waitingTask(instance, "7"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(MAIN_NODE).count(), "只恢复 A，不能再生成 B 的主办待办");
        assertEquals(2, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("assist").count());
        assertEquals(assistTaskIds, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("assist")
                .list().stream().map(Task::getId).collect(java.util.stream.Collectors.toSet()), "B 的协办原任务不能被重建");
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("staff").count());
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("center").count());
        assertTrue(engine.getRuntimeService().createVariableInstanceQuery().processInstanceId(instance)
                .variableNameLike("bpm_dispatch_pending_%").list().stream()
                .noneMatch(variable -> variable.getValue() instanceof Collection && ((Collection<?>) variable.getValue()).contains(a.getId())));
        Task restored = waitingTask(instance, "7");
        String scope = engine.getRuntimeService().createExecutionQuery().executionId(restored.getExecutionId()).singleResult().getParentId();
        scope = engine.getRuntimeService().createExecutionQuery().executionId(scope).singleResult().getParentId();
        engine.getRuntimeService().setVariableLocal(scope, PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES,
                Collections.singletonMap("center", Collections.singletonList(41L)));
        engine.getRuntimeService().setVariableLocal(scope, "next_node1", "center");
        BpmDispatchSources.prepare(engine.getRuntimeService(), scope, restored.getId(), Collections.singletonMap("center", Collections.singletonList(41L)));
        engine.getTaskService().complete(restored.getId());
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("center").count(), "仍须等待 B 的协办完成");
        engine.getRuntimeService().setVariableLocal(instance, PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES,
                Collections.singletonMap("center", Collections.singletonList(42L)));
        for (Task assist : engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("assist").list()) {
            BpmDispatchSources.prepare(engine.getRuntimeService(), instance, assist.getId(), Collections.singletonMap("center", Collections.singletonList(42L)));
            engine.getTaskService().complete(assist.getId());
        }
        List<Task> centers = engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("center").list();
        assertFalse(centers.isEmpty(), engine.getRuntimeService().createExecutionQuery().processInstanceId(instance).list()
                .stream().map(execution -> execution.getId() + ":" + execution.getParentId() + ":" + execution.getActivityId())
                .collect(java.util.stream.Collectors.joining(",")));
        assertTrue(centers.stream().noneMatch(task -> "40".equals(task.getAssignee())), "撤回前的登记中心旧选人不能再次发送");
        for (Task center : centers) engine.getTaskService().complete(center.getId());
        assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).count(), "不能留下卡住的汇总执行流");
    }

    private void completeWaitingMain(Task task, List<Long> recipients) {
        RuntimeService runtime = engine.getRuntimeService();
        String scope = runtime.createExecutionQuery().executionId(task.getExecutionId()).singleResult().getParentId();
        scope = runtime.createExecutionQuery().executionId(scope).singleResult().getParentId();
        Map<String, List<Long>> selections = new LinkedHashMap<>();
        Object previous = runtime.getVariableLocal(scope, PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
        if (previous instanceof Map) selections.putAll((Map<String, List<Long>>) previous);
        Set<Long> union = new LinkedHashSet<>(selections.getOrDefault("staff", Collections.emptyList()));
        union.addAll(recipients); selections.put("staff", new ArrayList<>(union));
        runtime.setVariableLocal(scope, PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, selections);
        runtime.setVariableLocal(scope, "next_node1", "staff");
        BpmDispatchSources.prepare(runtime, scope, task.getId(), Collections.singletonMap("staff", recipients));
        BpmTaskSmsPolicy.prepare(runtime, scope, Collections.singletonMap("staff", recipients), true);
        engine.getTaskService().setVariableLocal(task.getId(), cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        engine.getTaskService().setVariableLocal(task.getId(), cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_REASON, "原意见");
        engine.getTaskService().complete(task.getId());
    }

    private void verifyRegistration(boolean sequential, boolean cardinality) {
        deploy(sequential, cardinality);
        // 同一已缓存行为重复进入，不能依赖上一次调用留下的 collectionVariable。
        for (int round = 0; round < 2; round++) {
            String process = engine.getRuntimeService().startProcessInstanceByKey(PROCESS_KEY).getId();
            List<Task> tasks = engine.getTaskService().createTaskQuery().processInstanceId(process).list();
            assertEquals(1, tasks.size(), "登记任务不能按候选人数生成多份");
            Task task = tasks.get(0);
            assertNull(task.getAssignee());
            assertEquals(2, engine.getTaskService().getIdentityLinksForTask(task.getId()).stream()
                    .filter(link -> "candidate".equals(link.getType())).count());
            assertEquals(1, registers.claim(7L, process));
            assertEquals("7", engine.getTaskService().createTaskQuery().taskId(task.getId()).singleResult().getAssignee());
            assertEquals(1, registers.completeOnSubmit(7L, process,
                    Collections.singletonMap(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES,
                            Collections.singletonMap(MAIN_NODE, Arrays.asList(7L, 8L))), false));
            List<Task> mainTasks = engine.getTaskService().createTaskQuery().processInstanceId(process).list();
            assertEquals(2, mainTasks.size());
            assertTrue(mainTasks.stream().allMatch(next -> MAIN_NODE.equals(next.getTaskDefinitionKey())));
            for (Task next : mainTasks) engine.getTaskService().complete(next.getId());
            assertEquals(0, engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(process).count());
        }
    }

    private void deploy(boolean sequential, boolean cardinality) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:flowable=\"http://flowable.org/bpmn\" targetNamespace=\"test\">"
                + "<process id=\"" + PROCESS_KEY + "\" isExecutable=\"true\"><startEvent id=\"start\"/>"
                + "<sequenceFlow id=\"f1\" sourceRef=\"start\" targetRef=\"" + REGISTER_NODE + "\"/>"
                + "<userTask id=\"" + REGISTER_NODE + "\" name=\"科室登记\"><extensionElements>"
                + "<flowable:candidateStrategy>36</flowable:candidateStrategy><flowable:properties>"
                + "<flowable:property name=\"business_node_type\" value=\"register\"/>"
                + "<flowable:property name=\"register_submit_mode\" value=\"complete_on_submit\"/>"
                + "</flowable:properties></extensionElements>"
                + "<multiInstanceLoopCharacteristics isSequential=\"" + sequential + "\" flowable:collection=\"${coll_userList}\">"
                + (cardinality ? "<loopCardinality>1</loopCardinality>" : "")
                + "<completionCondition>${nrOfCompletedInstances &gt;= nrOfInstances}</completionCondition>"
                + "</multiInstanceLoopCharacteristics></userTask>"
                + "<sequenceFlow id=\"f2\" sourceRef=\"" + REGISTER_NODE + "\" targetRef=\"" + MAIN_NODE + "\"/>"
                + "<userTask id=\"" + MAIN_NODE + "\" name=\"主办科室\">"
                + "<multiInstanceLoopCharacteristics flowable:collection=\"${coll_userList}\">"
                + "<completionCondition>${nrOfCompletedInstances &gt;= nrOfInstances}</completionCondition>"
                + "</multiInstanceLoopCharacteristics></userTask>"
                + "<sequenceFlow id=\"f3\" sourceRef=\"" + MAIN_NODE + "\" targetRef=\"end\"/><endEvent id=\"end\"/>"
                + "</process></definitions>";
        engine.getRepositoryService().createDeployment().addString("land-register.bpmn20.xml", xml).deploy();
    }
}
