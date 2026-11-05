package cn.iocoder.yudao.module.bpm.service.timeexplain;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo.TimeExplainSaveReqVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TimeExplainTimeTest {
    private final TimeExplainServiceImpl service = new TimeExplainServiceImpl();
    private final HolidayService holidays = mock(HolidayService.class);

    @BeforeEach
    void setUpCalendar() {
        ReflectionTestUtils.setField(service, "holidayService", holidays);
        when(holidays.getAllHolidaySummary()).thenReturn(Collections.emptyList());
    }

    private TimeExplainSaveReqVO request() {
        return new TimeExplainSaveReqVO()
                .setCheckBegin(LocalDateTime.of(2026, 11, 5, 9, 15))
                .setCheckEnd(LocalDateTime.of(2026, 11, 5, 16, 45));
    }

    @Test
    void retainsSelectedHoursAndMinutes() {
        TimeExplainSaveReqVO request = request();
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(LocalDateTime.of(2026, 11, 5, 9, 15), request.getCheckBegin());
        assertEquals(LocalDateTime.of(2026, 11, 5, 16, 45), request.getCheckEnd());
    }

    @Test
    void rejectsEarlierEndEvenWithinSamePeriod() {
        TimeExplainSaveReqVO request = request().setCheckEnd(LocalDateTime.of(2026, 11, 5, 9, 10));
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request));
    }

    @Test
    void acceptsOvernightRangeAndRejectsMissingTime() {
        TimeExplainSaveReqVO request = request().setCheckEnd(LocalDateTime.of(2026, 11, 6, 8, 0));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request));
        request.setCheckBegin(null);
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request));
    }

    @Test
    void keepsOldClientPeriodCompatibility() {
        TimeExplainSaveReqVO request = request().setStartPeriod("1").setEndPeriod("2");
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(8, request.getCheckBegin().getHour());
        assertEquals(30, request.getCheckBegin().getMinute());
        assertEquals(13, request.getCheckEnd().getHour());
        assertEquals(30, request.getCheckEnd().getMinute());
    }

    @Test
    void countsOverlappingWorkPeriodsInHalfDays() {
        TimeExplainSaveReqVO request = request()
                .setCheckBegin(LocalDateTime.of(2026, 11, 5, 8, 0))
                .setCheckEnd(LocalDateTime.of(2026, 11, 5, 12, 0));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("0.50"), request.getDays());
        request.setCheckEnd(LocalDateTime.of(2026, 11, 5, 20, 0));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("1.00"), request.getDays());
        request.setCheckEnd(LocalDateTime.of(2026, 11, 6, 2, 0));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("1.00"), request.getDays());
        request.setCheckEnd(LocalDateTime.of(2026, 11, 6, 9, 0));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("1.50"), request.getDays());
    }

    @Test
    void excludesNightHours() {
        TimeExplainSaveReqVO request = request()
                .setCheckBegin(LocalDateTime.of(2026, 11, 5, 23, 0))
                .setCheckEnd(LocalDateTime.of(2026, 11, 6, 0, 0));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("0.00"), request.getDays());
    }

    @Test
    void coversWorkPeriodBoundariesAndPartialPeriods() {
        assertDays("08:30", "11:30", "0.50");
        assertDays("13:30", "17:30", "0.50");
        assertDays("09:00", "09:15", "0.50");
        assertDays("14:00", "14:15", "0.50");
        assertDays("11:00", "14:00", "1.00");
        assertDays("11:30", "13:30", "0.00");
        assertDays("08:00", "08:30", "0.00");
        assertDays("17:30", "18:00", "0.00");
        assertDays("09:00", "09:00", "0.00");
    }

    private void assertDays(String begin, String end, String expected) {
        TimeExplainSaveReqVO request = request()
                .setCheckBegin(LocalDateTime.parse("2026-11-05T" + begin))
                .setCheckEnd(LocalDateTime.parse("2026-11-05T" + end));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal(expected), request.getDays(), begin + " - " + end);
    }

    @Test
    void excludesConfiguredHolidaysAndOrdinaryWeekendsButCountsMakeupWorkdays() {
        Map<String, String> calendar = new HashMap<>();
        calendar.put("restDay", "2026-11-06"); // 周五放假
        calendar.put("workDay", "2026-11-07"); // 周六补班
        when(holidays.getAllHolidaySummary()).thenReturn(Collections.singletonList(calendar));
        TimeExplainSaveReqVO request = request()
                .setCheckBegin(LocalDateTime.of(2026, 11, 6, 8, 30))
                .setCheckEnd(LocalDateTime.of(2026, 11, 8, 17, 30));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("1.00"), request.getDays());
        request.setCheckBegin(LocalDateTime.of(2026, 11, 8, 8, 30));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("0.00"), request.getDays());
    }

    @Test
    void countsCrossYearCalendarAndStopsWhenCalendarCannotBeLoaded() {
        Map<String, String> nextYear = new HashMap<>();
        nextYear.put("restDay", "2027-01-01");
        when(holidays.getAllHolidaySummary()).thenReturn(Collections.singletonList(nextYear));
        TimeExplainSaveReqVO request = request()
                .setCheckBegin(LocalDateTime.of(2026, 12, 31, 14, 0))
                .setCheckEnd(LocalDateTime.of(2027, 1, 1, 17, 30));
        ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request);
        assertEquals(new BigDecimal("0.50"), request.getDays());
        when(holidays.getAllHolidaySummary()).thenThrow(new IllegalStateException("calendar unavailable"));
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "normalizePeriodTime", request));
    }
}
