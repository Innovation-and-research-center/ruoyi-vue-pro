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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.Execution;
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
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private ManagementService managementService;

    // 上诉阶段的目标节点：法规科办理(上诉)
    private static final String TARGET_ACTIVITY_ID = "Activity_0gfzyss";
    private static final String JOIN_ACTIVITY_ID = "Gateway_0trntwd";

    // 沿用收文日期起算 15 个自然日。
    private static final int TIMEOUT_DAYS = 15;

    private static final String TIMEOUT_REMARK = "上诉阶段过期系统自动完成";

    // 保留旧流程节点，新增可选的法规科人员办理(上诉)。
    private static final List<String> ALLOW_TIMEOUT_ACTIVITY_IDS = Arrays.asList(
            "Activity_03i9nb5", "Activity_1j3mjl5", "Activity_0c22of2", "Activity_1g5bzru"
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
                // 超时意见、状态和跳转共用事务，失败时整体回滚。
                boolean jumped = managementService.executeCommand(
                        context -> jumpToAppealTargetNode(xzssDO.getProcessInstanceId()));
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
        boolean canTimeout = currentActivityIds.stream().allMatch(ALLOW_TIMEOUT_ACTIVITY_IDS::contains);
        if (!canTimeout) {
            return false;
        }

        // 包含网关上的等待执行，并避免跨阶段强制跳转。
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId).list();
        boolean hasOtherStage = executions.stream().map(Execution::getActivityId)
                .filter(java.util.Objects::nonNull)
                .anyMatch(id -> !ALLOW_TIMEOUT_ACTIVITY_IDS.contains(id) && !JOIN_ACTIVITY_ID.equals(id));
        if (hasOtherStage) {
            log.warn("[XzssAppealTimeOutJob] 实例 {} 存在本阶段之外的执行节点，跳过跳转", processInstanceId);
            return false;
        }
        // 同父执行的多实例根统一合并，只生成一份后续法规科办理。
        List<String> branchExecutionIds = executions.stream()
                .filter(execution -> processInstanceId.equals(execution.getParentId()))
                .map(Execution::getId).collect(Collectors.toList());
        if (branchExecutionIds.isEmpty()) {
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
                .moveExecutionsToSingleActivityId(branchExecutionIds, TARGET_ACTIVITY_ID)
                .changeState();

        return true;
    }
}
