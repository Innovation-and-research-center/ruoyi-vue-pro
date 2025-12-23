package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import com.google.common.collect.Sets;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class BpmTaskCandidateLastNodeSelectStrategy extends AbstractBpmTaskCandidateDeptLeaderStrategy{
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private BpmProcessInstanceService processInstanceService;

    @Override
    public BpmTaskCandidateStrategyEnum getStrategy() {
        return BpmTaskCandidateStrategyEnum.MANUAL_SELECTED;
    }

    @Override
    public void validateParam(String param) {}

    @Override
    public boolean isParamRequired() {
        return false;
    }

    @Override
    public LinkedHashSet<Long> calculateUsersByTask(DelegateExecution execution, String param) {
        ProcessInstance processInstance = processInstanceService.getProcessInstance(execution.getProcessInstanceId());
        Assert.notNull(processInstance, "流程实例({})不能为空", execution.getProcessInstanceId());
        Map<String, List<Long>> nextNodeSelectAssignees = FlowableUtils.getLastNodeSelectAssignees(processInstance);
        Assert.notNull(nextNodeSelectAssignees, "流程实例({}) 的手动选择审批人不能为空",
                execution.getProcessInstanceId());
        // 获得审批人
        List<Long> assignees = nextNodeSelectAssignees.get(execution.getCurrentActivityId());
        return CollUtil.isNotEmpty(assignees) ? new LinkedHashSet<>(assignees) : Sets.newLinkedHashSet();
    }

    @Override
    public LinkedHashSet<Long> calculateUsersByActivity(BpmnModel bpmnModel, String activityId, String param,
                                                        Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        if (processVariables == null) {
            return Sets.newLinkedHashSet();
        }
        Map<String, List<Long>> nextNodeSelectAssignees = FlowableUtils.getLastNodeSelectAssignees(processVariables);
        if (nextNodeSelectAssignees == null) {
            return Sets.newLinkedHashSet();
        }
        // 获得审批人
        List<Long> assignees = nextNodeSelectAssignees.get(activityId);
        return CollUtil.isNotEmpty(assignees) ? new LinkedHashSet<>(assignees) : Sets.newLinkedHashSet();
    }
}
