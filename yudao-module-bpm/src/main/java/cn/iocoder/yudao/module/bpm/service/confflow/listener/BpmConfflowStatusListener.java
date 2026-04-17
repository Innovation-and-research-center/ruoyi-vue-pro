package cn.iocoder.yudao.module.bpm.service.confflow.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.xzfy.XzfyService;
import cn.iocoder.yudao.module.bpm.service.xzfy.XzfyServiceImpl;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
@Component
public class BpmConfflowStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private XzfyService xzfyService;
    @Override
    protected String getProcessDefinitionKey() {
        return XzfyServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        xzfyService.updateXzfyStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());

    }
}
