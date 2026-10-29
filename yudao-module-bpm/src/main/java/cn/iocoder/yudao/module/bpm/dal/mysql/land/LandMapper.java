package cn.iocoder.yudao.module.bpm.dal.mysql.land;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;

@Mapper
public interface LandMapper extends BaseMapperX<LandDO> {
    default PageResult<LandDO> selectPage(LandPageReqVO request) {
        return selectPage(request, new LambdaQueryWrapperX<LandDO>()
                .likeIfPresent(LandDO::getProjectName, request.getProjectName())
                .likeIfPresent(LandDO::getLocation, request.getLocation())
                .likeIfPresent(LandDO::getStartUserName, request.getStartUserName())
                .eqIfPresent(LandDO::getStatus, request.getStatus())
                .betweenIfPresent(LandDO::getApplyDate, request.getApplyDate())
                .ne(LandDO::getStatus, BpmProcessInstanceStatusEnum.INVALID.getStatus())
                .orderByDesc(LandDO::getId));
    }

    default LandDO selectForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<LandDO>().eq(LandDO::getId, id).last("FOR UPDATE"));
    }
}
