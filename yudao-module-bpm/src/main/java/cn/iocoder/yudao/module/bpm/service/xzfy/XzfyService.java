package cn.iocoder.yudao.module.bpm.service.xzfy;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyKzDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 行政复议 Service 接口
 *
 * @author 管理员
 */
public interface XzfyService {

    /**
     * 创建行政复议
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createXzfy(Long userId,@Valid XzfySaveReqVO createReqVO);

    /**
     * 更新行政复议
     *
     * @param updateReqVO 更新信息
     */
    void updateXzfy(@Valid XzfySaveReqVO updateReqVO);

    /**
     * 删除行政复议
     *
     * @param id 编号
     */
    void deleteXzfy(Long id,String reason);

    /**
    * 批量删除行政复议
    *
    * @param ids 编号
    */
    void deleteXzfyListByIds(List<Long> ids,String reason);

    void updateXzfyStatus(Long id, Integer status);

    /**
     * 获得行政复议
     *
     * @param id 编号
     * @return 行政复议
     */
    XzfyDO getXzfy(Long id);

    /**
     * 获得行政复议分页
     *
     * @param pageReqVO 分页查询
     * @return 行政复议分页
     */
    PageResult<XzfyDO> getXzfyPage(XzfyPageReqVO pageReqVO);

    // ==================== 子表（行政复议扩展） ====================

    /**
     * 获得行政复议扩展
     *
     * @param xmGuid 备用主键
     * @return 行政复议扩展
     */
    XzfyKzDO getXzfyKzByXmGuid(String xmGuid);

    List<XzfyDO> getXzfyListByXmGuid(String xmGuid);

    PageResult<XzfyDO> getUnlinkedXzfyPage(XzfyPageReqVO pageReqVO);



}