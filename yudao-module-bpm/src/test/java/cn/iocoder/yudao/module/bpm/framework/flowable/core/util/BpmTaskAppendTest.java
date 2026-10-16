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

class BpmTaskAppendTest {
    private ProcessEngine engine;

    @BeforeEach
    void setUp() {
        BpmTaskCandidateInvoker candidates = mock(BpmTaskCandidateInvoker.class);
        when(candidates.calculateUsersByTask(any())).thenAnswer(call -> {
            DelegateExecution execution = call.getArgument(0);
            switch (execution.getCurrentActivityId()) {
                case "source": return new LinkedHashSet<>(Collections.singletonList(1L));
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


    private BpmTaskAppendService service() throws Exception {
        BpmTaskAppendService service = new BpmTaskAppendService();
        BpmProcessInstanceService processes = mock(BpmProcessInstanceService.class);
        when(processes.getAppendCandidateUserIdsBatch(anyLong(), anyString(), anyMap())).thenCallRealMethod();
        when(processes.getAppendCandidateUserIds(anyLong(), anyString(), anyString(), anyString()))
                .thenAnswer(call -> new HashSet<>(Arrays.asList(4L, 5L, 6L, 8L, 9L)));
        AdminUserApi users = mock(AdminUserApi.class);
        when(users.getUser(anyLong())).thenAnswer(call -> user(call.getArgument(0)));
        when(users.getUserList(anyCollection())).thenAnswer(call -> ((Collection<Long>) call.getArgument(0))
                .stream().map(this::user).collect(Collectors.toList()));
        Map<String, Object> fields = new HashMap<>();
        fields.put("runtimeService", engine.getRuntimeService()); fields.put("historyService", engine.getHistoryService());
        fields.put("repositoryService", engine.getRepositoryService()); fields.put("managementService", engine.getManagementService());
        fields.put("flowableTaskService", engine.getTaskService()); fields.put("processInstanceService", processes);
        fields.put("adminUserApi", users);
        DeptService departments = mock(DeptService.class);
        when(departments.getDeptMap(anyCollection())).thenAnswer(call -> {
            DeptDO department = new DeptDO(); department.setId(10L); department.setName("办公室");
            return Collections.singletonMap(10L, department);
        });
        fields.put("deptService", departments);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            java.lang.reflect.Field field = service.getClass().getDeclaredField(entry.getKey());
            field.setAccessible(true); field.set(service, entry.getValue());
        }
        return service;
    }
    private AdminUserRespDTO user(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO(); user.setId(id); user.setNickname("人员" + id); user.setDeptId(id == 9L ? null : 10L); return user;
    }
    private String dispatch(String instance) {
        Task source = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getTaskService().setVariableLocal(source.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, source.getId(),
                Collections.singletonMap("target", Arrays.asList(4L, 5L, 6L)));
        engine.getTaskService().complete(source.getId());
        return source.getId();
    }
    private BpmTaskAppendReqVO request(String source, String root, Long... users) {
        BpmTaskAppendReqVO req = new BpmTaskAppendReqVO(); req.setSourceTaskId(source); req.setTargetExecutionId(root);
        req.setUserIds(Arrays.asList(users)); req.setSendSms(false); req.setReason("协同办理"); return req;
    }
    private void submit(BpmTaskAppendService service, BpmTaskAppendReqVO req) {
        // 一个引擎事务覆盖校验、增员和记录，模拟业务服务 @Transactional 边界。
        engine.getManagementService().executeCommand(context -> { service.append(1L, req); return null; });
    }
    private Task task(String instance, String user) {
        return engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee(user).singleResult();
    }
    @Test
    void appendDisplayUsesIncomingFlowMetadataAndBatchDepartments() throws Exception {
        String instance = start(false, false);
        String source = dispatch(instance);
        String definition = engine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(instance).singleResult().getProcessDefinitionId();
        BpmnModel model = engine.getRepositoryService().getBpmnModel(definition);
        SequenceFlow flow = (SequenceFlow) model.getFlowElement("f1");
        flow.setName("送主办协办");
        ExtensionElement wrapper = new ExtensionElement(); wrapper.setName("properties");
        ExtensionElement property = new ExtensionElement(); property.setName("property");
        property.addAttribute(new ExtensionAttribute("name", "order"));
        property.addAttribute(new ExtensionAttribute("value", "3"));
        wrapper.addChildElement(property); flow.addExtensionElement(wrapper);
        // 无关的其他人工节点后续连线不能覆盖本次来源的分组信息。
        ((SequenceFlow) model.getFlowElement("f2")).setName("后续办理");
        BpmTaskAppendService service = service();
        BpmTaskAppendTargetRespVO target = service.targets(1L, source).get(0);
        assertEquals("送主办协办", target.getFlowName());
        assertEquals(3, target.getFlowSort());
        BpmTaskAppendTargetRespVO.Member member = target.getCandidates().stream()
                .filter(user -> user.getId() == 8L).findFirst().get();
        assertEquals(10L, member.getDeptId());
        assertEquals("办公室", member.getDeptName());
        assertNull(target.getCandidates().stream().filter(user -> user.getId() == 9L).findFirst().get().getDeptId());
        java.lang.reflect.Field field = service.getClass().getDeclaredField("deptService"); field.setAccessible(true);
        verify((DeptService) field.get(service), times(1)).getDeptMap(Collections.singleton(10L));
    }

    @Test
    void candidatesFollowDepartmentAndUserSortIncludingMissingValues() throws Exception {
        String instance = start(false, false);
        String source = dispatch(instance);
        BpmTaskAppendService service = service();
        java.lang.reflect.Field userField = service.getClass().getDeclaredField("adminUserApi"); userField.setAccessible(true);
        AdminUserApi users = (AdminUserApi) userField.get(service);
        when(users.getUserList(anyCollection())).thenAnswer(call -> {
            List<AdminUserRespDTO> result = new ArrayList<>();
            for (Long id : (Collection<Long>) call.getArgument(0)) {
                AdminUserRespDTO user = user(id);
                user.setDeptId(id == 9L ? null : id == 4L ? 10L : 20L);
                user.setSort(id == 5L ? 2L : 1L);
                if (id == 8L) user.setSort(null);
                result.add(user);
            }
            Collections.reverse(result); // 数据库返回顺序不应决定页面顺序
            return result;
        });
        java.lang.reflect.Field deptField = service.getClass().getDeclaredField("deptService"); deptField.setAccessible(true);
        DeptService departments = (DeptService) deptField.get(service);
        DeptDO first = new DeptDO(); first.setId(20L); first.setSort(1);
        DeptDO second = new DeptDO(); second.setId(10L); second.setSort(2);
        Map<Long, DeptDO> map = new HashMap<>(); map.put(20L, first); map.put(10L, second);
        when(departments.getDeptMap(anyCollection())).thenReturn(map);
        assertEquals(Arrays.asList(6L, 5L, 8L, 4L, 9L), service.targets(1L, source).get(0).getCandidates()
                .stream().map(BpmTaskAppendTargetRespVO.Member::getId).collect(Collectors.toList()));
        first.setSort(null); // 未配置部门排序排到已配置部门后面
        assertEquals(Arrays.asList(4L, 6L, 5L, 8L, 9L), service.targets(1L, source).get(0).getCandidates()
                .stream().map(BpmTaskAppendTargetRespVO.Member::getId).collect(Collectors.toList()));
        first.setSort(2); // 部门排序相同，按部门编号稳定排序
        assertEquals(Arrays.asList(4L, 6L, 5L, 8L, 9L), service.targets(1L, source).get(0).getCandidates()
                .stream().map(BpmTaskAppendTargetRespVO.Member::getId).collect(Collectors.toList()));
        verify(departments, times(3)).getDeptMap(anyCollection());
    }

    @Test
    void businessFormCannotSupplyDispatchAuthorization() {
        Map<String, Object> input = new HashMap<>();
        input.put(BpmDispatchSources.SOURCES, Collections.singletonMap("8", Collections.singleton("forged")));
        input.put("bpm_dispatch_pending_6_target_8", Collections.singleton("forged"));
        input.put(BpmDispatchSources.OPTIONS, Collections.singleton("arbitrary-node"));
        input.put(BpmDispatchSources.LEDGER_PREFIX + "forged", Collections.emptyMap());
        input.put(BpmDispatchSources.BRANCH, true);
        input.put(BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap("target", Collections.singleton(99L)));
        input.put("reason", "正常业务变量");
        Map<String, Object> filtered = BpmDispatchSources.businessVariables(input);
        assertEquals(Collections.singletonMap("reason", "正常业务变量"), filtered);
        assertEquals(7, input.size());
        assertTrue(BpmDispatchSources.businessVariables(null).isEmpty());
    }

    @Test
    void restrictedInternalOperatorCannotBypassDepartmentLimitThroughAppend() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskAppendService service = service();
        String root = service.targets(1L, source).get(0).getExecutionId();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("processInstanceService");
        field.setAccessible(true);
        BpmProcessInstanceService processes = (BpmProcessInstanceService) field.get(service);
        when(processes.isRestrictedInternalOperator(anyLong(), anyString(), anyString(), anyBoolean())).thenReturn(true);
        assertTrue(service.targets(1L, source).isEmpty());
        assertThrows(RuntimeException.class, () -> submit(service, request(source, root, 8L)));
        assertNull(task(instance, "8"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertTrue(engine.getTaskService().getTaskComments(source, "10").isEmpty());
    }

    @Test
    void parallelAppendPreservesCompletedMembersCountsSmsAndAudit() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskAppendService service = service();
        String root = service.targets(1L, source).get(0).getExecutionId();
        engine.getTaskService().complete(task(instance, "4").getId());
        submit(service, request(source, root, 8L, 9L));
        assertEquals(5, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertEquals(4, engine.getRuntimeService().getVariableLocal(root, "nrOfActiveInstances"));
        assertEquals(1, engine.getRuntimeService().getVariableLocal(root, "nrOfCompletedInstances"));
        assertEquals(false, engine.getTaskService().getVariableLocal(task(instance, "8").getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertEquals(true, engine.getTaskService().getVariableLocal(task(instance, "5").getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertTrue(engine.getTaskService().getTaskComments(source, "10").get(0).getFullMessage().contains("人员8"));
        assertThrows(RuntimeException.class, () -> submit(service, request(source, root, 4L)));
        assertThrows(RuntimeException.class, () -> submit(service, request(source, root, 8L)));
        assertThrows(RuntimeException.class, () -> submit(service, request(source, root, 99L)));
        for (String id : Arrays.asList("5", "6", "8")) engine.getTaskService().complete(task(instance, id).getId());
        assertNull(task(instance, "7"));
        engine.getTaskService().complete(task(instance, "9").getId());
        assertNotNull(task(instance, "7"));
        assertTrue(service.targets(1L, source).isEmpty());
        assertThrows(RuntimeException.class, () -> submit(service, request(source, root, 9L)));
    }
    @Test
    void dispatchedMembersRemainVisibleButDisabled() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        engine.getTaskService().complete(task(instance, "4").getId());
        List<BpmTaskAppendTargetRespVO.Member> members = service().targets(1L, source).get(0).getCandidates();
        assertEquals(new HashSet<>(Arrays.asList(4L, 5L, 6L)), members.stream()
                .filter(BpmTaskAppendTargetRespVO.Member::isDisabled).map(BpmTaskAppendTargetRespVO.Member::getId)
                .collect(Collectors.toSet()));
        assertTrue(members.stream().filter(member -> member.getId().equals(8L)).noneMatch(BpmTaskAppendTargetRespVO.Member::isDisabled));
    }

    @Test
    void batchFailureRollsBackEarlierTargetCountersSmsAndAudit() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskAppendService service = service();
        String root = service.targets(1L, source).get(0).getExecutionId();
        BpmTaskAppendReqVO req = new BpmTaskAppendReqVO(); req.setSourceTaskId(source); req.setSendSms(false);
        BpmTaskAppendReqVO.Target first = new BpmTaskAppendReqVO.Target();
        first.setTargetExecutionId(root); first.setUserIds(Collections.singletonList(8L));
        BpmTaskAppendReqVO.Target invalid = new BpmTaskAppendReqVO.Target();
        invalid.setTargetExecutionId("missing"); invalid.setUserIds(Collections.singletonList(9L));
        req.setTargets(Arrays.asList(first, invalid));
        assertThrows(RuntimeException.class, () -> submit(service, req));
        assertNull(task(instance, "8"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertTrue(engine.getTaskService().getTaskComments(source, "10").isEmpty());
        assertEquals(true, engine.getTaskService().getVariableLocal(task(instance, "4").getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        req.setTargets(Collections.singletonList(first));
        submit(service, req);
        assertNotNull(task(instance, "8"));
        assertEquals(1, engine.getTaskService().getTaskComments(source, "10").size());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "true,false", "false,true"})
    void endedTargetCreatesIsolatedRoundAndCleansUpWithoutRestoringSource(boolean sequential, boolean async) throws Exception {
        String instance = start(sequential, async);
        Task original = task(instance, "1");
        BpmDispatchSources.recordOptions(engine.getManagementService(), engine.getTaskService(), original.getId(),
                original.getExecutionId(), (FlowNode) engine.getRepositoryService().getBpmnModel(original.getProcessDefinitionId()).getFlowElement("source"));
        String source = dispatch(instance);
        runJobs(instance);
        for (String id : Arrays.asList("4", "5", "6")) engine.getTaskService().complete(task(instance, id).getId());
        Task originalAfter = task(instance, "7"); assertNotNull(originalAfter);
        BpmTaskAppendService service = service();
        BpmTaskAppendTargetRespVO target = service.targets(1L, source).get(0);
        assertTrue(target.isNewRound());
        assertTrue(target.getCandidates().stream().filter(member -> member.getId().equals(4L)).allMatch(BpmTaskAppendTargetRespVO.Member::isDisabled));
        submit(service, request(source, target.getExecutionId(), 8L));
        runJobs(instance);
        Task added = task(instance, "8"); assertNotNull(added);
        assertNull(task(instance, "1"));
        String root = engine.getRuntimeService().createExecutionQuery().executionId(added.getExecutionId()).singleResult().getParentId();
        assertEquals(1, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertEquals(false, engine.getTaskService().getVariableLocal(added.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.NEW_MEMBERS));
        assertEquals(originalAfter.getId(), task(instance, "7").getId());
        engine.getTaskService().complete(added.getId());
        // 测试直接调用引擎 complete：两个独立分支分别沿模型继续，业务审批另有活跃人员去重。
        assertEquals(2, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("after").count());
        for (Task task : engine.getTaskService().createTaskQuery().processInstanceId(instance).list()) engine.getTaskService().complete(task.getId());
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
    }

    @Test
    void sequentialAppendQueuesWithoutCreatingEarlyTasks() throws Exception {
        String instance = start(true, false); String source = dispatch(instance); BpmTaskAppendService service = service();
        String root = service.targets(1L, source).get(0).getExecutionId();
        submit(service, request(source, root, 8L, 9L));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        for (String id : Arrays.asList("4", "5", "6", "8", "9")) {
            Task current = task(instance, id); assertNotNull(current);
            if (id.equals("8") || id.equals("9")) assertEquals(false,
                    engine.getTaskService().getVariableLocal(current.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
            engine.getTaskService().complete(current.getId());
        }
        assertNotNull(task(instance, "7"));
    }
    @Test
    void rejectsOtherOwnersUnrelatedInstancesAndLegacySources() throws Exception {
        BpmTaskAppendService service = service();
        String first = start(false, false); String source = dispatch(first);
        String second = start(false, false); String other = dispatch(second);
        String otherRoot = service.targets(1L, other).get(0).getExecutionId();
        assertThrows(RuntimeException.class, () -> service.targets(2L, source));
        assertThrows(RuntimeException.class, () -> submit(service, request(source, otherRoot, 8L)));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(second).count());
        String legacy = start(false, false); Task old = task(legacy, "1");
        engine.getTaskService().setVariableLocal(old.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        engine.getTaskService().complete(old.getId());
        assertTrue(service.targets(1L, old.getId()).isEmpty());
    }
    @Test
    void anyOneCompletionCancelsAppendedSiblings() throws Exception {
        String instance = start(false, false, false, true); String source = dispatch(instance);
        BpmTaskAppendService service = service();
        String root = service.targets(1L, source).get(0).getExecutionId();
        submit(service, request(source, root, 8L));
        engine.getTaskService().complete(task(instance, "8").getId());
        assertNotNull(task(instance, "7"));
        assertNull(task(instance, "4"));
        assertTrue(service.targets(1L, source).isEmpty());
    }

    @Test
    void asyncTargetKeepsSourceAndAppendedSmsChoice() throws Exception {
        String instance = start(false, true); String source = dispatch(instance);
        BpmTaskAppendService service = service();
        engine.getManagementService().createJobQuery().list().forEach(job -> engine.getManagementService().executeJob(job.getId()));
        String root = service.targets(1L, source).get(0).getExecutionId();
        submit(service, request(source, root, 8L));
        engine.getManagementService().createJobQuery().list().forEach(job -> engine.getManagementService().executeJob(job.getId()));
        assertEquals(false, engine.getTaskService().getVariableLocal(task(instance, "8").getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
    }

    @Test
    void sameNamedNodesInOneProcessRemainSeparate() throws Exception {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process(); process.setId("branches"); model.addProcess(process);
        StartEvent begin = new StartEvent(); begin.setId("start"); process.addFlowElement(begin);
        ParallelGateway split = new ParallelGateway(); split.setId("split"); process.addFlowElement(split);
        for (String id : Arrays.asList("source", "otherSource", "target")) {
            UserTask node = new UserTask(); node.setId(id); node.setName(id);
            {
                MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics(); mi.setInputDataItem("${users}");
                mi.setElementVariable("user"); mi.setCompletionCondition("${nrOfCompletedInstances >= nrOfInstances}"); node.setLoopCharacteristics(mi);
            }
            process.addFlowElement(node);
        }
        EndEvent end = new EndEvent(); end.setId("end"); process.addFlowElement(end);
        for (String[] edge : new String[][]{{"start", "split"}, {"split", "source"}, {"split", "otherSource"},
                {"source", "target"}, {"otherSource", "target"}, {"target", "end"}}) {
            SequenceFlow flow = new SequenceFlow(edge[0], edge[1]); flow.setId(edge[0] + edge[1]); process.addFlowElement(flow);
        }
        engine.getRepositoryService().createDeployment().addBpmnModel("branches.bpmn20.xml", model).deploy();
        String instance = engine.getRuntimeService().startProcessInstanceByKey("branches").getId();
        List<Task> sources = engine.getTaskService().createTaskQuery().processInstanceId(instance).list();
        for (Task task : sources) {
            engine.getTaskService().setAssignee(task.getId(), "1");
            engine.getTaskService().setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
            org.flowable.engine.runtime.Execution member = engine.getRuntimeService().createExecutionQuery()
                    .executionId(task.getExecutionId()).singleResult();
            org.flowable.engine.runtime.Execution miRoot = engine.getRuntimeService().createExecutionQuery()
                    .executionId(member.getParentId()).singleResult();
            BpmDispatchSources.prepare(engine.getRuntimeService(), miRoot.getParentId(), task.getId(),
                    Collections.singletonMap("target", Arrays.asList(4L, 5L, 6L)));
            engine.getTaskService().complete(task.getId());
        }
        BpmTaskAppendService service = service();
        String first = service.targets(1L, sources.get(0).getId()).get(0).getExecutionId();
        String second = service.targets(1L, sources.get(1).getId()).get(0).getExecutionId();
        assertNotEquals(first, second);
        assertThrows(RuntimeException.class, () -> submit(service, request(sources.get(0).getId(), second, 8L)));
        submit(service, request(sources.get(0).getId(), first, 8L));
        assertEquals(4, engine.getRuntimeService().getVariableLocal(first, "nrOfInstances"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(second, "nrOfInstances"));
        assertEquals(first, engine.getRuntimeService().createExecutionQuery().executionId(task(instance, "8").getExecutionId()).singleResult().getParentId());
        BpmTaskWithdrawService withdrawal = withdrawService();
        assertThrows(RuntimeException.class, () -> withdraw(withdrawal, withdrawal(sources.get(0).getId(), second, 4L)));
        withdraw(withdrawal, withdrawal(sources.get(0).getId(), first, 4L, 8L));
        assertEquals(2, engine.getRuntimeService().getVariableLocal(first, "nrOfInstances"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(second, "nrOfInstances"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee("4").count());

    }

    @Test
    void concurrentDuplicateAppendCreatesOneMemberAndOneAuditRecord() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskAppendService service = service(); String root = service.targets(1L, source).get(0).getExecutionId();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("processInstanceService"); field.setAccessible(true);
        BpmProcessInstanceService processes = (BpmProcessInstanceService) field.get(service);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        when(processes.getAppendCandidateUserIds(anyLong(), anyString(), anyString(), anyString())).thenAnswer(call -> {
            barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
            return new HashSet<>(Arrays.asList(4L, 5L, 6L, 8L, 9L));
        });
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) results.add(workers.submit(() -> {
                try { submit(service, request(source, root, 8L)); return true; }
                catch (RuntimeException expected) { return false; }
            }));
            int succeeded = 0;
            for (java.util.concurrent.Future<Boolean> result : results) if (result.get(20, java.util.concurrent.TimeUnit.SECONDS)) succeeded++;
            assertEquals(1, succeeded);
            assertEquals(4, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
            assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskAssignee("8").count());
            assertEquals(1, engine.getTaskService().getTaskComments(source, "10").size());
        } finally { workers.shutdownNow(); }
    }

    @Test
    void finalApprovalWinningRaceRollsBackAppendAndAudit() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskAppendService service = service(); String root = service.targets(1L, source).get(0).getExecutionId();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("adminUserApi"); field.setAccessible(true);
        AdminUserApi users = (AdminUserApi) field.get(service);
        java.util.concurrent.CountDownLatch staged = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch approved = new java.util.concurrent.CountDownLatch(1);
        when(users.getUser(1L)).thenAnswer(call -> {
            staged.countDown(); assertTrue(approved.await(10, java.util.concurrent.TimeUnit.SECONDS)); return user(1L);
        });
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<Boolean> appended = worker.submit(() -> {
                try { submit(service, request(source, root, 8L)); return true; }
                catch (RuntimeException expected) { return false; }
            });
            assertTrue(staged.await(10, java.util.concurrent.TimeUnit.SECONDS));
            try {
                for (String id : Arrays.asList("4", "5", "6")) engine.getTaskService().complete(task(instance, id).getId());
            } finally { approved.countDown(); }
            assertFalse(appended.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertNotNull(task(instance, "7")); assertNull(task(instance, "8"));
            assertTrue(engine.getTaskService().getTaskComments(source, "10").isEmpty());
        } finally { approved.countDown(); worker.shutdownNow(); }
    }

    @Test
    void skippedOutgoingBranchIsNotRecordedAsDispatchTarget() {
        String instance = start(false, false);
        Task source = task(instance, "1");
        BpmnModel model = engine.getRepositoryService().getBpmnModel(source.getProcessDefinitionId());
        UserTask node = (UserTask) model.getFlowElement("source");
        SequenceFlow flow = node.getOutgoingFlows().get(0);
        String original = flow.getConditionExpression();
        try {
            flow.setConditionExpression("${false}");
            assertTrue(BpmDispatchSources.selectedTargets(engine.getManagementService(), source.getExecutionId(), node).isEmpty());
            flow.setConditionExpression("${true}");
            assertEquals(Collections.singleton("target"),
                    BpmDispatchSources.selectedTargets(engine.getManagementService(), source.getExecutionId(), node));
        } finally { flow.setConditionExpression(original); }
    }

    @Test
    void batchAppendsRunningTargetAndPreviouslyUnselectedTarget() throws Exception {
        String instance = start(false, false, false, false, true);
        Task original = task(instance, "1");
        BpmDispatchSources.recordOptions(engine.getManagementService(), engine.getTaskService(), original.getId(), original.getExecutionId(),
                (FlowNode) engine.getRepositoryService().getBpmnModel(original.getProcessDefinitionId()).getFlowElement("source"));
        assertThrows(RuntimeException.class, () -> service().targets(1L, original.getId()));
        String source = dispatch(instance);
        assertEquals(0, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("extra").count());
        BpmTaskAppendService service = service();
        List<BpmTaskAppendTargetRespVO> targets = service.targets(1L, source);
        assertEquals(2, targets.size());
        BpmTaskAppendReqVO req = new BpmTaskAppendReqVO(); req.setSourceTaskId(source); req.setSendSms(false);
        List<BpmTaskAppendReqVO.Target> lines = new ArrayList<>();
        for (BpmTaskAppendTargetRespVO target : targets) {
            BpmTaskAppendReqVO.Target line = new BpmTaskAppendReqVO.Target(); line.setTargetExecutionId(target.getExecutionId());
            line.setUserIds(Collections.singletonList(target.isNewRound() ? 9L : 8L)); lines.add(line);
        }
        req.setTargets(lines); submit(service, req);
        assertEquals("target", task(instance, "8").getTaskDefinitionKey());
        assertEquals("extra", task(instance, "9").getTaskDefinitionKey());
        assertEquals(2, engine.getTaskService().getTaskComments(source, "10").size());
        assertFalse((Boolean) engine.getTaskService().getVariableLocal(task(instance, "9").getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        engine.getTaskService().complete(task(instance, "9").getId());
        assertNotNull(task(instance, "4"));
        BpmTaskAppendTargetRespVO extra = service.targets(1L, source).stream().filter(t -> t.getNodeId().equals("extra")).findFirst().get();
        assertTrue(extra.isNewRound());
        assertTrue(extra.getCandidates().stream().filter(m -> m.getId().equals(9L)).allMatch(BpmTaskAppendTargetRespVO.Member::isDisabled));
        for (String user : Arrays.asList("4", "5", "6", "8")) engine.getTaskService().complete(task(instance, user).getId());
        engine.getTaskService().complete(task(instance, "7").getId());
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
    }

    @Test
    void appendOptionsFreezeBusinessGatewayAndDoNotOpenAppeal() {
        String instance = start(false, false);
        Task source = task(instance, "1");
        BpmnModel model = engine.getRepositoryService().getBpmnModel(source.getProcessDefinitionId());
        UserTask node = (UserTask) model.getFlowElement("source");
        SequenceFlow normal = node.getOutgoingFlows().get(0);
        normal.setConditionExpression("${variables:get(select_node_7) == 'appeal'}");
        BpmDispatchSources.recordOptions(engine.getManagementService(), engine.getTaskService(), source.getId(), source.getExecutionId(), node);
        assertTrue(((Set<?>) engine.getTaskService().getVariableLocal(source.getId(), BpmDispatchSources.OPTIONS)).isEmpty());
        normal.setConditionExpression("${variables:get(select_node_7) == 'office'}");
        BpmDispatchSources.recordOptions(engine.getManagementService(), engine.getTaskService(), source.getId(), source.getExecutionId(), node);
        assertEquals(Collections.singleton("target"), engine.getTaskService().getVariableLocal(source.getId(), BpmDispatchSources.OPTIONS));
        ExclusiveGateway gateway = new ExclusiveGateway(); gateway.setId("businessDecision");
        SequenceFlow restricted = new SequenceFlow("businessDecision", "target"); restricted.setId("restricted");
        restricted.setTargetFlowElement(model.getFlowElement("target")); restricted.setConditionExpression("${days > 3}");
        gateway.setOutgoingFlows(Collections.singletonList(restricted)); normal.setTargetFlowElement(gateway); normal.setConditionExpression(null);
        engine.getRuntimeService().setVariable(instance, "days", 5);
        BpmDispatchSources.recordOptions(engine.getManagementService(), engine.getTaskService(), source.getId(), source.getExecutionId(), node);
        engine.getRuntimeService().setVariable(instance, "days", 1);
        assertEquals(Collections.singleton("target"), engine.getTaskService().getVariableLocal(source.getId(), BpmDispatchSources.OPTIONS));
    }

    @Test
    void legacySourceWithRealDispatchCanSupplementExplicitManualRoutes() throws Exception {
        String instance = start(false, false, false, false, true);
        String source = dispatch(instance); // 没有保存 options 的旧来源，但有真实转出关系
        BpmTaskAppendService service = service();
        List<BpmTaskAppendTargetRespVO> targets = service.targets(1L, source);
        assertEquals(new HashSet<>(Arrays.asList("target", "extra")), targets.stream()
                .map(BpmTaskAppendTargetRespVO::getNodeId).collect(Collectors.toSet()));
        java.lang.reflect.Field usersField = service.getClass().getDeclaredField("adminUserApi"); usersField.setAccessible(true);
        AdminUserApi users = (AdminUserApi) usersField.get(service);
        verify(users, times(1)).getUserList(anyCollection()); // 所有节点合并一次人员详情查询

        submit(service, request(source, "new:extra", 9L));
        assertNotNull(task(instance, "9"));
    }

    @Test
    void manualRouteParsingSupportsQuotedKeysAndPlainExpressionsWithoutOpeningBusinessBranches() {
        UserTask source = new UserTask(); source.setId("source");
        String[] expressions = {"${variables:get('select_node_111') == 'all'}",
                "${variables:get(\"select_node_112\") == 'leader'}", "${select_node_113 == 'dept2'}",
                "${variables:get(role_condition) == 'grade_11'}", "${variables:get(select_node_7) == 'appeal'}"};
        for (int i = 0; i < expressions.length; i++) {
            UserTask target = new UserTask(); target.setId("target" + i);
            SequenceFlow flow = new SequenceFlow(); flow.setId("flow" + i); flow.setConditionExpression(expressions[i]);
            flow.setTargetFlowElement(target); source.getOutgoingFlows().add(flow);
        }
        assertEquals(new HashSet<>(Arrays.asList("target0", "target1", "target2")), BpmDispatchSources.optionalManualTargets(source));
    }

    private void runJobs(String instance) {
        for (org.flowable.job.api.Job job : engine.getManagementService().createJobQuery().processInstanceId(instance).list()) {
            engine.getManagementService().executeJob(job.getId());
        }
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
    private BpmTaskWithdrawReqVO withdrawal(String source, String root, Long... ids) {
        BpmTaskWithdrawReqVO request = new BpmTaskWithdrawReqVO(); request.setSourceTaskId(source);
        request.setMembers(Arrays.stream(ids).map(id -> {
            BpmTaskWithdrawReqVO.Member member = new BpmTaskWithdrawReqVO.Member(); member.setExecutionId(root); member.setUserId(id); return member;
        }).collect(Collectors.toList())); return request;
    }
    private void withdraw(BpmTaskWithdrawService service, BpmTaskWithdrawReqVO request) {
        engine.getManagementService().executeCommand(context -> { service.withdraw(1L, request); return null; });
    }
    @Test
    void withdrawPartialAndReappendPreservesOtherMembers() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskAppendService append = service(); String root = append.targets(1L, source).get(0).getExecutionId();
        BpmTaskWithdrawService withdraw = withdrawService();
        withdraw(withdraw, withdrawal(source, root, 5L));
        assertNull(task(instance, "5")); assertNotNull(task(instance, "4")); assertNotNull(task(instance, "6")); assertNull(task(instance, "1"));
        assertEquals(2, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertEquals(0, engine.getRuntimeService().getVariableLocal(root, "nrOfCompletedInstances"));
        submit(append, request(source, root, 5L)); assertNotNull(task(instance, "5"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
        assertThrows(RuntimeException.class, () -> withdraw(withdraw, withdrawal(source, root, 99L)));
    }
    @Test
    void withdrawRemainingAfterOneCompletedAdvancesWithoutRestoringSource() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        engine.getTaskService().complete(task(instance, "4").getId());
        BpmTaskWithdrawService withdraw = withdrawService();
        assertFalse(withdraw.targets(1L, source).stream().filter(row -> row.getUserId() == 4L).findFirst().get().isWithdrawable());
        withdraw(withdraw, withdrawal(source, root, 5L, 6L));
        assertNull(task(instance, "1")); assertNotNull(task(instance, "7"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        engine.getTaskService().complete(task(instance, "7").getId());
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
    }
    @Test
    void withdrawAllRestoresExactlyOneSourceAndNoDownstream() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        BpmTaskWithdrawService withdraw = withdrawService();
        withdraw(withdraw, withdrawal(source, root, 4L, 5L, 6L));
        assertNotNull(task(instance, "1")); assertNull(task(instance, "7"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertThrows(RuntimeException.class, () -> withdraw(withdraw, withdrawal(source, root, 4L, 5L, 6L)));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test
    void withdrawSequentialQueueAndCurrentDoesNotSkipNext() throws Exception {
        String instance = start(true, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        BpmTaskWithdrawService withdraw = withdrawService();
        assertTrue(withdraw.targets(1L, source).stream().filter(row -> row.getUserId() == 6L).findFirst().get().isQueued());
        withdraw(withdraw, withdrawal(source, root, 6L));
        assertNotNull(task(instance, "4")); assertNull(task(instance, "5"));
        withdraw(withdraw, withdrawal(source, root, 4L));
        assertNotNull(task(instance, "5"));
        assertEquals(0, engine.getRuntimeService().getVariableLocal(root, "nrOfCompletedInstances"));
        engine.getTaskService().complete(task(instance, "5").getId());
        assertNotNull(task(instance, "7"));
    }
    @Test
    void withdrawSequentialRemainingAfterCompletedPreservesResult() throws Exception {
        String instance = start(true, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        engine.getTaskService().complete(task(instance, "4").getId());
        withdraw(withdrawService(), withdrawal(source, root, 5L, 6L));
        assertNotNull(task(instance, "7")); assertNull(task(instance, "1"));
    }
    @Test
    void withdrawalRejectsForeignSourceAndFinishedMemberAtomically() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        BpmTaskWithdrawService withdraw = withdrawService();
        assertThrows(RuntimeException.class, () -> withdraw.targets(2L, source));
        engine.getTaskService().complete(task(instance, "4").getId());
        assertThrows(RuntimeException.class, () -> withdraw(withdraw, withdrawal(source, root, 5L, 4L)));
        assertNotNull(task(instance, "5")); assertNotNull(task(instance, "6"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
    }


    @Test
    void withdrawalOfAddedBranchUnblocksInclusiveJoinWithoutSourceRestore() throws Exception {
        String instance = start(false, false, false, false, true); String source = dispatch(instance);
        BpmTaskAppendService append = service();
        submit(append, request(source, "new:extra", 9L));
        for (String user : Arrays.asList("4", "5", "6", "7")) engine.getTaskService().complete(task(instance, user).getId());
        BpmTaskWithdrawService withdraw = withdrawService();
        BpmTaskWithdrawTargetRespVO extra = withdraw.targets(1L, source).stream().filter(row -> row.getUserId() == 9L).findFirst().get();
        withdraw(withdraw, withdrawal(source, extra.getExecutionId(), 9L));
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
    }
    @Test
    void withdrawingOneWholeBranchPreservesOtherBranchAndLaterRestoresOnlyOnce() throws Exception {
        String instance = start(false, false, false, false, true); String source = dispatch(instance);
        BpmTaskAppendService append = service(); String root = append.targets(1L, source).stream()
                .filter(t -> t.getNodeId().equals("target")).findFirst().get().getExecutionId();
        submit(append, request(source, "new:extra", 9L));
        BpmTaskWithdrawService withdraw = withdrawService();
        withdraw(withdraw, withdrawal(source, root, 4L, 5L, 6L));
        assertNotNull(task(instance, "9")); assertNull(task(instance, "1")); assertNull(task(instance, "7"));
        String extra = withdraw.targets(1L, source).stream().filter(row -> row.getUserId() == 9L).findFirst().get().getExecutionId();
        withdraw(withdraw, withdrawal(source, extra, 9L));
        assertNotNull(task(instance, "1"));
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test
    void remainingParallelMemberCompletesAfterTwoWithdrawals() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        BpmTaskWithdrawService withdraw = withdrawService();
        withdraw(withdraw, withdrawal(source, root, 5L)); withdraw(withdraw, withdrawal(source, root, 6L));
        assertEquals(1, engine.getRuntimeService().getVariableLocal(root, "nrOfActiveInstances"));
        engine.getTaskService().complete(task(instance, "4").getId());
        assertNotNull(task(instance, "7"));
    }
    @Test
    void pendingDispatchPreventsPrematureRestoration() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        BpmDispatchSources.prepare(engine.getRuntimeService(), instance, source, Collections.singletonMap("extra", Collections.singleton(9L)));
        BpmTaskWithdrawService withdraw = withdrawService();
        assertThrows(RuntimeException.class, () -> withdraw(withdraw, withdrawal(source, root, 4L, 5L, 6L)));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test
    void sequentialAllWithdrawnRestoresSourceWithoutQueueTasks() throws Exception {
        String instance = start(true, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        withdraw(withdrawService(), withdrawal(source, root, 4L, 5L, 6L));
        assertNotNull(task(instance, "1")); assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }


    @Test
    void multiInstanceSourceIsRestoredWithOneUserAndFreshRouting() throws Exception {
        String instance = start(false, false, false, false, false, true);
        Task original = task(instance, "1");
        BpmnModel model = engine.getRepositoryService().getBpmnModel(original.getProcessDefinitionId());
        ((SequenceFlow) model.getFlowElement("f1")).setConditionExpression("${variables:get(select_node_42) == 'target'}");
        engine.getRuntimeService().setVariable(instance, "select_node_42", "target");
        engine.getRuntimeService().setVariable(instance, "otherBranchValue", "保留其他分支数据");
        engine.getTaskService().setVariableLocal(original.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        org.flowable.engine.runtime.Execution member = engine.getRuntimeService().createExecutionQuery().executionId(original.getExecutionId()).singleResult();
        org.flowable.engine.runtime.Execution sourceRoot = engine.getRuntimeService().createExecutionQuery().executionId(member.getParentId()).singleResult();
        BpmDispatchSources.prepare(engine.getRuntimeService(), sourceRoot.getParentId(), original.getId(), Collections.singletonMap("target", Arrays.asList(4L, 5L, 6L)));
        engine.getTaskService().complete(original.getId());
        String root = service().targets(1L, original.getId()).get(0).getExecutionId();
        withdraw(withdrawService(), withdrawal(original.getId(), root, 4L, 5L, 6L));
        Task restored = task(instance, "1"); assertNotNull(restored); assertNotEquals(original.getId(), restored.getId());
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
        assertEquals(false, engine.getTaskService().getVariableLocal(restored.getId(), BpmTaskSmsPolicy.TASK_SEND_SMS));
        assertNull(engine.getRuntimeService().getVariable(restored.getExecutionId(), "select_node_42"));
        assertEquals("target", engine.getRuntimeService().getVariable(instance, "select_node_42"));
        assertEquals("保留其他分支数据", engine.getRuntimeService().getVariable(instance, "otherBranchValue"));
        org.flowable.engine.runtime.Execution restoredMember = engine.getRuntimeService().createExecutionQuery()
                .executionId(restored.getExecutionId()).singleResult();
        org.flowable.engine.runtime.Execution restoredRoot = engine.getRuntimeService().createExecutionQuery()
                .executionId(restoredMember.getParentId()).singleResult();
        engine.getRuntimeService().setVariableLocal(restoredRoot.getParentId(), "select_node_42", "target");
        engine.getTaskService().complete(restored.getId());
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskDefinitionKey("target").count());
    }
    @Test
    void registerSourceRestoredAsCandidateTask() throws Exception {
        String instance = start(false, false, true);
        Task original = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getTaskService().setAssignee(original.getId(), "1");
        String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        withdraw(withdrawService(), withdrawal(source, root, 4L, 5L, 6L));
        Task restored = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        assertEquals("source", restored.getTaskDefinitionKey()); assertNull(restored.getAssignee());
        assertEquals(1, engine.getTaskService().createTaskQuery().processInstanceId(instance).taskCandidateUser("1").count());
    }
    @Test
    void concurrentWithdrawalCommitsOnlyOnce() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        BpmTaskWithdrawService service = withdrawService();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("adminUserApi"); field.setAccessible(true);
        AdminUserApi users = (AdminUserApi) field.get(service);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);
        when(users.getUserList(anyCollection())).thenAnswer(call -> {
            barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
            return ((Collection<Long>) call.getArgument(0)).stream().map(this::user).collect(Collectors.toList());
        });
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) results.add(workers.submit(() -> {
                try { withdraw(service, withdrawal(source, root, 5L)); return true; }
                catch (RuntimeException expected) { return false; }
            }));
            int succeeded = 0;
            for (java.util.concurrent.Future<Boolean> result : results) if (result.get(20, java.util.concurrent.TimeUnit.SECONDS)) succeeded++;
            assertEquals(1, succeeded); assertEquals(2, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
            assertEquals(1, engine.getTaskService().getTaskComments(source, "10").size());
        } finally { workers.shutdownNow(); }
    }
    @Test
    void approvalWinningRaceRollsBackWithdrawalAndLedger() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        String approvedTaskId = task(instance, "5").getId();
        BpmTaskWithdrawService service = withdrawService();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("adminUserApi"); field.setAccessible(true);
        AdminUserApi users = (AdminUserApi) field.get(service);
        java.util.concurrent.CountDownLatch staged = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch approved = new java.util.concurrent.CountDownLatch(1);
        when(users.getUserList(anyCollection())).thenAnswer(call -> {
            staged.countDown(); assertTrue(approved.await(10, java.util.concurrent.TimeUnit.SECONDS));
            return ((Collection<Long>) call.getArgument(0)).stream().map(this::user).collect(Collectors.toList());
        });
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            java.util.concurrent.Future<Boolean> result = worker.submit(() -> {
                try { withdraw(service, withdrawal(source, root, 5L)); return true; }
                catch (RuntimeException expected) { return false; }
            });
            assertTrue(staged.await(10, java.util.concurrent.TimeUnit.SECONDS));
            engine.getTaskService().complete(approvedTaskId); approved.countDown();
            assertFalse(result.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(3, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
            assertEquals(1, engine.getRuntimeService().getVariableLocal(root, "nrOfCompletedInstances"));
            assertTrue(engine.getTaskService().getTaskComments(source, "10").isEmpty());
            Map<?, ?> ledger = (Map<?, ?>) engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source);
            assertTrue(((Collection<?>) ledger.get("target")).contains(5L));
        } finally { approved.countDown(); worker.shutdownNow(); }
    }


    @Test
    void boundaryTimersRemainForOtherMembersAndAreRemovedForCancelledOnes() throws Exception {
        String instance = start(false, false, false, false, false, false, true); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        long before = engine.getManagementService().createTimerJobQuery().processInstanceId(instance).count();
        assertTrue(before > 0);
        String preservedTask = task(instance, "4").getId();
        withdraw(withdrawService(), withdrawal(source, root, 5L));
        assertEquals(preservedTask, task(instance, "4").getId());
        assertTrue(engine.getManagementService().createTimerJobQuery().processInstanceId(instance).count() > 0);
        withdraw(withdrawService(), withdrawal(source, root, 4L, 6L));
        assertEquals(0, engine.getManagementService().createTimerJobQuery().processInstanceId(instance).count());
        assertNotNull(task(instance, "1"));
    }
    @Test
    void anyOneCompletionCannotBeWithdrawnAsAnUnfinishedMember() throws Exception {
        String instance = start(false, false, false, true); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        engine.getTaskService().complete(task(instance, "4").getId());
        BpmTaskWithdrawService service = withdrawService();
        assertTrue(service.targets(1L, source).stream().noneMatch(BpmTaskWithdrawTargetRespVO::isWithdrawable));
        assertThrows(RuntimeException.class, () -> withdraw(service, withdrawal(source, root, 5L)));
        assertNotNull(task(instance, "7"));
    }
    @Test
    void suspendedOrDisallowedProcessRejectsWithdrawal() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        BpmTaskWithdrawService service = withdrawService();
        engine.getRuntimeService().suspendProcessInstanceById(instance);
        assertThrows(RuntimeException.class, () -> service.targets(1L, source));
        engine.getRuntimeService().activateProcessInstanceById(instance);
        java.lang.reflect.Field field = service.getClass().getDeclaredField("processDefinitionService"); field.setAccessible(true);
        cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions =
                (cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService) field.get(service);
        cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO info =
                new cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO(); info.setAllowWithdrawTask(false);
        when(definitions.getProcessDefinitionInfo(anyString())).thenReturn(info);
        assertThrows(RuntimeException.class, () -> service.targets(1L, source));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }
    @Test
    void untrackedInternalMemberBlocksUnsafeRestoration() throws Exception {
        String instance = start(false, false); String source = dispatch(instance);
        String root = service().targets(1L, source).get(0).getExecutionId();
        engine.getRuntimeService().setVariableLocal(root, BpmDispatchSources.SOURCES,
                Collections.singletonMap("4", Collections.singleton(source)));
        BpmTaskWithdrawService service = withdrawService();
        assertTrue(service.targets(1L, source).stream().noneMatch(BpmTaskWithdrawTargetRespVO::isWithdrawable));
        assertThrows(RuntimeException.class, () -> withdraw(service, withdrawal(source, root, 4L)));
        assertEquals(3, engine.getTaskService().createTaskQuery().processInstanceId(instance).count());
    }


    private String legacyDispatch(String instance) {
        Task source = engine.getTaskService().createTaskQuery().processInstanceId(instance).singleResult();
        engine.getTaskService().setVariableLocal(source.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, 2);
        engine.getTaskService().complete(source.getId());
        return source.getId();
    }
    @Test
    void legacyUniqueDispatchPreviewIsReadOnlyAndWithdrawalAllowsReappend() throws Exception {
        String instance = start(false, false); String source = legacyDispatch(instance);
        BpmTaskWithdrawService service = withdrawService();
        List<BpmTaskWithdrawTargetRespVO> rows = service.targets(1L, source);
        assertEquals(3, rows.size()); assertTrue(rows.stream().allMatch(BpmTaskWithdrawTargetRespVO::isWithdrawable));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source));
        String root = rows.get(0).getExecutionId();
        assertTrue(((Map<?, ?>) engine.getRuntimeService().getVariableLocal(root, BpmDispatchSources.SOURCES)).isEmpty());
        withdraw(service, withdrawal(source, root, 5L));
        assertNull(task(instance, "5")); assertNotNull(task(instance, "4"));
        submit(service(), request(source, root, 5L)); assertNotNull(task(instance, "5"));
    }
    @Test
    void legacySequentialAllWithdrawalRestoresSource() throws Exception {
        String instance = start(true, false); String source = legacyDispatch(instance);
        BpmTaskWithdrawService service = withdrawService();
        List<BpmTaskWithdrawTargetRespVO> rows = service.targets(1L, source);
        assertEquals(3, rows.size()); assertEquals(2, rows.stream().filter(BpmTaskWithdrawTargetRespVO::isQueued).count());
        withdraw(service, withdrawal(source, rows.get(0).getExecutionId(), 4L, 5L, 6L));
        assertNotNull(task(instance, "1"));
        assertTrue(service.targets(1L, source).isEmpty());
    }
    @Test
    void legacyCompletedMemberIsPreservedAndInvalidBatchDoesNotInstallRecords() throws Exception {
        String instance = start(false, false); String source = legacyDispatch(instance);
        engine.getTaskService().complete(task(instance, "4").getId());
        engine.getManagementService().createJobQuery().processInstanceId(instance).list().forEach(job -> engine.getManagementService().executeJob(job.getId()));
        BpmTaskWithdrawService service = withdrawService();
        List<BpmTaskWithdrawTargetRespVO> rows = service.targets(1L, source);
        String root = rows.get(0).getExecutionId();
        assertThrows(RuntimeException.class, () -> withdraw(service, withdrawal(source, root, 5L, 4L)));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source));
        withdraw(service, withdrawal(source, root, 5L, 6L));
        assertNotNull(task(instance, "7")); assertNull(task(instance, "1"));
    }

    @Test
    void legacyBatchEvidenceAcceptsExactSourceAndRejectsMixedRoot() throws Exception {
        String instance = start(false, false);
        Task original = task(instance, "1");
        engine.getTaskService().setVariableLocal(original.getId(), "RETURN_BRANCH_CONTEXT", Collections.singletonMap("source", "batch-a"));
        String source = legacyDispatch(instance);
        for (Task target : engine.getTaskService().createTaskQuery().processInstanceId(instance).list()) {
            engine.getTaskService().setVariableLocal(target.getId(), "RETURN_BRANCH_CONTEXT", Collections.singletonMap("source", "batch-a"));
            engine.getTaskService().setVariableLocal(target.getId(), "RETURNABLE_TASK_PATH", Collections.singletonList("source"));
        }
        BpmTaskWithdrawService service = withdrawService();
        assertEquals(3, service.targets(1L, source).size());
        engine.getTaskService().setVariableLocal(task(instance, "5").getId(), "RETURN_BRANCH_CONTEXT", Collections.singletonMap("source", "batch-b"));
        assertThrows(RuntimeException.class, () -> service.targets(1L, source));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source));
    }
    @Test
    void legacyInstallRollsBackWithFailedAudit() throws Exception {
        String instance = start(false, false); String source = legacyDispatch(instance);
        BpmTaskWithdrawService service = withdrawService();
        String root = service.targets(1L, source).get(0).getExecutionId();
        java.lang.reflect.Field field = service.getClass().getDeclaredField("adminUserApi"); field.setAccessible(true);
        AdminUserApi users = (AdminUserApi) field.get(service);
        when(users.getUserList(anyCollection())).thenThrow(new RuntimeException("audit failed"));
        assertThrows(RuntimeException.class, () -> withdraw(service, withdrawal(source, root, 5L)));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source));
        assertTrue(((Map<?, ?>) engine.getRuntimeService().getVariableLocal(root, BpmDispatchSources.SOURCES)).isEmpty());
        assertNotNull(task(instance, "5"));
        assertEquals(3, engine.getRuntimeService().getVariableLocal(root, "nrOfInstances"));
    }
    @Test
    void legacyAsyncDispatchDoesNotInferEmptyLedger() throws Exception {
        String instance = start(false, true); String source = legacyDispatch(instance);
        assertThrows(RuntimeException.class, () -> withdrawService().targets(1L, source));
        assertNull(engine.getRuntimeService().getVariableLocal(instance, BpmDispatchSources.LEDGER_PREFIX + source));
    }


    @Test
    void legacyFinishedSiblingPreventsIncorrectSourceRestoration() throws Exception {
        String instance = start(false, false, false, false, true);
        engine.getRuntimeService().setVariable(instance, "select_node_2", "extra");
        String source = legacyDispatch(instance);
        engine.getTaskService().complete(task(instance, "7").getId()); // extra 已办，target 仍在办理
        engine.getManagementService().createJobQuery().processInstanceId(instance).list().forEach(job -> engine.getManagementService().executeJob(job.getId()));
        BpmTaskWithdrawService service = withdrawService();
        List<BpmTaskWithdrawTargetRespVO> rows = service.targets(1L, source);
        assertTrue(rows.stream().anyMatch(row -> row.getNodeId().equals("extra") && !row.isWithdrawable()));
        String root = rows.stream().filter(row -> row.getNodeId().equals("target")).findFirst().get().getExecutionId();
        withdraw(service, withdrawal(source, root, 4L, 5L, 6L));
        assertNull(task(instance, "1"));
        assertNull(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(instance).singleResult());
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
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("sms");
        model.addProcess(process);
        StartEvent start = new StartEvent(); start.setId("start"); process.addFlowElement(start);
        for (String id : optional ? Arrays.asList("source", "target", "after", "extra") : Arrays.asList("source", "target", "after")) {
            UserTask task = new UserTask(); task.setId(id); task.setName("source".equals(id) ? "来文登记" : id);
            MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
            mi.setInputDataItem("${users}");
            mi.setElementVariable("user");
            mi.setSequential("target".equals(id) && sequential);
            mi.setCompletionCondition(anyOne && "target".equals(id) ? "${nrOfCompletedInstances > 0}" : "${nrOfCompletedInstances >= nrOfInstances}");
            task.setLoopCharacteristics("source".equals(id) && !sourceMi ? null : mi);
            if (register && "source".equals(id)) {
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
        String[] ids = {"start", "source", "target", "after", "end"};
        for (int i = 0; i < ids.length - 1; i++) {
            SequenceFlow flow = new SequenceFlow(ids[i], ids[i + 1]);
            flow.setId("f" + i); process.addFlowElement(flow);
        }
        if (optional) {
            SequenceFlow flow = new SequenceFlow("source", "extra"); flow.setId("optional");
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
        return engine.getRuntimeService().startProcessInstanceByKey("sms").getId();
    }
}
