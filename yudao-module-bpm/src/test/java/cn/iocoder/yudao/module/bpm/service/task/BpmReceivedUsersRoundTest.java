package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmReceivedUsersRoundTest {

    @Test
    void completedRecipientsFromBeforeReturnAreExcluded() {
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 100), new Date(200)));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 250), new Date(200)));
    }

    @Test
    void runningAndCompletedRecipientsInThisRoundRemainVisible() {
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(1, 200), new Date(200)));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 201), new Date(200)));
    }

    @Test
    void canceledAndReturnedTasksRemainExcludedEvenInThisRound() {
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(4, 300), new Date(200)));
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(5, 300), new Date(200)));
    }

    @Test
    void legacyMissingStatusIsAllowedButMissingCreationTimeCannotEstablishCurrentRound() {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getCreateTime()).thenReturn(new Date(300));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(task, new Date(200)));
        when(task.getCreateTime()).thenReturn(null);
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(task, new Date(200)));
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(task, null));
    }

    @Test
    void multiInstanceRoundUsesParentActivityStartAndIgnoresOtherBranches() {
        BpmProcessInstanceServiceImpl service = new BpmProcessInstanceServiceImpl();
        RuntimeService runtime = mock(RuntimeService.class);
        HistoryService history = mock(HistoryService.class);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "historyService", history);

        Task current = mock(Task.class);
        when(current.getCreateTime()).thenReturn(new Date(400));
        when(current.getExecutionId()).thenReturn("child");
        when(current.getProcessInstanceId()).thenReturn("process");
        when(current.getTaskDefinitionKey()).thenReturn("source");
        Execution execution = mock(Execution.class);
        when(execution.getParentId()).thenReturn("mi-root");
        ExecutionQuery executionQuery = mock(ExecutionQuery.class);
        when(runtime.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.executionId("child")).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(execution);

        HistoricActivityInstanceQuery activityQuery = mock(HistoricActivityInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
        List<HistoricActivityInstance> activities = Arrays.asList(
                activity("other-branch", 100), activity("child", 400), activity("mi-root", 200));
        when(activityQuery.list()).thenReturn(activities);

        Date start = ReflectionTestUtils.invokeMethod(service, "getSendRoundStartTime", current);
        assertEquals(new Date(200), start);
        // 本轮会签的其他人员已发送的任务，即使早于当前子任务创建，也应保留。
        assertTrue(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 300), start));
        assertFalse(BpmProcessInstanceServiceImpl.isCurrentRoundReceivedTask(recipient(2, 150), start));
        verify(activityQuery).processInstanceId("process");
        verify(activityQuery).activityId("source");
        verify(activityQuery).unfinished();

        when(activityQuery.list()).thenReturn(Collections.emptyList());
        assertEquals(new Date(400), ReflectionTestUtils.invokeMethod(service, "getSendRoundStartTime", current));
    }

    private static HistoricTaskInstance recipient(int status, long createdAt) {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getTaskLocalVariables()).thenReturn(Collections.singletonMap(BpmnVariableConstants.TASK_VARIABLE_STATUS, status));
        when(task.getCreateTime()).thenReturn(new Date(createdAt));
        return task;
    }

    private static HistoricActivityInstance activity(String executionId, long startedAt) {
        HistoricActivityInstance activity = mock(HistoricActivityInstance.class);
        when(activity.getExecutionId()).thenReturn(executionId);
        when(activity.getStartTime()).thenReturn(new Date(startedAt));
        return activity;
    }
}
