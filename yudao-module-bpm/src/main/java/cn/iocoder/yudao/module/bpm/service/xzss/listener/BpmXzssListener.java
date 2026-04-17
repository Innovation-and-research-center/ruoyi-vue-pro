package cn.iocoder.yudao.module.bpm.service.xzss.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.senddoc.SendDocService;
import cn.iocoder.yudao.module.bpm.service.senddoc.SendDocServiceImpl;
import cn.iocoder.yudao.module.bpm.service.xzss.XzssService;
import cn.iocoder.yudao.module.bpm.service.xzss.XzssServiceImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BpmXzssListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private XzssService xzssService; ;
    @Override
    protected String getProcessDefinitionKey() {
        return XzssServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        xzssService.updateXzssStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());

    }
}
