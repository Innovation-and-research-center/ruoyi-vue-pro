package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzfy.XzfyMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;
import org.flowable.engine.TaskService;

import javax.annotation.Resource;
import java.time.LocalDateTime;
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
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过省厅收文同步", currentTenantId);
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

        // 3. 拦截：如果当前已经在目标节点了，则不需要重复跳
        if (currentActivityIds.size() == 1 && currentActivityIds.contains(TARGET_ACTIVITY_ID)) {
            return false;
        }

        log.info("[XzfyTimeOutJob] 实例 {} 触发超时跳转: 当前节点 {} -> 目标节点 {}",
                processInstanceId, currentActivityIds, TARGET_ACTIVITY_ID);

        for (Task task : currentTasks) {
            // 参数：任务ID, 流程实例ID, 备注内容
            taskService.addComment(task.getId(), processInstanceId, TIMEOUT_REMARK);
        }
        // 4. 调用 Flowable 官方 API 动态移动执行实例 (ChangeActivityStateBuilder)
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                // 自动将现有的一个或多个并发活动全部取消，并转移到单一目标节点
                .moveActivityIdsToSingleActivityId(currentActivityIds, TARGET_ACTIVITY_ID)
                .changeState();
        return true;
    }

}
