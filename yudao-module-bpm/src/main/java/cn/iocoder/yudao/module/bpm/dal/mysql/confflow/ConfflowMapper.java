package cn.iocoder.yudao.module.bpm.dal.mysql.confflow;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.*;

/**
 * 会议报告单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ConfflowMapper extends BaseMapperX<ConfflowDO> {

    default PageResult<ConfflowDO> selectPage(ConfflowPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ConfflowDO>()
                .eqIfPresent(ConfflowDO::getUserId, reqVO.getUserId())
                .likeIfPresent(ConfflowDO::getUserName, reqVO.getUserName())
                .eqIfPresent(ConfflowDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ConfflowDO::getDeptName, reqVO.getDeptName())
                .betweenIfPresent(ConfflowDO::getApplyDate, reqVO.getApplyDate())
                .betweenIfPresent(ConfflowDO::getStartDate, reqVO.getStartDate())
                .eqIfPresent(ConfflowDO::getContent, reqVO.getContent())
                .eqIfPresent(ConfflowDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ConfflowDO::getVenue, reqVO.getVenue())
                .orderByDesc(ConfflowDO::getId));
    }

}