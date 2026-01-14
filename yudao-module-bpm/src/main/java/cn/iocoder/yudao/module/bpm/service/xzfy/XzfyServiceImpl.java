package cn.iocoder.yudao.module.bpm.service.xzfy;

import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyKzDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.xzfy.XzfyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzfy.XzfyKzMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.LEAVE;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.XZFY;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.XZFY_NOT_EXISTS;

/**
 * 行政复议 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class XzfyServiceImpl implements XzfyService {

    public static final String PROCESS_KEY = XZFY;

    @Resource
    private XzfyMapper xzfyMapper;
    @Resource
    private XzfyKzMapper xzfyKzMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createXzfy(Long userId,XzfySaveReqVO createReqVO) {

        UUID uuid = UUID.randomUUID();

        // 转换为字符串
        String guidString = uuid.toString();
        createReqVO.setXmGuid(guidString);
        // 插入
        XzfyDO xzfy = BeanUtils.toBean(createReqVO, XzfyDO.class);
        xzfyMapper.insert(xzfy);
        // 插入子表
        createXzfyKz(guidString, createReqVO.getXzfyKz());

        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(xzfy.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        xzfyMapper.updateById(new XzfyDO().setId(xzfy.getId()).setProcessInstanceId(processInstanceId));
        // 返回
        return xzfy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateXzfy(XzfySaveReqVO updateReqVO) {
        // 校验存在
        validateXzfyExists(updateReqVO.getId());
        // 更新
        XzfyDO updateObj = BeanUtils.toBean(updateReqVO, XzfyDO.class);
        xzfyMapper.updateById(updateObj);

        // 更新子表
        updateXzfyKz(updateReqVO.getXmGuid(), updateReqVO.getXzfyKz());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteXzfy(Long id) {
        // 校验存在
        validateXzfyExists(id);
        XzfyDO xzfy =xzfyMapper.selectById(id);

        // 删除
        xzfyMapper.deleteById(id);

        // 删除子表
        deleteXzfyKzByXmGuid(xzfy.getXmGuid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteXzfyListByIds(List<Long> ids) {
        List<XzfyDO> xzfyList = xzfyMapper.selectBatchIds(ids);
        if (CollectionUtils.isNotEmpty(xzfyList)) {
            // 3. 生成 xmGuid 列表
            // 假设 xmGuid 是 String 类型，根据你的实际情况调整
            List<String> xmGuids = xzfyList.stream()
                    .map(XzfyDO::getXmGuid)           // 提取 xmGuid
                    .filter(Objects::nonNull)       // 过滤掉可能存在的 null 值
                    .distinct()                     // 去重 (可选，视业务逻辑而定)
                    .collect(Collectors.toList());

            // 4. 放入到 deleteXzfyKzByXmGuids 批量删除子表
            if (CollectionUtils.isNotEmpty(xmGuids)) {
                deleteXzfyKzByXmGuids(xmGuids);
            }
        }
        // 删除
        xzfyMapper.deleteByIds(ids);

    }


    private void validateXzfyExists(Long id) {
        if (xzfyMapper.selectById(id) == null) {
            throw exception(XZFY_NOT_EXISTS);
        }
    }

    @Override
    public XzfyDO getXzfy(Long id) {
        return xzfyMapper.selectById(id);
    }

    @Override
    public PageResult<XzfyDO> getXzfyPage(XzfyPageReqVO pageReqVO) {
        return xzfyMapper.selectPage(pageReqVO);
    }

    // ==================== 子表（行政复议扩展） ====================

    @Override
    public XzfyKzDO getXzfyKzByXmGuid(String xmGuid) {
        return xzfyKzMapper.selectByXmGuid(xmGuid);
    }

    private void createXzfyKz(String xmGuid, XzfyKzDO xzfyKz) {
        if (xzfyKz == null) {
            return;
        }
        xzfyKz.setXmGuid(xmGuid);
        xzfyKzMapper.insert(xzfyKz);
    }

    private void updateXzfyKz(String xmGuid, XzfyKzDO xzfyKz) {
        if (xzfyKz == null) {
			return;
        }
        xzfyKz.setXmGuid(xmGuid).clean();// 解决更新情况下：updateTime 不更新
        xzfyKzMapper.insertOrUpdate(xzfyKz);
    }

    private void deleteXzfyKzByXmGuid(String xmGuid) {
        xzfyKzMapper.deleteByXmGuid(xmGuid);
    }

	private void deleteXzfyKzByXmGuids(List<String> xmGuids) {
        xzfyKzMapper.deleteByXmGuids(xmGuids);
	}

}