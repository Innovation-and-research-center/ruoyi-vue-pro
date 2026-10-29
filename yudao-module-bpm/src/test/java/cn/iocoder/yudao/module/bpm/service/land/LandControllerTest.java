package cn.iocoder.yudao.module.bpm.service.land;

import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.bpm.controller.admin.land.LandController;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.*;
import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.bpm.service.land.listener.BpmLandStatusListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LandControllerTest {
    private final LandService service = mock(LandService.class);
    private MockMvc mvc;
    private static final String BODY = "{\"id\":10,\"projectName\":\"测试项目\",\"location\":\"测试坐落\","
            + "\"deptOpinion\":\"科室意见\",\"applyDate\":1793145600000,\"startUserName\":\"忽略客户端身份\","
            + "\"fileList\":[{\"attachFileId\":21,\"fileName\":\"客户端文件名\",\"filePath\":\"客户端路径\"}]}";

    @BeforeEach
    void prepare() {
        LandController controller = new LandController();
        ReflectionTestUtils.setField(controller, "landService", service);
        JavaTimeModule time = new JavaTimeModule();
        time.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().registerModule(time)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new LoginUser().setId(7L), null));
    }

    @AfterEach
    void clearIdentity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveRouteAcceptsActualFrontendPayloadAndReturnsProcessAndTaskIds() throws Exception {
        when(service.save(eq(7L), any())).thenReturn(new LandSaveRespVO().setId(10L)
                .setProcessInstanceId("process-10").setTaskId("register-task"));
        mvc.perform(post("/bpm/land/save").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10)).andExpect(jsonPath("$.data.taskId").value("register-task"));
        ArgumentCaptor<LandSaveReqVO> data = ArgumentCaptor.forClass(LandSaveReqVO.class);
        verify(service).save(eq(7L), data.capture());
        assertNotNull(data.getValue().getApplyDate());
        assertEquals(21L, data.getValue().getFileList().get(0).getAttachFileId());
    }

    @Test
    void updateAndSubmitRoutesReachLandServiceInsteadOfBpmFallback() throws Exception {
        mvc.perform(put("/bpm/land/update").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        mvc.perform(post("/bpm/land/create-flow").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(service).update(eq(7L), any());
        verify(service).submit(eq(7L), any());
    }

    @Test
    void getRouteReturnsFrontendDetailContract() throws Exception {
        when(service.get(7L, 10L)).thenReturn(new LandDetailRespVO().setId(10L).setProjectName("测试项目"));
        mvc.perform(get("/bpm/land/get").param("id", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.projectName").value("测试项目"));
    }

    @Test
    void blankFieldsAndInvalidAttachmentsAreRejectedBeforePersistence() throws Exception {
        mvc.perform(post("/bpm/land/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\":\" \",\"location\":\"测试坐落\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/bpm/land/save").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\":\"项目\",\"location\":\"坐落\",\"fileList\":[{\"attachFileId\":-1}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void pageRouteBindsFiltersDatesAndPagination() throws Exception {
        when(service.getPage(eq(7L), any())).thenReturn(new PageResult<>(Collections.singletonList(
                new LandPageRespVO().setId(10L).setProjectName("测试项目").setStatus(1)), 21L));
        mvc.perform(get("/bpm/land/page").param("pageNo", "2").param("pageSize", "10")
                        .param("projectName", "测试").param("location", "测试坐落")
                        .param("startUserName", "登记员").param("status", "1")
                        .param("applyDate[0]", "2026-10-01 00:00:00")
                        .param("applyDate[1]", "2026-10-29 23:59:59"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(21))
                .andExpect(jsonPath("$.data.list[0].projectName").value("测试项目"));
        ArgumentCaptor<LandPageReqVO> query = ArgumentCaptor.forClass(LandPageReqVO.class);
        verify(service).getPage(eq(7L), query.capture());
        assertEquals(2, query.getValue().getPageNo());
        assertEquals(10, query.getValue().getPageSize());
        assertEquals("测试坐落", query.getValue().getLocation());
        assertEquals("登记员", query.getValue().getStartUserName());
        assertEquals(LocalDateTime.of(2026, 10, 29, 23, 59, 59), query.getValue().getApplyDate()[1]);
    }

    @Test
    void pageRouteRejectsInvalidPaginationAndDate() throws Exception {
        mvc.perform(get("/bpm/land/page").param("pageNo", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/bpm/land/page").param("pageSize", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/bpm/land/page").param("applyDate[0]", "invalid-date"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void managementUpdateAndDeleteUseDedicatedServiceMethods() throws Exception {
        mvc.perform(put("/bpm/land/management-update").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        mvc.perform(delete("/bpm/land/delete").param("id", "10").param("reason", "测试删除"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(service).managementUpdate(eq(7L), any());
        verify(service).delete(7L, 10L, "测试删除");
        verify(service, never()).update(any(), any());
    }

    @Test
    void statusListenerFiltersProcessKeyAndUsesEventProcessIdentity() {
        BpmLandStatusListener listener = new BpmLandStatusListener();
        ReflectionTestUtils.setField(listener, "landService", service);
        BpmProcessInstanceStatusEvent event = new BpmProcessInstanceStatusEvent(this);
        event.setBusinessKey("10"); event.setId("process-10"); event.setStatus(2);
        event.setProcessDefinitionKey("oa_leave"); listener.onApplicationEvent(event);
        verifyNoInteractions(service);
        event.setProcessDefinitionKey("lang_use"); listener.onApplicationEvent(event);
        verify(service).updateStatus(10L, "process-10", 2);
    }
}
