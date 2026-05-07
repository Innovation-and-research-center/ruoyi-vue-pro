package cn.iocoder.yudao.module.bpm.service.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.*;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.framework.common.util.object.PageUtils;
import cn.iocoder.yudao.framework.common.util.string.StrUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ConditionResult;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.*;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO.ActivityNodeTask;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.CandidateRule;
import cn.iocoder.yudao.module.bpm.convert.task.BpmProcessInstanceConvert;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.processInstance.BpmProcessInstanceUnifiedMapper;
import cn.iocoder.yudao.module.bpm.dal.redis.BpmProcessIdRedisDAO;
import cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmModelTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmSimpleModelNodeTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmReasonEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.event.BpmProcessInstanceEventPublisher;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmHttpRequestUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.SimpleModelUtils;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.iocoder.yudao.module.system.convert.user.UserConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.userdept.UserDeptService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO.ActivityNode;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.START_USER_NODE_ID;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils.parseNodeType;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.flowable.bpmn.constants.BpmnXMLConstants.*;

/**
 * 流程实例 Service 实现类
 * <p>
 * ProcessDefinition & ProcessInstance & Execution & Task 的关系：
 * 1. <a href="https://blog.csdn.net/bobozai86/article/details/105210414" />
 * <p>
 * HistoricProcessInstance & ProcessInstance 的关系：
 * 1. <a href=" https://my.oschina.net/843294669/blog/71902" />
 * <p>
 * 简单来说，前者 = 历史 + 运行中的流程实例，后者仅是运行中的流程实例
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class BpmProcessInstanceServiceImpl implements BpmProcessInstanceService {

    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;

    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    @Lazy // 避免循环依赖
    private BpmTaskService taskService;
    @Resource
    private BpmMessageService messageService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Resource
    private BpmProcessInstanceEventPublisher processInstanceEventPublisher;

    @Resource
    private BpmTaskCandidateInvoker taskCandidateInvoker;

    @Resource
    private BpmProcessIdRedisDAO processIdRedisDAO;

    @Resource
    private BpmUserGroupService userGroupService;

    @Resource
    private AdminUserService userService;

    @Resource
    private DeptService deptService;

    @Resource
    private BpmProcessInstanceUnifiedMapper unifiedMapper;

    @Resource
    private UserDeptService userDeptService;

    // ========== Query 查询相关方法 ==========

    @Override
    public ProcessInstance getProcessInstance(String id) {
        return runtimeService.createProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceId(id)
                .singleResult();
    }

    @Override
    public List<ProcessInstance> getProcessInstances(Set<String> ids) {
        return runtimeService.createProcessInstanceQuery().processInstanceIds(ids).includeProcessVariables().list();
    }

    @Override
    public HistoricProcessInstance getHistoricProcessInstance(String id) {
        return historyService.createHistoricProcessInstanceQuery().processInstanceId(id).includeProcessVariables()
                .singleResult();
    }

    @Override
    public List<HistoricProcessInstance> getHistoricProcessInstances(Set<String> ids) {
        return historyService.createHistoricProcessInstanceQuery().processInstanceIds(ids).includeProcessVariables()
                .list();
    }

    private Map<String, String> getFormFieldsPermission(BpmnModel bpmnModel,
                                                        String activityId, String taskId) {
        // 1. 获取流程活动编号。流程活动 Id 为空事，从流程任务中获取流程活动 Id
        if (StrUtil.isEmpty(activityId) && StrUtil.isNotEmpty(taskId)) {
            activityId = Optional.ofNullable(taskService.getHistoricTask(taskId))
                    .map(HistoricTaskInstance::getTaskDefinitionKey).orElse(null);
        }
        if (StrUtil.isEmpty(activityId)) {
            return null;
        }

        // 2. 从 BpmnModel 中解析表单字段权限
        return BpmnModelUtils.parseFormFieldsPermission(bpmnModel, activityId);
    }

    @Override
    public BpmApprovalDetailRespVO getApprovalDetail(Long loginUserId, BpmApprovalDetailReqVO reqVO) {
        // 1.1 从 reqVO 中，读取公共变量
        Long startUserId = loginUserId; // 流程发起人
        HistoricProcessInstance historicProcessInstance = null; // 流程实例
        Integer processInstanceStatus = BpmProcessInstanceStatusEnum.NOT_START.getStatus(); // 流程状态
        Map<String, Object> processVariables = new HashMap<>(); // 流程变量
        // 1.2 如果是流程已发起的场景，则使用流程实例的数据
        if (reqVO.getProcessInstanceId() != null) {
            historicProcessInstance = getHistoricProcessInstance(reqVO.getProcessInstanceId());
            if (historicProcessInstance == null) {
                throw exception(ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS);
            }
            startUserId = Long.valueOf(historicProcessInstance.getStartUserId());
            processInstanceStatus = FlowableUtils.getProcessInstanceStatus(historicProcessInstance);
            // 合并 DB 和前端传递的流量变量，以前端的为主
            if (CollUtil.isNotEmpty(historicProcessInstance.getProcessVariables())) {
                processVariables.putAll(historicProcessInstance.getProcessVariables());
            }
        }
        if (CollUtil.isNotEmpty(reqVO.getProcessVariables())) {
            processVariables.putAll(reqVO.getProcessVariables());
        }
        // 特殊：如果是未发起的场景，则设置发起用户，解决“发起流程”时，需要使用到该变量的问题。例如说：https://t.zsxq.com/fMw5g
        if (historicProcessInstance == null) {
            processVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, loginUserId);
        }
        // 1.3 读取其它相关数据
        ProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(
                historicProcessInstance != null ? historicProcessInstance.getProcessDefinitionId()
                        : reqVO.getProcessDefinitionId());
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService
                .getProcessDefinitionInfo(processDefinition.getId());
        BpmnModel bpmnModel = processDefinitionService.getProcessDefinitionBpmnModel(processDefinition.getId());

        // 2.1 已结束 + 进行中的活动节点
        List<ActivityNode> endActivityNodes = null; // 已结束的审批信息
        List<ActivityNode> runActivityNodes = null; // 进行中的审批信息
        List<HistoricActivityInstance> activities = null; // 流程实例列表
        if (reqVO.getProcessInstanceId() != null) {
            activities = taskService.getActivityListByProcessInstanceId(reqVO.getProcessInstanceId());
            List<HistoricTaskInstance> tasks = taskService.getTaskListByProcessInstanceId(reqVO.getProcessInstanceId(),
                    true);
            endActivityNodes = getEndActivityNodeList(startUserId, bpmnModel, processDefinitionInfo,
                    historicProcessInstance, processInstanceStatus, activities, tasks);
            runActivityNodes = getRunApproveNodeList(startUserId, bpmnModel, processDefinition, processVariables,
                    activities, tasks);
        }

        // 2.2 流程已经结束，直接 return，无需预测
        if (BpmProcessInstanceStatusEnum.isProcessEndStatus(processInstanceStatus)) {
            return buildApprovalDetail(reqVO, bpmnModel, processDefinition, processDefinitionInfo,
                    historicProcessInstance,
                    processInstanceStatus, endActivityNodes, runActivityNodes, null, null);
        }

        // 3.1 计算当前登录用户的待办任务
        BpmTaskRespVO todoTask = taskService.getTodoTask(loginUserId, reqVO.getTaskId(), reqVO.getProcessInstanceId());

        // 3.2 获取由于退回操作，需要预测的节点。从流程变量中获取，回退操作会设置这些变量
        Set<String> needSimulateTaskDefKeysByReturn = new HashSet<>();
        if (StrUtil.isNotEmpty(reqVO.getProcessInstanceId())) {
            Object needSimulateTaskIds = runtimeService.getVariable(reqVO.getProcessInstanceId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEED_SIMULATE_TASK_IDS);
            needSimulateTaskDefKeysByReturn.addAll(Convert.toSet(String.class, needSimulateTaskIds));
        }
        // 移除运行中的节点，运行中的节点无需预测
        if (CollUtil.isNotEmpty(runActivityNodes)) {
            runActivityNodes.forEach( activityNode -> needSimulateTaskDefKeysByReturn.remove(activityNode.getId()));
        }

        // 3.3 预测未运行节点的审批信息
        List<ActivityNode> simulateActivityNodes = getSimulateApproveNodeList(startUserId, bpmnModel,
                processDefinitionInfo,
                processVariables, activities, needSimulateTaskDefKeysByReturn);

        // 4. 拼接最终数据
        return buildApprovalDetail(reqVO, bpmnModel, processDefinition, processDefinitionInfo, historicProcessInstance,
                processInstanceStatus, endActivityNodes, runActivityNodes, simulateActivityNodes, todoTask);
    }

    @Override
    public List<ActivityNode> getNextApprovalNodes(Long loginUserId, BpmApprovalDetailReqVO reqVO) {
        // 1.1 校验任务存在，且是当前用户的
        Task task = taskService.validateTask(loginUserId, reqVO.getTaskId());
        // 1.2 校验流程实例存在
        ProcessInstance instance = getProcessInstance(task.getProcessInstanceId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstance(task.getProcessInstanceId());
        if (historicProcessInstance == null) {
            throw exception(ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS);
        }
        // 1.3 校验BpmnModel
        BpmnModel bpmnModel = processDefinitionService.getProcessDefinitionBpmnModel(task.getProcessDefinitionId());
        if (bpmnModel == null) {
            return null;
        }

        // 2. 设置流程变量
        Map<String, Object> processVariables = new HashMap<>();
        // 2.1 获取历史中流程变量
        if (CollUtil.isNotEmpty(historicProcessInstance.getProcessVariables())) {
            processVariables.putAll(historicProcessInstance.getProcessVariables());
        }
        // 2.2 合并前端传递的流程变量，以前端为准
        if (CollUtil.isNotEmpty(reqVO.getProcessVariables())) {
            processVariables.putAll(reqVO.getProcessVariables());
        }

        // 3. 获取下一个将要执行的节点集合
        FlowElement flowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        List<FlowNode> nextFlowNodes = BpmnModelUtils.getNextFlowNodes(flowElement, bpmnModel, processVariables);
        // 仅仅获取 UserTask 节点  TODO add from jason：如果网关节点和网关节点相连，获取下个 UserTask. 貌似有点不准。
        List<FlowNode> nextUserTaskList = CollectionUtils.filterList(nextFlowNodes, node -> node instanceof UserTask);
        List<ActivityNode> nextActivityNodes = convertList(nextUserTaskList, node -> new ActivityNode().setId(node.getId())
                .setName(node.getName()).setNodeType(BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType())
                .setStatus(BpmTaskStatusEnum.RUNNING.getStatus())
                .setCandidateStrategy(BpmnModelUtils.parseCandidateStrategy(node))
                .setCandidateUserIds(getTaskCandidateUserList(bpmnModel, node.getId(),
                        loginUserId, historicProcessInstance.getProcessDefinitionId(), processVariables)));
        if (CollUtil.isEmpty(nextActivityNodes)) {
            return nextActivityNodes;
        }

        // 4. 拼接基础信息
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSetByFlatMap(nextActivityNodes, ActivityNode::getCandidateUserIds, Collection::stream));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        nextActivityNodes.forEach(node -> node.setCandidateUsers(convertList(node.getCandidateUserIds(), userId -> {
            AdminUserRespDTO user = userMap.get(userId);
            if (user != null) {
                return BpmProcessInstanceConvert.INSTANCE.buildUser(userId, userMap, deptMap);
            }
            return null;
        })));
        return nextActivityNodes;
    }


    @Override
    public List<BpmNextTaskRespVO> getNextSelectNodes(Long loginUserId, BpmApprovalDetailReqVO reqVO) {
        // 1.1 从 reqVO 中，读取公共变量
        Long startUserId = loginUserId; // 流程发起人
        HistoricProcessInstance historicProcessInstance = null; // 流程实例
        Map<String, Object> processVariables = new HashMap<>(); // 流程变量

        // 1.2 如果是流程已发起的场景，则使用流程实例的数据
        if (reqVO.getProcessInstanceId() != null) {
            historicProcessInstance = getHistoricProcessInstance(reqVO.getProcessInstanceId());
            if (historicProcessInstance == null) {
                throw exception(ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS);
            }
            // 合并 DB 和前端传递的流量变量，以前端的为主
            if (CollUtil.isNotEmpty(historicProcessInstance.getProcessVariables())) {
                processVariables.putAll(historicProcessInstance.getProcessVariables());
            }
        }
        if (CollUtil.isNotEmpty(reqVO.getProcessVariables())) {
            processVariables.putAll(reqVO.getProcessVariables());
        }
        // 特殊：如果是未发起的场景，则设置发起用户，解决“发起流程”时，需要使用到该变量的问题。
        if (historicProcessInstance == null) {
            processVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, loginUserId);
        }

        ProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(
                historicProcessInstance != null ? historicProcessInstance.getProcessDefinitionId()
                        : reqVO.getProcessDefinitionId());
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService
                .getProcessDefinitionInfo(processDefinition.getId());
        BpmnModel bpmnModel = processDefinitionService.getProcessDefinitionBpmnModel(processDefinition.getId());
        if (bpmnModel == null) {
            throw exception(ErrorCodeConstants.MODEL_NOT_EXISTS);
        }

        FlowElement sourceElement = null;
        Task task = null; // 提取到外部声明，便于获取实例 ID

        if (reqVO.getTaskId() == null) {
            Process process = bpmnModel.getMainProcess();
            sourceElement = process.getFlowElements().stream()
                    .filter(e -> e instanceof StartEvent)
                    .findFirst().orElse(null);
        } else {
            // 1.1 校验任务存在，且是当前用户的
            task = taskService.validateTask(loginUserId, reqVO.getTaskId());
            // 1.2 校验流程实例存在
            ProcessInstance instance = getProcessInstance(task.getProcessInstanceId());
            if (instance == null) {
                throw exception(PROCESS_INSTANCE_NOT_EXISTS);
            }
            sourceElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        }

        if (!checkManualSelectProperty(sourceElement)) {
            return Collections.emptyList(); // 如果没开启手动选人，直接返回空
        }
        List<BpmNextTaskRespVO> result = new ArrayList<>();

        if (sourceElement instanceof UserTask) {
            // 解析当前节点的拓展属性
            Map<String, String> sourceProperties = parseAllProperties(sourceElement);
            // 判断是否开启了内循环标识
            if ("1".equals(sourceProperties.get("loop_flag"))) {
                BpmNextTaskRespVO loopNode = new BpmNextTaskRespVO();
                loopNode.setTaskDefKey(sourceElement.getId() + "_internal_loop");
                loopNode.setTaskName("部门内循环或同环节移交");
                loopNode.setFlowName("部门内循环或同环节移交");
                loopNode.setFlowSort(2);
                loopNode.setExtensionProperties(sourceProperties);
                result.add(loopNode);
            }
        }

        if (sourceElement instanceof FlowNode) {
            analyzeOutgoingFlows((FlowNode) sourceElement, result,null, processVariables);
//            if(reqVO.getTaskId() == null){
//                analyzeOutgoingFlows((FlowNode) sourceElement, result,null, processVariables);
//            }
//            else{
//                analyzeOutgoingFlows((FlowNode) sourceElement, result, null,null);
//            }

        }

        AdminUserDO loginUser = userService.getUser(loginUserId);
        Long currentDeptId = (loginUser != null) ? loginUser.getDeptId() : null;

        Set<Long> managedDeptIds = userDeptService.getUserDeptIds(loginUserId);
        if (managedDeptIds == null) {
            managedDeptIds = new HashSet<>();
        }

        Map<String, List<AdminUserDO>> nodeCandidateMap = new HashMap<>();
        Set<Long> deptIdsToQuery = new HashSet<>();

        // =========================================================================================
        // 【终极无死角方案：真实任务表(含候选人) + 历史变量表 双管齐下】
        // =========================================================================================
        Map<String, Object> selectedAssigneesMap = new HashMap<>();
        Set<String> targetTaskKeys = result.stream().map(BpmNextTaskRespVO::getTaskDefKey).collect(Collectors.toSet());

        if (task != null && task.getProcessInstanceId() != null) {
            String processInstanceId = task.getProcessInstanceId();

            // 途径 1：从真实任务表提取 (处理并行分支/会签已经生成的实际任务)
            List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .list();

            for (org.flowable.task.api.history.HistoricTaskInstance hiTask : historicTasks) {
                String taskKey = hiTask.getTaskDefinitionKey();

                if (targetTaskKeys.contains(taskKey)) {
                    List<Long> existingIds = selectedAssigneesMap.containsKey(taskKey) ?
                            Convert.toList(Long.class, selectedAssigneesMap.get(taskKey)) : new ArrayList<>();
                    boolean added = false;

                    // 1.1 提取直接派发/签收的处理人
                    if (StrUtil.isNotBlank(hiTask.getAssignee())) {
                        Long assigneeId = Convert.toLong(hiTask.getAssignee(), null);
                        if (assigneeId != null && !existingIds.contains(assigneeId)) {
                            existingIds.add(assigneeId);
                            added = true;
                        }
                    }

                    // 1.2 提取处于候选组/未签收状态的人员 (IdentityLink)
                    List<org.flowable.identitylink.api.history.HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(hiTask.getId());
                    for (org.flowable.identitylink.api.history.HistoricIdentityLink link : links) {
                        if (StrUtil.isNotBlank(link.getUserId())) {
                            Long candidateId = Convert.toLong(link.getUserId(), null);
                            if (candidateId != null && !existingIds.contains(candidateId)) {
                                existingIds.add(candidateId);
                                added = true;
                            }
                        }
                    }

                    if (added) {
                        selectedAssigneesMap.put(taskKey, existingIds);
                    }
                }
            }

            // 途径 2：从历史变量表提取 (处理预测出来的但还没走到、未生成真实任务的节点)
            List<org.flowable.variable.api.history.HistoricVariableInstance> varInstances = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES)
                    .list();

            for (org.flowable.variable.api.history.HistoricVariableInstance var : varInstances) {
                if (var.getValue() instanceof Map) {
                    Map<String, Object> mapValue = (Map<String, Object>) var.getValue();
                    for (Map.Entry<String, Object> entry : mapValue.entrySet()) {
                        String taskKey = entry.getKey();
                        if (!targetTaskKeys.contains(taskKey)) continue;

                        List<Long> userIds = Convert.toList(Long.class, entry.getValue());
                        if (CollUtil.isNotEmpty(userIds)) {
                            List<Long> existingIds = selectedAssigneesMap.containsKey(taskKey) ?
                                    Convert.toList(Long.class, selectedAssigneesMap.get(taskKey)) : new ArrayList<>();
                            for (Long id : userIds) {
                                if (!existingIds.contains(id)) existingIds.add(id);
                            }
                            selectedAssigneesMap.put(taskKey, existingIds);
                        }
                    }
                }
            }
        }

        // 途径 3：兜底从前端传入的流程变量获取
        if (selectedAssigneesMap.isEmpty()) {
            Object assigneesObj = processVariables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES);
            if (assigneesObj instanceof Map) {
                selectedAssigneesMap.putAll((Map<String, Object>) assigneesObj);
            }
        }
        // =========================================================================================

        Set<Long> allAssignedUserIdsToQuery = new HashSet<>();
        for (BpmNextTaskRespVO node : result) {
            // 解析候选人逻辑
            if (!node.getTaskDefKey().equals("end")) {
                CandidateRule rule = parseCandidateRule(node.getExtensionProperties());
                if (rule != null) {
                    List<AdminUserDO> users = getCandidateUsers(rule.getType(), rule.getValue());
                    if (CollUtil.isNotEmpty(users)) {
                        nodeCandidateMap.put(node.getTaskDefKey(), users);
                        users.forEach(u -> {
                            if (u.getDeptId() != null) deptIdsToQuery.add(u.getDeptId());
                        });
                    }
                }
            }

            // 收集已分配的人员 ID，准备统一查库
            if (selectedAssigneesMap.containsKey(node.getTaskDefKey())) {
                List<Long> assignedIds = Convert.toList(Long.class, selectedAssigneesMap.get(node.getTaskDefKey()));
                if (CollUtil.isNotEmpty(assignedIds)) {
                    node.setAssignedUserIds(assignedIds);
                    allAssignedUserIdsToQuery.addAll(assignedIds);
                }
            }
        }

        // 统一查询所有的已分配用户详细信息 (消除 N+1)
        Map<Long, AdminUserDO> globalAssignedUserMap = new HashMap<>();
        if (CollUtil.isNotEmpty(allAssignedUserIdsToQuery)) {
            List<AdminUserDO> allAssignedUsers = userService.getUserList(allAssignedUserIdsToQuery);
            globalAssignedUserMap = CollectionUtils.convertMap(allAssignedUsers, AdminUserDO::getId);
            // 将这批人的部门 ID 加入待查询集合
            allAssignedUsers.forEach(u -> {
                if (u.getDeptId() != null) deptIdsToQuery.add(u.getDeptId());
            });
        }

        // 统一查询所有的部门信息
        Map<Long, DeptDO> deptMap = new HashMap<>();
        if (CollUtil.isNotEmpty(deptIdsToQuery)) {
            List<DeptDO> deptList = deptService.getDeptList(deptIdsToQuery);
            deptMap = CollectionUtils.convertMap(deptList, DeptDO::getId);
        }

        for (BpmNextTaskRespVO node : result) {
            // 组装候选人员树结构
            List<AdminUserDO> rawUsers = nodeCandidateMap.get(node.getTaskDefKey());
            if (CollUtil.isNotEmpty(rawUsers)) {
                Map<Long, List<AdminUserDO>> usersByDept = rawUsers.stream()
                        .collect(Collectors.groupingBy(u -> u.getDeptId() != null ? u.getDeptId() : -1L));

                List<BpmUserGroupRespVO> treeList = new ArrayList<>();
                for (Map.Entry<Long, List<AdminUserDO>> entry : usersByDept.entrySet()) {
                    Long deptId = entry.getKey();
                    List<AdminUserDO> deptUsers = entry.getValue();

                    BpmUserGroupRespVO group = new BpmUserGroupRespVO();
                    if (deptId == -1L) {
                        group.setId(-1L);
                        group.setName("未分配部门");
                    } else {
                        DeptDO dept = deptMap.get(deptId);
                        group.setId(deptId);
                        group.setName(dept != null ? dept.getName() : "未知部门");
                    }
                    group.setChildren(UserConvert.INSTANCE.convertSimpleList(deptUsers, null));
                    treeList.add(group);
                }

                Long finalCurrentDeptId = currentDeptId;
                if (finalCurrentDeptId != null) {
                    treeList.sort((d1, d2) -> {
                        boolean d1IsCurrent = Objects.equals(d1.getId(), finalCurrentDeptId);
                        boolean d2IsCurrent = Objects.equals(d2.getId(), finalCurrentDeptId);
                        if (d1IsCurrent && !d2IsCurrent) return -1;
                        if (!d1IsCurrent && d2IsCurrent) return 1;
                        return Long.compare(d1.getId(), d2.getId());
                    });
                }
                node.setCandidateUsers(treeList);
            }

            // 组装已设置的任务人员详细信息
            if (CollUtil.isNotEmpty(node.getAssignedUserIds())) {
                List<AdminUserDO> assignedUsersForNode = new ArrayList<>();
                for (Long uid : node.getAssignedUserIds()) {
                    AdminUserDO u = globalAssignedUserMap.get(uid);
                    if (u != null) {
                        assignedUsersForNode.add(u);
                    }
                }
                node.setAssignedUsers(UserConvert.INSTANCE.convertSimpleList(assignedUsersForNode, deptMap));
            }
        }

        return result;
    }
    @Override
    public BpmNextTaskRespVO getCurrentNode(Long loginUserId, BpmApprovalDetailReqVO reqVO) {

        HistoricProcessInstance historicProcessInstance = null; // 流程实例
        Map<String, Object> processVariables = new HashMap<>(); // 流程变量
        // 1.2 如果是流程已发起的场景，则使用流程实例的数据
        if (reqVO.getProcessInstanceId() != null) {
            historicProcessInstance = getHistoricProcessInstance(reqVO.getProcessInstanceId());
            if (historicProcessInstance == null) {
                throw exception(ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS);
            }
            if (CollUtil.isNotEmpty(historicProcessInstance.getProcessVariables())) {
                processVariables.putAll(historicProcessInstance.getProcessVariables());
            }
        }
        if (CollUtil.isNotEmpty(reqVO.getProcessVariables())) {
            processVariables.putAll(reqVO.getProcessVariables());
        }
        if (historicProcessInstance == null) {
            processVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, loginUserId);
        }
        ProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(
                historicProcessInstance != null ? historicProcessInstance.getProcessDefinitionId()
                        : reqVO.getProcessDefinitionId());
        BpmnModel bpmnModel = processDefinitionService.getProcessDefinitionBpmnModel(processDefinition.getId());
        if (bpmnModel == null) {
            throw exception(ErrorCodeConstants.MODEL_NOT_EXISTS);
        }

        Task task = taskService.validateTask(loginUserId, reqVO.getTaskId());
        // 1.2 校验流程实例存在
        ProcessInstance instance = getProcessInstance(task.getProcessInstanceId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }
        FlowElement sourceElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());;
        return buildTaskOption((UserTask) sourceElement, null,null);

    }

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<HistoricProcessInstance> getProcessInstancePage(Long userId,
                                                                      BpmProcessInstancePageReqVO pageReqVO) {
        // 1. 构建查询条件
        HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .processInstanceTenantId(FlowableUtils.getTenantId())
                .orderByProcessInstanceStartTime().desc();
        if (userId != null) { // 【我的流程】菜单时，需要传递该字段
            processInstanceQuery.startedBy(String.valueOf(userId));
        } else if (pageReqVO.getStartUserId() != null) { // 【管理流程】菜单时，才会传递该字段
            processInstanceQuery.startedBy(String.valueOf(pageReqVO.getStartUserId()));
        }
        if (StrUtil.isNotEmpty(pageReqVO.getName())) {
            processInstanceQuery.processInstanceNameLike("%" + pageReqVO.getName() + "%");
        }
        if (StrUtil.isNotEmpty(pageReqVO.getProcessDefinitionKey())) {
            processInstanceQuery.processDefinitionKey(pageReqVO.getProcessDefinitionKey());
        }
        if (StrUtil.isNotEmpty(pageReqVO.getCategory())) {
            processInstanceQuery.processDefinitionCategory(pageReqVO.getCategory());
        }
        if (pageReqVO.getStatus() != null) {
            processInstanceQuery.variableValueEquals(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                    pageReqVO.getStatus());
        }
        if (ArrayUtil.isNotEmpty(pageReqVO.getCreateTime())) {
            processInstanceQuery.startedAfter(DateUtils.of(pageReqVO.getCreateTime()[0]));
            processInstanceQuery.startedBefore(DateUtils.of(pageReqVO.getCreateTime()[1]));
        }
        if (ArrayUtil.isNotEmpty(pageReqVO.getEndTime())) {
            processInstanceQuery.finishedAfter(DateUtils.of(pageReqVO.getEndTime()[0]));
            processInstanceQuery.finishedBefore(DateUtils.of(pageReqVO.getEndTime()[1]));
        }
        // 表单字段查询
        Map<String, Object> formFieldsParams = JsonUtils.parseObject(pageReqVO.getFormFieldsParams(), Map.class);
        if (CollUtil.isNotEmpty(formFieldsParams)) {
            formFieldsParams.forEach((key, value) -> {
                if (StrUtil.isEmpty(String.valueOf(value))) {
                    return;
                }
                // TODO @lesan：应支持多种类型的查询方式，目前只有字符串全等
                processInstanceQuery.variableValueEquals(key, value);
            });
        }

        // 2.1 查询数量
        long processInstanceCount = processInstanceQuery.count();
        if (processInstanceCount == 0) {
            return PageResult.empty(processInstanceCount);
        }
        // 2.2 查询列表
        List<HistoricProcessInstance> processInstanceList = processInstanceQuery.listPage(PageUtils.getStart(pageReqVO),
                pageReqVO.getPageSize());
        return new PageResult<>(processInstanceList, processInstanceCount);
    }

    /**
     * 拼接审批详情的最终数据
     * <p>
     * 主要是，拼接审批人的用户信息、部门信息
     */
    private BpmApprovalDetailRespVO buildApprovalDetail(BpmApprovalDetailReqVO reqVO,
                                                        BpmnModel bpmnModel,
                                                        ProcessDefinition processDefinition,
                                                        BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                        HistoricProcessInstance processInstance,
                                                        Integer processInstanceStatus,
                                                        List<ActivityNode> endApprovalNodeInfos,
                                                        List<ActivityNode> runningApprovalNodeInfos,
                                                        List<ActivityNode> simulateApprovalNodeInfos,
                                                        BpmTaskRespVO todoTask) {
        // 1. 获取所有需要读取用户信息的 userIds
        List<ActivityNode> approveNodes = newArrayList(
                asList(endApprovalNodeInfos, runningApprovalNodeInfos, simulateApprovalNodeInfos));
        Set<Long> userIds = BpmProcessInstanceConvert.INSTANCE.parseUserIds(processInstance, approveNodes, todoTask);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));

        // 2. 表单权限
        String taskId = reqVO.getTaskId() == null && todoTask != null ? todoTask.getId() : reqVO.getTaskId();
        Map<String, String> formFieldsPermission = getFormFieldsPermission(bpmnModel, reqVO.getActivityId(), taskId);

        // 3. 拼接数据
        return BpmProcessInstanceConvert.INSTANCE.buildApprovalDetail(bpmnModel, processDefinition,
                processDefinitionInfo, processInstance,
                processInstanceStatus, approveNodes, todoTask, formFieldsPermission, userMap, deptMap);
    }

    private boolean checkManualSelectProperty(FlowElement element) {
        if (element == null) return false;

        Map<String, List<ExtensionElement>> extensions = element.getExtensionElements();

        // 1. 尝试直接获取 property (标准写法)
        if (hasTargetProperty(extensions.get("property"))) {
            return true;
        }

        // 2. 针对你提供的 XML：获取 properties 标签 (嵌套写法)
        // <flowable:properties> ... </flowable:properties>
        if (extensions.containsKey("properties")) {
            List<ExtensionElement> propertiesWrappers = extensions.get("properties");
            for (ExtensionElement wrapper : propertiesWrappers) {
                // 获取 wrapper 内部的子元素 <flowable:property>
                Map<String, List<ExtensionElement>> childExtensions = wrapper.getChildElements();
                if (hasTargetProperty(childExtensions.get("property"))) {
                    return true;
                }
            }
        }

        return false;
    }


    private boolean hasTargetProperty(List<ExtensionElement> propertyList) {
        if (propertyList == null || propertyList.isEmpty()) return false;

        for (ExtensionElement prop : propertyList) {
            String name = prop.getAttributeValue(null, "name");
            String value = prop.getAttributeValue(null, "value");
            if ("select_manually".equals(name) && "1".equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void analyzeOutgoingFlows(FlowNode source, List<BpmNextTaskRespVO> result, String incomingCondition, Map<String, Object> processVariables) {
        List<SequenceFlow> outgoingFlows = source.getOutgoingFlows();

        // ====================================================================
        // 第一步：寻找并筛选真正需要走的分支
        // ====================================================================
        List<SequenceFlow> flowsToTake = new ArrayList<>();
        SequenceFlow defaultFlow = null;
        String defaultFlowId = null;

        if (source instanceof ExclusiveGateway) {
            defaultFlowId = ((ExclusiveGateway) source).getDefaultFlow();
        } else if (source instanceof InclusiveGateway) {
            defaultFlowId = ((InclusiveGateway) source).getDefaultFlow();
        } else if (source instanceof Activity) {
            defaultFlowId = ((Activity) source).getDefaultFlow();
        }

        // 【修改点 1】：将 matchedAnyCondition 改为 matchedStrictCondition
        // 用来记录是否找到了“实打实满足条件”的连线
        boolean matchedStrictCondition = false;

        for (SequenceFlow flow : outgoingFlows) {
            if (defaultFlowId != null && defaultFlowId.equals(flow.getId())) {
                defaultFlow = flow;
                continue;
            }

            String conditionExpression = flow.getConditionExpression();
            boolean isStrictTrue = false;

            if (StringUtil.isNotEmpty(conditionExpression)) {
                Boolean evalResult = evaluateExpression(conditionExpression, processVariables);

                if (Boolean.FALSE.equals(evalResult)) {
                    // 只有明确算出 false 才拦截，跳过
                    continue;
                }

                if (Boolean.TRUE.equals(evalResult)) {
                    // 明确算出了 true
                    isStrictTrue = true;
                    matchedStrictCondition = true; // 【修改点 2】：标记找到了严格满足的条件
                }
            } else {
                // 连线上压根没配条件，视为严格满足
                isStrictTrue = true;
                matchedStrictCondition = true; // 【修改点 3】：无条件的线也算严格满足
            }

            // 宽容放行的线，或者是严格满足的线，都加进来
            flowsToTake.add(flow);

            // 如果是排他网关，并且是“严格满足条件”时，才立马终止筛选
            if (source instanceof ExclusiveGateway && isStrictTrue) {
                break;
            }
        }

        // 【修改点 4】：兜底逻辑判定更改
        // 如果没有找到任何【严格满足】的条件线（要么全 false，要么是因为缺变量全宽容放行了），且配置了默认线
        if (!matchedStrictCondition && defaultFlow != null) {
            flowsToTake.add(defaultFlow);
        }

        // ====================================================================
        // 第二步：对筛选出来的连线，执行你原有的节点解析逻辑
        // ====================================================================
        for (SequenceFlow flow : flowsToTake) {
            FlowElement target = flow.getTargetFlowElement();

            String currentCondition = StringUtil.isNotEmpty(flow.getConditionExpression())
                    ? flow.getConditionExpression()
                    : incomingCondition;

            if (target instanceof UserTask) {
                // 找到目标任务
                result.add(buildTaskOption((UserTask) target, currentCondition, flow));
            } else if (target instanceof Gateway) {
                // 遇到网关，递归穿透 (记得把 processVariables 传下去)
                analyzeOutgoingFlows((FlowNode) target, result, currentCondition, processVariables);
            } else if (target instanceof SubProcess) {
                // 3. 遇到子流程 (嵌入式子流程)
                SubProcess subProcess = (SubProcess) target;

                String parentConditionStr = flow.getConditionExpression();

                int beforeSize = result.size();
                // 获取子流程内部的所有元素
                Collection<FlowElement> subElements = subProcess.getFlowElements();

                // 遍历找到子流程内部的“开始事件”
                for (FlowElement subElement : subElements) {
                    if (subElement instanceof StartEvent) {
                        // 找到 StartEvent 后，将其视为普通的 FlowNode，递归调用本方法
                        // 这样就能顺着 StartEvent -> 线 -> 内部的 UserTask 找到了
                        analyzeOutgoingFlows((FlowNode) subElement, result, currentCondition, processVariables);
                    }
                }

                if (StringUtil.isNotEmpty(parentConditionStr)) {
                    // 解析父条件
                    ConditionResult parentCondition = extractConditionValue(parentConditionStr);

                    // 遍历本次递归新增的任务 (从 beforeSize 开始到当前 size)
                    for (int i = beforeSize; i < result.size(); i++) {
                        BpmNextTaskRespVO childTaskVO = result.get(i);

                        // 策略 A：直接覆盖（适用于内部 Start -> Task 之间通常没有连线条件的场景）
                        if (childTaskVO.getConditionExpression() == null) {
                            childTaskVO.setConditionExpression(parentCondition);
                        }
                        // 策略 B：合并条件（如果内部也有条件，则是 "外部条件 && 内部条件"）
                        else {
                            // ... 合并逻辑 ...
                        }
                    }
                }
            } else if (target instanceof EndEvent) {
                // 3. 找到结束事件：处理流程终点
                String targetName = StringUtil.isNotEmpty(target.getName()) ? target.getName() : "结束";
                BpmNextTaskRespVO endNodeVO = new BpmNextTaskRespVO()
                        .setTaskName(targetName)
                        .setTaskDefKey("end");

                // 提取指向结束节点的连线条件
                ConditionResult extractedValue = extractConditionValue(currentCondition);
                endNodeVO.setConditionExpression(extractedValue);

                result.add(endNodeVO);
            }
        }
    }
    private ConditionResult extractConditionValue(String conditionExpression) {
//        if (conditionExpression == null) return "default";
//        Matcher matcher = Pattern.compile("==\\s*[\"'](.*?)[\"']").matcher(conditionExpression);
//        return matcher.find() ? matcher.group(1) : "default";
        if (conditionExpression == null) {
            return null; // 或者返回一个默认对象
        }
        // 正则表达式解释：
        // variables:get\((.*?)\)  -> 捕获组1：匹配 get(...) 括号里面的内容 (即 Key)
        // \s*==\s* -> 匹配等号，允许周围有空格
        // ["'](.*?)["']          -> 捕获组2：匹配单引号或双引号里面的内容 (即 Value)
        String regex = "variables:get\\((.*?)\\)\\s*==\\s*[\"'](.*?)[\"']";
        Matcher matcher = Pattern.compile(regex).matcher(conditionExpression);
        if (matcher.find()) {
            String key = matcher.group(1).trim(); // 获取第一个括号捕获的内容
            String value = matcher.group(2).trim(); // 获取第二个括号捕获的内容
            return new ConditionResult(key, value);
        }
        return null; // 如果没匹配到，返回 null 或默认值

    }


    private BpmNextTaskRespVO buildTaskOption(UserTask userTask, String condition,SequenceFlow flow) {
        BpmNextTaskRespVO vo = new BpmNextTaskRespVO();
        vo.setTaskDefKey(userTask.getId());
        vo.setTaskName(userTask.getName());
        ConditionResult extractedValue = extractConditionValue(condition);
        vo.setConditionExpression(extractedValue);
        // 解析目标节点的拓展属性
        vo.setExtensionProperties(parseAllProperties(userTask));
        if (flow != null) {
            vo.setFlowName(flow.getName());
            Map<String, String> flowProperties = parseAllProperties(flow);
            if (flowProperties.containsKey("order") && StringUtil.isNotEmpty(flowProperties.get("order"))) {
                // 使用 Hutool 的 Convert 安全转换为 Integer
                vo.setFlowSort(Convert.toInt(flowProperties.get("order")));
            }
        }
        return vo;
    }

    private Map<String, String> parseAllProperties(FlowElement element) {
        Map<String, String> resultMap = new HashMap<>();
        Map<String, List<ExtensionElement>> extensions = element.getExtensionElements();

        // 提取器 lambda
        java.util.function.Consumer<List<ExtensionElement>> extract = (list) -> {
            if (list == null) return;
            for (ExtensionElement prop : list) {
                String name = prop.getAttributeValue(null, "name");
                String value = prop.getAttributeValue(null, "value");
                if (name != null) resultMap.put(name, value);
            }
        };

        // 1. 提取直接子节点
        extract.accept(extensions.get("property"));

        // 2. 提取嵌套在 properties 中的节点
        if (extensions.containsKey("properties")) {
            for (ExtensionElement wrapper : extensions.get("properties")) {
                extract.accept(wrapper.getChildElements().get("property"));
            }
        }
        return resultMap;
    }

    private String getExtensionAttribute(FlowElement element, String name) {
        Map<String, List<ExtensionElement>> extensions = element.getExtensionElements();
        // 具体解析逻辑同上...
        return null;
    }

    private void traverseGateway(Gateway gateway, List<BpmNextTaskRespVO> result) {
        List<SequenceFlow> flows = gateway.getOutgoingFlows();
        for (SequenceFlow flow : flows) {
            FlowElement target = flow.getTargetFlowElement();
            if (target instanceof UserTask) {
                // 网关出来的线通常带有条件
                result.add(buildTaskOption((UserTask) target, flow.getConditionExpression(),flow));
            } else if (target instanceof Gateway) {
                // 如果是连续网关，递归找
                traverseGateway((Gateway) target, result);
            }
        }
    }

    /**
     * 获得【已结束】的活动节点们
     */
    private List<ActivityNode> getEndActivityNodeList(Long startUserId, BpmnModel bpmnModel,
                                                      BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                      HistoricProcessInstance historicProcessInstance, Integer processInstanceStatus,
                                                      List<HistoricActivityInstance> activities, List<HistoricTaskInstance> tasks) {
        // 遍历 tasks 列表，只处理已结束的 UserTask
        // 为什么不通过 activities 呢？因为，加签场景下，它只存在于 tasks，没有 activities，导致如果遍历 activities 的话，它无法成为一个节点
        List<HistoricTaskInstance> endTasks = filterList(tasks, task -> task.getEndTime() != null);
        List<ActivityNode> approvalNodes = convertList(endTasks, task -> {
            FlowElement flowNode = BpmnModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
            ActivityNode activityNode = new ActivityNode().setId(task.getTaskDefinitionKey()).setName(task.getName())
                    .setNodeType(START_USER_NODE_ID.equals(task.getTaskDefinitionKey())
                            ? BpmSimpleModelNodeTypeEnum.START_USER_NODE.getType()
                            : ObjUtil.defaultIfNull(parseNodeType(flowNode), // 目的：解决“办理节点”的识别
                            BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType()))
                    .setStatus(getEndActivityNodeStatus(task))
                    .setCandidateStrategy(BpmnModelUtils.parseCandidateStrategy(flowNode))
                    .setStartTime(DateUtils.of(task.getCreateTime())).setEndTime(DateUtils.of(task.getEndTime()))
                    .setTasks(singletonList(BpmProcessInstanceConvert.INSTANCE.buildApprovalTaskInfo(task)));
            // 如果是取消状态，则跳过
            if (BpmTaskStatusEnum.isCancelStatus(activityNode.getStatus())) {
                return null;
            }
            return activityNode;
        });

        // 遍历 activities，只处理已结束的 StartEvent、EndEvent
        List<HistoricActivityInstance> endActivities = filterList(activities, activity -> activity.getEndTime() != null
                && (StrUtil.equalsAny(activity.getActivityType(), ELEMENT_EVENT_START, ELEMENT_CALL_ACTIVITY, ELEMENT_EVENT_END)));
        endActivities.forEach(activity -> {
            // StartEvent：只处理 BPMN 的场景。因为，SIMPLE 情况下，已经有 START_USER_NODE 节点
            if (ELEMENT_EVENT_START.equals(activity.getActivityType())
                    && BpmModelTypeEnum.BPMN.getType().equals(processDefinitionInfo.getModelType())
                    && !CollUtil.contains(activities, // 特殊：如果已经存在用户手动创建的 START_USER_NODE_ID 节点，则忽略 StartEvent
                    historicActivity -> historicActivity.getActivityId().equals(START_USER_NODE_ID))) {
                ActivityNodeTask startTask = new ActivityNodeTask().setId(BpmnModelConstants.START_USER_NODE_ID)
                        .setAssignee(startUserId).setStatus(BpmTaskStatusEnum.APPROVE.getStatus());
                ActivityNode startNode = new ActivityNode().setId(startTask.getId())
                        .setName(BpmSimpleModelNodeTypeEnum.START_USER_NODE.getName())
                        .setNodeType(BpmSimpleModelNodeTypeEnum.START_USER_NODE.getType())
                        .setStatus(startTask.getStatus()).setTasks(ListUtil.of(startTask))
                        .setStartTime(DateUtils.of(activity.getStartTime()))
                        .setEndTime(DateUtils.of(activity.getEndTime()));
                approvalNodes.add(0, startNode);
                return;
            }
            // EndEvent
            if (ELEMENT_EVENT_END.equals(activity.getActivityType())) {
                if (BpmProcessInstanceStatusEnum.isRejectStatus(processInstanceStatus)) {
                    // 拒绝情况下，不需要展示 EndEvent 结束节点。原因是：前端已经展示 x 效果，无需重复展示
                    return;
                }
                ActivityNode endNode = new ActivityNode().setId(activity.getId())
                        .setName(BpmSimpleModelNodeTypeEnum.END_NODE.getName())
                        .setNodeType(BpmSimpleModelNodeTypeEnum.END_NODE.getType()).setStatus(processInstanceStatus)
                        .setStartTime(DateUtils.of(activity.getStartTime()))
                        .setEndTime(DateUtils.of(activity.getEndTime()));
                String reason = FlowableUtils.getProcessInstanceReason(historicProcessInstance);
                if (StrUtil.isNotEmpty(reason)) {
                    endNode.setTasks(singletonList(new ActivityNodeTask().setId(endNode.getId())
                            .setStatus(endNode.getStatus()).setReason(reason)));
                }
                approvalNodes.add(endNode);
            }
            // CallActivity
            if (ELEMENT_CALL_ACTIVITY.equals(activity.getActivityType())) {
                ActivityNode callActivity = new ActivityNode().setId(activity.getId())
                        .setName(BpmSimpleModelNodeTypeEnum.CHILD_PROCESS.getName())
                        .setNodeType(BpmSimpleModelNodeTypeEnum.CHILD_PROCESS.getType()).setStatus(processInstanceStatus)
                        .setStartTime(DateUtils.of(activity.getStartTime()))
                        .setEndTime(DateUtils.of(activity.getEndTime()))
                        .setProcessInstanceId(activity.getCalledProcessInstanceId());
                approvalNodes.add(callActivity);
            }
        });

        // 按照时间排序
        approvalNodes.sort(Comparator.comparing(ActivityNode::getStartTime));
        return approvalNodes;
    }

    /**
     * 获取结束节点的状态
     */
    private Integer getEndActivityNodeStatus(HistoricTaskInstance task) {
        Integer status = FlowableUtils.getTaskStatus(task);
        if (status != null) {
            return status;
        }
        // 结束节点未获取到状态，为跳过状态。可见 bpmn 或者 simple 的 skipExpression
        return BpmTaskStatusEnum.SKIP.getStatus();
    }

    /**
     * 获得【进行中】的活动节点们
     */
    private List<ActivityNode> getRunApproveNodeList(Long startUserId,
                                                     BpmnModel bpmnModel,
                                                     ProcessDefinition processDefinition,
                                                     Map<String, Object> processVariables,
                                                     List<HistoricActivityInstance> activities,
                                                     List<HistoricTaskInstance> tasks) {
        // 构建运行中的任务、子流程，基于 activityId 分组
        List<HistoricActivityInstance> runActivities = filterList(activities, activity -> activity.getEndTime() == null
                && (StrUtil.equalsAny(activity.getActivityType(), ELEMENT_TASK_USER, ELEMENT_CALL_ACTIVITY)));
        Map<String, List<HistoricActivityInstance>> runningTaskMap = convertMultiMap(runActivities,
                HistoricActivityInstance::getActivityId);

        // 按照 activityId 分组，构建 ApprovalNodeInfo 节点
        Map<String, HistoricTaskInstance> taskMap = convertMap(tasks, HistoricTaskInstance::getId);
        return convertList(runningTaskMap.entrySet(), entry -> {
            String activityId = entry.getKey();
            List<HistoricActivityInstance> taskActivities = entry.getValue();
            // 构建活动节点
            FlowElement flowNode = BpmnModelUtils.getFlowElementById(bpmnModel, activityId);
            HistoricActivityInstance firstActivity = CollUtil.getFirst(taskActivities); // 取第一个任务，会签/或签的任务，开始时间相同
            ActivityNode activityNode = new ActivityNode().setId(firstActivity.getActivityId())
                    .setName(firstActivity.getActivityName())
                    .setNodeType(ObjUtil.defaultIfNull(parseNodeType(flowNode), // 目的：解决“办理节点”和"子流程"的识别
                            BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType()))
                    .setStatus(BpmTaskStatusEnum.RUNNING.getStatus())
                    .setCandidateStrategy(BpmnModelUtils.parseCandidateStrategy(flowNode))
                    .setStartTime(DateUtils.of(CollUtil.getFirst(taskActivities).getStartTime()))
                    .setTasks(new ArrayList<>());
            // 处理每个任务的 tasks 属性
            for (HistoricActivityInstance activity : taskActivities) {
                HistoricTaskInstance task = taskMap.get(activity.getTaskId());
                // 特殊情况：子流程节点 ChildProcess 仅存在于 activity 中，并且没有自身的 task，需要跳过执行
                // TODO @芋艿：后续看看怎么优化！
                if (task == null) {
                    continue;
                }
                activityNode.getTasks().add(BpmProcessInstanceConvert.INSTANCE.buildApprovalTaskInfo(task));
                // 加签子任务，需要过滤掉已经完成的加签子任务
                List<HistoricTaskInstance> childrenTasks = filterList(
                        taskService.getAllChildrenTaskListByParentTaskId(activity.getTaskId(), tasks),
                        childTask -> childTask.getEndTime() == null);
                if (CollUtil.isNotEmpty(childrenTasks)) {
                    activityNode.getTasks().addAll(
                            convertList(childrenTasks, BpmProcessInstanceConvert.INSTANCE::buildApprovalTaskInfo));
                }
            }
            // 处理每个任务的 candidateUsers 属性：如果是依次审批，需要预测它的后续审批人。因为 Task 是审批完一个，创建一个新的 Task
            if (BpmnModelUtils.isSequentialUserTask(flowNode)) {
                List<Long> candidateUserIds = getTaskCandidateUserList(bpmnModel, flowNode.getId(),
                        startUserId, processDefinition.getId(), processVariables);
                // 截取当前审批人位置后面的候选人，不包含当前审批人
                ActivityNodeTask approvalTaskInfo = CollUtil.getFirst(activityNode.getTasks());
                Assert.notNull(approvalTaskInfo, "任务不能为空");
                int index = CollUtil.indexOf(candidateUserIds,
                        userId -> ObjectUtils.equalsAny(userId, approvalTaskInfo.getOwner(),
                                approvalTaskInfo.getAssignee())); // 委派或者向前加签情况，需要先比较 owner
                activityNode.setCandidateUserIds(CollUtil.sub(candidateUserIds, index + 1, candidateUserIds.size()));
            }
            if (BpmSimpleModelNodeTypeEnum.CHILD_PROCESS.getType().equals(activityNode.getNodeType())) {
                activityNode.setProcessInstanceId(firstActivity.getCalledProcessInstanceId());
            }
            return activityNode;
        });
    }

    /**
     * 获得【预测（未来）】的活动节点们
     */
    private List<ActivityNode> getSimulateApproveNodeList(Long startUserId, BpmnModel bpmnModel,
                                                          BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                          Map<String, Object> processVariables,
                                                          List<HistoricActivityInstance> activities,
                                                          Set<String> needSimulateTaskDefKeysByReturn) {
        // TODO @芋艿：【可优化】在驳回场景下，未来的预测准确性不高。原因是，驳回后，HistoricActivityInstance
        // 包括了历史的操作，不是只有 startEvent 到当前节点的记录
        Set<String> runActivityIds = convertSet(activities, HistoricActivityInstance::getActivityId);
        // 逻辑：endTime 为 null 的 activity 代表当前正在停留的节点
        Set<String> currentActivityIds = convertSet(
                filterList(activities, activity -> activity.getEndTime() == null),
                HistoricActivityInstance::getActivityId
        );
        // 情况一：BPMN 设计器
        if (Objects.equals(BpmModelTypeEnum.BPMN.getType(), processDefinitionInfo.getModelType())) {
            List<FlowElement> flowElements = BpmnModelUtils.simulateProcess(bpmnModel, processVariables,currentActivityIds);
            return convertList(flowElements, flowElement -> buildNotRunApproveNodeForBpmn(
                    startUserId, bpmnModel, flowElements,
                    processDefinitionInfo, processVariables, flowElement, runActivityIds, needSimulateTaskDefKeysByReturn));
        }
        // 情况二：SIMPLE 设计器
        if (Objects.equals(BpmModelTypeEnum.SIMPLE.getType(), processDefinitionInfo.getModelType())) {
            BpmSimpleModelNodeVO simpleModel = JsonUtils.parseObject(processDefinitionInfo.getSimpleModel(),
                    BpmSimpleModelNodeVO.class);
            List<BpmSimpleModelNodeVO> simpleNodes = SimpleModelUtils.simulateProcess(simpleModel, processVariables);
            return convertList(simpleNodes, simpleNode -> buildNotRunApproveNodeForSimple(
                    startUserId, bpmnModel,
                    processDefinitionInfo, processVariables, simpleNode, runActivityIds, needSimulateTaskDefKeysByReturn));
        }
        throw new IllegalArgumentException("未知设计器类型：" + processDefinitionInfo.getModelType());
    }

    private ActivityNode buildNotRunApproveNodeForSimple(Long startUserId, BpmnModel bpmnModel,
                                                         BpmProcessDefinitionInfoDO processDefinitionInfo, Map<String, Object> processVariables,
                                                         BpmSimpleModelNodeVO node, Set<String> runActivityIds,
                                                         Set<String> needSimulateTaskDefKeysByReturn) {
        // TODO @芋艿：【可优化】在驳回场景下，未来的预测准确性不高。原因是，驳回后，HistoricActivityInstance
        // 包括了历史的操作，不是只有 startEvent 到当前节点的记录
        if (runActivityIds.contains(node.getId())
                && !needSimulateTaskDefKeysByReturn.contains(node.getId())) { // 特殊：回退操作时候，会记录需要预测的节点到流程变量中。即使在历史操作中，也需要预测
            return null;
        }
        Integer status = BpmTaskStatusEnum.NOT_START.getStatus();
        // 如果节点被跳过。设置状态为跳过
        if (SimpleModelUtils.isSkipNode(node, processVariables)) {
            status = BpmTaskStatusEnum.SKIP.getStatus();
        }
        ActivityNode activityNode = new ActivityNode().setId(node.getId()).setName(node.getName())
                .setNodeType(node.getType()).setCandidateStrategy(node.getCandidateStrategy())
                .setStatus(status);

        // 1. 开始节点/审批节点
        if (ObjectUtils.equalsAny(node.getType(),
                BpmSimpleModelNodeTypeEnum.START_USER_NODE.getType(),
                BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType(),
                BpmSimpleModelNodeTypeEnum.TRANSACTOR_NODE.getType())) {
            List<Long> candidateUserIds = getTaskCandidateUserList(bpmnModel, node.getId(),
                    startUserId, processDefinitionInfo.getProcessDefinitionId(), processVariables);
            activityNode.setCandidateUserIds(candidateUserIds);
            return activityNode;
        }

        // 2. 结束节点
        if (BpmSimpleModelNodeTypeEnum.END_NODE.getType().equals(node.getType())) {
            return activityNode;
        }

        // 3. 抄送节点
        if (CollUtil.isEmpty(runActivityIds) && // 流程发起时：需要展示抄送节点，用于选择抄送人
                BpmSimpleModelNodeTypeEnum.COPY_NODE.getType().equals(node.getType())) {
            List<Long> candidateUserIds = getTaskCandidateUserList(bpmnModel, node.getId(),
                    startUserId, processDefinitionInfo.getProcessDefinitionId(), processVariables);
            activityNode.setCandidateUserIds(candidateUserIds);
            return activityNode;
        }

        // 4. 子流程节点
        if (BpmSimpleModelNodeTypeEnum.CHILD_PROCESS.getType().equals(node.getType())) {
            return activityNode;
        }
        return null;
    }

    // 注意：返回值改成了大写的 Boolean
    private Boolean evaluateExpression(String expression, Map<String, Object> variables) {
        if (StringUtil.isEmpty(expression)) {
            return true;
        }

        try {
            // 1. 去除 Flowable 的包装
            String el = expression.replaceAll("^\\s*[\\$#]\\{(.*)\\}\\s*$", "$1");
            // 2. 擦除特殊函数
            el = el.replaceAll("variables:get\\(\\s*['\"]?([a-zA-Z0-9_]+)['\"]?\\s*\\)", "$1");
            el = el.replaceAll("execution\\.getVariable\\(\\s*['\"]?([a-zA-Z0-9_]+)['\"]?\\s*\\)", "$1");

            // 3. 变量为空时宽容放行 -> 返回 null
            if (variables == null || variables.isEmpty()) {
                return null; // 【修改点】代表未知/无法计算
            }

            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext(variables);

            // 使用自定义的 MapAccessor 防止缺 key 报错
            context.addPropertyAccessor(new MapAccessor() {
                @Override
                public boolean canRead(EvaluationContext context, Object target, String name) {
                    return true;
                }
            });
            variables.forEach(context::setVariable);

            Boolean result = parser.parseExpression(el).getValue(context, Boolean.class);
            return result;

        } catch (Exception e) {
            // 运算报错（如 null > 1000）导致异常时，宽容放行 -> 返回 null
            return null; // 【修改点】代表未知/无法计算
        }
    }
    private ActivityNode buildNotRunApproveNodeForBpmn(Long startUserId, BpmnModel bpmnModel, List<FlowElement> flowElements,
                                                       BpmProcessDefinitionInfoDO processDefinitionInfo,
                                                       Map<String, Object> processVariables,
                                                       FlowElement node, Set<String> runActivityIds,
                                                       Set<String> needSimulateTaskDefKeysByReturn) {
        // 回退操作时候，会记录需要预测的节点到流程变量中。即使节点在历史操作中，也需要预测。
        if (!needSimulateTaskDefKeysByReturn.contains(node.getId()) && runActivityIds.contains(node.getId())) {
            return null;
        }

        Integer status = BpmTaskStatusEnum.NOT_START.getStatus();
        // 如果节点被跳过，状态设置为跳过
        if (BpmnModelUtils.isSkipNode(node, processVariables)) {
            status = BpmTaskStatusEnum.SKIP.getStatus();
        }
        ActivityNode activityNode = new ActivityNode().setId(node.getId())
                .setStatus(status);

        // 1. 开始节点
        if (node instanceof StartEvent) {
            if (CollUtil.contains(flowElements, // 特殊：如果已经存在用户手动创建的 START_USER_NODE_ID 节点，则忽略 StartEvent
                    flowElement -> flowElement.getId().equals(START_USER_NODE_ID))) {
                return null;
            }
            return activityNode.setName(BpmSimpleModelNodeTypeEnum.START_USER_NODE.getName())
                    .setNodeType(BpmSimpleModelNodeTypeEnum.START_USER_NODE.getType());
        }

        // 2. 审批节点
        if (node instanceof UserTask) {
            List<Long> candidateUserIds = getTaskCandidateUserList(bpmnModel, node.getId(),
                    startUserId, processDefinitionInfo.getProcessDefinitionId(), processVariables);
            return activityNode.setName(node.getName()).setNodeType(BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType())
                    .setCandidateStrategy(BpmnModelUtils.parseCandidateStrategy(node))
                    .setCandidateUserIds(candidateUserIds);
        }

        // 3. 结束节点
        if (node instanceof EndEvent) {
            return activityNode.setName(BpmSimpleModelNodeTypeEnum.END_NODE.getName())
                    .setNodeType(BpmSimpleModelNodeTypeEnum.END_NODE.getType());
        }
        return null;
    }

    private List<Long> getTaskCandidateUserList(BpmnModel bpmnModel, String activityId,
                                                Long startUserId, String processDefinitionId, Map<String, Object> processVariables) {
        Set<Long> userIds = taskCandidateInvoker.calculateUsersByActivity(bpmnModel, activityId,
                startUserId, processDefinitionId, processVariables);
        return new ArrayList<>(userIds);
    }

    @Override
    public BpmProcessInstanceBpmnModelViewRespVO getProcessInstanceBpmnModelView(String id) {
        // 1.1 获得流程实例
        HistoricProcessInstance processInstance = getHistoricProcessInstance(id);
        if (processInstance == null) {
            return null;
        }
        // 1.2 获得流程定义
        BpmnModel bpmnModel = processDefinitionService
                .getProcessDefinitionBpmnModel(processInstance.getProcessDefinitionId());
        if (bpmnModel == null) {
            return null;
        }
        BpmSimpleModelNodeVO simpleModel = null;
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService.getProcessDefinitionInfo(
                processInstance.getProcessDefinitionId());
        if (processDefinitionInfo != null
                && BpmModelTypeEnum.SIMPLE.getType().equals(processDefinitionInfo.getModelType())) {
            simpleModel = JsonUtils.parseObject(processDefinitionInfo.getSimpleModel(), BpmSimpleModelNodeVO.class);
        }
        // 1.3 获得流程实例对应的活动实例列表 + 任务列表
        List<HistoricActivityInstance> activities = taskService.getActivityListByProcessInstanceId(id);
        List<HistoricTaskInstance> tasks = taskService.getTaskListByProcessInstanceId(id, true);

        // 2.1 拼接进度信息
        Set<String> unfinishedTaskActivityIds = convertSet(activities, HistoricActivityInstance::getActivityId,
                activityInstance -> activityInstance.getEndTime() == null);
        Set<String> finishedTaskActivityIds = convertSet(activities, HistoricActivityInstance::getActivityId,
                activityInstance -> activityInstance.getEndTime() != null
                        && ObjectUtil.notEqual(activityInstance.getActivityType(),
                        BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW));
        Set<String> finishedSequenceFlowActivityIds = convertSet(activities, HistoricActivityInstance::getActivityId,
                activityInstance -> activityInstance.getEndTime() != null
                        && ObjectUtil.equals(activityInstance.getActivityType(),
                        BpmnXMLConstants.ELEMENT_SEQUENCE_FLOW));
        // 特殊：会签情况下，会有部分已完成（审批）、部分未完成（待审批），此时需要 finishedTaskActivityIds 移除掉
        finishedTaskActivityIds.removeAll(unfinishedTaskActivityIds);
        // 特殊：如果流程实例被拒绝，则需要计算是哪个活动节点。
        // 注意，只取最后一个。因为会存在多次拒绝的情况，拒绝驳回到指定节点
        Set<String> rejectTaskActivityIds = CollUtil.newHashSet();
        if (BpmProcessInstanceStatusEnum.isRejectStatus(FlowableUtils.getProcessInstanceStatus(processInstance))) {
            tasks.stream()
                    .filter(task -> BpmTaskStatusEnum.isRejectStatus(FlowableUtils.getTaskStatus(task)))
                    .max(Comparator.comparing(HistoricTaskInstance::getEndTime))
                    .ifPresent(reject -> rejectTaskActivityIds.add(reject.getTaskDefinitionKey()));
            finishedTaskActivityIds.removeAll(rejectTaskActivityIds);
        }

        // 2.2 拼接基础信息
        Set<Long> userIds = BpmProcessInstanceConvert.INSTANCE.parseUserIds02(processInstance, tasks);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        return BpmProcessInstanceConvert.INSTANCE.buildProcessInstanceBpmnModelView(processInstance, tasks, bpmnModel,
                simpleModel,
                unfinishedTaskActivityIds, finishedTaskActivityIds, finishedSequenceFlowActivityIds,
                rejectTaskActivityIds,
                userMap, deptMap);
    }

    // ========== Update 写入相关方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqVO createReqVO) {
        // 获得流程定义
        ProcessDefinition definition = processDefinitionService
                .getProcessDefinition(createReqVO.getProcessDefinitionId());
        // 发起流程
        return createProcessInstance0(userId, definition, createReqVO.getVariables(), null,
                createReqVO.getStartUserSelectAssignees());
    }

    @Override
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public String createProcessInstance(Long userId, @Valid BpmProcessInstanceCreateReqDTO createReqDTO) {
        return FlowableUtils.executeAuthenticatedUserId(userId, () -> {
            // 获得流程定义
            ProcessDefinition definition = processDefinitionService
                    .getActiveProcessDefinition(createReqDTO.getProcessDefinitionKey());
            // 发起流程
            return createProcessInstance0(userId, definition, createReqDTO.getVariables(),
                    createReqDTO.getBusinessKey(),
                    createReqDTO.getStartUserSelectAssignees());
        });
    }

    private String createProcessInstance0(Long userId, ProcessDefinition definition,
                                          Map<String, Object> variables, String businessKey,
                                          Map<String, List<Long>> startUserSelectAssignees) {
        // 1.1 校验流程定义
        if (definition == null) {
            throw exception(PROCESS_DEFINITION_NOT_EXISTS);
        }
        if (definition.isSuspended()) {
            throw exception(PROCESS_DEFINITION_IS_SUSPENDED);
        }
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService
                .getProcessDefinitionInfo(definition.getId());
        if (processDefinitionInfo == null) {
            throw exception(PROCESS_DEFINITION_NOT_EXISTS);
        }
        // 1.2 校验是否能够发起
        if (!processDefinitionService.canUserStartProcessDefinition(processDefinitionInfo, userId)) {
            throw exception(PROCESS_INSTANCE_START_USER_CAN_START);
        }
        // 1.3 校验发起人自选审批人
        validateStartUserSelectAssignees(userId, definition, startUserSelectAssignees, variables);

        // 2. 创建流程实例
        if (variables == null) {
            variables = new HashMap<>();
        }
        FlowableUtils.filterProcessInstanceFormVariable(variables); // 过滤一下，避免 ProcessInstance 系统级的变量被占用
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, userId); // 设置流程变量，发起人 ID
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS, // 流程实例状态：审批中
                BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        variables.put(BpmnVariableConstants.PROCESS_INSTANCE_SKIP_EXPRESSION_ENABLED, true); // 跳过表达式需要添加此变量为 true，不影响没配置 skipExpression 的节点
        if (CollUtil.isNotEmpty(startUserSelectAssignees)) {
            // 设置流程变量，发起人自选审批人
            variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES,
                    startUserSelectAssignees);
        }

        // 3. 创建流程
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(definition.getId())
                .businessKey(businessKey)
                .variables(variables);
        // 3.1 创建流程 ID
        BpmModelMetaInfoVO.ProcessIdRule processIdRule = processDefinitionInfo.getProcessIdRule();
        if (processIdRule != null && Boolean.TRUE.equals(processIdRule.getEnable())) {
            processInstanceBuilder.predefineProcessInstanceId(processIdRedisDAO.generate(processIdRule));
        }
        // 3.2 流程名称
        processInstanceBuilder.name(generateProcessInstanceName(userId, definition, processDefinitionInfo, variables));
        // 3.3 发起流程实例
        ProcessInstance instance = processInstanceBuilder.start();
        return instance.getId();
    }

    private void validateStartUserSelectAssignees(Long userId, ProcessDefinition definition,
                                                  Map<String, List<Long>> startUserSelectAssignees,
                                                  Map<String, Object> variables) {
        // 1. 获取预测的节点信息
        BpmApprovalDetailRespVO detailRespVO = getApprovalDetail(userId, new BpmApprovalDetailReqVO()
                .setProcessDefinitionId(definition.getId())
                .setProcessVariables(variables));
        List<ActivityNode> activityNodes = detailRespVO.getActivityNodes();
        if (CollUtil.isEmpty(activityNodes)) {
            return;
        }

        // 2.1 移除掉不是发起人自选审批人节点
        activityNodes.removeIf(task ->
                ObjectUtil.notEqual(BpmTaskCandidateStrategyEnum.START_USER_SELECT.getStrategy(), task.getCandidateStrategy()));
        // 2.2 流程发起时要先获取当前流程的预测走向节点，发起时只校验预测的节点发起人自选审批人的审批人和抄送人是否都配置了
        activityNodes.forEach(task -> {
            List<Long> assignees = startUserSelectAssignees != null ? startUserSelectAssignees.get(task.getId()) : null;
            if (CollUtil.isEmpty(assignees)) {
                throw exception(PROCESS_INSTANCE_START_USER_SELECT_ASSIGNEES_NOT_CONFIG, task.getName());
            }
            Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(assignees);
            assignees.forEach(assignee -> {
                if (userMap.get(assignee) == null) {
                    throw exception(PROCESS_INSTANCE_START_USER_SELECT_ASSIGNEES_NOT_EXISTS, task.getName(), assignee);
                }
            });
        });
    }

    private String generateProcessInstanceName(Long userId,
                                               ProcessDefinition definition,
                                               BpmProcessDefinitionInfoDO definitionInfo,
                                               Map<String, Object> variables) {
        if (definition == null || definitionInfo == null) {
            return null;
        }
        BpmModelMetaInfoVO.TitleSetting titleSetting = definitionInfo.getTitleSetting();
        if (titleSetting == null || !BooleanUtil.isTrue(titleSetting.getEnable())) {
            return definition.getName();
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        Map<String, Object> cloneVariables = new HashMap<>(variables);
        cloneVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, user.getNickname());
        cloneVariables.put(BpmnVariableConstants.PROCESS_START_TIME, DateUtil.now());
        cloneVariables.put(BpmnVariableConstants.PROCESS_DEFINITION_NAME, definition.getName().trim());
        return StrUtil.format(definitionInfo.getTitleSetting().getTitle(), cloneVariables);
    }

    @Override
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void cancelProcessInstanceByStartUser(Long userId, @Valid BpmProcessInstanceCancelReqVO cancelReqVO) {
        // 1.1 校验流程实例存在
        ProcessInstance instance = getProcessInstance(cancelReqVO.getId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_CANCEL_FAIL_NOT_EXISTS);
        }
        // 1.2 只能取消自己的
        if (!Objects.equals(instance.getStartUserId(), String.valueOf(userId))) {
            throw exception(PROCESS_INSTANCE_CANCEL_FAIL_NOT_SELF);
        }
        // 1.3 校验允许撤销审批中的申请
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService
                .getProcessDefinitionInfo(instance.getProcessDefinitionId());
        Assert.notNull(processDefinitionInfo, "流程定义({})不存在", processDefinitionInfo);
        if (processDefinitionInfo.getAllowCancelRunningProcess() != null // 防止未配置 AllowCancelRunningProcess , 默认为可取消
                && BooleanUtil.isFalse(processDefinitionInfo.getAllowCancelRunningProcess())) {
            throw exception(PROCESS_INSTANCE_CANCEL_FAIL_NOT_ALLOW);
        }
        // 1.4 子流程不允许取消
        if (StrUtil.isNotBlank(instance.getSuperExecutionId())) {
            throw exception(PROCESS_INSTANCE_CANCEL_CHILD_FAIL_NOT_ALLOW);
        }
        // 1.5 判断后续节点是否已被审批过（只要存在一个非发起人节点，且状态为 APPROVE 的已完成任务，即视为已被审批过）
        List<HistoricTaskInstance> finishedTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(cancelReqVO.getId())
                .finished()
                .includeTaskLocalVariables() // 必须包含本地变量，以便获取 TASK_VARIABLE_STATUS
                .list();

        boolean hasApprovedNode = finishedTasks.stream()
                .filter(task -> !START_USER_NODE_ID.equals(task.getTaskDefinitionKey())) // 排除系统自动生成的发起人节点
                .anyMatch(task -> {
                    Integer status = (Integer) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
                    return BpmTaskStatusEnum.APPROVE.getStatus().equals(status);
                });

        if (hasApprovedNode) {
            // 提示：你需要在 ErrorCodeConstants.java 中新增此错误码，或者直接在这里抛出带中文的 RuntimeException
            throw exception(PROCESS_INSTANCE_CANCEL_FAIL_NEXT_NODE_APPROVED);
        }

        // 2. 取消流程
        updateProcessInstanceCancel(cancelReqVO.getId(),
                BpmReasonEnum.CANCEL_PROCESS_INSTANCE_BY_START_USER.format(cancelReqVO.getReason()));
    }

    @Override
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void cancelProcessInstanceByAdmin(Long userId, BpmProcessInstanceCancelReqVO cancelReqVO) {
        // 1.1 校验流程实例存在
        ProcessInstance instance = getProcessInstance(cancelReqVO.getId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_CANCEL_FAIL_NOT_EXISTS);
        }

        // 2. 取消流程
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        updateProcessInstanceCancel(cancelReqVO.getId(),
                BpmReasonEnum.CANCEL_PROCESS_INSTANCE_BY_ADMIN.format(user.getNickname(), cancelReqVO.getReason()));
    }

    private void updateProcessInstanceCancel(String id, String reason) {
        // 1. 更新流程实例 status
        runtimeService.setVariable(id, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.CANCEL.getStatus());
        runtimeService.setVariable(id, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_REASON, reason);

        // 2. 取消所有子流程
        List<ProcessInstance> childProcessInstances = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(id).list();
        childProcessInstances.forEach(processInstance -> updateProcessInstanceCancel(
                processInstance.getProcessInstanceId(), BpmReasonEnum.CANCEL_CHILD_PROCESS_INSTANCE_BY_MAIN_PROCESS.getReason()));

        // 3. 结束流程
        taskService.moveTaskToEnd(id, reason);
    }

    @Override
    public void updateProcessInstanceReject(ProcessInstance processInstance, String reason) {
        runtimeService.setVariable(processInstance.getProcessInstanceId(),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                BpmProcessInstanceStatusEnum.REJECT.getStatus());
        runtimeService.setVariable(processInstance.getProcessInstanceId(),
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_REASON,
                BpmReasonEnum.REJECT_TASK.format(reason));
    }

    @Override
    public void updateProcessInstanceVariables(String id, Map<String, Object> variables) {
        runtimeService.setVariables(id, variables);
    }

    @Override
    public void removeProcessInstanceVariables(String id, Collection<String> variableNames) {
        runtimeService.removeVariables(id, variableNames);
    }

    // ========== Event 事件相关方法 ==========

    @Override
    public void processProcessInstanceCompleted(ProcessInstance instance) {
        // 1.1 获取当前状态
        Integer status = (Integer) instance.getProcessVariables()
                .get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS);
        String reason = (String) instance.getProcessVariables()
                .get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_REASON);
        // 1.2 当流程状态还是审批状态中，说明审批通过了，则变更下它的状态
        // 为什么这么处理？因为流程完成，并且完成了，说明审批通过了
        if (Objects.equals(status, BpmProcessInstanceStatusEnum.RUNNING.getStatus())) {
            status = BpmProcessInstanceStatusEnum.APPROVE.getStatus();
            runtimeService.setVariable(instance.getId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_STATUS,
                    status);
        }

        // 1.3 如果子流程拒绝，设置其父流程也为拒绝状态，且结束父流程
        // 相关问题链接：https://t.zsxq.com/kZhyb
        if (Objects.equals(status, BpmProcessInstanceStatusEnum.REJECT.getStatus())
                && StrUtil.isNotBlank(instance.getSuperExecutionId())) {
            // 1.3.1 获取父流程实例 并标记为不通过
            Execution execution = runtimeService.createExecutionQuery().executionId(instance.getSuperExecutionId()).singleResult();
            ProcessInstance parentProcessInstance = getProcessInstance(execution.getProcessInstanceId());
            updateProcessInstanceReject(parentProcessInstance, BpmReasonEnum.REJECT_CHILD_PROCESS.getReason());

            // 1.3.2 结束父流程。需要在子流程结束事务提交后执行
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCompletion(int transactionStatus) {
                    // 回滚情况，直接返回
                    if (ObjectUtil.equal(transactionStatus, TransactionSynchronization.STATUS_ROLLED_BACK)) {
                        return;
                    }
                    taskService.moveTaskToEnd(parentProcessInstance.getId(), BpmReasonEnum.REJECT_CHILD_PROCESS.getReason());
                }
            });
        }

        // 2. 发送对应的消息通知
        if (Objects.equals(status, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            messageService.sendMessageWhenProcessInstanceApprove(
                    BpmProcessInstanceConvert.INSTANCE.buildProcessInstanceApproveMessage(instance));
        } else if (Objects.equals(status, BpmProcessInstanceStatusEnum.REJECT.getStatus())) {
            messageService.sendMessageWhenProcessInstanceReject(
                    BpmProcessInstanceConvert.INSTANCE.buildProcessInstanceRejectMessage(instance, reason));
        }

        // 3. 发送流程实例的状态事件
        processInstanceEventPublisher.sendProcessInstanceResultEvent(
                BpmProcessInstanceConvert.INSTANCE.buildProcessInstanceStatusEvent(this, instance, status, reason));

        // 4. 流程后置通知
        if (Objects.equals(status, BpmProcessInstanceStatusEnum.APPROVE.getStatus())) {
            BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService.
                    getProcessDefinitionInfo(instance.getProcessDefinitionId());
            if (ObjUtil.isNotNull(processDefinitionInfo) &&
                    ObjUtil.isNotNull(processDefinitionInfo.getProcessAfterTriggerSetting())) {
                BpmModelMetaInfoVO.HttpRequestSetting setting = processDefinitionInfo.getProcessAfterTriggerSetting();

                BpmHttpRequestUtils.executeBpmHttpRequest(instance,
                        setting.getUrl(), setting.getHeader(), setting.getBody(), true, setting.getResponse());
            }
        }
    }

    @Override
    public void processProcessInstanceCreated(ProcessInstance instance) {
        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService.
                getProcessDefinitionInfo(instance.getProcessDefinitionId());
        ProcessDefinition processDefinition = processDefinitionService.getProcessDefinition(instance.getProcessDefinitionId());
        if (processDefinition == null || processDefinitionInfo == null) {
            return;
        }

        // 自定义标题。目的：主要处理子流程的标题无法处理
        // 注意：必须使用 TransactionSynchronizationManager 事务提交后，否则不生效！！！
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                String name = generateProcessInstanceName(Long.valueOf(instance.getStartUserId()),
                        processDefinition, processDefinitionInfo, instance.getProcessVariables());
                if (ObjUtil.notEqual(instance.getName(), name)) {
                    runtimeService.setProcessInstanceName(instance.getProcessInstanceId(), name);
                }

                // 流程前置通知：需要在流程启动后(事务提交后)，保证 variables 已设置
                // 相关问题链接：https://t.zsxq.com/DF7Kq
                if (ObjUtil.isNull(processDefinitionInfo.getProcessBeforeTriggerSetting())) {
                    return;
                }
                BpmModelMetaInfoVO.HttpRequestSetting setting = processDefinitionInfo.getProcessBeforeTriggerSetting();
                BpmHttpRequestUtils.executeBpmHttpRequest(instance,
                        setting.getUrl(), setting.getHeader(), setting.getBody(), true, setting.getResponse());
            }

        });
    }


    private List<AdminUserDO> getCandidateUsers(String chooseRule, String ruleValue) {
        if (StrUtil.isEmpty(chooseRule) || StrUtil.isEmpty(ruleValue)) {
            return Collections.emptyList();
        }

        if ("role".equals(chooseRule)) {
            Set<Long> roleIds = StrUtils.splitToLongSet(ruleValue);
            return userService.getUserListByRoleIds(roleIds);
        } else if ("group".equals(chooseRule)) {
            Set<Long> groupIds = StrUtils.splitToLongSet(ruleValue);
            List<BpmUserGroupDO> groupList = userGroupService.getUserGroupList(groupIds);
            Set<Long> allUserIds = groupList.stream()
                    .map(BpmUserGroupDO::getUserIds)
                    .filter(Objects::nonNull)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            return userService.getUserList(allUserIds);
        }
        // 可以扩展 dept, post 等其他规则
        return Collections.emptyList();
    }

    private CandidateRule parseCandidateRule(Map<String,String> extensionProperties) {
        // 示例实现：假设存储在自定义属性中，你需要根据实际 BPMN XML 结构调整
         String strategy = extensionProperties.get("choose_rule");
         String param = extensionProperties.get("rule_value");
         return new CandidateRule(strategy, param);

        // 如果你的系统是基于 RuoYi-Vue-Pro 或类似框架，规则通常需要在 BpmTaskCandidateRule 表中查询
        // 或者是直接解析 userTask.getCandidateGroups() 如果里面存的是 JSON 配置
//        return null;
    }

    @Override
    @DataPermission(enable = false)
    public PageResult<BpmProcessInstanceUnifiedRespVO> getUnifiedProcessInstancePage(Long userId, BpmProcessInstanceUnifiedReqVO reqVO) {

        Long count = unifiedMapper.selectUnifiedCount(userId, reqVO);
        if (count == 0) {
            return PageResult.empty();
        }

        List<BpmProcessInstanceUnifiedRespVO> list = unifiedMapper.selectUnifiedList(userId, reqVO);

        return new PageResult<>(list, count);
    }


}
