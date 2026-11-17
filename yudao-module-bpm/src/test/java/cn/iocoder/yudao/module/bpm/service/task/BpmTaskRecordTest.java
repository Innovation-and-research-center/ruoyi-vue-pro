package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.convert.task.BpmTaskConvert;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmRestoredOpinion;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmTaskRecordTest {
    @Test
    void traceUsesBusinessStatusEvenWhenEngineDeleteReasonIsPresent() {
        BpmTaskServiceImpl service = new BpmTaskServiceImpl();
        HistoryService history = mock(HistoryService.class);
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricTaskInstanceQuery()).thenReturn(query);
        ReflectionTestUtils.setField(service, "historyService", history);
        List<HistoricTaskInstance> tasks = new ArrayList<>();
        List<BpmTaskFlowTaskNodeRespVO> nodes = new ArrayList<>();
        for (int status : new int[]{1, 2, 3, 4, 5, 7, 0}) {
            HistoricTaskInstance task = task("t" + status, status);
            when(task.getDeleteReason()).thenReturn("Change activity to restored node");
            tasks.add(task);
            BpmTaskFlowTaskNodeRespVO node = new BpmTaskFlowTaskNodeRespVO();
            node.setTaskId(task.getId()); node.setActivityType("userTask"); node.setStatus(3);
            nodes.add(node);
        }
        when(query.list()).thenReturn(tasks);
        BpmTaskTraceDTO trace = new BpmTaskTraceDTO();
        trace.setPreviousTasks(nodes);
        ReflectionTestUtils.invokeMethod(service, "fillTraceTaskStatuses", trace);
        for (int i = 0; i < tasks.size(); i++) {
            assertEquals(tasks.get(i).getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS), nodes.get(i).getStatus());
        }
        verify(query).includeTaskLocalVariables();
        verify(query, times(1)).list();
    }

    @Test
    void missingBusinessStatusNeverTreatsCancellationAsRejection() {
        BpmTaskServiceImpl service = new BpmTaskServiceImpl();
        assertNull(ReflectionTestUtils.invokeMethod(service, "calculateStatus", new Date(), "deleted"));
        assertEquals(Integer.valueOf(1), ReflectionTestUtils.invokeMethod(service, "calculateStatus", null, "deleted"));
        assertEquals(Integer.valueOf(2), ReflectionTestUtils.invokeMethod(service, "calculateStatus", new Date(), null));
    }

    @Test
    void historyListKeepsCancelledAndReturnedTasks() {
        List<BpmTaskRespVO> records = BpmTaskConvert.INSTANCE.buildTaskListByProcessInstanceId(
                Arrays.asList(task("cancelled", 4), task("returned", 5)),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(2, records.size());
        assertEquals(Integer.valueOf(4), records.get(0).getStatus());
        assertEquals(Integer.valueOf(5), records.get(1).getStatus());
    }

    @Test
    void recoveryUsesExplicitSourceBeforeTimeBasedGuessing() {
        BpmTaskServiceImpl service = new BpmTaskServiceImpl();
        HistoryService history = mock(HistoryService.class);
        HistoricVariableInstanceQuery variables = mock(HistoricVariableInstanceQuery.class, RETURNS_SELF);
        HistoricVariableInstance sourceVariable = mock(HistoricVariableInstance.class);
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricVariableInstanceQuery()).thenReturn(variables);
        doReturn(sourceVariable).when(variables).singleResult();
        when(sourceVariable.getValue()).thenReturn("source");
        when(history.createHistoricTaskInstanceQuery()).thenReturn(query);
        doReturn(task("source", 2)).when(query).singleResult();
        ReflectionTestUtils.setField(service, "historyService", history);
        List<BpmTaskFlowTaskNodeRespVO> nodes = ReflectionTestUtils.invokeMethod(service, "findPreviousNodes", task("restored", 1));
        assertEquals("source", nodes.get(0).getTaskId());
        verify(variables).variableName(BpmRestoredOpinion.SOURCE_TASK);
        verify(history, never()).createHistoricActivityInstanceQuery();
    }

    @Test
    void outgoingTraceIncludesRecoveryTasksWithoutDuplicates() {
        BpmTaskServiceImpl service = new BpmTaskServiceImpl();
        HistoryService history = mock(HistoryService.class);
        HistoricVariableInstanceQuery variables = mock(HistoricVariableInstanceQuery.class, RETURNS_SELF);
        HistoricVariableInstance variable = mock(HistoricVariableInstance.class);
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        when(history.createHistoricVariableInstanceQuery()).thenReturn(variables);
        when(variables.list()).thenReturn(Collections.singletonList(variable));
        when(variable.getTaskId()).thenReturn("restored");
        when(history.createHistoricTaskInstanceQuery()).thenReturn(query);
        doReturn(task("restored", 1)).when(query).singleResult();
        ReflectionTestUtils.setField(service, "historyService", history);
        List<BpmTaskFlowTaskNodeRespVO> nodes = ReflectionTestUtils.invokeMethod(service, "findNextNodes", task("source", 2));
        assertEquals(1, nodes.size());
        assertEquals("restored", nodes.get(0).getTaskId());
        verify(variables).variableName(BpmRestoredOpinion.SOURCE_TASK);
        verify(history, never()).createHistoricActivityInstanceQuery();
    }

    private HistoricTaskInstance task(String id, int status) {
        HistoricTaskInstance task = mock(HistoricTaskInstance.class);
        when(task.getId()).thenReturn(id);
        when(task.getProcessInstanceId()).thenReturn("process");
        when(task.getTaskLocalVariables()).thenReturn(Collections.singletonMap(BpmnVariableConstants.TASK_VARIABLE_STATUS, status));
        return task;
    }
}
