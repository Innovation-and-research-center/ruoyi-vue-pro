package cn.iocoder.yudao.module.bpm.service.leave;

import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.LeaveController;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.LeaveDetailRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.LeaveSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmRegisterTaskService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LeaveDepartmentSnapshotTest {
    @Test
    void firstSaveCapturesOnlyPrimaryDepartmentEvenWithMultipleMemberships() {
        LeaveServiceImpl service = new LeaveServiceImpl();
        LeaveMapper mapper = mock(LeaveMapper.class);
        AdminUserApi users = mock(AdminUserApi.class);
        DeptApi departments = mock(DeptApi.class);
        AdminUserService userService = mock(AdminUserService.class);
        PermissionService permissions = mock(PermissionService.class);
        RoleService roles = mock(RoleService.class);
        ReflectionTestUtils.setField(service, "leaveMapper", mapper);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        ReflectionTestUtils.setField(service, "deptApi", departments);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "permissionService", permissions);
        ReflectionTestUtils.setField(service, "roleService", roles);
        ReflectionTestUtils.setField(service, "processInstanceApi", mock(BpmProcessInstanceApi.class));
        ReflectionTestUtils.setField(service, "bpmRegisterTaskService", mock(BpmRegisterTaskService.class));
        when(users.getUser(20L)).thenReturn(new AdminUserRespDTO().setDeptId(30L));
        when(userService.getUserDeptIds(20L)).thenReturn(new HashSet<>(Arrays.asList(40L, 50L)));
        when(departments.getDept(30L)).thenReturn(new DeptRespDTO().setId(30L).setName("主部门"));
        when(userService.getUser(20L)).thenReturn(new AdminUserDO().setNickname("测试申请人"));
        when(permissions.getUserRoleIdListByUserId(20L)).thenReturn(Collections.emptySet());
        when(roles.getRoleList(any())).thenReturn(new ArrayList<>());
        try (MockedStatic<DictFrameworkUtils> ignored = mockStatic(DictFrameworkUtils.class)) {
            service.saveLeave(20L, request());
        }
        ArgumentCaptor<LeaveDO> inserted = ArgumentCaptor.forClass(LeaveDO.class);
        verify(mapper).insert(inserted.capture());
        assertEquals("主部门", inserted.getValue().getApplyDeptName());
        verify(userService, never()).getUserDeptIds(anyLong());
        verify(departments, never()).getDeptList(any());
        assertEquals(20, inserted.getValue().getUserid());
    }

    @Test
    void editingDoesNotRefreshDepartmentSnapshot() {
        LeaveServiceImpl service = new LeaveServiceImpl();
        LeaveMapper mapper = mock(LeaveMapper.class);
        LeaveAttachMapper attachments = mock(LeaveAttachMapper.class);
        AdminUserApi users = mock(AdminUserApi.class);
        DeptApi departments = mock(DeptApi.class);
        ReflectionTestUtils.setField(service, "leaveMapper", mapper);
        ReflectionTestUtils.setField(service, "leaveAttachMapper", attachments);
        ReflectionTestUtils.setField(service, "adminUserApi", users);
        ReflectionTestUtils.setField(service, "deptApi", departments);
        when(mapper.selectById(10L)).thenReturn(new LeaveDO().setId(10L).setUserid(20)
                .setApplyDeptName("原科室").setQxjType(1));
        when(attachments.selectListByLeaveId(10L)).thenReturn(Collections.emptyList());
        service.updateLeave(request().setId(10L));
        ArgumentCaptor<LeaveDO> updated = ArgumentCaptor.forClass(LeaveDO.class);
        verify(mapper).updateById(updated.capture());
        assertNull(updated.getValue().getApplyDeptName()); // 默认非空更新策略保留库中快照
        verifyNoInteractions(users, departments);
    }

    @Test
    void detailKeepsSnapshotAfterTransferAndWhenApplicantIsMissing() {
        assertDetailDepartment("原科室", "原科室");
    }

    @Test
    void missingHistoricalDepartmentStaysUnknown() {
        assertDetailDepartment(null, "");
    }

    private void assertDetailDepartment(String snapshot, String expected) {
        LeaveController controller = new LeaveController();
        LeaveService service = mock(LeaveService.class);
        AdminUserApi users = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(controller, "leaveService", service);
        ReflectionTestUtils.setField(controller, "adminUserApi", users);
        LeaveDetailRespVO detail = new LeaveDetailRespVO();
        detail.setUserid(20);
        detail.setApplyDeptName(snapshot);
        when(service.getLeaveDetail(10L)).thenReturn(detail);
        when(users.getUser(20L)).thenReturn(new AdminUserRespDTO().setDeptId(99L));
        assertEquals(expected, controller.getLeave(10L).getData().getDeptName());
        when(users.getUser(20L)).thenReturn(null);
        assertEquals(expected, controller.getLeave(10L).getData().getDeptName());
    }

    private LeaveSaveReqVO request() {
        return new LeaveSaveReqVO().setQxjType(1).setTotalTs(BigDecimal.ONE)
                .setQxjStartDate(LocalDateTime.of(2026, 11, 5, 8, 30))
                .setQxjEndDate(LocalDateTime.of(2026, 11, 5, 13, 30))
                .setStartPeriod("1").setEndPeriod("2");
    }
}
