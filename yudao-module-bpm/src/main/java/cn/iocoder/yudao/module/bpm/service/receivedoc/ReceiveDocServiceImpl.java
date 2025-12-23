package cn.iocoder.yudao.module.bpm.service.receivedoc;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.*;
/**
 * 收文 Service 实现类
 *
 * @author 芋道
 */
@Service
@Validated
public class ReceiveDocServiceImpl implements ReceiveDocService {

    public static final String PROCESS_KEY = RECEIVE;

    @Resource
    private ReceiveDocMapper receiveDocMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    public Long createReceiveDoc(Long userId,ReceiveDocSaveReqVO createReqVO) {
        // 插入
        ReceiveDocDO receiveDoc = BeanUtils.toBean(createReqVO, ReceiveDocDO.class);
        receiveDocMapper.insert(receiveDoc);

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(receiveDoc.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        receiveDocMapper.updateById(new ReceiveDocDO().setId(receiveDoc.getId()).setProcessInstanceId(processInstanceId));


        // 返回
        return receiveDoc.getId();
    }

    @Override
    public void updateReceiveDoc(ReceiveDocSaveReqVO updateReqVO) {
        // 校验存在
        validateReceiveDocExists(updateReqVO.getId());
        // 更新
        ReceiveDocDO updateObj = BeanUtils.toBean(updateReqVO, ReceiveDocDO.class);
        receiveDocMapper.updateById(updateObj);
    }

    @Override
    public void deleteReceiveDoc(Long id) {
        // 校验存在
        validateReceiveDocExists(id);
        // 删除
        receiveDocMapper.deleteById(id);
    }

    @Override
        public void deleteReceiveDocListByIds(List<Long> ids) {
        // 删除
        receiveDocMapper.deleteByIds(ids);
        }


    private void validateReceiveDocExists(Long id) {
        if (receiveDocMapper.selectById(id) == null) {
            throw exception(RECEIVE_DOC_NOT_EXISTS);
        }
    }

    @Override
    public ReceiveDocDO getReceiveDoc(Long id) {
        return receiveDocMapper.selectById(id);
    }

    @Override
    public PageResult<ReceiveDocDO> getReceiveDocPage(ReceiveDocPageReqVO pageReqVO) {
        return receiveDocMapper.selectPage(pageReqVO);
    }

}