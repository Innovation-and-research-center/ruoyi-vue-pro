package cn.iocoder.yudao.module.bpm.service.leave;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
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
        if ((createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(1)) > 0 && createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(30))<=0 && roleCondition.equals("grade_3")) || (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(7))<=0 && roleCondition == "grade_7"))
        {
            days_condition3 = "3_6";
        }
        //如果中层正职，或者中层副职请假7天以上，或一般人员超过30天；分管领导-->局领导审核
        if (roleCondition == "grade_11" || (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(7))>0 && roleCondition.equals("grade_7")) || (createReqVO.getTotalTs().compareTo(BigDecimal.valueOf(30))>0 && roleCondition == "grade_3"))
        {
            days_condition3 = "3_5";
        }

        // 发起 BPM 流程
        Map<String, Object> processInstanceVariables = new HashMap<>();
        processInstanceVariables.put("role_condition", roleCondition);
        processInstanceVariables.put("days_condition3", days_condition3);
        processInstanceVariables.put("days_condition4", days_condition4);
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

}