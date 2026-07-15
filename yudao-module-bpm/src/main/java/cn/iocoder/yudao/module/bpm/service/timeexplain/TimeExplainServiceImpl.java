package cn.iocoder.yudao.module.bpm.service.timeexplain;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain.TimeExplainAttachMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.task.BpmRegisterTaskService;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain.TimeExplainMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.OUT;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;

/**
 * 外出请假补假 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class TimeExplainServiceImpl implements TimeExplainService {

    public static final String PROCESS_KEY = OUT;

    @Resource
    private TimeExplainMapper timeExplainMapper;

    @Resource
    private TimeExplainAttachMapper timeExplainAttachMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private AdminUserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private BpmInvalidateHelper bpmInvalidateHelper;

    @Resource
    private BpmRegisterTaskService bpmRegisterTaskService;

    @Override
    public Long createTimeExplain(TimeExplainSaveReqVO createReqVO) {
        // 插入
        TimeExplainDO timeExplain = BeanUtils.toBean(createReqVO, TimeExplainDO.class);
        timeExplainMapper.insert(timeExplain);

        // 返回
        return timeExplain.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOut(Long userId, TimeExplainSaveReqVO createReqVO) {
        TimeExplainDO out = createOutAndProcess(userId, createReqVO);
        bpmRegisterTaskService.completeOnSubmit(userId, out.getProcessInstanceId(), buildProcessVariables(userId, createReqVO));
        return out.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOut(Long userId, TimeExplainSaveReqVO createReqVO) {
        TimeExplainDO out = createOutAndProcess(userId, createReqVO);
        bpmRegisterTaskService.claim(userId, out.getProcessInstanceId());
        return out.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFlowOut(Long userId, TimeExplainSaveReqVO updateReqVO) {
        TimeExplainDO out = timeExplainMapper.selectById(updateReqVO.getId());
        if (out == null) throw exception(TIME_EXPLAIN_NOT_EXISTS);
        normalizePeriodTime(updateReqVO);
        updateTimeExplain(updateReqVO);
        bpmRegisterTaskService.completeOnSubmit(userId, out.getProcessInstanceId(), buildProcessVariables(userId, updateReqVO));
    }

    private TimeExplainDO createOutAndProcess(Long userId, TimeExplainSaveReqVO createReqVO) {
        normalizePeriodTime(createReqVO);
        AdminUserDO user = userService.getUser(userId);
        TimeExplainDO out = BeanUtils.toBean(createReqVO, TimeExplainDO.class)
                .setUserId( userId).setStatus(Long.valueOf(BpmTaskStatusEnum.RUNNING.getStatus()))
                .setUserName(user.getUsername())
                .setFirstType("外出").setSecondType("因公外出");
        timeExplainMapper.insert(out);

        createTimeExplainAttachList(out.getId(), createReqVO.getFileList());

        Map<String, Object> processInstanceVariables = buildProcessVariables(userId, createReqVO);
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(out.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        timeExplainMapper.updateById(new TimeExplainDO().setId(out.getId()).setProcessInstanceId(processInstanceId));
        return out.setProcessInstanceId(processInstanceId);
    }

    private Map<String, Object> buildProcessVariables(Long userId, TimeExplainSaveReqVO createReqVO) {
        AdminUserDO user = userService.getUser(userId);
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));

        String roleCondition = "grade_3";
        int maxGradeVal=3 ;
        for(RoleDO role : roles){
            String key =role.getCode();
            if(key !=null && key.startsWith("grade_")){
                int current_grade = Integer.parseInt(key.replace("grade_", ""));
                if (current_grade >= maxGradeVal) {
                    maxGradeVal = current_grade;
                    roleCondition = "grade_"+maxGradeVal;
                }
            }
        }
        String customName = user.getNickname() + "因公外出"+createReqVO.getEndPeriod();
        Map<String, Object> processInstanceVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(createReqVO.getProcessVariables())) processInstanceVariables.putAll(createReqVO.getProcessVariables());
        processInstanceVariables.put("role_condition", roleCondition);
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        String timeKey = "common";
        String timeoutLabel = DictFrameworkUtils.parseDictDataLabel("bpm_process_timeout_config", timeKey);
        if (StrUtil.isNotBlank(timeoutLabel) && NumberUtil.isNumber(timeoutLabel)) {
            int hours = Integer.parseInt(timeoutLabel);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = now.plusHours(hours);
            processInstanceVariables.put(PROCESS_FINISH_TIME, timeoutLabel);
            processInstanceVariables.put(PROCESS_DEADLINE_DATE, DateUtils.of(deadline));
        }
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());

        return processInstanceVariables;
    }

    private void normalizePeriodTime(TimeExplainSaveReqVO reqVO) {
        reqVO.setCheckBegin(reqVO.getCheckBegin().toLocalDate()
                .atTime("1".equals(reqVO.getStartPeriod()) ? LocalTime.of(8, 30) : LocalTime.of(13, 30)));
        reqVO.setCheckEnd(reqVO.getCheckEnd().toLocalDate()
                .atTime("1".equals(reqVO.getEndPeriod()) ? LocalTime.of(8, 30) : LocalTime.of(13, 30)));
    }

    @Override
    public void updateTimeExplain(TimeExplainSaveReqVO updateReqVO) {
        // 校验存在
        validateTimeExplainExists(updateReqVO.getId());
        // 更新
        TimeExplainDO updateObj = BeanUtils.toBean(updateReqVO, TimeExplainDO.class);
        timeExplainMapper.updateById(updateObj);

        // 更新附件子表
        updateTimeExplainAttachList(updateReqVO.getId(), updateReqVO.getFileList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTimeExplain(Long id,String reason) {
        // 校验存在
        TimeExplainDO timeExplain = timeExplainMapper.selectById(id);
        if (timeExplain == null) {
            throw exception(TIME_EXPLAIN_NOT_EXISTS);
        }
        // 删除
        Long userId = getLoginUserId();
        bpmInvalidateHelper.executeInvalidate(
                userId,
                timeExplain.getProcessInstanceId(),
                timeExplain.getStatus().intValue(), // TimeExplain表使用的是 status 字段
                reason,
                () -> {
                    // 3. 更新业务表：标记状态为已作废(5)，并存入作废原因
                    timeExplainMapper.updateById(new TimeExplainDO()
                            .setId(id)
                            .setStatus(Long.valueOf(BpmProcessInstanceStatusEnum.INVALID.getStatus()))
                            .setCancelReason(reason));
                }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTimeExplainListByIds(List<Long> ids,String reason) {
        // 删除
        for (Long id : ids) {
            deleteTimeExplain(id, reason);
        }
    }


    private void validateTimeExplainExists(Long id) {
        if (timeExplainMapper.selectById(id) == null) {
            throw exception(TIME_EXPLAIN_NOT_EXISTS);
        }
    }

    @Override
    public TimeExplainDO getTimeExplain(Long id) {
        return timeExplainMapper.selectById(id);
    }

    @Override
    public PageResult<TimeExplainDO> getTimeExplainPage(TimeExplainPageReqVO pageReqVO) {
        return timeExplainMapper.selectPage(pageReqVO);
    }
    @Override
    public void updateTimeExplainStatus(Long id, Integer status) {
        TimeExplainDO timeExplain = timeExplainMapper.selectById(id);
        if (timeExplain == null) {
            return;
        }

        // 核心拦截：如果当前业务状态已经是已作废(5)，则拒绝任何后续状态覆盖（例如工作流的取消事件(4)）
        if (BpmProcessInstanceStatusEnum.INVALID.getStatus().equals(timeExplain.getStatus().intValue())) {
            return;
        }

        // 正常更新状态
        timeExplainMapper.updateById(new TimeExplainDO().setId(id).setStatus(Long.valueOf(status)));
    }

    private void createTimeExplainAttachList(Long timeExplainId, List<TimeExplainAttachDO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        list.forEach(o -> o.setTimeExplainId(timeExplainId).clean());
        timeExplainAttachMapper.insertBatch(list);
    }

    private void updateTimeExplainAttachList(Long timeExplainId, List<TimeExplainAttachDO> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.forEach(o -> o.setTimeExplainId(timeExplainId).clean());
        List<TimeExplainAttachDO> oldList = timeExplainAttachMapper.selectListByTimeExplainId(timeExplainId);

        List<List<TimeExplainAttachDO>> diffList = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = cn.hutool.core.util.ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).clean();
            }
            return same;
        });

        if (CollUtil.isNotEmpty(diffList.get(0))) {
            timeExplainAttachMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            timeExplainAttachMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            timeExplainAttachMapper.deleteBatchIds(cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList(diffList.get(2), TimeExplainAttachDO::getId));
        }
    }

    @Override
    public List<TimeExplainAttachRespVO> getTimeExplainAttachListByTimeExplainId(Long timeExplainId) {
        List<TimeExplainAttachDO> doList = timeExplainAttachMapper.selectListByTimeExplainId(timeExplainId);
        if (CollUtil.isEmpty(doList)) {
            return Collections.emptyList();
        }
        List<TimeExplainAttachRespVO> voList = BeanUtils.toBean(doList, TimeExplainAttachRespVO.class);
        voList.forEach(vo -> {
            vo.setFileUrl(vo.getFilePath());
        });
        return voList;
    }

}
