package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior.BpmActivityBehaviorFactory;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.service.task.*;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BpmTaskReturnTest {
    private static final String SOURCE = cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_DIRECTOR_TASK;
    private static final String RECEIVE = cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE;
    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        BpmTaskCandidateInvoker candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            DelegateExecution execution = call.getArgument(0);
            switch (execution.getCurrentActivityId()) {
                case SOURCE:
                case "otherSource": return new LinkedHashSet<>(Collections.singletonList(1L));
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



    private BpmTaskWithdrawService withdrawService() throws Exception {
        BpmTaskWithdrawService service = new BpmTaskWithdrawService();
        cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions =
                mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);
        cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO info =
                new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO();
        info.setAllowWithdrawTask(true);
        when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUserList(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0))
                .stream().map(this::user).collect(Collectors.toList()));
        Map<String, Object> fields = new HashMap<>();
        fields.put("runtimeService", engine.getRuntimeService()); fields.put("historyService", engine.getHistoryService());
        fields.put("repositoryService", engine.getRepositoryService()); fields.put("managementService", engine.getManagementService());
        fields.put("flowableTaskService", engine.getTaskService()); fields.put("processDefinitionService", definitions); fields.put("adminUserApi", users);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            java.lang.reflect.Field field = service.getClass().getDeclaredField(entry.getKey()); field.setAccessible(true); field.set(service, entry.getValue());
        }
        return service;
    }
    private String internal(String instance, long sender, Long... recipients) {
        Task source = task(instance, "" + sender);
        String root = engine.getRuntimeService().createExecutionQuery().executionId(source.getExecutionId()).singleResult().getParentId();
        engine.getTaskService().setVariableLocal(source.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        BpmInternalDispatch.add(engine.getManagementService(), engine.getRuntimeService(), source, root, Arrays.asList(recipients), false, Collections.emptyMap());
        engine.getTaskService().complete(source.getId()); return source.getId();
    }
    private void internalBack(String instance, long sender, long recipient, String historic) throws Exception {
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(task(instance, "" + sender).getId());
        req.setTargetTaskDefinitionKey("target"); req.setTargetTaskId(historic);
        req.setReturnAssignees(Collections.singletonList(recipient)); req.setReason("内循环补充材料");
        service().returnTask(sender, req);
    }
    @Test void internalChainReturnsToOriginalWithoutTouchingPeers() throws Exception {
        String instance = start(false, false); dispatch(instance); String peer = task(instance, "5").getId();
        String d = internal(instance, 4, 8L); internal(instance, 8, 9L);
        BpmTaskReturnTargetRespVO target = service().targets(9L, task(instance, "9").getId()).stream().filter(t -> "target".equals(t.getTaskDefinitionKey())).findFirst().get();
        assertEquals(Arrays.asList(8L, 4L), target.getAssignees().stream().map(BpmTaskReturnTargetRespVO.Candidate::getId).collect(Collectors.toList()));
        internalBack(instance, 9, 4, d); assertNotNull(task(instance, "4")); assertNull(task(instance, "9")); assertEquals(peer, task(instance, "5").getId());
    }
    @Test void internalChainReturnsToPreviousAndKeepsEarlierSource() throws Exception {
        String instance = start(false, false); dispatch(instance); String d = internal(instance, 4, 8L); String e = internal(instance, 8, 9L);
        internalBack(instance, 9, 8, e);
        assertEquals(d, engine.getTaskService().getVariableLocal(task(instance, "8").getId(), BpmInternalDispatch.SOURCE));
        internalBack(instance, 8, 4, d); assertNotNull(task(instance, "4"));
    }
    @Test void internalWithdrawOnlyBeforeDirectRecipientCompletes() throws Exception {
        String instance = start(false, false); dispatch(instance); String d = internal(instance, 4, 8L); String e = internal(instance, 8, 9L);
        assertThrows(RuntimeException.class, () -> withdrawService().withdrawAll(4L, d)); assertNotNull(task(instance, "9"));
        withdrawService().withdrawAll(8L, e); assertNotNull(task(instance, "8")); assertNull(task(instance, "9")); assertNotNull(task(instance, "5"));
    }
    @Test void internalOrSignHandoffDoesNotCompleteNode() throws Exception {
        String instance = start(false, false, false, true); dispatch(instance); internal(instance, 4, 8L);
        assertNotNull(task(instance, "8")); assertNotNull(task(instance, "5"));
        engine.getTaskService().complete(task(instance, "8").getId()); assertNull(task(instance, "5")); assertNotNull(task(instance, "7"));
    }
    @Test void internalSequentialQueuedMemberKeepsItsSource() throws Exception {
        String instance = start(true, false); dispatch(instance); String d = internal(instance, 4, 8L); assertNull(task(instance, "8"));
        engine.getTaskService().complete(task(instance, "5").getId()); engine.getTaskService().complete(task(instance, "6").getId());
        assertEquals(d, engine.getTaskService().getVariableLocal(task(instance, "8").getId(), BpmInternalDispatch.SOURCE));
        internalBack(instance, 8, 4, d); assertNotNull(task(instance, "4"));
    }
    @Test void internalWithdrawProtectsPeers() throws Exception {
        String instance = start(false, false); dispatch(instance); String peer = task(instance, "5").getId();
        String d = internal(instance, 4, 8L, 9L); withdrawService().withdrawAll(4L, d);
        Task restored = task(instance, "4"); assertNotNull(restored); assertNull(task(instance, "8")); assertNull(task(instance, "9"));
        assertEquals(peer, task(instance, "5").getId());
        assertEquals(new HashSet<>(Arrays.asList(5L, 6L)), BpmTaskReturnService.retainedUsers(engine.getRuntimeService(), instance, restored.getExecutionId(), "target"));
    }
    @Test void internalLastWithdrawalDoesNotAdvance() throws Exception {
        String instance = start(false, false);
        engine.getRuntimeService().setVariableLocal(instance, BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap("target", Collections.singleton(4L)));
        dispatch(instance); String d = internal(instance, 4, 8L); withdrawService().withdrawAll(4L, d);
        assertNotNull(task(instance, "4")); assertNull(task(instance, "7"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void internalLastReturnDoesNotAdvance() throws Exception {
        String instance = start(false, false);
        engine.getRuntimeService().setVariableLocal(instance, BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap("target", Collections.singleton(4L)));
        dispatch(instance); String d = internal(instance, 4, 8L); internalBack(instance, 8, 4, d);
        assertNotNull(task(instance, "4")); assertNull(task(instance, "7"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void internalForgedTargetRejectsAtomically() throws Exception {
        String instance = start(false, false); dispatch(instance); String unrelated = task(instance, "5").getId();
        engine.getTaskService().setVariableLocal(unrelated, BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        engine.getTaskService().complete(unrelated); internal(instance, 4, 8L); String pending = task(instance, "8").getId();
        assertThrows(RuntimeException.class, () -> internalBack(instance, 8, 5, unrelated));
        assertEquals(pending, task(instance, "8").getId()); assertNull(task(instance, "5"));
    }
    @Test void internalQueuedWithdrawalClearsDeferredSourceContext() throws Exception {
        String instance = start(true, false); dispatch(instance); String d = internal(instance, 4, 8L);
        Task peer = task(instance, "5");
        String root = engine.getRuntimeService().createExecutionQuery().executionId(peer.getExecutionId()).singleResult().getParentId();
        withdrawService().withdrawAll(4L, d);
        assertNull(engine.getRuntimeService().getVariableLocal(root, BpmInternalDispatch.META));
        assertEquals(peer.getId(), task(instance, "5").getId()); assertNotNull(task(instance, "4"));
        String nextSource = internal(instance, 5, 8L);
        engine.getTaskService().complete(task(instance, "6").getId());
        assertEquals(nextSource, engine.getTaskService().getVariableLocal(task(instance, "8").getId(), BpmInternalDispatch.SOURCE));
    }
    @Test void internalTaskCreationDoesNotInheritUnrelatedFinisherContext() throws Exception {
        BpmTaskServiceImpl approvals = new BpmTaskServiceImpl();
        TaskService tasks = mock(TaskService.class); RuntimeService runtime = mock(RuntimeService.class);
        for (String name : Arrays.asList("taskService", "runtimeService")) {
            java.lang.reflect.Field f = BpmTaskServiceImpl.class.getDeclaredField(name); f.setAccessible(true);
            f.set(approvals, name.equals("taskService") ? tasks : runtime);
        }
        Task task = mock(Task.class); when(task.getId()).thenReturn("new-internal");
        when(task.getProcessInstanceId()).thenReturn("process"); when(task.getExecutionId()).thenReturn("child");
        when(task.getTaskLocalVariables()).thenReturn(Collections.singletonMap(BpmnVariableConstants.TASK_VARIABLE_STATUS, 1));
        when(tasks.getVariableLocal("new-internal", BpmInternalDispatch.SOURCE)).thenReturn("original-dispatcher");
        List<ThreadLocal<Object>> locals = new ArrayList<>();
        try {
            String[] fields = {"RETURN_BRANCH_CONTEXT_CARRIER", "RETURNABLE_TASK_PATH_CARRIER"};
            String[] classes = {"ReturnBranchContextCarrier", "ReturnableTaskPathCarrier"};
            for (int i = 0; i < fields.length; i++) {
                java.lang.reflect.Field f = BpmTaskServiceImpl.class.getDeclaredField(fields[i]); f.setAccessible(true);
                ThreadLocal<Object> local = (ThreadLocal<Object>) f.get(null); locals.add(local);
                java.lang.reflect.Constructor<?> ctor = Class.forName(BpmTaskServiceImpl.class.getName() + "$" + classes[i]).getDeclaredConstructors()[0]; ctor.setAccessible(true);
                local.set(ctor.newInstance("process", i == 0 ? Collections.singletonMap("wrong", "batch") : Collections.singletonList("wrong")));
            }
            approvals.processTaskCreated(task);
            verifyNoInteractions(runtime);
            verify(tasks, never()).setVariableLocal(anyString(), anyString(), any());
        } finally { locals.forEach(ThreadLocal::remove); }
    }
    private BpmTaskReturnService service() throws Exception {
        BpmTaskReturnService service = new BpmTaskReturnService();
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUser(anyLong())).thenAnswer(call -> user(call.getArgument(0)));
        when(users.getUserList(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0)).stream().map(this::user).collect(Collectors.toList()));
        Map<String, Object> fields = new HashMap<>();
        fields.put("runtimeService", engine.getRuntimeService()); fields.put("historyService", engine.getHistoryService());
        fields.put("repositoryService", engine.getRepositoryService()); fields.put("managementService", engine.getManagementService());
        fields.put("flowableTaskService", engine.getTaskService()); fields.put("adminUserApi", users);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            java.lang.reflect.Field field = service.getClass().getDeclaredField(entry.getKey()); field.setAccessible(true); field.set(service, entry.getValue());
        }
        return service;
    }
    private AdminUserRespDTO user(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO(); user.setId(id); user.setNickname("人员" + id); user.setStatus(0); return user;
    }
    private String dispatch(String instance) {
        Task source = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        if (source.getAssignee() == null) engine.getTaskService().setAssignee(source.getId(), "1");
        Map<String, Object> context = new HashMap<>();
        context.put("RETURNABLE_TASK_PATH", Collections.singletonList(SOURCE));
        context.put("RETURN_BRANCH_CONTEXT", Collections.singletonMap(SOURCE, source.getId()));
        engine.getTaskService().setVariablesLocal(source.getId(), context);
        engine.getTaskService().setVariableLocal(source.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, source.getId(), Collections.singletonMap("target", Arrays.asList(4L, 5L, 6L)));
        engine.getTaskService().complete(source.getId());
        return source.getId();
    }
    private Task task(String instance, String user) {
        return engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee(user).singleResult();
    }
    private void back(String instance, long user) throws Exception {
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(task(instance, "" + user).getId());
        req.setTargetTaskDefinitionKey(SOURCE); req.setReturnAssignees(Collections.singletonList(1L)); req.setReason("补充材料");
        service().returnTask(user, req);
    }
    @Test void parallelReturnKeepsOtherTasksAndVariablesAndAudit() throws Exception {
        String instance = start(false, false); dispatch(instance);
        Task d = task(instance, "4"), e = task(instance, "5");
        engine.getRuntimeService().setVariableLocal(e.getExecutionId(), "private_value", "unchanged");
        back(instance, 4);
        assertEquals(e.getId(), task(instance, "5").getId());
        assertEquals("unchanged", engine.getRuntimeService().getVariableLocal(e.getExecutionId(), "private_value"));
        assertNotNull(task(instance, "1"));
        assertNull(task(instance, "4"));
        String root = engine.getRuntimeService().createExecutionQuery().executionId(e.getExecutionId()).singleResult().getParentId();
        assertEquals(2, ((Number) engine.getRuntimeService().getVariableLocal(root, "nrOfActiveInstances")).intValue());
        assertEquals(cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.RETURN.getStatus(), engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(d.getId()).includeTaskLocalVariables().singleResult().getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
        assertTrue(engine.getTaskService().getTaskComments(d.getId(), "4").get(0).getFullMessage().contains("补充材料"));
        assertEquals("target", BpmTaskReturnService.retainedTarget(engine.getRuntimeService(), task(instance, "1").getExecutionId(), SOURCE));
    }
    @Test void allMembersReturnCreatesIndependentTasksAndNoForwardToken() throws Exception {
        String instance = start(false, false); dispatch(instance);
        back(instance, 4); back(instance, 5); back(instance, 6);
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(SOURCE).count());
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("after").count());
        assertEquals(0, engine.getRuntimeService().createExecutionQuery().processInstanceId(instance).activityId("target").count());
    }
    @Test void sequentialReturnActivatesNextAndDoesNotRejoinOldRoot() throws Exception {
        String instance = start(true, false); dispatch(instance);
        assertNull(task(instance, "5")); back(instance, 4);
        assertNotNull(task(instance, "5")); assertNull(task(instance, "6"));
        back(instance, 5); assertNotNull(task(instance, "6")); back(instance, 6);
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(SOURCE).count());
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("after").count());
    }
    @Test void orSignReturnDoesNotCancelOthersAndNormalApprovalStillCompletes() throws Exception {
        String instance = start(false, false, false, true); dispatch(instance);
        back(instance, 4); assertNotNull(task(instance, "5")); assertNotNull(task(instance, "6"));
        engine.getTaskService().complete(task(instance, "5").getId());
        assertNull(task(instance, "6")); assertNotNull(task(instance, "1"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("after").count());
    }
    @Test void completedThenLastReturnAllowsOriginalRouteAndKeepsNewTask() throws Exception {
        String instance = start(false, false); dispatch(instance);
        engine.getTaskService().complete(task(instance, "4").getId()); back(instance, 5); back(instance, 6);
        assertEquals(2, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(SOURCE).count());
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("after").count());
    }
    @Test void returnToMultiInstanceCreatesExactlyOneTargetUser() throws Exception {
        String instance = start(false, false, false, false, false, true); dispatch(instance);
        back(instance, 4);
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(SOURCE).count());
        assertNotNull(task(instance, "1"));
    }
    @Test void forgedUserAndMultipleAssigneesRejectWithoutMutation() throws Exception {
        String instance = start(false, false); dispatch(instance);
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(task(instance, "4").getId()); req.setTargetTaskDefinitionKey(SOURCE); req.setReason("test");
        req.setReturnAssignees(Collections.singletonList(9L));
        assertThrows(RuntimeException.class, () -> service().returnTask(4L, req));
        req.setReturnAssignees(Arrays.asList(1L, 1L));
        assertThrows(RuntimeException.class, () -> service().returnTask(4L, req));
        assertThrows(RuntimeException.class, () -> service().targets(5L, req.getId()));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void listAndSubmitRejectUnvisitedNodeAndStaleTask() throws Exception {
        String instance = start(false, false); dispatch(instance);
        Task d = task(instance, "4");
        assertEquals(Collections.singletonList(SOURCE), service().targets(4L, d.getId()).stream().map(BpmTaskReturnTargetRespVO::getTaskDefinitionKey).collect(Collectors.toList()));
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(d.getId()); req.setTargetTaskDefinitionKey("after"); req.setReason("test");
        assertThrows(RuntimeException.class, () -> service().returnTask(4L, req));
        engine.getTaskService().complete(d.getId()); req.setTargetTaskDefinitionKey(SOURCE);
        assertThrows(RuntimeException.class, () -> service().returnTask(4L, req));
        assertNull(task(instance, "1"));
    }

    @Test void redoAllowsOtherRoutesAndRejectsRetainedRecipients() throws Exception {
        String instance = start(false, false); dispatch(instance); back(instance, 4);
        Task restored = task(instance, "1"); Task oldE = task(instance, "5");
        Set<Long> retained = BpmTaskReturnService.retainedUsers(engine.getRuntimeService(), instance, restored.getExecutionId(), SOURCE);
        assertEquals(new HashSet<>(Arrays.asList(5L, 6L)), retained);
        BpmTaskServiceImpl approvals = new BpmTaskServiceImpl();
        java.lang.reflect.Field runtime = BpmTaskServiceImpl.class.getDeclaredField("runtimeService"); runtime.setAccessible(true); runtime.set(approvals, engine.getRuntimeService());
        java.lang.reflect.Method validate = BpmTaskServiceImpl.class.getDeclaredMethod("validateRestoredDispatch", Task.class, BpmnModel.class, Set.class, Map.class); validate.setAccessible(true);
        BpmnModel model = engine.getRepositoryService().getBpmnModel(restored.getProcessDefinitionId());
        ExtensionElement manual = new ExtensionElement(); manual.setName("candidateStrategy"); manual.setElementText("55");
        ((UserTask) model.getFlowElement("target")).addExtensionElement(manual);
        validate.invoke(approvals, restored, model, Collections.singleton("after"), Collections.emptyMap());
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> validate.invoke(approvals, restored, model, Collections.singleton("target"), Collections.singletonMap("target", Collections.singletonList(5L))));
        validate.invoke(approvals, restored, model, Collections.singleton("target"), Collections.singletonMap("target", Collections.singletonList(8L)));
        String branch = engine.getRuntimeService().createExecutionQuery().executionId(restored.getExecutionId()).singleResult().getParentId();
        engine.getRuntimeService().setVariableLocal(branch, BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap("target", Collections.singleton(8L)));
        BpmDispatchSources.prepare(engine.getRuntimeService(), branch, restored.getId(), Collections.singletonMap("target", Collections.singletonList(8L)));
        engine.getTaskService().complete(restored.getId());
        assertNotNull(task(instance, "8")); assertNull(task(instance, "4"));
        assertEquals(oldE.getId(), task(instance, "5").getId());
        String oldRoot = engine.getRuntimeService().createExecutionQuery().executionId(oldE.getExecutionId()).singleResult().getParentId();
        String newRoot = engine.getRuntimeService().createExecutionQuery().executionId(task(instance, "8").getExecutionId()).singleResult().getParentId();
        assertNotEquals(oldRoot, newRoot);
    }
    @Test void returnedSourceCanSendOtherBranchAndKeepsExistingSiblingRecipients() throws Exception {
        String instance = start(false, false, false, false, true);
        Task source = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getRuntimeService().setVariable(instance, "select_node_2", "extra");
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, source.getId(), Collections.singletonMap("extra", Collections.singletonList(7L)));
        dispatch(instance); String sibling = task(instance, "7").getId(); back(instance, 4);
        Task restored = task(instance, "1");
        Map<String, Set<Long>> retained = BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), instance, restored.getExecutionId(), SOURCE);
        assertEquals(new HashSet<>(Arrays.asList(5L, 6L)), retained.get("target"));
        assertEquals(Collections.singleton(7L), retained.get("extra"));
        BpmTaskServiceImpl approvals = new BpmTaskServiceImpl();
        java.lang.reflect.Field runtime = BpmTaskServiceImpl.class.getDeclaredField("runtimeService"); runtime.setAccessible(true); runtime.set(approvals, engine.getRuntimeService());
        java.lang.reflect.Method validate = BpmTaskServiceImpl.class.getDeclaredMethod("validateRestoredDispatch", Task.class, BpmnModel.class, Set.class, Map.class); validate.setAccessible(true);
        BpmnModel model = engine.getRepositoryService().getBpmnModel(restored.getProcessDefinitionId());
        for (String node : Arrays.asList("target", "extra")) {
            ExtensionElement manual = new ExtensionElement(); manual.setName("candidateStrategy"); manual.setElementText("55");
            ((UserTask) model.getFlowElement(node)).addExtensionElement(manual);
        }
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> validate.invoke(approvals, restored, model,
                Collections.singleton("extra"), Collections.singletonMap("extra", Collections.singletonList(7L))));
        Map<String, List<Long>> choices = new LinkedHashMap<>(); choices.put("target", Collections.singletonList(8L)); choices.put("extra", Collections.singletonList(9L));
        validate.invoke(approvals, restored, model, choices.keySet(), choices);
        String branch = engine.getRuntimeService().createExecutionQuery().executionId(restored.getExecutionId()).singleResult().getParentId();
        engine.getRuntimeService().setVariableLocal(branch, "select_node_2", "extra");
        engine.getRuntimeService().setVariableLocal(branch, BpmDispatchSources.NEW_MEMBERS, choices);
        BpmDispatchSources.prepare(engine.getRuntimeService(), branch, restored.getId(), choices);
        engine.getTaskService().complete(restored.getId());
        assertNotNull(task(instance, "8")); assertNotNull(task(instance, "9"));
        assertEquals(sibling, task(instance, "7").getId()); assertNotNull(task(instance, "5"));
    }

    @Test void siblingReturnReleasesRecipientForAlreadyOpenRestoredTask() throws Exception {
        String instance = start(false, false, false, false, true);
        Task source = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getRuntimeService().setVariable(instance, "select_node_2", "extra");
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, source.getId(), Collections.singletonMap("extra", Collections.singletonList(7L)));
        dispatch(instance); back(instance, 4); Task restored = task(instance, "1");
        String branch = engine.getRuntimeService().createExecutionQuery().executionId(restored.getExecutionId()).singleResult().getParentId();
        engine.getRuntimeService().removeVariableLocal(branch, BpmTaskReturnService.RETAINED_SOURCES);
        assertEquals(Collections.singleton(7L), BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), engine.getHistoryService(), instance, restored.getExecutionId(), SOURCE).get("extra"));
        engine.getRuntimeService().setVariableLocal(branch, BpmTaskReturnService.RETAINED_SOURCES, Collections.singletonMap(SOURCE, Collections.singleton(source.getId())));
        assertEquals(Collections.singleton(7L), BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), instance, restored.getExecutionId(), SOURCE).get("extra"));
        back(instance, 7);
        assertTrue(BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), instance, restored.getExecutionId(), SOURCE).get("extra").isEmpty());
        assertEquals(2, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee("1").count());
        assertNotNull(task(instance, "5"));
    }

    @Test void secondReturnKeepsFirstRoundRecipientsAndTracksTheirLaterChanges() throws Exception {
        String instance = start(false, false, false, false, true);
        Task original = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getRuntimeService().setVariable(instance, "select_node_2", "extra");
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, original.getId(), Collections.singletonMap("extra", Collections.singletonList(7L)));
        dispatch(instance); String peerId = task(instance, "5").getId(); String siblingId = task(instance, "7").getId();
        back(instance, 4); Task first = task(instance, "1");
        String branch = engine.getRuntimeService().createExecutionQuery().executionId(first.getExecutionId()).singleResult().getParentId();
        engine.getTaskService().setVariablesLocal(first.getId(), new HashMap<String, Object>() {{
            put("RETURNABLE_TASK_PATH", Collections.singletonList(SOURCE));
            put("RETURN_BRANCH_CONTEXT", Collections.singletonMap(SOURCE, first.getId()));
            put(BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        }});
        engine.getRuntimeService().setVariableLocal(branch, BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap("target", Collections.singleton(4L)));
        BpmDispatchSources.prepare(engine.getRuntimeService(), branch, first.getId(), Collections.singletonMap("target", Collections.singletonList(4L)));
        engine.getTaskService().complete(first.getId());
        back(instance, 4); Task second = task(instance, "1");
        Map<String, Set<Long>> retained = BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), engine.getHistoryService(), instance, second.getExecutionId(), SOURCE);
        assertEquals(new HashSet<>(Arrays.asList(5L, 6L)), retained.get("target"));
        assertEquals(Collections.singleton(7L), retained.get("extra"));
        assertEquals(peerId, task(instance, "5").getId()); assertEquals(siblingId, task(instance, "7").getId());
        BpmTaskServiceImpl approvals = new BpmTaskServiceImpl();
        for (String name : Arrays.asList("runtimeService", "historyService")) {
            java.lang.reflect.Field field = BpmTaskServiceImpl.class.getDeclaredField(name); field.setAccessible(true);
            field.set(approvals, name.equals("runtimeService") ? engine.getRuntimeService() : engine.getHistoryService());
        }
        java.lang.reflect.Method validate = BpmTaskServiceImpl.class.getDeclaredMethod("validateRestoredDispatch", Task.class, BpmnModel.class, Set.class, Map.class); validate.setAccessible(true);
        BpmnModel model = engine.getRepositoryService().getBpmnModel(second.getProcessDefinitionId());
        ExtensionElement manual = new ExtensionElement(); manual.setName("candidateStrategy"); manual.setElementText("55");
        ((UserTask) model.getFlowElement("target")).addExtensionElement(manual);
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> validate.invoke(approvals, second, model, Collections.singleton("target"), Collections.singletonMap("target", Collections.singletonList(5L))));
        validate.invoke(approvals, second, model, Collections.singleton("target"), Collections.singletonMap("target", Collections.singletonList(4L)));
        String originalRoot = engine.getRuntimeService().createExecutionQuery().executionId(task(instance, "5").getExecutionId()).singleResult().getParentId();
        internal(instance, 5, 8L);
        BpmTaskReturnService.retainAddedMembers(engine.getRuntimeService(), instance, originalRoot, Collections.singleton(8L));
        assertTrue(BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), engine.getHistoryService(), instance, second.getExecutionId(), SOURCE).get("target").contains(8L));
        back(instance, 6);
        assertFalse(BpmTaskReturnService.retainedRecipients(engine.getRuntimeService(), engine.getHistoryService(), instance, second.getExecutionId(), SOURCE).get("target").contains(6L));
    }

    @Test void smsAndRouteDecisionsAreLocalToEachReturn() throws Exception {
        String instance = start(false, false); dispatch(instance);
        engine.getRuntimeService().setVariable(instance, "select_node_99", "old");
        engine.getRuntimeService().setVariable(instance, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, Collections.singletonMap("target", Arrays.asList(4L, 5L, 6L)));
        Task d = task(instance, "4");
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(d.getId()); req.setTargetTaskDefinitionKey(SOURCE); req.setReason("补充"); req.setSendSms(true);
        service().returnTask(4L, req);
        Task restored = task(instance, "1");
        assertEquals(true, engine.getTaskService().getVariableLocal(restored.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertEquals(true, engine.getRuntimeService().getVariableLocal(restored.getExecutionId(), String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, SOURCE)));
        assertEquals(Collections.emptyMap(), engine.getRuntimeService().getVariable(restored.getExecutionId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES));
        assertEquals(Collections.singletonMap("target", Arrays.asList(4L, 5L, 6L)), engine.getRuntimeService().getVariable(instance, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES));
        back(instance, 5);
        List<Task> sources = engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(SOURCE).list();
        Task second = sources.stream().filter(t -> !t.getId().equals(restored.getId())).findFirst().get();
        assertEquals(false, engine.getTaskService().getVariableLocal(second.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertEquals(true, engine.getTaskService().getVariableLocal(restored.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
    }
    @Test void failureCreatingTargetRollsBackReturnAndAudit() throws Exception {
        String instance = start(false, false); dispatch(instance);
        Task d = task(instance, "4");
        BpmnModel model = engine.getRepositoryService().getBpmnModel(d.getProcessDefinitionId());
        ((UserTask) model.getFlowElement(SOURCE)).setBehavior((org.flowable.engine.impl.delegate.ActivityBehavior) execution -> { throw new IllegalStateException("target creation failed"); });
        assertThrows(RuntimeException.class, () -> back(instance, 4));
        assertEquals(d.getId(), task(instance, "4").getId());
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertTrue(engine.getTaskService().getTaskComments(d.getId(), "4").isEmpty());
    }
    @Test void disabledHistoricUserCannotBeChosen() throws Exception {
        String instance = start(false, false); dispatch(instance);
        BpmTaskReturnService service = service();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("adminUserApi"); field.setAccessible(true);
        AdminUserApi users = (AdminUserApi) field.get(service);
        AdminUserRespDTO disabled = user(1L); disabled.setStatus(1);
        when(users.getUserList(anyCollection())).thenReturn(Collections.singletonList(disabled));
        doThrow(new IllegalArgumentException("disabled user")).when(users).validateUser(1L);
        assertTrue(service.targets(4L, task(instance, "4").getId()).isEmpty());
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(task(instance, "4").getId()); req.setTargetTaskDefinitionKey(SOURCE); req.setReason("test");
        assertThrows(RuntimeException.class, () -> service.returnTask(4L, req));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }

    @Test void registrationReturnAssignsOnlyChosenHistoricUser() throws Exception {
        String instance = start(false, false, true); dispatch(instance); back(instance, 4);
        assertNotNull(task(instance, "1"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey(SOURCE).count());
    }
    @Test void inheritedRecipientMappingSurvivesAnotherReturnWithoutRestrictingRoutes() throws Exception {
        String instance = start(false, false); dispatch(instance);
        engine.getRuntimeService().setVariableLocal(task(instance, "4").getExecutionId(), BpmTaskReturnService.SCOPE, Collections.singletonMap("target", "after"));
        back(instance, 4);
        assertEquals("after", BpmTaskReturnService.retainedTarget(engine.getRuntimeService(), task(instance, "1").getExecutionId(), "target"));
    }
    @Test void singleInstanceReturnPreservesIndependentBranch() throws Exception {
        BpmnModel model = new BpmnModel(); org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process(); process.setId("singleReturn"); model.addProcess(process);
        StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
        for (String id : Arrays.asList(SOURCE, "target")) { UserTask task = new UserTask(); task.setId(id); task.setName(id); process.addFlowElement(task); }
        EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
        String[] ids = {"start", SOURCE, "target", "end"};
        for (int i = 0; i < ids.length - 1; i++) { SequenceFlow f = new SequenceFlow(ids[i], ids[i+1]); f.setId("f" + i); process.addFlowElement(f); }
        engine.getRepositoryService().createDeployment().addBpmnModel("single.bpmn20.xml", model).deploy();
        String instance = engine.getRuntimeService().startProcessInstanceByKey("singleReturn", Collections.singletonMap(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT)).getId();
        String source = dispatch(instance);
        Task target = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getRuntimeService().setVariableLocal(target.getExecutionId(), "RETURNABLE_TASK_PATH", Collections.singletonList(SOURCE));
        engine.getRuntimeService().setVariableLocal(target.getExecutionId(), "RETURN_BRANCH_CONTEXT", Collections.singletonMap(SOURCE, source));
        back(instance, Long.parseLong(target.getAssignee()));
        assertNotNull(task(instance, "1"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void resolvedDelegationCanReturnButPendingDelegationCannot() throws Exception {
        String instance = start(false, false); dispatch(instance); Task d = task(instance, "4");
        engine.getTaskService().delegateTask(d.getId(), "9");
        assertThrows(RuntimeException.class, () -> service().targets(9L, d.getId()));
        engine.getTaskService().resolveTask(d.getId());
        back(instance, 4); assertNotNull(task(instance, "1"));
    }

    @Test void legacyProcessKeepsLegacyRulesForLaterSequentialTasks() throws Exception {
        String instance = start(true, false);
        engine.getRuntimeService().removeVariableLocal(instance, BpmProcessOperationVersion.KEY);
        assertEquals("LEGACY", service().mode(1L, task(instance, "1").getId()));
        dispatch(instance);
        assertEquals("LEGACY", service().mode(4L, task(instance, "4").getId()));
        engine.getTaskService().complete(task(instance, "4").getId());
        assertEquals("LEGACY", service().mode(5L, task(instance, "5").getId()));
        assertNull(task(instance, "6"));
    }

    @Test void taskAndBusinessVariablesCannotOverrideProcessVersion() throws Exception {
        String instance = start(false, false); legacyDispatch(instance);
        engine.getTaskService().setVariableLocal(task(instance, "4").getId(), BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT);
        assertEquals("LEGACY", service().mode(4L, task(instance, "4").getId()));
        Map<String, Object> form = new HashMap<>();
        form.put(BpmProcessOperationVersion.KEY, 1); form.put("businessField", "keep");
        assertEquals(Collections.singletonMap("businessField", "keep"), BpmDispatchSources.businessVariables(form));
        assertEquals(1, form.get(BpmProcessOperationVersion.KEY));
        legacySubmit(instance, legacyRequest(instance));
        assertEquals("LEGACY", service().mode(1L, task(instance, "1").getId()));
        String modern = start(false, false);
        engine.getTaskService().setVariableLocal(task(modern, "1").getId(), BpmProcessOperationVersion.KEY, 1);
        assertEquals("SINGLE", service().mode(1L, task(modern, "1").getId()));
    }

    private String legacyDispatch(String instance) {
        Task source = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getRuntimeService().removeVariableLocal(instance, BpmProcessOperationVersion.KEY);
        engine.getTaskService().setVariableLocal(source.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        engine.getTaskService().complete(source.getId());
        return source.getId();
    }
    private BpmTaskReturnReqVO legacyRequest(String instance) {
        BpmTaskReturnReqVO req = new BpmTaskReturnReqVO(); req.setId(task(instance, "4").getId());
        req.setTargetTaskDefinitionKey(SOURCE); req.setReason("整体退回补正"); req.setExpectedMode("LEGACY"); req.setSendSms(false); return req;
    }
    private void legacySubmit(String instance, BpmTaskReturnReqVO req) throws Exception {
        BpmTaskServiceImpl tasks = new BpmTaskServiceImpl();
        Map<String, Object> fields = new HashMap<>(); fields.put("taskService", engine.getTaskService());
        fields.put("runtimeService", engine.getRuntimeService()); fields.put("historyService", engine.getHistoryService());
        fields.put("managementService", engine.getManagementService()); fields.put("taskReturnService", service());
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            java.lang.reflect.Field field = tasks.getClass().getDeclaredField(entry.getKey()); field.setAccessible(true); field.set(tasks, entry.getValue());
        }
        Task task = task(instance, "4"); BpmnModel model = engine.getRepositoryService().getBpmnModel(task.getProcessDefinitionId());
        tasks.returnTask(4L, model, task, model.getFlowElement(SOURCE), req);
    }
    @Test void pureLegacyParallelReturnsWholeGroupAndRetainsHistory() throws Exception {
        String instance = start(false, false); legacyDispatch(instance);
        Task d = task(instance, "4"), e = task(instance, "5");
        assertEquals("LEGACY", service().mode(4L, d.getId()));
        BpmTaskReturnTargetRespVO target = service().targets(4L, d.getId()).get(0);
        assertEquals("LEGACY", target.getReturnMode()); assertTrue(target.getAssignees().isEmpty());
        legacySubmit(instance, legacyRequest(instance));
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("target").count());
        assertNotNull(task(instance, "1"));
        assertEquals(false, engine.getTaskService().getVariableLocal(task(instance, "1").getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertEquals(cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.RETURN.getStatus(), engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(d.getId()).includeTaskLocalVariables().singleResult().getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
        assertEquals(cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.CANCEL.getStatus(), engine.getHistoryService().createHistoricTaskInstanceQuery().taskId(e.getId()).includeTaskLocalVariables().singleResult().getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
    }
    @Test void pureLegacySequentialDropsQueueAndRestoresSource() throws Exception {
        String instance = start(true, false); legacyDispatch(instance);
        BpmTaskReturnReqVO request = legacyRequest(instance); request.setExpectedMode(null); // 旧客户端兼容
        legacySubmit(instance, request);
        assertNotNull(task(instance, "1")); assertNull(task(instance, "5")); assertNull(task(instance, "6"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void pureLegacyOrSignReturnsWholeGroupToMultiInstanceSource() throws Exception {
        String instance = start(false, false, false, true, false, true); legacyDispatch(instance);
        legacySubmit(instance, legacyRequest(instance));
        assertNotNull(task(instance, "1"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("target").count());
    }
    @Test void legacyProcessIgnoresLaterMetadataAndRestoresLegacyTask() throws Exception {
        String instance = start(false, false); legacyDispatch(instance); BpmTaskReturnReqVO req = legacyRequest(instance);
        engine.getRuntimeService().setVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + "other", Collections.emptyMap());
        assertEquals("LEGACY", service().mode(4L, req.getId()));
        assertEquals("LEGACY", service().targets(4L, req.getId()).get(0).getReturnMode());
        legacySubmit(instance, req);
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertEquals("LEGACY", service().mode(1L, task(instance, "1").getId()));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmProcessOperationVersion.KEY));
    }
    @Test void legacyRejectsSinglePersonAndStaleModeAndUnvisitedTarget() throws Exception {
        String instance = start(false, false); legacyDispatch(instance); BpmTaskReturnReqVO req = legacyRequest(instance);
        req.setReturnAssignees(Collections.singletonList(1L)); assertThrows(RuntimeException.class, () -> legacySubmit(instance, req));
        req.setReturnAssignees(null); req.setExpectedMode("SINGLE"); assertThrows(RuntimeException.class, () -> legacySubmit(instance, req));
        req.setExpectedMode("LEGACY"); req.setTargetTaskDefinitionKey("after"); assertThrows(RuntimeException.class, () -> legacySubmit(instance, req));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void incompleteNewProvenanceNeverFallsBackToLegacy() throws Exception {
        String instance = start(false, false); legacyDispatch(instance); BpmTaskReturnReqVO req = legacyRequest(instance);
        engine.getRuntimeService().setVariableLocal(instance, BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT);
        String root = engine.getRuntimeService().createExecutionQuery().executionId(task(instance, "4").getExecutionId()).singleResult().getParentId();
        engine.getRuntimeService().setVariableLocal(root, BpmDispatchSources.SOURCES, Collections.singletonMap("4", Collections.singleton("missingSource")));
        assertEquals("SINGLE", service().mode(4L, req.getId())); assertTrue(service().targets(4L, req.getId()).isEmpty());
        assertThrows(RuntimeException.class, () -> legacySubmit(instance, req));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test void legacyModeDoesNotChangeWhenPathIsAddedAfterOpeningDialog() throws Exception {
        String instance = start(false, false); legacyDispatch(instance); BpmTaskReturnReqVO req = legacyRequest(instance);
        assertEquals("LEGACY", service().targets(4L, req.getId()).get(0).getReturnMode());
        engine.getRuntimeService().setVariableLocal(task(instance, "4").getExecutionId(), "RETURNABLE_TASK_PATH", Collections.singletonList(SOURCE));
        assertEquals("LEGACY", service().mode(4L, req.getId()));
        legacySubmit(instance, req);
        assertNotNull(task(instance, "1"));
        assertNull(task(instance, "4"));
    }
    private String start(boolean sequential, boolean async) {
        return start(sequential, async, false);
    }

    private String start(boolean sequential, boolean async, boolean register) {
        return start(sequential, async, register, false);
    }
    private String start(boolean sequential, boolean async, boolean register, boolean anyOne) {
        return start(sequential, async, register, anyOne, false);
    }
    private String start(boolean sequential, boolean async, boolean register, boolean anyOne, boolean optional) {
        return start(sequential, async, register, anyOne, optional, false);
    }
    private String start(boolean sequential, boolean async, boolean register, boolean anyOne, boolean optional, boolean sourceMi) {
        return start(sequential, async, register, anyOne, optional, sourceMi, false);
    }
    private String start(boolean sequential, boolean async, boolean register, boolean anyOne, boolean optional, boolean sourceMi, boolean timers) {
        return start(sequential, async, register, anyOne, optional, sourceMi, timers, RECEIVE, SOURCE);
    }
    private String start(boolean sequential, boolean async, boolean register, boolean anyOne, boolean optional, boolean sourceMi, boolean timers,
                         String processKey, String sourceKey) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId(processKey);
        model.addProcess(process);
        StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
        for (String id : optional ? Arrays.asList(sourceKey, "target", "after", "extra") : Arrays.asList(sourceKey, "target", "after")) {
            UserTask task = new UserTask(); task.setId(id); task.setName(sourceKey.equals(id) ? "来文登记" : id);
            MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
            mi.setInputDataItem("${users}");
            mi.setElementVariable("user");
            mi.setSequential("target".equals(id) && sequential);
            mi.setCompletionCondition(anyOne && "target".equals(id) ? "${nrOfCompletedInstances > 0}" : "${nrOfCompletedInstances >= nrOfInstances}");
            task.setLoopCharacteristics(sourceKey.equals(id) && !sourceMi ? null : mi);
            if (register && sourceKey.equals(id)) {
                task.setLoopCharacteristics(null);
                ExtensionElement properties = new ExtensionElement();
                properties.setName("properties");
                properties.setNamespace("http://flowable.org/bpmn");
                properties.setNamespacePrefix("flowable");
                for (String[] pair : new String[][]{{"business_node_type", "register"},
                        {"register_submit_mode", "complete_on_submit"}}) {
                    ExtensionElement property = new ExtensionElement();
                    property.setName("property");
                    property.setNamespace("http://flowable.org/bpmn");
                    property.setNamespacePrefix("flowable");
                    property.addAttribute(new ExtensionAttribute("name", pair[0]));
                    property.addAttribute(new ExtensionAttribute("value", pair[1]));
                    properties.addChildElement(property);
                }
                task.addExtensionElement(properties);
                ExtensionElement strategy = new ExtensionElement();
                strategy.setName("candidateStrategy");
                strategy.setNamespace("http://flowable.org/bpmn");
                strategy.setNamespacePrefix("flowable");
                strategy.setElementText("36");
                task.addExtensionElement(strategy);
            }
            task.setAsynchronous("target".equals(id) && async);
            process.addFlowElement(task);
        }
        EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
        String[] ids = {"start", sourceKey, "target", "after", "end"};
        for (int i = 0; i < ids.length - 1; i++) {
            SequenceFlow flow = new SequenceFlow(ids[i], ids[i + 1]);
            flow.setId("f" + i); process.addFlowElement(flow);
        }
        if (optional) {
            SequenceFlow flow = new SequenceFlow(sourceKey, "extra"); flow.setId("optional");
            flow.setConditionExpression("${variables:get(select_node_2) == 'extra'}"); process.addFlowElement(flow);
            InclusiveGateway join = new InclusiveGateway(); join.setId("join"); process.addFlowElement(join);
            ((SequenceFlow) process.getFlowElement("f3")).setTargetRef("join");
            SequenceFlow finish = new SequenceFlow("extra", "join"); finish.setId("extraEnd"); process.addFlowElement(finish);
            SequenceFlow joined = new SequenceFlow("join", "end"); joined.setId("joined"); process.addFlowElement(joined);
        }
        if (timers) {
            BoundaryEvent reminder = new BoundaryEvent(); reminder.setId("reminder"); reminder.setCancelActivity(false);
            reminder.setAttachedToRef((UserTask) process.getFlowElement("target"));
            TimerEventDefinition timer = new TimerEventDefinition(); timer.setTimeDuration("PT10M"); reminder.addEventDefinition(timer);
            process.addFlowElement(reminder);
            SequenceFlow finishReminder = new SequenceFlow("reminder", "end"); finishReminder.setId("reminderEnd"); process.addFlowElement(finishReminder);
        }
        engine.getRepositoryService().createDeployment().addBpmnModel("sms.bpmn20.xml", model).deploy();
        return engine.getRuntimeService().startProcessInstanceByKey(processKey, Collections.singletonMap(BpmProcessOperationVersion.KEY, BpmProcessOperationVersion.CURRENT)).getId();
    }
}
