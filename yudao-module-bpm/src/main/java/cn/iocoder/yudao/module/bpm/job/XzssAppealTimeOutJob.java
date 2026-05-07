package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class XzssAppealTimeOutJob implements JobHandler {

    @Resource
    private XzssMapper xzssMapper;

    @Resource
    private HolidayService holidayService;

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    // 上诉阶段的目标节点：法规科办理(上诉)
    private static final String TARGET_ACTIVITY_ID = "Activity_0gfzyss";

    // 超时天数阈值 (可根据业务调整)
    private static final int TIMEOUT_DAYS = 15;

    private static final String TIMEOUT_REMARK = "上诉阶段过期系统自动完成";

    // 允许触发跳转的白名单节点：局长(上诉)、分管领导(上诉)、相关单位(上诉)
    private static final List<String> ALLOW_TIMEOUT_ACTIVITY_IDS = Arrays.asList(
            "Activity_03i9nb5", "Activity_1j3mjl5", "Activity_0c22of2"
    );

    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            return "跳过非目标租户";
        }

        log.info("开始自动办结行政诉讼-上诉阶段");
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(TIMEOUT_DAYS);

        // 查询正在运行且可能超时的上诉相关记录
        List<XzssDO> runningList = xzssMapper.selectList(new LambdaQueryWrapper<XzssDO>()
                .isNotNull(XzssDO::getProcessInstanceId)
                .eq(XzssDO::getStatus, BpmProcessInstanceStatusEnum.RUNNING.getStatus())
                .le(XzssDO::getSwRq, thresholdDate));

        if (CollUtil.isEmpty(runningList)) {
            return "成功，无处理数据";
        }

        int successCount = 0;
        for (XzssDO xzssDO : runningList) {
            try {
                boolean jumped = jumpToAppealTargetNode(xzssDO.getProcessInstanceId());
                if (jumped) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("[XzssAppealTimeOutJob] 处理流程实例 {} 时发生异常", xzssDO.getProcessInstanceId(), e);
            }
        }

        return String.format("上诉阶段成功跳转 %d 个实例", successCount);
    }

    private boolean jumpToAppealTargetNode(String processInstanceId) {
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (CollUtil.isEmpty(currentTasks)) {
            return false;
        }

        List<String> currentActivityIds = currentTasks.stream()
                .map(Task::getTaskDefinitionKey)
                .distinct()
                .collect(Collectors.toList());

        // 1. 白名单校验：必须在指定的上诉审批节点才触发
        boolean canTimeout = currentActivityIds.stream().anyMatch(ALLOW_TIMEOUT_ACTIVITY_IDS::contains);
        if (!canTimeout) {
            return false;
        }

        // 2. 核心变量校验：上诉阶段使用的是 lawsuitUserId
        Object lawsuitUserId = runtimeService.getVariable(processInstanceId, "lawsuitUserId");
        if (lawsuitUserId == null) {
            log.warn("[XzssAppealTimeOutJob] 流程实例 {} 缺少 lawsuitUserId 变量，放弃自动完成", processInstanceId);
            return false;
        }

        // 3. 执行伪装完成并跳转
        log.info("[XzssAppealTimeOutJob] 实例 {} 触发上诉超时跳转 -> {}", processInstanceId, TARGET_ACTIVITY_ID);

        for (Task task : currentTasks) {
            taskService.addComment(task.getId(), processInstanceId, TIMEOUT_REMARK);
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus());
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON, TIMEOUT_REMARK);
        }

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(currentActivityIds, TARGET_ACTIVITY_ID)
                .changeState();

        return true;
    }
}