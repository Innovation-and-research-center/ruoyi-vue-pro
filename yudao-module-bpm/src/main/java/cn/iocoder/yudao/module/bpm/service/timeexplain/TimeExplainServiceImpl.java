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
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
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
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private AdminUserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private PermissionService permissionService;

    @Override
    public Long createTimeExplain(TimeExplainSaveReqVO createReqVO) {
        // 插入
        TimeExplainDO timeExplain = BeanUtils.toBean(createReqVO, TimeExplainDO.class);
        timeExplainMapper.insert(timeExplain);

        // 返回
        return timeExplain.getId();
    }

    @Override
    public Long createOut(Long userId, TimeExplainSaveReqVO createReqVO) {
        //根据时段增加时间上午为8:30 下午为 13:30
        if(createReqVO.getStartPeriod().equals("1")){
            createReqVO.setCheckBegin(createReqVO.getCheckBegin().plusHours(8).plusMinutes(30));
        }
        else {
            createReqVO.setCheckBegin(createReqVO.getCheckBegin().plusHours(13).plusMinutes(30));
        }
        if(createReqVO.getEndPeriod().equals("1")){
            createReqVO.setCheckEnd(createReqVO.getCheckEnd().plusHours(8).plusMinutes(30));
        }
        else{
            createReqVO.setCheckEnd(createReqVO.getCheckEnd().plusHours(13).plusMinutes(30));
        }
        AdminUserDO user = userService.getUser(getLoginUserId());
        TimeExplainDO out = BeanUtils.toBean(createReqVO, TimeExplainDO.class)
                .setUserId( userId).setStatus(Long.valueOf(BpmTaskStatusEnum.RUNNING.getStatus()))
                .setUserName(user.getUsername())
                .setFirstType("外出").setSecondType("因公外出");
        timeExplainMapper.insert(out);
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())&& role.getCode().contains("grade_")); // 移除禁用的角色

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

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(out.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        timeExplainMapper.updateById(new TimeExplainDO().setId(out.getId()).setProcessInstanceId(processInstanceId));
        return out.getId() ;
    }

    @Override
    public void updateTimeExplain(TimeExplainSaveReqVO updateReqVO) {
        // 校验存在
        validateTimeExplainExists(updateReqVO.getId());
        // 更新
        TimeExplainDO updateObj = BeanUtils.toBean(updateReqVO, TimeExplainDO.class);
        timeExplainMapper.updateById(updateObj);
    }

    @Override
    public void deleteTimeExplain(Long id) {
        // 校验存在
        validateTimeExplainExists(id);
        // 删除
        timeExplainMapper.deleteById(id);
    }

    @Override
        public void deleteTimeExplainListByIds(List<Long> ids) {
        // 删除
        timeExplainMapper.deleteByIds(ids);
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

}