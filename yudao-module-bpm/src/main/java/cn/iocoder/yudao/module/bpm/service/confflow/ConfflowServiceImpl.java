package cn.iocoder.yudao.module.bpm.service.confflow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
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

    @Resource
    private ConfflowMapper confflowMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private BpmInvalidateHelper bpmInvalidateHelper;

    @Override
    public Long createConfflow(Long userId,ConfflowSaveReqVO createReqVO) {
        // 插入
        ConfflowDO confflow = BeanUtils.toBean(createReqVO, ConfflowDO.class);
        confflowMapper.insert(confflow);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(createReqVO.getProcessVariables())) {
            processInstanceVariables.putAll(createReqVO.getProcessVariables());
        }
//        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        String customName = StringUtil.isEmpty(createReqVO.getTitle()) ? "会议报告单":createReqVO.getTitle();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String timeKey = "common";
        String timeoutLabel = DictFrameworkUtils.parseDictDataLabel("bpm_process_timeout_config", timeKey);
        if (StrUtil.isNotBlank(timeoutLabel) && NumberUtil.isNumber(timeoutLabel)) {
            int hours = Integer.parseInt(timeoutLabel);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = now.plusHours(hours);
            processInstanceVariables.put(PROCESS_FINISH_TIME, timeoutLabel);
            processInstanceVariables.put(PROCESS_DEADLINE_DATE, DateUtils.of(deadline));
        }
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(confflow.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        confflowMapper.updateById(new ConfflowDO().setId(confflow.getId()).setProcessInstanceId(processInstanceId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue()));

        // 返回
        return confflow.getId();
    }

    @Override
    public void updateConfflow(ConfflowSaveReqVO updateReqVO) {
        // 校验存在
        validateConfflowExists(updateReqVO.getId());
        // 更新
        ConfflowDO updateObj = BeanUtils.toBean(updateReqVO, ConfflowDO.class);
        confflowMapper.updateById(updateObj);
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



}