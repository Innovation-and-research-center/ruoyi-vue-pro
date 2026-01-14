package cn.iocoder.yudao.module.bpm.dal.mysql.xzfy;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyKzDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行政复议扩展 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface XzfyKzMapper extends BaseMapperX<XzfyKzDO> {

    default XzfyKzDO selectByXmGuid(String xmGuid) {
        return selectOne(XzfyKzDO::getXmGuid, xmGuid);
    }

    default int deleteByXmGuid(String xmGuid) {
        return delete(XzfyKzDO::getXmGuid, xmGuid);
    }

	default int deleteByXmGuids(List<String> xmGuids) {
	    return deleteBatch(XzfyKzDO::getXmGuid, xmGuids);
	}

}