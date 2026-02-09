package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.user;

import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateStrategy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept.AbstractBpmTaskCandidateDeptLeaderStrategy;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.service.userdept.UserDeptService;
import com.google.common.collect.Sets;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;

@Component
public class BpmTaskCandidateLeaderDeptStrategy extends AbstractBpmTaskCandidateDeptLeaderStrategy {

    @Resource
    private UserDeptService userDeptService;


    @Resource
    @Lazy // 避免循环依赖
    private BpmProcessInstanceService processInstanceService;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.LEADER_DEPT;
    }

    @Override
    public boolean isParamRequired() {
        return false;
    }

    @Override
    public void validateParam(String param) {

    }

    @Override
    public LinkedHashSet<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        // 获得流程发起人
        ProcessInstance processInstance = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        Long startUserId = NumberUtils.parseLong(processInstance.getStartUserId());
        // 获取发起人的部门负责人
        return getLeaderDept(startUserId, param);
    }

    @Override
    public LinkedHashSet<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId, String param,
                                              Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        // 获取发起人的部门负责人
        return getLeaderDept(startUserId, param);
    }

    private LinkedHashSet<Long> getLeaderDept(Long startUserId, String param) {
        DeptRespDTO dept = super.getStartUserDept(startUserId);
        if (dept == null) {
            return Sets.newLinkedHashSet();
        }
        List<Long> assignees =new ArrayList<>(userDeptService.getUserIdsByDeptId(dept.getId())) ;
        return new LinkedHashSet<>(assignees);
    }
}
