package cn.iocoder.yudao.module.bpm.service.commentattach;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.module.bpm.service.land.LandTaskAttachmentService;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.commentattach.CommentAttachMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

/**
 * 评论附件 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class CommentAttachServiceImpl implements CommentAttachService {

    @Resource
    private CommentAttachMapper commentAttachMapper;

    @Resource
    private LandTaskAttachmentService landTaskAttachmentService;

    @Override
    public Long createCommentAttach(CommentAttachSaveReqVO createReqVO) {
        if (LandTaskAttachmentService.DOC_TYPE.equals(createReqVO.getDocType())) throw exception(LAND_ACCESS_DENIED);
        // 插入
        CommentAttachDO commentAttach = BeanUtils.toBean(createReqVO, CommentAttachDO.class);
        commentAttachMapper.insert(commentAttach);

        // 返回
        return commentAttach.getId();
    }

    @Override
    public void updateCommentAttach(CommentAttachSaveReqVO updateReqVO) {
        // 校验存在
        validateCommentAttachExists(updateReqVO.getId());
        if (LandTaskAttachmentService.DOC_TYPE.equals(updateReqVO.getDocType())
                || LandTaskAttachmentService.DOC_TYPE.equals(commentAttachMapper.selectById(updateReqVO.getId()).getDocType())) {
            throw exception(LAND_ACCESS_DENIED);
        }
        // 更新
        CommentAttachDO updateObj = BeanUtils.toBean(updateReqVO, CommentAttachDO.class);
        commentAttachMapper.updateById(updateObj);
    }

    @Override
    public void deleteCommentAttach(Long id) {
        // 校验存在
        validateCommentAttachExists(id);
        if (LandTaskAttachmentService.DOC_TYPE.equals(commentAttachMapper.selectById(id).getDocType())) {
            landTaskAttachmentService.delete(getLoginUserId(), id);
            return;
        }
        // 删除
        commentAttachMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
        public void deleteCommentAttachListByIds(List<Long> ids) {
        // 删除
        for (CommentAttachDO attachment : commentAttachMapper.selectBatchIds(ids)) {
            if (LandTaskAttachmentService.DOC_TYPE.equals(attachment.getDocType())) throw exception(LAND_ACCESS_DENIED);
        }
        commentAttachMapper.deleteByIds(ids);
        }


    private void validateCommentAttachExists(Long id) {
        if (commentAttachMapper.selectById(id) == null) {
            throw exception(COMMENT_ATTACH_NOT_EXISTS);
        }
    }

    @Override
    public CommentAttachDO getCommentAttach(Long id) {
        CommentAttachDO attachment = commentAttachMapper.selectById(id);
        if (attachment != null && LandTaskAttachmentService.DOC_TYPE.equals(attachment.getDocType())) {
            landTaskAttachmentService.validateRead(getLoginUserId(), attachment.getDocId());
        }
        return attachment;
    }

    @Override
    public PageResult<CommentAttachDO> getCommentAttachPage(CommentAttachPageReqVO pageReqVO) {
        PageResult<CommentAttachDO> page = commentAttachMapper.selectPage(pageReqVO);
        page.getList().stream().filter(a -> LandTaskAttachmentService.DOC_TYPE.equals(a.getDocType()))
                .map(CommentAttachDO::getDocId).distinct()
                .forEach(docId -> landTaskAttachmentService.validateRead(getLoginUserId(), docId));
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCommentAttachList(String docId, String docType, List<CommentAttachDO> list) {
        if (LandTaskAttachmentService.DOC_TYPE.equals(docType)) throw exception(LAND_ACCESS_DENIED);
        if (list == null) {
            list = new ArrayList<>();
        }

        // 核心修改：遍历时同时设置 docId 和 docType
        list.forEach(o -> o.setDocId(docId).setDocType(docType).clean());

        // 核心修改：查询老数据时，带上 docType 条件
        List<CommentAttachDO> oldList = commentAttachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentAttachDO>()
                        .eq(CommentAttachDO::getDocId, docId)
                        .eq(CommentAttachDO::getDocType, docType)
        );

        // 比对新老数据，得出需要新增、修改、删除的集合 (这部分逻辑不变)
        List<List<CommentAttachDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = cn.hutool.core.util.ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).clean();
            }
            return same;
        });

        // 批量执行操作
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            commentAttachMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            commentAttachMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            commentAttachMapper.deleteByIds(convertList(diffList.get(2), CommentAttachDO::getId));
        }
    }

    @Override
    public List<CommentAttachDO> getCommentAttachList(String docId, String docType) {
        if (LandTaskAttachmentService.DOC_TYPE.equals(docType)) {
            landTaskAttachmentService.validateRead(getLoginUserId(), docId);
        }
        // 核心修改：追加 docType 过滤
        return commentAttachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentAttachDO>()
                        .eq(CommentAttachDO::getDocId, docId)
                        .eq(CommentAttachDO::getDocType, docType)
                        .eq(CommentAttachDO::getDeleted, false)
        );
    }

    @Override
    public void deleteCommentAttach(String docId, String docType) {
        if (LandTaskAttachmentService.DOC_TYPE.equals(docType)) throw exception(LAND_ACCESS_DENIED);
        // 核心修改：追加 docType 过滤
        commentAttachMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CommentAttachDO>()
                        .eq(CommentAttachDO::getDocId, docId)
                        .eq(CommentAttachDO::getDocType, docType)
        );
    }

}