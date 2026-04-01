package cn.iocoder.yudao.module.bpm.dal.mysql.commentattach;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo.*;

/**
 * 评论附件 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface CommentAttachMapper extends BaseMapperX<CommentAttachDO> {

    default PageResult<CommentAttachDO> selectPage(CommentAttachPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CommentAttachDO>()
                .eqIfPresent(CommentAttachDO::getTaskId, reqVO.getTaskId())
                .eqIfPresent(CommentAttachDO::getFilepath, reqVO.getFilepath())
                .likeIfPresent(CommentAttachDO::getFilename, reqVO.getFilename())
                .eqIfPresent(CommentAttachDO::getFileextension, reqVO.getFileextension())
                .eqIfPresent(CommentAttachDO::getDocType, reqVO.getDocType())
                .eqIfPresent(CommentAttachDO::getDocId, reqVO.getDocId())
                .eqIfPresent(CommentAttachDO::getCommentType, reqVO.getCommentType())
                .betweenIfPresent(CommentAttachDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CommentAttachDO::getId));
    }

}