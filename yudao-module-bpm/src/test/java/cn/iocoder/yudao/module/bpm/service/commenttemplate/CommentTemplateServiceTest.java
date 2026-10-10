package cn.iocoder.yudao.module.bpm.service.commenttemplate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.commenttemplate.vo.CommentTemplateSaveVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commenttemplate.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.commenttemplate.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentTemplateServiceTest {
    @InjectMocks private CommentTemplateService service;
    @Mock private PersonalCommentTemplateMapper personalMapper;
    @Mock private BizCommentTemplateMapper bizMapper;
    @Mock private CommentTemplateBindingMapper bindingMapper;
    @Mock private ReceiveDocMapper receiveDocMapper;
    @Mock private BpmTaskService taskService;
    @Mock private RepositoryService repositoryService;
    @Mock private Task task;
    @Mock private ProcessDefinition definition;

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        TenantContextHolder.setTenantId(1L);
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @Test void personalReceiveTemplatesWorkWithoutBusinessBinding() {
        authorizedBinding();
        when(bindingMapper.selectOne(any())).thenReturn(null);
        when(definition.getKey()).thenReturn(cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE);
        when(personalMapper.selectList(any())).thenReturn(Collections.emptyList());
        CommentTemplateService.Options result = service.options(7L, "task", "Default");
        assertEquals("receivedoc", result.getIdeaType());
        assertTrue(result.getBusiness().isEmpty());
        verify(personalMapper).selectList(any());
        verifyNoInteractions(bizMapper);
    }

    @Test void personalSendTemplatesWorkWithoutBusinessBinding() {
        authorizedBinding();
        when(bindingMapper.selectOne(any())).thenReturn(null);
        when(definition.getKey()).thenReturn(cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.SEND);
        when(personalMapper.selectList(any())).thenReturn(Collections.emptyList());
        assertEquals("senddoc", service.options(7L, "task", "Default").getIdeaType());
        verifyNoInteractions(bizMapper);
    }

    @Test void returnsLegacyDefaultsWhenPersonalTemplatesAreEmpty() {
        authorizedBinding();
        when(bindingMapper.selectOne(any())).thenReturn(null);
        when(personalMapper.selectList(any())).thenReturn(Collections.emptyList());
        CommentTemplateService.Options result = service.options(7L, "task", "Default");
        assertEquals(Arrays.asList("已阅。", "拟同意。", "同意。", "请XX办理。", "请XX处阅。",
                "拟同意,请厅长签发。", "请陈厅长阅示。", "请全处阅。", "请XX处会同XX处办理。",
                "请厅领导阅,请厅机关各处（室、局）和直属事业单位阅。"),
                result.getPersonal().stream().map(PersonalCommentTemplateDO::getIdeaMessage)
                        .collect(java.util.stream.Collectors.toList()));
        for (int i = 0; i < result.getPersonal().size(); i++) {
            PersonalCommentTemplateDO row = result.getPersonal().get(i);
            assertNull(row.getOaIdeaId());
            assertEquals(result.getIdeaType(), row.getIdeaType());
            assertEquals(Integer.valueOf(i), row.getIdeaOrder());
        }
        verify(personalMapper, never()).insert(any(PersonalCommentTemplateDO.class));
    }

    @Test void keepsPersonalTemplatesWithoutAppendingDefaults() {
        authorizedBinding();
        when(bindingMapper.selectOne(any())).thenReturn(null);
        PersonalCommentTemplateDO row = new PersonalCommentTemplateDO();
        row.setOaIdeaId(123L);
        row.setIdeaMessage("我的意见");
        when(personalMapper.selectList(any())).thenReturn(Collections.singletonList(row));
        assertEquals(Collections.singletonList(row), service.options(7L, "task", "Default").getPersonal());
    }

    @Test void rendersLegacyAliasesAndUsesBusinessOfAuthorizedTask() {
        CommentTemplateBindingDO binding = authorizedBinding();
        BizCommentTemplateDO template = template();
        when(bizMapper.selectList(any())).thenReturn(Collections.singletonList(template));
        ReceiveDocDO data = new ReceiveDocDO();
        data.setSubject("预算$5\\附件");
        when(receiveDocMapper.selectOne(any())).thenReturn(data);
        assertEquals("关于《预算$5\\附件》，请办理。", service.render(7L, "task", "legacy", "default"));
        verify(receiveDocMapper).selectOne(argThat(wrapper -> wrapper.getSqlSegment().contains("process_instance_id")));
        assertEquals("old-biz", binding.getBizdefGuid());
    }

    @Test void rejectsTemplateOutsideCurrentNodeBeforeLoadingBusiness() {
        authorizedBinding();
        BizCommentTemplateDO template = template();
        template.setCommentAct("other-old-node");
        when(bizMapper.selectList(any())).thenReturn(Collections.singletonList(template));
        assertThrows(ServiceException.class, () -> service.render(7L, "task", "legacy", "Default"));
        verifyNoInteractions(receiveDocMapper);
    }

    @Test void rejectsUnknownFieldInsteadOfSilentlyDroppingIt() {
        authorizedBinding();
        BizCommentTemplateDO template = template();
        template.setCommentContent("{UNMAPPED_COLUMN}");
        when(bizMapper.selectList(any())).thenReturn(Collections.singletonList(template));
        when(receiveDocMapper.selectOne(any())).thenReturn(new ReceiveDocDO());
        assertThrows(ServiceException.class, () -> service.render(7L, "task", "legacy", "Default"));
    }

    @Test void rejectsUnclaimedTasks() {
        when(taskService.validateTask(7L, "task")).thenReturn(task);
        when(task.getAssignee()).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.options(7L, "task", "Default"));
        verifyNoInteractions(bindingMapper, personalMapper, bizMapper);
    }

    @Test void rejectsTaskFromAnotherTenant() {
        when(taskService.validateTask(7L, "task")).thenReturn(task);
        when(task.getAssignee()).thenReturn("7");
        when(task.getTenantId()).thenReturn("2");
        assertThrows(ServiceException.class, () -> service.options(7L, "task", "Default"));
        verifyNoInteractions(bindingMapper, personalMapper, bizMapper);
    }

    @Test void cannotUpdateOrDeleteAnotherUsersPersonalTemplate() {
        PersonalCommentTemplateDO row = new PersonalCommentTemplateDO();
        row.setUserId(8L);
        when(personalMapper.selectById(10L)).thenReturn(row);
        CommentTemplateSaveVO.Personal vo = new CommentTemplateSaveVO.Personal();
        vo.setOaIdeaId(10L); vo.setIdeaMessage("同意"); vo.setIdeaOrder(1);
        assertThrows(ServiceException.class, () -> service.savePersonal(7L, vo));
        assertThrows(ServiceException.class, () -> service.deletePersonal(7L, 10L));
        verify(personalMapper, never()).updateById(any(PersonalCommentTemplateDO.class));
        verify(personalMapper, never()).deleteById(anyLong());
    }

    @Test void reorderValidatesEveryOwnerBeforeWriting() {
        PersonalCommentTemplateDO own = new PersonalCommentTemplateDO(); own.setUserId(7L);
        PersonalCommentTemplateDO other = new PersonalCommentTemplateDO(); other.setUserId(8L);
        when(personalMapper.selectById(1L)).thenReturn(own);
        when(personalMapper.selectById(2L)).thenReturn(other);
        assertThrows(ServiceException.class, () -> service.orderPersonal(7L, Arrays.asList(1L, 2L)));
        verify(personalMapper, never()).updateById(any(PersonalCommentTemplateDO.class));
    }

    @Test void rejectsUnsafeFieldMapping() {
        CommentTemplateSaveVO.Binding vo = new CommentTemplateSaveVO.Binding();
        vo.setSourceType("receivedoc");
        vo.setFieldMapping(Collections.singletonMap("OLD", "class.classLoader"));
        assertThrows(ServiceException.class, () -> service.saveBinding(vo));
        verifyNoInteractions(bindingMapper);
    }

    private CommentTemplateBindingDO authorizedBinding() {
        when(taskService.validateTask(7L, "task")).thenReturn(task);
        when(task.getAssignee()).thenReturn("7");
        when(task.getTenantId()).thenReturn("1");
        when(task.getTaskDefinitionKey()).thenReturn("review");
        when(task.getProcessDefinitionId()).thenReturn("process:1");
        when(repositoryService.getProcessDefinition("process:1")).thenReturn(definition);
        when(definition.getKey()).thenReturn("receive");
        CommentTemplateBindingDO binding = new CommentTemplateBindingDO();
        binding.setBizdefGuid("old-biz"); binding.setSourceType("receivedoc");
        binding.setNodeMapping("{\"old-node\":\"review\"}");
        binding.setFieldMapping("{\"T_DOC.SUBJECT\":\"subject\"}");
        when(bindingMapper.selectOne(any())).thenReturn(binding);
        lenient().when(task.getProcessInstanceId()).thenReturn("instance");
        return binding;
    }
    private BizCommentTemplateDO template() {
        BizCommentTemplateDO template = new BizCommentTemplateDO();
        template.setCommentGuid("legacy"); template.setCommentCode("Default,other");
        template.setCommentAct("old-node"); template.setCommentContent("关于《{T_DOC.SUBJECT}》，请办理。");
        return template;
    }
}
