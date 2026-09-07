package cn.iocoder.yudao.module.bpm.service.message;

import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.module.bpm.convert.message.BpmMessageConvert;
import cn.iocoder.yudao.module.bpm.enums.message.BpmMessageEnum;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceApproveReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenProcessInstanceRejectReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * BPM 消息 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class BpmMessageServiceImpl implements BpmMessageService {

    @Resource
    private SmsSendApi smsSendApi;

    @Resource
    private WebProperties webProperties;

     @Resource
     private ConfigApi configApi;

    private boolean isMessageSendEnable() {
        try {
            // TODO: 3. 替换为实际读取“配置管理”参数的代码，例如根据键值获取：
             String enableStr = configApi.getConfigValueByKey("bpm.message.send.enable");
             return Boolean.parseBoolean(enableStr);
        } catch (Exception e) {
            log.error("[isMessageSendEnable][读取流程消息发送开关异常]", e);
            return false; // 发生异常时建议降级为不发送
        }
    }

    private void executeSendMessage(Long userId, String smsTemplateCode, String dingTemplateCode, Map<String, Object> templateParams) {
        // 1. 校验全局发送开关
        if (!isMessageSendEnable()) {
            log.info("[executeSendMessage][BPM消息发送开关已关闭，跳过发送. userId={}]", userId);
        }
        // 2. 发送常规短信
        else if (StringUtils.hasText(smsTemplateCode)) {
            try {
                smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(userId, smsTemplateCode, templateParams));
            } catch (Exception e) {
                log.error("[executeSendMessage][发送短信失败, userId({}) templateCode({})]", userId, smsTemplateCode, e);
            }
        }

        // 3. 发送钉钉消息
        if (StringUtils.hasText(dingTemplateCode)) {
            try {
                smsSendApi.sendSingleSmsToAdmin(BpmMessageConvert.INSTANCE.convert(userId, dingTemplateCode, templateParams));
            } catch (Exception e) {
                log.error("[executeSendMessage][发送钉钉消息失败, userId({}) templateCode({})]", userId, dingTemplateCode, e);
            }
        }
    }

    @Override
    public void sendMessageWhenProcessInstanceApprove(BpmMessageSendWhenProcessInstanceApproveReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        executeSendMessage(reqDTO.getStartUserId(),
                BpmMessageEnum.PROCESS_INSTANCE_APPROVE.getSmsTemplateCode(),
                null, // 需在枚举中补充
                templateParams);
    }

    @Override
    public void sendMessageWhenProcessInstanceReject(BpmMessageSendWhenProcessInstanceRejectReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("reason", reqDTO.getReason());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        executeSendMessage(reqDTO.getStartUserId(),
                BpmMessageEnum.PROCESS_INSTANCE_REJECT.getSmsTemplateCode(),
                null, // 需在枚举中补充
                templateParams);
    }

    @Override
    public void sendMessageWhenTaskAssigned(BpmMessageSendWhenTaskCreatedReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("startUserNickname", reqDTO.getStartUserNickname());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        templateParams.put("dueDate", reqDTO.getDueDate());
        executeSendMessage(reqDTO.getAssigneeUserId(),
                BpmMessageEnum.TASK_ASSIGNED.getSmsTemplateCode(),
                BpmMessageEnum.TASK_ASSIGNED_DING.getSmsTemplateCode(), // 需在枚举中补充
                templateParams);
    }


    @Override
    public void sendMessageWhenTaskTimeout(BpmMessageSendWhenTaskTimeoutReqDTO reqDTO) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("processInstanceName", reqDTO.getProcessInstanceName());
        templateParams.put("taskName", reqDTO.getTaskName());
        templateParams.put("detailUrl", getProcessInstanceDetailUrl(reqDTO.getProcessInstanceId()));
        executeSendMessage(reqDTO.getAssigneeUserId(),
                BpmMessageEnum.TASK_TIMEOUT.getSmsTemplateCode(),
                null, // 需在枚举中补充
                templateParams);
    }

    private String getProcessInstanceDetailUrl(String taskId) {
        return webProperties.getAdminUi().getUrl() + "/bpm/process-instance/detail?id=" + taskId;
    }

}
