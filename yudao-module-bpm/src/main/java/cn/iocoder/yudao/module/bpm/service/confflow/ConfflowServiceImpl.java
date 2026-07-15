package cn.iocoder.yudao.module.bpm.service.confflow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowAttachMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.task.BpmRegisterTaskService;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.CONFLOW_REPORT;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;

/**
 * 会议报告单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ConfflowServiceImpl implements ConfflowService {

    public static final String PROCESS_KEY = CONFLOW_REPORT;
    private static final short DRAFT_STATUS = 0;

    @Resource
    private ConfflowMapper confflowMapper;

    @Resource
    private ConfflowAttachMapper confflowAttachMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private BpmInvalidateHelper bpmInvalidateHelper;

    @Resource
    private BpmRegisterTaskService bpmRegisterTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfflow(Long userId,ConfflowSaveReqVO createReqVO) {
        validateSubmitReq(createReqVO);
        // 插入
        ConfflowDO confflow = BeanUtils.toBean(createReqVO, ConfflowDO.class);
        confflowMapper.insert(confflow);

        createConfflowAttachList(confflow.getId(), createReqVO.getFileList());

        startProcess(userId, confflow.getId(), createReqVO);

        // 返回
        return confflow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveConfflow(Long userId, ConfflowSaveReqVO createReqVO) {
        ConfflowDO confflow = BeanUtils.toBean(createReqVO, ConfflowDO.class);
        confflow.setCreator(String.valueOf(userId));
        confflow.setProcessInstanceId(null);
        confflow.setStatus(DRAFT_STATUS);
        confflowMapper.insert(confflow);

        createConfflowAttachList(confflow.getId(), createReqVO.getFileList());
        Map<String, Object> processInstanceVariables = buildProcessVariables(createReqVO);
        String processInstanceId = createProcessInstance(userId, confflow.getId(), createReqVO,
                processInstanceVariables);
        int claimedCount = bpmRegisterTaskService.claim(userId, processInstanceId);
        if (claimedCount == 0) {
            throw exception(CONFFLOW_REGISTER_TASK_NOT_ACTIVE);
        }
        confflowMapper.updateById(new ConfflowDO().setId(confflow.getId())
                .setProcessInstanceId(processInstanceId)
                .setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue()));
        return confflow.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFlowConfflow(Long userId, ConfflowSaveReqVO updateReqVO) {
        ConfflowDO confflow = confflowMapper.selectById(updateReqVO.getId());
        if (confflow == null) {
            throw exception(CONFFLOW_NOT_EXISTS);
        }
        validateSubmitReq(updateReqVO);

        ConfflowDO updateObj = BeanUtils.toBean(updateReqVO, ConfflowDO.class);
        updateObj.setProcessInstanceId(confflow.getProcessInstanceId());
        confflowMapper.updateById(updateObj);

        updateConfflowAttachList(updateReqVO.getId(), updateReqVO.getFileList());
        submitRegisterTask(userId, updateReqVO.getId(), confflow.getProcessInstanceId(), updateReqVO);
    }

    private void validateSubmitReq(ConfflowSaveReqVO reqVO) {
        if (StringUtil.isEmpty(reqVO.getTitle()) || reqVO.getStartDate() == null || StringUtil.isEmpty(reqVO.getVenue())) {
            throw exception(CONFFLOW_SUBMIT_REQUIRED);
        }
    }

    private void startProcess(Long userId, Long confflowId, ConfflowSaveReqVO reqVO) {
        Map<String, Object> processInstanceVariables = buildProcessVariables(reqVO);
        String processInstanceId = createProcessInstance(userId, confflowId, reqVO, processInstanceVariables);
        completeRegisterTask(userId, processInstanceId, processInstanceVariables);
        updateProcessInfo(confflowId, processInstanceId);
    }

    private Map<String, Object> buildProcessVariables(ConfflowSaveReqVO reqVO) {
        Map<String, Object> processInstanceVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(reqVO.getProcessVariables())) {
            processInstanceVariables.putAll(reqVO.getProcessVariables());
        }
//        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        String customName = StringUtil.isEmpty(reqVO.getTitle()) ? "会议报告单" : reqVO.getTitle();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, reqVO.getNextNodeAssignees());
        String timeKey = "common";
        String timeoutLabel = DictFrameworkUtils.parseDictDataLabel("bpm_process_timeout_config", timeKey);
        if (StrUtil.isNotBlank(timeoutLabel) && NumberUtil.isNumber(timeoutLabel)) {
            int hours = Integer.parseInt(timeoutLabel);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = now.plusHours(hours);
            processInstanceVariables.put(PROCESS_FINISH_TIME, timeoutLabel);
            processInstanceVariables.put(PROCESS_DEADLINE_DATE, DateUtils.of(deadline));
        }
        return processInstanceVariables;
    }

    private String createProcessInstance(Long userId, Long confflowId, ConfflowSaveReqVO reqVO,
                                         Map<String, Object> processInstanceVariables) {
        return processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(confflowId))
                        .setStartUserSelectAssignees(reqVO.getStartUserSelectAssignees()));
    }

    private void submitRegisterTask(Long userId, Long confflowId, String processInstanceId,
                                    ConfflowSaveReqVO reqVO) {
        Map<String, Object> processInstanceVariables = buildProcessVariables(reqVO);
        if (StrUtil.isBlank(processInstanceId)) {
            processInstanceId = createProcessInstance(userId, confflowId, reqVO, processInstanceVariables);
        }
        completeRegisterTask(userId, processInstanceId, processInstanceVariables);
        updateProcessInfo(confflowId, processInstanceId);
    }

    private void completeRegisterTask(Long userId, String processInstanceId,
                                      Map<String, Object> processInstanceVariables) {
        int completedCount = bpmRegisterTaskService.completeOnSubmit(userId, processInstanceId,
                processInstanceVariables);
        if (completedCount == 0) {
            throw exception(CONFFLOW_REGISTER_TASK_NOT_ACTIVE);
        }
    }

    private void updateProcessInfo(Long confflowId, String processInstanceId) {
        confflowMapper.updateById(new ConfflowDO().setId(confflowId).setProcessInstanceId(processInstanceId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfflow(ConfflowSaveReqVO updateReqVO) {
        // 校验存在
        validateConfflowExists(updateReqVO.getId());
        // 更新
        ConfflowDO updateObj = BeanUtils.toBean(updateReqVO, ConfflowDO.class);
        confflowMapper.updateById(updateObj);

        // 更新附件子表
        updateConfflowAttachList(updateReqVO.getId(), updateReqVO.getFileList());
    }

    @Override
    public void deleteConfflow(Long id,String reason) {
        // 校验存在
        ConfflowDO confflow = confflowMapper.selectById(id);
        if(confflow == null){
            throw exception(CONFFLOW_NOT_EXISTS);
        }
        if(StringUtil.isBlank(confflow.getProcessInstanceId())){
            ConfflowDO updateObj = new ConfflowDO()
                    .setId(id)
                    .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue())
                    .setCancelReason(reason);
            confflowMapper.updateById(updateObj);
        }
        else {
            Integer currentStatus = confflow.getStatus() != null ? Integer.valueOf(confflow.getStatus()) : null;
            Long userId = getLoginUserId();
            bpmInvalidateHelper.executeInvalidate(
                    userId,
                    confflow.getProcessInstanceId(),
                    currentStatus,
                    reason,
                    () -> {
                        ConfflowDO updateObj = new ConfflowDO()
                                .setId(id)
                                .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue()) // 设置为 5(已作废)
                                .setCancelReason(reason); // 写入作废原因
                        confflowMapper.updateById(updateObj);
                    }
            );


        }

    }

    @Override
    public void deleteConfflowListByIds(List<Long> ids,String reason) {
        // 删除
        for (Long id : ids) {
            deleteConfflow( id, reason);
        }
    }


    private void validateConfflowExists(Long id) {
        if (confflowMapper.selectById(id) == null) {
            throw exception(CONFFLOW_NOT_EXISTS);
        }
    }

    @Override
    public ConfflowDO getConfflow(Long id) {
        return confflowMapper.selectById(id);
    }

    @Override
    public PageResult<ConfflowDO> getConfflowPage(ConfflowPageReqVO pageReqVO) {
        return confflowMapper.selectPage(pageReqVO);
    }

    @Override
    public void updateConfflowStatus(Long id, Integer status) {
        ConfflowDO confflow = confflowMapper.selectById(id);
        if (confflow == null) {
            return;
        }
        if (BpmProcessInstanceStatusEnum.INVALID.getStatus().equals(Integer.valueOf(confflow.getStatus()))) {
            return;
        }
        // 正常更新状态
        confflowMapper.updateById(new ConfflowDO().setId(id).setStatus(status.shortValue()));
    }

    private void createConfflowAttachList(Long commId, List<ConfflowAttachDO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(o -> o.setCommId(commId).clean());
        confflowAttachMapper.insertBatch(list);
    }

    private void updateConfflowAttachList(Long commId, List<ConfflowAttachDO> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.forEach(o -> o.setCommId(commId).clean());
        List<ConfflowAttachDO> oldList = confflowAttachMapper.selectListByCommId(commId);

        List<List<ConfflowAttachDO>> diffList = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = cn.hutool.core.util.ObjectUtil.equal(oldVal.getConfflowAttachId(), newVal.getConfflowAttachId());
            if (same) {
                newVal.setConfflowAttachId(oldVal.getConfflowAttachId()).clean();
            }
            return same;
        });

        if (CollUtil.isNotEmpty(diffList.get(0))) {
            confflowAttachMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            confflowAttachMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            confflowAttachMapper.deleteBatchIds(cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList(diffList.get(2), ConfflowAttachDO::getConfflowAttachId));
        }
    }

    @Override
    public List<ConfflowAttachRespVO> getConfflowAttachListByCommId(Long commId) {
        List<ConfflowAttachDO> doList = confflowAttachMapper.selectListByCommId(commId);
        if (CollUtil.isEmpty(doList)) {
            return Collections.emptyList();
        }
        List<ConfflowAttachRespVO> voList = BeanUtils.toBean(doList, ConfflowAttachRespVO.class);
        voList.forEach(vo -> {
            vo.setFileUrl(vo.getFilePath());
        });
        return voList;
    }

}
