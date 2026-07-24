package cn.iocoder.yudao.module.bpm.dal.mysql.xzfy;

import java.util.*;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.util.BpmQueryUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
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
                .likeIfPresent(XzfyDO::getDsr, reqVO.getDsr())
                .likeIfPresent(XzfyDO::getTdZl, reqVO.getTdZl())
                .eqIfPresent(XzfyDO::getLb1, reqVO.getLb1())
                .eqIfPresent(XzfyDO::getLb2, reqVO.getLb2())
                .eqIfPresent(XzfyDO::getLb3, reqVO.getLb3())
                .neIfPresent(XzfyDO::getStatus, BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue());
        applyEffectiveStatusFilter(wrapper, reqVO.getStatus());
        likeKeywords(wrapper, XzfyDO::getSqr, reqVO.getSqr());
        orderBy(reqVO, wrapper);
        return selectPage(reqVO, wrapper);
    }

    default void applyEffectiveStatusFilter(LambdaQueryWrapperX<XzfyDO> wrapper, Short status) {
        if (status == null) {
            return;
        }
        if (Objects.equals(status, BpmTaskStatusEnum.APPROVE.getStatus().shortValue())) {
            wrapper.and(w -> w.eq(XzfyDO::getStatus, status).or().apply(
                    "EXISTS (SELECT 1 FROM hist_wf.biz_project_map m " +
                            "JOIN hist_wf.proinst hp ON hp.project_id = m.project_id " +
                            "WHERE m.business_type = 'xzfy' AND m.bizinst_guid = t_xzfy_list.xm_guid " +
                            "AND (hp.proinst_status IN (2, 8) OR hp.end_date IS NOT NULL))"));
            return;
        }
        if (Objects.equals(status, BpmTaskStatusEnum.RUNNING.getStatus().shortValue())) {
            wrapper.and(w -> w.eq(XzfyDO::getStatus, status).or().apply(
                    "EXISTS (SELECT 1 FROM hist_wf.biz_project_map m " +
                            "JOIN hist_wf.proinst hp ON hp.project_id = m.project_id " +
                            "WHERE m.business_type = 'xzfy' AND m.bizinst_guid = t_xzfy_list.xm_guid " +
                            "AND hp.proinst_status NOT IN (2, 8) AND hp.end_date IS NULL)"));
            return;
        }
        wrapper.eq(XzfyDO::getStatus, status);
    }

    default void likeKeywords(LambdaQueryWrapperX<XzfyDO> wrapper, SFunction<XzfyDO, ?> column, String keyword) {
        List<String> keywords = BpmQueryUtils.splitKeywords(keyword);
        if (keywords.isEmpty()) {
            return;
        }
        wrapper.and(w -> keywords.forEach(item -> w.like(column, item)));
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
