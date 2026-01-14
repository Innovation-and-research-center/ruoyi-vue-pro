package cn.iocoder.yudao.module.bpm.dal.mysql.xzss;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssKzDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 行政诉讼拓展 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface XzssKzMapper extends BaseMapperX<XzssKzDO> {

    default XzssKzDO selectByXmGuid(String xmGuid) {
        return selectOne(XzssKzDO::getXmGuid, xmGuid);
    }

    default int deleteByXmGuid(String xmGuid) {
        return delete(XzssKzDO::getXmGuid, xmGuid);
    }

	default int deleteByXmGuids(List<String> xmGuids) {
	    return deleteBatch(XzssKzDO::getXmGuid, xmGuids);
	}

}