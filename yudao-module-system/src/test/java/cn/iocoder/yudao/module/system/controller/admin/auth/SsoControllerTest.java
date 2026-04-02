package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.util.AESUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SsoControllerTest {

    @InjectMocks
    private SsoController ssoController;

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletResponse response;

    private final String FRONTEND_URL = "http://localhost:80";

    @BeforeEach
    void setUp() {
        // 注入 @Value 属性
        ReflectionTestUtils.setField(ssoController, "frontendUrl", FRONTEND_URL);
    }

    /**
     * 辅助方法：生成模拟的 JWT Token
     */
    private String generateMockToken(JSONObject payloadJson) {
        String base64Payload = Base64.encode(payloadJson.toString());
        return "header." + base64Payload + ".signature";
    }

    /**
     * 辅助方法：生成基础的 Payload JSON，基于提供的 user.json
     */
    private JSONObject getBasePayload() {
        String jsonStr = "{\n" +
                "  \"tenant_id\" : \"330782\",\n" +
                "  \"user_tel\" : \"18200006666\",\n" +
                "  \"user_name\" : \"ymgd\",\n" +
                "  \"encrypt_str\" : \"dummy_encrypted_string\",\n" +
                "  \"user_nickname\" : \"%E4%B8%9A%E5%8A%A1%E7%AE%A1%E7%90%86%E5%91%98\",\n" + // 业务管理员
                "  \"ding_id\" : \"ding123\"\n" +
                "}";
        return JSONUtil.parseObj(jsonStr);
    }

    @Test
    void testSsoLogin_TokenIsEmpty() throws IOException {
        Map<String, String> params = new HashMap<>();
        ssoController.ssoLogin(params, response);
        verify(response).sendRedirect(FRONTEND_URL + "/sso-error?error=token_is_empty");
    }

    @Test
    void testSsoLogin_TokenFormatError() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("token", "invalid_token_without_dot");

        ssoController.ssoLogin(params, response);
        verify(response).sendRedirect(FRONTEND_URL + "/sso-error?error=token_format_error");
    }

    @Test
    void testSsoLogin_TokenDecodeError() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("token", "header.!!!invalid_base64!!!.sig");

        ssoController.ssoLogin(params, response);
        verify(response).sendRedirect(FRONTEND_URL + "/sso-error?error=token_decode_error");
    }

    @Test
    void testSsoLogin_PhoneNumberNotFound() throws IOException {
        JSONObject payload = getBasePayload();
        payload.set("encrypt_str", ""); // 置空加密字符串

        Map<String, String> params = new HashMap<>();
        params.put("token", generateMockToken(payload));

        ssoController.ssoLogin(params, response);
        verify(response).sendRedirect(FRONTEND_URL + "/sso-error?error=phone_number_not_found");
    }

    @Test
    void testSsoLogin_NewUserCreateSuccess() throws Exception {
        JSONObject payload = getBasePayload();
        Map<String, String> params = new HashMap<>();
        params.put("token", generateMockToken(payload));

        try (MockedStatic<AESUtils> aesUtilsMock = mockStatic(AESUtils.class)) {
            // Mock 解密手机号
            aesUtilsMock.when(() -> AESUtils.decrypt(anyString(), anyString())).thenReturn("18200006666_1234");

            // Mock 查询手机号不存在
            when(adminUserService.getUserByMobile("18200006666")).thenReturn(null);
            // Mock 查询用户名不冲突
            when(adminUserService.getUserByUsername("ymgd")).thenReturn(null);

            // Mock 创建用户并返回ID
            when(adminUserService.createSsoUser(eq("ymgd"), eq("18200006666"), eq("业务管理员"), eq("ding123"))).thenReturn(100L);

            AdminUserDO mockCreatedUser = new AdminUserDO();
            mockCreatedUser.setId(100L);
            mockCreatedUser.setUsername("ymgd");
            mockCreatedUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
            when(adminUserService.getUser(100L)).thenReturn(mockCreatedUser);

            // Mock Redis
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            ssoController.ssoLogin(params, response);

            // 验证 Redis 设值和重定向
            verify(valueOperations).set(startsWith("sso:ticket:"), eq("100"), eq(60L), eq(TimeUnit.SECONDS));

            ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(redirectCaptor.capture());
            assertTrue(redirectCaptor.getValue().startsWith(FRONTEND_URL + "/sso-redirect?ticket="));
        }
    }

    @Test
    void testSsoLogin_NewUser_UsernameConflict() throws Exception {
        JSONObject payload = getBasePayload();
        Map<String, String> params = new HashMap<>();
        params.put("token", generateMockToken(payload));

        try (MockedStatic<AESUtils> aesUtilsMock = mockStatic(AESUtils.class)) {
            aesUtilsMock.when(() -> AESUtils.decrypt(anyString(), anyString())).thenReturn("18200006666_1234");

            when(adminUserService.getUserByMobile("18200006666")).thenReturn(null);

            // Mock 查询用户名存在冲突
            when(adminUserService.getUserByUsername("ymgd")).thenReturn(new AdminUserDO());

            // 核心验证：发生冲突时，是否回退使用手机号作为 username
            when(adminUserService.createSsoUser(eq("18200006666"), eq("18200006666"), eq("业务管理员"), eq("ding123"))).thenReturn(101L);
            when(adminUserService.getUser(101L)).thenReturn(new AdminUserDO().setId(101L).setStatus(CommonStatusEnum.ENABLE.getStatus()));
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            ssoController.ssoLogin(params, response);

            // 验证确实使用了手机号创建用户
            verify(adminUserService).createSsoUser(eq("18200006666"), eq("18200006666"), anyString(), anyString());
        }
    }

    @Test
    void testSsoLogin_ExistingUser_UpdateDingId() throws Exception {
        JSONObject payload = getBasePayload();
        Map<String, String> params = new HashMap<>();
        params.put("token", generateMockToken(payload));

        try (MockedStatic<AESUtils> aesUtilsMock = mockStatic(AESUtils.class)) {
            aesUtilsMock.when(() -> AESUtils.decrypt(anyString(), anyString())).thenReturn("18200006666_1234");

            AdminUserDO existingUser = new AdminUserDO();
            existingUser.setId(200L);
            existingUser.setStatus(CommonStatusEnum.ENABLE.getStatus());
            existingUser.setDingId("oldDingId"); // 数据库中是旧的 dingId

            // 查到已存在用户
            when(adminUserService.getUserByMobile("18200006666")).thenReturn(existingUser);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

            ssoController.ssoLogin(params, response);

            // 验证是否调用了更新 DingId 的方法
            verify(adminUserService).updateUserDingId(200L, "ding123");

            ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
            verify(response).sendRedirect(redirectCaptor.capture());
            assertTrue(redirectCaptor.getValue().startsWith(FRONTEND_URL + "/sso-redirect?ticket="));
        }
    }

    @Test
    void testSsoLogin_ExistingUser_Disabled() throws Exception {
        JSONObject payload = getBasePayload();
        Map<String, String> params = new HashMap<>();
        params.put("token", generateMockToken(payload));

        try (MockedStatic<AESUtils> aesUtilsMock = mockStatic(AESUtils.class)) {
            aesUtilsMock.when(() -> AESUtils.decrypt(anyString(), anyString())).thenReturn("18200006666_1234");

            AdminUserDO existingUser = new AdminUserDO();
            existingUser.setId(300L);
            existingUser.setStatus(CommonStatusEnum.DISABLE.getStatus()); // 用户被禁用

            when(adminUserService.getUserByMobile("18200006666")).thenReturn(existingUser);

            ssoController.ssoLogin(params, response);

            // 验证抛出 SecurityException 并重定向到错误页
            verify(response).sendRedirect(FRONTEND_URL + "/sso-error?error=user_disabled");
        }
    }

    @Test
    void testSsoLogin_CreateUserFailed() throws Exception {
        JSONObject payload = getBasePayload();
        Map<String, String> params = new HashMap<>();
        params.put("token", generateMockToken(payload));

        try (MockedStatic<AESUtils> aesUtilsMock = mockStatic(AESUtils.class)) {
            aesUtilsMock.when(() -> AESUtils.decrypt(anyString(), anyString())).thenReturn("18200006666_1234");
            when(adminUserService.getUserByMobile("18200006666")).thenReturn(null);

            // 模拟创建用户时抛出异常
            when(adminUserService.createSsoUser(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("DB Error"));

            ssoController.ssoLogin(params, response);

            verify(response).sendRedirect(FRONTEND_URL + "/sso-error?error=user_create_failed");
        }
    }
}