package cn.iocoder.yudao.module.bpm.service.task.listener;


import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;
import org.flowable.engine.delegate.TaskListener;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class TimeoutSyncListener implements TaskListener{

    @Resource
    private ConfigApi configApi;
    @Override
    public void notify(DelegateTask delegateTask) {
        BpmnModel bpmnModel = CommandContextUtil.getProcessEngineConfiguration()
                .getRepositoryService()
                .getBpmnModel(delegateTask.getProcessDefinitionId());

        FlowElement flowElement = bpmnModel.getFlowElement(delegateTask.getTaskDefinitionKey());
        if (!(flowElement instanceof UserTask)) {
            return;
        }
        UserTask userTask = (UserTask) flowElement;

        boolean isCustomDateSet = false;

        // 2. 查找是否有边界定时器
        List<BoundaryEvent> boundaryEvents = userTask.getBoundaryEvents();
//        if (boundaryEvents == null || boundaryEvents.isEmpty()) {
//            return;
//        }

        for (BoundaryEvent event : boundaryEvents) {
            // 只处理定时事件
            if (event.getEventDefinitions() != null && !event.getEventDefinitions().isEmpty()
                    && event.getEventDefinitions().get(0) instanceof TimerEventDefinition) {

                TimerEventDefinition timerDef = (TimerEventDefinition) event.getEventDefinitions().get(0);
                String timeDuration = timerDef.getTimeDuration();

                // 3. 解析时间并设置 DueDate
                if (timeDuration != null && timeDuration.startsWith("P")) {
                    try {
                        // 解析 ISO 8601 (如 PT96H)
                        Duration duration = Duration.parse(timeDuration);
                        long seconds = duration.getSeconds();

                        // 计算到期时间：当前时间 + 持续时间
                        Date dueDate = new Date(System.currentTimeMillis() + (seconds * 1000));

                        // 【核心】设置给 Task
                        delegateTask.setDueDate(dueDate);

                        isCustomDateSet = true;
                        log.info("任务[{}] 根据边界定时器同步超时时间成功: {}", delegateTask.getName(), timeDuration);
                        break;

                    } catch (Exception e) {
                        log.warn("任务[{}] 自动同步超时时间失败, 格式: {}", delegateTask.getName(), timeDuration);
                    }
                }
                break; // 找到一个就处理退出
            }
        }

        if (!isCustomDateSet) {
            String timeout = configApi.getConfigValueByKey("bpm_task_timeout");
            Date defaultDueDate = new Date(System.currentTimeMillis() + (Duration.ofHours(Long.parseLong(timeout)).getSeconds() * 1000));
            delegateTask.setDueDate(defaultDueDate);
            log.info("任务[{}] 未配置有效定时器，已设置默认超时时间: {} 小时", delegateTask.getName(), timeout);
        }
    }
}
