package cn.iocoder.yudao.module.bpm.dal.mysql.leave;

import java.time.LocalDateTime;
import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 假期申请审批 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface LeaveMapper extends BaseMapperX<LeaveDO> {

    List<LeaveSummaryRespVO> selectLeaveSummaryList(LeaveSummaryReqVO reqVO);

    List<LeaveDO> selectDetailList(LeaveSummaryReqVO reqVO);

    default PageResult<LeaveDO> selectPage(LeavePageReqVO reqVO) {
        MPJLambdaWrapperX<LeaveDO> wrapper = new MPJLambdaWrapperX<LeaveDO>()
                .selectAll(LeaveDO.class)
                .leftJoin(AdminUserDO.class, AdminUserDO::getId, LeaveDO::getUserid)
                .betweenIfPresent(LeaveDO::getApplyDate, reqVO.getApplyDate())
                .betweenIfPresent(LeaveDO::getQxjStartDate, reqVO.getQxjStartDate())
                .betweenIfPresent(LeaveDO::getQxjEndDate, reqVO.getQxjEndDate())
                .leIfPresent(LeaveDO::getQxjStartDate, getRangeEnd(reqVO.getLeaveTime()))
                .geIfPresent(LeaveDO::getQxjEndDate, getRangeStart(reqVO.getLeaveTime()))
                .eqIfPresent(LeaveDO::getQxjType, reqVO.getQxjType())
                .likeIfPresent(LeaveDO::getSjReason, reqVO.getSjReason())
                .eqIfPresent(LeaveDO::getTotalTs, reqVO.getTotalTs())
                .eqIfPresent(LeaveDO::getFilepath, reqVO.getFilepath())
                .eqIfPresent(LeaveDO::getUserid, reqVO.getUserId())
                .likeIfPresent(AdminUserDO::getNickname, reqVO.getNickName())
                .eqIfPresent(LeaveDO::getSpzt, reqVO.getSpzt())
                .orderByDesc(LeaveDO::getId);
        return selectJoinPage(reqVO, LeaveDO.class, wrapper);
    }

    static LocalDateTime getRangeStart(LocalDateTime[] range) {
        return range != null && range.length > 0 ? range[0] : null;
    }

    static LocalDateTime getRangeEnd(LocalDateTime[] range) {
        return range != null && range.length > 1 ? range[1] : null;
    }

    @Select("SELECT qxj_type AS qxjType, " +
            "COUNT(1) AS leaveCount, " +
            "SUM(total_ts) AS totalDays " +
            "FROM bpm_leave " +
            "WHERE userid = #{userId} " +
            "AND qxj_start_date >= #{beginTime} " +
            "AND qxj_start_date <= #{endTime} " +
            "AND spzt IN (1, 2) " + // 重点强调：只统计审批通过的。如果想把“审批中(1)”的也算上，可以改成 AND spzt IN (1, 2)
            "GROUP BY qxj_type")
    List<LeaveTypeStatRespVO> selectLeaveTypeStat(@Param("userId") Long userId,
                                                  @Param("beginTime") LocalDateTime beginTime,
                                                  @Param("endTime") LocalDateTime endTime);

}
