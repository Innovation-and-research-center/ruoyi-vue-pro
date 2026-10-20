package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.*;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.flowable.bpmn.model.*;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.flowable.engine.*;
import org.flowable.engine.runtime.Execution;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPEND_INVALID;

/** 已办追加转出：运行目标精确增员，未派发/已结束目标按原办理范围新建独立轮次。 */
@Service
@lombok.extern.slf4j.Slf4j
public class BpmTaskAppendService {
    @Resource private RuntimeService runtimeService;
    @Resource private HistoryService historyService;
    @Resource private RepositoryService repositoryService;
    @Resource private ManagementService managementService;
    @Resource private TaskService flowableTaskService;
    @Resource private BpmProcessInstanceService processInstanceService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private DeptService deptService;

    private HistoricTaskInstance source(Long userId, String taskId) {
        HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskId(taskId)
                .includeTaskLocalVariables().singleResult();
        if (task == null || task.getEndTime() == null || !String.valueOf(userId).equals(task.getAssignee())
                || !BpmTaskStatusEnum.APPROVE.getStatus().equals(task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS))) {
            throw exception(TASK_APPEND_INVALID, "只能从本人已办理完成的任务追加");
        }
        if (!BpmTaskDispatchPolicy.allowsMemberOperations(repositoryService.getBpmnModel(task.getProcessDefinitionId()), task.getTaskDefinitionKey())) {
            throw exception(TASK_APPEND_INVALID, "只有收文主任拟办节点允许追加");
        }
        if (runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).active().singleResult() == null) {
            throw exception(TASK_APPEND_INVALID, "流程已结束或挂起");
        }
        return task;
    }

    public List<BpmTaskAppendTargetRespVO> targets(Long userId, String taskId) {
        long started = System.nanoTime();
        HistoricTaskInstance source = source(userId, taskId);
        List<BpmTaskAppendTargetRespVO> result = new ArrayList<>();
        if (processInstanceService.isRestrictedInternalOperator(userId, source.getProcessDefinitionId(),
                source.getTaskDefinitionKey(), source.getTaskLocalVariables().containsKey("internal_source_task_id"))) {
            // 内循环追加尚需绑定原会签实例；先禁止借普通转出追加越过本部门限制。
            return result;
        }
        String instanceId = source.getProcessInstanceId();
        org.flowable.bpmn.model.BpmnModel model = repositoryService.getBpmnModel(source.getProcessDefinitionId());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(instanceId).list();
        Map<String, Object> roots = localValues(instanceId, "nrOfInstances");
        Map<String, Object> origins = localValues(instanceId, BpmDispatchSources.SOURCES);
        // 只批量读取会签集合，不加载业务表单等无关变量。
        Map<String, Map<String, Object>> collections = new HashMap<>();
        runtimeService.createVariableInstanceQuery().processInstanceId(instanceId).excludeTaskVariables()
                .variableNameLike("%_assignees").list().forEach(variable -> collections
                .computeIfAbsent(variable.getExecutionId(), key -> new HashMap<>()).put(variable.getName(), variable.getValue()));
        Object ledger = runtimeService.getVariableLocal(instanceId, BpmDispatchSources.LEDGER_PREFIX + taskId);
        Map<String, Set<Long>> disabledByNode = new HashMap<>();
        for (Execution execution : executions) {
            if (!roots.containsKey(execution.getId())) continue;
            Set<Long> disabled = disabledByNode.computeIfAbsent(execution.getActivityId(), key -> new LinkedHashSet<>());
            Object members = collections.getOrDefault(execution.getId(), Collections.emptyMap())
                    .get(FlowableUtils.formatExecutionCollectionVariable(execution.getActivityId()));
            addMembers(disabled, members);
            if (!BpmDispatchSources.contains(origins.get(execution.getId()), taskId)) continue;
            Object element = model.getFlowElement(execution.getActivityId());
            if (!(element instanceof UserTask) || ((UserTask) element).getLoopCharacteristics() == null) continue;
            result.add(target((UserTask) element, execution.getId(), false));
        }
        if (ledger instanceof Map) ((Map<?, ?>) ledger).forEach((node, members) ->
                addMembers(disabledByNode.computeIfAbsent(node.toString(), key -> new LinkedHashSet<>()), members));
        flowableTaskService.createTaskQuery().processInstanceId(instanceId).active().list().forEach(task -> {
            if (task.getAssignee() != null) disabledByNode.computeIfAbsent(task.getTaskDefinitionKey(), key -> new LinkedHashSet<>())
                    .add(Long.valueOf(task.getAssignee()));
        });
        Object snapshot = source.getTaskLocalVariables().get(BpmDispatchSources.OPTIONS);
        Set<String> options = new LinkedHashSet<>();
        if (snapshot instanceof Collection) ((Collection<?>) snapshot).forEach(node -> options.add(node.toString()));
        boolean dispatched = !result.isEmpty() || ledger instanceof Map && !((Map<?, ?>) ledger).isEmpty();
        // 明确保存的空范围不能补授权；旧任务至少须存在可靠的真实转出关联。
        if (dispatched && (!(snapshot instanceof Collection) || !options.isEmpty())) {
            Object sourceNode = model.getFlowElement(source.getTaskDefinitionKey());
            if (sourceNode instanceof org.flowable.bpmn.model.FlowNode) options.addAll(
                    BpmDispatchSources.optionalManualTargets((org.flowable.bpmn.model.FlowNode) sourceNode));
        }
        if (dispatched) {
            Set<String> liveNodes = result.stream().map(BpmTaskAppendTargetRespVO::getNodeId).collect(Collectors.toSet());
            for (String nodeId : options) {
                if (liveNodes.contains(nodeId)) continue;
                Object element = model.getFlowElement(nodeId);
                if (!(element instanceof UserTask)) continue;
                UserTask node = (UserTask) element;
                if (node.getLoopCharacteristics() == null || node.getSubProcess() != null) continue;
                result.add(target(node, "new:" + nodeId, true));
            }
        }
        // 只为已授权目标补展示信息，不重新求值路由条件或扩大追加范围。
        Map<String, SequenceFlow> displayFlows = new LinkedHashMap<>();
        FlowElement sourceElement = model.getFlowElement(source.getTaskDefinitionKey());
        if (sourceElement instanceof FlowNode) collectDisplayFlows((FlowNode) sourceElement, displayFlows, new HashSet<>());
        for (BpmTaskAppendTargetRespVO target : result) {
            SequenceFlow flow = displayFlows.get(target.getNodeId());
            if (flow == null) continue;
            target.setFlowName(flow.getName());
            target.setFlowSort(flowOrder(flow));
        }
        long scopeLoaded = System.nanoTime();
        Map<String, String> executionNodes = new LinkedHashMap<>();
        result.forEach(target -> executionNodes.put(target.getExecutionId(), target.getNodeId()));
        Map<String, Set<Long>> allowedCandidates = processInstanceService.getAppendCandidateUserIdsBatch(userId, instanceId, executionNodes);
        long candidatesLoaded = System.nanoTime();
        Map<String, Set<Long>> candidatesByExecution = new LinkedHashMap<>();
        Set<Long> allUsers = new LinkedHashSet<>();
        for (BpmTaskAppendTargetRespVO target : result) {
            Set<Long> candidates = new LinkedHashSet<>(allowedCandidates.getOrDefault(target.getExecutionId(), Collections.emptySet()));
            candidates.addAll(disabledByNode.getOrDefault(target.getNodeId(), Collections.emptySet()));
            candidatesByExecution.put(target.getExecutionId(), candidates); allUsers.addAll(candidates);
        }
        Map<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO> users = adminUserApi.getUserList(allUsers)
                .stream().collect(Collectors.toMap(user -> user.getId(), user -> user, (first, duplicate) -> first));
        Set<Long> deptIds = users.values().stream().map(user -> user.getDeptId())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptDO> departments = deptIds.isEmpty() ? Collections.emptyMap() : deptService.getDeptMap(deptIds);
        // 与发送候选树一致：部门排序、部门编号、人员排序、人员编号，空值最后。
        Comparator<AdminUserRespDTO> displayOrder = Comparator
                .comparingInt((AdminUserRespDTO user) -> {
                    DeptDO department = user.getDeptId() == null ? null : departments.get(user.getDeptId());
                    return department != null && department.getSort() != null ? department.getSort() : Integer.MAX_VALUE;
                })
                .thenComparing(AdminUserRespDTO::getDeptId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(AdminUserRespDTO::getSort, Comparator.nullsLast(Long::compareTo))
                .thenComparing(AdminUserRespDTO::getId, Comparator.nullsLast(Long::compareTo));
        for (BpmTaskAppendTargetRespVO target : result) {
            Set<Long> disabled = disabledByNode.getOrDefault(target.getNodeId(), Collections.emptySet());
            target.setCandidates(candidatesByExecution.get(target.getExecutionId()).stream().filter(users::containsKey)
                    .sorted((first, second) -> displayOrder.compare(users.get(first), users.get(second))).map(id -> {
                BpmTaskAppendTargetRespVO.Member member = new BpmTaskAppendTargetRespVO.Member();
                member.setId(id); member.setNickname(users.get(id).getNickname());
                member.setDeptId(users.get(id).getDeptId()); member.setDisabled(disabled.contains(id));
                DeptDO department = departments.get(member.getDeptId());
                if (department != null) member.setDeptName(department.getName());
                member.setDisabledReason(member.isDisabled() ? "已派发或已完成，不能重复追加" : null);
                return member;
            }).collect(Collectors.toList()));
        }
        long finished = System.nanoTime();
        if (finished - started > 500_000_000L) {
            log.info("[append-targets] targets={}, scopeMs={}, candidatesMs={}, memberDetailsMs={}, totalMs={}",
                    result.size(), (scopeLoaded - started) / 1_000_000, (candidatesLoaded - scopeLoaded) / 1_000_000,
                    (finished - candidatesLoaded) / 1_000_000, (finished - started) / 1_000_000);
        }
        return result;
    }

    /** 与发送一致：取到达目标人工节点的连线名称和 order；不穿透其他人工节点。 */
    private static void collectDisplayFlows(FlowNode source, Map<String, SequenceFlow> flows, Set<String> visited) {
        if (!visited.add(source.getId())) return;
        for (SequenceFlow flow : source.getOutgoingFlows()) {
            FlowElement target = flow.getTargetFlowElement();
            if (target instanceof UserTask) flows.putIfAbsent(target.getId(), flow);
            else if (target instanceof Gateway) collectDisplayFlows((FlowNode) target, flows, visited);
        }
    }

    private static Integer flowOrder(SequenceFlow flow) {
        List<ExtensionElement> properties = new ArrayList<>(flow.getExtensionElements()
                .getOrDefault("property", Collections.emptyList()));
        for (ExtensionElement wrapper : flow.getExtensionElements().getOrDefault("properties", Collections.emptyList())) {
            properties.addAll(wrapper.getChildElements().getOrDefault("property", Collections.emptyList()));
        }
        String order = null;
        for (ExtensionElement property : properties) {
            if ("order".equals(property.getAttributeValue(null, "name"))) order = property.getAttributeValue(null, "value");
        }
        return cn.hutool.core.convert.Convert.toInt(order);
    }

    private Map<String, Object> localValues(String instanceId, String name) {
        Map<String, Object> result = new HashMap<>();
        runtimeService.createVariableInstanceQuery().processInstanceId(instanceId).excludeTaskVariables().variableName(name)
                .list().forEach(variable -> result.put(variable.getExecutionId(), variable.getValue()));
        return result;
    }

    private static void addMembers(Set<Long> members, Object value) {
        if (value instanceof Collection) for (Object user : (Collection<?>) value) {
            if (user != null) members.add(Long.valueOf(user.toString()));
        }
    }

    private static BpmTaskAppendTargetRespVO target(UserTask node, String executionId, boolean newRound) {
        BpmTaskAppendTargetRespVO target = new BpmTaskAppendTargetRespVO();
        target.setExecutionId(executionId); target.setNodeId(node.getId()); target.setNodeName(node.getName());
        target.setSequential(node.getLoopCharacteristics().isSequential()); target.setNewRound(newRound);
        return target;
    }

    @SuppressWarnings("unchecked")
    private Set<Long> dispatchedMembers(HistoricTaskInstance source, String nodeId) {
        Object value = runtimeService.getVariableLocal(source.getProcessInstanceId(), BpmDispatchSources.LEDGER_PREFIX + source.getId());
        if (!(value instanceof Map)) return Collections.emptySet();
        Object members = ((Map<?, ?>) value).get(nodeId);
        return members instanceof Collection ? new LinkedHashSet<>((Collection<Long>) members) : Collections.emptySet();
    }

    private Set<Long> existingMembers(String executionId, String nodeId) {
        Object value = runtimeService.getVariableLocal(executionId, FlowableUtils.formatExecutionCollectionVariable(nodeId));
        Set<Long> members = new LinkedHashSet<>();
        if (value instanceof Collection) for (Object user : (Collection<?>) value) {
            if (user != null) members.add(Long.valueOf(user.toString()));
        }
        return members;
    }

    @Transactional(rollbackFor = Exception.class)
    public void append(Long userId, BpmTaskAppendReqVO req) {
        if (req == null) throw exception(TASK_APPEND_INVALID, "追加请求不能为空");
        List<BpmTaskAppendReqVO.Target> lines = req.getTargets();
        if (lines == null) {
            validateLine(req.getTargetExecutionId(), req.getUserIds());
            appendOne(userId, req);
            return;
        }
        if (lines.isEmpty() || lines.size() > 50 || req.getTargetExecutionId() != null || req.getUserIds() != null) {
            throw exception(TASK_APPEND_INVALID, "请选择追加目标，且不能混用单目标和多目标参数");
        }
        Set<String> executionIds = new HashSet<>();
        int total = 0;
        for (BpmTaskAppendReqVO.Target line : lines) {
            if (line == null) throw exception(TASK_APPEND_INVALID, "追加目标不能为空");
            validateLine(line.getTargetExecutionId(), line.getUserIds());
            if (!executionIds.add(line.getTargetExecutionId())) throw exception(TASK_APPEND_INVALID, "同一目标不能重复提交");
            total += line.getUserIds().size();
        }
        if (total > 200) throw exception(TASK_APPEND_INVALID, "一次最多追加200人");
        // 所有目标共用外层事务；任一目标失效，人员、计数、短信策略和操作记录一起回滚。
        for (BpmTaskAppendReqVO.Target line : lines) {
            BpmTaskAppendReqVO single = new BpmTaskAppendReqVO();
            single.setSourceTaskId(req.getSourceTaskId()); single.setTargetExecutionId(line.getTargetExecutionId());
            single.setUserIds(line.getUserIds()); single.setReason(req.getReason()); single.setSendSms(req.getSendSms());
            appendOne(userId, single);
        }
    }

    private void validateLine(String executionId, List<Long> users) {
        if (executionId == null || executionId.trim().isEmpty() || users == null || users.isEmpty()
                || users.size() > 200 || users.contains(null) || new HashSet<>(users).size() != users.size()) {
            throw exception(TASK_APPEND_INVALID, "目标及追加人员不能为空、重复或超过200人");
        }
    }

    private void appendOne(Long userId, BpmTaskAppendReqVO req) {
        HistoricTaskInstance source = source(userId, req.getSourceTaskId());
        BpmTaskAppendTargetRespVO target = targets(userId, req.getSourceTaskId()).stream()
                .filter(item -> item.getExecutionId().equals(req.getTargetExecutionId())).findFirst()
                .orElseThrow(() -> exception(TASK_APPEND_INVALID, "目标状态已变化或不在原转出范围内，请刷新后重试"));
        Set<Long> requested = new LinkedHashSet<>(req.getUserIds());
        Set<Long> allowed = target.getCandidates().stream().filter(member -> !member.isDisabled()).map(BpmTaskAppendTargetRespVO.Member::getId).collect(Collectors.toSet());
        if (!allowed.containsAll(requested)) throw exception(TASK_APPEND_INVALID, "存在已加入、已完成或不在候选范围内的人员，请刷新后重试");
        if (target.isNewRound()) {
            createRound(source, target, requested, req.getSendSms());
        } else managementService.executeCommand(context -> {
            ExecutionEntity root = CommandContextUtil.getExecutionEntityManager().findById(req.getTargetExecutionId());
            if (root == null || !root.isMultiInstanceRoot() || root.isSuspended()
                    || !source.getProcessInstanceId().equals(root.getProcessInstanceId())
                    || !BpmDispatchSources.contains(root.getVariableLocal(BpmDispatchSources.SOURCES), req.getSourceTaskId())) {
                throw exception(TASK_APPEND_INVALID, "目标状态已变化，请刷新后重试");
            }
            // 与并发审批/追加竞争时，由引擎乐观锁使其中一个事务回滚，避免计数被覆盖。
            root.forceUpdate();
            CommandContextUtil.getExecutionEntityManager().findById(root.getProcessInstanceId()).forceUpdate();
            String collectionKey = FlowableUtils.formatExecutionCollectionVariable(root.getActivityId());
            Set<Long> members = existingMembers(root.getId(), root.getActivityId());
            if (!Collections.disjoint(members, requested)) throw exception(TASK_APPEND_INVALID, "人员已经加入该实例");
            members.addAll(requested);
            root.setVariableLocal(collectionKey, members);
            BpmTaskSmsPolicy.prepare(runtimeService, root.getId(), Collections.singletonMap(root.getActivityId(), requested), req.getSendSms());
            @SuppressWarnings("unchecked")
            Map<String, Set<String>> origins = new LinkedHashMap<>((Map<String, Set<String>>) root.getVariableLocal(BpmDispatchSources.SOURCES));
            requested.forEach(id -> origins.put(id.toString(), new LinkedHashSet<>(Collections.singleton(req.getSourceTaskId()))));
            root.setVariableLocal(BpmDispatchSources.SOURCES, origins);
            BpmDispatchSources.remember(root, source.getId(), root.getActivityId(), requested);
            int count = ((Number) root.getVariableLocal("nrOfInstances")).intValue();
            root.setVariableLocal("nrOfInstances", count + requested.size());
            if (!target.isSequential()) {
                // 直接锁定当前根执行流，避免原生 API 按 activityId 搜索时命中同名兄弟实例。
                for (Long id : requested) {
                    ExecutionEntity child = CommandContextUtil.getExecutionEntityManager().createChildExecution(root);
                    child.setCurrentFlowElement(root.getCurrentFlowElement());
                    child.setVariableLocal(FlowableUtils.formatExecutionCollectionElementVariable(root.getActivityId()), id);
                    CommandContextUtil.getAgenda(context).planContinueMultiInstanceOperation(child, root, count++);
                }
            }
            return null;
        });
        String names = adminUserApi.getUserList(requested).stream().map(user -> user.getNickname()).collect(Collectors.joining("、"));
        String operator = adminUserApi.getUser(userId).getNickname();
        String description = operator + "向【" + target.getNodeName() + "】追加办理人【" + names + "】"
                        + (target.isNewRound() ? "（另开一轮办理）" : target.isSequential() ? "（加入顺序办理队尾）" : "（生成待办）")
                        + "；发送短信：" + (!Boolean.FALSE.equals(req.getSendSms()) ? "是" : "否")
                        + (req.getReason() == null || req.getReason().trim().isEmpty() ? "" : "；原因：" + req.getReason());
        // 已办任务不再存在于运行表，标准 addComment 拒绝历史 taskId；通过引擎管理器保留准确的历史任务关联。
        managementService.executeCommand(context -> {
            org.flowable.engine.impl.persistence.entity.CommentEntityManager manager =
                    CommandContextUtil.getProcessEngineConfiguration(context).getCommentEntityManager();
            org.flowable.engine.impl.persistence.entity.CommentEntity comment = manager.create();
            comment.setTaskId(source.getId()); comment.setProcessInstanceId(source.getProcessInstanceId());
            comment.setUserId(userId.toString()); comment.setType("10"); comment.setAction("AddComment");
            comment.setTime(CommandContextUtil.getProcessEngineConfiguration(context).getClock().getCurrentTime());
            comment.setMessage(description.length() > 160 ? description.substring(0, 160) + "..." : description);
            comment.setFullMessage(description); manager.insert(comment); return null;
        });
    }
    private void createRound(HistoricTaskInstance source, BpmTaskAppendTargetRespVO target,
                             Set<Long> requested, Boolean sendSms) {
        managementService.executeCommand(context -> {
            org.flowable.engine.impl.persistence.entity.ExecutionEntityManager manager = CommandContextUtil.getExecutionEntityManager();
            ExecutionEntity process = manager.findById(source.getProcessInstanceId());
            if (process == null || process.isSuspended() || process.isEnded()) throw exception(TASK_APPEND_INVALID, "流程状态已变化");
            process.forceUpdate();
            if (!Collections.disjoint(dispatchedMembers(source, target.getNodeId()), requested)) {
                throw exception(TASK_APPEND_INVALID, "人员已经派发，请刷新后重试");
            }
            org.flowable.bpmn.model.BpmnModel model = repositoryService.getBpmnModel(source.getProcessDefinitionId());
            UserTask node = (UserTask) model.getFlowElement(target.getNodeId());
            ExecutionEntity branch = manager.createChildExecution(process);
            branch.setActive(false); branch.setScope(false);
            // Flowable 包容网关会检查每个执行流的 activityId。隔离容器停在结束节点，
            // 不能留空，也不能伪装成仍可到达汇合点的原节点；实际办理路径由其子执行流承担。
            org.flowable.bpmn.model.EndEvent end = BpmnModelUtils.getEndEvent(model);
            if (end == null) throw exception(TASK_APPEND_INVALID, "模型缺少结束节点，无法创建独立追加分支");
            branch.setCurrentFlowElement(end);
            branch.setVariableLocal(BpmDispatchSources.BRANCH, true);
            branch.setVariableLocal(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, new LinkedHashMap<>());
            branch.setVariableLocal(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME, new LinkedHashMap<>());
            // 仅遮蔽人工路由变量，不拷贝旧任务的业务表单、返回路径、短信或会签计数。
            java.util.regex.Pattern routeKey = java.util.regex.Pattern.compile("\\b(?:select_node_\\d+|next_node\\d+)\\b");
            for (org.flowable.bpmn.model.SequenceFlow flow : model.getMainProcess().findFlowElementsOfType(org.flowable.bpmn.model.SequenceFlow.class, true)) {
                if (flow.getConditionExpression() == null) continue;
                java.util.regex.Matcher matcher = routeKey.matcher(flow.getConditionExpression());
                while (matcher.find()) branch.setVariableLocal(matcher.group(), null);
            }
            branch.setVariableLocal(BpmDispatchSources.NEW_MEMBERS, Collections.singletonMap(node.getId(), requested));
            BpmTaskSmsPolicy.prepare(runtimeService, branch.getId(), Collections.singletonMap(node.getId(), requested), sendSms);
            BpmDispatchSources.prepare(runtimeService, branch.getId(), source.getId(), Collections.singletonMap(node.getId(), requested));
            BpmDispatchSources.remember(branch, source.getId(), node.getId(), requested);
            ExecutionEntity entry = manager.createChildExecution(branch);
            entry.setCurrentFlowElement(node);
            CommandContextUtil.getAgenda(context).planContinueProcessOperation(entry);
            return null;
        });
    }

}
