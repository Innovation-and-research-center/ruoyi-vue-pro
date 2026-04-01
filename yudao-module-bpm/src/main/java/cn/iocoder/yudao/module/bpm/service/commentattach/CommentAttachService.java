package cn.iocoder.yudao.module.bpm.service.commentattach;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 评论附件 Service 接口
 *
 * @author 管理员
 */
public interface CommentAttachService {

    /**
     * 创建评论附件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCommentAttach(@Valid CommentAttachSaveReqVO createReqVO);

    /**
     * 更新评论附件
     *
     * @param updateReqVO 更新信息
     */
    void updateCommentAttach(@Valid CommentAttachSaveReqVO updateReqVO);

    /**
     * 删除评论附件
     *
     * @param id 编号
     */
    void deleteCommentAttach(Long id);

    /**
    * 批量删除评论附件
    *
    * @param ids 编号
    */
    void deleteCommentAttachListByIds(List<Long> ids);

    /**
     * 获得评论附件
     *
     * @param id 编号
     * @return 评论附件
     */
    CommentAttachDO getCommentAttach(Long id);

    /**
     * 获得评论附件分页
     *
     * @param pageReqVO 分页查询
     * @return 评论附件分页
     */
    PageResult<CommentAttachDO> getCommentAttachPage(CommentAttachPageReqVO pageReqVO);

    void saveCommentAttachList(String docId, String docType, List<CommentAttachDO> list);

    /**
     * 根据业务主键和类型获取附件列表
     *
     * @param docId   业务主键
     * @param docType 业务类型
     * @return 附件列表
     */
    List<CommentAttachDO> getCommentAttachList(String docId, String docType);

    /**
     * 根据业务主键和类型删除所有相关附件
     *
     * @param docId   业务主键
     * @param docType 业务类型
     */
    void deleteCommentAttach(String docId, String docType);

}