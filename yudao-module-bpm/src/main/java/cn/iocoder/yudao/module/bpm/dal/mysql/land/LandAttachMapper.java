package cn.iocoder.yudao.module.bpm.dal.mysql.land;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandAttachDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LandAttachMapper extends BaseMapperX<LandAttachDO> {
    default List<LandAttachDO> selectByLandId(Long landId) {
        return selectList(new LambdaQueryWrapperX<LandAttachDO>().eq(LandAttachDO::getLandId, landId)
                .orderByAsc(LandAttachDO::getSort, LandAttachDO::getId));
    }
}
