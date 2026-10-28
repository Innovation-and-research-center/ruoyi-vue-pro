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
public class XzssTimeOutJob implements JobHandler {

    @Resource
    private XzssMapper xzssMapper;

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private ManagementService managementService;

    // 新旧流程共用的汇合后“法规科办理”，仍使用 transactorUserId。
    private static final String TARGET_ACTIVITY_ID = "Activity_01ukxlz";
    private static final String JOIN_ACTIVITY_ID = "Gateway_1cwnri4";

    // 沿用收文日期起算 15 个自然日。
    private static final int TIMEOUT_DAYS = 15;

    private static final String TIMEOUT_REMARK = "过期系统自动完成";

    private static final List<String> ALLOW_TIMEOUT_ACTIVITY_IDS = Arrays.asList(
            "Activity_06bpq99", "Activity_16wkjql", "Activity_1q3wpn8", "Activity_0xurhyv"
    );
    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过", currentTenantId);
            return "跳过非目标租户";
        }
        log.info("开始自动办结行政诉讼");
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(TIMEOUT_DAYS);
        List<XzssDO> timeoutList = xzssMapper.selectList(new LambdaQueryWrapper<XzssDO>()
                .isNotNull(XzssDO::getProcessInstanceId)
                .eq(XzssDO::getStatus, BpmProcessInstanceStatusEnum.RUNNING.getStatus())
                .isNotNull(XzssDO::getSfyjgRq) // 确保时间字段不为空
                .le(XzssDO::getSwRq, thresholdDate));

        if (CollUtil.isEmpty(timeoutList)) {
            log.info("[XzssTimeOutJob] 当前没有进行中的行政诉讼实例。");
            return "成功，无处理数据";
        }

        int successCount = 0;
        for (XzssDO xzssDO : timeoutList) {
            try {
                // 超时意见、状态和跳转共用事务，失败时整体回滚。
                boolean jumped = managementService.executeCommand(
                        context -> jumpToTargetNode(xzssDO.getProcessInstanceId()));
                if (jumped) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("[XzssTimeOutJob] 处理流程实例 {} 时发生异常", xzssDO.getProcessInstanceId(), e);
            }
        }

        log.info("[XzssTimeOutJob] 执行完成，共成功跳转 {} 个流程实例。", successCount);
        return String.format("成功跳转 %d 个实例", successCount);

    }

    private boolean jumpToTargetNode(String processInstanceId) {
        // 1. 获取当前实例的所有活动任务
        List<Task> currentTasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (CollUtil.isEmpty(currentTasks)) {
            return false;
        }

        // 2. 获取所有当前执行的活动节点 ID 集合
        List<String> currentActivityIds = currentTasks.stream()
                .map(Task::getTaskDefinitionKey)
                .distinct()
                .collect(Collectors.toList());

        // 只处理本阶段的前期支线，不误结束后续办理或上诉阶段任务。
        boolean canTimeout = currentActivityIds.stream().allMatch(ALLOW_TIMEOUT_ACTIVITY_IDS::contains);

        if (!canTimeout) {
            log.info("[XzssTimeOutJob] 实例 {} 当前节点 {} 不在允许超时的范围内（已流转到后续阶段或已结束），跳过跳转",
                    processInstanceId, currentActivityIds);
            return false;
        }

        // 包含网关上已完成、正在等待的支线（这些执行没有对应待办）。
        List<Execution> executions = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId).list();
        boolean hasOtherStage = executions.stream().map(Execution::getActivityId)
                .filter(java.util.Objects::nonNull)
                .anyMatch(id -> !ALLOW_TIMEOUT_ACTIVITY_IDS.contains(id) && !JOIN_ACTIVITY_ID.equals(id));
        if (hasOtherStage) {
            log.warn("[XzssTimeOutJob] 实例 {} 存在本阶段之外的执行节点，跳过跳转", processInstanceId);
            return false;
        }
        // 合并同一父执行下的多实例根，避免按活动 ID 跳转生成多个后续办理。
        List<String> branchExecutionIds = executions.stream()
                .filter(execution -> processInstanceId.equals(execution.getParentId()))
                .map(Execution::getId).collect(Collectors.toList());
        if (branchExecutionIds.isEmpty()) {
            return false;
        }

        // ================= 【核心拦截：办理人校验】 =================
        Object transactorUserId = runtimeService.getVariable(processInstanceId, "transactorUserId");
        if (transactorUserId == null) {
            log.warn("[XzssTimeOutJob] 流程实例 {} 缺少 transactorUserId 变量，放弃超时自动完成", processInstanceId);
            return false;
        }
        // =========================================================

        log.info("[XzssTimeOutJob] 实例 {} 触发超时跳转: 伪装完成并跳转 {} -> {}",
                processInstanceId, currentActivityIds, TARGET_ACTIVITY_ID);

        for (Task task : currentTasks) {
            // 参数：任务ID, 流程实例ID, 备注内容
            taskService.addComment(task.getId(), processInstanceId, TIMEOUT_REMARK);
            // 强行注入状态，伪装正常通过
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus());
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON, TIMEOUT_REMARK);
        }

        // 4. 调用 Flowable 官方 API 动态移动执行实例
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveExecutionsToSingleActivityId(branchExecutionIds, TARGET_ACTIVITY_ID)
                .changeState();
        return true;
    }

}
