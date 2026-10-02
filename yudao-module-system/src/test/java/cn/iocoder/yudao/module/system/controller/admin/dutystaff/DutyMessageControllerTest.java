package cn.iocoder.yudao.module.system.controller.admin.dutystaff;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictDataDO;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsTemplateDO;
import cn.iocoder.yudao.module.system.service.dict.DictDataService;
import cn.iocoder.yudao.module.system.service.sms.SmsTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DutyMessageControllerTest {

    private DutyMessageController controller;
    private SmsTemplateService smsTemplateService;
    private DictDataService dictDataService;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        controller = new DutyMessageController();
        smsTemplateService = mock(SmsTemplateService.class);
        dictDataService = mock(DictDataService.class);
        ReflectionTestUtils.setField(controller, "smsTemplateService", smsTemplateService);
        ReflectionTestUtils.setField(controller, "dictDataService", dictDataService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void updateTemplateOnlyChangesContent() {
        SmsTemplateDO template = new SmsTemplateDO();
        template.setId(10L);
        template.setCode("duty_1");
        template.setContent("原文");
        template.setName("值班短信");
        template.setApiTemplateId("api-1");
        template.setChannelId(20L);
        when(smsTemplateService.getSmsTemplateByCodeFromCache("duty_1")).thenReturn(template);
        when(dictDataService.getDictDataList(any(), eq("duty_params"))).thenReturn(Collections.emptyList());
        DutyMessageController.UpdateTemplateContentReqVO request = new DutyMessageController.UpdateTemplateContentReqVO();
        request.setCode("duty_1");
        request.setContent("值班日期：{dutydate}");

        controller.updateTemplateContent(request);

        ArgumentCaptor<SmsTemplateSaveReqVO> captor = ArgumentCaptor.forClass(SmsTemplateSaveReqVO.class);
        verify(smsTemplateService).updateSmsTemplate(captor.capture());
        assertEquals("值班日期：{dutydate}", captor.getValue().getContent());
        assertEquals("api-1", captor.getValue().getApiTemplateId());
        assertEquals(20L, captor.getValue().getChannelId());
    }

    @Test
    void listUsesConfiguredTemplateNames() {
        SmsTemplateDO template = new SmsTemplateDO();
        template.setCode("duty_ding_1");
        template.setName("领导值班提醒");
        template.setChannelCode("DG_WORK");
        when(smsTemplateService.getSmsTemplateByCodeFromCache("duty_ding_1")).thenReturn(template);

        DutyMessageController.DutyTemplateRespVO result = controller.getTemplates().getData().stream()
                .filter(item -> "duty_ding_1".equals(item.getCode())).findFirst().orElseThrow();

        assertEquals("领导值班提醒", result.getName());
        assertEquals("DG_WORK", result.getChannelCode());
    }

    @Test
    void rejectOtherSmsTemplate() {
        DutyMessageController.UpdateTemplateContentReqVO request = new DutyMessageController.UpdateTemplateContentReqVO();
        request.setCode("login_code");
        request.setContent("新内容");

        assertThrows(ServiceException.class, () -> controller.updateTemplateContent(request));
        verifyNoInteractions(smsTemplateService);
    }

    @Test
    void rejectUnknownTemplateVariable() {
        SmsTemplateDO template = new SmsTemplateDO();
        template.setCode("duty_1");
        when(smsTemplateService.getSmsTemplateByCodeFromCache("duty_1")).thenReturn(template);
        when(dictDataService.getDictDataList(any(), eq("duty_params"))).thenReturn(Collections.emptyList());
        DutyMessageController.UpdateTemplateContentReqVO request = new DutyMessageController.UpdateTemplateContentReqVO();
        request.setCode("duty_1");
        request.setContent("您好，{unknown}");

        assertThrows(ServiceException.class, () -> controller.updateTemplateContent(request));
        verify(smsTemplateService, never()).updateSmsTemplate(any());
    }

    @Test
    void updateDutyParamOnlyChangesValue() {
        DictDataDO dictData = new DictDataDO();
        dictData.setId(4L);
        dictData.setDictType("duty_params");
        dictData.setLabel("address");
        dictData.setValue("旧地址");
        dictData.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(dictDataService.getDictData(4L)).thenReturn(dictData);
        DutyMessageController.UpdateParamValueReqVO request = new DutyMessageController.UpdateParamValueReqVO();
        request.setId(4L);
        request.setValue("新地址");

        controller.updateParamValue(request);

        ArgumentCaptor<DictDataSaveReqVO> captor = ArgumentCaptor.forClass(DictDataSaveReqVO.class);
        verify(dictDataService).updateDictData(captor.capture());
        assertEquals("新地址", captor.getValue().getValue());
        assertEquals("address", captor.getValue().getLabel());
        assertEquals("duty_params", captor.getValue().getDictType());
    }

    @Test
    void createDutyParamUsesFixedDictionaryType() {
        when(dictDataService.getDictDataListByDictType("duty_params")).thenReturn(Collections.emptyList());
        when(dictDataService.createDictData(any())).thenReturn(8L);
        DutyMessageController.CreateParamReqVO request = new DutyMessageController.CreateParamReqVO();
        request.setLabel("address");
        request.setValue("值班室");

        assertEquals(8L, controller.createParam(request).getData());

        ArgumentCaptor<DictDataSaveReqVO> captor = ArgumentCaptor.forClass(DictDataSaveReqVO.class);
        verify(dictDataService).createDictData(captor.capture());
        assertEquals("duty_params", captor.getValue().getDictType());
        assertEquals("address", captor.getValue().getLabel());
        assertEquals("值班室", captor.getValue().getValue());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
    }

    @Test
    void rejectDuplicateDutyParamName() {
        DictDataDO existing = new DictDataDO();
        existing.setLabel("address");
        when(dictDataService.getDictDataListByDictType("duty_params"))
                .thenReturn(Collections.singletonList(existing));
        DutyMessageController.CreateParamReqVO request = new DutyMessageController.CreateParamReqVO();
        request.setLabel("address");
        request.setValue("新值");

        assertThrows(ServiceException.class, () -> controller.createParam(request));
        verify(dictDataService, never()).createDictData(any());
    }

    @Test
    void rejectReservedDutyParamName() {
        DutyMessageController.CreateParamReqVO request = new DutyMessageController.CreateParamReqVO();
        request.setLabel("dutydate");
        request.setValue("手工日期");

        assertThrows(ServiceException.class, () -> controller.createParam(request));
        verifyNoInteractions(dictDataService);
    }

    @Test
    void rejectOtherDictionary() {
        DictDataDO dictData = new DictDataDO();
        dictData.setId(3L);
        dictData.setDictType("system_user_sex");
        when(dictDataService.getDictData(3L)).thenReturn(dictData);
        DutyMessageController.UpdateParamValueReqVO request = new DutyMessageController.UpdateParamValueReqVO();
        request.setId(3L);
        request.setValue("新内容");

        assertThrows(ServiceException.class, () -> controller.updateParamValue(request));
        verify(dictDataService, never()).updateDictData(any(DictDataSaveReqVO.class));
    }

    @Test
    void rejectOtherTenant() {
        TenantContextHolder.setTenantId(2L);

        assertThrows(ServiceException.class, () -> controller.getTemplates());
        verifyNoInteractions(smsTemplateService);
    }
}
