package cn.iocoder.yudao.module.system.service.meetingroom;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.system.dal.mysql.meetingroom.MeetingRoomMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 会议室 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class MeetingRoomServiceImpl implements MeetingRoomService {

    @Resource
    private MeetingRoomMapper meetingRoomMapper;

    @Override
    public Long createMeetingRoom(MeetingRoomSaveReqVO createReqVO) {
        // 插入
        MeetingRoomDO meetingRoom = BeanUtils.toBean(createReqVO, MeetingRoomDO.class);
        meetingRoomMapper.insert(meetingRoom);

        // 返回
        return meetingRoom.getId();
    }

    @Override
    public void updateMeetingRoom(MeetingRoomSaveReqVO updateReqVO) {
        // 校验存在
        validateMeetingRoomExists(updateReqVO.getId());
        // 更新
        MeetingRoomDO updateObj = BeanUtils.toBean(updateReqVO, MeetingRoomDO.class);
        meetingRoomMapper.updateById(updateObj);
    }

    @Override
    public void deleteMeetingRoom(Long id) {
        // 校验存在
        validateMeetingRoomExists(id);
        // 删除
        meetingRoomMapper.deleteById(id);
    }

    @Override
        public void deleteMeetingRoomListByIds(List<Long> ids) {
        // 删除
        meetingRoomMapper.deleteByIds(ids);
        }


    private void validateMeetingRoomExists(Long id) {
        if (meetingRoomMapper.selectById(id) == null) {
            throw exception(MEETING_ROOM_NOT_EXISTS);
        }
    }

    @Override
    public MeetingRoomDO getMeetingRoom(Long id) {
        return meetingRoomMapper.selectById(id);
    }

    @Override
    public PageResult<MeetingRoomDO> getMeetingRoomPage(MeetingRoomPageReqVO pageReqVO) {
        return meetingRoomMapper.selectPage(pageReqVO);
    }

}