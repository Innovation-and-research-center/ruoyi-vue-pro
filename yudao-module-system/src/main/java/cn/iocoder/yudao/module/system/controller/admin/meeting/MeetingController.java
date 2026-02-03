package cn.iocoder.yudao.module.system.controller.admin.meeting;

import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingScheduleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.MeetingRoomSaveReqVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import javax.servlet.http.*;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

import cn.iocoder.yudao.module.system.controller.admin.meeting.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.meeting.MeetingDO;
import cn.iocoder.yudao.module.system.service.meeting.MeetingService;

@Tag(name = "管理后台 - 会议记录")
@RestController
@RequestMapping("/system/meeting")
@Validated
public class MeetingController {

    @Resource
    private MeetingService meetingService;

    @PostMapping("/create")
    @Operation(summary = "创建会议记录")
//    @PreAuthorize("@ss.hasPermission('system:meeting:create')")
    public CommonResult<Long> createMeeting(@Valid @RequestBody MeetingSaveReqVO createReqVO) {
        return success(meetingService.createMeeting(createReqVO));
    }

    @PostMapping("/book")
    @Operation(summary = "预定会议室")
    public CommonResult<Long> bookMeetingRoom(@Valid @RequestBody BookingSaveReqVO createReqVO) {
        return success(meetingService.createBooking(createReqVO));
    }




    @PutMapping("/update")
    @Operation(summary = "更新会议记录")
//    @PreAuthorize("@ss.hasPermission('system:meeting:update')")
    public CommonResult<Boolean> updateMeeting(@Valid @RequestBody MeetingSaveReqVO updateReqVO) {
        meetingService.updateMeeting(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议记录")
    @Parameter(name = "id", description = "编号", required = true)
//    @PreAuthorize("@ss.hasPermission('system:meeting:delete')")
    public CommonResult<Boolean> deleteMeeting(@RequestParam("id") Long id) {
        meetingService.deleteMeeting(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除会议记录")
//                @PreAuthorize("@ss.hasPermission('system:meeting:delete')")
    public CommonResult<Boolean> deleteMeetingList(@RequestParam("ids") List<Long> ids) {
        meetingService.deleteMeetingListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('system:meeting:query')")
    public CommonResult<MeetingRespVO> getMeeting(@RequestParam("id") Long id) {
        MeetingDO meeting = meetingService.getMeeting(id);
        return success(BeanUtils.toBean(meeting, MeetingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议记录分页")
//    @PreAuthorize("@ss.hasPermission('system:meeting:query')")
    public CommonResult<PageResult<MeetingRespVO>> getMeetingPage(@Valid MeetingPageReqVO pageReqVO) {
        PageResult<MeetingDO> pageResult = meetingService.getMeetingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MeetingRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会议记录 Excel")
//    @PreAuthorize("@ss.hasPermission('system:meeting:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMeetingExcel(@Valid MeetingPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MeetingDO> list = meetingService.getMeetingPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "会议记录.xls", "数据", MeetingRespVO.class,
                        BeanUtils.toBean(list, MeetingRespVO.class));
    }


    @GetMapping("/schedule")
    @Operation(summary = "获得会议预约排班表")
    public CommonResult<List<BookingScheduleRespVO>> getBookingSchedule(
            @Parameter(description = "开始时间", required = true, example = "2023-10-01 00:00:00")
            @RequestParam("beginTime")
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime beginTime,

            @Parameter(description = "结束时间", required = true, example = "2023-10-07 23:59:59")
            @RequestParam("endTime")
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) LocalDateTime endTime) {

        List<BookingScheduleRespVO> list = meetingService.getBookingSchedule(beginTime, endTime);
        return success(list);
    }

    @GetMapping("/export-meeting")
    @Operation(summary = "导出会议预约 Excel")
    // @PreAuthorize("@ss.hasPermission('system:meeting-booking:export')") // 记得开启权限
    public void exportMeetingSchedule(@Valid MeetingExportReqVO exportReqVO,
                                   HttpServletResponse response) throws IOException {
        meetingService.exportMeetingSchedule(exportReqVO, response);
    }

    @GetMapping("/monthly-schedule")
    @Operation(summary = "获得会议预约月度排班数据")
    // @PreAuthorize(...)
    public CommonResult<List<BookingScheduleRespVO>> getMonthlySchedule(@Valid MeetingExportReqVO reqVO) {
        // 调用 Service
        return success(meetingService.getMonthlySchedule(reqVO));
    }


}