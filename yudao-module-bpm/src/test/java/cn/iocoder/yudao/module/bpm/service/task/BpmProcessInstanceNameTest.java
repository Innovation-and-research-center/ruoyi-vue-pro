package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmProcessInstanceNameTest {

    @Test
    void titleLookupIgnoresThreadDataPermissionAndRestoresIt() {
        BpmProcessInstanceServiceImpl service = new BpmProcessInstanceServiceImpl();
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setNickname("系统用户");
        when(adminUserApi.getUser(1L)).thenAnswer(invocation -> {
            assertNotNull(DataPermissionContextHolder.get());
            assertFalse(DataPermissionContextHolder.get().enable());
            return user;
        });

        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(definition.getName()).thenReturn("收文流程");
        BpmModelMetaInfoVO.TitleSetting titleSetting = new BpmModelMetaInfoVO.TitleSetting();
        titleSetting.setEnable(true);
        titleSetting.setTitle("{PROCESS_START_USER_ID}-收文");
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO();
        definitionInfo.setTitleSetting(titleSetting);

        String name = ReflectionTestUtils.invokeMethod(service, "generateProcessInstanceName",
                1L, definition, definitionInfo, new HashMap<>());

        assertEquals("系统用户-收文", name);
        assertNull(DataPermissionContextHolder.get());
    }

    @Test
    void missingStartUserFallsBackToDefinitionName() {
        BpmProcessInstanceServiceImpl service = new BpmProcessInstanceServiceImpl();
        AdminUserApi adminUserApi = mock(AdminUserApi.class);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(definition.getName()).thenReturn("收文流程");
        BpmModelMetaInfoVO.TitleSetting titleSetting = new BpmModelMetaInfoVO.TitleSetting();
        titleSetting.setEnable(true);
        titleSetting.setTitle("{PROCESS_START_USER_ID}-收文");
        BpmProcessDefinitionInfoDO definitionInfo = new BpmProcessDefinitionInfoDO();
        definitionInfo.setTitleSetting(titleSetting);

        String name = ReflectionTestUtils.invokeMethod(service, "generateProcessInstanceName",
                1L, definition, definitionInfo, new HashMap<>());

        assertEquals("收文流程", name);
        assertNull(DataPermissionContextHolder.get());
    }
}
