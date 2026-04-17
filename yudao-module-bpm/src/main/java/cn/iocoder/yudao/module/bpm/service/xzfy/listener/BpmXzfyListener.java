package cn.iocoder.yudao.module.bpm.service.xzfy.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.senddoc.SendDocService;
import cn.iocoder.yudao.module.bpm.service.senddoc.SendDocServiceImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BpmXzfyListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private SendDocService sendDocService;
    @Override
    protected String getProcessDefinitionKey() {
        return SendDocServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        sendDocService.updateSendDocStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());

    }
}
