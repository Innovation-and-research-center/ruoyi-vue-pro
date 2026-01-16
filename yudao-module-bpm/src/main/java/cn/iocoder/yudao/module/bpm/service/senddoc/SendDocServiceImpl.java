package cn.iocoder.yudao.module.bpm.service.senddoc;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.senddoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.senddoc.SendDocDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.senddoc.SendDocMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.SEND;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_CUSTOM_NAME;

/**
 * 发文 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class SendDocServiceImpl implements SendDocService {

    public static final String PROCESS_KEY = SEND;
    @Resource
    private SendDocMapper sendDocMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    public Long createSendDoc(Long userId,SendDocSaveReqVO createReqVO) {
        // 插入
        SendDocDO sendDoc = BeanUtils.toBean(createReqVO, SendDocDO.class);
        sendDocMapper.insert(sendDoc);
        Map<String, Object> processInstanceVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(createReqVO.getProcessVariables())) {
            processInstanceVariables.putAll(createReqVO.getProcessVariables());
        }
        String customName = StringUtil.isEmpty(createReqVO.getSubject()) ? "发文":createReqVO.getSubject();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(sendDoc.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        sendDocMapper.updateById(new SendDocDO().setId(sendDoc.getId()).setProcessInstanceId(processInstanceId));
        // 返回
        return sendDoc.getId();
    }

    @Override
    public void updateSendDoc(SendDocSaveReqVO updateReqVO) {
        // 校验存在
        validateSendDocExists(updateReqVO.getId());
        // 更新
        SendDocDO updateObj = BeanUtils.toBean(updateReqVO, SendDocDO.class);
        sendDocMapper.updateById(updateObj);
    }

    @Override
    public void deleteSendDoc(Long id) {
        // 校验存在
        validateSendDocExists(id);
        // 删除
        sendDocMapper.deleteById(id);
    }

    @Override
        public void deleteSendDocListByIds(List<Long> ids) {
        // 删除
        sendDocMapper.deleteByIds(ids);
        }


    private void validateSendDocExists(Long id) {
        if (sendDocMapper.selectById(id) == null) {
            throw exception(SEND_DOC_NOT_EXISTS);
        }
    }

    @Override
    public SendDocDO getSendDoc(Long id) {
//        LambdaQueryWrapper<SendDocDO> wrapper  = Wrappers.<SendDocDO>lambdaQuery()
//                .eq(SendDocDO::getId, id) // WHERE id = ?
//                .select(SendDocDO::getId, SendDocDO::getSubject, SendDocDO::getSendStatus);
//
//        return sendDocMapper.selectOne(wrapper);// SELECT id, subject, send_status ...

//// 使用 selectOne 查询
//        SendDocDO sendDoc = sendDocMapper.selectOne(wrapper);
        return sendDocMapper.selectById(id);
    }

    @Override
    public PageResult<SendDocDO> getSendDocPage(SendDocPageReqVO pageReqVO) {
        return sendDocMapper.selectPage(pageReqVO);
    }

}