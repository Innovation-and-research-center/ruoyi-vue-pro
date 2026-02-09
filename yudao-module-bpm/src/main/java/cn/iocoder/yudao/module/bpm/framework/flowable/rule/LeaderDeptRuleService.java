package cn.iocoder.yudao.module.bpm.framework.flowable.rule;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component("leaderDeptRuleService")
public class LeaderDeptRuleService {
    public Set<Long> calculateApprovers(DelegateExecution execution) {
        Set<Long> candidateUserIds = new HashSet<>();

        // 2. 获取流程启动时或流转中设置的变量
        // 例如：获取请假天数
        Long days = (Long) execution.getVariable("days");
        // 例如：获取发起人 ID
        String startUserId = (String) execution.getVariable("PROCESS_INITIATOR");

        // 3. 编写你的业务逻辑
        if (days != null && days > 3) {
            // 如果大于3天，由总经理审批 (假设 ID 为 101)
            candidateUserIds.add(101L);
        } else {
            // 否则由部门经理审批 (假设 ID 为 102)
            candidateUserIds.add(102L);
        }

        // 注意：这里返回的是系统中的 User ID (admin_users 表的 id)
        return candidateUserIds;
    }
}
