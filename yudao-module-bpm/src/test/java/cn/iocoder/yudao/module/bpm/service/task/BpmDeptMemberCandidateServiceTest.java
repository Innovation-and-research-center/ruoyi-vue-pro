package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.BpmUserSelectController;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmUserOptionsReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmDeptMemberCandidateServiceTest {

    private final AdminUserService users = mock(AdminUserService.class);
    private final BpmDeptMemberCandidateService service = new BpmDeptMemberCandidateService();

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(service, "userService", users);
    }

    @Test
    void includesPrimaryAndSecondaryDepartmentsAndDeduplicatesEnabledMembers() {
        when(users.getUser(1L)).thenReturn(user(1L, 10L, 0));
        // 主部门缺少关联行时也必须补入查询范围。
        when(users.getUserDeptIds(1L)).thenReturn(Collections.singleton(20L));
        AdminUserDO shared = user(3L, 99L, 0);
        when(users.getUserListByDeptIds(Collections.singleton(10L)))
                .thenReturn(Arrays.asList(user(2L, 10L, 0), shared, user(4L, 10L, 1)));
        when(users.getUserListByDeptIds(Collections.singleton(20L)))
                .thenReturn(Arrays.asList(shared, user(5L, 99L, 0)));

        List<AdminUserDO> result = service.getCandidates(1L);
        assertEquals(Arrays.asList(2L, 3L, 5L), ids(result));
        assertEquals(Arrays.asList(10L, 10L, 20L),
                result.stream().map(AdminUserDO::getDeptId).collect(Collectors.toList()));
        assertEquals(99L, shared.getDeptId(), "展示分组不得修改原用户数据");
    }

    @Test
    void absentUserOrDepartmentDoesNotFallBackToAllUsers() {
        assertTrue(service.getCandidates(1L).isEmpty());
        when(users.getUser(1L)).thenReturn(user(1L, null, 0));
        when(users.getUserDeptIds(1L)).thenReturn(Collections.emptySet());
        assertTrue(service.getCandidates(1L).isEmpty());
        verify(users, never()).getUserListByDeptIds(anyCollection());
    }

    @Test
    void validatesEachOperatorsOwnSelectionBeforeExistingMeetingSelectionsAreMerged() {
        when(users.getUser(1L)).thenReturn(user(1L, 10L, 0));
        when(users.getUser(2L)).thenReturn(user(2L, 20L, 0));
        when(users.getUserListByDeptIds(Collections.singleton(10L)))
                .thenReturn(Collections.singletonList(user(11L, 10L, 0)));
        when(users.getUserListByDeptIds(Collections.singleton(20L)))
                .thenReturn(Collections.singletonList(user(22L, 20L, 0)));
        BpmnModel model = model("dept_members");

        assertDoesNotThrow(() -> service.validateSelections(1L, model, selection(11L)));
        assertDoesNotThrow(() -> service.validateSelections(2L, model, selection(22L)));
        assertThrows(ServiceException.class, () -> service.validateSelections(1L, model, selection(22L)));
        assertThrows(ServiceException.class, () -> service.validateSelections(2L, model, selection(11L, 22L)));
        assertThrows(ServiceException.class, () -> service.validateSelections(1L, model, selection((Long) null)));
    }

    @Test
    void legacyGroupAndRoleSelectionsDoNotUseDepartmentRule() {
        assertDoesNotThrow(() -> service.validateSelections(1L, model("group"), selection(99L)));
        assertDoesNotThrow(() -> service.validateSelections(1L, model("role"), selection(99L)));
        assertDoesNotThrow(() -> service.validateSelections(1L, model("dept_members"), null));
        verifyNoInteractions(users);
    }

    @Test
    void nextNodeQuerySupportsRuleWithoutRuleValueAndReturnsSharedDepartment() {
        when(users.getUser(1L)).thenReturn(user(1L, 10L, 0));
        when(users.getUserListByDeptIds(Collections.singleton(10L)))
                .thenReturn(Collections.singletonList(user(11L, 99L, 0)));
        BpmProcessInstanceServiceImpl instances = new BpmProcessInstanceServiceImpl();
        BpmProcessDefinitionService definitions = mock(BpmProcessDefinitionService.class);
        DeptService departments = mock(DeptService.class);
        ReflectionTestUtils.setField(instances, "processDefinitionService", definitions);
        ReflectionTestUtils.setField(instances, "deptMemberCandidateService", service);
        ReflectionTestUtils.setField(instances, "userService", users);
        ReflectionTestUtils.setField(instances, "deptService", departments);
        ConfigApi config = mock(ConfigApi.class);
        cn.iocoder.yudao.module.infra.api.config.dto.ConfigRespDTO displayGroup =
                new cn.iocoder.yudao.module.infra.api.config.dto.ConfigRespDTO();
        displayGroup.setId(1L); displayGroup.setName("其他发送分组"); displayGroup.setValue("11");
        when(config.getConfigsByKeyPrefix(anyString())).thenReturn(Collections.singletonList(displayGroup));
        ReflectionTestUtils.setField(instances, "configApi", config);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition");
        when(definition.getKey()).thenReturn("lang_use");
        when(definitions.getProcessDefinition("definition")).thenReturn(definition);
        BpmnModel model = model("dept_members");
        UserTask source = new UserTask(); source.setId("source");
        ExtensionElement props = new ExtensionElement(); props.setName("properties");
        props.addChildElement(property("select_manually", "1")); source.addExtensionElement(props);
        model.getMainProcess().addFlowElement(source);
        SequenceFlow flow = new SequenceFlow("source", "staff");
        flow.setId("send"); flow.setTargetFlowElement(model.getFlowElement("staff"));
        source.setOutgoingFlows(Collections.singletonList(flow));
        when(definitions.getProcessDefinitionBpmnModel("definition")).thenReturn(model);
        DeptDO dept = new DeptDO(); dept.setId(10L); dept.setName("共同科室");
        when(departments.getDeptList(anyCollection())).thenReturn(Collections.singletonList(dept));

        List<BpmNextTaskRespVO> result = instances.getNextSelectNodes(1L,
                new BpmApprovalDetailReqVO().setProcessDefinitionId("definition").setActivityId("source"));
        assertEquals(1, result.size());
        assertEquals("dept_members", result.get(0).getExtensionProperties().get("choose_rule"));
        assertEquals(10L, result.get(0).getCandidateUsers().get(0).getId());
        assertEquals(11L, result.get(0).getCandidateUsers().get(0).getChildren().get(0).getId());
    }

    @Test
    void fallbackEndpointUsesAuthenticatedOperatorWithoutFixedDepartmentParameter() {
        when(users.getUser(1L)).thenReturn(user(1L, 10L, 0));
        when(users.getUserListByDeptIds(Collections.singleton(10L)))
                .thenReturn(Collections.singletonList(user(11L, 99L, 0)));
        BpmUserSelectController controller = new BpmUserSelectController();
        ReflectionTestUtils.setField(controller, "deptMemberCandidateService", service);
        ReflectionTestUtils.setField(controller, "deptService", mock(DeptService.class));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(1L), null));
        try {
            assertEquals(11L, controller.getUserGroupPage(new BpmUserOptionsReqVO()
                    .setChooseRule("dept_members")).getData().get(0).getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void approveEndpointRejectsForgedRecipientsThroughEitherAssigneeParameter() {
        when(users.getUser(1L)).thenReturn(user(1L, 10L, 0));
        when(users.getUserListByDeptIds(Collections.singleton(10L)))
                .thenReturn(Collections.singletonList(user(11L, 10L, 0)));
        BpmTaskServiceImpl tasks = spy(new BpmTaskServiceImpl());
        BpmModelService models = mock(BpmModelService.class);
        BpmProcessInstanceService instances = mock(BpmProcessInstanceService.class);
        ReflectionTestUtils.setField(tasks, "modelService", models);
        ReflectionTestUtils.setField(tasks, "processInstanceService", instances);
        ReflectionTestUtils.setField(tasks, "deptMemberCandidateService", service);
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getProcessDefinitionId()).thenReturn("definition");
        doReturn(task).when(tasks).validateTask(1L, "current");
        when(instances.getProcessInstance("instance")).thenReturn(mock(ProcessInstance.class));
        when(models.getBpmnModelByDefinitionId("definition")).thenReturn(model("dept_members"));
        assertThrows(ServiceException.class, () -> tasks.approveTask(1L,
                new BpmTaskApproveReqVO().setId("current").setNextNodeAssignees(selection(99L))));
        assertThrows(ServiceException.class, () -> tasks.approveTask(1L,
                new BpmTaskApproveReqVO().setId("current").setNextAssignees(selection(99L))));
    }

    static BpmnModel model(String rule) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("test");
        UserTask staff = new UserTask();
        staff.setId("staff");
        ExtensionElement properties = new ExtensionElement();
        properties.setName("properties");
        properties.addChildElement(property("choose_rule", rule)); staff.addExtensionElement(properties);
        process.addFlowElement(staff); model.addProcess(process);
        return model;
    }

    private static ExtensionElement property(String key, String text) {
        ExtensionElement property = new ExtensionElement(); property.setName("property");
        ExtensionAttribute name = new ExtensionAttribute(); name.setName("name"); name.setValue(key);
        ExtensionAttribute value = new ExtensionAttribute(); value.setName("value"); value.setValue(text);
        property.addAttribute(name); property.addAttribute(value);
        return property;
    }

    private static Map<String, List<Long>> selection(Long... ids) {
        return Collections.singletonMap("staff", Arrays.asList(ids));
    }

    private static AdminUserDO user(Long id, Long dept, Integer status) {
        return new AdminUserDO().setId(id).setDeptId(dept).setStatus(status);
    }

    private static List<Long> ids(List<AdminUserDO> users) {
        return users.stream().map(AdminUserDO::getId).collect(Collectors.toList());
    }
}
