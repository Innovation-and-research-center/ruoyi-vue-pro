package cn.iocoder.yudao.module.bpm.dal.mysql.xzss;

import java.util.*;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.util.BpmQueryUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo.*;

/**
 * 行政诉讼 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface XzssMapper extends BaseMapperX<XzssDO> {

    default PageResult<XzssDO> selectPage(XzssPageReqVO reqVO) {
        LambdaQueryWrapperX<XzssDO> wrapper = new LambdaQueryWrapperX<XzssDO>()
                .likeIfPresent(XzssDO::getSwWh, reqVO.getSwWh())
                .likeIfPresent(XzssDO::getSwJg, reqVO.getSwJg())
                .betweenIfPresent(XzssDO::getSwRq, reqVO.getSwRq())
                .likeIfPresent(XzssDO::getDsr, reqVO.getDsr())
                .likeIfPresent(XzssDO::getTdZl, reqVO.getTdZl())
                .eqIfPresent(XzssDO::getSsLx, reqVO.getSsLx())
                .eqIfPresent(XzssDO::getLb1, reqVO.getLb1())
                .eqIfPresent(XzssDO::getLb2, reqVO.getLb2())
                .eqIfPresent(XzssDO::getLb3, reqVO.getLb3())
                .eqIfPresent(XzssDO::getLb4, reqVO.getLb4())
                .eqIfPresent(XzssDO::getLb5, reqVO.getLb5())
                .likeIfPresent(XzssDO::getSsNr, reqVO.getSsNr())
                .likeIfPresent(XzssDO::getCbr, reqVO.getCbr())
                .betweenIfPresent(XzssDO::getCbRq, reqVO.getCbRq())
                .betweenIfPresent(XzssDO::getSfyjgRq, reqVO.getSfyjgRq())
                .eqIfPresent(XzssDO::getIssupervise, reqVO.getIssupervise())
                .eqIfPresent(XzssDO::getMailTip, reqVO.getMailTip())
                .eqIfPresent(XzssDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(XzssDO::getCreateTime, reqVO.getCreateTime())
                .neIfPresent(XzssDO::getStatus, BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue());
        likeKeywords(wrapper, XzssDO::getSqr, reqVO.getSqr());
        likeKeywords(wrapper, XzssDO::getBsqr, reqVO.getBsqr());
        orderBy(reqVO, wrapper);
        return selectPage(reqVO, wrapper);
    }

    default void likeKeywords(LambdaQueryWrapperX<XzssDO> wrapper, SFunction<XzssDO, ?> column, String keyword) {
        List<String> keywords = BpmQueryUtils.splitKeywords(keyword);
        if (keywords.isEmpty()) {
            return;
        }
        wrapper.and(w -> keywords.forEach(item -> w.like(column, item)));
    }

    default void orderBy(XzssPageReqVO reqVO, LambdaQueryWrapperX<XzssDO> wrapper) {
        if (StrUtil.isBlank(reqVO.getOrderField()) || StrUtil.isBlank(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(XzssDO::getId);
            return;
        }
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        switch (reqVO.getOrderField()) {
            case "swWh":
                if (asc) wrapper.orderByAsc(XzssDO::getSwWh);
                else wrapper.orderByDesc(XzssDO::getSwWh);
                break;
            case "swJg":
                if (asc) wrapper.orderByAsc(XzssDO::getSwJg);
                else wrapper.orderByDesc(XzssDO::getSwJg);
                break;
            case "swRq":
                if (asc) wrapper.orderByAsc(XzssDO::getSwRq);
                else wrapper.orderByDesc(XzssDO::getSwRq);
                break;
            case "sqr":
                if (asc) wrapper.orderByAsc(XzssDO::getSqr);
                else wrapper.orderByDesc(XzssDO::getSqr);
                break;
            case "bsqr":
                if (asc) wrapper.orderByAsc(XzssDO::getBsqr);
                else wrapper.orderByDesc(XzssDO::getBsqr);
                break;
            case "dsr":
                if (asc) wrapper.orderByAsc(XzssDO::getDsr);
                else wrapper.orderByDesc(XzssDO::getDsr);
                break;
            case "ssLx":
                if (asc) wrapper.orderByAsc(XzssDO::getSsLx);
                else wrapper.orderByDesc(XzssDO::getSsLx);
                break;
            case "lb1":
                if (asc) wrapper.orderByAsc(XzssDO::getLb1);
                else wrapper.orderByDesc(XzssDO::getLb1);
                break;
            case "lb2":
                if (asc) wrapper.orderByAsc(XzssDO::getLb2);
                else wrapper.orderByDesc(XzssDO::getLb2);
                break;
            case "lb3":
                if (asc) wrapper.orderByAsc(XzssDO::getLb3);
                else wrapper.orderByDesc(XzssDO::getLb3);
                break;
            case "lb4":
                if (asc) wrapper.orderByAsc(XzssDO::getLb4);
                else wrapper.orderByDesc(XzssDO::getLb4);
                break;
            case "lb5":
                if (asc) wrapper.orderByAsc(XzssDO::getLb5);
                else wrapper.orderByDesc(XzssDO::getLb5);
                break;
            default:
                wrapper.orderByDesc(XzssDO::getId);
        }
    }

}
