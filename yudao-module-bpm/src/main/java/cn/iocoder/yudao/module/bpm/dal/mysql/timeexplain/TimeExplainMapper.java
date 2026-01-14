package cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
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
        return selectPage(reqVO, new LambdaQueryWrapperX<TimeExplainDO>()
                .eqIfPresent(TimeExplainDO::getActinstId, reqVO.getActinstId())
                .eqIfPresent(TimeExplainDO::getUserId, reqVO.getUserId())
                .likeIfPresent(TimeExplainDO::getUserName, reqVO.getUserName())
                .betweenIfPresent(TimeExplainDO::getCheckDate, reqVO.getCheckDate())
                .eqIfPresent(TimeExplainDO::getCheckBegin, reqVO.getCheckBegin())
                .eqIfPresent(TimeExplainDO::getCheckEnd, reqVO.getCheckEnd())
                .eqIfPresent(TimeExplainDO::getStatus, reqVO.getStatus())
                .eqIfPresent(TimeExplainDO::getDays, reqVO.getDays())
                .eqIfPresent(TimeExplainDO::getYear, reqVO.getYear())
                .betweenIfPresent(TimeExplainDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(TimeExplainDO::getId));
    }

}