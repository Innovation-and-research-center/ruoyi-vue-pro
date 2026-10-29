package cn.iocoder.yudao.module.bpm.service.land;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.*;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmUserGroupRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.*;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmRegisterTaskService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmTaskSortMapper;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;
import static cn.iocoder.yudao.module.bpm.service.land.LandServiceImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LandServiceTest {
    private final LandServiceImpl service = new LandServiceImpl();
    private final LandMapper lands = mock(LandMapper.class);
    private final LandAttachMapper attachments = mock(LandAttachMapper.class);
    private final FileMapper files = mock(FileMapper.class);
    private final BpmProcessInstanceApi processes = mock(BpmProcessInstanceApi.class);
    private final BpmProcessInstanceService instances = mock(BpmProcessInstanceService.class);
    private final BpmRegisterTaskService registration = mock(BpmRegisterTaskService.class);
    private final TaskService tasks = mock(TaskService.class);
    private final HistoryService history = mock(HistoryService.class);
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final TaskQuery query = mock(TaskQuery.class, RETURNS_SELF);
    private final Task task = mock(Task.class);
    private final BpmTaskSortMapper titles = mock(BpmTaskSortMapper.class);
    private final BpmUpdateOperateLogService updateLogs = mock(BpmUpdateOperateLogService.class);
    private final BpmDeleteOperateLogService deleteLogs = mock(BpmDeleteOperateLogService.class);
    private final BpmInvalidateHelper invalidation = new BpmInvalidateHelper();
    private final ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AdminUserApi users = mock(AdminUserApi.class);

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(service, "landMapper", lands);
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "taskSortMapper", titles);
        ReflectionTestUtils.setField(service, "updateLogService", updateLogs);
        ReflectionTestUtils.setField(service, "deleteLogService", deleteLogs);
        ReflectionTestUtils.setField(service, "invalidateHelper", invalidation);
        ReflectionTestUtils.setField(invalidation, "processInstanceService", instances);
        ReflectionTestUtils.setField(invalidation, "taskSortMapper", titles);
        when(runtime.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.processInstanceId(anyString())).thenReturn(processQuery);
        ReflectionTestUtils.setField(service, "landAttachMapper", attachments);
        ReflectionTestUtils.setField(service, "fileMapper", files);
        ReflectionTestUtils.setField(service, "processInstanceApi", processes);
        ReflectionTestUtils.setField(service, "processInstanceService", instances);
        ReflectionTestUtils.setField(service, "registerTaskService", registration);
        ReflectionTestUtils.setField(service, "taskService", tasks);
        ReflectionTestUtils.setField(service, "historyService", history);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "userApi", users);
        ReflectionTestUtils.setField(service, "deptApi", mock(DeptApi.class));
        when(tasks.createTaskQuery()).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(task));
        when(task.getId()).thenReturn("register-task");
        when(task.getAssignee()).thenReturn("7");
        when(lands.selectForUpdate(10L)).thenReturn(land());
        when(lands.selectById(10L)).thenReturn(land());
        when(attachments.selectByLandId(anyLong())).thenReturn(Collections.emptyList());
    }

    @Test
    void saveCreatesBusinessProcessAndRegistrationWithServerIdentity() {
        when(users.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L).setNickname("登记员"));
        doAnswer(invocation -> { ((LandDO) invocation.getArgument(0)).setId(10L); return 1; })
                .when(lands).insert(any(LandDO.class));
        when(processes.createProcessInstance(eq(7L), any())).thenReturn("process-10");
        when(registration.claim(7L, "process-10")).thenReturn(1);

        LandSaveRespVO result = service.save(7L, request().setId(null));
        assertEquals(10L, result.getId());
        assertEquals("process-10", result.getProcessInstanceId());
        assertEquals("register-task", result.getTaskId());
        ArgumentCaptor<LandDO> business = ArgumentCaptor.forClass(LandDO.class);
        verify(lands).insert(business.capture());
        assertEquals(7L, business.getValue().getUserId());
        assertEquals("登记中心", business.getValue().getStartUnit());
        ArgumentCaptor<BpmProcessInstanceCreateReqDTO> process = ArgumentCaptor.forClass(BpmProcessInstanceCreateReqDTO.class);
        verify(processes).createProcessInstance(eq(7L), process.capture());
        assertEquals(PROCESS_KEY, process.getValue().getProcessDefinitionKey());
        assertEquals("10", process.getValue().getBusinessKey());
        assertEquals("测试项目", process.getValue().getVariables().get(PROCESS_CUSTOM_NAME));
        verify(runtime).setProcessInstanceName("process-10", "测试项目");
    }

    @Test
    void missingRegistrationFailsSaveInsteadOfReturningFalseSuccess() {
        when(users.getUser(7L)).thenReturn(new AdminUserRespDTO().setId(7L));
        doAnswer(invocation -> { ((LandDO) invocation.getArgument(0)).setId(10L); return 1; })
                .when(lands).insert(any(LandDO.class));
        when(processes.createProcessInstance(eq(7L), any())).thenReturn("process-10");
        assertEquals(LAND_REGISTER_NOT_ACTIVE.getCode(), assertThrows(ServiceException.class,
                () -> service.save(7L, request().setId(null))).getCode());
    }

    @Test
    void saveWithExistingIdUpdatesWithoutStartingAnotherProcess() {
        assertEquals(10L, service.save(7L, request()).getId());
        verifyNoInteractions(processes, registration);
        verify(lands).selectForUpdate(10L);
        verify(runtime).setProcessInstanceName("process-10", "测试项目");
    }

    @Test
    void updateSynchronizesTrimmedProjectNameAndTitleVariablesWithoutChangingProcessIdentity() {
        service.update(7L, request().setProjectName("  修改后的项目  "));
        verify(runtime).setProcessInstanceName("process-10", "修改后的项目");
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(runtime).setVariables(eq("process-10"), variables.capture());
        assertEquals("修改后的项目", variables.getValue().get(PROCESS_CUSTOM_NAME));
        assertEquals("修改后的项目", variables.getValue().get("projectName"));
        verifyNoInteractions(processes, registration);
    }

    @Test
    void submitUsesAllowedDeduplicatedMainRecipientsAndIgnoresInternalVariables() {
        candidates(11L, 12L);
        when(registration.completeOnSubmit(eq(7L), eq("process-10"), anyMap(), eq(false))).thenReturn(1);
        service.submit(7L, request().setSendSms(false).setNextNodeAssignees(
                Collections.singletonMap(MAIN_NODE, Arrays.asList(11L, 11L, 12L)))
                .setProcessVariablesStr("{\"PROCESS_START_USER_ID\":999,\"nrOfInstances\":0}"));
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(registration).completeOnSubmit(eq(7L), eq("process-10"), variables.capture(), eq(false));
        assertEquals(Collections.singletonMap(MAIN_NODE, Arrays.asList(11L, 12L)),
                variables.getValue().get(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES));
        assertFalse(variables.getValue().containsKey(PROCESS_INSTANCE_VARIABLE_START_USER_ID));
        assertFalse(variables.getValue().containsKey("nrOfInstances"));
        assertEquals("测试项目", variables.getValue().get(PROCESS_CUSTOM_NAME));
        verify(runtime).setProcessInstanceName("process-10", "测试项目");
        verify(lands).selectForUpdate(10L);
    }

    @Test
    void forgedOrMissingMainRecipientsAreRejectedBeforeMutations() {
        candidates(11L);
        for (Map<String, List<Long>> selected : Arrays.asList(Collections.<String, List<Long>>emptyMap(),
                Collections.singletonMap(MAIN_NODE, Collections.singletonList(99L)),
                Collections.singletonMap("other-node", Collections.singletonList(11L)))) {
            assertEquals(LAND_ASSIGNEES_INVALID.getCode(), assertThrows(ServiceException.class,
                    () -> service.submit(7L, request().setNextNodeAssignees(selected))).getCode());
        }
        verifyNoInteractions(registration, attachments, files);
    }

    @Test
    void secondSubmissionCannotSendAfterRegistrationHasCompleted() {
        candidates(11L);
        when(registration.completeOnSubmit(eq(7L), eq("process-10"), anyMap(), isNull())).thenReturn(1);
        service.submit(7L, request().setNextNodeAssignees(Collections.singletonMap(MAIN_NODE, Collections.singletonList(11L))));
        when(query.list()).thenReturn(Collections.emptyList());
        assertEquals(LAND_REGISTER_NOT_ACTIVE.getCode(), assertThrows(ServiceException.class,
                () -> service.submit(7L, request())).getCode());
        verify(registration, times(1)).completeOnSubmit(eq(7L), eq("process-10"), anyMap(), isNull());
    }

    @Test
    void editAndSubmitRequireCurrentAssigneeEvenIfCallerIsOriginalOwner() {
        when(task.getAssignee()).thenReturn("8");
        assertThrows(ServiceException.class, () -> service.update(7L, request()));
        assertThrows(ServiceException.class, () -> service.submit(7L, request()));
        verifyNoInteractions(attachments, files, registration);
    }

    @Test
    void attachmentsUseCanonicalMetadataAndOnlyNewOwnedOrPreviouslyAssociatedFiles() {
        FileDO file = new FileDO().setId(21L).setName("材料.pdf").setPath("storage/material.pdf").setUrl("https://files.example.test/material.pdf");
        file.setCreator("7");
        when(files.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(file));
        service.update(7L, request().setFileList(Collections.singletonList(new LandAttachSaveReqVO().setAttachFileId(21L))));
        ArgumentCaptor<LandAttachDO> attachment = ArgumentCaptor.forClass(LandAttachDO.class);
        verify(attachments).insert(attachment.capture());
        assertEquals(10L, attachment.getValue().getLandId());
        assertEquals("材料.pdf", attachment.getValue().getFileName());
        assertEquals("storage/material.pdf", attachment.getValue().getFilePath());
        assertEquals(0, attachment.getValue().getSort());

        file.setCreator("8");
        assertEquals(LAND_ATTACHMENT_INVALID.getCode(), assertThrows(ServiceException.class,
                () -> service.update(7L, request().setFileList(Collections.singletonList(new LandAttachSaveReqVO().setAttachFileId(21L))))).getCode());
        when(attachments.selectByLandId(10L)).thenReturn(Collections.singletonList(new LandAttachDO().setId(31L).setLandId(10L).setAttachFileId(21L)));
        assertDoesNotThrow(() -> service.update(7L, request().setFileList(Collections.singletonList(new LandAttachSaveReqVO().setAttachFileId(21L)))));
    }

    @Test
    void missingFileFailsBeforeDeletingOldAssociations() {
        when(attachments.selectByLandId(10L)).thenReturn(Collections.singletonList(new LandAttachDO().setId(31L).setAttachFileId(21L)));
        assertThrows(ServiceException.class, () -> service.update(7L, request().setFileList(
                Collections.singletonList(new LandAttachSaveReqVO().setAttachFileId(22L)))));
        verify(attachments, never()).deleteBatchIds(anyCollection());
    }

    @Test
    void readAllowsOwnerAndParticipantButRejectsUnrelatedUser() {
        assertEquals(10L, service.get(7L, 10L).getId());
        HistoricTaskInstanceQuery historyQuery = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricTaskInstanceQuery()).thenReturn(historyQuery);
        assertEquals(LAND_ACCESS_DENIED.getCode(), assertThrows(ServiceException.class,
                () -> service.get(8L, 10L)).getCode());
        when(historyQuery.count()).thenReturn(1L);
        assertEquals(10L, service.get(8L, 10L).getId());
    }

    @Test
    void nonexistentBusinessAndMissingIdAreRejected() {
        assertEquals(LAND_NOT_EXISTS.getCode(), assertThrows(ServiceException.class,
                () -> service.get(7L, 99L)).getCode());
        assertThrows(ServiceException.class, () -> service.update(7L, request().setId(null)));
    }

    @Test
    void managementPageRequiresPermissionEvenForBusinessOwner() {
        assertEquals(LAND_ACCESS_DENIED.getCode(), assertThrows(ServiceException.class,
                () -> service.getPage(7L, new LandPageReqVO())).getCode());
        assertEquals(LAND_ACCESS_DENIED.getCode(), assertThrows(ServiceException.class,
                () -> service.getPage(null, new LandPageReqVO())).getCode());
        verify(lands, never()).selectPage(any(LandPageReqVO.class));
    }

    @Test
    void managementQueryReturnsPageAndAllowsDetailButDoesNotGrantEditing() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:query")).thenReturn(true);
        LandPageReqVO request = new LandPageReqVO().setProjectName("测试").setStatus(1);
        when(lands.selectPage(request)).thenReturn(new PageResult<>(Collections.singletonList(land()), 15L));
        PageResult<LandPageRespVO> result = service.getPage(8L, request);
        assertEquals(15L, result.getTotal());
        assertEquals("原项目", result.getList().get(0).getProjectName());
        assertEquals("process-10", result.getList().get(0).getProcessInstanceId());
        HistoricTaskInstanceQuery historyQuery = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricTaskInstanceQuery()).thenReturn(historyQuery);
        assertEquals(10L, service.get(8L, 10L).getId());
        assertEquals(LAND_REGISTER_NOT_ACTIVE.getCode(), assertThrows(ServiceException.class,
                () -> service.update(8L, request())).getCode());
        verify(lands, never()).updateById(any(LandDO.class));
    }

    @Test
    void queryPermissionDoesNotGrantManagementUpdateOrDelete() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:query")).thenReturn(true);
        assertEquals(LAND_ACCESS_DENIED.getCode(), assertThrows(ServiceException.class,
                () -> service.managementUpdate(8L, request())).getCode());
        assertEquals(LAND_ACCESS_DENIED.getCode(), assertThrows(ServiceException.class,
                () -> service.delete(8L, 10L, "测试删除")).getCode());
        verify(lands, never()).selectForUpdate(any());
        verifyNoInteractions(updateLogs, deleteLogs);
    }

    @Test
    void managementUpdateWorksOutsideRegistrationAndPreservesIdentityAndState() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:update")).thenReturn(true);
        LandDO old = land().setStatus(1).setDeptName("原科室");
        when(lands.selectForUpdate(10L)).thenReturn(old);
        when(processQuery.singleResult()).thenReturn(mock(ProcessInstance.class));
        service.managementUpdate(8L, request().setProjectName("  管理改名  "));
        verify(tasks, never()).createTaskQuery();
        verify(titles).updateRuntimeProcessInstanceName("process-10", "管理改名");
        verify(titles).updateHistoricProcessInstanceName("process-10", "管理改名");
        verify(runtime).setVariables(eq("process-10"), argThat(values -> "管理改名".equals(values.get(PROCESS_CUSTOM_NAME))));
        ArgumentCaptor<LandDO> before = ArgumentCaptor.forClass(LandDO.class);
        ArgumentCaptor<LandDO> after = ArgumentCaptor.forClass(LandDO.class);
        verify(updateLogs).recordUpdate(eq("土地用途认定"), eq(10L), before.capture(), after.capture(), any(), any());
        assertEquals("原项目", before.getValue().getProjectName());
        assertEquals("管理改名", after.getValue().getProjectName());
        assertEquals(7L, after.getValue().getUserId());
        assertEquals(1, after.getValue().getStatus());
    }

    @Test
    void finishedProcessCanBeModifiedWithoutAccessingRuntimeVariables() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:update")).thenReturn(true);
        when(lands.selectForUpdate(10L)).thenReturn(land().setStatus(2));
        service.managementUpdate(8L, request());
        verify(titles).updateHistoricProcessInstanceName("process-10", "测试项目");
        verify(runtime, never()).setVariables(anyString(), anyMap());
        verify(runtime, never()).setProcessInstanceName(anyString(), anyString());
    }

    @Test
    void deletingRunningBusinessInvalidatesCancelsAndRecordsReasonWithoutRemovingFiles() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:delete")).thenReturn(true);
        when(lands.selectForUpdate(10L)).thenReturn(land().setStatus(1));
        service.delete(8L, 10L, "  测试删除  ");
        verify(lands).updateById(argThat((LandDO value) -> value.getId() == 10L && value.getStatus() == 5));
        verify(instances).cancelProcessInstanceByAdmin(eq(8L), argThat(request ->
                "process-10".equals(request.getId()) && "测试删除".equals(request.getReason())));
        verify(titles).updateHistoricProcessLongVariable("process-10", PROCESS_INSTANCE_VARIABLE_STATUS, 5);
        verify(deleteLogs).recordDelete("土地用途认定", 10L, "测试删除");
        verifyNoInteractions(attachments, files);
    }

    @Test
    void finishedBusinessDeletionDoesNotCancelAndRepeatedDeletionIsRejected() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:delete")).thenReturn(true);
        when(lands.selectForUpdate(10L)).thenReturn(land().setStatus(2));
        service.delete(8L, 10L, "办结后删除");
        verify(instances, never()).cancelProcessInstanceByAdmin(any(), any());
        when(lands.selectForUpdate(10L)).thenReturn(land().setStatus(5));
        assertEquals(LAND_CANNOT_DELETE.getCode(), assertThrows(ServiceException.class,
                () -> service.delete(8L, 10L, "重复删除")).getCode());
        assertEquals(LAND_DELETE_REASON_REQUIRED.getCode(), assertThrows(ServiceException.class,
                () -> service.delete(8L, 10L, " ")).getCode());
        verify(deleteLogs, times(1)).recordDelete(anyString(), anyLong(), anyString());
    }

    @Test
    void failedCancellationDoesNotRecordDeletionSuccess() {
        when(permissions.hasAnyPermissions(8L, "bpm:land:delete")).thenReturn(true);
        when(lands.selectForUpdate(10L)).thenReturn(land().setStatus(1));
        doThrow(new IllegalStateException("取消失败")).when(instances).cancelProcessInstanceByAdmin(any(), any());
        assertThrows(IllegalStateException.class, () -> service.delete(8L, 10L, "删除原因"));
        verifyNoInteractions(deleteLogs);
    }

    private void candidates(Long... ids) {
        List<UserSimpleRespVO> users = new ArrayList<>();
        for (Long id : ids) users.add(new UserSimpleRespVO().setId(id));
        BpmUserGroupRespVO group = new BpmUserGroupRespVO(); group.setChildren(users);
        BpmNextTaskRespVO node = new BpmNextTaskRespVO(); node.setTaskDefKey(MAIN_NODE);
        node.setCandidateUsers(Collections.singletonList(group));
        when(instances.getNextSelectNodes(eq(7L), any())).thenReturn(Collections.singletonList(node));
    }

    private LandDO land() {
        return new LandDO().setId(10L).setUserId(7L).setProcessInstanceId("process-10").setProjectName("原项目");
    }

    private LandSaveReqVO request() {
        return new LandSaveReqVO().setId(10L).setProjectName("测试项目").setLocation("测试坐落");
    }
}
