package cn.iocoder.yudao.module.system.service.meetingroom;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 会议室 Service 接口
 *
 * @author 管理员
 */
public interface MeetingRoomService {

    /**
     * 创建会议室
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMeetingRoom(@Valid MeetingRoomSaveReqVO createReqVO);

    /**
     * 更新会议室
     *
     * @param updateReqVO 更新信息
     */
    void updateMeetingRoom(@Valid MeetingRoomSaveReqVO updateReqVO);

    /**
     * 删除会议室
     *
     * @param id 编号
     */
    void deleteMeetingRoom(Long id);

    /**
    * 批量删除会议室
    *
    * @param ids 编号
    */
    void deleteMeetingRoomListByIds(List<Long> ids);

    /**
     * 获得会议室
     *
     * @param id 编号
     * @return 会议室
     */
    MeetingRoomDO getMeetingRoom(Long id);

    /**
     * 获得会议室分页
     *
     * @param pageReqVO 分页查询
     * @return 会议室分页
     */
    PageResult<MeetingRoomDO> getMeetingRoomPage(MeetingRoomPageReqVO pageReqVO);

}