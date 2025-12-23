package cn.iocoder.yudao.module.bpm.dal.mysql.leave;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.*;

/**
 * 假期申请审批 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface LeaveMapper extends BaseMapperX<LeaveDO> {

    default PageResult<LeaveDO> selectPage(LeavePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LeaveDO>()
                .betweenIfPresent(LeaveDO::getApplyDate, reqVO.getApplyDate())
                .betweenIfPresent(LeaveDO::getQxjStartDate, reqVO.getQxjStartDate())
                .betweenIfPresent(LeaveDO::getQxjEndDate, reqVO.getQxjEndDate())
                .eqIfPresent(LeaveDO::getQxjType, reqVO.getQxjType())
                .eqIfPresent(LeaveDO::getTotalTs, reqVO.getTotalTs())
                .eqIfPresent(LeaveDO::getFilepath, reqVO.getFilepath())
                .orderByDesc(LeaveDO::getId));
    }

}