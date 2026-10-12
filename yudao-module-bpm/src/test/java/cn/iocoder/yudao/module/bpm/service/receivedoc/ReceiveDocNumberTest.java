package cn.iocoder.yudao.module.bpm.service.receivedoc;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import org.mockito.MockedStatic;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ReceiveDocCreateNumberVO;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ReceiveDocSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_REGISTER_TASK;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiveDocNumberTest {
    @Mock private ReceiveDocMapper mapper;
    @Mock private ReceiveDocAttachMapper attachMapper;
    @Mock private BpmProcessInstanceApi processInstanceApi;
    @InjectMocks private ReceiveDocServiceImpl service;

    @Test void webNumberGenerationDoesNotTryCandidates() {
        when(mapper.selectObjs(any())).thenReturn(Collections.singletonList(99L));
        ReceiveDocCreateNumberVO request = new ReceiveDocCreateNumberVO();
        request.setDocClass("41");
        request.setYear("2026");
        assertEquals("2026-41-0100", service.generateDocumentSequence(request));
        verify(mapper, never()).selectCount(any());
    }

    @Test void jobUsesSameSkippingRule() {
        when(mapper.selectObjs(any())).thenReturn(Collections.singletonList(99L));
        when(mapper.selectCount(any())).thenReturn(1L, 0L);
        assertEquals(101L, service.generateDocumentSequence("41", "2026"));
    }

    @Test void emptySequenceStartsAtOne() {
        when(mapper.selectObjs(any())).thenReturn(Collections.singletonList(null));
        when(mapper.selectCount(any())).thenReturn(0L);
        assertEquals(1L, service.generateDocumentSequence("41", "2026"));
    }

    @Test void generationStopsAfterTwentyOccupiedCandidates() {
        when(mapper.selectObjs(any())).thenReturn(Collections.singletonList(99L));
        when(mapper.selectCount(any())).thenReturn(1L);
        assertEquals(RECEIVE_DOC_EXISTS.getCode(), assertThrows(ServiceException.class,
                () -> service.generateDocumentSequence("41", "2026")).getCode());
        verify(mapper, times(20)).selectCount(any());
        verify(mapper, never()).insert(any(ReceiveDocDO.class));
    }

    @Test void availableNumberIsPreservedAndSequenceDerivedFromNumber() {
        when(mapper.selectCount(any())).thenReturn(0L);
        ReceiveDocSaveReqVO request = request("2026-41-0100");
        request.setDocSequence(1L);
        ReceiveDocDO result = service.prepareJobReceiveDocForInsert(request);
        assertEquals("2026-41-0100", result.getReceiveDocNumber());
        assertEquals(100L, result.getDocSequence());
    }

    @Test void conflictIncrementsWithoutChangingNumberPrefix() {
        when(mapper.selectCount(any())).thenReturn(1L, 1L, 0L);
        ReceiveDocSaveReqVO request = request("2025-41-9999");
        ReceiveDocDO result = service.prepareJobReceiveDocForInsert(request);
        assertEquals("2025-41-10001", result.getReceiveDocNumber());
        assertEquals(10001L, result.getDocSequence());
        assertEquals(result.getReceiveDocNumber(), request.getReceiveDocNumber());
        assertEquals(result.getDocSequence(), request.getDocSequence());
    }

    @Test void saveStopsAtLimitWithoutInsertingOrChangingRequest() {
        when(mapper.selectCount(any())).thenReturn(1L);
        ReceiveDocSaveReqVO request = request("2026-41-0100");
        assertEquals(RECEIVE_DOC_EXISTS.getCode(), assertThrows(ServiceException.class,
                () -> service.prepareJobReceiveDocForInsert(request)).getCode());
        assertEquals("2026-41-0100", request.getReceiveDocNumber());
        verify(mapper, times(20)).selectCount(any());
        verify(mapper, never()).insert(any(ReceiveDocDO.class));
    }

    @Test void invalidWebNumberIsRejectedAfterOriginalDuplicateCheck() {
        when(mapper.selectCount(any())).thenReturn(0L);
        assertEquals(RECEIVE_DOC_ERROR.getCode(), assertThrows(ServiceException.class,
                () -> service.saveReceiveDoc(7L, request("invalid"))).getCode());
        verify(mapper).selectCount(any());
        verify(mapper, never()).insert(any(ReceiveDocDO.class));
    }

    @Test void overflowFailsWithoutWrappingSequence() {
        when(mapper.selectCount(any())).thenReturn(1L);
        assertEquals(RECEIVE_DOC_ERROR.getCode(), assertThrows(ServiceException.class,
                () -> service.prepareJobReceiveDocForInsert(request("2026-41-" + Long.MAX_VALUE))).getCode());
    }

    @Test void webCreationAndSaveRejectConflictWithoutTryingAnotherNumber() {
        when(mapper.selectCount(any())).thenReturn(1L);
        for (int entry = 0; entry < 2; entry++) {
            ReceiveDocSaveReqVO request = request("2026-41-0100");
            final int currentEntry = entry;
            assertEquals(RECEIVE_DOC_EXISTS.getCode(), assertThrows(ServiceException.class, () -> {
                if (currentEntry == 0) service.createReceiveDoc(7L, request);
                else service.saveReceiveDoc(7L, request);
            }).getCode());
            assertEquals("2026-41-0100", request.getReceiveDocNumber());
        }
        verify(mapper, times(2)).selectCount(any());
        verify(mapper, never()).insert(any(ReceiveDocDO.class));
    }

    @Test void webCreationAndSaveKeepOriginalNumberWhenAvailable() {
        when(mapper.selectCount(any())).thenReturn(0L);
        RuntimeException insertFailure = new RuntimeException("insert failed");
        when(mapper.insert(any(ReceiveDocDO.class))).thenThrow(insertFailure);
        assertSame(insertFailure, assertThrows(RuntimeException.class,
                () -> service.createReceiveDoc(7L, request("2026-41-0100"))));
        assertSame(insertFailure, assertThrows(RuntimeException.class,
                () -> service.saveReceiveDoc(7L, request("2026-41-0100"))));
        verify(mapper, times(2)).insert(argThat((ReceiveDocDO row) ->
                "2026-41-0100".equals(row.getReceiveDocNumber()) && row.getDocSequence() == 100L));
    }

    @Test void jobRetriesBeforeSingleInsertAndPropagatesOtherFailures() {
        RuntimeException insertFailure = new RuntimeException("insert failed");
        when(mapper.insert(any(ReceiveDocDO.class))).thenThrow(insertFailure);
        when(mapper.selectCount(any())).thenReturn(1L, 0L);
        ReceiveDocSaveReqVO request = request("2026-41-0100");
        request.setNextNodeAssignees(Collections.singletonMap(RECEIVE_REGISTER_TASK,
                Collections.singletonList(7L)));
        request.setStartUserSelectAssignees(request.getNextNodeAssignees());
        assertSame(insertFailure, assertThrows(RuntimeException.class,
                () -> service.saveJobReceiveDoc(7L, request)));
        assertEquals("2026-41-0101", request.getReceiveDocNumber());
        verify(mapper).insert(argThat((ReceiveDocDO row) ->
                "2026-41-0101".equals(row.getReceiveDocNumber()) && row.getDocSequence() == 101L));
    }

    @Test void numberTakenAfterGenerationRetriesAndCreatesAttachmentsAndProcessOnlyOnce() {
        when(mapper.selectObjs(any())).thenReturn(Collections.singletonList(99L));
        // 取号时可用，保存时已经占用，下一编号可用。
        when(mapper.selectCount(any())).thenReturn(0L, 1L, 0L);
        ReceiveDocSaveReqVO request = jobRequest("2026-41-" + String.format("%04d",
                service.generateDocumentSequence("41", "2026")));
        mockSuccessfulInsert();
        when(processInstanceApi.createProcessInstance(eq(7L), any())).thenReturn("process-123");
        try (MockedStatic<DictFrameworkUtils> dict = mockStatic(DictFrameworkUtils.class)) {
            assertEquals(123L, service.saveJobReceiveDoc(7L, request));
        }
        assertEquals("2026-41-0101", request.getReceiveDocNumber());
        assertEquals(101L, request.getDocSequence());
        verify(mapper, times(3)).selectCount(any());
        verify(mapper).insert(argThat((ReceiveDocDO row) ->
                "2026-41-0101".equals(row.getReceiveDocNumber()) && row.getDocSequence() == 101L));
        verify(attachMapper).insertBatch(anyCollection());
        assertEquals(123L, request.getFileList().get(0).getReceiveDocId());
        verify(processInstanceApi).createProcessInstance(eq(7L), argThat(dto ->
                "123".equals(dto.getBusinessKey())));
        verify(mapper).updateById(argThat((ReceiveDocDO row) ->
                row.getId() == 123L && "process-123".equals(row.getProcessInstanceId())));
    }

    @Test void jobSaveSharesTwentyCandidateLimitWithoutWritingAnything() {
        when(mapper.selectCount(any())).thenReturn(1L);
        assertEquals(RECEIVE_DOC_EXISTS.getCode(), assertThrows(ServiceException.class,
                () -> service.saveJobReceiveDoc(7L, jobRequest("2026-41-0100"))).getCode());
        verify(mapper, times(20)).selectCount(any());
        verify(mapper, never()).insert(any(ReceiveDocDO.class));
        verifyNoInteractions(attachMapper, processInstanceApi);
    }

    @Test void twentiethCandidateCanBeSaved() {
        java.util.concurrent.atomic.AtomicInteger checks = new java.util.concurrent.atomic.AtomicInteger();
        when(mapper.selectCount(any())).thenAnswer(invocation -> checks.incrementAndGet() < 20 ? 1L : 0L);
        mockSuccessfulInsert();
        when(processInstanceApi.createProcessInstance(eq(7L), any())).thenReturn("process-123");
        ReceiveDocSaveReqVO request = jobRequest("2026-41-0100");
        try (MockedStatic<DictFrameworkUtils> dict = mockStatic(DictFrameworkUtils.class)) {
            assertEquals(123L, service.saveJobReceiveDoc(7L, request));
        }
        assertEquals("2026-41-0119", request.getReceiveDocNumber());
        verify(mapper, times(20)).selectCount(any());
        verify(mapper).insert(any(ReceiveDocDO.class));
        verify(processInstanceApi).createProcessInstance(eq(7L), any());
    }

    @Test void processFailureWithDuplicateErrorCodeIsNeverRetried() {
        when(mapper.selectCount(any())).thenReturn(0L);
        mockSuccessfulInsert();
        ServiceException failure = new ServiceException(RECEIVE_DOC_EXISTS);
        when(processInstanceApi.createProcessInstance(eq(7L), any())).thenThrow(failure);
        try (MockedStatic<DictFrameworkUtils> dict = mockStatic(DictFrameworkUtils.class)) {
            assertSame(failure, assertThrows(ServiceException.class,
                    () -> service.saveJobReceiveDoc(7L, jobRequest("2026-41-0100"))));
        }
        verify(mapper).selectCount(any());
        verify(mapper).insert(any(ReceiveDocDO.class));
        verify(attachMapper).insertBatch(anyCollection());
        verify(processInstanceApi).createProcessInstance(eq(7L), any());
        verify(mapper, never()).updateById(any(ReceiveDocDO.class));
    }

    private void mockSuccessfulInsert() {
        when(mapper.insert(any(ReceiveDocDO.class))).thenAnswer(invocation -> {
            ReceiveDocDO row = invocation.getArgument(0);
            row.setId(123L);
            return 1;
        });
    }

    private ReceiveDocSaveReqVO jobRequest(String number) {
        ReceiveDocSaveReqVO request = request(number);
        request.setNextNodeAssignees(Collections.singletonMap(RECEIVE_REGISTER_TASK,
                Collections.singletonList(7L)));
        request.setStartUserSelectAssignees(request.getNextNodeAssignees());
        request.setFileList(Collections.singletonList(new ReceiveDocAttachDO()));
        return request;
    }

    private ReceiveDocSaveReqVO request(String number) {
        ReceiveDocSaveReqVO request = new ReceiveDocSaveReqVO();
        request.setReceiveDocNumber(number);
        request.setDocClass("41");
        request.setYear("2026");
        return request;
    }
}
