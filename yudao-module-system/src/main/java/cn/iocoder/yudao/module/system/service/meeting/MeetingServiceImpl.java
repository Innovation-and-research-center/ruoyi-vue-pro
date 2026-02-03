package cn.iocoder.yudao.module.system.service.meeting;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.BookingScheduleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.MeetingRoomRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.meetingroom.MeetingRoomMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.meetingroom.MeetingRoomService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.system.controller.admin.meeting.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.meeting.MeetingDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.system.dal.mysql.meeting.MeetingMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 会议记录 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class MeetingServiceImpl implements MeetingService {

    @Resource
    private MeetingMapper meetingMapper;

    @Resource
    private MeetingRoomMapper meetingRoomMapper;

    @Resource
    private AdminUserService adminUserService;
    @Resource
    private DeptService deptService;

    @Override
    public Long createMeeting(MeetingSaveReqVO createReqVO) {
        // 插入
        MeetingDO meeting = BeanUtils.toBean(createReqVO, MeetingDO.class);
        meetingMapper.insert(meeting);

        // 返回
        return meeting.getId();
    }

    @Override
    public void updateMeeting(MeetingSaveReqVO updateReqVO) {
        // 校验存在
        validateMeetingExists(updateReqVO.getId());
        // 更新
        MeetingDO updateObj = BeanUtils.toBean(updateReqVO, MeetingDO.class);
        meetingMapper.updateById(updateObj);
    }

    @Override
    public void deleteMeeting(Long id) {
        // 校验存在
        validateMeetingExists(id);
        // 删除
        meetingMapper.deleteById(id);
    }

    @Override
        public void deleteMeetingListByIds(List<Long> ids) {
        // 删除
        meetingMapper.deleteByIds(ids);
        }


    private void validateMeetingExists(Long id) {
        if (meetingMapper.selectById(id) == null) {
            throw exception(MEETING_NOT_EXISTS);
        }
    }

    @Override
    public MeetingDO getMeeting(Long id) {
        return meetingMapper.selectById(id);
    }

    @Override
    public PageResult<MeetingDO> getMeetingPage(MeetingPageReqVO pageReqVO) {
        return meetingMapper.selectPage(pageReqVO);
    }

    // 创建预约
    public Long createBooking(BookingSaveReqVO createReqVO) {
        // 1. 校验时间合法性
        if (createReqVO.getStartTime().isAfter(createReqVO.getEndTime())) {
            throw exception(MEETING_TIME_ERROR);
        }

        // 2. 核心：冲突检测 (迁移旧代码逻辑)
        Long conflictCount = meetingMapper.selectCount(new LambdaQueryWrapper<MeetingDO>()
                .eq(MeetingDO::getMeetingRoomId, createReqVO.getMeetingRoomId())
                .eq(MeetingDO::getDeleted, 0) // 只查有效的
                .lt(MeetingDO::getStartTime, createReqVO.getEndTime()) // 现有开始 < 新结束
                .gt(MeetingDO::getEndTime, createReqVO.getStartTime()) // 现有结束 > 新开始
        );

        if (conflictCount>0) {
            throw exception(MEETING_TIME_BOOKED);
        }

        // 3. 插入数据
        MeetingDO booking = BeanUtils.toBean(createReqVO, MeetingDO.class);
        Long userId = getLoginUserId();
        booking.setUserId(userId); // 获取当前登录人
        // 获取用户详情
        AdminUserDO user = adminUserService.getUser(userId);
        if (user != null) {
            // 自动填充昵称和手机号
            booking.setStaffName(user.getNickname());
            booking.setTelephone(user.getMobile());

            // 自动填充部门名称
            if (user.getDeptId() != null) {
                DeptDO dept = deptService.getDept(user.getDeptId());
                if (dept != null) {
                    booking.setDepartment(dept.getName());
                }
            }
        } else {
            // 异常兜底：理论上登录用户一定存在，但以防万一
            booking.setStaffName("未知用户");
        }
        meetingMapper.insert(booking);
        return booking.getId();
    }

    public List<BookingScheduleRespVO> getBookingSchedule(LocalDateTime beginTime, LocalDateTime endTime) {
        // 1. 获取所有可用会议室，按 Sequence 排序
        List<MeetingRoomDO> rooms = meetingRoomMapper.selectList(
                new LambdaQueryWrapper<MeetingRoomDO>().orderByAsc(MeetingRoomDO::getSequence)
        );

        // 2. 获取该时间段内所有的预约记录
        List<MeetingDO> allBookings = meetingMapper.selectList(
                new LambdaQueryWrapper<MeetingDO>()
                        .between(MeetingDO::getStartTime, beginTime, endTime)
        );

        // 3. 组装数据
        List<BookingScheduleRespVO> result = new ArrayList<>();
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();

        for (MeetingRoomDO room : rooms) {
            BookingScheduleRespVO vo = new BookingScheduleRespVO();
            vo.setRoom(BeanUtils.toBean(room, MeetingRoomRespVO.class));

            // 筛选属于该会议室的预约
            List<BookingScheduleRespVO.BookingDetail> details = allBookings.stream()
                    .filter(b -> b.getMeetingRoomId().equals(room.getId()))
                    .map(b -> {
                        BookingScheduleRespVO.BookingDetail detail = BeanUtils.toBean(b, BookingScheduleRespVO.BookingDetail.class);
                        // 实际项目中需要调用 AdminUserApi 获取用户昵称，这里简化
                        detail.setNickName(b.getStaffName());
                        detail.setDepartment(b.getDepartment());
                        detail.setTelephone(b.getTelephone());
                        detail.setIsMyBooking(b.getUserId().equals(currentUserId));
                        return detail;
                    }).collect(Collectors.toList());

            vo.setBookingList(details);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<BookingScheduleRespVO> getMonthlySchedule(MeetingExportReqVO reqVO) {
        Integer year = reqVO.getYear();
        Integer month = reqVO.getMonth();

        // 1. 计算当月起止时间
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDateTime beginTime = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(year, month, daysInMonth, 23, 59, 59);

        // 2. 复用之前的核心逻辑：getBookingSchedule
        // 因为 getBookingSchedule 本身就是接收 beginTime 和 endTime 的
        // 只要逻辑是通用的，直接调用即可
        return this.getBookingSchedule(beginTime, endTime);
    }

    @Override
    public void exportMeetingSchedule(MeetingExportReqVO reqVO, HttpServletResponse response) throws IOException {
        int year = reqVO.getYear();
        int month = reqVO.getMonth();

        // 1. 计算时间范围
        YearMonth yearMonth = YearMonth.of(year, month);
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDateTime beginTime = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(year, month, daysInMonth, 23, 59, 59);

        // 2. 获取数据 (复用之前的查询逻辑)
        // 2.1 查所有启用的会议室
        List<MeetingRoomDO> rooms = meetingRoomMapper.selectList(new LambdaQueryWrapper<MeetingRoomDO>()// 假设有 status 字段，或 deleted=0
                .orderByAsc(MeetingRoomDO::getSequence));

        // 2.2 查当月所有预约
        List<MeetingDO> bookings = meetingMapper.selectList(new LambdaQueryWrapper<MeetingDO>()
                .between(MeetingDO::getStartTime, beginTime, endTime)
                .eq(MeetingDO::getDeleted, 0)); // 0正常

        // 3. 动态构建 Excel 表头 (List<List<String>>)
        // C# 表头: 名称 | 面积 | 座位 | 备注 | 1日 | 2日 ...
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("会议室名称"));
        head.add(Collections.singletonList("面积"));
        head.add(Collections.singletonList("座位数"));
        head.add(Collections.singletonList("备注"));

        for (int i = 1; i <= daysInMonth; i++) {
            head.add(Collections.singletonList(month + "月" + i + "日"));
        }

        // 4. 构建数据行 (List<List<Object>>)
        List<List<Object>> data = new ArrayList<>();

        for (MeetingRoomDO room : rooms) {
            List<Object> row = new ArrayList<>();
            // 4.1 固定列数据
            row.add(room.getRoomName());
            row.add(room.getRoomArea() != null ? room.getRoomArea() + "平米" : "");
            row.add(room.getSeats() != null ? room.getSeats() + "个" : "");
            row.add(room.getRemark());

            // 4.2 每天的数据
            for (int day = 1; day <= daysInMonth; day++) {
                // 当前日期的字符串匹配 key，例如 "2026-02-03"
                String currentDateStr = LocalDate.of(year, month, day).toString();

                // 筛选出该房间、该日期的所有预约
                List<MeetingDO> dayBookings = bookings.stream()
                        .filter(b -> b.getMeetingRoomId().equals(room.getId()) &&
                                b.getStartTime().toLocalDate().toString().equals(currentDateStr))
                        .sorted(Comparator.comparing(MeetingDO::getStartTime))
                        .collect(Collectors.toList());

                // 拼接单元格内容
                // C# 格式: "预约人：Dept-Name，会议时段：Start-End"
                StringBuilder cellContent = new StringBuilder();
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

                for (MeetingDO bk : dayBookings) {
                    if (cellContent.length() > 0) {
                        cellContent.append("\n"); // 多个预约换行显示
                    }
                    String timeRange = bk.getStartTime().format(timeFmt) + "-" + bk.getEndTime().format(timeFmt);
                    // 注意：DO里如果没有 deptName，需要关联查询或者在DO里冗余存入
                    String dept = bk.getDepartment() == null ? "" : bk.getDepartment();
                    String name = bk.getStaffName() == null ? "" : bk.getStaffName();

                    cellContent.append(String.format("预约人：%s-%s，时段：%s", dept, name, timeRange));
                }

                row.add(cellContent.toString());
            }
            data.add(row);
        }

        // 5. 写入流
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 防止中文乱码
        String fileName = URLEncoder.encode(year + "年" + month + "月会议室预约记录", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .autoCloseStream(false) // 不要自动关闭流，交给Servlet容器
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()) // 自动列宽
                .sheet("会议预约")
                .doWrite(data);
    }

}