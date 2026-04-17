package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class XzssTimeOutJob implements JobHandler {

    @Resource
    private XzssMapper xzssMapper;

    @Resource
    private HolidayService holidayService;

    @Resource
    private TaskService taskService;

    @Resource
    private RuntimeService runtimeService;

    // BPMN中定义的“法规科(结果录入)”的任务节点 ID
    private static final String TARGET_ACTIVITY_ID = "Activity_01ukxlz";

    // 超时工作日天数阈值
    private static final int TIMEOUT_DAYS = 15;

    private static final String TIMEOUT_REMARK = "过期系统自动取消";
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
                boolean jumped = jumpToTargetNode(xzssDO.getProcessInstanceId());
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
