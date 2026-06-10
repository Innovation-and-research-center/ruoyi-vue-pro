package cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo.*;

/**
 * 外出请假补假 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface TimeExplainMapper extends BaseMapperX<TimeExplainDO> {

    default PageResult<TimeExplainDO> selectPage(TimeExplainPageReqVO reqVO) {
        MPJLambdaWrapperX<TimeExplainDO> wrapper = new MPJLambdaWrapperX<TimeExplainDO>()
                .selectAll(TimeExplainDO.class)
                .leftJoin(AdminUserDO.class, AdminUserDO::getId, TimeExplainDO::getUserId)
                .eqIfPresent(TimeExplainDO::getActinstId, reqVO.getActinstId())
                .eqIfPresent(TimeExplainDO::getUserId, reqVO.getUserId())
                .likeIfPresent(TimeExplainDO::getUserName, reqVO.getUserName())
                .likeIfPresent(AdminUserDO::getNickname, reqVO.getNickName())
                .betweenIfPresent(TimeExplainDO::getCheckDate, reqVO.getCheckDate())
                .leIfPresent(TimeExplainDO::getCheckBegin, getRangeEnd(reqVO.getOutingTime()))
                .geIfPresent(TimeExplainDO::getCheckEnd, getRangeStart(reqVO.getOutingTime()))
                .likeIfPresent(TimeExplainDO::getReason, reqVO.getReason())
                .likeIfPresent(TimeExplainDO::getStartPlace, reqVO.getStartPlace())
                .likeIfPresent(TimeExplainDO::getEndPlace, reqVO.getEndPlace())
                .eqIfPresent(TimeExplainDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TimeExplainDO::getDays, reqVO.getDays())
                .eqIfPresent(TimeExplainDO::getYear, reqVO.getYear())
                .betweenIfPresent(TimeExplainDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TimeExplainDO::getId);
        return selectJoinPage(reqVO, TimeExplainDO.class, wrapper);
    }

    static java.time.LocalDateTime getRangeStart(java.time.LocalDateTime[] range) {
        return range != null && range.length > 0 ? range[0] : null;
    }

    static java.time.LocalDateTime getRangeEnd(java.time.LocalDateTime[] range) {
        return range != null && range.length > 1 ? range[1] : null;
    }

}
