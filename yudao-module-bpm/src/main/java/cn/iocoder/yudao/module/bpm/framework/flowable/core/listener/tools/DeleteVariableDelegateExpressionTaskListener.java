package cn.iocoder.yudao.module.bpm.framework.flowable.core.listener.tools;

import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DeleteVariableDelegateExpressionTaskListener implements TaskListener {

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RuntimeService runtimeService;
    @Override
    public void notify(DelegateTask delegateTask) {
        String processDefId = delegateTask.getProcessDefinitionId();
        String processInstanceId = delegateTask.getProcessInstanceId();
        String taskDefKey = delegateTask.getTaskDefinitionKey();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
        FlowElement flowElement = bpmnModel.getFlowElement(taskDefKey);
        if (flowElement instanceof UserTask) {
            UserTask userTask = (UserTask) flowElement;
            Map<String, List<ExtensionElement>> extensionElements = userTask.getExtensionElements();

            // 这一步建议封装成工具方法，逻辑同之前所述
            String myValue = getNestedProperty(extensionElements, "delete_variable");
            System.out.println("获取到的 delete_variable 属性值: " + myValue);
            if (myValue != null && !myValue.trim().isEmpty()) {
                // 按逗号分割字符串
                String[] variableNames = myValue.split(",");

                for (String variableName : variableNames) {
                    // 去除可能存在的首尾空格
                    String trimmedName = variableName.trim();

                    if (!trimmedName.isEmpty()) {
                        // 1. 从 RuntimeService 中移除（流程实例级别）
                        runtimeService.removeVariable(processInstanceId, trimmedName);

                        // 2. 从 DelegateTask 中移除（当前任务级别，如果是局部变量）
                        delegateTask.removeVariable(trimmedName);

                        log.info("已删除变量: {}", trimmedName);
                    }
                }
            }
//            runtimeService.removeVariable(processInstanceId, myValue);
//            delegateTask.removeVariable(myValue);
        }

    }

    private String getNestedProperty(Map<String, List<ExtensionElement>> extensionElements, String targetName) {
        if (extensionElements.containsKey("properties")) {
            for (ExtensionElement wrapper : extensionElements.get("properties")) {
                Map<String, List<ExtensionElement>> children = wrapper.getChildElements();
                if (children.containsKey("property")) {
                    for (ExtensionElement prop : children.get("property")) {
                        String name = prop.getAttributeValue(null, "name");
                        if (targetName.equals(name)) {
                            return prop.getAttributeValue(null, "value");
                        }
                    }
                }
            }
        }
        return null;
    }
}
