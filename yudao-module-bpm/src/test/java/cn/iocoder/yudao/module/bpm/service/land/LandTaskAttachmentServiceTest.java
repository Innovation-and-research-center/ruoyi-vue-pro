package cn.iocoder.yudao.module.bpm.service.land;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandTaskAttachmentSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo.CommentAttachSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.commentattach.CommentAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandMapper;
import cn.iocoder.yudao.module.bpm.service.commentattach.CommentAttachServiceImpl;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.DelegationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LandTaskAttachmentServiceTest {
    private final LandTaskAttachmentService service = new LandTaskAttachmentService();
    private final LandService landService = mock(LandService.class);
    private final LandMapper lands = mock(LandMapper.class);
    private final CommentAttachMapper attachments = mock(CommentAttachMapper.class);
    private final FileMapper files = mock(FileMapper.class);
    private final TaskService tasks = mock(TaskService.class);
    private final TaskQuery query = mock(TaskQuery.class);
    private final Task task = mock(Task.class);

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(service, "landService", landService);
        ReflectionTestUtils.setField(service, "landMapper", lands);
        ReflectionTestUtils.setField(service, "commentAttachMapper", attachments);
        ReflectionTestUtils.setField(service, "fileMapper", files);
        ReflectionTestUtils.setField(service, "taskService", tasks);
        when(lands.selectForUpdate(10L)).thenReturn(new LandDO().setId(10L).setStatus(1).setProcessInstanceId("process-10"));
        when(tasks.createTaskQuery()).thenReturn(query);
        when(query.taskId(anyString())).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.singleResult()).thenReturn(task);
        when(task.getId()).thenReturn("task-main");
        when(task.getProcessInstanceId()).thenReturn("process-10");
        when(task.getAssignee()).thenReturn("7");
        when(task.getName()).thenReturn("主办科室");
        FileDO file = new FileDO().setId(21L).setName("附件.docx").setPath("test/file.docx").setUrl("https://example.invalid/file.docx");
        file.setCreator("7");
        when(files.selectById(21L)).thenReturn(file);
        when(attachments.selectList(any())).thenReturn(Collections.emptyList());
        doAnswer(call -> { ((CommentAttachDO) call.getArgument(0)).setId(31L); return 1; })
                .when(attachments).insert(any(CommentAttachDO.class));
    }

    @Test
    void uploadUsesServerFileMetadataAndBindsBusinessAndCurrentTask() {
        assertEquals(31L, service.create(7L, request()));
        ArgumentCaptor<CommentAttachDO> saved = ArgumentCaptor.forClass(CommentAttachDO.class);
        verify(attachments).insert(saved.capture());
        assertEquals("LAND", saved.getValue().getDocType());
        assertEquals("10", saved.getValue().getDocId());
        assertEquals("task-main", saved.getValue().getTaskId());
        assertEquals("附件.docx", saved.getValue().getFilename());
        assertEquals("7", saved.getValue().getCreator());
        assertEquals("主办科室", saved.getValue().getCommentType());
    }

    @Test
    void foreignTaskAssigneeAndPendingDelegationCannotUpload() {
        when(task.getProcessInstanceId()).thenReturn("other-process");
        assertDenied(() -> service.create(7L, request()));
        when(task.getProcessInstanceId()).thenReturn("process-10");
        when(task.getAssignee()).thenReturn("8");
        assertDenied(() -> service.create(7L, request()));
        when(task.getAssignee()).thenReturn("7");
        when(task.getDelegationState()).thenReturn(DelegationState.PENDING);
        assertDenied(() -> service.create(7L, request()));
        verifyNoInteractions(files);
    }

    @Test
    void finishedOrInvalidBusinessAndCompletedTaskCannotUpload() {
        when(lands.selectForUpdate(10L)).thenReturn(new LandDO().setId(10L).setStatus(5));
        assertDenied(() -> service.create(7L, request()));
        when(lands.selectForUpdate(10L)).thenReturn(new LandDO().setId(10L).setStatus(2));
        assertDenied(() -> service.create(7L, request()));
        when(lands.selectForUpdate(10L)).thenReturn(new LandDO().setId(10L).setStatus(1));
        when(query.singleResult()).thenReturn(null);
        assertDenied(() -> service.create(7L, request()));
    }

    @Test
    void missingCrossTenantBusinessAndAnotherUsersFileAreRejected() {
        when(lands.selectForUpdate(10L)).thenReturn(null);
        assertEquals(LAND_NOT_EXISTS.getCode(), assertThrows(ServiceException.class, () -> service.create(7L, request())).getCode());
        when(lands.selectForUpdate(10L)).thenReturn(new LandDO().setId(10L).setStatus(1).setProcessInstanceId("process-10"));
        when(files.selectById(21L)).thenReturn(new FileDO().setId(21L).setPath("test.docx").setName("他人附件"));
        assertEquals(LAND_ATTACHMENT_INVALID.getCode(), assertThrows(ServiceException.class, () -> service.create(7L, request())).getCode());
        verify(attachments, never()).insert(any(CommentAttachDO.class));
    }

    @Test
    void repeatedAssociationReusesExistingTaskAttachment() {
        when(attachments.selectList(any())).thenReturn(Collections.singletonList(new CommentAttachDO().setId(31L)));
        assertEquals(31L, service.create(7L, request()));
        verify(attachments, never()).insert(any(CommentAttachDO.class));
    }

    @Test
    void onlyUploaderOnActiveTaskCanDeleteAndFileBodyIsPreserved() {
        CommentAttachDO attachment = new CommentAttachDO().setId(31L).setDocType("LAND").setDocId("10").setTaskId("task-main");
        attachment.setCreator("8");
        when(attachments.selectById(31L)).thenReturn(attachment);
        assertDenied(() -> service.delete(7L, 31L));
        attachment.setCreator("7");
        service.delete(7L, 31L);
        verify(attachments).deleteById(31L);
        verifyNoInteractions(files);
        when(query.singleResult()).thenReturn(null);
        assertDenied(() -> service.delete(7L, 31L));
        verify(attachments, times(1)).deleteById(31L);
    }

    @Test
    void readReusesBusinessAccessCheckBeforeQueryingAttachments() {
        doThrow(new ServiceException(LAND_ACCESS_DENIED)).when(landService).get(8L, 10L);
        assertDenied(() -> service.getList(8L, 10L));
        verify(attachments, never()).selectList(any());
        service.getList(7L, 10L);
        verify(landService).get(7L, 10L);
    }

    @Test
    void genericAttachmentMutationCannotBypassLandTaskRules() {
        CommentAttachServiceImpl generic = new CommentAttachServiceImpl();
        ReflectionTestUtils.setField(generic, "commentAttachMapper", attachments);
        ReflectionTestUtils.setField(generic, "landTaskAttachmentService", service);
        assertDenied(() -> generic.createCommentAttach(new CommentAttachSaveReqVO().setDocType("LAND")));
        when(attachments.selectById(31L)).thenReturn(new CommentAttachDO().setId(31L).setDocType("LAND"));
        assertDenied(() -> generic.updateCommentAttach(new CommentAttachSaveReqVO().setId(31L).setDocType("XZFY")));
        when(attachments.selectBatchIds(anyCollection())).thenReturn(Collections.singletonList(new CommentAttachDO().setId(31L).setDocType("LAND")));
        assertDenied(() -> generic.deleteCommentAttachListByIds(Collections.singletonList(31L)));
        verify(attachments, never()).deleteByIds(anyCollection());
        // 其他业务创建仍沿用原实现。
        generic.createCommentAttach(new CommentAttachSaveReqVO().setDocType("XZFY").setDocId("other"));
        verify(attachments).insert(argThat((CommentAttachDO a) -> "XZFY".equals(a.getDocType())));
    }

    private LandTaskAttachmentSaveReqVO request() {
        return new LandTaskAttachmentSaveReqVO().setLandId(10L).setTaskId("task-main").setFileId(21L);
    }
    private void assertDenied(org.junit.jupiter.api.function.Executable operation) {
        assertEquals(LAND_ACCESS_DENIED.getCode(), assertThrows(ServiceException.class, operation).getCode());
    }
}
