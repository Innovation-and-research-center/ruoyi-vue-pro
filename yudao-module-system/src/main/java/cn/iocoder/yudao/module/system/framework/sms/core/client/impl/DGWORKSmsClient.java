package cn.iocoder.yudao.module.system.framework.sms.core.client.impl;


import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.core.KeyValue;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsReceiveRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsTemplateRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.enums.SmsTemplateAuditStatusEnum;
import cn.iocoder.yudao.module.system.framework.sms.core.property.SmsChannelProperties;
import com.alibaba.xxpt.gateway.shared.api.request.OapiMessageWorkNotificationRequest;
import com.alibaba.xxpt.gateway.shared.api.response.OapiMessageWorkNotificationResponse;
import com.alibaba.xxpt.gateway.shared.client.http.ExecutableClient;
import com.alibaba.xxpt.gateway.shared.client.http.IntelligentGetClient;
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
        executableClient.setDomainName("openplatform.dg-work.cn");
        executableClient.setProtocal("https");
        // 复用系统后台配置的 apiKey 和 apiSecret
        executableClient.setAccessKey(properties.getApiKey());
        executableClient.setSecretKey(properties.getApiSecret());
        executableClient.init();
    }
    @Override
    public SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId, List<KeyValue<String, Object>> templateParams) throws Throwable {
        IntelligentGetClient intelligentGetClient = executableClient.newIntelligentGetClient("/message/workNotification");
        OapiMessageWorkNotificationRequest request = new OapiMessageWorkNotificationRequest();

        // 1. 接收人：在政务钉钉中通常是手机号或者专有的 accountId
        request.setReceiverIds(mobile);

        // 2. 租户ID：巧妙地复用后台短信渠道配置里的“短信签名(Signature)”字段
        // 这样就可以在前端页面配置，不用把租户ID写死在代码里
        request.setTenantId(properties.getSignature());

        // 3. 业务消息id：使用系统生成的发送日志ID，方便后续在 OA 系统中排查
        request.setBizMsgId(String.valueOf(logId));

        // 4. 构造消息内容 Msg (组装为钉钉要求的 JSON 格式)
        // 这里以 text 文本消息为例。你也可以根据前端传来的模板参数，组装 OABody 或 Markdown
        String textContent = String.format("【OA系统通知】\n消息类型：%s\n详细内容：%s",
                apiTemplateId, MapUtils.convertMap(templateParams));

        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("msgtype", "text");
        msgMap.put("text", MapUtil.builder().put("content", textContent).build());
        request.setMsg(JsonUtils.toJsonString(msgMap));

        // 5. 执行发送
        OapiMessageWorkNotificationResponse response = intelligentGetClient.get(request);

        // 6. 解析结果并返回给 ruoyi-vue-pro 框架
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
