package cn.iocoder.yudao.module.bpm.dal.mysql.land;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LandMapper extends BaseMapperX<LandDO> {
    default LandDO selectForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<LandDO>().eq(LandDO::getId, id).last("FOR UPDATE"));
    }
}
