package cn.iocoder.yudao.module.bpm.service.leave;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

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

    @Resource
    private BpmTaskService taskService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private BpmProcessInstanceService processInstanceService;

    @Resource
    private BpmInvalidateHelper bpmInvalidateHelper;

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
    @Transactional(rollbackFor = Exception.class)
    public void deleteLeave(Long id,String reason) {
        // 校验存在
        LeaveDO leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw exception(LEAVE_NOT_EXISTS);
        }
        Long userId = getLoginUserId();
        bpmInvalidateHelper.executeInvalidate(
                userId,
                leave.getProcessInstanceId(),
                Integer.valueOf(leave.getSpzt()),
                reason,
                () -> {
                    // 使用你的新枚举：INVALID (5, 已作废)
                    leaveMapper.updateById(new LeaveDO()
                            .setId(id)
                            .setSpzt(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue())
                            .setReason(reason));
                }
        );
    }

    @Override
        public void deleteLeaveListByIds(List<Long> ids,String reason) {
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
        LeaveDO leave = leaveMapper.selectById(id);
        if (leave == null) {
            return;
        }
        if (BpmProcessInstanceStatusEnum.INVALID.getStatus().equals(Integer.valueOf(leave.getSpzt()))) {
            return;
        }
        // 正常更新状态
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
    public List<LeaveHistoryRespVO> getLeaveDetailList(LeaveSummaryReqVO reqVO) {
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
        List<LeaveDO> detailList = leaveMapper.selectDetailList(reqVO);
        if (CollUtil.isEmpty(detailList)) {
            return Collections.emptyList();
        }

        // 2. 转换并填充 BPM 流程信息（复用历史记录的逻辑）
        List<LeaveHistoryRespVO> list = BeanUtils.toBean(detailList, LeaveHistoryRespVO.class);
        fillBpmInfoBatch(list);

        return list;
//        return leaveMapper.selectDetailList(reqVO);
    }

    @Override
    public PageResult<LeaveHistoryRespVO> getLeaveHistoryPage(LeavePageReqVO pageReqVO) {
        // 1. 强制过滤当前用户
        pageReqVO.setUserId(String.valueOf(SecurityFrameworkUtils.getLoginUserId()));

        // 2. 查询数据库分页
        PageResult<LeaveDO> pageResult = leaveMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty();
        }

        // 3. 转换并填充 BPM 信息
        List<LeaveHistoryRespVO> list = BeanUtils.toBean(pageResult.getList(), LeaveHistoryRespVO.class);

        fillBpmInfoBatch(list);

        return new PageResult<>(list, pageResult.getTotal());
    }

    private void cancelBpmProcessInstance(String processInstanceId, String reason) {
        try {
            // 【方式一】使用芋道源码封装的 Service (推荐)
            // 传入登录用户的 ID 和 作废请求参数
            processInstanceService.cancelProcessInstanceByAdmin(SecurityFrameworkUtils.getLoginUserId(),
                    new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO()
                            .setId(processInstanceId).setReason(reason));

            // 【方式二】如果方式一的方法签名在你的版本中不同，或者引入包报错，直接使用 Flowable 原生 API (无视权限直接强制作废)
            // runtimeService.deleteProcessInstance(processInstanceId, reason);
        } catch (Exception e) {
            // 如果流程已经结束（如已通过、已拒绝），调用作废可能会抛出异常。
            // 这里可以根据实际业务需求决定是吃掉异常还是抛出阻断删除
//            log.warn("作废流程实例失败, 流程可能已结束. processInstanceId: {}", processInstanceId, e);
        }
    }

    private void fillBpmInfoBatch(List<LeaveHistoryRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }

        // 1. 优先从业务状态判断，收集真正处于“审批中”(状态为 1) 的流程实例 ID
        List<String> runningProcessInstanceIds = new ArrayList<>();

        for (LeaveHistoryRespVO vo : list) {
            // 获取实际的业务状态
            Integer status = Integer.valueOf(vo.getSpzt());

            // 根据你提供的枚举进行精准拦截
            if (status == null || status == -1) {
                vo.setCurrentNodeName("未开始");
                vo.setCurrentAssigneeNames("-");
            } else if (status == 2) {
                vo.setCurrentNodeName("审批通过");
                vo.setCurrentAssigneeNames("-");
            } else if (status == 3) {
                vo.setCurrentNodeName("审批不通过");
                vo.setCurrentAssigneeNames("-");
            } else if (status == 4) {
                vo.setCurrentNodeName("已取消");
                vo.setCurrentAssigneeNames("-");
            } else if (status == 1) {
                // 【核心】只有处于“审批中”的状态，才去收集 ProcessInstanceId
                if (StrUtil.isNotBlank(vo.getProcessInstanceId())) {
                    runningProcessInstanceIds.add(vo.getProcessInstanceId());
                } else {
                    vo.setCurrentNodeName("数据异常(无流程ID)");
                    vo.setCurrentAssigneeNames("-");
                }
            }
        }

        // 2. 如果当前页没有任何处于“审批中”的请假单，直接结束方法！(0 次查询 BPM 引擎)
        if (CollUtil.isEmpty(runningProcessInstanceIds)) {
            return;
        }

        // 去重，防止有脏数据重复查询
        runningProcessInstanceIds = runningProcessInstanceIds.stream().distinct().collect(Collectors.toList());

        // 3. 批量获取运行中的 Task (底层原生支持只查 active 的任务)
        Map<String, List<Task>> taskMap = taskService.getTaskMapByProcessInstanceIds(runningProcessInstanceIds);

        // 4. 提取所有任务的办理人 ID，准备批量获取用户昵称
        Set<Long> assigneeUserIds = new HashSet<>();
        taskMap.values().forEach(tasks -> {
            tasks.forEach(task -> {
                if (StrUtil.isNotBlank(task.getAssignee())) {
                    assigneeUserIds.add(Long.valueOf(task.getAssignee()));
                }
            });
        });

        // 5. 批量获取办理人的用户信息 (只查一次系统用户表)
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isEmpty(assigneeUserIds) ?
                Collections.emptyMap() : adminUserApi.getUserMap(assigneeUserIds);

        // 6. 将查询到的 BPM 数据组装回处于“审批中”的 VO
        list.forEach(vo -> {
            // 仅处理状态为 1 (审批中) 的数据
            if (vo.getSpzt() != null && vo.getSpzt() == 1) {
                String processInstanceId = vo.getProcessInstanceId();
                if (StrUtil.isBlank(processInstanceId)) {
                    return;
                }

                // O(1) 复杂度取出该实例的所有运行中任务
                List<Task> activeTasks = taskMap.get(processInstanceId);

                if (CollUtil.isNotEmpty(activeTasks)) {
                    // 取第一个任务的节点名作为当前环节名称
                    vo.setCurrentNodeName(activeTasks.get(0).getName());

                    // 提取办理人名称并拼接
                    StringJoiner joiner = new StringJoiner(",");
                    activeTasks.forEach(task -> {
                        if (StrUtil.isNotBlank(task.getAssignee())) {
                            AdminUserRespDTO user = userMap.get(Long.valueOf(task.getAssignee()));
                            joiner.add(user != null ? user.getNickname() : task.getAssignee());
                        } else {
                            joiner.add("等待拾取/未分配");
                        }
                    });
                    vo.setCurrentAssigneeNames(joiner.toString());

                } else {
                    // 容错兜底：业务状态是“审批中”，但在流程引擎中查不到处于 active 的任务
                    // （可能是流程卡死、遇到异常导致任务挂起、或者是异步操作导致的短暂延迟）
                    vo.setCurrentNodeName("系统流转中");
                    vo.setCurrentAssigneeNames("-");
                }
            }
        });
    }

    @Override
    public List<LeaveTypeStatRespVO> getCurrentUserYearlyLeaveStat() {
        // 1. 获取当前调用接口的人的 ID
        Long userId = SecurityFrameworkUtils.getLoginUserId();

        // 2. 划定今年的时间范围：今年的 1月1日 00:00:00 到 12月31日 23:59:59
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beginTime = LocalDateTime.of(now.getYear(), 1, 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(now.getYear(), 12, 31, 23, 59, 59);

        // 3. 调用 Mapper 进行统计查询
        return leaveMapper.selectLeaveTypeStat(userId, beginTime, endTime);
    }

}