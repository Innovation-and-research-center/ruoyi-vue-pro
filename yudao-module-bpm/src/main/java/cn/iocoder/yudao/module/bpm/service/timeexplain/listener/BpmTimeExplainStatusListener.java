package cn.iocoder.yudao.module.bpm.service.timeexplain.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.leave.LeaveServiceImpl;
import cn.iocoder.yudao.module.bpm.service.timeexplain.TimeExplainService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BpmTimeExplainStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private TimeExplainService timeExplainService;

    @Override
    protected String getProcessDefinitionKey() {
        return LeaveServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        timeExplainService.updateTimeExplainStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }
}
