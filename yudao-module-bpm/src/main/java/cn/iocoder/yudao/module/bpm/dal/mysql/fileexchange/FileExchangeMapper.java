package cn.iocoder.yudao.module.bpm.dal.mysql.fileexchange;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo.*;

/**
 * 文件交换 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface FileExchangeMapper extends BaseMapperX<FileExchangeDO> {

    default PageResult<FileExchangeDO> selectPage(FileExchangePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FileExchangeDO>()
                .eqIfPresent(FileExchangeDO::getDocId, reqVO.getDocId())
                .eqIfPresent(FileExchangeDO::getDocunique, reqVO.getDocunique())
                .eqIfPresent(FileExchangeDO::getSendDocNumber, reqVO.getSendDocNumber())
                .eqIfPresent(FileExchangeDO::getSubject, reqVO.getSubject())
                .betweenIfPresent(FileExchangeDO::getOperationDate, reqVO.getOperationDate())
                .eqIfPresent(FileExchangeDO::getOperationType, reqVO.getOperationType())
                .eqIfPresent(FileExchangeDO::getOperationPerson, reqVO.getOperationPerson())
                .eqIfPresent(FileExchangeDO::getOperationInformation, reqVO.getOperationInformation())
                .betweenIfPresent(FileExchangeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(FileExchangeDO::getId));
    }

}