package cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;

/**
 * 收文 Mapper
 *
 * @author 芋道
 */
@Mapper
public interface ReceiveDocMapper extends BaseMapperX<ReceiveDocDO> {

    default PageResult<ReceiveDocDO> selectPage(ReceiveDocPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ReceiveDocDO>()
                .eqIfPresent(ReceiveDocDO::getDocClass, reqVO.getDocClass())
                .eqIfPresent(ReceiveDocDO::getSendDept, reqVO.getSendDept())
                .eqIfPresent(ReceiveDocDO::getSendDocNumber, reqVO.getSendDocNumber())
                .eqIfPresent(ReceiveDocDO::getReceiveDocNumber, reqVO.getReceiveDocNumber())
                .betweenIfPresent(ReceiveDocDO::getReceiveTime, reqVO.getReceiveTime())
                .likeIfPresent(ReceiveDocDO::getSubject, reqVO.getSubject())
                .eqIfPresent(ReceiveDocDO::getUrgencyDegree, reqVO.getUrgencyDegree())
                .eqIfPresent(ReceiveDocDO::getDocSecondClass, reqVO.getDocSecondClass())
                .orderByDesc(ReceiveDocDO::getId));
    }

}