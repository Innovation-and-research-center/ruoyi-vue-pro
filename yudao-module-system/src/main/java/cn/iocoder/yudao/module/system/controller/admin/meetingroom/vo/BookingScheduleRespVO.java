package cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingScheduleRespVO {
    // 包含会议室的基本信息
    private MeetingRoomRespVO room;

    // 该会议室在当前查询周期内的所有预约
    private List<BookingDetail> bookingList;

    @Data
    public static class BookingDetail {
        private Long id;
        private Long userId;
        private String nickName; // 预约人姓名
        private String telephone;
        private String department;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String subject;
        private Boolean isMyBooking; // 辅助字段：是不是我约的（用于前端判断颜色）
    }
}
