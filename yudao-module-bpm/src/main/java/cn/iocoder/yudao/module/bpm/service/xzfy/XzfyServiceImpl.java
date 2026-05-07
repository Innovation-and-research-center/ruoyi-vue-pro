package cn.iocoder.yudao.module.bpm.service.xzfy;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.commentattach.CommentAttachService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.LEAVE;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.XZFY;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.XZFY_NOT_EXISTS;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;

/**
 * 行政复议 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class XzfyServiceImpl implements XzfyService {

    public static final String PROCESS_KEY = XZFY;

    public static final String DOC_TYPE_XZFY = "XZFY";

    @Resource
    private XzfyMapper xzfyMapper;
    @Resource
    private XzfyKzMapper xzfyKzMapper;

    @Resource
    private XzssMapper xzssMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private CommentAttachService commentAttachService;

    @Resource
    private BpmInvalidateHelper bpmInvalidateHelper;

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

        commentAttachService.saveCommentAttachList(guidString, DOC_TYPE_XZFY, createReqVO.getFileList());

        Map<String, Object> processInstanceVariables = new HashMap<>();
        String customName = StringUtil.isEmpty(createReqVO.getSqr()) ? "行政复议":createReqVO.getSqr();
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
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(xzfy.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        xzfyMapper.updateById(new XzfyDO().setId(xzfy.getId()).setProcessInstanceId(processInstanceId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue()));
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

        commentAttachService.saveCommentAttachList(updateReqVO.getXmGuid(), DOC_TYPE_XZFY, updateReqVO.getFileList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteXzfy(Long id, String reason) {
        // 校验存在
        XzfyDO xzfy =xzfyMapper.selectById(id);
        if(xzfy == null){
            throw exception(XZFY_NOT_EXISTS);
        }
        if (StrUtil.isBlank(xzfy.getProcessInstanceId())) {
            XzfyDO updateObj = new XzfyDO()
                    .setId(id)
                    .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue()) // 设置为 5(已作废) [cite: 40]
                    .setCancelReason(reason);
            xzfyMapper.updateById(updateObj);
        }
        else{
            Integer currentStatus = xzfy.getStatus() != null ? Integer.valueOf(xzfy.getStatus()) : null;
            Long userId = getLoginUserId();
            bpmInvalidateHelper.executeInvalidate(
                    userId,
                    xzfy.getProcessInstanceId(),
                    currentStatus,
                    reason,
                    () -> {
                        XzfyDO updateObj = new XzfyDO()
                                .setId(id)
                                .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue()) // 设置为 5(已作废) [cite: 40]
                                .setCancelReason(reason);
                        xzfyMapper.updateById(updateObj);
                    }
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteXzfyListByIds(List<Long> ids, String reason) {
        for (Long id : ids) {
            deleteXzfy( id, reason);
        }
//        List<XzfyDO> xzfyList = xzfyMapper.selectBatchIds(ids);
//        if (CollectionUtils.isNotEmpty(xzfyList)) {
//            // 3. 生成 xmGuid 列表
//            // 假设 xmGuid 是 String 类型，根据你的实际情况调整
//            List<String> xmGuids = xzfyList.stream()
//                    .map(XzfyDO::getXmGuid)           // 提取 xmGuid
//                    .filter(Objects::nonNull)       // 过滤掉可能存在的 null 值
//                    .distinct()                     // 去重 (可选，视业务逻辑而定)
//                    .collect(Collectors.toList());
//
//            // 4. 放入到 deleteXzfyKzByXmGuids 批量删除子表
//            if (CollectionUtils.isNotEmpty(xmGuids)) {
//                deleteXzfyKzByXmGuids(xmGuids);
//            }
//        }
//        // 删除
//        xzfyMapper.deleteByIds(ids);

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

    @Override
    public List<XzfyDO> getXzfyListByXmGuid(String xmGuid) {
        return xzfyMapper.selectList(new LambdaQueryWrapper<XzfyDO>()
                .eq(XzfyDO::getXmGuid, xmGuid));
    }

    @Override
    public PageResult<XzfyDO> getUnlinkedXzfyPage(XzfyPageReqVO reqVO) {
        // 1. 查询所有已被行政诉讼关联的 fyGuid
        List<XzssDO> xzssList = xzssMapper.selectList(new LambdaQueryWrapper<XzssDO>()
                .select(XzssDO::getFyGuid)
                .isNotNull(XzssDO::getFyGuid)
                .ne(XzssDO::getFyGuid, ""));

        // 2. 提取 Guid 列表并去重
        Set<String> usedGuids = xzssList.stream()
                .map(XzssDO::getFyGuid)
                .collect(Collectors.toSet());

        // 3. 构建查询条件
        LambdaQueryWrapperX<XzfyDO> queryWrapper = new LambdaQueryWrapperX<>();

        // --- 核心过滤：排除已关联的数据 ---
        if (!usedGuids.isEmpty()) {
            queryWrapper.notIn(XzfyDO::getXmGuid, usedGuids);
        }

        // --- 常规查询条件 (仿照您原有的 getXzfyPage 逻辑) ---
        // 建议直接调用您现有的构建查询条件的方法，或者手动添加 ReqVO 中的字段
        // 例如：
        // queryWrapper.likeIfPresent(XzfyDO::getSwWh, reqVO.getSwWh())
        //             .likeIfPresent(XzfyDO::getSqr, reqVO.getSqr())
        //             .eqIfPresent(XzfyDO::getLb1, reqVO.getLb1());
        queryWrapper.likeIfPresent(XzfyDO::getSwWh, reqVO.getSwWh())
                .likeIfPresent(XzfyDO::getSqr, reqVO.getSqr());

        // 排序
        queryWrapper.orderByDesc(XzfyDO::getId);

        // 4. 执行分页查询
        return xzfyMapper.selectPage(reqVO, queryWrapper);
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

    public void updateXzfyStatus(Long id, Integer status) {

        XzfyDO xzfy = xzfyMapper.selectById(id);
        if (xzfy == null) {
            return;
        }

        if (BpmProcessInstanceStatusEnum.INVALID.getStatus().equals(Integer.valueOf(xzfy.getStatus()))) {
            return;
        }
        // 正常更新状态
        xzfyMapper.updateById(new XzfyDO().setId(id).setStatus(status.shortValue()));
    }

}