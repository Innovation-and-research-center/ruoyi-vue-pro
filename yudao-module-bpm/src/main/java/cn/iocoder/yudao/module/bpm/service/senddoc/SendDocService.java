package cn.iocoder.yudao.module.bpm.service.senddoc;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.senddoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.senddoc.SendDocDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 发文 Service 接口
 *
 * @author 管理员
 */
public interface SendDocService {

    /**
     * 创建发文
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSendDoc(Long userId,@Valid SendDocSaveReqVO createReqVO);

    /**
     * 更新发文
     *
     * @param updateReqVO 更新信息
     */
    void updateSendDoc(@Valid SendDocSaveReqVO updateReqVO);

    /**
     * 删除发文
     *
     * @param id 编号
     */
    void deleteSendDoc(Long id);

    /**
    * 批量删除发文
    *
    * @param ids 编号
    */
    void deleteSendDocListByIds(List<Long> ids);

    /**
     * 获得发文
     *
     * @param id 编号
     * @return 发文
     */
    SendDocDO getSendDoc(Long id);

    /**
     * 获得发文分页
     *
     * @param pageReqVO 分页查询
     * @return 发文分页
     */
    PageResult<SendDocDO> getSendDocPage(SendDocPageReqVO pageReqVO);

}