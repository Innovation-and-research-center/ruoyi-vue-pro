package cn.iocoder.yudao.module.system.service.meeting;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import javax.servlet.http.HttpServletResponse;
import javax.validation.*;
import cn.iocoder.yudao.module.system.controller.admin.meeting.vo.*;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingScheduleRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.meeting.MeetingDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 会议记录 Service 接口
 *
 * @author 管理员
 */
public interface MeetingService {

    /**
     * 创建会议记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMeeting(@Valid MeetingSaveReqVO createReqVO);

    Long createBooking(@Valid BookingSaveReqVO createReqVO);


    List<BookingScheduleRespVO> getBookingSchedule(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 更新会议记录
     *
     * @param updateReqVO 更新信息
     */
    void updateMeeting(@Valid MeetingSaveReqVO updateReqVO);

    /**
     * 删除会议记录
     *
     * @param id 编号
     */
    void deleteMeeting(Long id);

    /**
    * 批量删除会议记录
    *
    * @param ids 编号
    */
    void deleteMeetingListByIds(List<Long> ids);

    /**
     * 获得会议记录
     *
     * @param id 编号
     * @return 会议记录
     */
    MeetingDO getMeeting(Long id);

    /**
     * 获得会议记录分页
     *
     * @param pageReqVO 分页查询
     * @return 会议记录分页
     */
    PageResult<MeetingDO> getMeetingPage(MeetingPageReqVO pageReqVO);

    List<BookingScheduleRespVO> getMonthlySchedule(MeetingExportReqVO reqVO);

    void exportMeetingSchedule(MeetingExportReqVO reqVO, HttpServletResponse response) throws IOException;

}