package cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc;

import java.util.*;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;

/**
 * 收文 Mapper
 *
 * @author 芋道
 */
@Mapper
public interface ReceiveDocMapper extends BaseMapperX<ReceiveDocDO> {

    String WEB_CREATE_SOURCE = "网页新建";
    String UNKNOWN_SOURCE = "未知来源";
    String DICT_TYPE_AGENCY_NAME = "agency_name";
    String DICT_TYPE_DOC_CLASS = "doc_class";

    default PageResult<ReceiveDocDO> selectPage(ReceiveDocPageReqVO reqVO) {
        MPJLambdaWrapperX<ReceiveDocDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(ReceiveDocDO.class);
        wrapper.select("CASE WHEN t1.id IS NULL THEN '" + WEB_CREATE_SOURCE
                + "' WHEN t1.operation_information IS NULL OR t1.operation_information = '' THEN '" + UNKNOWN_SOURCE
                + "' ELSE t1.operation_information END AS source");
        wrapper.leftJoin(FileExchangeDO.class, FileExchangeDO::getDocId, ReceiveDocDO::getId);
        wrapper.eqIfPresent(ReceiveDocDO::getDocClass, reqVO.getDocClass());
        likeDictValueOrLabel(wrapper, ReceiveDocDO::getSendDept, reqVO.getSendDept(), DICT_TYPE_AGENCY_NAME);
        wrapper.likeIfPresent(ReceiveDocDO::getSendDocNumber, reqVO.getSendDocNumber());
        wrapper.likeIfPresent(ReceiveDocDO::getReceiveDocNumber, reqVO.getReceiveDocNumber());
        wrapper.betweenIfPresent(ReceiveDocDO::getReceiveTime, reqVO.getReceiveTime());
        wrapper.likeIfPresent(ReceiveDocDO::getSubject, reqVO.getSubject());
        wrapper.eqIfPresent(ReceiveDocDO::getUrgencyDegree, reqVO.getUrgencyDegree());
        likeDictValueOrLabel(wrapper, ReceiveDocDO::getDocSecondClass, reqVO.getDocSecondClass(), DICT_TYPE_DOC_CLASS);
        wrapper.eqIfPresent(ReceiveDocDO::getStatus, reqVO.getStatus());
        wrapper.orderByDesc(ReceiveDocDO::getId);

        if (StrUtil.isNotBlank(reqVO.getSource())) {
            if (WEB_CREATE_SOURCE.equals(reqVO.getSource())) {
                wrapper.isNull(FileExchangeDO::getId);
            } else if (UNKNOWN_SOURCE.equals(reqVO.getSource())) {
                wrapper.isNotNull(FileExchangeDO::getId);
                wrapper.and(w -> w.isNull(FileExchangeDO::getOperationInformation)
                        .or()
                        .eq(FileExchangeDO::getOperationInformation, ""));
            } else {
                wrapper.eq(FileExchangeDO::getOperationInformation, reqVO.getSource());
            }
        }

        PageResult<ReceiveDocDO> pageResult = selectJoinPage(reqVO, ReceiveDocDO.class, wrapper);
        pageResult.getList().forEach(item -> {
            if (item.getSource() == null) {
                item.setSource(WEB_CREATE_SOURCE);
            }
        });
        return pageResult;
    }

    default <S> void likeDictValueOrLabel(MPJLambdaWrapperX<ReceiveDocDO> wrapper, SFunction<S, ?> column,
                                          String keyword, String dictType) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        String label = DictFrameworkUtils.parseDictDataLabel(dictType, keyword);
        String value = DictFrameworkUtils.parseDictDataValue(dictType, keyword);

        wrapper.and(w -> {
            w.like(column, keyword);
            if (StrUtil.isNotBlank(label) && !StrUtil.equals(label, keyword)) {
                w.or().like(column, label);
            }
            if (StrUtil.isNotBlank(value) && !StrUtil.equals(value, keyword)) {
                w.or().like(column, value);
            }
        });
    }

}
