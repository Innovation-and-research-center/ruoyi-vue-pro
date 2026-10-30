package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmTaskSmsPolicy;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ExtensionElement;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 回调在方法数据权限注解退出后执行，模拟本人数据范围无法查询其他发起人的情况。 */
class BpmTaskAssignedNotificationTest {
    private final BpmTaskServiceImpl service = new BpmTaskServiceImpl();
    private final AdminUserApi users = mock(AdminUserApi.class);
    private final BpmMessageService messages = mock(BpmMessageService.class);
    private final TaskService tasks = mock(TaskService.class);
    private final Task task = mock(Task.class);

    @BeforeEach
    void prepare() {
        BpmProcessInstanceService instances = mock(BpmProcessInstanceService.class);
        BpmProcessDefinitionService definitions = mock(BpmProcessDefinitionService.class);
        BpmModelService models = mock(BpmModelService.class);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        ReflectionTestUtils.setField(service, "messageService", messages);
        ReflectionTestUtils.setField(service, "taskService", tasks);
        ReflectionTestUtils.setField(service, "processInstanceService", instances);
        ReflectionTestUtils.setField(service, "bpmProcessDefinitionService", definitions);
        ReflectionTestUtils.setField(service, "modelService", models);
        ReflectionTestUtils.setField(service, "runtimeService", mock(RuntimeService.class));
        when(task.getId()).thenReturn("notification-task");
        when(task.getProcessInstanceId()).thenReturn("test-instance");
        when(task.getProcessDefinitionId()).thenReturn("test-definition");
        when(task.getTaskDefinitionKey()).thenReturn("staff");
        when(task.getName()).thenReturn("科室人员办理");
        when(task.getAssignee()).thenReturn("2");
        when(task.getTenantId()).thenReturn("19");
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessInstanceId()).thenReturn("test-instance");
        when(instance.getProcessDefinitionId()).thenReturn("test-definition");
        when(instance.getStartUserId()).thenReturn("1");
        when(instance.getName()).thenReturn("测试项目");
        when(instances.getProcessInstance("test-instance")).thenReturn(instance);
        when(definitions.getProcessDefinitionInfo("test-definition")).thenReturn(new BpmProcessDefinitionInfoDO());
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("test");
        UserTask staff = new UserTask(); staff.setId("staff"); process.addFlowElement(staff);
        model.addProcess(process);
        when(models.getBpmnModelByDefinitionId("test-definition")).thenReturn(model);
        TenantContextHolder.setTenantId(99L);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clear() {
        TransactionSynchronizationManager.clearSynchronization();
        DataPermissionContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    void committedCallbackReadsOtherStarterWithoutDataScopeButKeepsTenant() {
        when(users.getUser(1L)).thenAnswer(call -> {
            assertEquals(19L, TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            // 不忽略本人数据权限时，真实 SQL 会把 starter=1 与当前办理人条件组合为无结果。
            return DataPermissionContextHolder.get() != null && !DataPermissionContextHolder.get().enable()
                    ? new AdminUserRespDTO().setId(1L).setNickname("测试发起人") : null;
        });
        when(tasks.getVariableLocal("notification-task", BpmTaskSmsPolicy.TASK_SEND_SMS)).thenReturn(false);
        service.processTaskAssigned(task);
        assertNull(DataPermissionContextHolder.get());
        complete(TransactionSynchronization.STATUS_COMMITTED);
        ArgumentCaptor<BpmMessageSendWhenTaskCreatedReqDTO> sent = ArgumentCaptor.forClass(BpmMessageSendWhenTaskCreatedReqDTO.class);
        verify(messages, timeout(2000)).sendMessageWhenTaskAssigned(sent.capture());
        assertEquals(1L, sent.getValue().getStartUserId());
        assertEquals(2L, sent.getValue().getAssigneeUserId());
        assertEquals("测试发起人", sent.getValue().getStartUserNickname());
        assertEquals(false, sent.getValue().getSendSms());
        assertNull(DataPermissionContextHolder.get(), "忽略范围仅限资料查询，不能泄露到后续请求");
        assertEquals(99L, TenantContextHolder.getTenantId(), "回调后恢复原租户");
    }

    @Test
    void starterDepartmentLookupAlsoIgnoresDataScopeWithinCallback() {
        when(task.getAssignee()).thenReturn("1");
        BpmModelService models = (BpmModelService) ReflectionTestUtils.getField(service, "modelService");
        BpmnModel model = models.getBpmnModelByDefinitionId("test-definition");
        ExtensionElement handler = new ExtensionElement();
        handler.setName("assignStartUserHandlerType");
        handler.setElementText("3");
        model.getFlowElement("staff").addExtensionElement(handler);
        when(users.getUser(1L)).thenAnswer(call -> {
            assertNotNull(DataPermissionContextHolder.get());
            assertFalse(DataPermissionContextHolder.get().enable());
            assertEquals(19L, TenantContextHolder.getTenantId());
            return new AdminUserRespDTO().setId(1L).setNickname("测试发起人").setDeptId(10L);
        });
        DeptApi departments = mock(DeptApi.class);
        ReflectionTestUtils.setField(service, "deptApi", departments);
        when(departments.getDept(10L)).thenAnswer(call -> {
            assertNotNull(DataPermissionContextHolder.get());
            assertFalse(DataPermissionContextHolder.get().enable());
            assertEquals(19L, TenantContextHolder.getTenantId());
            assertFalse(TenantContextHolder.isIgnore());
            // 发起人恰为负责人时，无需转给其他人，继续发送分配通知。
            return new DeptRespDTO().setId(10L).setLeaderUserId(1L);
        });
        service.processTaskAssigned(task);
        complete(TransactionSynchronization.STATUS_COMMITTED);
        verify(departments).getDept(10L);
        verify(messages, timeout(2000)).sendMessageWhenTaskAssigned(any());
        assertNull(DataPermissionContextHolder.get());
        assertEquals(99L, TenantContextHolder.getTenantId());
    }

    @Test
    void genuinelyMissingStarterSkipsNotificationWithoutNullPointer() {
        service.processTaskAssigned(task);
        assertDoesNotThrow(() -> complete(TransactionSynchronization.STATUS_COMMITTED));
        verify(users).getUser(1L);
        verifyNoInteractions(messages);
        assertNull(DataPermissionContextHolder.get());
        assertEquals(99L, TenantContextHolder.getTenantId());
    }

    @Test
    void rollbackDoesNotReadStarterOrNotify() {
        service.processTaskAssigned(task);
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(users, messages);
    }

    @Test
    void lookupFailureRestoresDataScopeAndTenant() {
        when(users.getUser(1L)).thenThrow(new IllegalStateException("模拟查询失败"));
        service.processTaskAssigned(task);
        assertThrows(IllegalStateException.class, () -> complete(TransactionSynchronization.STATUS_COMMITTED));
        assertNull(DataPermissionContextHolder.get());
        assertEquals(99L, TenantContextHolder.getTenantId());
        verifyNoInteractions(messages);
    }

    private void complete(int status) {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(status);
        }
    }
}
