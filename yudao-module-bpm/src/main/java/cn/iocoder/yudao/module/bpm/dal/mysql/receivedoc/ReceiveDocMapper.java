package cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc;

import java.util.*;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.util.BpmQueryUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("<script>" +
            "SELECT r.process_instance_id AS \"processInstanceId\", " +
            "CASE WHEN count(f.id) = 0 THEN '" + WEB_CREATE_SOURCE + "' " +
            "WHEN max(NULLIF(f.operation_information, '')) IS NULL THEN '" + UNKNOWN_SOURCE + "' " +
            "ELSE max(NULLIF(f.operation_information, '')) END AS source " +
            "FROM bpm_receive_doc r " +
            "LEFT JOIN t_file_exchange f ON f.doc_id = r.id AND COALESCE(f.deleted, 0) = 0 " +
            "WHERE r.process_instance_id IN " +
            "<foreach collection='processInstanceIds' item='processInstanceId' open='(' separator=',' close=')'>#{processInstanceId}</foreach> " +
            "AND COALESCE(r.deleted, 0) = 0 " +
            "GROUP BY r.process_instance_id" +
            "</script>")
    List<Map<String, Object>> selectSourceByProcessInstanceIds(@Param("processInstanceIds") Collection<String> processInstanceIds);

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
        likeSubjectKeywords(wrapper, reqVO.getSubject());
        wrapper.eqIfPresent(ReceiveDocDO::getUrgencyDegree, reqVO.getUrgencyDegree());
        likeDictValueOrLabel(wrapper, ReceiveDocDO::getDocSecondClass, reqVO.getDocSecondClass(), DICT_TYPE_DOC_CLASS);
        wrapper.neIfPresent(ReceiveDocDO::getStatus, BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue());
        applyEffectiveStatusFilter(wrapper, reqVO.getStatus());

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
        orderBy(reqVO, wrapper);

        PageResult<ReceiveDocDO> pageResult = selectJoinPage(reqVO, ReceiveDocDO.class, wrapper);
        pageResult.getList().forEach(item -> {
            if (item.getSource() == null) {
                item.setSource(WEB_CREATE_SOURCE);
            }
        });
        return pageResult;
    }

    default void applyEffectiveStatusFilter(MPJLambdaWrapperX<ReceiveDocDO> wrapper, Short status) {
        if (status == null) {
            return;
        }
        if (Objects.equals(status, BpmTaskStatusEnum.APPROVE.getStatus().shortValue())) {
            wrapper.and(w -> w.eq(ReceiveDocDO::getStatus, status)
                    .or()
                    .apply("t.project_id IS NOT NULL AND t.project_id <> '' AND EXISTS (" +
                            "SELECT 1 FROM hist_wf.proinst hp WHERE hp.project_id = t.project_id " +
                            "AND (hp.proinst_status IN (2, 8) OR hp.end_date IS NOT NULL))"));
            return;
        }
        if (Objects.equals(status, BpmTaskStatusEnum.RUNNING.getStatus().shortValue())) {
            wrapper.and(w -> w.eq(ReceiveDocDO::getStatus, status)
                    .or()
                    .apply("t.project_id IS NOT NULL AND t.project_id <> '' AND EXISTS (" +
                            "SELECT 1 FROM hist_wf.proinst hp WHERE hp.project_id = t.project_id " +
                            "AND hp.proinst_status NOT IN (2, 8) AND hp.end_date IS NULL)"));
            return;
        }
        wrapper.eq(ReceiveDocDO::getStatus, status);
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

    default void likeSubjectKeywords(MPJLambdaWrapperX<ReceiveDocDO> wrapper, String subject) {
        if (StrUtil.isBlank(subject)) {
            return;
        }
        List<String> keywords = BpmQueryUtils.splitKeywords(subject);
        if (keywords.isEmpty()) {
            return;
        }
        wrapper.and(w -> keywords.forEach(keyword -> w.like(ReceiveDocDO::getSubject, keyword)));
    }

    default void orderBy(ReceiveDocPageReqVO reqVO, MPJLambdaWrapperX<ReceiveDocDO> wrapper) {
        if (StrUtil.isBlank(reqVO.getOrderField()) || StrUtil.isBlank(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(ReceiveDocDO::getId);
            return;
        }
        boolean asc = "asc".equalsIgnoreCase(reqVO.getOrderDirection());
        switch (reqVO.getOrderField()) {
            case "subject":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getSubject);
                else wrapper.orderByDesc(ReceiveDocDO::getSubject);
                break;
            case "docClass":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getDocClass);
                else wrapper.orderByDesc(ReceiveDocDO::getDocClass);
                break;
            case "receiveDocNumber":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getReceiveDocNumber);
                else wrapper.orderByDesc(ReceiveDocDO::getReceiveDocNumber);
                break;
            case "source":
                if (asc) wrapper.orderByAsc(FileExchangeDO::getOperationInformation);
                else wrapper.orderByDesc(FileExchangeDO::getOperationInformation);
                break;
            case "sendDept":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getSendDept);
                else wrapper.orderByDesc(ReceiveDocDO::getSendDept);
                break;
            case "sendDocNumber":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getSendDocNumber);
                else wrapper.orderByDesc(ReceiveDocDO::getSendDocNumber);
                break;
            case "docSecondClass":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getDocSecondClass);
                else wrapper.orderByDesc(ReceiveDocDO::getDocSecondClass);
                break;
            case "urgencyDegree":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getUrgencyDegree);
                else wrapper.orderByDesc(ReceiveDocDO::getUrgencyDegree);
                break;
            case "receiveTime":
                if (asc) wrapper.orderByAsc(ReceiveDocDO::getReceiveTime);
                else wrapper.orderByDesc(ReceiveDocDO::getReceiveTime);
                break;
            default:
                wrapper.orderByDesc(ReceiveDocDO::getId);
        }
    }

}
