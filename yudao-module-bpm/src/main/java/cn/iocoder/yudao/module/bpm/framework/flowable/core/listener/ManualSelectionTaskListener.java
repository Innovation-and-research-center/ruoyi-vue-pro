package cn.iocoder.yudao.module.bpm.framework.flowable.core.listener;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 手动选择审批人监听器
 * 监听任务创建事件，检查流程变量中是否有指定当前节点的审批人
 */
@Component("manualSelectionTaskListener")
public class ManualSelectionTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        // 获取流程变量中的 "NEXT_NODE_ASSIGNEES_MAP"
        // 格式约定：Map<String, Long> -> { "Activity_ID": UserId }
        Object variable = delegateTask.getVariable("PROCESS_MANUAL_SELECT_MAP");
        if (variable instanceof Map) {
            Map<String, Object> assigneesMap = (Map<String, Object>) variable;
            String taskDefinitionKey = delegateTask.getTaskDefinitionKey();

            // 检查是否有针对当前节点的指定人员
            if (assigneesMap.containsKey(taskDefinitionKey)) {
                Object userIdObj = assigneesMap.get(taskDefinitionKey);
                if (userIdObj != null) {
                    delegateTask.setAssignee(String.valueOf(userIdObj));
                }
            }
        }
    }
}