package cn.iocoder.yudao.module.bpm.service.fileexchange;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.fileexchange.FileExchangeMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

/**
 * 文件交换 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class FileExchangeServiceImpl implements FileExchangeService {

    @Resource
    private FileExchangeMapper fileExchangeMapper;

    @Override
    public Long createFileExchange(FileExchangeSaveReqVO createReqVO) {
        // 插入
        FileExchangeDO fileExchange = BeanUtils.toBean(createReqVO, FileExchangeDO.class);
        fileExchangeMapper.insert(fileExchange);

        // 返回
        return fileExchange.getId();
    }

    @Override
    public void updateFileExchange(FileExchangeSaveReqVO updateReqVO) {
        // 校验存在
        validateFileExchangeExists(updateReqVO.getId());
        // 更新
        FileExchangeDO updateObj = BeanUtils.toBean(updateReqVO, FileExchangeDO.class);
        fileExchangeMapper.updateById(updateObj);
    }

    @Override
    public void deleteFileExchange(Long id) {
        // 校验存在
        validateFileExchangeExists(id);
        // 删除
        fileExchangeMapper.deleteById(id);
    }

    @Override
        public void deleteFileExchangeListByIds(List<Long> ids) {
        // 删除
        fileExchangeMapper.deleteByIds(ids);
        }


    private void validateFileExchangeExists(Long id) {
        if (fileExchangeMapper.selectById(id) == null) {
            throw exception(FILE_EXCHANGE_NOT_EXISTS);
        }
    }

    @Override
    public FileExchangeDO getFileExchange(Long id) {
        return fileExchangeMapper.selectById(id);
    }

    @Override
    public PageResult<FileExchangeDO> getFileExchangePage(FileExchangePageReqVO pageReqVO) {
        return fileExchangeMapper.selectPage(pageReqVO);
    }

}