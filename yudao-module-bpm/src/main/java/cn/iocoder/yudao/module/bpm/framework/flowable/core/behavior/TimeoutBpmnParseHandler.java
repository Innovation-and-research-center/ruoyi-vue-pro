package cn.iocoder.yudao.module.bpm.framework.flowable.core.behavior;

import cn.iocoder.yudao.module.bpm.service.task.listener.TimeoutSyncListener;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.flowable.bpmn.model.FlowableListener; // 【修正这里】
import org.flowable.bpmn.model.ImplementationType; // 【新增引用】用于获取常量
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TimeoutBpmnParseHandler extends AbstractBpmnParseHandler<UserTask> {
    @Resource
    private TimeoutSyncListener timeoutSyncListener;

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return UserTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, UserTask userTask) {
//        if (userTask.getBoundaryEvents() != null && !userTask.getBoundaryEvents().isEmpty()) {

            // 创建 FlowableListener 对象 (原 ActivitiListener)
            FlowableListener listener = new FlowableListener();

            // 设置事件类型: create (任务创建时触发)
            listener.setEvent(TaskListener.EVENTNAME_CREATE);

            // 设置实现类型: delegateExpression (委托表达式)
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);

            // 设置具体实现: 指向 Spring 容器中的 Bean ID
            listener.setImplementation("${timeoutSyncListener}");

            // 添加到 UserTask 的监听器列表中
            userTask.getTaskListeners().add(listener);
//        }

    }
}
