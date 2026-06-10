package cn.iocoder.yudao.module.bpm.dal.mysql.xzss;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
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
        return selectPage(reqVO, new LambdaQueryWrapperX<XzssDO>()
                .likeIfPresent(XzssDO::getSwWh, reqVO.getSwWh())
                .likeIfPresent(XzssDO::getSwJg, reqVO.getSwJg())
                .betweenIfPresent(XzssDO::getSwRq, reqVO.getSwRq())
                .likeIfPresent(XzssDO::getSqr, reqVO.getSqr())
                .likeIfPresent(XzssDO::getBsqr, reqVO.getBsqr())
                .likeIfPresent(XzssDO::getDsr, reqVO.getDsr())
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
                .orderByDesc(XzssDO::getId));
    }

}
