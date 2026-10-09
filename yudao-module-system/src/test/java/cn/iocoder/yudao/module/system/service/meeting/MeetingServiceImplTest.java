package cn.iocoder.yudao.module.system.service.meeting;

import cn.iocoder.yudao.module.system.controller.admin.meeting.vo.MeetingExportReqVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingScheduleRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.meeting.MeetingDO;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import cn.iocoder.yudao.module.system.dal.mysql.meeting.MeetingMapper;
import cn.iocoder.yudao.module.system.dal.mysql.meetingroom.MeetingRoomMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MeetingServiceImplTest {

    private MeetingServiceImpl meetingService;

    @BeforeEach
    void setUp() {
        meetingService = new MeetingServiceImpl();
        MeetingMapper meetingMapper = mock(MeetingMapper.class);
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

    private void assertBookingDetail(List<BookingScheduleRespVO> schedule) {
        assertEquals(1, schedule.size());
        assertEquals(1, schedule.get(0).getBookingList().size());
        BookingScheduleRespVO.BookingDetail detail = schedule.get(0).getBookingList().get(0);
        assertEquals(10L, detail.getId());
        assertEquals(12, detail.getAttendNumber());
        assertEquals("项目进度讨论", detail.getSubject());
    }
}
