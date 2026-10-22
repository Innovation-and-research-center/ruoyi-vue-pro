package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import org.flowable.bpmn.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BpmInternalLoopUtilsTest {

    @Test
    void reviewAndLawsuitCanContinueThroughSingleGateway() {
        for (String key : new String[]{"oa_review", "oa_lawsuit"}) {
            BpmnModel model = model(key);
            UserTask source = node(model, new UserTask(), "relatedDept");
            InclusiveGateway gateway = node(model, new InclusiveGateway(), "gateway");
            UserTask target = node(model, new UserTask(), "legalDept");
            connect(source, gateway);
            connect(gateway, target);
            assertTrue(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        }
    }

    @Test
    void supportsDirectTaskAndEndEvent() {
        for (FlowNode target : new FlowNode[]{new UserTask(), new EndEvent()}) {
            BpmnModel model = model("oa_review");
            UserTask source = node(model, new UserTask(), "source");
            node(model, target, "target");
            connect(source, target);
            assertTrue(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        }
    }

    @Test
    void otherProcessesKeepInternalRestriction() {
        BpmnModel model = model("other");
        UserTask source = node(model, new UserTask(), "source");
        connect(source, node(model, new UserTask(), "target"));
        assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
    }

    @Test
    void rejectsMultipleOutgoingFlowsIncludingGatewayBranches() {
        for (boolean atGateway : new boolean[]{false, true}) {
            BpmnModel model = model("oa_lawsuit");
            UserTask source = node(model, new UserTask(), "source");
            FlowNode branching = source;
            if (atGateway) {
                branching = node(model, new InclusiveGateway(), "gateway");
                connect(source, branching);
            }
            connect(branching, node(model, new UserTask(), "first"));
            connect(branching, node(model, new UserTask(), "second"));
            assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        }
    }

    @Test
    void rejectsConditionalPathsIncludingGatewayConditions() {
        for (boolean atGateway : new boolean[]{false, true}) {
            BpmnModel model = model("oa_review");
            UserTask source = node(model, new UserTask(), "source");
            InclusiveGateway gateway = node(model, new InclusiveGateway(), "gateway");
            SequenceFlow first = connect(source, gateway);
            SequenceFlow second = connect(gateway, node(model, new UserTask(), "target"));
            (atGateway ? second : first).setConditionExpression("${approved}");
            assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        }
    }

    @Test
    void rejectsCyclesAndMissingTargets() {
        BpmnModel model = model("oa_review");
        UserTask source = node(model, new UserTask(), "source");
        assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        InclusiveGateway gateway = node(model, new InclusiveGateway(), "gateway");
        connect(source, gateway);
        connect(gateway, gateway);
        assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        gateway.getOutgoingFlows().clear();
        connect(gateway, source);
        assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
        gateway.getOutgoingFlows().clear();
        SequenceFlow missing = new SequenceFlow();
        missing.setTargetRef("missing");
        gateway.getOutgoingFlows().add(missing);
        assertFalse(BpmInternalLoopUtils.canContinueToSingleTarget(model, source));
    }

    private BpmnModel model(String key) {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        process.setId(key);
        model.addProcess(process);
        return model;
    }

    private <T extends FlowNode> T node(BpmnModel model, T node, String id) {
        node.setId(id);
        model.getMainProcess().addFlowElement(node);
        return node;
    }

    private SequenceFlow connect(FlowNode source, FlowNode target) {
        SequenceFlow flow = new SequenceFlow(source.getId(), target.getId());
        flow.setTargetFlowElement(target);
        source.getOutgoingFlows().add(flow);
        target.getIncomingFlows().add(flow);
        return flow;
    }
}
