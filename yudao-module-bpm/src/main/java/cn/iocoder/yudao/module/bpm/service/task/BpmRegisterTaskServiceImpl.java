package cn.iocoder.yudao.module.bpm.service.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmRegisterTaskUtils;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.REGISTER_SUBMIT_MODE;

/**
 * 登记类任务 Service 实现。
 */
@Service
@Slf4j
public class BpmRegisterTaskServiceImpl implements BpmRegisterTaskService {

    private static final String COMPLETE_ON_SUBMIT = "complete_on_submit";

    @Resource
    private org.flowable.engine.TaskService flowableTaskService;
    @Resource
    private org.flowable.engine.RepositoryService repositoryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int claim(Long userId, String processInstanceId) {
        if (userId == null || StrUtil.isBlank(processInstanceId)) {
            return 0;
        }
        List<Task> registerTasks = getCompleteOnSubmitTasks(processInstanceId);
        if (CollUtil.isEmpty(registerTasks)) {
            log.warn("[claim][processInstanceId({}) 未找到登记任务]", processInstanceId);
            return 0;
        }
        String userIdStr = String.valueOf(userId);
        for (Task task : registerTasks) {
            flowableTaskService.setAssignee(task.getId(), userIdStr);
            flowableTaskService.setOwner(task.getId(), userIdStr);
            flowableTaskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS,
                    BpmTaskStatusEnum.RUNNING.getStatus());
        }
        return registerTasks.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int completeOnSubmit(Long userId, String processInstanceId,
                                Map<String, Object> processVariables) {
        if (userId == null || StrUtil.isBlank(processInstanceId)) {
            return 0;
        }
        List<Task> registerTasks = getCompleteOnSubmitTasks(processInstanceId);
        if (CollUtil.isEmpty(registerTasks)) {
            log.warn("[completeOnSubmit][processInstanceId({}) 未找到自动完成的登记任务]", processInstanceId);
            return 0;
        }

        String userIdStr = String.valueOf(userId);
        for (Task task : registerTasks) {
            log.info("[completeOnSubmit][processInstanceId({}) 自动完成登记任务 taskId({}) taskKey({}) taskName({})]",
                    processInstanceId, task.getId(), task.getTaskDefinitionKey(), task.getName());
            flowableTaskService.setAssignee(task.getId(), userIdStr);
            flowableTaskService.setOwner(task.getId(), userIdStr);
            flowableTaskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_STATUS,
                    BpmTaskStatusEnum.APPROVE.getStatus());
            flowableTaskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON,
                    "提交业务表单并完成登记");
            flowableTaskService.complete(task.getId(), processVariables);
        }
        return registerTasks.size();
    }

    private List<Task> getCompleteOnSubmitTasks(String processInstanceId) {
        return flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .list()
                .stream()
                .filter(this::isCompleteOnSubmitRegisterTask)
                .collect(Collectors.toList());
    }

    private boolean isCompleteOnSubmitRegisterTask(Task task) {
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        org.flowable.bpmn.model.FlowElement flowElement =
                cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils
                        .getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        return BpmRegisterTaskUtils.isRegisterTask(flowElement)
                && StrUtil.equals(COMPLETE_ON_SUBMIT,
                BpmRegisterTaskUtils.getProperty(flowElement, REGISTER_SUBMIT_MODE));
    }

}
