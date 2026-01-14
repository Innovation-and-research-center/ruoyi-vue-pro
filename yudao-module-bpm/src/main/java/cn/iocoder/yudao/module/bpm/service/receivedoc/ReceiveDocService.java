package cn.iocoder.yudao.module.bpm.service.receivedoc;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 收文 Service 接口
 *
 * @author 芋道
 */
public interface ReceiveDocService {

    /**
     * 创建收文
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createReceiveDoc(Long userId,@Valid ReceiveDocSaveReqVO createReqVO);

    Long saveReceiveDoc(Long userId,@Valid ReceiveDocSaveReqVO createReqVO);

    String generateDocumentSequence(ReceiveDocCreateNumberVO createReqVO);

    Long generateDocumentSequence(String docClass);

    void createFlowReceiveDoc(Long userId,@Valid ReceiveDocSaveReqVO createReqVO);

    /**
     * 更新收文
     *
     * @param updateReqVO 更新信息
     */
    void updateReceiveDoc(@Valid ReceiveDocSaveReqVO updateReqVO);

    /**
     * 删除收文
     *
     * @param id 编号
     */
    void deleteReceiveDoc(Long id);

    /**
    * 批量删除收文
    *
    * @param ids 编号
    */
    void deleteReceiveDocListByIds(List<Long> ids);

    /**
     * 获得收文
     *
     * @param id 编号
     * @return 收文
     */
    ReceiveDocDO getReceiveDoc(Long id);

    /**
     * 获得收文分页
     *
     * @param pageReqVO 分页查询
     * @return 收文分页
     */
    PageResult<ReceiveDocDO> getReceiveDocPage(ReceiveDocPageReqVO pageReqVO);

    List<ReceiveFileRespVO> getReceiveDocAttachListByReceiveDocId(Long receiveDocId);

}