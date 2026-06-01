package cn.iocoder.yudao.module.bpm.service.processfile;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.bpm.dal.dataobject.processfile.BpmProcessFileDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.processfile.BpmProcessFileMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain.TimeExplainAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain.TimeExplainMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS;

@Service
@Validated
public class BpmProcessFileServiceImpl implements BpmProcessFileService {

    @Resource
    private BpmProcessFileMapper processFileMapper;
    @Resource
    private ReceiveDocMapper receiveDocMapper;
    @Resource
    private ReceiveDocAttachMapper receiveDocAttachMapper;
    @Resource
    private ConfflowMapper confflowMapper;
    @Resource
    private ConfflowAttachMapper confflowAttachMapper;
    @Resource
    private LeaveMapper leaveMapper;
    @Resource
    private LeaveAttachMapper leaveAttachMapper;
    @Resource
    private TimeExplainMapper timeExplainMapper;
    @Resource
    private TimeExplainAttachMapper timeExplainAttachMapper;
    @Resource
    private FileMapper fileMapper;

    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmTaskService taskService;

    @Override
    public void save(String filePath, String processInstanceId) {
        BpmProcessFileDO entity = BpmProcessFileDO.builder()
                .filePath(filePath)
                .processInstanceId(processInstanceId)
                .build();
        processFileMapper.insert(entity);
    }

    @Override
    public BpmProcessFileDO getByFilePath(String filePath) {
        return processFileMapper.selectOne(BpmProcessFileDO::getFilePath, filePath);
    }

    @Override
    public boolean canAccess(Long userId, String fileUrl) {
        String processInstanceId = findProcessInstanceByFileUrl(fileUrl);
        if (processInstanceId == null) return false;
        return isUserInvolved(userId.toString(), processInstanceId);
    }

    /** 从文件 URL 反查所属流程实例 */
    private String findProcessInstanceByFileUrl(String fileUrl) {
        // 1. BPM 生成文件关联（阅办单等 Word 文档）
        BpmProcessFileDO pf = processFileMapper.selectOne(BpmProcessFileDO::getFilePath, fileUrl);
        if (pf != null) return pf.getProcessInstanceId();

        // 2. 反查 FileDO
        FileDO fileDO = fileMapper.selectOne(FileDO::getUrl, fileUrl);
        if (fileDO == null) return null;

        // 3. 收文附件
        ReceiveDocAttachDO rdAttach = receiveDocAttachMapper.selectOne(
                ReceiveDocAttachDO::getAttachFileId, fileDO.getId());
        if (rdAttach != null && rdAttach.getReceiveDocId() != null) {
            ReceiveDocDO doc = receiveDocMapper.selectById(rdAttach.getReceiveDocId());
            if (doc != null) return doc.getProcessInstanceId();
        }

        // 4. 会议报告单附件（无 attachFileId，用 filePath 匹配）
        ConfflowAttachDO cfAttach = confflowAttachMapper.selectOne(
                ConfflowAttachDO::getFilePath, fileUrl);
        if (cfAttach != null && cfAttach.getCommId() != null) {
            ConfflowDO cfDoc = confflowMapper.selectById(cfAttach.getCommId());
            if (cfDoc != null) return cfDoc.getProcessInstanceId();
        }

        // 5. 请假附件
        LeaveAttachDO lvAttach = leaveAttachMapper.selectOne(
                LeaveAttachDO::getAttachFileId, fileDO.getId());
        if (lvAttach != null && lvAttach.getLeaveId() != null) {
            LeaveDO leaveDoc = leaveMapper.selectById(lvAttach.getLeaveId());
            if (leaveDoc != null) return leaveDoc.getProcessInstanceId();
        }

        // 6. 外出附件
        TimeExplainAttachDO teAttach = timeExplainAttachMapper.selectOne(
                TimeExplainAttachDO::getAttachFileId, fileDO.getId());
        if (teAttach != null && teAttach.getTimeExplainId() != null) {
            TimeExplainDO teDoc = timeExplainMapper.selectById(teAttach.getTimeExplainId());
            if (teDoc != null) return teDoc.getProcessInstanceId();
        }

        return null;
    }

    /** 检查用户是否参与流程（发起人或审批人） */
    private boolean isUserInvolved(String userId, String processInstanceId) {
        HistoricProcessInstance pi = processInstanceService.getHistoricProcessInstance(processInstanceId);
        if (pi == null) throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        if (pi.getStartUserId() != null && pi.getStartUserId().equals(userId)) return true;

        List<HistoricTaskInstance> tasks = taskService.getTaskListByProcessInstanceId(processInstanceId, true);
        if (CollUtil.isNotEmpty(tasks)) {
            for (HistoricTaskInstance task : tasks) {
                if (task.getAssignee() != null && task.getAssignee().equals(userId)) return true;
            }
        }
        return false;
    }
}
