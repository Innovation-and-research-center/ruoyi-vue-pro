package cn.iocoder.yudao.module.bpm.dal.mysql.confflow;

import java.util.*;

import cn.hutool.core.util.StrUtil;
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
        LambdaQueryWrapperX<ConfflowDO> wrapper = new LambdaQueryWrapperX<ConfflowDO>()
                .eqIfPresent(ConfflowDO::getUserId, reqVO.getUserId())
                .likeIfPresent(ConfflowDO::getUserName, reqVO.getUserName())
                .eqIfPresent(ConfflowDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ConfflowDO::getDeptName, reqVO.getDeptName())
                .betweenIfPresent(ConfflowDO::getApplyDate, reqVO.getApplyDate())
                .betweenIfPresent(ConfflowDO::getStartDate, reqVO.getStartDate())
                .likeIfPresent(ConfflowDO::getTitle, reqVO.getTitle())
                .likeIfPresent(ConfflowDO::getContent, reqVO.getContent())
                .likeIfPresent(ConfflowDO::getRemark, reqVO.getRemark())
                .likeIfPresent(ConfflowDO::getVenue, reqVO.getVenue())
                .likeIfPresent(ConfflowDO::getJoinUnit, reqVO.getJoinUnit())
                .likeIfPresent(ConfflowDO::getOfferUnit, reqVO.getOfferUnit())
                .likeIfPresent(ConfflowDO::getOfferPerson, reqVO.getOfferPerson())
                .eqIfPresent(ConfflowDO::getStatus, reqVO.getStatus());
        orderBy(reqVO, wrapper);
        return selectPage(reqVO, wrapper);
    }

    default void orderBy(ConfflowPageReqVO reqVO, LambdaQueryWrapperX<ConfflowDO> wrapper) {
        if (StrUtil.isBlank(reqVO.getOrderField()) || StrUtil.isBlank(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(ConfflowDO::getStartDate).orderByDesc(ConfflowDO::getId);
            return;
        }
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        switch (reqVO.getOrderField()) {
            case "title":
                if (asc) wrapper.orderByAsc(ConfflowDO::getTitle);
                else wrapper.orderByDesc(ConfflowDO::getTitle);
                break;
            case "startDate":
                if (asc) wrapper.orderByAsc(ConfflowDO::getStartDate);
                else wrapper.orderByDesc(ConfflowDO::getStartDate);
                break;
            case "venue":
                if (asc) wrapper.orderByAsc(ConfflowDO::getVenue);
                else wrapper.orderByDesc(ConfflowDO::getVenue);
                break;
            case "joinUnit":
                if (asc) wrapper.orderByAsc(ConfflowDO::getJoinUnit);
                else wrapper.orderByDesc(ConfflowDO::getJoinUnit);
                break;
            case "offerUnit":
                if (asc) wrapper.orderByAsc(ConfflowDO::getOfferUnit);
                else wrapper.orderByDesc(ConfflowDO::getOfferUnit);
                break;
            case "status":
                if (asc) wrapper.orderByAsc(ConfflowDO::getStatus);
                else wrapper.orderByDesc(ConfflowDO::getStatus);
                break;
            default:
                wrapper.orderByDesc(ConfflowDO::getStartDate).orderByDesc(ConfflowDO::getId);
        }
    }

}
