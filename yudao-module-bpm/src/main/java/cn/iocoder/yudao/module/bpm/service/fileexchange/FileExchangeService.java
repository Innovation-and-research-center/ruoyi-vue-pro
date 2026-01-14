package cn.iocoder.yudao.module.bpm.service.fileexchange;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 文件交换 Service 接口
 *
 * @author 管理员
 */
public interface FileExchangeService {

    /**
     * 创建文件交换
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFileExchange(@Valid FileExchangeSaveReqVO createReqVO);

    /**
     * 更新文件交换
     *
     * @param updateReqVO 更新信息
     */
    void updateFileExchange(@Valid FileExchangeSaveReqVO updateReqVO);

    /**
     * 删除文件交换
     *
     * @param id 编号
     */
    void deleteFileExchange(Long id);

    /**
    * 批量删除文件交换
    *
    * @param ids 编号
    */
    void deleteFileExchangeListByIds(List<Long> ids);

    /**
     * 获得文件交换
     *
     * @param id 编号
     * @return 文件交换
     */
    FileExchangeDO getFileExchange(Long id);

    /**
     * 获得文件交换分页
     *
     * @param pageReqVO 分页查询
     * @return 文件交换分页
     */
    PageResult<FileExchangeDO> getFileExchangePage(FileExchangePageReqVO pageReqVO);

}