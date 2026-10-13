package cn.iocoder.yudao.module.bpm.service.message;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class BpmTaskSmsMessageTest {
    @Test
    void uncheckedOnlySkipsSmsAndLegacyRequestStillSends() {
        check(false, "true", 0);
        check(true, "true", 1);
        check(null, "true", 1);
        check(true, "false", 0);
    }

    private void check(Boolean selected, String global, int smsCount) {
        BpmMessageServiceImpl service = new BpmMessageServiceImpl();
        SmsSendApi sender = mock(SmsSendApi.class);
        ConfigApi config = mock(ConfigApi.class);
        when(config.getConfigValueByKey("bpm.message.send.enable")).thenReturn(global);
        WebProperties web = new WebProperties();
        WebProperties.Ui ui = new WebProperties.Ui();
        ui.setUrl("http://localhost");
        web.setAdminUi(ui);
        ReflectionTestUtils.setField(service, "smsSendApi", sender);
        ReflectionTestUtils.setField(service, "configApi", config);
        ReflectionTestUtils.setField(service, "webProperties", web);
        BpmMessageSendWhenTaskCreatedReqDTO dto = new BpmMessageSendWhenTaskCreatedReqDTO();
        dto.setAssigneeUserId(1L);
        dto.setSendSms(selected);
        service.sendMessageWhenTaskAssigned(dto);
        verify(sender, times(smsCount)).sendSingleSmsToAdmin(argThat(request ->
                "task_notice_zs_sms".equals(request.getTemplateCode())));
        verify(sender).sendSingleSmsToAdmin(argThat(request ->
                "task_notice_ding".equals(request.getTemplateCode())));
        verifyNoMoreInteractions(sender);
    }
}
