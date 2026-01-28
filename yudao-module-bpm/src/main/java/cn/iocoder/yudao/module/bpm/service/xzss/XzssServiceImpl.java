package cn.iocoder.yudao.module.bpm.service.xzss;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssKzDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssKzMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.XZSS;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_CUSTOM_NAME;

/**
 * 行政诉讼 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class XzssServiceImpl implements XzssService {

    public static final String PROCESS_KEY = XZSS;

    @Resource
    private XzssMapper xzssMapper;
    @Resource
    private XzssKzMapper xzssKzMapper;
    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createXzss(Long userId,XzssSaveReqVO createReqVO) {
        UUID uuid = UUID.randomUUID();

        // 转换为字符串
        String guidString = uuid.toString();
        createReqVO.setXmGuid(guidString);
        // 插入
        XzssDO xzss = BeanUtils.toBean(createReqVO, XzssDO.class);
        xzssMapper.insert(xzss);
        // 插入子表
        createXzssKz(xzss.getXmGuid(), createReqVO.getXzssKz());
        Map<String, Object> processInstanceVariables = new HashMap<>();
        String customName = StringUtil.isEmpty(createReqVO.getSqr()) ? "行政诉讼":createReqVO.getSqr()+"的行政诉讼";
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(xzss.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        xzssMapper.updateById(new XzssDO().setId(xzss.getId()).setProcessInstanceId(processInstanceId));
        // 返回
        return xzss.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateXzss(XzssSaveReqVO updateReqVO) {
        // 校验存在
        validateXzssExists(updateReqVO.getId());
        // 更新
        XzssDO updateObj = BeanUtils.toBean(updateReqVO, XzssDO.class);
        xzssMapper.updateById(updateObj);

        // 更新子表
        updateXzssKz(updateReqVO.getXmGuid(), updateReqVO.getXzssKz());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteXzss(Long id) {
        // 校验存在
        validateXzssExists(id);
        XzssDO xzss =xzssMapper.selectById(id);
        // 删除
        xzssMapper.deleteById(id);

        // 删除子表
        deleteXzssKzByXmGuid(xzss.getXmGuid());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteXzssListByIds(List<Long> ids) {
        List<XzssDO> xzssList = xzssMapper.selectBatchIds(ids);
        if (CollectionUtils.isEmpty(xzssList)) {
           List< String> xmGuids = xzssList.stream()
                   .map(XzssDO::getXmGuid)
                   .filter(Objects::nonNull)
                   .distinct()
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(xmGuids)) {
                deleteXzssKzByXmGuids(xmGuids);
            }
        }
        // 删除
        xzssMapper.deleteByIds(ids);

    }


    private void validateXzssExists(Long id) {
        if (xzssMapper.selectById(id) == null) {
            throw exception(XZSS_NOT_EXISTS);
        }
    }

    @Override
    public XzssDO getXzss(Long id) {
        return xzssMapper.selectById(id);
    }

    @Override
    public PageResult<XzssDO> getXzssPage(XzssPageReqVO pageReqVO) {
        return xzssMapper.selectPage(pageReqVO);
    }

    // ==================== 子表（行政诉讼拓展） ====================

    @Override
    public XzssKzDO getXzssKzByXmGuid(String xmGuid) {
        return xzssKzMapper.selectByXmGuid(xmGuid);
    }

    private void createXzssKz(String xmGuid, XzssKzDO xzssKz) {
        if (xzssKz == null) {
            return;
        }
        xzssKz.setXmGuid(xmGuid);
        xzssKzMapper.insert(xzssKz);
    }

    private void updateXzssKz(String xmGuid, XzssKzDO xzssKz) {
        if (xzssKz == null) {
			return;
        }
        xzssKz.setXmGuid(xmGuid).clean();// 解决更新情况下：updateTime 不更新
        xzssKzMapper.insertOrUpdate(xzssKz);
    }

    private void deleteXzssKzByXmGuid(String xmGuid) {
        xzssKzMapper.deleteByXmGuid(xmGuid);
    }

	private void deleteXzssKzByXmGuids(List<String> xmGuids) {
        xzssKzMapper.deleteByXmGuids(xmGuids);
	}

    @Override
    public List<XzssDO> getXzssListByFyGuid(String fyGuid) {
        return xzssMapper.selectList(new LambdaQueryWrapper<XzssDO>()
                .eq(XzssDO::getFyGuid, fyGuid)); // 假设 XzssDO 中对应的字段是 fyGuid
    }

    @Override
    public List<XzssDO> getXzssListBySsGuid(String ssGuid) {
        return xzssMapper.selectList(new LambdaQueryWrapper<XzssDO>()
                .eq(XzssDO::getSsGuid, ssGuid));
    }

}