package cn.iocoder.yudao.module.system.framework.sms.core.client.impl;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.core.KeyValue;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.sms.SmsTemplateMapper;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsReceiveRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsTemplateRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.enums.SmsTemplateAuditStatusEnum;
import cn.iocoder.yudao.module.system.framework.sms.core.property.SmsChannelProperties;
import com.alibaba.fastjson.JSON;
import com.alibaba.xxpt.gateway.shared.api.request.OapiMessageWorkNotificationRequest;
import com.alibaba.xxpt.gateway.shared.api.response.OapiMessageWorkNotificationResponse;
import com.alibaba.xxpt.gateway.shared.client.http.ExecutableClient;
import com.alibaba.xxpt.gateway.shared.client.http.IntelligentGetClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class DGWORKSmsClient extends  AbstractSmsClient {

    private ExecutableClient executableClient;

    public DGWORKSmsClient(SmsChannelProperties properties) {
        super(properties);
        Assert.notEmpty(properties.getApiKey(), "apiKey 不能为空");
        Assert.notEmpty(properties.getApiSecret(), "apiSecret 不能为空");
        initDingClient();
    }
    private void initDingClient() {
        executableClient = ExecutableClient.getInstance();
        executableClient.setDomainName("10.130.146.149:28082");
        executableClient.setProtocal("http");
        // 复用系统后台配置的 apiKey 和 apiSecret
        executableClient.setAccessKey(properties.getApiKey());
        executableClient.setSecretKey(properties.getApiSecret());
        executableClient.init();
    }
    @Override
    public SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId, List<KeyValue<String, Object>> templateParams) throws Throwable {
        SmsTemplateMapper smsTemplateMapper = SpringUtil.getBean(SmsTemplateMapper.class);

        SmsTemplateDO templateDO = smsTemplateMapper.selectOne(
                new LambdaQueryWrapper<SmsTemplateDO>().eq(SmsTemplateDO::getApiTemplateId, apiTemplateId)
        );

        String content = templateDO != null ? templateDO.getContent() : "";
        if (CollUtil.isNotEmpty(templateParams) && StrUtil.isNotBlank(content)) {
            for (KeyValue<String, Object> param : templateParams) {
                String placeholder = "{" + param.getKey() + "}";
                String value = param.getValue() != null ? String.valueOf(param.getValue()) : "";
                content = StrUtil.replace(content, placeholder, value);
            }
        }

        IntelligentGetClient intelligentGetClient = executableClient.newIntelligentGetClient("/message/workNotification");
        OapiMessageWorkNotificationRequest request = new OapiMessageWorkNotificationRequest();
        request.setReceiverIds(mobile);
        request.setTenantId(properties.getSignature());
        request.setBizMsgId(String.valueOf(logId));
//        String textContent = String.format("【OA系统通知】\n消息类型：%s\n详细内容：%s",
//                apiTemplateId, MapUtils.convertMap(templateParams));
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("msgtype", "text");
        msgMap.put("text", MapUtil.builder().put("content", content).build());
        request.setMsg(JsonUtils.toJsonString(msgMap));
        OapiMessageWorkNotificationResponse response = intelligentGetClient.get(request);
        System.out.println("钉钉接口返回详情: " + JSON.toJSONString(response));
        boolean success = response != null && response.getSuccess();
        String errCode = response != null ? String.valueOf(response.getCode()) : "500";
        String errMsg = response != null ? response.getMessage() : "请求政务钉钉无响应";
        // 如果成功，取钉钉返回的 messageId 作为流水号；失败则随机生成一个防止报错
        String serialNo = StrUtil.uuid();

        return new SmsSendRespDTO().setSuccess(success)
                .setSerialNo(serialNo)
                .setApiCode(errCode)
                .setApiMsg(errMsg);
    }

    @Override
    public List<SmsReceiveRespDTO> parseSmsReceiveStatus(String text) throws Throwable {
        throw new UnsupportedOperationException("政务钉钉工作通知暂不支持接收状态解析");
    }

    @Override
    public SmsTemplateRespDTO getSmsTemplate(String apiTemplateId) throws Throwable {
        return new SmsTemplateRespDTO().setId(apiTemplateId).setContent("")
                .setAuditStatus(SmsTemplateAuditStatusEnum.SUCCESS.getStatus()).setAuditReason("");
    }
}
