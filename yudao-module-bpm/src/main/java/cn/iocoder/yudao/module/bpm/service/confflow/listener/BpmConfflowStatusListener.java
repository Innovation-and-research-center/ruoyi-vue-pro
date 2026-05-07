package cn.iocoder.yudao.module.bpm.service.confflow.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.confflow.ConfflowService;
import cn.iocoder.yudao.module.bpm.service.confflow.ConfflowServiceImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
@Component
public class BpmConfflowStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private ConfflowService confflowService; ;
    @Override
    protected String getProcessDefinitionKey() {
        return ConfflowServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {

        confflowService.updateConfflowStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());

    }
}
