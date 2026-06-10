package cn.iocoder.yudao.module.bpm.service.confflow;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 会议报告单 Service 接口
 *
 * @author 芋道源码
 */
public interface ConfflowService {

    /**
     * 创建会议报告单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createConfflow(Long userId, @Valid ConfflowSaveReqVO createReqVO);

    Long saveConfflow(Long userId, @Valid ConfflowSaveReqVO createReqVO);

    void createFlowConfflow(Long userId, @Valid ConfflowSaveReqVO updateReqVO);

    /**
     * 更新会议报告单
     *
     * @param updateReqVO 更新信息
     */
    void updateConfflow(@Valid ConfflowSaveReqVO updateReqVO);

    /**
     * 删除会议报告单
     *
     * @param id 编号
     */
    void deleteConfflow(Long id,String reason);

    /**
    * 批量删除会议报告单
    *
    * @param ids 编号
    */
    void deleteConfflowListByIds(List<Long> ids,String reason);

    /**
     * 获得会议报告单
     *
     * @param id 编号
     * @return 会议报告单
     */
    ConfflowDO getConfflow(Long id);

    /**
     * 获得会议报告单分页
     *
     * @param pageReqVO 分页查询
     * @return 会议报告单分页
     */
    PageResult<ConfflowDO> getConfflowPage(ConfflowPageReqVO pageReqVO);

    void updateConfflowStatus(Long id, Integer status);

    List<ConfflowAttachRespVO> getConfflowAttachListByCommId(Long commId);

}
