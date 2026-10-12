package cn.iocoder.yudao.module.system.service.sms;

import cn.hutool.core.map.MapUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.iocoder.yudao.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.iocoder.yudao.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum;
import cn.iocoder.yudao.module.system.framework.sms.config.SmsCodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import(SmsCodeServiceImpl.class)
public class SmsCodeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SmsCodeServiceImpl smsCodeService;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @MockBean
    private SmsCodeProperties smsCodeProperties;
    @MockBean
    private SmsSendService smsSendService;

    @BeforeEach
    public void setUp() {
        when(smsCodeProperties.getAdminLoginTemplateCode()).thenReturn("login_code");
        when(smsCodeProperties.getExpireTimes()).thenReturn(Duration.ofMinutes(5));
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMinutes(1));
        when(smsCodeProperties.getSendMaximumQuantityPerDay()).thenReturn(10);
        when(smsCodeProperties.getBeginCode()).thenReturn(9999);
        when(smsCodeProperties.getEndCode()).thenReturn(9999);
    }

    @Test
    public void sendSmsCode_success() {
        // 准备参数
        SmsCodeSendReqDTO reqDTO = randomPojo(SmsCodeSendReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setReceiveMobile(null);
            o.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        });

        // 调用
        smsCodeService.sendSmsCode(reqDTO);
        // 断言 code 验证码
        SmsCodeDO smsCodeDO = smsCodeMapper.selectOne(null);
        assertPojoEquals(reqDTO, smsCodeDO);
        assertEquals("9999", smsCodeDO.getCode());
        assertEquals(1, smsCodeDO.getTodayIndex());
        assertFalse(smsCodeDO.getUsed());
        // 断言调用
        verify(smsSendService).sendSingleSms(eq(reqDTO.getMobile()), isNull(), isNull(),
                eq("user-sms-login"), eq(MapUtil.of("code", "9999")));
    }

    @Test
    public void sendSmsCode_alternateMobileBoundToLoginMobile() {
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO().setMobile("13800138000")
                .setReceiveMobile("13900139000").setScene(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene())
                .setCreateIp("127.0.0.1");
        smsCodeService.sendSmsCode(reqDTO);
        verify(smsSendService).sendSingleSms(eq("13900139000"), isNull(), isNull(),
                eq("login_code"), eq(MapUtil.of("code", "9999")));
        assertEquals("13800138000", smsCodeMapper.selectOne(null).getMobile());
        assertServiceException(() -> smsCodeService.useSmsCode(new SmsCodeUseReqDTO()
                .setMobile("13900139000").setCode("9999").setScene(reqDTO.getScene()).setUsedIp("127.0.0.1")),
                SMS_CODE_NOT_FOUND);
        SmsCodeUseReqDTO useReqDTO = new SmsCodeUseReqDTO().setMobile(reqDTO.getMobile())
                .setCode("9999").setScene(reqDTO.getScene()).setUsedIp("127.0.0.1");
        smsCodeService.useSmsCode(useReqDTO);
        assertServiceException(() -> smsCodeService.useSmsCode(useReqDTO), SMS_CODE_USED);
    }

    @Test
    public void loginCodeUsesSixDigitsAndConfiguredTemplate() {
        when(smsCodeProperties.getBeginCode()).thenReturn(100000);
        when(smsCodeProperties.getEndCode()).thenReturn(999999);
        SmsCodeSendReqDTO reqDTO = new SmsCodeSendReqDTO().setMobile("13800138000")
                .setScene(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene()).setCreateIp("127.0.0.1");
        smsCodeService.sendSmsCode(reqDTO);
        String code = smsCodeMapper.selectOne(null).getCode();
        assertTrue(code.matches("[1-9][0-9]{5}"));
        verify(smsSendService).sendSingleSms(eq(reqDTO.getMobile()), isNull(), isNull(),
                eq("login_code"), eq(MapUtil.of("code", code)));
    }

    @Test
    public void sharedReceiverDoesNotShareLoginCode() {
        SmsCodeSendReqDTO alice = new SmsCodeSendReqDTO().setMobile("13800138000")
                .setReceiveMobile("13800138000").setScene(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene())
                .setCreateIp("127.0.0.1");
        smsCodeService.sendSmsCode(alice);
        when(smsCodeProperties.getBeginCode()).thenReturn(8888);
        when(smsCodeProperties.getEndCode()).thenReturn(8888);
        SmsCodeSendReqDTO bob = new SmsCodeSendReqDTO().setMobile("13900139000")
                .setReceiveMobile(alice.getMobile()).setScene(alice.getScene()).setCreateIp("127.0.0.1");
        smsCodeService.sendSmsCode(bob);
        assertServiceException(() -> smsCodeService.useSmsCode(new SmsCodeUseReqDTO()
                .setMobile(alice.getMobile()).setScene(alice.getScene()).setCode("8888").setUsedIp("127.0.0.1")),
                SMS_CODE_NOT_FOUND);
        smsCodeService.useSmsCode(new SmsCodeUseReqDTO().setMobile(bob.getMobile())
                .setScene(bob.getScene()).setCode("8888").setUsedIp("127.0.0.1"));
    }

    @Test
    public void sendSmsCode_tooFast() {
        // mock 数据
        SmsCodeDO smsCodeDO = randomPojo(SmsCodeDO.class,
                o -> o.setMobile("15601691300").setTodayIndex(1));
        smsCodeMapper.insert(smsCodeDO);
        // 准备参数
        SmsCodeSendReqDTO reqDTO = randomPojo(SmsCodeSendReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        });

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.sendSmsCode(reqDTO),
                SMS_CODE_SEND_TOO_FAST);
    }

    @Test
    public void sendSmsCode_exceedDay() {
        // mock 数据
        SmsCodeDO smsCodeDO = randomPojo(SmsCodeDO.class,
                o -> o.setMobile("15601691300").setTodayIndex(10).setCreateTime(LocalDateTime.now()));
        smsCodeMapper.insert(smsCodeDO);
        // 准备参数
        SmsCodeSendReqDTO reqDTO = randomPojo(SmsCodeSendReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(SmsSceneEnum.MEMBER_LOGIN.getScene());
        });
        when(smsCodeProperties.getSendFrequency()).thenReturn(Duration.ofMillis(0));

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.sendSmsCode(reqDTO),
                SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
    }

    @Test
    public void testUseSmsCode_success() {
        // 准备参数
        SmsCodeUseReqDTO reqDTO = randomPojo(SmsCodeUseReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> {
            o.setMobile(reqDTO.getMobile()).setScene(reqDTO.getScene())
                    .setCode(reqDTO.getCode()).setUsed(false);
        }));

        // 调用
        smsCodeService.useSmsCode(reqDTO);
        // 断言
        SmsCodeDO smsCodeDO = smsCodeMapper.selectOne(null);
        assertTrue(smsCodeDO.getUsed());
        assertNotNull(smsCodeDO.getUsedTime());
        assertEquals(reqDTO.getUsedIp(), smsCodeDO.getUsedIp());
    }

    @Test
    public void validateSmsCode_success() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> o.setMobile(reqDTO.getMobile())
                .setScene(reqDTO.getScene()).setCode(reqDTO.getCode()).setUsed(false)));

        // 调用
        smsCodeService.validateSmsCode(reqDTO);
    }

    @Test
    public void validateSmsCode_notFound() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(reqDTO),
                SMS_CODE_NOT_FOUND);
    }

    @Test
    public void validateSmsCode_expired() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> o.setMobile(reqDTO.getMobile())
                .setScene(reqDTO.getScene()).setCode(reqDTO.getCode()).setUsed(false)
                .setCreateTime(LocalDateTime.now().minusMinutes(6))));

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(reqDTO),
                SMS_CODE_EXPIRED);
    }

    @Test
    public void validateSmsCode_used() {
        // 准备参数
        SmsCodeValidateReqDTO reqDTO = randomPojo(SmsCodeValidateReqDTO.class, o -> {
            o.setMobile("15601691300");
            o.setScene(randomEle(SmsSceneEnum.values()).getScene());
        });
        smsCodeMapper.insert(randomPojo(SmsCodeDO.class, o -> o.setMobile(reqDTO.getMobile())
                .setScene(reqDTO.getScene()).setCode(reqDTO.getCode()).setUsed(true)
                .setCreateTime(LocalDateTime.now())));

        // 调用，并断言异常
        assertServiceException(() -> smsCodeService.validateSmsCode(reqDTO),
                SMS_CODE_USED);
    }

}
