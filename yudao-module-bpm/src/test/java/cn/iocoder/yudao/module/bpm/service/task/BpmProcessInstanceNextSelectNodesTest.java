package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.api.config.dto.ConfigRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.flowable.bpmn.model.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BpmProcessInstanceNextSelectNodesTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmProcessInstanceServiceImpl service;
    @Mock
    private BpmTaskService taskService;
    @Mock
    private BpmProcessDefinitionService processDefinitionService;
    @Mock
    private BpmUserGroupService userGroupService;
    @Mock
    private AdminUserService userService;
    @Mock
    private DeptService deptService;
    @Mock
    private ConfigApi configApi;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RuntimeService runtimeService;
    @Mock
    private HistoryService historyService;

    @Test
    void groupMemberKeepsAllRoutesEvenWhenTaskWasDispatchedByInternalLoop() {
        prepare(10L, true);

        List<BpmNextTaskRespVO> nodes = getNodes(10L);

        assertEquals(new HashSet<>(Arrays.asList("current_internal_loop", "current_specified", "next", "end")),
                keys(nodes));
        assertTrue(candidateIds(find(nodes, "current_internal_loop")).contains(40L));
    }

    @Test
    void internalLoopNonMemberOnlySeesDepartmentLoopAndCompletionWithRealCondition() {
        prepare(20L, true);

        List<BpmNextTaskRespVO> nodes = getNodes(20L);

        assertEquals(2, nodes.size());
        assertEquals(new HashSet<>(Arrays.asList("current_internal_loop", "end")), keys(nodes));
        BpmNextTaskRespVO loop = find(nodes, "current_internal_loop");
        assertEquals(new HashSet<>(Arrays.asList(10L, 20L)), candidateIds(loop));
        assertFalse(loop.getExtensionProperties().containsKey("choose_rule"));
        assertFalse(loop.getExtensionProperties().containsKey("rule_value"));
        BpmNextTaskRespVO end = find(nodes, "end");
        assertEquals("办理完成", end.getTaskName());
        assertEquals("route", end.getConditionExpression().key);
        assertEquals("finish", end.getConditionExpression().value);
    }

    @Test
    void ordinaryTaskKeepsExistingRoutes() {
        prepare(20L, false);

        assertEquals(new HashSet<>(Arrays.asList("current_internal_loop", "current_specified", "next", "end")),
                keys(getNodes(20L)));
    }

    @Test
    void internalLoopShowsOwnDepartmentFirstEvenWithDepartmentSortAndConfiguredGroups() {
        prepare(10L, true);
        when(deptService.getDeptList(anyCollection())).thenReturn(Arrays.asList(
                new DeptDO().setId(1L).setName("本部门").setSort(100),
                new DeptDO().setId(2L).setName("其他部门").setSort(1)));
        ConfigRespDTO config = new ConfigRespDTO();
        config.setId(1L);
        config.setName("发送分组");
        config.setValue("40,20");
        when(configApi.getConfigsByKeyPrefix(anyString())).thenReturn(Collections.singletonList(config));

        List<BpmNextTaskRespVO> nodes = getNodes(10L);
        BpmNextTaskRespVO loop = find(nodes, "current_internal_loop");

        assertEquals(1L, loop.getCandidateUsers().get(0).getId());
        assertEquals(Arrays.asList(10L, 20L), loop.getCandidateUsers().get(0).getChildren().stream()
                .map(user -> user.getId()).collect(Collectors.toList()));
        assertEquals("发送分组", loop.getCandidateUsers().get(1).getName());
        assertEquals(40L, loop.getCandidateUsers().get(1).getChildren().get(0).getId());
        assertEquals(Arrays.asList(10L, 20L, 40L), loop.getCandidateUserOrderIds());
        assertEquals("发送分组", find(nodes, "current_specified").getCandidateUsers().get(0).getName());
    }

    @Test
    void configuredGroupKeepsRoutesCandidateSetAndOriginalDefaultOrder() {
        prepare(20L, false);
        List<BpmNextTaskRespVO> original = getNodes(20L);
        ConfigRespDTO config = new ConfigRespDTO();
        config.setId(100L);
        config.setName("局主要领导");
        config.setValue("40,999");
        when(configApi.getConfigsByKeyPrefix("bpm.send-user-group.")).thenReturn(Collections.singletonList(config));

        List<BpmNextTaskRespVO> grouped = getNodes(20L);
        assertEquals(keys(original), keys(grouped));
        for (BpmNextTaskRespVO node : grouped) {
            BpmNextTaskRespVO before = find(original, node.getTaskDefKey());
            assertEquals(before.getCandidateUserOrderIds(), node.getCandidateUserOrderIds());
            if (node.getCandidateUsers() != null) assertEquals(candidateIds(before), candidateIds(node));
            assertEquals(before.getAssignedUserIds(), node.getAssignedUserIds());
            assertEquals(before.getExtensionProperties(), node.getExtensionProperties());
        }
        assertEquals("本部门", find(grouped, "current_internal_loop").getCandidateUsers().get(0).getName());
        assertEquals("局主要领导", find(grouped, "current_internal_loop").getCandidateUsers().get(1).getName());
    }

    @Test
    void receivedUsersExcludeReturnedAndCancelledTasksAndTheirCandidates() {
        prepare(20L, false);
        HistoricTaskInstance returned = receivedTask("returned", 10L, BpmTaskStatusEnum.RETURN);
        HistoricTaskInstance cancelled = receivedTask("cancelled", 20L, BpmTaskStatusEnum.CANCEL);
        HistoricTaskInstance running = receivedTask("running", 30L, BpmTaskStatusEnum.RUNNING);
        HistoricTaskInstance approved = receivedTask("approved", 40L, BpmTaskStatusEnum.APPROVE);
        HistoricTaskInstance unclaimed = receivedTask("unclaimed", null, BpmTaskStatusEnum.RUNNING);
        when(historyService.createHistoricTaskInstanceQuery().list())
                .thenReturn(Arrays.asList(returned, cancelled, running, approved, unclaimed));
        HistoricIdentityLink candidate = mock(HistoricIdentityLink.class);
        when(candidate.getUserId()).thenReturn("50");
        when(historyService.getHistoricIdentityLinksForTask(anyString()))
                .thenAnswer(invocation -> "unclaimed".equals(invocation.getArgument(0))
                        ? Collections.singletonList(candidate) : Collections.emptyList());
        doReturn(Arrays.asList(user(30L, 1L, 0), user(40L, 2L, 0), user(50L, 2L, 0)))
                .when(userService).getUserList(new HashSet<>(Arrays.asList(30L, 40L, 50L)));

        BpmNextTaskRespVO next = find(getNodes(20L), "next");

        assertEquals(new HashSet<>(Arrays.asList(30L, 40L, 50L)), new HashSet<>(next.getAssignedUserIds()));
        assertEquals(3, next.getAssignedUsers().size());
        verify(historyService.createHistoricTaskInstanceQuery()).includeTaskLocalVariables();
        verify(historyService, never()).getHistoricIdentityLinksForTask("returned");
        verify(historyService, never()).getHistoricIdentityLinksForTask("cancelled");
        verify(historyService, never()).createHistoricVariableInstanceQuery();
    }

    @Test
    void cancelledUserIsShownOnlyWhenTheyReceiveANewValidTask() {
        prepare(20L, false);
        HistoricTaskInstance cancelled = receivedTask("cancelled", 30L, BpmTaskStatusEnum.CANCEL);
        when(historyService.createHistoricTaskInstanceQuery().list())
                .thenReturn(Collections.singletonList(cancelled));
        BpmApprovalDetailReqVO request = new BpmApprovalDetailReqVO()
                .setProcessDefinitionId("definition").setTaskId("task")
                .setProcessVariables(Collections.singletonMap(
                        BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                        Collections.singletonMap("next", Collections.singletonList(30L))));

        List<Long> assignedIds = find(service.getNextSelectNodes(20L, request), "next").getAssignedUserIds();
        assertTrue(assignedIds == null || assignedIds.isEmpty());
        verify(historyService, never()).createHistoricVariableInstanceQuery();

        HistoricTaskInstance resent = receivedTask("resent", 30L, BpmTaskStatusEnum.RUNNING);
        when(historyService.createHistoricTaskInstanceQuery().list()).thenReturn(Arrays.asList(cancelled, resent));
        doReturn(Collections.singletonList(user(30L, 1L, 0)))
                .when(userService).getUserList(Collections.singleton(30L));
        assertEquals(Collections.singletonList(30L), find(getNodes(20L), "next").getAssignedUserIds());
    }

    @Test
    void submissionCandidatesForNonMemberAreOnlyEnabledOwnDepartmentUsers() {
        prepare(20L, true);
        getNodes(20L);
        assertTrue(service.isRestrictedInternalOperator(20L, "definition", "current", true));
        assertEquals(new HashSet<>(Arrays.asList(10L, 20L)),
                service.getInternalLoopCandidateUserIds(20L, "definition", "current", true));
        assertFalse(service.getInternalLoopCandidateUserIds(20L, "definition", "current", true).contains(40L));
    }

    @Test
    void submissionCandidatesForGroupMemberKeepGroupAndOwnDepartment() {
        prepare(10L, true);
        getNodes(10L);
        assertFalse(service.isRestrictedInternalOperator(10L, "definition", "current", true));
        assertEquals(new HashSet<>(Arrays.asList(10L, 20L, 40L)),
                service.getInternalLoopCandidateUserIds(10L, "definition", "current", true));
    }

    @Test
    void nonMemberWithoutDepartmentHasNoCandidatesInQueryOrSubmission() {
        prepare(20L, true);
        getNodes(20L);
        when(userService.getUser(20L)).thenReturn(user(20L, null, 0));
        assertTrue(service.getInternalLoopCandidateUserIds(20L, "definition", "current", true).isEmpty());
        assertTrue(candidateIds(find(getNodes(20L), "current_internal_loop")).isEmpty());
    }

    @Test
    void subsequentRecipientUsesTheirOwnDepartmentInsteadOfOriginalSenderDepartment() {
        prepare(20L, true);
        getNodes(20L);
        when(userService.getUser(20L)).thenReturn(user(20L, 3L, 0));
        when(userService.getUserListByDeptIds(Collections.singleton(3L)))
                .thenReturn(Collections.singletonList(user(50L, 3L, 0)));
        assertEquals(Collections.singleton(50L),
                service.getInternalLoopCandidateUserIds(20L, "definition", "current", true));
    }

    @Test
    void appendBatchSharesModelConfigurationGroupsAndUserLookup() {
        prepare(20L, false);
        getNodes(20L);
        BpmnModel model = processDefinitionService.getProcessDefinitionBpmnModel("definition");
        for (String nodeId : Arrays.asList("current", "next")) {
            FlowElement node = model.getFlowElement(nodeId);
            ExtensionElement strategy = new ExtensionElement(); strategy.setName("candidateStrategy"); strategy.setElementText("55");
            node.addExtensionElement(strategy);
            if ("next".equals(nodeId)) { property(node, "choose_rule", "group"); property(node, "rule_value", "7"); }
        }
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessDefinitionId()).thenReturn("definition");
        when(instance.getProcessDefinitionKey()).thenReturn("process");
        when(runtimeService.createProcessInstanceQuery().processInstanceId("instance").singleResult()).thenReturn(instance);
        clearInvocations(processDefinitionService, configApi, userGroupService, userService);
        Map<String, String> targets = new LinkedHashMap<>(); targets.put("new:current", "current"); targets.put("new:next", "next");
        Map<String, Set<Long>> candidates = service.getAppendCandidateUserIdsBatch(20L, "instance", targets);
        assertEquals(new HashSet<>(Arrays.asList(10L, 40L)), candidates.get("new:current"));
        assertEquals(candidates.get("new:current"), candidates.get("new:next"));
        candidates.get("new:current").clear();
        assertEquals(2, candidates.get("new:next").size());
        verify(processDefinitionService, times(1)).getProcessDefinitionBpmnModel("definition");
        verify(configApi, times(1)).getConfigValueByKey(anyString());
        verify(userGroupService, times(1)).getUserGroupList(Collections.singleton(7L));
        verify(userService, times(1)).getUserList(new HashSet<>(Arrays.asList(10L, 40L)));
    }

    private HistoricTaskInstance receivedTask(String id, Long userId, BpmTaskStatusEnum status) {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        if (status != BpmTaskStatusEnum.CANCEL && status != BpmTaskStatusEnum.RETURN) {
            when(task.getId()).thenReturn(id);
            when(task.getTaskDefinitionKey()).thenReturn("next");
            when(task.getAssignee()).thenReturn(userId != null ? userId.toString() : null);
        }
        when(task.getTaskLocalVariables()).thenReturn(Collections.singletonMap(
                BpmnVariableConstants.TASK_VARIABLE_STATUS, status.getStatus()));
        return task;
    }

    private void prepare(Long loginUserId, boolean internalLoop) {
        UserTask current = new UserTask();
        current.setId("current");
        property(current, "select_manually", "1");
        property(current, "loop_flag", "1");
        property(current, "specified_flag", "1");
        property(current, "choose_rule", "group");
        property(current, "rule_value", "7");
        UserTask next = new UserTask();
        next.setId("next");
        EndEvent end = new EndEvent();
        end.setId("endEvent");
        connect(current, next, "${variables:get(route) == 'next'}");
        connect(current, end, "${variables:get(route) == 'finish'}");
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("process");
        process.addFlowElement(current);
        process.addFlowElement(next);
        process.addFlowElement(end);
        BpmnModel model = new BpmnModel();
        model.addProcess(process);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(definition.getId()).thenReturn("definition");
        when(definition.getKey()).thenReturn("process");
        when(processDefinitionService.getProcessDefinition("definition")).thenReturn(definition);
        when(processDefinitionService.getProcessDefinitionBpmnModel("definition")).thenReturn(model);
        Task task = mock(Task.class);
        when(task.getTaskDefinitionKey()).thenReturn("current");
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getTaskLocalVariables()).thenReturn(internalLoop
                ? Collections.singletonMap("internal_source_task_id", "sourceTask") : Collections.emptyMap());
        when(taskService.validateTask(loginUserId, "task")).thenReturn(task);
        when(runtimeService.createProcessInstanceQuery().includeProcessVariables()
                .processInstanceId("instance").singleResult()).thenReturn(mock(ProcessInstance.class));
        HistoricTaskInstanceQuery historicTasks = mock(HistoricTaskInstanceQuery.class, Answers.RETURNS_SELF);
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(historicTasks);
        when(historicTasks.list()).thenReturn(Collections.emptyList());
        Set<Long> groupIds = Collections.singleton(7L);
        Set<Long> memberIds = new HashSet<>(Arrays.asList(10L, 40L));
        when(userGroupService.getUserGroupList(groupIds)).thenReturn(Collections.singletonList(
                new BpmUserGroupDO().setId(7L).setUserIds(memberIds)));
        when(userService.getUserList(memberIds)).thenReturn(Arrays.asList(user(10L, 1L, 0), user(40L, 2L, 0)));
        when(userService.getUser(loginUserId)).thenReturn(user(loginUserId, 1L, 0));
        when(userService.getUserListByDeptIds(Collections.singleton(1L))).thenReturn(Arrays.asList(
                user(10L, 1L, 0), user(20L, 1L, 0), user(30L, 1L, 1)));
        when(deptService.getDeptList(anyCollection())).thenReturn(Arrays.asList(
                new DeptDO().setId(1L).setName("本部门"), new DeptDO().setId(2L).setName("其他部门")));
    }

    private List<BpmNextTaskRespVO> getNodes(Long userId) {
        return service.getNextSelectNodes(userId, new BpmApprovalDetailReqVO()
                .setProcessDefinitionId("definition").setTaskId("task"));
    }

    private static AdminUserDO user(Long id, Long deptId, Integer status) {
        return new AdminUserDO().setId(id).setDeptId(deptId).setStatus(status);
    }

    private static void property(FlowElement element, String name, String value) {
        ExtensionElement property = new ExtensionElement();
        property.setName("property");
        property.addAttribute(new ExtensionAttribute("name", name));
        property.addAttribute(new ExtensionAttribute("value", value));
        element.addExtensionElement(property);
    }

    private static void connect(FlowNode source, FlowNode target, String condition) {
        SequenceFlow flow = new SequenceFlow(source.getId(), target.getId());
        flow.setId(source.getId() + "_" + target.getId());
        flow.setSourceFlowElement(source);
        flow.setTargetFlowElement(target);
        flow.setConditionExpression(condition);
        source.getOutgoingFlows().add(flow);
    }

    private static Set<String> keys(List<BpmNextTaskRespVO> nodes) {
        return nodes.stream().map(BpmNextTaskRespVO::getTaskDefKey).collect(Collectors.toSet());
    }

    private static BpmNextTaskRespVO find(List<BpmNextTaskRespVO> nodes, String key) {
        return nodes.stream().filter(node -> key.equals(node.getTaskDefKey())).findFirst().orElseThrow(AssertionError::new);
    }

    private static Set<Long> candidateIds(BpmNextTaskRespVO node) {
        return node.getCandidateUsers().stream().flatMap(dept -> dept.getChildren().stream())
                .map(user -> user.getId()).collect(Collectors.toSet());
    }
}
