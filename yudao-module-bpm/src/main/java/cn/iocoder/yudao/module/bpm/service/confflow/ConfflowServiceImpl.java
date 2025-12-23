package cn.iocoder.yudao.module.bpm.service.confflow;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.CONFLOW_REPORT;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

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

    @Override
    public Long createConfflow(Long userId,ConfflowSaveReqVO createReqVO) {
        // 插入
        ConfflowDO confflow = BeanUtils.toBean(createReqVO, ConfflowDO.class);
        confflowMapper.insert(confflow);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(confflow.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        confflowMapper.updateById(new ConfflowDO().setId(confflow.getId()).setProcessInstanceId(processInstanceId));

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
    public void deleteConfflow(Long id) {
        // 校验存在
        validateConfflowExists(id);
        // 删除
        confflowMapper.deleteById(id);
    }

    @Override
        public void deleteConfflowListByIds(List<Long> ids) {
        // 删除
        confflowMapper.deleteByIds(ids);
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

}