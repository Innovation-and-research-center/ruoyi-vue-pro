package cn.iocoder.yudao.module.system.dal.mysql.meetingroom;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.*;

/**
 * 会议室 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface MeetingRoomMapper extends BaseMapperX<MeetingRoomDO> {

    default PageResult<MeetingRoomDO> selectPage(MeetingRoomPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MeetingRoomDO>()
                .likeIfPresent(MeetingRoomDO::getRoomName, reqVO.getRoomName())
                .betweenIfPresent(MeetingRoomDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MeetingRoomDO::getId));
    }

}