package cn.iocoder.yudao.module.bpm.dal.mysql.xzfy;

import java.util.*;

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
        return selectPage(reqVO, new LambdaQueryWrapperX<XzfyDO>()
                .likeIfPresent(XzfyDO::getSwWh, reqVO.getSwWh())
                .likeIfPresent(XzfyDO::getSwJg, reqVO.getSwJg())
                .betweenIfPresent(XzfyDO::getSwRq, reqVO.getSwRq())
                .likeIfPresent(XzfyDO::getSqr, reqVO.getSqr())
                .likeIfPresent(XzfyDO::getDsr, reqVO.getDsr())
                .eqIfPresent(XzfyDO::getLb1, reqVO.getLb1())
                .eqIfPresent(XzfyDO::getLb2, reqVO.getLb2())
                .eqIfPresent(XzfyDO::getLb3, reqVO.getLb3())
                .eqIfPresent(XzfyDO::getStatus, reqVO.getStatus())
                .orderByDesc(XzfyDO::getId));
    }

}
