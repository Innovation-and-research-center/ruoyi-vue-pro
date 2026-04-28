package cn.iocoder.yudao.module.system.framework.sms.core.client.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpRequest;
import cn.iocoder.yudao.framework.common.core.KeyValue;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsTemplateDO;
import cn.iocoder.yudao.module.system.dal.mysql.sms.SmsTemplateMapper;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsReceiveRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.client.dto.SmsTemplateRespDTO;
import cn.iocoder.yudao.module.system.framework.sms.core.enums.SmsTemplateAuditStatusEnum;
import cn.iocoder.yudao.module.system.framework.sms.core.property.SmsChannelProperties;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ZhenShanSmsClient extends AbstractSmsClient {

    private static final String EC_NAME = "义乌市数据管理中心";
    private static final String AP_ID = "ywzgj2";
    private static final String S_KEY = "b32d21ca6fbe4b31a3872b36ab3fbdda";
    private static final String SIGN = "mkEoQtYt3";
    private static final String SECRET_KEY = "Ziguiju2@1234";
    private static final String SUBMIT_URL = "http://10.130.146.149:28082/sms";

    public ZhenShanSmsClient(SmsChannelProperties properties){
        super(properties);
        Assert.notEmpty(properties.getApiKey(), "apiKey 不能为空");
    }
    @Override
    public SmsSendRespDTO sendSms(Long sendLogId, String mobile, String apiTemplateId, List<KeyValue<String, Object>> templateParams) throws Throwable {


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
        String addSerial = ""; // 扩展码，按样例为空字符串 [cite: 9]
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(EC_NAME)
                .append(AP_ID)
                .append(SECRET_KEY)
                .append(mobile)
                .append(content)
                .append(SIGN)
                .append(addSerial); // [cite: 9]

        String mac = DigestUtil.md5Hex(stringBuilder.toString());
        Map<String, Object> params = new HashMap<>();
        params.put("ecName", EC_NAME);
        params.put("apId", AP_ID);
        params.put("secretKey", SECRET_KEY);
        params.put("mobiles",mobile);
        params.put("content",content);
        params.put("sign", SIGN);
        params.put("addSerial", addSerial);
        params.put("mac", mac);
        String paramJson = JsonUtils.toJsonString(params);

        // 4. 将 JSON 参数进行 Base64 编码
        String encodeParam = Base64.encode(paramJson.getBytes(StandardCharsets.UTF_8));

        // 5. 构建请求 URL 并执行 POST 请求
        // 确保 URL 结尾拼接 norsubmit
        String url = SUBMIT_URL + "/" + "norsubmit";

        String curlCommand = String.format(
                "curl -X POST \"%s\" -H \"Content-Type: text/plain; charset=UTF-8\" -d '%s'",
                url, encodeParam
        );

        // 样例中 HttpClient.doPost 将 encode 后的字符串直接作为 body 发送，并指定 utf-8
        String responseText = HttpRequest.post(url)
                .body(encodeParam)
                .charset(StandardCharsets.UTF_8)
                .execute()
                .body();

        System.out.println("臻善接口返回详情: " + JSON.toJSONString(responseText));

        // 6. 解析结果判定
        Map<?, ?> responseObj = JsonUtils.parseObject(responseText, Map.class);
        // 根据样例通过 success 布尔值判断是否成功
        Boolean success = (Boolean) responseObj.get("success");
        String message = String.valueOf(responseObj.get("message"));

        return new SmsSendRespDTO()
                .setSuccess(Boolean.TRUE.equals(success))
                .setSerialNo(StrUtil.uuid())
                .setApiCode(Boolean.TRUE.equals(success) ? "200" : "500")
                .setApiMsg(message);

    }

    @Override
    public List<SmsReceiveRespDTO> parseSmsReceiveStatus(String text) throws Throwable {
        throw new UnsupportedOperationException("私有短信服务端，暂无需解析回调");
    }

    @Override
    public SmsTemplateRespDTO getSmsTemplate(String apiTemplateId) throws Throwable {
        return new SmsTemplateRespDTO().setId(apiTemplateId).setContent("")
                .setAuditStatus(SmsTemplateAuditStatusEnum.SUCCESS.getStatus()).setAuditReason("");
    }
}
