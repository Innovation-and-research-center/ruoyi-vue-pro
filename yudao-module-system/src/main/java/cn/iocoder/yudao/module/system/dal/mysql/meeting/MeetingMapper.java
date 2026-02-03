package cn.iocoder.yudao.module.system.dal.mysql.meeting;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.meeting.MeetingDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.system.controller.admin.meeting.vo.*;

/**
 * 会议记录 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface MeetingMapper extends BaseMapperX<MeetingDO> {

    default PageResult<MeetingDO> selectPage(MeetingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MeetingDO>()
                .eqIfPresent(MeetingDO::getMeetingRoomId, reqVO.getMeetingRoomId())
                .betweenIfPresent(MeetingDO::getRecordTime, reqVO.getRecordTime())
                .betweenIfPresent(MeetingDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(MeetingDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(MeetingDO::getUserId, reqVO.getUserId())
                .likeIfPresent(MeetingDO::getStaffName, reqVO.getStaffName())
                .eqIfPresent(MeetingDO::getDepartment, reqVO.getDepartment())
                .eqIfPresent(MeetingDO::getTelephone, reqVO.getTelephone())
                .eqIfPresent(MeetingDO::getAttendNumber, reqVO.getAttendNumber())
                .eqIfPresent(MeetingDO::getMeetingAbstract, reqVO.getMeetingAbstract())
                .betweenIfPresent(MeetingDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MeetingDO::getId));
    }

}