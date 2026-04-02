package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.logger.LoginLogService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    @Resource
    private PermissionService permissionService;

    @Value("${oa.sso.frontend-url:http://localhost:8081}")
    private String frontendUrl;;

    private static final String TICKET_REDIS_KEY_PREFIX = "sso:ticket:";
    private static final String DECRYPT_KEY = "gisq39561c9fe068";

    @GetMapping("/sso-login")
    @PermitAll
    @Operation(summary = "单点登录换区票据")
    public void ssoLogin(@RequestParam Map<String, String> params, HttpServletResponse response) throws IOException {
        String token = params.get("token");
        if (StrUtil.isBlank(token)) {
            token = params.get("Token"); // 兼容大小写
        }
        log.info("SSO接收到回调Token: {}", token);

        try {
            // 1. 基础校验 Token
            if (StrUtil.isBlank(token)) {
                throw new IllegalArgumentException("token_is_empty");
            }

            String[] tokenParts = token.split("\\.");
            if (tokenParts.length < 2) {
                throw new IllegalArgumentException("token_format_error");
            }
//            String jsonStr;
//            try {
//                String payload = tokenParts[1];
//                jsonStr = Base64.decodeStr(payload, StandardCharsets.UTF_8);
//            } catch (Exception e) {
//                throw new IllegalArgumentException("token_decode_error");
//            }
//
//            JSONObject userInfoJson = JSONUtil.parseObj(jsonStr);
            JSONObject userInfoJson;
            try {
                String payload = tokenParts[1];
                String jsonStr = Base64.decodeStr(payload, StandardCharsets.UTF_8);
                // 把 JSON 解析也包进来，任何解析失败都算 token_decode_error
                userInfoJson = JSONUtil.parseObj(jsonStr);
            } catch (Exception e) {
                throw new IllegalArgumentException("token_decode_error");
            }

            // ================== 第一步：解析手机号 (最核心，因为要作为后续的保底) ==================
            String encryptStr = userInfoJson.getStr("encrypt_str");
            String userPhone = null;
            if (StrUtil.isNotBlank(encryptStr)) {
                try {
                    String decryptStr = AESUtils.decrypt(encryptStr, DECRYPT_KEY);
                    String[] telAndIdNum = decryptStr.split("_");
                    userPhone = telAndIdNum.length > 0 ? telAndIdNum[0] : null;
                } catch (Exception e) {
                    log.error("AES解密手机号失败", e);
                }
            }
            // 【拦截点】：如果解析不出手机号，直接报错中断，返回前端错误页
            if (StrUtil.isBlank(userPhone)) {
                log.error("SSO解析手机号失败，token内容中缺少有效的手机号信息");
                throw new IllegalArgumentException("phone_number_not_found");
            }

            // ================== 第二步：处理用户名 (为空则用手机号保底) ==================
            String username = userInfoJson.getStr("user_name");
            if (StrUtil.isBlank(username)) {
                username = userPhone;
                log.info("SSO未获取到user_name，使用手机号 {} 作为默认用户名", username);
            }

            // ================== 第三步：处理昵称 (为空则用 username 保底) ==================
            String encodedNickname = userInfoJson.getStr("user_nickname");
            String nickname = username; // 默认保底
            if (StrUtil.isNotBlank(encodedNickname)) {
                try {
                    nickname = java.net.URLDecoder.decode(encodedNickname, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    log.error("SSO昵称解码失败: {}", encodedNickname);
                }
            }

            String dingId = userInfoJson.getStr("ding_id");
            AdminUserDO finalUser = adminUserService.getUserByMobile(userPhone);
            // ================== 第四步：核心匹配逻辑：以手机号为准 ==================
            if (finalUser == null) {
                // 手机号没查到 -> 新增用户
                log.info("SSO手机号 {} 未匹配到用户，准备新增。保底账号: {}", userPhone, username);

                // 防重校验：如果 Token 里的 username 已经被别的手机号占用了，新增时强制用手机号作为账号名
                if (adminUserService.getUserByUsername(username) != null) {
                    username = userPhone;
                }

                try {
                    Long newUserId = adminUserService.createSsoUser(username, userPhone, nickname, dingId);
                    finalUser = adminUserService.getUser(newUserId);
                } catch (Exception e) {
                    log.error("SSO自动创建用户失败", e);
                    throw new RuntimeException("user_create_failed");
                }
            } else {
                // 手机号能查到 -> 无论用户名是否一致，都直接使用该用户，不新增
                log.info("SSO手机号 {} 匹配成功，登录用户: {}", userPhone, finalUser.getUsername());

                if (CommonStatusEnum.DISABLE.getStatus().equals(finalUser.getStatus())) {
                    throw new SecurityException("user_disabled");
                }
                // 按需同步 ding_id 等信息
                if (StrUtil.isNotBlank(dingId) && !Objects.equals(dingId, finalUser.getDingId())) {
                    adminUserService.updateUserDingId(finalUser.getId(), dingId);
                }
            }

            // ================== 第六步：生成 Ticket ==================
            String ticket = IdUtil.fastSimpleUUID();
            String redisKey = TICKET_REDIS_KEY_PREFIX + ticket;
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(finalUser.getId()), 60, TimeUnit.SECONDS);

            // ================== 第七步：重定向回前端 ==================
            String redirectUrl = StrUtil.format("{}/sso-redirect?ticket={}", frontendUrl, ticket);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // ================== 异常统一处理并重定向到前端错误页 ==================
            log.error("SSO登录异常", e);
            String errorCode = "sso_exception"; // 默认错误码

            // 提取具体的错误标识
            if (e instanceof IllegalArgumentException || e instanceof SecurityException || "user_create_failed".equals(e.getMessage())) {
                errorCode = e.getMessage();
            }

            // 对错误信息进行编码，防止 URL 格式非法
            String encodedError = URLEncoder.encode(errorCode, StandardCharsets.UTF_8.name());
            response.sendRedirect(frontendUrl + "/sso-error?error=" + encodedError);
        }
    }

}
