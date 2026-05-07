package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzfy.XzfyMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.flowable.engine.TaskService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class XzfyTimeOutJob implements JobHandler {

    @Resource
    private XzfyMapper xzfyMapper;

    @Resource
    private HolidayService holidayService;

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    // BPMN中定义的“法规科(结果录入)”的任务节点 ID
    private static final String TARGET_ACTIVITY_ID = "Activity_1tghx6r";

    // 超时工作日天数阈值
    private static final int TIMEOUT_WORK_DAYS = 10;

    private static final String TIMEOUT_REMARK = "过期系统自动取消";

    private static final List<String> ALLOW_TIMEOUT_ACTIVITY_IDS = Arrays.asList("Activity_176l096", "Activity_0f53oqt", "Activity_0c9a0ni"
    );
    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过行政复议超时跳转", currentTenantId);
            return "跳过非目标租户";
        }
        log.info("开始自动办结行政复议");
        List<XzfyDO> runningList = xzfyMapper.selectList(new LambdaQueryWrapper<XzfyDO>()
                .isNotNull(XzfyDO::getProcessInstanceId)
                .eq(XzfyDO::getStatus, BpmProcessInstanceStatusEnum.RUNNING.getStatus()));

        if (CollUtil.isEmpty(runningList)) {
            log.info("[XzfyTimeOutJob] 当前没有进行中的行政复议实例。");
            return "成功，无处理数据";
        }

        int successCount = 0;
        for (XzfyDO xzfyDO : runningList) {
            try {
                // 2. 计算是否超过 10 个工作日
                // 使用 BaseDO 继承过来的 createTime 作为流程发起的基准时间
                LocalDateTime startTime = xzfyDO.getSwRq();
                if (startTime == null) {
                    continue;
                }

                // 调用 HolidayService 计算经过 10 个工作日后的“截止期限”
                LocalDateTime deadline = holidayService.addWorkingDays(startTime, TIMEOUT_WORK_DAYS);

                // 3. 判断当前时间是否已经超过了截止期限
                if (LocalDateTime.now().isAfter(deadline)) {
                    boolean jumped = jumpToTargetNode(xzfyDO.getProcessInstanceId());
                    if (jumped) {
                        successCount++;
                    }
                }
            } catch (Exception e) {
                log.error("[XzfyTimeOutJob] 处理流程实例 {} 时发生异常", xzfyDO.getProcessInstanceId(), e);
            }
        }

        log.info("[XzfyTimeOutJob] 执行完成，共成功跳转 {} 个流程实例。", successCount);
        return String.format("成功跳转 %d 个实例", successCount);

    }

    private boolean jumpToTargetNode(String processInstanceId) {
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

        // ================= 【核心拦截：白名单校验】 =================
        // 检查当前所有的活动节点中，是否包含在白名单内的节点
        boolean canTimeout = currentActivityIds.stream().anyMatch(ALLOW_TIMEOUT_ACTIVITY_IDS::contains);

        if (!canTimeout) {
            log.info("[XzfyTimeOutJob] 实例 {} 当前节点 {} 不在允许超时的范围内（已流转到后续阶段或已结束），跳过跳转",
                    processInstanceId, currentActivityIds);
            return false;
        }
        // =========================================================

        // 校验：如果没有办理人变量，直接退出不执行 (之前的防御逻辑)
        Object transactorUserId = runtimeService.getVariable(processInstanceId, "transactorUserId");
        if (transactorUserId == null) {
            log.warn("[XzfyTimeOutJob] 流程实例 {} 缺少 transactorUserId 变量，放弃超时自动完成", processInstanceId);
            return false;
        }

        // ... 正常的伪装完成和跳转逻辑 ...
        for (Task task : currentTasks) {
            taskService.addComment(task.getId(), processInstanceId, "超时系统自动完成");
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus());
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON, "超时系统自动完成");
        }

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(currentActivityIds, TARGET_ACTIVITY_ID)
                .changeState();

        return true;
    }
}
