package cn.iocoder.yudao.module.bpm.service.timeexplain;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 外出请假补假 Service 接口
 *
 * @author 管理员
 */
public interface TimeExplainService {

    /**
     * 创建外出请假补假
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTimeExplain(@Valid TimeExplainSaveReqVO createReqVO);

    Long createOut(Long userId,@Valid TimeExplainSaveReqVO createReqVO);

    Long saveOut(Long userId, @Valid TimeExplainSaveReqVO createReqVO);

    void createFlowOut(Long userId, @Valid TimeExplainSaveReqVO updateReqVO);

    /**
     * 更新外出请假补假
     *
     * @param updateReqVO 更新信息
     */
    void updateTimeExplain(@Valid TimeExplainSaveReqVO updateReqVO);

    /**
     * 删除外出请假补假
     *
     * @param id 编号
     */
    void deleteTimeExplain(Long id,String reason);

    /**
    * 批量删除外出请假补假
    *
    * @param ids 编号
    */
    void deleteTimeExplainListByIds(List<Long> ids,String reason);

    /**
     * 获得外出请假补假
     *
     * @param id 编号
     * @return 外出请假补假
     */
    TimeExplainDO getTimeExplain(Long id);

    /**
     * 获得外出请假补假分页
     *
     * @param pageReqVO 分页查询
     * @return 外出请假补假分页
     */
    PageResult<TimeExplainDO> getTimeExplainPage(TimeExplainPageReqVO pageReqVO);

    void updateTimeExplainStatus(Long id, Integer status);

    List<TimeExplainAttachRespVO> getTimeExplainAttachListByTimeExplainId(Long timeExplainId);

}
