package cn.iocoder.yudao.module.bpm.dal.mysql.land;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandPageReqVO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

@Mapper
public interface LandMapper extends BaseMapperX<LandDO> {
    default PageResult<LandDO> selectPage(LandPageReqVO request) {
        LambdaQueryWrapperX<LandDO> wrapper = new LambdaQueryWrapperX<LandDO>()
                .likeIfPresent(LandDO::getProjectName, request.getProjectName())
                .likeIfPresent(LandDO::getLocation, request.getLocation())
                .likeIfPresent(LandDO::getStartUserName, request.getStartUserName())
                .eqIfPresent(LandDO::getStatus, request.getStatus())
                .betweenIfPresent(LandDO::getApplyDate, request.getApplyDate());
        wrapper.ne(LandDO::getStatus, BpmProcessInstanceStatusEnum.INVALID.getStatus());
        // 排序字段仅映射到固定属性，禁止把请求字段直接拼进 SQL。
        if ("asc".equals(request.getOrderDirection()) || "desc".equals(request.getOrderDirection())) {
            SFunction<LandDO, ?> column = null;
            switch (request.getOrderField() == null ? "" : request.getOrderField()) {
                case "projectName": column = LandDO::getProjectName; break;
                case "location": column = LandDO::getLocation; break;
                case "startUserName": column = LandDO::getStartUserName; break;
                case "deptName": column = LandDO::getDeptName; break;
                case "applyDate": column = LandDO::getApplyDate; break;
                case "status": column = LandDO::getStatus; break;
                default: break;
            }
            if (column != null) wrapper.orderBy(true, "asc".equals(request.getOrderDirection()), column);
        }
        wrapper.orderByDesc(LandDO::getId);
        return selectPage(request, wrapper);
    }

    default LandDO selectForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<LandDO>().eq(LandDO::getId, id).last("FOR UPDATE"));
    }
}
