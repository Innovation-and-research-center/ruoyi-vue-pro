package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmDispatchSources;
import org.flowable.bpmn.model.*;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmRestoredDispatchTest {
    private final RuntimeService runtime = mock(RuntimeService.class);
    private final Task task = mock(Task.class);
    private final BpmTaskServiceImpl service = new BpmTaskServiceImpl();
    private final BpmnModel model = new BpmnModel();
    private final UserTask target = new UserTask();

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        when(task.getExecutionId()).thenReturn("restored");
        when(task.getProcessInstanceId()).thenReturn("instance");
        when(task.getTaskDefinitionKey()).thenReturn("director");
        when(runtime.getVariable("restored", BpmDispatchSources.RESTORED_FROM))
                .thenReturn(Collections.singletonMap("director", Collections.singleton("oldTask")));
        when(runtime.getVariableLocal("instance", BpmDispatchSources.LEDGER_PREFIX + "oldTask"))
                .thenReturn(Collections.singletonMap("target", new LinkedHashSet<>(Arrays.asList(4L, 6L))));
        target.setId("target"); target.setName("主办科室");
        ExtensionElement strategy = new ExtensionElement(); strategy.setName("candidateStrategy"); strategy.setElementText("55");
        target.addExtensionElement(strategy);
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId("process"); process.addFlowElement(target); model.addProcess(process);
    }

    @Test
    void retainedMembersRejectedButWithdrawnAndNewRecipientsAllowed() {
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 4L));
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 5L, 6L));
        assertDoesNotThrow(() -> validate(Collections.singleton("target"), 5L, 8L));
    }

    @Test
    void unselectedRouteDoesNotBlockOtherDispatchOrCompletion() {
        assertDoesNotThrow(() -> validate(Collections.emptySet(), 4L));
    }

    @Test
    void automaticRouteCannotRedispatchRetainedMembersThroughForgedSelection() {
        target.getExtensionElements().clear();
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target")));
        assertThrows(RuntimeException.class, () -> validate(Collections.singleton("target"), 8L));
        when(runtime.getVariableLocal("instance", BpmDispatchSources.LEDGER_PREFIX + "oldTask"))
                .thenReturn(Collections.emptyMap());
        assertDoesNotThrow(() -> validate(Collections.singleton("target")));
    }

    @Test
    void ordinaryTaskWithoutRecoveryContextKeepsExistingRules() {
        when(runtime.getVariable("restored", BpmDispatchSources.RESTORED_FROM)).thenReturn(null);
        assertDoesNotThrow(() -> validate(Collections.singleton("target"), 4L));
    }

    private void validate(Set<String> selected, Long... users) {
        service.validateRestoredDispatch(task, model, selected,
                Collections.singletonMap("target", Arrays.asList(users)));
    }
}
