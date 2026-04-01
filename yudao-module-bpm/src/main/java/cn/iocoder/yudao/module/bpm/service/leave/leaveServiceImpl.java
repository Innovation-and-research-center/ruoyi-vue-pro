package cn.iocoder.yudao.module.bpm.service.leave;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveMapper;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_CUSTOM_NAME;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_FINISH_TIME;

/**
 * 假期申请审批 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class LeaveServiceImpl implements LeaveService {

    public static final String PROCESS_KEY = LEAVE;

    @Resource
    private LeaveMapper leaveMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private AdminUserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private PermissionService permissionService;


    @Override
    public Long createLeave(Long userId,LeaveSaveReqVO createReqVO) {

        //根据时段增加时间上午为8:30 下午为 13:30
        if(createReqVO.getStartPeriod().equals("1")){
            createReqVO.setQxjStartDate(createReqVO.getQxjStartDate().plusHours(8).plusMinutes(30));
        }
        else {
            createReqVO.setQxjStartDate(createReqVO.getQxjStartDate().plusHours(13).plusMinutes(30));
        }
        if(createReqVO.getEndPeriod().equals("1")){
            createReqVO.setQxjEndDate(createReqVO.getQxjEndDate().plusHours(8).plusMinutes(30));
        }
        else{
            createReqVO.setQxjEndDate(createReqVO.getQxjEndDate().plusHours(13).plusMinutes(30));
        }
        // 插入
        LeaveDO leave = BeanUtils.toBean(createReqVO, LeaveDO.class)
                .setUserid(userId.intValue()).setSpzt(BpmTaskStatusEnum.RUNNING.getStatus().shortValue());
        leaveMapper.insert(leave);


        AdminUserDO user = userService.getUser(getLoginUserId());

        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(getLoginUserId());
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())&& role.getCode().contains("grade_")); // 移除禁用的角色

        String roleCondition = "grade_3";
        String days_condition4 = "";
        String days_condition3 = "";
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
        if (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(1)) <= 0 && roleCondition.equals("grade_3"))
        {
            days_condition4 = "4_6";
        }
        else {
            //其他转分管领导审核
            days_condition4 = "4_3";
        }


        //如果普通人员请假大于1天，或者中层副职请假7天及以内；分管领导-->人教科备案
        if ((createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(1)) > 0 && createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(30))<=0 && roleCondition.equals("grade_3")) || (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(7))<=0 && roleCondition.equals("grade_7")))
        {
            days_condition3 = "3_6";
        }
        //如果中层正职，或者中层副职请假7天以上，或一般人员超过30天；分管领导-->局领导审核
        if (roleCondition == "grade_11" || (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(7))>0 && roleCondition.equals("grade_7")) || (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(30))>0 && roleCondition.equals("grade_3")))
        {
            days_condition3 = "3_5";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        String timeStr = "";
        if (createReqVO.getQxjStartDate() != null) {
            timeStr += "(" + createReqVO.getQxjStartDate().format(formatter) + "-";
        } else {
            timeStr += "无";
        }

        if (createReqVO.getQxjEndDate() != null) {
            timeStr += createReqVO.getQxjEndDate().format(formatter) + ")";
        } else {
            timeStr += "无";
        }
        String dictLabel = DictFrameworkUtils.parseDictDataLabel("leave_type",createReqVO.getQxjType());
        //自定义标题
        String customName = user.getNickname() + dictLabel+timeStr;

        // 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("role_condition", roleCondition);
        processInstanceVariables.put("days_condition3", days_condition3);
        processInstanceVariables.put("days_condition4", days_condition4);
        String timeKey = "common";
        String timeoutLabel = DictFrameworkUtils.parseDictDataLabel("bpm_process_timeout_config", timeKey);
        processInstanceVariables.put(PROCESS_FINISH_TIME, timeoutLabel);
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());

        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(leave.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        leaveMapper.updateById(new LeaveDO().setId(leave.getId()).setProcessInstanceId(processInstanceId));

        // 返回
        return leave.getId();
    }

    @Override
    public void updateLeave(LeaveSaveReqVO updateReqVO) {
        // 校验存在
        validateLeaveExists(updateReqVO.getId());
        // 更新
        LeaveDO updateObj = BeanUtils.toBean(updateReqVO, LeaveDO.class);
        leaveMapper.updateById(updateObj);
    }

    @Override
    public void deleteLeave(Long id) {
        // 校验存在
        validateLeaveExists(id);
        // 删除
        leaveMapper.deleteById(id);
    }

    @Override
        public void deleteLeaveListByIds(List<Long> ids) {
        // 删除
        leaveMapper.deleteByIds(ids);
        }


    private void validateLeaveExists(Long id) {
        if (leaveMapper.selectById(id) == null) {
            throw exception(LEAVE_NOT_EXISTS);
        }
    }

    @Override
    public LeaveDO getLeave(Long id) {
        return leaveMapper.selectById(id);
    }

    @Override
    public PageResult<LeaveDO> getLeavePage(LeavePageReqVO pageReqVO) {
        return leaveMapper.selectPage(pageReqVO);
    }


    @Override
    public void updateLeaveStatus(Long id, Integer status) {
        validateLeaveExists(id);
        leaveMapper.updateById(new LeaveDO().setId(id).setSpzt(status.shortValue()));
    }

    @Override
    public List<LeaveSummaryRespVO> getLeaveSummary(LeaveSummaryReqVO reqVO) {
        // 如果没有传年份，默认不进行时间过滤，或者你可以设置为当前年份
        if (reqVO.getYear() != null) {
            int year = reqVO.getYear();
            LocalDateTime beginTime;
            LocalDateTime endTime;

            if (reqVO.getMonth() != null) {
                // 统计指定月：例如 2024-02-01 00:00:00 到 2024-02-29 23:59:59
                LocalDate firstDay = LocalDate.of(year, reqVO.getMonth(), 1);
                LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());

                beginTime = LocalDateTime.of(firstDay, LocalTime.MIN);
                endTime = LocalDateTime.of(lastDay, LocalTime.MAX);
            } else {
                // 统计整年：2024-01-01 到 2024-12-31
                LocalDate firstDay = LocalDate.of(year, 1, 1);
                LocalDate lastDay = LocalDate.of(year, 12, 31);

                beginTime = LocalDateTime.of(firstDay, LocalTime.MIN);
                endTime = LocalDateTime.of(lastDay, LocalTime.MAX);
            }

            // 将计算好的时间填入 VO，传给 Mapper
            reqVO.setBeginTime(beginTime);
            reqVO.setEndTime(endTime);
        }

        // 执行查询
        return leaveMapper.selectLeaveSummaryList(reqVO);
    }


    @Override
    public List<LeaveDO> getLeaveDetailList(LeaveSummaryReqVO reqVO) {
        // 复用之前的年月转时间范围逻辑
        if (reqVO.getYear() != null) {
            int year = reqVO.getYear();
            LocalDateTime beginTime;
            LocalDateTime endTime;

            if (reqVO.getMonth() != null) {
                // 统计指定月：例如 2024-02-01 00:00:00 到 2024-02-29 23:59:59
                LocalDate firstDay = LocalDate.of(year, reqVO.getMonth(), 1);
                LocalDate lastDay = firstDay.with(TemporalAdjusters.lastDayOfMonth());

                beginTime = LocalDateTime.of(firstDay, LocalTime.MIN);
                endTime = LocalDateTime.of(lastDay, LocalTime.MAX);
            } else {
                // 统计整年：2024-01-01 到 2024-12-31
                LocalDate firstDay = LocalDate.of(year, 1, 1);
                LocalDate lastDay = LocalDate.of(year, 12, 31);

                beginTime = LocalDateTime.of(firstDay, LocalTime.MIN);
                endTime = LocalDateTime.of(lastDay, LocalTime.MAX);
            }

            reqVO.setBeginTime(beginTime);
            reqVO.setEndTime(endTime);
        }
        return leaveMapper.selectDetailList(reqVO);
    }

}