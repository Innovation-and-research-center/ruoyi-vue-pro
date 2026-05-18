package cn.iocoder.yudao.module.bpm.dal.mysql.confflow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowAttachDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ConfflowAttachMapper extends BaseMapperX<ConfflowAttachDO> {

    default List<ConfflowAttachDO> selectListByCommId(Long commId) {
        return selectList(ConfflowAttachDO::getCommId, commId);
    }
}
