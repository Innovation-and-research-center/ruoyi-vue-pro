package cn.iocoder.yudao.module.bpm.dal.mysql.xzfy;

import java.util.*;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.*;

/**
 * 行政复议 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface XzfyMapper extends BaseMapperX<XzfyDO> {

    default PageResult<XzfyDO> selectPage(XzfyPageReqVO reqVO) {
        LambdaQueryWrapperX<XzfyDO> wrapper = new LambdaQueryWrapperX<XzfyDO>()
                .likeIfPresent(XzfyDO::getSwWh, reqVO.getSwWh())
                .likeIfPresent(XzfyDO::getSwJg, reqVO.getSwJg())
                .betweenIfPresent(XzfyDO::getSwRq, reqVO.getSwRq())
                .likeIfPresent(XzfyDO::getSqr, reqVO.getSqr())
                .likeIfPresent(XzfyDO::getDsr, reqVO.getDsr())
                .likeIfPresent(XzfyDO::getTdZl, reqVO.getTdZl())
                .eqIfPresent(XzfyDO::getLb1, reqVO.getLb1())
                .eqIfPresent(XzfyDO::getLb2, reqVO.getLb2())
                .eqIfPresent(XzfyDO::getLb3, reqVO.getLb3())
                .eqIfPresent(XzfyDO::getStatus, reqVO.getStatus());
        orderBy(reqVO, wrapper);
        return selectPage(reqVO, wrapper);
    }

    default void orderBy(XzfyPageReqVO reqVO, LambdaQueryWrapperX<XzfyDO> wrapper) {
        if (StrUtil.isBlank(reqVO.getOrderField()) || StrUtil.isBlank(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(XzfyDO::getId);
            return;
        }
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        switch (reqVO.getOrderField()) {
            case "swWh":
                if (asc) wrapper.orderByAsc(XzfyDO::getSwWh);
                else wrapper.orderByDesc(XzfyDO::getSwWh);
                break;
            case "swJg":
                if (asc) wrapper.orderByAsc(XzfyDO::getSwJg);
                else wrapper.orderByDesc(XzfyDO::getSwJg);
                break;
            case "swRq":
                if (asc) wrapper.orderByAsc(XzfyDO::getSwRq);
                else wrapper.orderByDesc(XzfyDO::getSwRq);
                break;
            case "sqr":
                if (asc) wrapper.orderByAsc(XzfyDO::getSqr);
                else wrapper.orderByDesc(XzfyDO::getSqr);
                break;
            case "bsqr":
                if (asc) wrapper.orderByAsc(XzfyDO::getBsqr);
                else wrapper.orderByDesc(XzfyDO::getBsqr);
                break;
            case "dsr":
                if (asc) wrapper.orderByAsc(XzfyDO::getDsr);
                else wrapper.orderByDesc(XzfyDO::getDsr);
                break;
            case "tdZl":
                if (asc) wrapper.orderByAsc(XzfyDO::getTdZl);
                else wrapper.orderByDesc(XzfyDO::getTdZl);
                break;
            case "lb1":
                if (asc) wrapper.orderByAsc(XzfyDO::getLb1);
                else wrapper.orderByDesc(XzfyDO::getLb1);
                break;
            case "lb2":
                if (asc) wrapper.orderByAsc(XzfyDO::getLb2);
                else wrapper.orderByDesc(XzfyDO::getLb2);
                break;
            case "lb3":
                if (asc) wrapper.orderByAsc(XzfyDO::getLb3);
                else wrapper.orderByDesc(XzfyDO::getLb3);
                break;
            default:
                wrapper.orderByDesc(XzfyDO::getId);
        }
    }

}
