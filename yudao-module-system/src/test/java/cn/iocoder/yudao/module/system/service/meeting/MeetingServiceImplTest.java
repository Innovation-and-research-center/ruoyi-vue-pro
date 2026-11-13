package cn.iocoder.yudao.module.system.service.meeting;

import cn.iocoder.yudao.module.system.controller.admin.meeting.vo.MeetingExportReqVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingScheduleRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.meeting.MeetingDO;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import cn.iocoder.yudao.module.system.dal.mysql.meeting.MeetingMapper;
import cn.iocoder.yudao.module.system.dal.mysql.meetingroom.MeetingRoomMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeetingServiceImplTest {

    private MeetingServiceImpl meetingService;
    private MeetingMapper meetingMapper;
    private DeptService deptService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        DataPermissionContextHolder.clear();
    }

    @BeforeEach
    void setUp() {
        meetingService = new MeetingServiceImpl();
        meetingMapper = mock(MeetingMapper.class);
        MeetingRoomMapper meetingRoomMapper = mock(MeetingRoomMapper.class);
        ReflectionTestUtils.setField(meetingService, "meetingMapper", meetingMapper);
        ReflectionTestUtils.setField(meetingService, "meetingRoomMapper", meetingRoomMapper);

        MeetingRoomDO room = new MeetingRoomDO();
        room.setId(1L);
        MeetingDO booking = MeetingDO.builder()
                .id(10L)
                .meetingRoomId(1L)
                .userId(20L)
                .startTime(LocalDateTime.of(2026, 10, 9, 9, 0))
                .endTime(LocalDateTime.of(2026, 10, 9, 10, 0))
                .attendNumber(12)
                .meetingAbstract("项目进度讨论")
                .build();
        when(meetingRoomMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(room));
        when(meetingMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(booking));
    }

    @Test
    void bookingScheduleIncludesAttendeeCountAndSubject() {
        assertBookingDetail(meetingService.getBookingSchedule(
                LocalDateTime.of(2026, 10, 5, 0, 0), LocalDateTime.of(2026, 10, 11, 23, 59, 59)));
    }

    @Test
    void monthlyScheduleIncludesAttendeeCountAndSubject() {
        MeetingExportReqVO request = new MeetingExportReqVO();
        request.setYear(2026);
        request.setMonth(10);

        assertBookingDetail(meetingService.getMonthlySchedule(request));
    }

    @Test
    void bookingResolvesOwnDepartmentWithSelfOnlyDataScope() {
        BookingSaveReqVO request = prepareBooking();
        when(deptService.getDept(30L)).thenAnswer(invocation -> {
            // 模拟仅本人权限：正常查询看不到部门，内部查询才能获得部门。
            if (DataPermissionContextHolder.get() == null || DataPermissionContextHolder.get().enable()) {
                return null;
            }
            DeptDO dept = new DeptDO();
            dept.setName("测试部门");
            return dept;
        });
        when(meetingMapper.insert(any(MeetingDO.class))).thenAnswer(invocation -> {
            assertNull(DataPermissionContextHolder.get(), "保存预约时必须恢复数据权限");
            return 1;
        });

        meetingService.createBooking(request);

        ArgumentCaptor<MeetingDO> saved = ArgumentCaptor.forClass(MeetingDO.class);
        verify(meetingMapper).insert(saved.capture());
        assertEquals(20L, saved.getValue().getUserId());
        assertEquals("测试部门", saved.getValue().getDepartment());
        verify(deptService).getDept(30L);
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void missingDepartmentDoesNotInventDepartmentName() {
        BookingSaveReqVO request = prepareBooking();
        when(deptService.getDept(30L)).thenReturn(null);

        meetingService.createBooking(request);

        ArgumentCaptor<MeetingDO> saved = ArgumentCaptor.forClass(MeetingDO.class);
        verify(meetingMapper).insert(saved.capture());
        assertNull(saved.getValue().getDepartment());
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void departmentLookupFailureRestoresDataPermissionAndDoesNotSave() {
        BookingSaveReqVO request = prepareBooking();
        when(deptService.getDept(30L)).thenThrow(new IllegalStateException("department lookup failed"));

        assertThrows(Exception.class, () -> meetingService.createBooking(request));

        assertNull(DataPermissionContextHolder.get());
        verify(meetingMapper, never()).insert(any(MeetingDO.class));
    }

    private BookingSaveReqVO prepareBooking() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        WebFrameworkUtils.setLoginUserId(servletRequest, 20L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        AdminUserService userService = mock(AdminUserService.class);
        deptService = mock(DeptService.class);
        ReflectionTestUtils.setField(meetingService, "adminUserService", userService);
        ReflectionTestUtils.setField(meetingService, "deptService", deptService);
        AdminUserDO user = new AdminUserDO();
        user.setId(20L);
        user.setDeptId(30L);
        when(userService.getUser(20L)).thenAnswer(invocation -> {
            assertNull(DataPermissionContextHolder.get(), "用户查询仍应受数据权限约束");
            return user;
        });
        when(meetingMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        BookingSaveReqVO request = new BookingSaveReqVO();
        request.setMeetingRoomId(1L);
        request.setStartTime(LocalDateTime.of(2026, 11, 12, 9, 0));
        request.setEndTime(LocalDateTime.of(2026, 11, 12, 10, 0));
        request.setAttendNumber(2);
        request.setMeetingAbstract("测试预约");
        return request;
    }

    private void assertBookingDetail(List<BookingScheduleRespVO> schedule) {
        assertEquals(1, schedule.size());
        assertEquals(1, schedule.get(0).getBookingList().size());
        BookingScheduleRespVO.BookingDetail detail = schedule.get(0).getBookingList().get(0);
        assertEquals(10L, detail.getId());
        assertEquals(12, detail.getAttendNumber());
        assertEquals("项目进度讨论", detail.getSubject());
    }
}
