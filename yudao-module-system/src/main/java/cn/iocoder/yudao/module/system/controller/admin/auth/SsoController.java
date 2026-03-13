package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.logger.LoginLogService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.util.AESUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Tag(name = "单点登录")
@RestController
@RequestMapping("/system/sso")
@Validated
@Slf4j
public class SsoController {

    @Resource
    private AdminUserService adminUserService;

    @Resource
    private OAuth2TokenService oauth2TokenService;

    @Resource
    private LoginLogService loginLogService;

    @Resource
    private AdminUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${oa.sso.frontend-url:http://localhost:8081}")
    private String frontendUrl;;

    private static final String TICKET_REDIS_KEY_PREFIX = "sso:ticket:";

    @GetMapping("/sso-login")
    @PermitAll
    @Operation(summary = "单点登录换区票据")
    public  void ssoLogin(@RequestParam Map<String, String> params, HttpServletResponse response)throws IOException {
        String token = null;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if ("token".equalsIgnoreCase(entry.getKey())) {
                token = entry.getValue();
                break;
            }
        }
        log.info("SSO接收到回调Token: {}", token);

        if (StrUtil.isBlank(token)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Token不能为空");
            return;
        }

        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length < 2) {
                throw new IllegalArgumentException("Token格式错误");
            }
            String payload = tokenParts[1];
            String jsonStr = Base64.decodeStr(payload, StandardCharsets.UTF_8);
            JSONObject userInfoJson = JSONUtil.parseObj(jsonStr);
            String encryptStr = userInfoJson.getStr("encrypt_str");
            if (StrUtil.isBlank(encryptStr)) {
                throw new IllegalArgumentException("用户信息缺失encrypt_str");
            }
            String DECRYPT_KEY = "gisq39561c9fe068";
            // 使用 Hutool 的 AES 工具解密 (假设是标准AES，密钥即为 Key)
//            AES aes = SecureUtil.aes(DECRYPT_KEY.getBytes(StandardCharsets.UTF_8));
            String decryptStr = AESUtils.decrypt(encryptStr,DECRYPT_KEY); // 解密结果格式: 手机号_身份证号

            String[] telAndIdNum = decryptStr.split("_");
            String userPhone = telAndIdNum.length > 0 ? telAndIdNum[0] : "";

            if (StrUtil.isBlank(userPhone)) {
                throw new SecurityException("解密后手机号为空");
            }

            // 5. 根据手机号查找用户 (ruoyi-vue-pro 现有逻辑)
            AdminUserDO user = adminUserService.getUserByMobile(userPhone);
            if (user == null) {
                // 可选：如果用户不存在，是否自动创建？这里暂且报错
                log.error("SSO登录失败，未找到手机号为 {} 的用户", userPhone);
                response.sendRedirect(frontendUrl + "/sso-error?error=user_not_found");
                return;
            }

            if (CommonStatusEnum.DISABLE.getStatus().equals(user.getStatus())) {
                response.sendRedirect(frontendUrl + "/sso-error?error=user_disabled");
                return;
            }

//            createLoginLog(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SSO, LoginResultEnum.SUCCESS);
//            // 创建访问令牌
//            OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToken(user.getId(), getUserType().getValue(),
//                    OAuth2ClientConstants.CLIENT_ID_DEFAULT, null);
//            // 构建返回结果
//            AuthConvert.INSTANCE.convert(accessTokenDO);
            String ticket = IdUtil.fastSimpleUUID();
            String redisKey = TICKET_REDIS_KEY_PREFIX + ticket;
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(user.getId()), 60, TimeUnit.SECONDS);

            // 8. 重定向回前端首页，带上 Token
            // 最终跳转地址: http://localhost:80/sso-redirect?token=xxxx
            String redirectUrl = StrUtil.format("{}/sso-redirect?ticket={}", frontendUrl, ticket);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("SSO登录异常", e);
            response.sendRedirect(frontendUrl + "/sso-error?error=sso_exception");
        }

    }



}
