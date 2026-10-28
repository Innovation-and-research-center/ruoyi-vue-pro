package cn.iocoder.yudao.module.bpm.service.land.listener;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.iocoder.yudao.module.bpm.service.land.LandService;
import cn.iocoder.yudao.module.bpm.service.land.LandServiceImpl;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

@Component
public class BpmLandStatusListener extends BpmProcessInstanceStatusEventListener {
    @Resource
    private LandService landService;

    @Override
    protected String getProcessDefinitionKey() {
        return LandServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        landService.updateStatus(Long.valueOf(event.getBusinessKey()), event.getId(), event.getStatus());
    }
}
