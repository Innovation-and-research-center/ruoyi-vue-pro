package cn.iocoder.yudao.module.bpm.service.receivedoc.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocServiceImpl;

import javax.annotation.Resource;

public class BpmReceiveStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource
    private ReceiveDocService receiveDocService;


    @Override
    protected String getProcessDefinitionKey() {
        return ReceiveDocServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        receiveDocService.updateReceiveStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());

    }
}
