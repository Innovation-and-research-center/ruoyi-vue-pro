package cn.iocoder.yudao.module.bpm.service.leave.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.leave.LeaveService;
import cn.iocoder.yudao.module.bpm.service.leave.LeaveServiceImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BpmLeaveStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource
    private LeaveService leaveService;

    @Override
    protected String getProcessDefinitionKey() {
        return LeaveServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        System.out.print("请假流程实例状态发生变化："+event.getStatus());
        leaveService.updateLeaveStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }
}
