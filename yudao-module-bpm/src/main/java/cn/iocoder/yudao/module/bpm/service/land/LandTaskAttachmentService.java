package cn.iocoder.yudao.module.bpm.service.land;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandTaskAttachmentSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.commentattach.CommentAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.DelegationState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

/** 土地各节点复用行政复议的评论附件表，附件关联到具体任务，不覆盖登记附件。 */
@Service
@DataPermission(enable = false)
public class LandTaskAttachmentService {
    public static final String DOC_TYPE = "LAND";
    @Resource private LandService landService;
    @Resource private LandMapper landMapper;
    @Resource private CommentAttachMapper commentAttachMapper;
    @Resource private FileMapper fileMapper;
    @Resource private TaskService taskService;

    public List<CommentAttachDO> getList(Long userId, Long landId) {
        validateRead(userId, String.valueOf(landId));
        return commentAttachMapper.selectList(new LambdaQueryWrapper<CommentAttachDO>()
                .eq(CommentAttachDO::getDocId, String.valueOf(landId)).eq(CommentAttachDO::getDocType, DOC_TYPE)
                .orderByAsc(CommentAttachDO::getCreateTime, CommentAttachDO::getId));
    }

    public void validateRead(Long userId, String docId) {
        landService.get(userId, parseLandId(docId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, LandTaskAttachmentSaveReqVO request) {
        Task task = requireTask(userId, request.getLandId(), request.getTaskId());
        FileDO file = fileMapper.selectById(request.getFileId());
        if (file == null || !String.valueOf(userId).equals(file.getCreator())
                || StrUtil.isBlank(file.getName()) || StrUtil.isBlank(file.getPath())) {
            throw exception(LAND_ATTACHMENT_INVALID);
        }
        String path = StrUtil.blankToDefault(file.getUrl(), file.getPath());
        List<CommentAttachDO> previous = commentAttachMapper.selectList(new LambdaQueryWrapper<CommentAttachDO>()
                .eq(CommentAttachDO::getDocId, String.valueOf(request.getLandId()))
                .eq(CommentAttachDO::getDocType, DOC_TYPE).eq(CommentAttachDO::getTaskId, task.getId())
                .eq(CommentAttachDO::getFilepath, path));
        if (!previous.isEmpty()) return previous.get(0).getId();
        CommentAttachDO attachment = new CommentAttachDO().setTaskId(task.getId())
                .setDocId(String.valueOf(request.getLandId())).setDocType(DOC_TYPE)
                .setFilename(file.getName()).setFilepath(path).setFileextension(FileUtil.extName(file.getName()))
                .setCommentType(task.getName());
        attachment.setCreator(String.valueOf(userId));
        commentAttachMapper.insert(attachment);
        return attachment.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        CommentAttachDO attachment = commentAttachMapper.selectById(id);
        if (attachment == null || !DOC_TYPE.equals(attachment.getDocType())) throw exception(LAND_ATTACHMENT_INVALID);
        requireTask(userId, parseLandId(attachment.getDocId()), attachment.getTaskId());
        if (!String.valueOf(userId).equals(attachment.getCreator())) throw exception(LAND_ACCESS_DENIED);
        commentAttachMapper.deleteById(id);
    }

    private Task requireTask(Long userId, Long landId, String taskId) {
        LandDO land = landId == null ? null : landMapper.selectForUpdate(landId);
        if (land == null) throw exception(LAND_NOT_EXISTS);
        if (userId == null || !Objects.equals(land.getStatus(), BpmProcessInstanceStatusEnum.RUNNING.getStatus())
                || StrUtil.isBlank(taskId)) throw exception(LAND_ACCESS_DENIED);
        Task task = taskService.createTaskQuery().taskId(taskId).active().singleResult();
        if (task == null || !Objects.equals(task.getProcessInstanceId(), land.getProcessInstanceId())
                || !String.valueOf(userId).equals(task.getAssignee())
                || task.getDelegationState() == DelegationState.PENDING) throw exception(LAND_ACCESS_DENIED);
        return task;
    }

    private Long parseLandId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
        }
        throw exception(LAND_NOT_EXISTS);
    }
}
