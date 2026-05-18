package cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainAttachDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface TimeExplainAttachMapper extends BaseMapperX<TimeExplainAttachDO> {

    default List<TimeExplainAttachDO> selectListByTimeExplainId(Long timeExplainId) {
        return selectList(TimeExplainAttachDO::getTimeExplainId, timeExplainId);
    }
}
