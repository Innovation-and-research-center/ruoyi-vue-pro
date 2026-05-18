package cn.iocoder.yudao.module.bpm.dal.mysql.leave;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveAttachDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface LeaveAttachMapper extends BaseMapperX<LeaveAttachDO> {

    default List<LeaveAttachDO> selectListByLeaveId(Long leaveId) {
        return selectList(LeaveAttachDO::getLeaveId, leaveId);
    }
}