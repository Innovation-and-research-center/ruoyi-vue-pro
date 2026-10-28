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
    private BpmRegisterTaskServiceImpl registers;

    @BeforeEach
    void prepare() {
        BpmTaskCandidateInvoker candidates = mock(BpmTaskCandidateInvoker.class);
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
