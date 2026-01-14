package cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收文附件 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface ReceiveDocAttachMapper extends BaseMapperX<ReceiveDocAttachDO> {

    default List<ReceiveDocAttachDO> selectListByReceiveDocId(Long receiveDocId) {
        return selectList(ReceiveDocAttachDO::getReceiveDocId, receiveDocId);
    }

    default int deleteByReceiveDocId(Long receiveDocId) {
        return delete(ReceiveDocAttachDO::getReceiveDocId, receiveDocId);
    }

	default int deleteByReceiveDocIds(List<Long> receiveDocIds) {
	    return deleteBatch(ReceiveDocAttachDO::getReceiveDocId, receiveDocIds);
	}

}