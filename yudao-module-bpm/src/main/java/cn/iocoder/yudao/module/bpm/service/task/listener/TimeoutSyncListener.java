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


    private volatile String cachedTimeout = null;
    private volatile long lastFetchTime = 0L;
    private static final long CACHE_DURATION_MS = 5 * 60 * 1000L;

    private String getDefaultTimeout() {
        long currentTime = System.currentTimeMillis();
        // 双重检查锁（DCL），保证在多线程高并发下的高性能和线程安全
        if (cachedTimeout == null || (currentTime - lastFetchTime) > CACHE_DURATION_MS) {
            synchronized (this) {
                if (cachedTimeout == null || (currentTime - lastFetchTime) > CACHE_DURATION_MS) {
                    try {
                        cachedTimeout = configApi.getConfigValueByKey("bpm_task_timeout");
                        lastFetchTime = System.currentTimeMillis();
                        log.debug("已刷新 BPM 任务默认超时时间缓存: {}", cachedTimeout);
                    } catch (Exception e) {
                        log.error("获取 BPM 任务超时配置失败", e);
                        // 兜底策略：如果查询失败，且之前没缓存，默认给 24 小时防止流程卡死
                        if (cachedTimeout == null) {
                            cachedTimeout = "24";
                        }
                    }
                }
            }
        }
        return cachedTimeout;
    }

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
            String timeoutStr = getDefaultTimeout();
            try {
                long timeoutHours = Long.parseLong(timeoutStr);
                Date defaultDueDate = new Date(System.currentTimeMillis() + (Duration.ofHours(timeoutHours).getSeconds() * 1000));
                delegateTask.setDueDate(defaultDueDate);
                log.info("任务[{}] 未配置有效定时器，已设置默认超时时间: {} 小时", delegateTask.getName(), timeoutStr);
            } catch (NumberFormatException e) {
                log.error("解析 BPM 任务超时配置失败，配置值不是有效的数字: {}", timeoutStr, e);
            }
        }
    }
}
