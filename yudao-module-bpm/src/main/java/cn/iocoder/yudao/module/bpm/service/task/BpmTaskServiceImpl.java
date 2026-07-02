package cn.iocoder.yudao.module.bpm.service.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.*;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.framework.common.util.object.PageUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.convert.task.BpmTaskConvert;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmFormDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmTaskSortMapper;
import cn.iocoder.yudao.module.bpm.enums.definition.*;
import cn.iocoder.yudao.module.bpm.enums.task.BpmCommentTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmReasonEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskSignTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmHttpRequestUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.definition.BpmFormService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmModelService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.message.BpmMessageService;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskCreatedReqDTO;
import cn.iocoder.yudao.module.bpm.service.message.dto.BpmMessageSendWhenTaskTimeoutReqDTO;
import cn.iocoder.yudao.module.bpm.util.BpmQueryUtils;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnModelConstants.START_USER_NODE_ID;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils.*;

/**
 * 流程任务实例 Service 实现类
 *
 * @author 芋道源码
 * @author jason
 */
@Slf4j
@Service
public class BpmTaskServiceImpl implements BpmTaskService {

    /**
     * 退回批次上下文：key 为审批流出节点 id，value 为该节点本次流出的批次 id。
     * 该变量必须放在 execution local 里，避免并行分支之间互相覆盖。
     */
    private static final String RETURN_BRANCH_CONTEXT_VARIABLE = "RETURN_BRANCH_CONTEXT";

    /**
     * 当前任务真实可退回路径：按执行实例实际走过的用户任务 key 顺序保存。
     * 该变量只放在 execution local，避免退回选项被其它并行分支或历史轮次污染。
     */
    private static final String RETURNABLE_TASK_PATH_VARIABLE = "RETURNABLE_TASK_PATH";

    private static final ThreadLocal<ReturnBranchContextCarrier> RETURN_BRANCH_CONTEXT_CARRIER = new ThreadLocal<>();

    private static final ThreadLocal<ReturnableTaskPathCarrier> RETURNABLE_TASK_PATH_CARRIER = new ThreadLocal<>();

    private static class ReturnBranchContextCarrier {
        private final String processInstanceId;
        private final Map<String, String> context;

        private ReturnBranchContextCarrier(String processInstanceId, Map<String, String> context) {
            this.processInstanceId = processInstanceId;
            this.context = context;
        }
    }

    private static class ReturnableTaskPathCarrier {
        private final String processInstanceId;
        private final List<String> path;

        private ReturnableTaskPathCarrier(String processInstanceId, List<String> path) {
            this.processInstanceId = processInstanceId;
            this.path = path;
        }
    }

    @Resource
    private TaskService taskService;
    @Resource
    private HistoryService historyService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private ManagementService managementService;

    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmProcessDefinitionService bpmProcessDefinitionService;
    @Resource
    private BpmProcessInstanceCopyService processInstanceCopyService;
    @Resource
    private BpmModelService modelService;
    @Resource
    private BpmMessageService messageService;
    @Resource
    private BpmFormService formService;
    @Resource
    private BpmTaskSortMapper taskSortMapper;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;


    public static final int QUERY_TYPE_PREV = 1; // 查前置
    public static final int QUERY_TYPE_NEXT = 2; // 查后置

    // ========== Query 查询相关方法 ==========

    @Override
    public PageResult<Task> getTaskTodoPage(Long userId, BpmTaskPageReqVO pageVO) {
        if (isProcessSortField(pageVO.getOrderField())) {
            return getTaskTodoPageBySql(userId, pageVO);
        }
        TaskQuery taskQuery = taskService.createTaskQuery()
                .or()
                .taskAssignee(String.valueOf(userId)) // 分配给自己
                .taskCandidateUser(String.valueOf(userId)) // 收文登记等候选任务
                .endOr()
                .active()
                .includeProcessVariables();
        if (StrUtil.isNotBlank(pageVO.getName())) {
            taskQuery.taskNameLike("%" + pageVO.getName() + "%");
        }
        if (StrUtil.isNotEmpty(pageVO.getCategory())) {
            taskQuery.taskCategory(pageVO.getCategory());
        }
        if (StrUtil.isNotEmpty(pageVO.getProcessDefinitionKey())) {
            taskQuery.processDefinitionKey(pageVO.getProcessDefinitionKey());
        }
        if (ArrayUtil.isNotEmpty(pageVO.getCreateTime())) {
            taskQuery.taskCreatedAfter(DateUtils.of(pageVO.getCreateTime()[0]));
            taskQuery.taskCreatedBefore(DateUtils.of(pageVO.getCreateTime()[1]));
        }
        Set<String> candidateProcessInstanceIds = null;
        if (StrUtil.isNotBlank(pageVO.getProcessInstanceName())) {
            candidateProcessInstanceIds = getRuntimeProcessInstanceIdsByNameKeywords(pageVO.getProcessInstanceName());
            if (CollUtil.isEmpty(candidateProcessInstanceIds)) {
                // 如果连流程实例都搜不到，那肯定没有对应的任务，直接返回空
                return PageResult.empty();
            }
        }
        if (ArrayUtil.isNotEmpty(pageVO.getProcessDeadline())) {
            Date startTime = DateUtils.of(pageVO.getProcessDeadline()[0]);
            Date endTime = DateUtils.of(pageVO.getProcessDeadline()[1]);

            // 1. 使用 RuntimeService 的查询器，它支持变量范围查询
            // 注意：这里的方法名通常是 variableValueGreaterThanOrEqual (没有 process 前缀)
            List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                    .variableValueGreaterThanOrEqual(PROCESS_DEADLINE_DATE, startTime)
                    .variableValueLessThanOrEqual(PROCESS_DEADLINE_DATE, endTime)
                    .list();

            if (CollUtil.isEmpty(instances)) {
                return PageResult.empty(); // 没查到符合时间的流程，直接返回空
            }
            Set<String> deadlineIds = convertSet(instances, ProcessInstance::getId);
            if (candidateProcessInstanceIds == null) {
                // 如果之前没查过（即没有办件名称限制），直接使用时间查出的 ID
                candidateProcessInstanceIds = deadlineIds;
            } else {
                // 如果之前查过（即有办件名称限制），取交集
                candidateProcessInstanceIds.retainAll(deadlineIds);
                // 如果交集为空，说明没有同时满足两个条件的任务
                if (CollUtil.isEmpty(candidateProcessInstanceIds)) {
                    return PageResult.empty();
                }
            }

        }
        if (candidateProcessInstanceIds != null) {
            taskQuery.processInstanceIdIn(candidateProcessInstanceIds);
        }
        if (StrUtil.isNotBlank(pageVO.getProcessInstanceId())) {
            taskQuery.processInstanceId(pageVO.getProcessInstanceId());
        }
        if (ArrayUtil.isNotEmpty(pageVO.getDueDate())) {
            taskQuery.taskDueAfter(DateUtils.of(pageVO.getDueDate()[0]));
            taskQuery.taskDueBefore(DateUtils.of(pageVO.getDueDate()[1]));
        }
        if (pageVO.getUrgencyDegree() != null) {
            taskQuery.processVariableValueEquals(PROCESS_URGENCY_DEGREE, String.valueOf(pageVO.getUrgencyDegree()));
        }
        if (StrUtil.isNotBlank(pageVO.getSendingUnit())) {
            taskQuery.processVariableValueLikeIgnoreCase(PROCESS_SOURCE_UNIT, "%" + pageVO.getSendingUnit() + "%");
        }

        orderTodoTaskQuery(taskQuery, pageVO);

        long count = taskQuery.count();
        if (count == 0) {
            return PageResult.empty();
        }
        List<Task> tasks = taskQuery.listPage(PageUtils.getStart(pageVO), pageVO.getPageSize());
        return new PageResult<>(tasks, count);
    }

    private PageResult<Task> getTaskTodoPageBySql(Long userId, BpmTaskPageReqVO pageVO) {
        Long count = taskSortMapper.selectTodoTaskCount(userId, pageVO);
        if (count == null || count == 0) {
            return PageResult.empty();
        }
        List<String> taskIds = taskSortMapper.selectTodoTaskIds(userId, pageVO);
        if (CollUtil.isEmpty(taskIds)) {
            return new PageResult<>(Collections.emptyList(), count);
        }
        List<Task> tasks = taskIds.stream()
                .map(taskId -> taskService.createTaskQuery().taskId(taskId).includeProcessVariables().singleResult())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageResult<>(tasks, count);
    }

    @Override
    public BpmTaskRespVO getTodoTask(Long userId, String taskId, String processInstanceId) {
        // 1.1 获取指定的用户待办任务
        Task todoTask = getMyTodoTask(userId, taskId);
        // 1.2 获取不到，则获取该流程实例下，第一个用户的待办任务
//        if (todoTask == null) {
//            todoTask = getMyFirstTodoTask(userId, processInstanceId);
//        }
        if (todoTask == null) {
            return null;
        }

        // 2. 查询该任务的子任务
        List<Task> childrenTasks = getAllChildrenTaskListByParentTaskId(todoTask.getId(), CollUtil.newArrayList(todoTask));

        // 3. 转换返回
        BpmnModel bpmnModel = bpmProcessDefinitionService.getProcessDefinitionBpmnModel(todoTask.getProcessDefinitionId());
        Map<Integer, BpmTaskRespVO.OperationButtonSetting> buttonsSetting = BpmnModelUtils.parseButtonsSetting(
                bpmnModel, todoTask.getTaskDefinitionKey());
        Boolean signEnable = parseSignEnable(bpmnModel, todoTask.getTaskDefinitionKey());
        Boolean reasonRequire = parseReasonRequire(bpmnModel, todoTask.getTaskDefinitionKey());
        Integer nodeType = parseNodeType(BpmnModelUtils.getFlowElementById(bpmnModel, todoTask.getTaskDefinitionKey()));

        // 4. 任务表单
        BpmFormDO taskForm = null;
        if (StrUtil.isNotBlank(todoTask.getFormKey())) {
            taskForm = formService.getForm(NumberUtils.parseLong(todoTask.getFormKey()));
        }

        return BpmTaskConvert.INSTANCE.buildTodoTask(todoTask, childrenTasks, buttonsSetting, taskForm)
                .setNodeType(nodeType).setSignEnable(signEnable).setReasonRequire(reasonRequire);
    }

    /**
     * 获得用户指定 taskId 任务编号的“待办”（未审批、且可审核）的任务
     *
     * @param userId 用户编号
     * @param taskId 任务编号
     * @return 任务
     */
    private Task getMyTodoTask(Long userId, String taskId) {
        if (StrUtil.isEmpty(taskId)) {
            return null;
        }
        Task task = getTask(taskId);
        if (task == null) {
            return null;
        }
        if (!isAssignUserTask(userId, task) && !isAddSignUserTask(userId, task)) {
            return null;
        }
        return task;
    }

    /**
     * 获得用户指定 processInstanceId 流程编号下的首个“待办”（未审批、且可审核）的任务
     *
     * @param userId            用户编号
     * @param processInstanceId 流程编号
     * @return 任务
     */
    private Task getMyFirstTodoTask(Long userId, String processInstanceId) {
        if (processInstanceId == null) {
            return null;
        }
        // 1. 查询所有任务
        List<Task> tasks = taskService.createTaskQuery()
                .active()
                .processInstanceId(processInstanceId)
                .includeTaskLocalVariables()
                .includeProcessVariables()
                .orderByTaskCreateTime().asc() // 按创建时间升序
                .list();

        // 2. 查询我的首个任务
        return CollUtil.findOne(tasks, task -> {
            return isAssignUserTask(userId, task) // 当前用户为审批人
                    || isAddSignUserTask(userId, task); // 当前用户为加签人（为了减签）
        });
    }

    @Override
    public PageResult<HistoricTaskInstance> getTaskDonePage(Long userId, BpmTaskPageReqVO pageVO) {
        if (isProcessSortField(pageVO.getOrderField())) {
            return getTaskDonePageBySql(userId, pageVO);
        }
        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery()
                .finished() // 已完成
                .taskAssignee(String.valueOf(userId)) // 分配给自己
                .includeTaskLocalVariables()
                .processVariableValueNotEquals(PROCESS_INSTANCE_VARIABLE_STATUS,
                        BpmProcessInstanceStatusEnum.INVALID.getStatus())
                .taskVariableValueNotEquals(BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.CANCEL.getStatus());
        if (StrUtil.isNotBlank(pageVO.getName())) {
            taskQuery.taskNameLike("%" + pageVO.getName() + "%");
        }
        if (pageVO.getStatus() != null) {
            taskQuery.taskVariableValueEquals(BpmnVariableConstants.TASK_VARIABLE_STATUS, pageVO.getStatus());
        }
        if (StrUtil.isNotEmpty(pageVO.getCategory())) {
            taskQuery.taskCategory(pageVO.getCategory());
        }
        if (StrUtil.isNotEmpty(pageVO.getProcessDefinitionKey())) {
            taskQuery.processDefinitionKey(pageVO.getProcessDefinitionKey());
        }
        if (ArrayUtil.isNotEmpty(pageVO.getCreateTime())) {
            taskQuery.taskCreatedAfter(DateUtils.of(pageVO.getCreateTime()[0]));
            taskQuery.taskCreatedBefore(DateUtils.of(pageVO.getCreateTime()[1]));
        }
        if (ArrayUtil.isNotEmpty(pageVO.getCreateTime())) {
            taskQuery.taskCreatedAfter(DateUtils.of(pageVO.getCreateTime()[0]));
            taskQuery.taskCreatedBefore(DateUtils.of(pageVO.getCreateTime()[1]));
        }
        Set<String> candidateProcessInstanceIds = null;

        // 2.1 办件名称过滤
        if (StrUtil.isNotBlank(pageVO.getProcessInstanceName())) {
            candidateProcessInstanceIds = getHistoricProcessInstanceIdsByNameKeywords(pageVO.getProcessInstanceName());
            if (CollUtil.isEmpty(candidateProcessInstanceIds)) {
                return PageResult.empty();
            }
        }

        // 2.2 办结时限过滤 (PROCESS_DEADLINE_DATE)
        if (ArrayUtil.isNotEmpty(pageVO.getProcessDeadline())) {
            Date startTime = DateUtils.of(pageVO.getProcessDeadline()[0]);
            Date endTime = DateUtils.of(pageVO.getProcessDeadline()[1]);

            List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                    .variableValueGreaterThanOrEqual(PROCESS_DEADLINE_DATE, startTime)
                    .variableValueLessThanOrEqual(PROCESS_DEADLINE_DATE, endTime)
                    .list();

            if (CollUtil.isEmpty(instances)) {
                return PageResult.empty();
            }
            Set<String> deadlineIds = convertSet(instances, HistoricProcessInstance::getId);
            if (candidateProcessInstanceIds == null) {
                candidateProcessInstanceIds = deadlineIds;
            } else {
                candidateProcessInstanceIds.retainAll(deadlineIds);
                if (CollUtil.isEmpty(candidateProcessInstanceIds)) {
                    return PageResult.empty();
                }
            }
        }
        // 2.4 办件编号
        if (StrUtil.isNotBlank(pageVO.getProcessInstanceId())) {
            taskQuery.processInstanceId(pageVO.getProcessInstanceId());
        }
        if (candidateProcessInstanceIds != null) {
            taskQuery.processInstanceIdIn(candidateProcessInstanceIds);
        }

        // 2.5 环节时限 (任务 DueDate)
        if (ArrayUtil.isNotEmpty(pageVO.getDueDate())) {
            taskQuery.taskDueAfter(DateUtils.of(pageVO.getDueDate()[0]));
            taskQuery.taskDueBefore(DateUtils.of(pageVO.getDueDate()[1]));
        }

        // 2.6 紧急程度 (流程变量)
        if (pageVO.getUrgencyDegree() != null) {
            taskQuery.processVariableValueEquals(PROCESS_URGENCY_DEGREE, String.valueOf(pageVO.getUrgencyDegree()));
        }

        // 2.7 来文单位 (流程变量)
        if (StrUtil.isNotBlank(pageVO.getSendingUnit())) {
            // 注意：HistoricTaskInstanceQuery 对于 processVariableValueLikeIgnoreCase 支持可能有限，
            // 如果此处报错，可能需要先查 ProcessInstance 再 filter ID，或者精确匹配。
            // 这里假设 flowable 历史查询支持该变量查询
            taskQuery.processVariableValueLikeIgnoreCase(PROCESS_SOURCE_UNIT, "%" + pageVO.getSendingUnit() + "%");
        }
        orderDoneTaskQuery(taskQuery, pageVO);
        // 执行查询
        long count = taskQuery.count();
        if (count == 0) {
            return PageResult.empty();
        }
        List<HistoricTaskInstance> tasks = taskQuery.listPage(PageUtils.getStart(pageVO), pageVO.getPageSize());

        // 特殊：强制移除自动完成的“发起人”节点
        // 补充说明：由于 taskQuery 无法方面的过滤，所以暂时通过内存过滤
        tasks.removeIf(task -> task.getTaskDefinitionKey().equals(START_USER_NODE_ID));
        // TODO @芋艿：https://t.zsxq.com/MNzqp 【flowable bug】：taskCreatedAfter、taskCreatedBefore 拼接的是 OR
        if (ArrayUtil.isNotEmpty(pageVO.getCreateTime())) {
            tasks.removeIf(task -> task.getCreateTime() == null
                    || task.getCreateTime().before(DateUtils.of(pageVO.getCreateTime()[0]))
                    || task.getCreateTime().after(DateUtils.of(pageVO.getCreateTime()[1])));
        }
        return new PageResult<>(tasks, count);
    }

    @Override
    public PageResult<BpmTaskRespVO> getUnifiedTaskDonePage(Long userId, BpmTaskPageReqVO pageVO) {
        Long count = taskSortMapper.selectUnifiedDoneTaskCount(userId, pageVO);
        if (count == null || count == 0) {
            return PageResult.empty();
        }
        List<BpmTaskRespVO> list = taskSortMapper.selectUnifiedDoneTaskList(userId, pageVO);
        return new PageResult<>(list, count);
    }

    private PageResult<HistoricTaskInstance> getTaskDonePageBySql(Long userId, BpmTaskPageReqVO pageVO) {
        Long count = taskSortMapper.selectDoneTaskCount(userId, pageVO);
        if (count == null || count == 0) {
            return PageResult.empty();
        }
        List<String> taskIds = taskSortMapper.selectDoneTaskIds(userId, pageVO);
        if (CollUtil.isEmpty(taskIds)) {
            return new PageResult<>(Collections.emptyList(), count);
        }
        List<HistoricTaskInstance> tasks = taskIds.stream()
                .map(taskId -> historyService.createHistoricTaskInstanceQuery().taskId(taskId).includeTaskLocalVariables().singleResult())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageResult<>(tasks, count);
    }

    private Set<String> getRuntimeProcessInstanceIdsByNameKeywords(String processInstanceName) {
        List<String> keywords = BpmQueryUtils.splitKeywords(processInstanceName);
        if (CollUtil.isEmpty(keywords)) {
            return null;
        }
        Set<String> result = null;
        for (String keyword : keywords) {
            List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery()
                    .processInstanceNameLike("%" + keyword + "%")
                    .list();
            if (CollUtil.isEmpty(processInstances)) {
                return Collections.emptySet();
            }
            Set<String> ids = convertSet(processInstances, ProcessInstance::getId);
            if (result == null) {
                result = new HashSet<>(ids);
            } else {
                result.retainAll(ids);
                if (CollUtil.isEmpty(result)) {
                    return Collections.emptySet();
                }
            }
        }
        return result;
    }

    private Set<String> getHistoricProcessInstanceIdsByNameKeywords(String processInstanceName) {
        List<String> keywords = BpmQueryUtils.splitKeywords(processInstanceName);
        if (CollUtil.isEmpty(keywords)) {
            return null;
        }
        Set<String> result = null;
        for (String keyword : keywords) {
            List<HistoricProcessInstance> processInstances = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceNameLike("%" + keyword + "%")
                    .list();
            if (CollUtil.isEmpty(processInstances)) {
                return Collections.emptySet();
            }
            Set<String> ids = convertSet(processInstances, HistoricProcessInstance::getId);
            if (result == null) {
                result = new HashSet<>(ids);
            } else {
                result.retainAll(ids);
                if (CollUtil.isEmpty(result)) {
                    return Collections.emptySet();
                }
            }
        }
        return result;
    }

    private boolean isProcessSortField(String orderField) {
        return StrUtil.equalsAny(orderField,
                "processInstance.name", "processInstanceName",
                "urgencyDegree", "deadlineDate",
                "processInstance.createTime");
    }

    private void orderTodoTaskQuery(TaskQuery taskQuery, BpmTaskPageReqVO pageVO) {
        boolean asc = "asc".equalsIgnoreCase(pageVO.getOrderDirection());
        if (StrUtil.isBlank(pageVO.getOrderField()) || StrUtil.isBlank(pageVO.getOrderDirection())) {
            taskQuery.orderByTaskCreateTime().desc();
            return;
        }
        switch (pageVO.getOrderField()) {
            case "name":
                taskQuery.orderByTaskName();
                break;
            case "createTime":
                taskQuery.orderByTaskCreateTime();
                break;
            case "dueDate":
                taskQuery.orderByTaskDueDate();
                break;
            case "processInstanceId":
                taskQuery.orderByProcessInstanceId();
                break;
            default:
                taskQuery.orderByTaskCreateTime().desc();
                return;
        }
        if (asc) {
            taskQuery.asc();
        } else {
            taskQuery.desc();
        }
    }

    private void orderDoneTaskQuery(HistoricTaskInstanceQuery taskQuery, BpmTaskPageReqVO pageVO) {
        boolean asc = "asc".equalsIgnoreCase(pageVO.getOrderDirection());
        if (StrUtil.isBlank(pageVO.getOrderField()) || StrUtil.isBlank(pageVO.getOrderDirection())) {
            taskQuery.orderByHistoricTaskInstanceEndTime().desc();
            return;
        }
        switch (pageVO.getOrderField()) {
            case "name":
                taskQuery.orderByTaskName();
                break;
            case "createTime":
                taskQuery.orderByHistoricTaskInstanceStartTime();
                break;
            case "endTime":
                taskQuery.orderByHistoricTaskInstanceEndTime();
                break;
            case "dueDate":
                taskQuery.orderByTaskDueDate();
                break;
            case "processInstanceId":
                taskQuery.orderByProcessInstanceId();
                break;
            default:
                taskQuery.orderByHistoricTaskInstanceEndTime().desc();
                return;
        }
        if (asc) {
            taskQuery.asc();
        } else {
            taskQuery.desc();
        }
    }

    @Override
    public PageResult<HistoricTaskInstance> getTaskPage(Long userId, BpmTaskPageReqVO pageVO) {
        HistoricTaskInstanceQuery taskQuery = historyService.createHistoricTaskInstanceQuery()
                .includeTaskLocalVariables()
                .taskTenantId(FlowableUtils.getTenantId())
                .orderByHistoricTaskInstanceEndTime().desc(); // 审批时间倒序
        if (StrUtil.isNotBlank(pageVO.getName())) {
            taskQuery.taskNameLike("%" + pageVO.getName() + "%");
        }
        if (StrUtil.isNotEmpty(pageVO.getCategory())) {
            taskQuery.taskCategory(pageVO.getCategory());
        }
//        if (ArrayUtil.isNotEmpty(pageVO.getCreateTime())) {
//            taskQuery.taskCreatedAfter(DateUtils.of(pageVO.getCreateTime()[0]));
//            taskQuery.taskCreatedBefore(DateUtils.of(pageVO.getCreateTime()[1]));
//        }
        // 执行查询
        long count = taskQuery.count();
        if (count == 0) {
            return PageResult.empty();
        }
        List<HistoricTaskInstance> tasks = taskQuery.listPage(PageUtils.getStart(pageVO), pageVO.getPageSize());
        // TODO @芋艿：https://t.zsxq.com/MNzqp 【flowable bug】：taskCreatedAfter、taskCreatedBefore 拼接的是 OR
        if (ArrayUtil.isNotEmpty(pageVO.getCreateTime())) {
            tasks.removeIf(task -> task.getCreateTime() == null
                    || task.getCreateTime().before(DateUtils.of(pageVO.getCreateTime()[0]))
                    || task.getCreateTime().after(DateUtils.of(pageVO.getCreateTime()[1])));
        }
        return new PageResult<>(tasks, count);
    }

    @Override
    public List<Task> getTasksByProcessInstanceIds(List<String> processInstanceIds) {
        if (CollUtil.isEmpty(processInstanceIds)) {
            return Collections.emptyList();
        }
        return taskService.createTaskQuery().processInstanceIdIn(processInstanceIds).list();
    }

    @Override
    public List<HistoricTaskInstance> getTaskListByProcessInstanceId(String processInstanceId, Boolean asc) {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .includeTaskLocalVariables()
                .processInstanceId(processInstanceId);
        if (Boolean.TRUE.equals(asc)) {
            query.orderByHistoricTaskInstanceStartTime().asc();
        } else {
            query.orderByHistoricTaskInstanceStartTime().desc();
        }
        return query.list();
    }

    @Override
    public Task validateTask(Long userId, String taskId) {
        Task task = validateTaskExist(taskId);
        // 为什么判断 assignee 非空的情况下？
        // 例如说：在审批人为空时，我们会有“自动审批通过”的策略，此时 userId 为 null，允许通过
        if (StrUtil.isNotBlank(task.getAssignee())
                && ObjectUtil.notEqual(userId, NumberUtils.parseLong(task.getAssignee()))) {
            throw exception(TASK_OPERATE_FAIL_ASSIGN_NOT_SELF);
        }
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(Long userId, String taskId) {
        Task task = validateTaskExist(taskId);
        String userIdStr = String.valueOf(userId);
        if (StrUtil.isNotBlank(task.getAssignee())) {
            if (StrUtil.equals(task.getAssignee(), userIdStr)) {
                return;
            }
            throw exception(TASK_CLAIM_FAIL_ASSIGNED);
        }
        Task candidateTask = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(userIdStr)
                .singleResult();
        if (candidateTask == null) {
            throw exception(TASK_CLAIM_FAIL_NOT_CANDIDATE);
        }
        taskService.claim(taskId, userIdStr);
    }

    private Task validateTaskExist(String id) {
        Task task = getTask(id);
        if (task == null) {
            throw exception(TASK_NOT_EXISTS);
        }
        return task;
    }

    @Override
    public Task getTask(String id) {
        return taskService.createTaskQuery().taskId(id).includeTaskLocalVariables().singleResult();
    }

    @Override
    public HistoricTaskInstance getHistoricTask(String id) {
        return historyService.createHistoricTaskInstanceQuery().taskId(id).includeTaskLocalVariables().singleResult();
    }

    @Override
    public List<HistoricTaskInstance> getHistoricTasks(Collection<String> taskIds) {
        return historyService.createHistoricTaskInstanceQuery().taskIds(taskIds).includeTaskLocalVariables().list();
    }

    @Override
    public List<Task> getRunningTaskListByProcessInstanceId(String processInstanceId, Boolean assigned, String defineKey) {
        Assert.notNull(processInstanceId, "processInstanceId 不能为空");
        TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(processInstanceId).active()
                .includeTaskLocalVariables();
        if (BooleanUtil.isTrue(assigned)) {
            taskQuery.taskAssigned();
        } else if (BooleanUtil.isFalse(assigned)) {
            taskQuery.taskUnassigned();
        }
        if (StrUtil.isNotEmpty(defineKey)) {
            taskQuery.taskDefinitionKey(defineKey);
        }
        return taskQuery.list();
    }

    @Override
    public List<UserTask> getUserTaskListByReturn(String id) {
        // 1.1 校验当前任务 task 存在
        Task task = validateTaskExist(id);
        // 1.2 根据流程定义获取流程模型信息
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        FlowElement source = BpmnModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        if (source == null) {
            throw exception(TASK_NOT_EXISTS);
        }

        // 2.1 查询该任务的前置任务节点的 key 集合
        List<UserTask> previousUserList = BpmnModelUtils.getPreviousUserTaskList(source, null, null);
        if (CollUtil.isEmpty(previousUserList)) {
            return Collections.emptyList();
        }
        previousUserList = previousUserList.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                UserTask::getId,
                                userTask -> userTask,
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ),
                        map -> new ArrayList<>(map.values())
                ));
        // 2.2 过滤：只有串行可到达的节点，才可以退回。类似非串行、子流程无法退回
        previousUserList.removeIf(userTask -> !BpmnModelUtils.isSequentialReachable(source, userTask, null));

        // 2.3 过滤：只能退回到已经处理过的节点（排除审批未经过的节点）。相关 issue：https://github.com/YunaiV/ruoyi-vue-pro/issues/982
        List<HistoricTaskInstance> finishedTasks = getFinishedTaskListByProcessInstanceIdWithoutCancel(task.getProcessInstanceId());
        Set<String> finishedTaskDefinitionKeys = convertSet(finishedTasks, HistoricTaskInstance::getTaskDefinitionKey);
        previousUserList.removeIf(userTask -> !finishedTaskDefinitionKeys.contains(userTask.getId()));

        // 新实例优先按真实运行路径过滤，避免历史轮次或兄弟分支节点混入退回选项。
        List<String> returnableTaskPath = getReturnableTaskPath(task.getProcessInstanceId(), task.getExecutionId());
        if (CollUtil.isNotEmpty(returnableTaskPath)) {
            Set<String> returnableTaskKeys = buildReturnableTaskKeySet(bpmnModel, returnableTaskPath, finishedTaskDefinitionKeys);
            previousUserList.removeIf(userTask -> !returnableTaskKeys.contains(userTask.getId()));
        }
        return previousUserList;
    }

    @Override
    public <T extends TaskInfo> List<T> getAllChildrenTaskListByParentTaskId(String parentTaskId, List<T> tasks) {
        if (CollUtil.isEmpty(tasks)) {
            return Collections.emptyList();
        }
        Map<String, List<T>> parentTaskMap = convertMultiMap(
                filterList(tasks, task -> StrUtil.isNotEmpty(task.getParentTaskId())), TaskInfo::getParentTaskId);
        if (CollUtil.isEmpty(parentTaskMap)) {
            return Collections.emptyList();
        }

        List<T> result = new ArrayList<>();
        // 1. 递归获取子级
        Stack<String> stack = new Stack<>();
        stack.push(parentTaskId);
        // 2. 递归遍历
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            if (stack.isEmpty()) {
                break;
            }
            // 2.1 获取子任务们
            String taskId = stack.pop();
            List<T> childTaskList = filterList(tasks, task -> StrUtil.equals(task.getParentTaskId(), taskId));
            // 2.2 如果非空，则添加到 stack 进一步递归
            if (CollUtil.isNotEmpty(childTaskList)) {
                stack.addAll(convertList(childTaskList, TaskInfo::getId));
                result.addAll(childTaskList);
            }
        }
        return result;
    }

    /**
     * 获得所有子任务列表
     *
     * @param parentTask 父任务
     * @return 所有子任务列表
     */
    private List<Task> getAllChildTaskList(Task parentTask) {
        List<Task> result = new ArrayList<>();
        // 1. 递归获取子级
        Stack<Task> stack = new Stack<>();
        stack.push(parentTask);
        // 2. 递归遍历
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            if (stack.isEmpty()) {
                break;
            }
            // 2.1 获取子任务们
            Task task = stack.pop();
            List<Task> childTaskList = getTaskListByParentTaskId(task.getId());
            // 2.2 如果非空，则添加到 stack 进一步递归
            if (CollUtil.isNotEmpty(childTaskList)) {
                stack.addAll(childTaskList);
                result.addAll(childTaskList);
            }
        }
        return result;
    }

    @Override
    public List<Task> getTaskListByParentTaskId(String parentTaskId) {
        String tableName = managementService.getTableName(TaskEntity.class);
        // taskService.createTaskQuery() 没有 parentId 参数，所以写 sql 查询
        String sql = "select ID_,NAME_,OWNER_,ASSIGNEE_ from " + tableName + " where PARENT_TASK_ID_=#{parentTaskId}";
        return taskService.createNativeTaskQuery().sql(sql).parameter("parentTaskId", parentTaskId).list();
    }

    /**
     * 获取子任务个数
     *
     * @param parentTaskId 父任务 ID
     * @return 剩余子任务个数
     */
    private Long getTaskCountByParentTaskId(String parentTaskId) {
        String tableName = managementService.getTableName(TaskEntity.class);
        String sql = "SELECT COUNT(1) from " + tableName + " WHERE PARENT_TASK_ID_=#{parentTaskId}";
        return taskService.createNativeTaskQuery().sql(sql).parameter("parentTaskId", parentTaskId).count();
    }

    /**
     * 获得任务根任务的父任务编号
     *
     * @param task 任务
     * @return 根任务的父任务编号
     */
    private String getTaskRootParentId(Task task) {
        if (task == null || task.getParentTaskId() == null) {
            return null;
        }
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            Task parentTask = getTask(task.getParentTaskId());
            if (parentTask == null) {
                return null;
            }
            if (parentTask.getParentTaskId() == null) {
                return parentTask.getId();
            }
            task = parentTask;
        }
        throw new IllegalArgumentException(String.format("Task(%s) 层级过深，无法获取父节点编号", task.getId()));
    }

    @Override
    public List<HistoricActivityInstance> getActivityListByProcessInstanceId(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc().list();
    }

    @Override
    public List<HistoricActivityInstance> getHistoricActivityListByExecutionId(String executionId) {
        return historyService.createHistoricActivityInstanceQuery().executionId(executionId).list();
    }

    @Override
    public List<HistoricTaskInstance> getFinishedTaskListByProcessInstanceIdWithoutCancel(String processInstanceId) {
        return historyService.createHistoricTaskInstanceQuery()
                .finished()
                .includeTaskLocalVariables()
                .processInstanceId(processInstanceId)
                .taskVariableValueNotEquals(BpmnVariableConstants.TASK_VARIABLE_STATUS,
                        BpmTaskStatusEnum.CANCEL.getStatus())
                .orderByHistoricTaskInstanceStartTime().asc().list();
    }

    /**
     * 判断指定用户，是否是当前任务的审批人
     *
     * @param userId 用户编号
     * @param task   任务
     * @return 是否
     */
    private boolean isAssignUserTask(Long userId, Task task) {
        Long assignee = NumberUtil.parseLong(task.getAssignee(), null);
        return ObjectUtil.equals(userId, assignee);
    }

    /**
     * 判断指定用户，是否是当前任务的拥有人
     *
     * @param userId 用户编号
     * @param task   任务
     * @return 是否
     */
    private boolean isOwnerUserTask(Long userId, Task task) {
        Long assignee = NumberUtil.parseLong(task.getOwner(), null);
        return ObjectUtil.equal(userId, assignee);
    }

    /**
     * 判断指定用户，是否是当前任务的加签人
     *
     * @param userId 用户 Id
     * @param task   任务
     * @return 是否
     */
    private boolean isAddSignUserTask(Long userId, Task task) {
        return (isAssignUserTask(userId, task) || isOwnerUserTask(userId, task))
                && BpmTaskSignTypeEnum.of(task.getScopeType()) != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false)
    public void  addComment(Long userId, @Valid BpmTaskApproveReqVO reqVO){
        // 1.1 校验任务存在
        Task task = validateTask(userId, reqVO.getId());
        // 1.2 校验流程实例存在
        ProcessInstance instance = processInstanceService.getProcessInstance(task.getProcessInstanceId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        Boolean reasonRequire = parseReasonRequire(bpmnModel, task.getTaskDefinitionKey());
        if (reasonRequire && StrUtil.isEmpty(reqVO.getReason())) {
            throw exception(TASK_REASON_REQUIRE);
        }
        List<Comment> existingComments = taskService.getTaskComments(task.getId(), BpmCommentTypeEnum.COMMENT.getType());
        Comment targetComment = null;
        if (CollUtil.isNotEmpty(existingComments)) {
            for (Comment comment : existingComments) {
                // 注意：Flowable 存的 userId 是 String，传入的 userId 是 Long
                if (StrUtil.equals(comment.getUserId(), String.valueOf(userId))) {
                    targetComment = comment;
                    break;
                }
            }
        }
        if (targetComment != null) {
            taskService.deleteComment(targetComment.getId());
        }
        taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_VARIABLE_REASON, reqVO.getReason());
        taskService.addComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.COMMENT.getType(),
                BpmCommentTypeEnum.COMMENT.formatComment(reqVO.getReason()));
    }
    // ========== Update 写入相关方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long userId, @Valid BpmTaskApproveReqVO reqVO) {
        // 1.1 校验任务存在
        Task task = validateTask(userId, reqVO.getId());
        // 1.2 校验流程实例存在
        ProcessInstance instance = processInstanceService.getProcessInstance(task.getProcessInstanceId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }
        // 1.3 校验签名
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        Boolean signEnable = parseSignEnable(bpmnModel, task.getTaskDefinitionKey());
        if (signEnable && StrUtil.isEmpty(reqVO.getSignPicUrl())) {
            throw exception(TASK_SIGNATURE_NOT_EXISTS);
        }
        // 1.4 校验审批意见
        Boolean reasonRequire = parseReasonRequire(bpmnModel, task.getTaskDefinitionKey());
        if (reasonRequire && StrUtil.isEmpty(reqVO.getReason())) {
            throw exception(TASK_REASON_REQUIRE);
        }

        // =================================================================================
        // 【第 1 道防线】：严格互斥拦截！同环节加签 与 向下流转 绝不能同时进行！
        // =================================================================================
        if (CollUtil.isNotEmpty(reqVO.getAddSignUserIds())) {
            boolean hasNextRouting = StrUtil.isNotEmpty(reqVO.getNextNode())
                    || CollUtil.isNotEmpty(reqVO.getNextAssignees())
                    || CollUtil.isNotEmpty(reqVO.getNextNodeAssignees());

            if (hasNextRouting) {
                throw exception(TASK_WITHDRAW_FAIL_NEXT_TASK_NOT_DOUBLE);
            }
        }

        // 情况一：被委派的任务，不调用 complete 去完成任务
        if (DelegationState.PENDING.equals(task.getDelegationState())) {
            approveDelegateTask(reqVO, task);
            return;
        }

        // 情况二：审批有【后】加签的任务
        if (BpmTaskSignTypeEnum.AFTER.getType().equals(task.getScopeType())) {
            approveAfterSignTask(task, reqVO);
            return;
        }

        // 2.1 更新 task 状态、原因、签字
        updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.APPROVE.getStatus(), reqVO.getReason());
        if (signEnable) {
            taskService.setVariableLocal(task.getId(), BpmnVariableConstants.TASK_SIGN_PIC_URL, reqVO.getSignPicUrl());
        }
        // 2.2 添加评论
        taskService.addComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.APPROVE.getType(),
                BpmCommentTypeEnum.APPROVE.formatComment(reqVO.getReason()));

        // =================================================================================
        // 【第 2 道防线】：分离锁定 Execution 口袋 (完美解决连线报错 No outgoing sequence flow)
        // =================================================================================
        org.flowable.engine.runtime.Execution taskExecution = runtimeService.createExecutionQuery()
                .executionId(task.getExecutionId())
                .singleResult();

        // 1. 用于存【路由变量】的口袋（必须能活到走连线那一刻）
        String localVariableExecutionId = taskExecution.getId();
        // 2. 用于【动态加签】的会签根节点
        String miRootExecutionId = null;

        if (taskExecution.getParentId() != null) {
            org.flowable.engine.runtime.Execution parentExecution = runtimeService.createExecutionQuery()
                    .executionId(taskExecution.getParentId())
                    .singleResult();
            // 只有身上带 nrOfInstances 计数器的，才是真正的多实例会签树根！
            if (parentExecution != null && runtimeService.hasVariableLocal(parentExecution.getId(), "nrOfInstances")) {
                miRootExecutionId = parentExecution.getId(); // 锁定 MI Root 给下面的加签代码用

                // 【修复报错核心】：多实例办结时 MI Root 会被引擎物理销毁！
                // 路由变量必须存放在 MI Root 的上一级（进线分支 Execution），才能活到走连线评估那一刻！
                if (parentExecution.getParentId() != null) {
                    localVariableExecutionId = parentExecution.getParentId();
                } else {
                    localVariableExecutionId = parentExecution.getId(); // 极小概率兜底
                }
            }
        }

        // 3. 准备合并流程变量
        Map<String, Object> processVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(instance.getProcessVariables())) { // 获取历史中流程变量
            processVariables.putAll(instance.getProcessVariables());
        }
        if (CollUtil.isNotEmpty(reqVO.getVariables())) { // 合并前端传递的流程变量，以前端为准
            processVariables.putAll(reqVO.getVariables());
        }

        // =================================================================================
        // 【关键接力】：从安全的局部口袋中读取“历史选人名单”，喂给全局进行正确 Merge！
        // =================================================================================
        Object localAssigneeMap = runtimeService.getVariableLocal(localVariableExecutionId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
        if (localAssigneeMap != null) {
            processVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, localAssigneeMap);
        }

        // 更新下一节点变量
        if (StrUtil.isNotEmpty(reqVO.getNextNode())) {
            processVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, reqVO.getNextNode());
        }

        // 4. 校验并处理 APPROVE_USER_SELECT 当前审批人，选择下一节点审批人的逻辑
        Map<String, List<Long>> nextNodeAssignees = CollUtil.isNotEmpty(reqVO.getNextAssignees()) ? reqVO.getNextAssignees() : reqVO.getNextNodeAssignees();
        Map<String, Object> variables = validateAndSetNextAssignees(task.getTaskDefinitionKey(), processVariables,
                bpmnModel, nextNodeAssignees, instance, task.getId());

        // =================================================================================
        // 【第 3 道防线】：防穿透拦截！如果选的目标人均有任务，安静销毁冗余 Token
        // =================================================================================
        if (Boolean.TRUE.equals(variables.get("KILL_CURRENT_TOKEN_FLAG"))) {
            EndEvent endEvent = BpmnModelUtils.getEndEvent(bpmnModel);
            Assert.notNull(endEvent, "流程中未找到结束节点");

            String mergeReason = "系统检测到所选目标人员均正在办理该环节，为避免重复派发，本分支已自动结束。";
            taskService.addComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.APPROVE.getType(), mergeReason);
            updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.APPROVE.getStatus(), mergeReason);

            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(task.getProcessInstanceId())
                    .moveExecutionsToSingleActivityId(CollUtil.newArrayList(task.getExecutionId()), endEvent.getId())
                    .changeState();
            return; // 【必须 return】
        }

        // =================================================================================
        // 【第 4 道防线】：终极物理隔离！剥离【路由条件】与【人员名单】，严防并发全局污染！
        // =================================================================================
        Map<String, Object> localIsolatedVariables = new HashMap<>();

        // 4.1 剥离选人变量
        String assigneeVarKey = BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES;
        if (variables.containsKey(assigneeVarKey)) {
            localIsolatedVariables.put(assigneeVarKey, variables.get(assigneeVarKey));
            variables.remove(assigneeVarKey); // 从将要全局保存的 Map 中彻底剔除
            runtimeService.removeVariable(task.getProcessInstanceId(), assigneeVarKey); // 物理删除全局数据库残留
        }

        // 4.2 剥离当前节点的出线路由条件 (如 select_node_xx)
        org.flowable.bpmn.model.FlowElement currentFlowElement = bpmnModel.getFlowElement(task.getTaskDefinitionKey());
        if (currentFlowElement instanceof org.flowable.bpmn.model.FlowNode) {
            List<org.flowable.bpmn.model.SequenceFlow> outgoingFlows = ((org.flowable.bpmn.model.FlowNode) currentFlowElement).getOutgoingFlows();
            for (org.flowable.bpmn.model.SequenceFlow flow : outgoingFlows) {
                String condition = flow.getConditionExpression();
                if (cn.hutool.core.util.StrUtil.isNotBlank(condition)) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*").matcher(condition);
                    while (m.find()) {
                        String match = m.group();
                        if (!"variables".equals(match) && !"get".equals(match) && !"null".equals(match) && !"empty".equals(match)) {
                            // 【终极修复：物理抹杀共享口袋里的历史决策】严防并发遗留意图累加
                            runtimeService.removeVariable(task.getProcessInstanceId(), match); // 删全局残留
//                            if (runtimeService.hasVariableLocal(localVariableExecutionId, match)) {
//                                runtimeService.removeVariableLocal(localVariableExecutionId, match); // 删该分支留下的局部历史
//                            }

                            // 提取本次提交的新选择
                            if (variables.containsKey(match)) {
                                localIsolatedVariables.put(match, variables.get(match));
                                variables.remove(match); // 从将要全量覆盖的集合中剥离
                            }
                        }
                    }
                }
            }
        }

        // =================================================================================
        // 【第 5 道防线】：双轨数据写入！
        // =================================================================================
        Map<String, String> returnBranchContext = buildReturnBranchContextForApproval(task.getProcessInstanceId(),
                task.getExecutionId(), localVariableExecutionId, task.getTaskDefinitionKey());
        List<String> returnableTaskPath = buildReturnableTaskPathForApproval(task.getProcessInstanceId(),
                task.getExecutionId(), localVariableExecutionId, task.getTaskDefinitionKey());
        taskService.setVariableLocal(task.getId(), RETURN_BRANCH_CONTEXT_VARIABLE, new LinkedHashMap<>(returnBranchContext));
        taskService.setVariableLocal(task.getId(), RETURNABLE_TASK_PATH_VARIABLE, new ArrayList<>(returnableTaskPath));

        // 5.1 全局写入：只保留真正的纯业务变量（表单填的数据等）
        runtimeService.setVariables(task.getProcessInstanceId(), variables);

        // 5.2 局部写入：将剥离出来的危险数据死死绑定在存活分支 (localVariableExecutionId) 的专属口袋上！
        if (!localIsolatedVariables.isEmpty()) {
            runtimeService.setVariablesLocal(localVariableExecutionId, localIsolatedVariables);
        }

        // =================================================================================
        // 【标准加签】：同环节动态追加审批人 (使用 Flowable 原生 API)
        // =================================================================================
        if (CollUtil.isNotEmpty(reqVO.getAddSignUserIds())) {
            if (miRootExecutionId == null) {
                throw new RuntimeException("当前节点非多实例节点，无法进行同环节加签！");
            }

            org.flowable.engine.runtime.Execution miRootExecution = runtimeService.createExecutionQuery()
                    .executionId(miRootExecutionId) // 准确使用刚才锁定的 MI Root
                    .singleResult();

            String collectionVarName = "coll_userList";
            String elementVarName = "assignee";
            if (currentFlowElement instanceof org.flowable.bpmn.model.UserTask) {
                org.flowable.bpmn.model.UserTask userTask = (org.flowable.bpmn.model.UserTask) currentFlowElement;
                if (userTask.getLoopCharacteristics() != null && StrUtil.isNotBlank(userTask.getLoopCharacteristics().getInputDataItem())) {
                    collectionVarName = userTask.getLoopCharacteristics().getInputDataItem().replace("${", "").replace("}", "").trim();
                }
            }

            // 【全局雷达】：直接查询数据库，获取该节点当前所有的活跃办理人！防跨 Token 穿透加签！
            List<Task> activeTasksInThisNode = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .taskDefinitionKey(task.getTaskDefinitionKey())
                    .active()
                    .list();
            Set<String> activeAssignees = activeTasksInThisNode.stream()
                    .map(Task::getAssignee)
                    .filter(cn.hutool.core.util.StrUtil::isNotBlank)
                    .collect(Collectors.toSet());

            Object collObj = runtimeService.getVariableLocal(miRootExecutionId, collectionVarName);
            if (collObj == null) {
                // 兜底：如果局部没有，再去全局拿
                collObj = runtimeService.getVariable(task.getProcessInstanceId(), collectionVarName);
            }
            List<Object> collUserList = collObj != null ? new ArrayList<>((Collection<?>) collObj) : new ArrayList<>();

            for (Long addSignUserId : reqVO.getAddSignUserIds()) {
                String userIdStr = String.valueOf(addSignUserId);

                // 【核心防线】：只要他已经在活跃任务列表里了，直接跳过！
                if (activeAssignees.contains(userIdStr)) {
                    continue;
                }

                // 维护引擎底层集合，防止 Flowable 内部多实例数据脱节
                if (!collUserList.contains(addSignUserId) && !collUserList.contains(userIdStr)) {
                    collUserList.add(addSignUserId);
                }

                Map<String, Object> miVars = new HashMap<>();
                miVars.put(elementVarName, userIdStr);

                // ====================================================================
                // 【官方原生 API】：一句话替代之前几十行的底层复杂逻辑
                // ====================================================================
//                org.flowable.engine.runtime.Execution newExecution = runtimeService.addMultiInstanceExecution(
//                        task.getTaskDefinitionKey(),
//                        miRootExecution.getParentId(), // ✅ 【唯一修改点】：传入整个流程实例的 ID！
//                        miVars
//                );
                Integer tempNrOfInstances = (Integer) runtimeService.getVariableLocal(miRootExecutionId, "nrOfInstances");
                if (tempNrOfInstances == null) tempNrOfInstances = 0;

                // 【修复编译报错】：声明一个 final 变量，专门传给内部类当 loopCounter 用！
                final int loopCounterIndex = tempNrOfInstances;

                // 外层事务执行总人数 + 1
                runtimeService.setVariableLocal(miRootExecutionId, "nrOfInstances", loopCounterIndex + 1);

                Integer tempNrOfActive = (Integer) runtimeService.getVariableLocal(miRootExecutionId, "nrOfActiveInstances");
                if (tempNrOfActive == null) tempNrOfActive = 0;
                // 外层事务执行活跃人数 + 1
                runtimeService.setVariableLocal(miRootExecutionId, "nrOfActiveInstances", tempNrOfActive + 1);
                org.flowable.engine.runtime.Execution newExecution = managementService.executeCommand(new org.flowable.common.engine.impl.interceptor.Command<org.flowable.engine.runtime.Execution>() {
                    @Override
                    public org.flowable.engine.runtime.Execution execute(org.flowable.common.engine.impl.interceptor.CommandContext commandContext) {
                        org.flowable.engine.impl.persistence.entity.ExecutionEntityManager executionEntityManager =
                                org.flowable.engine.impl.util.CommandContextUtil.getExecutionEntityManager(commandContext);

                        org.flowable.engine.impl.persistence.entity.ExecutionEntity miRoot =
                                executionEntityManager.findById(miRootExecution.getId());
                        org.flowable.engine.impl.persistence.entity.ExecutionEntity childExecution =
                                executionEntityManager.createChildExecution(miRoot);
                        childExecution.setCurrentFlowElement(miRoot.getCurrentFlowElement());

                        // 【修改点】：直接拿外面准备好的 currentNrOfInstances 作为编号
                        childExecution.setVariablesLocal(miVars);
                        childExecution.setVariableLocal("loopCounter", loopCounterIndex);
                        childExecution.setActive(true);
                        childExecution.setScope(false);

                        org.flowable.engine.impl.util.CommandContextUtil.getAgenda(commandContext)
                                .planContinueProcessOperation(childExecution);

                        return childExecution;
                    }
//                    public org.flowable.engine.runtime.Execution execute(org.flowable.common.engine.impl.interceptor.CommandContext commandContext) {
//                        org.flowable.engine.impl.persistence.entity.ExecutionEntityManager executionEntityManager =
//                                org.flowable.engine.impl.util.CommandContextUtil.getExecutionEntityManager(commandContext);
//
//                        // 1. 【核心防御】：精准锁定当前这根 Token 的 MI Root，指哪打哪，绝对不会找错！
//                        org.flowable.engine.impl.persistence.entity.ExecutionEntity miRoot =
//                                executionEntityManager.findById(miRootExecution.getId());
//
//                        // 2. 创建子 Execution 挂载在这棵指定的树上
//                        org.flowable.engine.impl.persistence.entity.ExecutionEntity childExecution =
//                                executionEntityManager.createChildExecution(miRoot);
//                        childExecution.setCurrentFlowElement(miRoot.getCurrentFlowElement());
//
//                        // 3. 手动维护多实例的计数器
//                        Integer nrOfInstances = (Integer) miRoot.getVariableLocal("nrOfInstances");
//                        if (nrOfInstances == null) nrOfInstances = 0;
//                        miRoot.setVariableLocal("nrOfInstances", nrOfInstances + 1);
//
//                        Integer nrOfActiveInstances = (Integer) miRoot.getVariableLocal("nrOfActiveInstances");
//                        if (nrOfActiveInstances == null) nrOfActiveInstances = 0;
//                        miRoot.setVariableLocal("nrOfActiveInstances", nrOfActiveInstances + 1);
//
//                        // 4. 设置局部变量（传入张三的 assignee）
//                        childExecution.setVariablesLocal(miVars);
//                        childExecution.setVariableLocal("loopCounter", nrOfInstances);
//                        childExecution.setActive(true);
//                        childExecution.setScope(false);
//
//                        // 5. 触发引擎继续流转，生成物理 Task 数据
//                        org.flowable.engine.impl.util.CommandContextUtil.getAgenda(commandContext)
//                                .planContinueProcessOperation(childExecution);
//
//                        return childExecution;
//                    }
                });

                Task newTask = taskService.createTaskQuery().executionId(newExecution.getId()).singleResult();
                if (newTask != null) {
                    taskService.setAssignee(newTask.getId(), userIdStr);
                    taskService.setOwner(newTask.getId(), userIdStr);
                    taskService.setVariableLocal(newTask.getId(), "internal_source_task_id", task.getId());
                    runtimeService.setVariableLocal(newExecution.getId(), RETURNABLE_TASK_PATH_VARIABLE,
                            new ArrayList<>(getReturnableTaskPath(task.getProcessInstanceId(), task.getExecutionId())));
                }
            }

            // 【终极隔离】：无视全局，强行把加签后的名单私有化到当前 Token 的 MI Root 上！严防多 Token 交叉泄露覆盖！
            runtimeService.setVariableLocal(miRootExecutionId, collectionVarName, collUserList);
            variables.remove(collectionVarName);

            // ====================================================================
            // 【严防死守】：将加签选人历史更新到 Local 口袋！绝不能泄露回 variables 全局！
            // ====================================================================
            Object lastNodeAssigneesObj = localIsolatedVariables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
            if (lastNodeAssigneesObj == null) {
                lastNodeAssigneesObj = runtimeService.getVariableLocal(localVariableExecutionId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
            }

            @SuppressWarnings("unchecked")
            Map<String, List<Long>> lastNodeAssigneesMap = new HashMap<>();
            if (lastNodeAssigneesObj != null) {
                lastNodeAssigneesMap.putAll((Map<String, List<Long>>) lastNodeAssigneesObj);
            }

            List<Long> currentTaskAssignees = lastNodeAssigneesMap.getOrDefault(task.getTaskDefinitionKey(), new ArrayList<>());
            Set<Long> uniqueAssignees = new LinkedHashSet<>(currentTaskAssignees);
            uniqueAssignees.addAll(reqVO.getAddSignUserIds());
            lastNodeAssigneesMap.put(task.getTaskDefinitionKey(), new ArrayList<>(uniqueAssignees));

            // 重新写回安全的局部口袋 (localVariableExecutionId)
            localIsolatedVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, lastNodeAssigneesMap);
            runtimeService.setVariablesLocal(localVariableExecutionId, localIsolatedVariables);

            try {
                List<AdminUserRespDTO> addUsers = adminUserApi.getUserList(reqVO.getAddSignUserIds());
                if (CollUtil.isNotEmpty(addUsers)) {
                    String addNames = addUsers.stream().map(AdminUserRespDTO::getNickname).collect(Collectors.joining(","));
                    String signComment = StrUtil.format("办理完成并向当前环节动态追加审批人: {}", addNames);
                    taskService.addComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.APPROVE.getType(), signComment);
                }
            } catch (Exception e) {
                log.warn("写入加签日志失败", e);
            }
        }

        // 6. 如果当前节点 Id 存在于需要预测的流程节点中，从中移除 (清理模拟历史残留)
        Object needSimulateTaskIds = runtimeService.getVariable(task.getProcessInstanceId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEED_SIMULATE_TASK_IDS);
        if (needSimulateTaskIds != null) {
            Set<String> needSimulateTaskIdsByReturn = Convert.toSet(String.class, needSimulateTaskIds);
            if (needSimulateTaskIdsByReturn.contains(task.getTaskDefinitionKey())) {
                needSimulateTaskIdsByReturn.remove(task.getTaskDefinitionKey());
                runtimeService.setVariable(task.getProcessInstanceId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEED_SIMULATE_TASK_IDS, needSimulateTaskIdsByReturn);
            }
        }

        // 7. 调用 BPM complete 去完成任务 (此时的 variables 已经完全脱敏剥离，绝对纯净安全！)
        ReturnBranchContextCarrier previousCarrier = RETURN_BRANCH_CONTEXT_CARRIER.get();
        ReturnableTaskPathCarrier previousPathCarrier = RETURNABLE_TASK_PATH_CARRIER.get();
        RETURN_BRANCH_CONTEXT_CARRIER.set(new ReturnBranchContextCarrier(task.getProcessInstanceId(), returnBranchContext));
        RETURNABLE_TASK_PATH_CARRIER.set(new ReturnableTaskPathCarrier(task.getProcessInstanceId(), returnableTaskPath));
        try {
            taskService.complete(task.getId(), variables, localIsolatedVariables);
        } finally {
            if (previousCarrier == null) {
                RETURN_BRANCH_CONTEXT_CARRIER.remove();
            } else {
                RETURN_BRANCH_CONTEXT_CARRIER.set(previousCarrier);
            }
            if (previousPathCarrier == null) {
                RETURNABLE_TASK_PATH_CARRIER.remove();
            } else {
                RETURNABLE_TASK_PATH_CARRIER.set(previousPathCarrier);
            }
        }

        // 【加签专属】处理加签任务
        handleParentTaskIfSign(task.getParentTaskId());
    }
    /**
     * 校验选择的下一个节点的审批人，是否合法
     * <p>
     * 1. 是否有漏选：没有选择审批人
     * 2. 是否有多选：非下一个节点
     *
     * @param taskDefinitionKey 当前任务节点标识
     * @param variables         流程变量
     * @param bpmnModel         流程模型
     * @param nextAssignees     下一个节点审批人集合（参数）
     * @param processInstance   流程实例
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> validateAndSetNextAssignees(String taskDefinitionKey, Map<String, Object> variables, BpmnModel bpmnModel,
                                                            Map<String, List<Long>> nextAssignees, ProcessInstance processInstance,String taskId) {
        // simple 设计器第一个节点默认为发起人节点，不校验是否存在审批人
        if (Objects.equals(taskDefinitionKey, START_USER_NODE_ID)) {
            return variables;
        }
        // 1. 获取下一个将要执行的节点集合
        FlowElement flowElement = bpmnModel.getFlowElement(taskDefinitionKey);
        List<FlowNode> rawNextNodes = getNextFlowNodes(flowElement, bpmnModel, variables);

        List<FlowNode> nextFlowNodes = new ArrayList<>();
        for (FlowNode node : rawNextNodes) {
            // 递归查找有效的 UserTask（如果是子流程，则通过 StartEvent 进入查找）
            collectEffectiveUserTasks(node, nextFlowNodes);
        }
        // 2. 校验选择的下一个节点的审批人，是否合法
        for (FlowNode nextFlowNode : nextFlowNodes) {
            Integer candidateStrategy = parseCandidateStrategy(nextFlowNode);
            // 2.1 情况一：如果节点中的审批人策略为 发起人自选
            if (ObjUtil.equals(candidateStrategy, BpmTaskCandidateStrategyEnum.START_USER_SELECT.getStrategy())) {
                // 特殊：如果当前节点已经存在审批人，则不允许覆盖
                Map<String, List<Long>> startUserSelectAssignees = FlowableUtils.getStartUserSelectAssignees(processInstance.getProcessVariables());
                if (startUserSelectAssignees != null && CollUtil.isNotEmpty(startUserSelectAssignees.get(nextFlowNode.getId()))) {
                    continue;
                }
                // 如果节点存在，但未配置审批人
                List<Long> assignees = nextAssignees != null ? nextAssignees.get(nextFlowNode.getId()) : null;
                if (CollUtil.isEmpty(assignees)) {
                    throw exception(PROCESS_INSTANCE_START_USER_SELECT_ASSIGNEES_NOT_CONFIG, nextFlowNode.getName());
                }

                // 设置 PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES
                if (startUserSelectAssignees == null) {
                    startUserSelectAssignees = new HashMap<>();
                }
                startUserSelectAssignees.put(nextFlowNode.getId(), assignees);
                variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES, startUserSelectAssignees);
                continue;
            }

            // 2.2 情况二：如果节点中的审批人策略为 审批人，在审批时选择下一个节点的审批人，并且该节点的审批人为空
            if (ObjUtil.equals(candidateStrategy, BpmTaskCandidateStrategyEnum.APPROVE_USER_SELECT.getStrategy())) {
                // 如果节点存在，但未配置审批人
                Map<String, List<Long>> approveUserSelectAssignees = FlowableUtils.getApproveUserSelectAssignees(processInstance.getProcessVariables());
                List<Long> assignees = nextAssignees != null ? nextAssignees.get(nextFlowNode.getId()) : null;
                if (CollUtil.isEmpty(assignees)) {
                    throw exception(PROCESS_INSTANCE_APPROVE_USER_SELECT_ASSIGNEES_NOT_CONFIG, nextFlowNode.getName());
                }

                // 设置 PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES
                if (approveUserSelectAssignees == null) {
                    approveUserSelectAssignees = new HashMap<>();
                }
                approveUserSelectAssignees.put(nextFlowNode.getId(), assignees);
                Map<String, List<Long>> existingApproveUserSelectAssignees = (Map<String, List<Long>>) variables.get(
                        BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);
                if (CollUtil.isNotEmpty(existingApproveUserSelectAssignees)) {
                    approveUserSelectAssignees.putAll(existingApproveUserSelectAssignees);
                }
                variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, approveUserSelectAssignees);
            }

            // 2.3 情况三：如果节点中的审批人策略为 手动，在审批时选择下一个节点的审批人，并且该节点的审批人为空
            if (ObjUtil.equals(candidateStrategy, BpmTaskCandidateStrategyEnum.MANUAL_SELECTED.getStrategy())) {

                String nodeId = nextFlowNode.getId();
                String processInstanceId = processInstance.getId();
                Map<String, List<Long>> finalAssigneeMap = (Map<String, List<Long>>) variables.get(
                        BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
                if (finalAssigneeMap == null) {
                    finalAssigneeMap = new HashMap<>();
                } else {
                    finalAssigneeMap = new HashMap<>(finalAssigneeMap);
                }
                Map<String, Long> updateDateMap = (Map<String, Long>) variables.get(
                        BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME);
                if (updateDateMap == null) {
                    updateDateMap = new HashMap<>();
                } else {
                    updateDateMap = new HashMap<>(updateDateMap);
                }

                // newAssignees: 前端本次提交新选择的人员名单 (例如：[张三, 李四])
                List<Long> newAssignees = nextAssignees != null ? nextAssignees.get(nodeId) : null;
                if (CollUtil.isEmpty(newAssignees)) {
                    continue;
                }

                Long lastUpdateTime = updateDateMap.get(nodeId);
                boolean isDirtyData = false;
                if (lastUpdateTime == null) {
                    isDirtyData = true;
                } else {
                    Task currentTask = taskService.createTaskQuery().taskId(taskId).singleResult();
                    Execution taskExecution = runtimeService.createExecutionQuery()
                            .executionId(currentTask.getExecutionId())
                            .singleResult();
                    String currentExecutionId = taskExecution.getId();
                    String parentExecutionId = taskExecution.getParentId();
                    // 【关键改动】：获取当前节点（审批环节）在本次循环中的最早进入时间，而不是当前子任务的创建时间！
                    List<HistoricActivityInstance> activeActivities = historyService.createHistoricActivityInstanceQuery()
                            .processInstanceId(processInstanceId)
                            .activityId(taskDefinitionKey) // 当前操作人所在的节点ID (例如：Activity_1bm8630)
                            .unfinished()
                            .orderByHistoricActivityInstanceStartTime().asc() // 取最早启动的那一条
                            .list();

                    long nodeEntryTime = System.currentTimeMillis(); // 兜底时间
                    for (HistoricActivityInstance activity : activeActivities) {
                        String actExecId = activity.getExecutionId();
                        // 判定条件：只要这个活动的执行流等于“当前任务执行流”或“当前父级(会签根)执行流”，它就属于本支线！
                        if (currentExecutionId.equals(actExecId) ||
                                (parentExecutionId != null && parentExecutionId.equals(actExecId))) {
                            nodeEntryTime = activity.getStartTime().getTime();
                            break; // 因为前面查库已经 asc 排序了，命中的第一个必然是本支线最早进入的时间
                        }
                    }

                    if (lastUpdateTime < nodeEntryTime) {
                        isDirtyData = true;
                    }
                }
                Set<Long> branchWantedSet = new LinkedHashSet<>();
                if (isDirtyData) {
                    branchWantedSet.addAll(newAssignees); // 脏数据，直接覆盖
                } else {
                    List<Long> existingAssignees = finalAssigneeMap.getOrDefault(nodeId, new ArrayList<>());
                    branchWantedSet.addAll(existingAssignees);
                    branchWantedSet.addAll(newAssignees); // 并发合并
                }

                List<Task> runningTasks = taskService.createTaskQuery()
                        .processInstanceId(processInstanceId)
                        .taskDefinitionKey(nodeId)
                        .active()
                        .list();

                if (CollUtil.isNotEmpty(runningTasks)) {
                    // ==========================================
                    // 场景 A：节点正在运行 -> 【动态加签模式】
                    // ==========================================
                    // 1. 提取当前正在运行的任务的审批人
                    Set<String> runningUserIds = runningTasks.stream()
                            .map(Task::getAssignee)
                            .filter(StrUtil::isNotBlank)
                            .collect(Collectors.toSet());

                    // 2. 找出需要真正创建新任务的人员（前端传来的名单 - 正在运行的名单）
                    // 重点：这里不去重历史已完成的人员！只要不在运行中，就重新生成！
                    Set<Long> tasksToCreate = new LinkedHashSet<>();
                    for (Long userId : branchWantedSet) {
                        if (!runningUserIds.contains(String.valueOf(userId))) {
                            tasksToCreate.add(userId);
                        }
                    }
                    if (CollUtil.isEmpty(tasksToCreate)) {
                        throw exception(PROCESS_INSTANCE_APPROVE_USER_SELECT_ASSIGNEES_IS_HAVE, nextFlowNode.getName());
//                        variables.put("KILL_CURRENT_TOKEN_FLAG", true);
//                        continue;
                    }
                    finalAssigneeMap.put(nodeId, new ArrayList<>(tasksToCreate));

                } else {
                    // ==========================================
                    // 场景 B：节点未运行 -> 走常规预埋变量模式
                    // ==========================================
                    // 节点还没走到，直接对传入的数组做个基础去重（防止前端传 [张三, 张三]），然后埋入变量即可
//                    variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, finalAssigneeMap);
//                    finalAssigneeMap.put(nodeId, new ArrayList<>(mergedSet));
                    finalAssigneeMap.put(nodeId, new ArrayList<>(branchWantedSet));
                }

                updateDateMap.put(nodeId, System.currentTimeMillis());
                // 统一存回变量池
                variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, finalAssigneeMap);
                variables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME, updateDateMap);
            }
        }
        return variables;
    }



    private void collectEffectiveUserTasks(FlowNode node, List<FlowNode> result) {
        if (node instanceof UserTask) {
            result.add(node);
        } else if (node instanceof SubProcess) {
            // 处理嵌入式子流程
            SubProcess subProcess = (SubProcess) node;
            Collection<FlowElement> subElements = subProcess.getFlowElements();
            for (FlowElement subElement : subElements) {
                // 从子流程内部的 StartEvent 开始寻找
                if (subElement instanceof StartEvent) {
                    analyzeInternalPath((FlowNode) subElement, result);
                }
            }
        } else if (node instanceof Gateway) {
            // 如果外层直接遇到了网关（极少情况，通常 getNextFlowNodes 已处理，但为了健壮性加上）
            analyzeInternalPath(node, result);
        }
    }

    private boolean hasBack(List<ExtensionElement> propertyList) {
        if (propertyList == null || propertyList.isEmpty()) return false;

        for (ExtensionElement prop : propertyList) {
            String name = prop.getAttributeValue(null, "name");
            String value = prop.getAttributeValue(null, "value");
            if ("is_back".equals(name) && "1".equals(value)) {
                return true;
            }
        }
        return false;
    }

    private void analyzeInternalPath(FlowNode source, List<FlowNode> result) {
        List<SequenceFlow> outgoingFlows = source.getOutgoingFlows();
        for (SequenceFlow flow : outgoingFlows) {
            FlowElement target = flow.getTargetFlowElement();

            if (target instanceof UserTask) {
                result.add((UserTask) target);
            } else if (target instanceof Gateway) {
                // 遇到网关，递归穿透
                analyzeInternalPath((FlowNode) target, result);
            } else if (target instanceof SubProcess) {
                // 遇到嵌套子流程，递归调用主收集方法
                collectEffectiveUserTasks((FlowNode) target, result);
            } else if (target instanceof EndEvent) {
                // 遇到结束事件，路径结束，不做处理
            }
        }
    }

    /**
     * 提取当前节点出线条件里使用的流程变量。
     * 退回后重新审批时，需要先清理这些变量的旧值，否则普通 UserTask 多出线会把旧走向和新走向一起触发。
     */
    private Set<String> getOutgoingConditionVariableNames(FlowElement flowElement) {
        if (!(flowElement instanceof FlowNode)) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (SequenceFlow flow : ((FlowNode) flowElement).getOutgoingFlows()) {
            String condition = flow.getConditionExpression();
            if (StrUtil.isBlank(condition)) {
                continue;
            }
            java.util.regex.Matcher variablesGetMatcher = java.util.regex.Pattern
                    .compile("variables:get\\(\\s*['\\\"]?([a-zA-Z_][a-zA-Z0-9_]*)['\\\"]?\\s*\\)")
                    .matcher(condition);
            boolean foundVariableGet = false;
            while (variablesGetMatcher.find()) {
                foundVariableGet = true;
                result.add(variablesGetMatcher.group(1));
            }
            // 兼容 ${foo == 'bar'} 这种没有 variables:get(...) 的表达式。
            if (!foundVariableGet) {
                java.util.regex.Matcher identifierMatcher = java.util.regex.Pattern
                        .compile("(?<!['\\\"])(?<![a-zA-Z0-9_])([a-zA-Z_][a-zA-Z0-9_]*)(?![a-zA-Z0-9_])(?!['\\\"])")
                        .matcher(condition);
                while (identifierMatcher.find()) {
                    String name = identifierMatcher.group(1);
                    if (!StrUtil.equalsAny(name, "variables", "get", "null", "empty", "true", "false")) {
                        result.add(name);
                    }
                }
            }
        }
        return result;
    }

    private void clearReturnTargetOutgoingVariables(String processInstanceId, List<String> executionIdsToMove,
                                                    FlowElement targetElement) {
        Set<String> variableNames = getOutgoingConditionVariableNames(targetElement);
        if (CollUtil.isEmpty(variableNames)) {
            return;
        }
        // 全局只清退回目标节点自身的出线变量。这些变量如果留在流程实例级，会污染重新进入目标节点后的路径判断。
        for (String variableName : variableNames) {
            runtimeService.removeVariable(processInstanceId, variableName);
        }

        // local 只清本次被退回 execution 的血统，不扫描全流程，避免影响其它并行分支自己的局部路由选择。
        Set<String> executionIdsToClean = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(executionIdsToMove)) {
            for (String executionId : executionIdsToMove) {
                String currentExecutionId = executionId;
                while (StrUtil.isNotBlank(currentExecutionId) && !StrUtil.equals(currentExecutionId, processInstanceId)
                        && executionIdsToClean.add(currentExecutionId)) {
                    Execution execution = runtimeService.createExecutionQuery().executionId(currentExecutionId).singleResult();
                    currentExecutionId = execution != null ? execution.getParentId() : null;
                }
            }
        }
        for (String variableName : variableNames) {
            for (String executionId : executionIdsToClean) {
                if (runtimeService.hasVariableLocal(executionId, variableName)) {
                    runtimeService.removeVariableLocal(executionId, variableName);
                }
            }
        }
    }

    private Map<String, String> getReturnBranchContext(String processInstanceId, String executionId) {
        return getReturnBranchContext(processInstanceId, executionId, false);
    }

    private Map<String, String> getReturnBranchContext(String processInstanceId, String executionId, boolean includeProcessInstanceRoot) {
        String currentExecutionId = executionId;
        while (StrUtil.isNotBlank(currentExecutionId)) {
            if (StrUtil.equals(currentExecutionId, processInstanceId) && !includeProcessInstanceRoot) {
                break;
            }
            Object value = runtimeService.getVariableLocal(currentExecutionId, RETURN_BRANCH_CONTEXT_VARIABLE);
            Map<String, String> context = convertReturnBranchContext(value);
            if (CollUtil.isNotEmpty(context)) {
                return context;
            }
            if (StrUtil.equals(currentExecutionId, processInstanceId)) {
                break;
            }
            Execution execution = runtimeService.createExecutionQuery().executionId(currentExecutionId).singleResult();
            currentExecutionId = execution != null ? execution.getParentId() : null;
        }
        return new LinkedHashMap<>();
    }

    private Map<String, String> convertReturnBranchContext(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!(value instanceof Map)) {
            return result;
        }
        Map<?, ?> map = (Map<?, ?>) value;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private Map<String, String> getHistoricTaskReturnBranchContext(String taskId) {
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery()
                .taskId(taskId)
                .variableName(RETURN_BRANCH_CONTEXT_VARIABLE)
                .singleResult();
        return variable != null ? convertReturnBranchContext(variable.getValue()) : new LinkedHashMap<>();
    }

    private String inferUniqueReturnBatchIdFromRunningTasks(String processInstanceId, String sourceTaskKey,
                                                           List<Task> runningTasks) {
        Set<String> batchIds = new LinkedHashSet<>();
        if (CollUtil.isEmpty(runningTasks)) {
            return null;
        }
        for (Task runningTask : runningTasks) {
            Map<String, String> context = getReturnBranchContext(processInstanceId, runningTask.getExecutionId());
            String batchId = context.get(sourceTaskKey);
            if (StrUtil.isNotBlank(batchId)) {
                batchIds.add(batchId);
            }
        }
        return batchIds.size() == 1 ? batchIds.iterator().next() : null;
    }

    private Map<String, String> buildReturnBranchContextForApproval(String processInstanceId, String taskExecutionId,
                                                                    String localVariableExecutionId, String taskDefinitionKey) {
        Map<String, String> context = getReturnBranchContext(processInstanceId, taskExecutionId);
        if (CollUtil.isEmpty(context)) {
            context = getReturnBranchContext(processInstanceId, localVariableExecutionId);
        }
        // 兼容已经落到流程实例 root 上的历史数据。只在审批流出构造上下文时读取，不用于退回判断，避免并行分支污染。
        if (CollUtil.isEmpty(context)) {
            context = getReturnBranchContext(processInstanceId, localVariableExecutionId, true);
        }
        context = new LinkedHashMap<>(context);
        context.put(taskDefinitionKey, IdUtil.fastSimpleUUID());
        return context;
    }

    private List<String> getReturnableTaskPath(String processInstanceId, String executionId) {
        List<String> path = findReturnableTaskPath(processInstanceId, executionId);
        return path != null ? path : new ArrayList<>();
    }

    private List<String> findReturnableTaskPath(String processInstanceId, String executionId) {
        String currentExecutionId = executionId;
        while (StrUtil.isNotBlank(currentExecutionId) && !StrUtil.equals(currentExecutionId, processInstanceId)) {
            if (runtimeService.hasVariableLocal(currentExecutionId, RETURNABLE_TASK_PATH_VARIABLE)) {
                Object value = runtimeService.getVariableLocal(currentExecutionId, RETURNABLE_TASK_PATH_VARIABLE);
                return convertReturnableTaskPath(value);
            }
            Execution execution = runtimeService.createExecutionQuery().executionId(currentExecutionId).singleResult();
            currentExecutionId = execution != null ? execution.getParentId() : null;
        }
        return null;
    }

    private List<String> convertReturnableTaskPath(Object value) {
        List<String> result = new ArrayList<>();
        if (!(value instanceof Collection)) {
            return result;
        }
        for (Object item : (Collection<?>) value) {
            if (item != null) {
                String taskDefinitionKey = String.valueOf(item);
                if (StrUtil.isNotBlank(taskDefinitionKey) && !result.contains(taskDefinitionKey)) {
                    result.add(taskDefinitionKey);
                }
            }
        }
        return result;
    }

    private List<String> buildReturnableTaskPathForApproval(String processInstanceId, String taskExecutionId,
                                                            String localVariableExecutionId, String taskDefinitionKey) {
        List<String> path = findReturnableTaskPath(processInstanceId, taskExecutionId);
        if (path == null) {
            path = findReturnableTaskPath(processInstanceId, localVariableExecutionId);
        }
        if (path == null) {
            path = new ArrayList<>();
        }
        path = new ArrayList<>(path);
        if (!path.contains(taskDefinitionKey)) {
            path.add(taskDefinitionKey);
        }
        return path;
    }

    private Set<String> buildReturnableTaskKeySet(BpmnModel bpmnModel, List<String> returnableTaskPath,
                                                  Set<String> finishedTaskDefinitionKeys) {
        Set<String> result = new LinkedHashSet<>(returnableTaskPath);
        if (CollUtil.isEmpty(returnableTaskPath)) {
            return result;
        }

        // 兼容半路开始记录路径的实例：路径首节点之前的已办祖先节点仍然允许退回。
        // 例如路径只有 [主任拟办] 时，需要把主任拟办之前的 [来文登记] 补回来。
        FlowElement firstRecordedElement = BpmnModelUtils.getFlowElementById(bpmnModel, returnableTaskPath.get(0));
        if (firstRecordedElement == null) {
            return result;
        }
        List<UserTask> previousUserTasks = BpmnModelUtils.getPreviousUserTaskList(firstRecordedElement, null, null);
        if (CollUtil.isEmpty(previousUserTasks)) {
            return result;
        }
        for (UserTask previousUserTask : previousUserTasks) {
            if (finishedTaskDefinitionKeys.contains(previousUserTask.getId())
                    && BpmnModelUtils.isSequentialReachable(firstRecordedElement, previousUserTask, null)) {
                result.add(previousUserTask.getId());
            }
        }
        return result;
    }

    private List<String> trimReturnableTaskPathForReturn(String processInstanceId, String executionId, String targetTaskKey) {
        List<String> path = getReturnableTaskPath(processInstanceId, executionId);
        if (CollUtil.isEmpty(path)) {
            return new ArrayList<>();
        }
        int targetIndex = path.indexOf(targetTaskKey);
        if (targetIndex < 0) {
            return new ArrayList<>(path);
        }
        return new ArrayList<>(path.subList(0, targetIndex));
    }

    /**
     * 审批通过存在“后加签”的任务。
     * <p>
     * 注意：该任务不能马上完成，需要一个中间状态（APPROVING），并激活剩余所有子任务（PROCESS）为可审批处理
     * 如果马上完成，则会触发下一个任务，甚至如果没有下一个任务则流程实例就直接结束了！
     *
     * @param task  当前任务
     * @param reqVO 前端请求参数
     */
    private void approveAfterSignTask(Task task, BpmTaskApproveReqVO reqVO) {
        // 更新父 task 状态 + 原因
        updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.APPROVING.getStatus(), reqVO.getReason());

        // 2. 激活子任务
        List<Task> childrenTaskList = getTaskListByParentTaskId(task.getId());
        for (Task childrenTask : childrenTaskList) {
            taskService.resolveTask(childrenTask.getId());
            // 更新子 task 状态
            updateTaskStatus(childrenTask.getId(), BpmTaskStatusEnum.RUNNING.getStatus());
        }
    }

    /**
     * 如果父任务是有前后【加签】的任务，如果它【加签】出来的子任务都被处理，需要处理父任务：
     * <p>
     * 1. 如果是【向前】加签，则需要重新激活父任务，让它可以被审批
     * 2. 如果是【向后】加签，则需要完成父任务，让它完成审批
     *
     * @param parentTaskId 父任务编号
     */
    private void handleParentTaskIfSign(String parentTaskId) {
        if (StrUtil.isBlank(parentTaskId)) {
            return;
        }
        // 1.1 判断是否还有子任务。如果没有，就不处理
        Long childrenTaskCount = getTaskCountByParentTaskId(parentTaskId);
        if (childrenTaskCount > 0) {
            return;
        }
        // 1.2 只处理加签的父任务
        Task parentTask = validateTaskExist(parentTaskId);
        String scopeType = parentTask.getScopeType();
        if (BpmTaskSignTypeEnum.of(scopeType) == null) {
            return;
        }

        // 2. 子任务已处理完成，清空 scopeType 字段，修改 parentTask 信息，方便后续可以继续向前后向后加签
        TaskEntityImpl parentTaskImpl = (TaskEntityImpl) parentTask;
        parentTaskImpl.setScopeType(null);
        taskService.saveTask(parentTaskImpl);

        // 3.1 情况一：处理向【向前】加签
        if (BpmTaskSignTypeEnum.BEFORE.getType().equals(scopeType)) {
            // 3.1.1 owner 重新赋值给父任务的 assignee，这样它就可以被审批
            taskService.resolveTask(parentTaskId);
            // 3.1.2 更新流程任务 status
            updateTaskStatus(parentTaskId, BpmTaskStatusEnum.RUNNING.getStatus());
            // 3.2 情况二：处理向【向后】加签
        } else if (BpmTaskSignTypeEnum.AFTER.getType().equals(scopeType)) {
            // 只有 parentTask 处于 APPROVING 的情况下，才可以继续 complete 完成
            // 否则，一个未审批的 parentTask 任务，在加签出来的任务都被减签的情况下，就直接完成审批，这样会存在问题
            Integer status = (Integer) parentTask.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
            if (ObjectUtil.notEqual(status, BpmTaskStatusEnum.APPROVING.getStatus())) {
                return;
            }
            // 3.2.2 完成自己（因为它已经没有子任务，所以也可以完成）
            updateTaskStatus(parentTaskId, BpmTaskStatusEnum.APPROVE.getStatus());
            taskService.complete(parentTaskId);
        }

        // 4. 递归处理父任务
        handleParentTaskIfSign(parentTask.getParentTaskId());
    }


    /**
     * 自动合并（秒批）同节点下分配给同一个人的重复任务
     * * @param currentTask      当前刚刚办理完成的主任务
     * @param processVariables 流程变量（原样传递给重复任务，保证路线不出错）
     * @param commentType      审批意见的类型字典值（例如："2"代表同意，"3"代表拒绝/退回等）
     * @param actionName       动作名称（用于拼接审批意见，如："同意"、"拒绝"）
     */
    private void autoMergeDuplicateTasks(org.flowable.task.api.Task currentTask,
                                         Map<String, Object> processVariables,
                                         String commentType,
                                         String actionName) {
        // 安全校验：如果任务为空或没有分配人，直接跳过
        if (currentTask == null || cn.hutool.core.util.StrUtil.isBlank(currentTask.getAssignee())) {
            return;
        }

        try {
            // 1. 查找同流程实例、同节点、同办理人的其他【未办】活跃任务
            List<org.flowable.task.api.Task> duplicateTasks = taskService.createTaskQuery()
                    .processInstanceId(currentTask.getProcessInstanceId())
                    .taskDefinitionKey(currentTask.getTaskDefinitionKey())
                    .taskAssignee(currentTask.getAssignee())
                    .active()
                    .list();

            // 2. 遍历并执行静默合并
            for (org.flowable.task.api.Task duplicateTask : duplicateTasks) {
                // 排除刚刚已经办完的主任务自身
                if (!duplicateTask.getId().equals(currentTask.getId())) {

                    // 3. 构造系统代办的流转意见，留下完美的审计记录
                    String mergeComment = String.format("系统识别到多路流转重复派发，已自动跟随主任务 [%s] 合并%s",
                            currentTask.getId(), actionName);

                    // 写入流转意见
                    taskService.addComment(duplicateTask.getId(), currentTask.getProcessInstanceId(), commentType, mergeComment);

                    // 4. 连带完成这个重复任务
                    taskService.complete(duplicateTask.getId(), processVariables);
                }
            }
        } catch (Exception e) {
            // 兜底保护：防止 Flowable 引擎底层会签机制自动销毁任务导致的报错
            log.warn("尝试合并重复任务时发生异常（任务可能已被引擎自动回收）: {}", e.getMessage());
        }
    }

    /**
     * 审批被委派的任务
     *
     * @param reqVO 前端请求参数，包含当前任务ID，审批意见等
     * @param task  当前被审批的任务
     */
    private void approveDelegateTask(BpmTaskApproveReqVO reqVO, Task task) {
        // 1. 添加审批意见
        AdminUserRespDTO currentUser = adminUserApi.getUser(WebFrameworkUtils.getLoginUserId());
        AdminUserRespDTO ownerUser = adminUserApi.getUser(NumberUtils.parseLong(task.getOwner())); // 发起委托的用户
        Assert.notNull(ownerUser, "委派任务找不到原审批人，需要检查数据");
        taskService.addComment(reqVO.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.DELEGATE_END.getType(),
                BpmCommentTypeEnum.DELEGATE_END.formatComment(currentUser.getNickname(), ownerUser.getNickname(), reqVO.getReason()));

        // 2.1 调用 resolveTask 完成任务。
        // 底层调用 TaskHelper.changeTaskAssignee(task, task.getOwner())：将 owner 设置为 assignee
        taskService.resolveTask(task.getId());
        // 2.2 更新 task 状态 + 原因
        updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.RUNNING.getStatus(), reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void rejectTask(Long userId, @Valid BpmTaskRejectReqVO reqVO) {
        // 1.1 校验任务存在
        Task task = validateTask(userId, reqVO.getId());
        // 1.2 校验流程实例存在
        ProcessInstance instance = processInstanceService.getProcessInstance(task.getProcessInstanceId());
        if (instance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }

        // 2.1 更新流程任务为不通过
        updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.REJECT.getStatus(), reqVO.getReason());
        // 2.2 添加流程评论
        taskService.addComment(task.getId(), task.getProcessInstanceId(), BpmCommentTypeEnum.REJECT.getType(),
                BpmCommentTypeEnum.REJECT.formatComment(reqVO.getReason()));
        // 2.3 如果当前任务时被加签的，则加它的根任务也标记成未通过
        // 疑问：为什么要标记未通过呢？
        // 回答：例如说 A 任务被向前加签除 B 任务时，B 任务被审批不通过，此时 A 会被取消。而 yudao-ui-admin-vue3 不展示“已取消”的任务，导致展示不出审批不通过的细节。
        if (task.getParentTaskId() != null) {
            String rootParentId = getTaskRootParentId(task);
            updateTaskStatusAndReason(rootParentId, BpmTaskStatusEnum.REJECT.getStatus(),
                    BpmCommentTypeEnum.REJECT.formatComment("加签任务不通过"));
            taskService.addComment(rootParentId, task.getProcessInstanceId(), BpmCommentTypeEnum.REJECT.getType(),
                    BpmCommentTypeEnum.REJECT.formatComment("加签任务不通过"));
        }

        // 3. 根据不同的 RejectHandler 处理策略
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        FlowElement userTaskElement = BpmnModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        // 3.1 情况一：驳回到指定的任务节点
        BpmUserTaskRejectHandlerTypeEnum userTaskRejectHandlerType = BpmnModelUtils.parseRejectHandlerType(userTaskElement);
        if (userTaskRejectHandlerType == BpmUserTaskRejectHandlerTypeEnum.RETURN_USER_TASK) {
            String returnTaskId = BpmnModelUtils.parseReturnTaskId(userTaskElement);
            Assert.notNull(returnTaskId, "退回的节点不能为空");
            returnTask(userId, new BpmTaskReturnReqVO().setId(task.getId())
                    .setTargetTaskDefinitionKey(returnTaskId).setReason(reqVO.getReason()));
            return;
        }

        // 3.2 情况二： 标记流程为不通过并结束流程
        processInstanceService.updateProcessInstanceReject(instance, reqVO.getReason()); // 标记不通过
        moveTaskToEnd(task.getProcessInstanceId(), BpmCommentTypeEnum.REJECT.formatComment(reqVO.getReason())); // 结束流程
    }

    /**
     * 更新流程任务的 status 状态
     *
     * @param id     任务编号
     * @param status 状态
     */
    private void updateTaskStatus(String id, Integer status) {
        taskService.setVariableLocal(id, BpmnVariableConstants.TASK_VARIABLE_STATUS, status);
    }

    /**
     * 更新流程任务的 status 状态、reason 理由
     *
     * @param id     任务编号
     * @param status 状态
     * @param reason 理由（审批通过、审批不通过的理由）
     */
    private void updateTaskStatusAndReason(String id, Integer status, String reason) {
        updateTaskStatus(id, status);
        taskService.setVariableLocal(id, BpmnVariableConstants.TASK_VARIABLE_REASON, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void returnTask(Long userId, BpmTaskReturnReqVO reqVO) {
        // 1.1 当前任务 task
        Task task = validateTask(userId, reqVO.getId());
        if (task.isSuspended()) {
            throw exception(TASK_IS_PENDING);
        }
        // 1.2 获取流程模型信息
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        // 1.3 校验源头和目标节点的关系，并返回目标元素
        FlowElement targetElement = validateTargetTaskCanReturn(bpmnModel, task.getTaskDefinitionKey(),
                reqVO.getTargetTaskDefinitionKey());

        // 2. 调用 Flowable 框架的退回逻辑
        returnTask(userId, bpmnModel, task, targetElement, reqVO);
    }

    /**
     * 退回流程节点时，校验目标任务节点是否可退回
     *
     * @param bpmnModel 流程模型
     * @param sourceKey 当前任务节点 Key
     * @param targetKey 目标任务节点 key
     * @return 目标任务节点元素
     */
    private FlowElement validateTargetTaskCanReturn(BpmnModel bpmnModel, String sourceKey, String targetKey) {
        // 1.1 获取当前任务节点元素
        FlowElement source = BpmnModelUtils.getFlowElementById(bpmnModel, sourceKey);
        // 1.2 获取跳转的节点元素
        FlowElement target = BpmnModelUtils.getFlowElementById(bpmnModel, targetKey);
        if (target == null) {
            throw exception(TASK_TARGET_NODE_NOT_EXISTS);
        }

        // 2. 只有串行可到达的节点，才可以退回。类似非串行、子流程无法退回
        if (!BpmnModelUtils.isSequentialReachable(source, target, null)) {
            throw exception(TASK_RETURN_FAIL_SOURCE_TARGET_ERROR);
        }
        return target;
    }

    /**
     * 执行退回逻辑
     *
     * @param userId        用户编号
     * @param bpmnModel     流程模型
     * @param currentTask   当前退回的任务
     * @param targetElement 需要退回到的目标任务
     * @param reqVO         前端参数封装
     */
    /**
     * 执行退回逻辑（精准分支隔离 + 防重复数据版）
     */
    /**
     * 执行退回逻辑（精准分支隔离 + 防重复数据 + 精准追溯历史办理人）
     *
     * @param userId        用户编号
     * @param bpmnModel     流程模型
     * @param currentTask   当前退回的任务
     * @param targetElement 需要退回到的目标任务
     * @param reqVO         前端参数封装
     */
    /**
     * 执行退回逻辑（精准分支隔离 + 防重复数据 + 精准追溯历史办理人）
     *
     * @param userId        用户编号
     * @param bpmnModel     流程模型
     * @param currentTask   当前退回的任务
     * @param targetElement 需要退回到的目标任务
     * @param reqVO         前端参数封装
     */
    /**
     * 执行退回逻辑（精准分支隔离 + 防重复数据 + 精准追溯历史办理人 + 并行分支防误杀）
     *
     * @param userId        用户编号
     * @param bpmnModel     流程模型
     * @param currentTask   当前退回的任务
     * @param targetElement 需要退回到的目标任务
     * @param reqVO         前端参数封装
     */
    public void returnTask(Long userId, BpmnModel bpmnModel, Task currentTask, FlowElement targetElement, BpmTaskReturnReqVO reqVO) {
        String processInstanceId = currentTask.getProcessInstanceId();
        String targetTaskKey = reqVO.getTargetTaskDefinitionKey();

        // 1. 构建需要预测的任务流程变量
        Set<String> needSimulateTaskDefinitionKeys = getNeedSimulateTaskDefinitionKeys(bpmnModel, currentTask, targetElement);

        // 2. 获取当前流程下所有处于 Active 状态的任务
        List<Task> allActiveTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).active().list();
        List<String> runTaskKeyList = allActiveTasks.stream()
                .map(Task::getTaskDefinitionKey).distinct().collect(Collectors.toList());

        // 3. 利用原有的拓扑算法，找出在 targetElement 之后的任务节点 (此时可能包含平行的误杀节点)
        List<UserTask> downstreamUserTasks = BpmnModelUtils.iteratorFindChildUserTasks(targetElement, runTaskKeyList, null, null);
        List<String> returnTaskKeyList = downstreamUserTasks.stream().map(UserTask::getId).distinct().collect(Collectors.toList());

        // ================== 【修复 BUG：新增运行时执行流血统校验 + 时间戳防误杀 + 同源分身合并】 ==================

        // 3.1 获取当前任务 (例如:局领导批示) 的完整 Execution 血统 (向上溯源，用于后续同源比对)
        Set<String> currentExecutionLineage = new HashSet<>();
        String traceExecId = currentTask.getExecutionId();
        while (cn.hutool.core.util.StrUtil.isNotBlank(traceExecId)) {
            currentExecutionLineage.add(traceExecId);
            org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery()
                    .executionId(traceExecId).singleResult();
            if (exec != null) {
                traceExecId = exec.getParentId();
            } else {
                break;
            }
        }

        // 3.2 找到目标退回节点 (例如:局长批示) 在历史中的执行流基座与【办结时间】
        String targetNodeExecutionId = null;
        Date targetTaskEndTime = null;
        for (String execId : currentExecutionLineage) {
            List<HistoricTaskInstance> historyTasks = historyService.createHistoricTaskInstanceQuery()
                    .executionId(execId)
                    .taskDefinitionKey(targetTaskKey)
                    .finished()
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .list();
            if (CollUtil.isNotEmpty(historyTasks)) {
                targetNodeExecutionId = execId;
                targetTaskEndTime = historyTasks.get(0).getEndTime();
                break;
            }
        }

        // 兜底查找: 如果底层执行流复用断层导致找不到，直接去全局历史表里找最近的那次目标节点办结记录
        if (targetTaskEndTime == null) {
            List<HistoricTaskInstance> globalHistoryTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(targetTaskKey)
                    .finished()
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .listPage(0, 1);
            if (CollUtil.isNotEmpty(globalHistoryTasks)) {
                targetNodeExecutionId = globalHistoryTasks.get(0).getExecutionId();
                targetTaskEndTime = globalHistoryTasks.get(0).getEndTime();
            }
        }

        // 3.3 过滤出真正需要被撤销的 Task（仅限当前分支及其下游衍生的分支，绝对防误杀平行分支）
        List<Task> tasksToCancel = new ArrayList<>();
        Map<String, String> currentReturnBranchContext = getReturnBranchContext(processInstanceId, currentTask.getExecutionId());
        String targetReturnBatchId = currentReturnBranchContext.get(targetTaskKey);
        for (Task activeTask : allActiveTasks) {
            if (!returnTaskKeyList.contains(activeTask.getTaskDefinitionKey())) {
                continue;
            }

            // 永远连带撤销当前正在操作的任务本身
            if (activeTask.getId().equals(currentTask.getId())) {
                tasksToCancel.add(activeTask);
                continue;
            }

            // 新流转数据优先按退回批次判断：同一个目标节点、同一个批次的下游活跃任务必须一起退回。
            if (StrUtil.isNotBlank(targetReturnBatchId)) {
                Map<String, String> activeReturnBranchContext = getReturnBranchContext(processInstanceId, activeTask.getExecutionId());
                String activeTargetBatchId = activeReturnBranchContext.get(targetTaskKey);
                if (StrUtil.equals(targetReturnBatchId, activeTargetBatchId)) {
                    tasksToCancel.add(activeTask);
                    continue;
                }
                if (StrUtil.isNotBlank(activeTargetBatchId)) {
                    continue;
                }
            }

            // 【核心防御 1：时间戳物理防误杀】
            // 如果活跃任务(主办/协办)的创建时间，早于目标节点(局长)的办结时间
            // 证明它们是之前并行网关同时派发出来的【平行兄弟】，直接护盾放过！
            if (targetTaskEndTime != null) {
                long timeDiff = activeTask.getCreateTime().getTime() - targetTaskEndTime.getTime();
                if (timeDiff < -1000) { // 容忍 2000ms 事务并发延迟
                    continue;
                }
                // 同一个任务节点多条出线时，多个兄弟分支不是 execution 父子关系，血统判断抓不到。
                // 只要它是目标节点本次流出后同批创建的下游活跃任务，就应该随当前分支一起退回。
                if (timeDiff <= 2000) {
                    tasksToCancel.add(activeTask);
                    continue;
                }
            }

            // 【核心防御 2：血统精准判定】
            boolean isDescendantOfTarget = false;
            String taskExecId = activeTask.getExecutionId();

            if (targetNodeExecutionId != null) {
                // 场景 A：能找到目标节点的底层执行流（单实例节点通常能找到）
                while (cn.hutool.core.util.StrUtil.isNotBlank(taskExecId)) {
                    if (taskExecId.equals(targetNodeExecutionId)) {
                        isDescendantOfTarget = true;
                        break;
                    }
                    org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery()
                            .executionId(taskExecId).singleResult();
                    taskExecId = exec != null ? exec.getParentId() : null;
                }
            } else {
                // 场景 B：找不到目标执行流（因为目标是多实例节点，底层被物理销毁）
                // 此时判断 activeTask 是否与 currentTask 属于“同一颗局部树”
                while (cn.hutool.core.util.StrUtil.isNotBlank(taskExecId) && !taskExecId.equals(processInstanceId)) {
                    // 只要跟 currentTask 的血统有交集（排除整个流程的 Root ID），就说明是多实例的另一个分身
                    if (currentExecutionLineage.contains(taskExecId)) {
                        isDescendantOfTarget = true;
                        break;
                    }
                    org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery()
                            .executionId(taskExecId).singleResult();
                    taskExecId = exec != null ? exec.getParentId() : null;
                }
            }

            if (isDescendantOfTarget) {
                tasksToCancel.add(activeTask);
            }
        }
        // ===============================================================================================

        List<String> executionIdsToMove = new ArrayList<>();

        // 4. 逐个处理需要撤销的任务，并精确收集它们的根 Execution (杜绝重复生成数据的关键)
        for (Task task : tasksToCancel) {
            // 4.1 添加审批意见和状态
            if (task.getId().equals(currentTask.getId())) {
                taskService.addComment(task.getId(), processInstanceId, BpmCommentTypeEnum.RETURN.getType(),
                        BpmCommentTypeEnum.RETURN.formatComment(reqVO.getReason()));
                updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.RETURN.getStatus(), reqVO.getReason());
            } else {
                String cancelComment = String.format("因同分支节点退回至[%s]，本任务自动撤销。", targetElement.getName());
                taskService.addComment(task.getId(), processInstanceId, BpmCommentTypeEnum.CANCEL.getType(), cancelComment);
                processTaskCanceled(task.getId()); // 标记为取消
            }

            // 4.2 防重复数据核心机制：寻找多实例根节点 (MI Root)
            org.flowable.engine.runtime.Execution taskExecution = runtimeService.createExecutionQuery()
                    .executionId(task.getExecutionId()).singleResult();
            String execIdToMove = taskExecution.getId();

            if (taskExecution.getParentId() != null) {
                org.flowable.engine.runtime.Execution parentExecution = runtimeService.createExecutionQuery()
                        .executionId(taskExecution.getParentId()).singleResult();
                // 如果父节点有 nrOfInstances 变量，说明它是多实例的包裹容器，必须拽着容器走！
                if (parentExecution != null && runtimeService.hasVariableLocal(parentExecution.getId(), "nrOfInstances")) {
                    execIdToMove = parentExecution.getId();
                }
            }

            // 去重收集 executionId
            if (!executionIdsToMove.contains(execIdToMove)) {
                executionIdsToMove.add(execIdToMove);
            }
        }

        // =========================================================================
        // 5. “精准追溯历史处理人”防污染血统算法
        // =========================================================================
        List<Long> actualReturnAssignees = reqVO.getReturnAssignees();

        if (CollUtil.isEmpty(actualReturnAssignees)) {
            actualReturnAssignees = new ArrayList<>();

            // 1) 递归获取当前任务所在分支的所有“祖先执行流 ID” (防跨分支污染)
            List<String> executionLineage = new ArrayList<>();
            String currentExecId = currentTask.getExecutionId();
            while (cn.hutool.core.util.StrUtil.isNotBlank(currentExecId)) {
                executionLineage.add(currentExecId);
                // 去运行表中找父级 Execution
                org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery()
                        .executionId(currentExecId)
                        .singleResult();
                if (exec != null) {
                    currentExecId = exec.getParentId(); // 向上溯源
                } else {
                    // 历史表兜底
                    org.flowable.engine.history.HistoricActivityInstance historicExec = historyService.createHistoricActivityInstanceQuery()
                            .executionId(currentExecId)
                            .listPage(0, 1)
                            .stream().findFirst().orElse(null);
                    break;
                }
            }

            // 2) 利用血统 ID 去历史任务表中精准找人
            List<HistoricTaskInstance> targetHistoryTasks = new ArrayList<>();
            for (String execId : executionLineage) {
                targetHistoryTasks = historyService.createHistoricTaskInstanceQuery()
                        .executionId(execId) // 【绝对隔离】：只查当前族谱上的分支！
                        .taskDefinitionKey(reqVO.getTargetTaskDefinitionKey())
                        .finished()
                        .orderByHistoricTaskInstanceEndTime().desc()
                        .list();

                // 只要在这个树枝上找到了，说明是最近的历史，立马停止！
                if (CollUtil.isNotEmpty(targetHistoryTasks)) {
                    break;
                }
            }

            // 3) 极小概率兜底：目标节点可能在拆分前的主干上
            if (CollUtil.isEmpty(targetHistoryTasks)) {
                targetHistoryTasks = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(currentTask.getProcessInstanceId())
                        .taskDefinitionKey(reqVO.getTargetTaskDefinitionKey())
                        .finished()
                        .orderByHistoricTaskInstanceEndTime().desc()
                        .listPage(0, 1);
            }

            // 4) 提取同批次的人员 (容差 2000 毫秒合并多实例人员)
            if (CollUtil.isNotEmpty(targetHistoryTasks)) {
                long baselineTime = targetHistoryTasks.get(0).getEndTime().getTime();
                Set<Long> cleanAssignees = new LinkedHashSet<>();
                for (HistoricTaskInstance ht : targetHistoryTasks) {
                    if (Math.abs(ht.getEndTime().getTime() - baselineTime) <= 1000) {
                        if (cn.hutool.core.util.StrUtil.isNotBlank(ht.getAssignee())) {
                            cleanAssignees.add(Long.valueOf(ht.getAssignee()));
                        }
                    } else {
                        break;
                    }
                }
                actualReturnAssignees.addAll(cleanAssignees);
            }
        }

        // 把洗干净的名单强行覆盖到底层，彻底抹杀其他分支残留！
        if (CollUtil.isNotEmpty(actualReturnAssignees)) {
            // 1) 安全覆写全局记忆 Map (防御 ClassCastException)
            Object lastNodeObj = runtimeService.getVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
            Map<String, List<Long>> lastNodeMap = new HashMap<>();
            if (lastNodeObj instanceof Map) {
                Map<?, ?> tempMap = (Map<?, ?>) lastNodeObj;
                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        Object val = entry.getValue();
                        if (val instanceof List) {
                            List<Long> userIds = new ArrayList<>();
                            for (Object item : (List<?>) val) {
                                userIds.add(Long.valueOf(item.toString()));
                            }
                            lastNodeMap.put(String.valueOf(entry.getKey()), userIds);
                        }
                    }
                }
            }
            lastNodeMap.put(targetTaskKey, actualReturnAssignees);
            runtimeService.setVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, lastNodeMap);

            // 2) 安全更新时间戳
            Object updateDateObj = runtimeService.getVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME);
            Map<String, Long> updateDateMap = new HashMap<>();
            if (updateDateObj instanceof Map) {
                Map<?, ?> tempMap = (Map<?, ?>) updateDateObj;
                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        updateDateMap.put(String.valueOf(entry.getKey()), Long.valueOf(entry.getValue().toString()));
                    }
                }
            }
            updateDateMap.put(targetTaskKey, System.currentTimeMillis());
            runtimeService.setVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME, updateDateMap);

            // 3) 防多实例无限裂变：强行覆写底层集合变量
            if (targetElement instanceof org.flowable.bpmn.model.UserTask) {
                org.flowable.bpmn.model.UserTask targetUserTask = (org.flowable.bpmn.model.UserTask) targetElement;
                if (targetUserTask.getLoopCharacteristics() != null && cn.hutool.core.util.StrUtil.isNotBlank(targetUserTask.getLoopCharacteristics().getInputDataItem())) {
                    String collectionVarName = targetUserTask.getLoopCharacteristics().getInputDataItem().replace("${", "").replace("}", "").trim();
                    runtimeService.setVariable(processInstanceId, collectionVarName, actualReturnAssignees);
                }
            }
        }

        // 退回到目标节点前，清理目标节点上一轮审批遗留的出线条件变量。
        // 否则重新审批目标节点时，旧走向和新走向会同时满足，导致退回前的分支再次进入。
        clearReturnTargetOutgoingVariables(processInstanceId, executionIdsToMove, targetElement);
        List<String> targetReturnableTaskPath = trimReturnableTaskPathForReturn(processInstanceId,
                currentTask.getExecutionId(), targetTaskKey);

        // =========================================================================
        // 6. 执行精确驳回（防裂变核心）：移动根 Execution 而不是 ActivityId
        // =========================================================================
        ReturnableTaskPathCarrier previousPathCarrier = RETURNABLE_TASK_PATH_CARRIER.get();
        RETURNABLE_TASK_PATH_CARRIER.set(new ReturnableTaskPathCarrier(processInstanceId, targetReturnableTaskPath));
        try {
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveExecutionsToSingleActivityId(executionIdsToMove, targetTaskKey)
                    .processVariable(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEED_SIMULATE_TASK_IDS, needSimulateTaskDefinitionKeys)
                    .localVariable(targetTaskKey, String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, targetTaskKey), Boolean.TRUE)
                    .changeState();
        } finally {
            if (previousPathCarrier == null) {
                RETURNABLE_TASK_PATH_CARRIER.remove();
            } else {
                RETURNABLE_TASK_PATH_CARRIER.set(previousPathCarrier);
            }
        }

        // 7. 强行覆盖引擎触发监听器分配的默认人 (适用于单节点)
        if (CollUtil.isNotEmpty(actualReturnAssignees)) {
            List<Task> newTargetTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .taskDefinitionKey(targetTaskKey)
                    .active()
                    .list();

            if (newTargetTasks.size() == 1 && actualReturnAssignees.size() == 1) {
                String targetAssignee = String.valueOf(actualReturnAssignees.get(0));
                Task newTargetTask = newTargetTasks.get(0);
                taskService.setAssignee(newTargetTask.getId(), targetAssignee);
                taskService.setOwner(newTargetTask.getId(), targetAssignee);
            }
        }
    }
//    public void returnTask(Long userId, BpmnModel bpmnModel, Task currentTask, FlowElement targetElement, BpmTaskReturnReqVO reqVO) {
//        // 1. 获得所有需要回撤的任务 taskDefinitionKey，用于稍后的 moveActivityIdsToSingleActivityId 回撤
//        // 1.1 获取所有正常进行的任务节点 Key
//        List<Task> taskList = taskService.createTaskQuery().processInstanceId(currentTask.getProcessInstanceId()).list();
//        List<String> runTaskKeyList = convertList(taskList, Task::getTaskDefinitionKey);
//        // 1.2 通过 targetElement 的出口连线，计算在 runTaskKeyList 有哪些 key 需要被撤回
//        // 为什么不直接使用 runTaskKeyList 呢？因为可能存在多个审批分支，例如说：A -> B -> C 和 D -> F，而只要 C 撤回到 A，需要排除掉 F
//        List<UserTask> returnUserTaskList = BpmnModelUtils.iteratorFindChildUserTasks(targetElement, runTaskKeyList, null, null);
//        List<String> returnTaskKeyList = convertList(returnUserTaskList, UserTask::getId);
//
//        List<String> runExecutionIds = new ArrayList<>();
//        // 2. 给当前要被退回的 task 数组，设置退回意见
//        taskList.forEach(task -> {
//            // 需要排除掉，不需要设置退回意见的任务
//            if (!returnTaskKeyList.contains(task.getTaskDefinitionKey())) {
//                return;
//            }
//            if (task.getExecutionId() != null) {
//                runExecutionIds.add(task.getExecutionId());
//            }
//
//            // 判断是否分配给自己任务，因为会签任务，一个节点会有多个任务
//            if (isAssignUserTask(userId, task)) { // 情况一：自己的任务，进行 RETURN 标记
//                // 2.1.1 添加评论
//                taskService.addComment(task.getId(), currentTask.getProcessInstanceId(), BpmCommentTypeEnum.RETURN.getType(),
//                        BpmCommentTypeEnum.RETURN.formatComment(reqVO.getReason()));
//                // 2.1.2 更新 task 状态 + 原因
//                updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.RETURN.getStatus(), reqVO.getReason());
//            } else { // 情况二：别人的任务，进行 CANCEL 标记
//                processTaskCanceled(task.getId());
//            }
//        });
//
//        // 3. 构建需要预测的任务流程变量
//        Set<String> needSimulateTaskDefinitionKeys = getNeedSimulateTaskDefinitionKeys(bpmnModel, currentTask, targetElement);
//
//
//        List<Long> actualReturnAssignees = reqVO.getReturnAssignees();
//
//        if (CollUtil.isEmpty(actualReturnAssignees)) {
//            actualReturnAssignees = new ArrayList<>();
//
//            // 1) 递归获取当前任务所在分支的所有“祖先执行流 ID” (防跨分支污染)
//            List<String> executionLineage = new ArrayList<>();
//            String currentExecId = currentTask.getExecutionId();
//            while (cn.hutool.core.util.StrUtil.isNotBlank(currentExecId)) {
//                executionLineage.add(currentExecId);
//                // 去运行表中找父级 Execution
//                org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery()
//                        .executionId(currentExecId)
//                        .singleResult();
//                if (exec != null) {
//                    currentExecId = exec.getParentId(); // 向上溯源
//                } else {
//                    // 历史表兜底
//                    org.flowable.engine.history.HistoricActivityInstance historicExec = historyService.createHistoricActivityInstanceQuery()
//                            .executionId(currentExecId)
//                            .listPage(0, 1)
//                            .stream().findFirst().orElse(null);
//                    break;
//                }
//            }
//
//            // 2) 利用血统 ID 去历史任务表中精准找人
//            List<HistoricTaskInstance> targetHistoryTasks = new ArrayList<>();
//            for (String execId : executionLineage) {
//                targetHistoryTasks = historyService.createHistoricTaskInstanceQuery()
//                        .executionId(execId) // 【绝对隔离】：只查当前族谱上的分支！
//                        .taskDefinitionKey(reqVO.getTargetTaskDefinitionKey())
//                        .finished()
//                        .orderByHistoricTaskInstanceEndTime().desc()
//                        .list();
//
//                // 只要在这个树枝上找到了，说明是最近的历史，立马停止！
//                if (CollUtil.isNotEmpty(targetHistoryTasks)) {
//                    break;
//                }
//            }
//
//            // 3) 极小概率兜底：目标节点可能在拆分前的主干上
//            if (CollUtil.isEmpty(targetHistoryTasks)) {
//                targetHistoryTasks = historyService.createHistoricTaskInstanceQuery()
//                        .processInstanceId(currentTask.getProcessInstanceId())
//                        .taskDefinitionKey(reqVO.getTargetTaskDefinitionKey())
//                        .finished()
//                        .orderByHistoricTaskInstanceEndTime().desc()
//                        .listPage(0, 1);
//            }
//
//            // 4) 提取同批次的人员 (容差 2000 毫秒合并多实例人员)
//            if (CollUtil.isNotEmpty(targetHistoryTasks)) {
//                long baselineTime = targetHistoryTasks.get(0).getEndTime().getTime();
//                Set<Long> cleanAssignees = new LinkedHashSet<>();
//                for (HistoricTaskInstance ht : targetHistoryTasks) {
//                    if (Math.abs(ht.getEndTime().getTime() - baselineTime) <= 2000) {
//                        if (cn.hutool.core.util.StrUtil.isNotBlank(ht.getAssignee())) {
//                            cleanAssignees.add(Long.valueOf(ht.getAssignee()));
//                        }
//                    } else {
//                        break;
//                    }
//                }
//                actualReturnAssignees.addAll(cleanAssignees);
//            }
//        }
//
//        // 把洗干净的名单强行覆盖到底层，彻底抹杀其他分支残留！
//        if (CollUtil.isNotEmpty(actualReturnAssignees)) {
//            String targetKey = reqVO.getTargetTaskDefinitionKey();
//            String processInstanceId = currentTask.getProcessInstanceId();
//
//            // 1) 安全覆写全局记忆 Map (防御 ClassCastException)
//            Object lastNodeObj = runtimeService.getVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
//            Map<String, List<Long>> lastNodeMap = new HashMap<>();
//            if (lastNodeObj instanceof Map) {
//                Map<?, ?> tempMap = (Map<?, ?>) lastNodeObj;
//                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
//                    if (entry.getKey() != null && entry.getValue() != null) {
//                        Object val = entry.getValue();
//                        if (val instanceof List) {
//                            List<Long> userIds = new ArrayList<>();
//                            for (Object item : (List<?>) val) {
//                                userIds.add(Long.valueOf(item.toString()));
//                            }
//                            lastNodeMap.put(String.valueOf(entry.getKey()), userIds);
//                        }
//                    }
//                }
//            }
//            lastNodeMap.put(targetKey, actualReturnAssignees);
//            runtimeService.setVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, lastNodeMap);
//
//            // 2) 安全更新时间戳
//            Object updateDateObj = runtimeService.getVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME);
//            Map<String, Long> updateDateMap = new HashMap<>();
//            if (updateDateObj instanceof Map) {
//                Map<?, ?> tempMap = (Map<?, ?>) updateDateObj;
//                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
//                    if (entry.getKey() != null && entry.getValue() != null) {
//                        updateDateMap.put(String.valueOf(entry.getKey()), Long.valueOf(entry.getValue().toString()));
//                    }
//                }
//            }
//            updateDateMap.put(targetKey, System.currentTimeMillis());
//            runtimeService.setVariable(processInstanceId, BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_UPDATE_TIME, updateDateMap);
//
//            // 3) 防多实例无限裂变：强行覆写底层集合变量
//            if (targetElement instanceof org.flowable.bpmn.model.UserTask) {
//                org.flowable.bpmn.model.UserTask targetUserTask = (org.flowable.bpmn.model.UserTask) targetElement;
//                if (targetUserTask.getLoopCharacteristics() != null && cn.hutool.core.util.StrUtil.isNotBlank(targetUserTask.getLoopCharacteristics().getInputDataItem())) {
//                    String collectionVarName = targetUserTask.getLoopCharacteristics().getInputDataItem().replace("${", "").replace("}", "").trim();
//                    runtimeService.setVariable(processInstanceId, collectionVarName, actualReturnAssignees);
//                }
//            }
//        }
//        // 4. 执行驳回
//        // ① 使用 moveExecutionsToSingleActivityId 替换 moveActivityIdsToSingleActivityId。原因：当多实例任务回退的时候有问题。
//        //    相关 issue: https://github.com/flowable/flowable-engine/issues/3944
//        // ② flowable 7.2.0 版本后，继续使用 moveActivityIdsToSingleActivityId 方法。原因：flowable 7.2.0 版本修复了该问题。
//        //    相关 issue：https://github.com/YunaiV/ruoyi-vue-pro/issues/1018
//        runtimeService.createChangeActivityStateBuilder()
//                .processInstanceId(currentTask.getProcessInstanceId())
//                .moveActivityIdsToSingleActivityId(returnTaskKeyList, reqVO.getTargetTaskDefinitionKey())
//                // 设置需要预测的任务 ids 的流程变量，用于辅助预测
//                .processVariable(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEED_SIMULATE_TASK_IDS, needSimulateTaskDefinitionKeys)
//                // 设置流程变量（local）节点退回标记, 用于退回到节点，不执行 BpmUserTaskAssignStartUserHandlerTypeEnum 策略，导致自动通过
//                .localVariable(reqVO.getTargetTaskDefinitionKey(),
//                        String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, reqVO.getTargetTaskDefinitionKey()), Boolean.TRUE)
//                .changeState();
//
//        if (CollUtil.isNotEmpty(actualReturnAssignees)) {
//            List<Task> newTargetTasks = taskService.createTaskQuery()
//                    .processInstanceId(currentTask.getProcessInstanceId())
//                    .taskDefinitionKey(reqVO.getTargetTaskDefinitionKey())
//                    .active()
//                    .list();
//
//            // 如果是单节点 (非多实例)，强行覆盖引擎触发监听器分配的默认人
//            if (newTargetTasks.size() == 1 && actualReturnAssignees.size() == 1) {
//                String targetAssignee = String.valueOf(actualReturnAssignees.get(0));
//                Task newTargetTask = newTargetTasks.get(0);
//                taskService.setAssignee(newTargetTask.getId(), targetAssignee);
//                taskService.setOwner(newTargetTask.getId(), targetAssignee);
//            }
//        }
//    }


    private List<Long> getActualReturnAssignees(Task currentTask, BpmTaskReturnReqVO reqVO) {
        List<Long> actualReturnAssignees = reqVO.getReturnAssignees();
        if (CollUtil.isNotEmpty(actualReturnAssignees)) return actualReturnAssignees;

        actualReturnAssignees = new ArrayList<>();
        // 递归获取当前分支的所有祖先执行流 ID (隔离分支)
        List<String> executionLineage = new ArrayList<>();
        String currentExecId = currentTask.getExecutionId();
        while (StrUtil.isNotBlank(currentExecId)) {
            executionLineage.add(currentExecId);
            org.flowable.engine.runtime.Execution exec = runtimeService.createExecutionQuery().executionId(currentExecId).singleResult();
            if (exec != null) currentExecId = exec.getParentId();
            else break;
        }

        List<HistoricTaskInstance> targetHistoryTasks = new ArrayList<>();
        for (String execId : executionLineage) {
            targetHistoryTasks = historyService.createHistoricTaskInstanceQuery()
                    .executionId(execId).taskDefinitionKey(reqVO.getTargetTaskDefinitionKey())
                    .finished().orderByHistoricTaskInstanceEndTime().desc().list();
            if (CollUtil.isNotEmpty(targetHistoryTasks)) break;
        }

        if (CollUtil.isNotEmpty(targetHistoryTasks)) {
            long baselineTime = targetHistoryTasks.get(0).getEndTime().getTime();
            for (HistoricTaskInstance ht : targetHistoryTasks) {
                if (Math.abs(ht.getEndTime().getTime() - baselineTime) <= 2000 && StrUtil.isNotBlank(ht.getAssignee())) {
                    actualReturnAssignees.add(Long.valueOf(ht.getAssignee()));
                } else break;
            }
        }
        return actualReturnAssignees;
    }

    private void handleAssigneeAfterReturn(Task currentTask, String targetKey, FlowElement targetElement, List<Long> actualReturnAssignees) {
        if (CollUtil.isEmpty(actualReturnAssignees)) return;
        String processInstanceId = currentTask.getProcessInstanceId();

        // 更新变量池逻辑（省略具体 Map 转换，使用您原文件 864-897 行逻辑）
        // ... 此处保留您原有的变量回写逻辑 ...

        // 强行覆盖新生成的任务处理人
        List<Task> newTasks = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey(targetKey).active().list();
        if (newTasks.size() == 1 && actualReturnAssignees.size() == 1) {
            taskService.setAssignee(newTasks.get(0).getId(), String.valueOf(actualReturnAssignees.get(0)));
            taskService.setOwner(newTasks.get(0).getId(), String.valueOf(actualReturnAssignees.get(0)));
        }
    }

    public void executeSmartReturn(Task currentTask, String targetTaskKey, BpmTaskReturnReqVO reqVO, BpmnModel bpmnModel) {
        String processInstanceId = currentTask.getProcessInstanceId();
        String sourceTaskKey = currentTask.getTaskDefinitionKey();

        // 1. 图结构侦测：判断目标节点(退回终点)到当前节点(退回起点)之间，是否包含会导致裂变的网关或并行拆分
        boolean isCrossParallel = checkCrossParallelSplit(bpmnModel, targetTaskKey, sourceTaskKey);

        if (isCrossParallel) {
            log.info("[智能退回] 检测到跨越并行/相容分支，触发【全局退回】策略。");

            // 【全局退回策略】：查出当前流程下所有处于 Active 状态的任务，一并撤销
            List<Task> allActiveTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .list();

            // 提取所有正在运行的节点 Key（去重）
            List<String> allActiveActivityIds = allActiveTasks.stream()
                    .map(Task::getTaskDefinitionKey)
                    .distinct()
                    .collect(Collectors.toList());

            // 给所有被连带取消的任务写上审批意见
            for (Task activeTask : allActiveTasks) {
                String comment = String.format("流程被驳回至[%s]，当前分支自动撤销。原因：%s", targetTaskKey, reqVO.getReason());
                taskService.addComment(activeTask.getId(), processInstanceId, BpmCommentTypeEnum.RETURN.getType(), comment);
                // 更新本地变量状态为取消或退回
                updateTaskStatusAndReason(activeTask.getId(), BpmTaskStatusEnum.RETURN.getStatus(), reqVO.getReason());
            }

            // 执行一锅端退回：将所有活跃分支全部拖回目标节点
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveActivityIdsToSingleActivityId(allActiveActivityIds, targetTaskKey)
                    // 写入退回标记等变量
                    .localVariable(targetTaskKey, String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, targetTaskKey), Boolean.TRUE)
                    .changeState();

        } else {
            log.info("[智能退回] 线性安全路径，触发【单线退回】策略。");

            // 【单线退回策略】：只退回当前执行流，不影响其他分支
            // 注意：必须处理多实例根节点 (MI Root)
            org.flowable.engine.runtime.Execution taskExecution = runtimeService.createExecutionQuery()
                    .executionId(currentTask.getExecutionId())
                    .singleResult();

            String executionIdToMove = taskExecution.getId();

            // 向上找寻多实例根节点
            if (taskExecution.getParentId() != null) {
                org.flowable.engine.runtime.Execution parentExecution = runtimeService.createExecutionQuery()
                        .executionId(taskExecution.getParentId())
                        .singleResult();
                // 如果父执行流身上有 nrOfInstances，说明它是会签/或签的根容器！必须拽着根容器退回！
                if (parentExecution != null && runtimeService.hasVariableLocal(parentExecution.getId(), "nrOfInstances")) {
                    executionIdToMove = parentExecution.getId();
                }
            }

            // 添加当前任务的退回意见
            taskService.addComment(currentTask.getId(), processInstanceId, BpmCommentTypeEnum.RETURN.getType(), reqVO.getReason());
            updateTaskStatusAndReason(currentTask.getId(), BpmTaskStatusEnum.RETURN.getStatus(), reqVO.getReason());

            // 执行单体执行流的移动
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(processInstanceId)
                    .moveExecutionToActivityId(executionIdToMove, targetTaskKey)
                    .localVariable(targetTaskKey, String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, targetTaskKey), Boolean.TRUE)
                    .changeState();
        }
    }

    private boolean checkCrossParallelSplit(BpmnModel bpmnModel, String targetTaskKey, String sourceTaskKey) {
        FlowElement targetElement = bpmnModel.getFlowElement(targetTaskKey);
        if (!(targetElement instanceof FlowNode)) {
            return false;
        }

        // BFS 遍历队列
        Queue<FlowNode> queue = new LinkedList<>();
        // 记录已访问节点，防死循环
        Set<String> visited = new HashSet<>();

        queue.offer((FlowNode) targetElement);
        visited.add(targetTaskKey);

        while (!queue.isEmpty()) {
            FlowNode currentNode = queue.poll();

            // 1. 如果在到达 sourceTaskKey 之前遇到了并行网关或相容网关，直接报警！
            if (currentNode instanceof ParallelGateway || currentNode instanceof InclusiveGateway) {
                return true;
            }

            // 2. 隐式并行检测：如果一个普通节点（包括 UserTask）直接画了 >1 条的出线，且它不是源节点，也视为并行拆分！
            // 在你的 XML 中，"主任拟办" 就是典型的这种节点
            List<SequenceFlow> outgoingFlows = currentNode.getOutgoingFlows();
            if (outgoingFlows.size() > 1 && !currentNode.getId().equals(sourceTaskKey) && !currentNode.getId().equals(targetTaskKey)) {
                // 注意：有时候排他网关 (ExclusiveGateway) 也有多条出线，但它不是并行，可以放过。
                // 但如果是非排他网关（如 UserTask 直接出多根线），在 Flowable 中相当于并行执行，必须拦截。
                if (!(currentNode instanceof org.flowable.bpmn.model.ExclusiveGateway)) {
                    return true;
                }
            }

            // 遍历下一层节点
            for (SequenceFlow flow : outgoingFlows) {
                FlowElement nextElement = flow.getTargetFlowElement();
                if (nextElement instanceof FlowNode) {
                    FlowNode nextNode = (FlowNode) nextElement;

                    // 如果找到了当前退回发起的节点，说明这条路径走通了
                    if (nextNode.getId().equals(sourceTaskKey)) {
                        // 找到源头，不再沿着这条路往下找
                        continue;
                    }

                    if (!visited.contains(nextNode.getId())) {
                        visited.add(nextNode.getId());
                        queue.offer(nextNode);
                    }
                }
            }
        }

        return false; // 安全走完，没有踩到任何并行雷区
    }

    private Set<String> getNeedSimulateTaskDefinitionKeys(BpmnModel bpmnModel, Task currentTask, FlowElement targetElement) {
        // 1. 获取需要预测的任务的 definition key。因为当前任务还没完成，也需要预测
        Set<String> taskDefinitionKeys = CollUtil.newHashSet(currentTask.getTaskDefinitionKey());

        // 2.1 获取已结束任务按时间倒序排序
        List<HistoricTaskInstance> endTaskList = CollectionUtils.filterList(
                getTaskListByProcessInstanceId(currentTask.getProcessInstanceId(), Boolean.FALSE),
                item -> item.getEndTime() != null);
        // 2.2 从结束任务中找到最近一个的目标任务
        HistoricTaskInstance targetTask = findFirst(endTaskList,
                item -> item.getTaskDefinitionKey().equals(targetElement.getId()));
        if (targetTask == null) {
            return taskDefinitionKeys;
        }
        // 2.3 遍历已结束的任务，找到在 targetTask 之后生成的任务，且串行可达的任务
        endTaskList.forEach(item -> {
            FlowElement element = getFlowElementById(bpmnModel, item.getTaskDefinitionKey());
            // 如果已结束的任务在回退目标节点之后生成，且串行可达，则加到需要预测节点中
            // TODO 串行可达的方法需要和判断可回退节点 validateTargetTaskCanReturn 分开吗？ 并行网关可能会有问题。
            if (item.getCreateTime().compareTo(targetTask.getCreateTime()) > 0
                    && BpmnModelUtils.isSequentialReachable(element, targetElement, null)) {
                taskDefinitionKeys.add(item.getTaskDefinitionKey());
            }
        });
        return taskDefinitionKeys;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void delegateTask(Long userId, BpmTaskDelegateReqVO reqVO) {
        String taskId = reqVO.getId();
        // 1.1 校验任务
        Task task = validateTask(userId, reqVO.getId());
        if (task.getAssignee().equals(reqVO.getDelegateUserId().toString())) { // 校验当前审批人和被委派人不是同一人
            throw exception(TASK_DELEGATE_FAIL_USER_REPEAT);
        }
        // 1.2 校验目标用户存在
        AdminUserRespDTO delegateUser = adminUserApi.getUser(reqVO.getDelegateUserId());
        if (delegateUser == null) {
            throw exception(TASK_DELEGATE_FAIL_USER_NOT_EXISTS);
        }

        // 2. 添加委托意见
        AdminUserRespDTO currentUser = adminUserApi.getUser(userId);
        taskService.addComment(taskId, task.getProcessInstanceId(), BpmCommentTypeEnum.DELEGATE_START.getType(),
                BpmCommentTypeEnum.DELEGATE_START.formatComment(currentUser.getNickname(), delegateUser.getNickname(), reqVO.getReason()));

        // 3.1 设置任务所有人 (owner) 为原任务的处理人 (assignee)
        // 特殊：如果已经被委派（owner 非空），则不需要更新 owner：https://gitee.com/zhijiantianya/yudao-cloud/issues/ICJ153
        if (StrUtil.isEmpty(task.getOwner())) {
            taskService.setOwner(taskId, task.getAssignee());
        }
        // 3.2 执行委派，将任务委派给 delegateUser
        taskService.delegateTask(taskId, reqVO.getDelegateUserId().toString());
        // 补充说明：委托不单独设置状态。如果需要，可通过 Task 的 DelegationState 字段，判断是否为 DelegationState.PENDING 委托中
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void transferTask(Long userId, BpmTaskTransferReqVO reqVO) {
        String taskId = reqVO.getId();
        // 1.1 校验任务
        Task task = validateTask(userId, reqVO.getId());
        if (task.getAssignee().equals(reqVO.getAssigneeUserId().toString())) { // 校验当前审批人和被转派人不是同一人
            throw exception(TASK_TRANSFER_FAIL_USER_REPEAT);
        }
        // 1.2 校验目标用户存在
        AdminUserRespDTO assigneeUser = adminUserApi.getUser(reqVO.getAssigneeUserId());
        if (assigneeUser == null) {
            throw exception(TASK_TRANSFER_FAIL_USER_NOT_EXISTS);
        }

        // 2. 添加委托意见
        AdminUserRespDTO currentUser = adminUserApi.getUser(userId);
        taskService.addComment(taskId, task.getProcessInstanceId(), BpmCommentTypeEnum.TRANSFER.getType(),
                BpmCommentTypeEnum.TRANSFER.formatComment(currentUser.getNickname(), assigneeUser.getNickname(), reqVO.getReason()));

        // 3.1 设置任务所有人 (owner) 为原任务的处理人 (assignee)
        // 特殊：如果已经被转派（owner 非空），则不需要更新 owner：https://gitee.com/zhijiantianya/yudao-cloud/issues/ICJ153
        if (StrUtil.isEmpty(task.getOwner())) {
            taskService.setOwner(taskId, task.getAssignee());
        }
        // 3.2 执行转派（审批人），将任务转派给 assigneeUser
        // 委托（ delegate）和转派（transfer）的差别，就在这块的调用！！！！
        taskService.setAssignee(taskId, reqVO.getAssigneeUserId().toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void moveTaskToEnd(String processInstanceId, String reason) {
        List<Task> taskList = getRunningTaskListByProcessInstanceId(processInstanceId, null, null);
        if (CollUtil.isEmpty(taskList)) {
            return;
        }

        // 1. 其它未结束的任务，直接取消
        // 疑问：为什么不通过 updateTaskStatusWhenCanceled 监听取消，而是直接提前调用呢？
        // 回答：详细见 updateTaskStatusWhenCanceled 的方法，加签的场景
        taskList.forEach(task -> {
            Integer otherTaskStatus = (Integer) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
            if (BpmTaskStatusEnum.isEndStatus(otherTaskStatus)) {
                return;
            }
            processTaskCanceled(task.getId());
        });

        // 2. 终止流程
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(taskList.get(0).getProcessDefinitionId());
        List<String> activityIds = CollUtil.newArrayList(convertSet(taskList, Task::getTaskDefinitionKey));
        EndEvent endEvent = BpmnModelUtils.getEndEvent(bpmnModel);
        Assert.notNull(endEvent, "结束节点不能为空");
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(activityIds, endEvent.getId())
                .changeState();

        // 3. 特殊：如果跳转到 EndEvent 流程还未结束， 执行 deleteProcessInstance 方法
        // TODO 芋艿：目前发现并行分支情况下，会存在这个情况，后续看看有没更好的方案；
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).list();
        if (CollUtil.isNotEmpty(executions)) {
            log.warn("[moveTaskToEnd][执行跳转到 EndEvent 后, 流程实例未结束，强制执行 deleteProcessInstance 方法]");
            runtimeService.deleteProcessInstance(processInstanceId, reason);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public void createSignTask(Long userId, BpmTaskSignCreateReqVO reqVO) {
        // 1. 获取和校验任务
        TaskEntityImpl taskEntity = validateTaskCanCreateSign(userId, reqVO);
        List<AdminUserRespDTO> userList = adminUserApi.getUserList(reqVO.getUserIds());
        if (CollUtil.isEmpty(userList)) {
            throw exception(TASK_SIGN_CREATE_USER_NOT_EXIST);
        }

        // 2. 处理当前任务
        // 2.1 开启计数功能，主要用于为了让表 ACT_RU_TASK 中的 SUB_TASK_COUNT_ 字段记录下总共有多少子任务，后续可能有用
        taskEntity.setCountEnabled(true);
        // 2.2 向前加签，设置 owner，置空 assign。等子任务都完成后，再调用 resolveTask 重新将 owner 设置为 assign
        // 原因是：不能和向前加签的子任务一起审批，需要等前面的子任务都完成才能审批
        if (reqVO.getType().equals(BpmTaskSignTypeEnum.BEFORE.getType())) {
            taskEntity.setOwner(taskEntity.getAssignee());
            taskEntity.setAssignee(null);
        }
        // 2.4 记录加签方式，完成任务时需要用到判断
        taskEntity.setScopeType(reqVO.getType());
        // 2.5 保存当前任务修改后的值
        taskService.saveTask(taskEntity);
        // 2.6 更新 task 状态为 WAIT，只有在向前加签的时候
        if (reqVO.getType().equals(BpmTaskSignTypeEnum.BEFORE.getType())) {
            updateTaskStatus(taskEntity.getId(), BpmTaskStatusEnum.WAIT.getStatus());
        }

        // 3. 创建加签任务
        createSignTaskList(convertList(reqVO.getUserIds(), String::valueOf), taskEntity);

        // 4. 记录加签的评论到 task 任务
        AdminUserRespDTO currentUser = adminUserApi.getUser(userId);
        String comment = StrUtil.format(BpmCommentTypeEnum.ADD_SIGN.getComment(),
                currentUser.getNickname(), BpmTaskSignTypeEnum.nameOfType(reqVO.getType()),
                String.join(",", convertList(userList, AdminUserRespDTO::getNickname)), reqVO.getReason());
        taskService.addComment(reqVO.getId(), taskEntity.getProcessInstanceId(), BpmCommentTypeEnum.ADD_SIGN.getType(), comment);
    }

    /**
     * 校验任务是否可以加签，主要校验加签类型是否一致：
     * <p>
     * 1. 如果存在“向前加签”的任务，则不能“向后加签”
     * 2. 如果存在“向后加签”的任务，则不能“向前加签”
     *
     * @param userId 当前用户 ID
     * @param reqVO  请求参数，包含任务 ID 和加签类型
     * @return 当前任务
     */
    private TaskEntityImpl validateTaskCanCreateSign(Long userId, BpmTaskSignCreateReqVO reqVO) {
        TaskEntityImpl taskEntity = (TaskEntityImpl) validateTask(userId, reqVO.getId());
        // 向前加签和向后加签不能同时存在
        if (taskEntity.getScopeType() != null
                && ObjectUtil.notEqual(taskEntity.getScopeType(), reqVO.getType())) {
            throw exception(TASK_SIGN_CREATE_TYPE_ERROR,
                    BpmTaskSignTypeEnum.nameOfType(taskEntity.getScopeType()), BpmTaskSignTypeEnum.nameOfType(reqVO.getType()));
        }

        // 同一个 key 的任务，审批人不重复
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(taskEntity.getProcessInstanceId())
                .taskDefinitionKey(taskEntity.getTaskDefinitionKey()).list();
        List<Long> currentAssigneeList = convertListByFlatMap(taskList, task -> // 需要考虑 owner 的情况，因为向后加签时，它暂时没 assignee 而是 owner
                Stream.of(NumberUtils.parseLong(task.getAssignee()), NumberUtils.parseLong(task.getOwner())));
        if (CollUtil.containsAny(currentAssigneeList, reqVO.getUserIds())) {
            List<AdminUserRespDTO> userList = adminUserApi.getUserList(CollUtil.intersection(currentAssigneeList, reqVO.getUserIds()));
            throw exception(TASK_SIGN_CREATE_USER_REPEAT, String.join(",", convertList(userList, AdminUserRespDTO::getNickname)));
        }
        return taskEntity;
    }

    /**
     * 创建加签子任务
     *
     * @param userIds    被加签的用户 ID
     * @param taskEntity 被加签的任务
     */
    private void createSignTaskList(List<String> userIds, TaskEntityImpl taskEntity) {
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        // 创建加签人的新任务，全部基于 taskEntity 为父任务来创建
        for (String addSignId : userIds) {
            if (StrUtil.isBlank(addSignId)) {
                continue;
            }
            createSignTask(taskEntity, addSignId);
        }
    }

    /**
     * 创建加签子任务
     *
     * @param parentTask 父任务
     * @param assignee   子任务的执行人
     */
    private void createSignTask(TaskEntityImpl parentTask, String assignee) {
        // 1. 生成子任务
        TaskEntityImpl task = (TaskEntityImpl) taskService.newTask(IdUtil.fastSimpleUUID());
        BpmTaskConvert.INSTANCE.copyTo(parentTask, task);

        // 2.1 向前加签，设置审批人
        if (BpmTaskSignTypeEnum.BEFORE.getType().equals(parentTask.getScopeType())) {
            task.setAssignee(assignee);
            // 2.2 向后加签，设置 owner 不设置 assignee 是因为不能同时审批，需要等父任务完成
        } else {
            task.setOwner(assignee);
        }
        // 2.3 保存子任务
        taskService.saveTask(task);

        // 3. 向后前签，设置子任务的状态为 WAIT，因为需要等父任务审批完
        if (BpmTaskSignTypeEnum.AFTER.getType().equals(parentTask.getScopeType())) {
            updateTaskStatus(task.getId(), BpmTaskStatusEnum.WAIT.getStatus());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    @SuppressWarnings("DataFlowIssue")
    public void deleteSignTask(Long userId, BpmTaskSignDeleteReqVO reqVO) {
        // 1.1 校验 task 可以被减签
        Task task = validateTaskCanSignDelete(reqVO.getId());
        // 1.2 校验取消人存在
        AdminUserRespDTO cancelUser = null;
        if (StrUtil.isNotBlank(task.getAssignee())) {
            cancelUser = adminUserApi.getUser(NumberUtils.parseLong(task.getAssignee()));
        }
        if (cancelUser == null && StrUtil.isNotBlank(task.getOwner())) {
            cancelUser = adminUserApi.getUser(NumberUtils.parseLong(task.getOwner()));
        }
        Assert.notNull(cancelUser, "任务中没有所有者和审批人，数据错误");

        // 2.1 获得子任务列表，包括子任务的子任务
        List<Task> childTaskList = getAllChildTaskList(task);
        childTaskList.add(task);
        // 2.2 更新子任务为已取消
        String cancelReason = StrUtil.format("任务被取消，原因：由于[{}]操作[减签]，", cancelUser.getNickname());
        childTaskList.forEach(childTask -> updateTaskStatusAndReason(childTask.getId(), BpmTaskStatusEnum.CANCEL.getStatus(), cancelReason));
        // 2.3 删除任务和所有子任务
        taskService.deleteTasks(convertList(childTaskList, Task::getId));

        // 3. 记录日志到父任务中。先记录日志是因为，通过 handleParentTask 方法之后，任务可能被完成了，并且不存在了，会报异常，所以先记录
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        taskService.addComment(task.getParentTaskId(), task.getProcessInstanceId(), BpmCommentTypeEnum.SUB_SIGN.getType(),
                StrUtil.format(BpmCommentTypeEnum.SUB_SIGN.getComment(), user.getNickname(), cancelUser.getNickname()));

        // 4. 处理当前任务的父任务
        handleParentTaskIfSign(task.getParentTaskId());
    }

    @Override
    public void copyTask(Long userId, BpmTaskCopyReqVO reqVO) {
        processInstanceCopyService.createProcessInstanceCopy(reqVO.getCopyUserIds(), reqVO.getReason(), reqVO.getId());
    }

    @Override
    public BpmTaskTraceDTO getTaskTrace(String taskId,String processInstanceId,int queryType) {
        BpmTaskTraceDTO result = new BpmTaskTraceDTO();

        if ("StartEvent".equals(taskId)) {
            fillProcessInfo(result, processInstanceId);
            // 3. 处理转入/转出
            result.setPreviousTasks(new ArrayList<>()); // 【需求】转入环节直接返回空
            result.setNextTasks(new ArrayList<>());

            // 如果是查“转出”(下一任务)，则查询流程的第一批人工任务
            if (QUERY_TYPE_NEXT == queryType) {
                result.setNextTasks(findNextNodesForStartEvent(processInstanceId));
            }
            fillUserNames(result);
            return result;
        }

        // 1. 获取当前任务详情
        HistoricTaskInstance currentTask = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId)
                .singleResult();

        if (currentTask == null) {
            throw new RuntimeException("未找到任务实例: " + taskId);
        }
        // 2. 获取流程实例信息 (用于在前端顶部展示：流程发起人、发起时间)
        String procInstId = currentTask.getProcessInstanceId();
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(procInstId)
                .singleResult();
        if (processInstance != null) {
            result.setProcessStartTime(convertDate(processInstance.getStartTime())); // 这就是第一个任务的开始时间
            result.setStartUserId(processInstance.getStartUserId());    // 顺便拿到发起人
        }
        // 3. 填充当前任务信息
        result.setCurrentTask(convert(currentTask));
        result.setPreviousTasks(new ArrayList<>());
        result.setNextTasks(new ArrayList<>());

        // 4. 根据类型执行查询
        if (QUERY_TYPE_PREV == queryType) {
            result.setPreviousTasks(findPreviousNodes(currentTask));
        } else if (QUERY_TYPE_NEXT == queryType) {
            // 只有当前任务结束了才有后置
            if (currentTask.getEndTime() != null) {
                result.setNextTasks(findNextNodes(currentTask));
            }
        }
        fillUserNames(result);
        return result;

    }

    private void fillUserNames(BpmTaskTraceDTO result) {
        // 1. 收集所有需要查询的 User ID
        Set<Long> userIds = new HashSet<>();

        // 收集发起人
        if (result.getStartUserId() != null) {
            userIds.add(Long.parseLong(result.getStartUserId()));
        }
        // 收集当前任务办理人
        addUserId(userIds, result.getCurrentTask());
        // 收集前置列表办理人
        if (result.getPreviousTasks() != null) {
            result.getPreviousTasks().forEach(node -> addUserId(userIds, node));
        }
        // 收集后置列表办理人
        if (result.getNextTasks() != null) {
            result.getNextTasks().forEach(node -> addUserId(userIds, node));
        }

        if (userIds.isEmpty()) return;

        // 2. 调用 System 模块 API 获取用户信息 Map
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        // 3. 回填昵称
        // 3.1 回填发起人名字 (如果前端需要单独展示)
        if (result.getStartUserId() != null) {
            AdminUserRespDTO startUser = userMap.get(Long.parseLong(result.getStartUserId()));
            // 这里你可以选择扩展DTO加个 startUserName字段，或者前端自己查，这里暂时不做额外处理
        }

        // 3.2 回填当前、前置、后置任务的 assigneeName
        setNickName(result.getCurrentTask(), userMap);
        if (result.getPreviousTasks() != null) {
            result.getPreviousTasks().forEach(node -> setNickName(node, userMap));
        }
        if (result.getNextTasks() != null) {
            result.getNextTasks().forEach(node -> setNickName(node, userMap));
        }
    }

    private void addUserId(Set<Long> userIds, BpmTaskFlowTaskNodeRespVO node) {
        if (node != null && node.getAssignee() != null) {
            try {
                userIds.add(Long.parseLong(node.getAssignee()));
            } catch (NumberFormatException e) {
                // 忽略非数字ID
            }
        }
    }

    private void setNickName(BpmTaskFlowTaskNodeRespVO node, Map<Long, AdminUserRespDTO> userMap) {
        if (node == null || node.getAssignee() == null) return;
        try {
            Long userId = Long.parseLong(node.getAssignee());
            AdminUserRespDTO user = userMap.get(userId);
            if (user != null) {
                node.setAssigneeName(user.getNickname()); // 【需求】设置昵称
                // 如果需要部门，也可以在这里 setDeptName(user.getDeptName())
            }
        } catch (Exception e) {
            node.setAssigneeName(node.getAssignee()); // 兜底显示ID
        }
    }
    private Integer calculateStatus(Date endTime, String deleteReason) {
        // 1. 如果没有结束时间 -> 进行中
        if (endTime == null) {
            return 1;
        }

        // 2. 如果有结束时间
        if (deleteReason == null) {
            return 2; // 正常完成
        }

        // 3. 特殊处理 deleteReason
        // "MI_END": 多实例任务正常结束
        // "completed": 部分API调用会显式写入这个值
        if ("MI_END".equals(deleteReason) || "completed".equalsIgnoreCase(deleteReason)) {
            return 2; // 视为正常完成
        }

        // 4. 其他情况 (如 "deleted", "canceled", "jump" 等) -> 视为取消/驳回
        return 3;
    }

    private void fillProcessInfo(BpmTaskTraceDTO result, String procInstId) {
        if (procInstId == null) return;
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(procInstId)
                .singleResult();
        if (processInstance != null) {
            result.setProcessStartTime(convertDate(processInstance.getStartTime()));
            result.setStartUserId(processInstance.getStartUserId());
        }
    }

    private List<BpmTaskFlowTaskNodeRespVO> findNextNodesForStartEvent(String processInstanceId) {
        List<BpmTaskFlowTaskNodeRespVO> nodes = new ArrayList<>();

        // 查询该流程下所有的 UserTask，按开始时间正序排列
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityType("userTask")
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        if (activities.isEmpty()) return nodes;

        // 逻辑：取列表里最早的那个，以及和它同时开始的(并行)任务
        HistoricActivityInstance firstNode = activities.get(0);
        nodes.add(convert(firstNode));

        // 处理并行网关：如果有多个任务几乎同时开始 (误差1秒内)
        long baselineTime = firstNode.getStartTime().getTime();
        for (int i = 1; i < activities.size(); i++) {
            HistoricActivityInstance node = activities.get(i);
            if (Math.abs(node.getStartTime().getTime() - baselineTime) < 1000) {
                nodes.add(convert(node));
            } else {
                break; // 只要遇到时间差别大的，后面的肯定都是后续步骤了，直接跳出
            }
        }
        return nodes;
    }

    private List<BpmTaskFlowTaskNodeRespVO> findPreviousNodes(HistoricTaskInstance currentTask) {
        List<BpmTaskFlowTaskNodeRespVO> nodes = new ArrayList<>();

        HistoricVariableInstance sourceVar = historyService.createHistoricVariableInstanceQuery()
                .taskId(currentTask.getId())
                .variableName("internal_source_task_id")
                .singleResult();

        if (sourceVar != null && sourceVar.getValue() != null) {
            String sourceTaskId = sourceVar.getValue().toString();
            // 直接精准查出它的“父亲”任务
            HistoricTaskInstance sourceTask = historyService.createHistoricTaskInstanceQuery()
                    .taskId(sourceTaskId).singleResult();
            if (sourceTask != null) {
                nodes.add(convert(sourceTask));
                return nodes; // 命中内循环，直接返回！
            }
        }

        String procInstId = currentTask.getProcessInstanceId();

        // 1. 获取流程实例，拿到“真正的”主流程开始节点ID
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(procInstId)
                .singleResult();

        // 这是皇室血统证明，只有ID等于这个的 startEvent 才是主流程的起点
        String rootStartActivityId = processInstance.getStartActivityId();
        String startUserId = processInstance.getStartUserId();

        // 2. 查询：同时查 userTask 和 startEvent
        List<String> targetTypes = Arrays.asList("userTask", "startEvent");

        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(procInstId)
                .activityTypes(new HashSet<>(targetTypes)) // 查这两种
                .finished()
                .orderByHistoricActivityInstanceEndTime().desc()
                .list();

        if (activities.isEmpty()) return nodes;

        HistoricActivityInstance baselineNode = null;

        // 3. 遍历寻找基准点 (并在此处执行“杀掉子流程开始节点”的逻辑)
        for (HistoricActivityInstance node : activities) {
            // 跳过当前任务自己
            if (node.getTaskId() != null && node.getTaskId().equals(currentTask.getId())) {
                continue;
            }

            // 【关键逻辑】如果是开始节点，必须校验它是不是主流程的起点
            if ("startEvent".equals(node.getActivityType())) {
                // 如果这个节点的ID 不是 主流程的开始ID -> 说明它是子流程的开始 -> 滚蛋
                if (rootStartActivityId != null && !rootStartActivityId.equals(node.getActivityId())) {
                    continue;
                }
            }

            // 正常的按时间查找逻辑
            if (node.getEndTime().getTime() <= currentTask.getStartTime().getTime()) {
                baselineNode = node;
                break;
            }
        }

        // 4. 组装结果 (处理并行网关汇聚)
        if (baselineNode != null) {
            nodes.add(convertWithStartUser(baselineNode, startUserId));

            long baselineTime = baselineNode.getEndTime().getTime();
            for (HistoricActivityInstance node : activities) {
                // 排除基准节点本身 和 当前任务
                if (node.getId().equals(baselineNode.getId()) ||
                        (node.getTaskId() != null && node.getTaskId().equals(currentTask.getId()))) {
                    continue;
                }

                // 【关键逻辑再次校验】并行汇聚时，万一混进来一个子流程开始节点，也得杀掉
                if ("startEvent".equals(node.getActivityType())) {
                    if (rootStartActivityId != null && !rootStartActivityId.equals(node.getActivityId())) {
                        continue;
                    }
                }

                // 时间容错 1000ms
                if (Math.abs(node.getEndTime().getTime() - baselineTime) < 1000) {
                    nodes.add(convert(node));
                }
            }
        }
        return nodes;
    }
    private BpmTaskFlowTaskNodeRespVO convertWithStartUser(HistoricActivityInstance activity, String startUserId) {
        // 调用原本的 convert 基础转换
        BpmTaskFlowTaskNodeRespVO vo = convert(activity);

        // 【补丁逻辑】：如果是开始节点，且原本没拿到 assignee，强制把发起人塞进去
        if ("startEvent".equals(activity.getActivityType())) {
            if (vo.getAssignee() == null) {
                vo.setAssignee(startUserId);
            }
            // 确保状态是已完成
            vo.setStatus(2);
        }
        return vo;
    }

    private List<BpmTaskFlowTaskNodeRespVO> findNextNodes(HistoricTaskInstance currentTask) {
        List<BpmTaskFlowTaskNodeRespVO> nodes = new ArrayList<>();


        // ================= 【核心新增：寻找内循环派发出去的子任务】 =================
        List<HistoricVariableInstance> targetVars = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(currentTask.getProcessInstanceId())
                .variableName("internal_source_task_id")
                .variableValueEquals("internal_source_task_id", currentTask.getId())
                .list();

        if (CollUtil.isNotEmpty(targetVars)) {
            for (HistoricVariableInstance var : targetVars) {
                HistoricTaskInstance nextTask = historyService.createHistoricTaskInstanceQuery()
                        .taskId(var.getTaskId()).singleResult();
                if (nextTask != null) {
                    nodes.add(convert(nextTask));
                }
            }
            return nodes; // 命中内循环，直接返回！
        }
        // 同样只查 userTask，忽略网关和结束节点
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(currentTask.getProcessInstanceId())
                .activityType("userTask")
                .startedAfter(currentTask.getEndTime())
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        if (activities.isEmpty()) return nodes;

        // 取最早开始的那个作为基准
        HistoricActivityInstance firstNextNode = activities.get(0);
        nodes.add(convert(firstNextNode));

        // 处理并行网关分叉 (tolerance: 1000ms)
        long baselineTime = firstNextNode.getStartTime().getTime();
        for (int i = 1; i < activities.size(); i++) {
            HistoricActivityInstance node = activities.get(i);
            if (Math.abs(node.getStartTime().getTime() - baselineTime) < 1000) {
                nodes.add(convert(node));
            } else {
                break;
            }
        }
        return nodes;
    }

    private BpmTaskFlowTaskNodeRespVO convert(HistoricTaskInstance task) {
        BpmTaskFlowTaskNodeRespVO dto = new BpmTaskFlowTaskNodeRespVO();
        dto.setTaskId(task.getId());
        dto.setTaskName(task.getName());
        dto.setAssignee(task.getAssignee());
        dto.setActivityType("userTask");
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setDuration(task.getDurationInMillis());
        dto.setStatus(calculateStatus(task.getEndTime(), task.getDeleteReason()));
        return dto;
    }

    private BpmTaskFlowTaskNodeRespVO convert(HistoricActivityInstance activity) {
        BpmTaskFlowTaskNodeRespVO dto = new BpmTaskFlowTaskNodeRespVO();
        dto.setTaskId(activity.getTaskId());
        if ("startEvent".equals(activity.getActivityType())) {
            dto.setTaskName(activity.getActivityName() != null ? activity.getActivityName() : "流程发起");
            dto.setActivityType("startEvent");
        } else {
            dto.setTaskName(activity.getActivityName());
            dto.setActivityType("userTask");
        }
//        dto.setTaskName(activity.getActivityName());
        dto.setAssignee(activity.getAssignee());
        dto.setStartTime(activity.getStartTime());
        dto.setEndTime(activity.getEndTime());
        dto.setDuration(activity.getDurationInMillis());
        dto.setStatus(calculateStatus(activity.getEndTime(), activity.getDeleteReason()));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据
    public void withdrawTask(Long userId, String taskId) {
        // 1.1 查询本人已办任务
        HistoricTaskInstance taskInstance = historyService.createHistoricTaskInstanceQuery()
                .taskId(taskId).taskAssignee(userId.toString()).finished().singleResult();
        if (ObjUtil.isNull(taskInstance)) {
            throw exception(TASK_WITHDRAW_FAIL_TASK_NOT_EXISTS);
        }
        // 1.2 校验流程是否结束
        ProcessInstance processInstance = processInstanceService.getProcessInstance(taskInstance.getProcessInstanceId());
        if (ObjUtil.isNull(processInstance)) {
            throw exception(TASK_WITHDRAW_FAIL_PROCESS_NOT_RUNNING);
        }
        // 1.3 判断此流程是否允许撤回
        BpmProcessDefinitionInfoDO processDefinitionInfo = bpmProcessDefinitionService.getProcessDefinitionInfo(
                processInstance.getProcessDefinitionId());
        if (ObjUtil.isNull(processDefinitionInfo) || !Boolean.TRUE.equals(processDefinitionInfo.getAllowWithdrawTask())) {
            throw exception(TASK_WITHDRAW_FAIL_NOT_ALLOW);
        }

        // 1.4 获取当前节点定义的合法下一节点集合
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(taskInstance.getProcessDefinitionId());
        UserTask userTask = (UserTask) BpmnModelUtils.getFlowElementById(bpmnModel, taskInstance.getTaskDefinitionKey());
        List<String> nextUserTaskKeys = convertList(BpmnModelUtils.getNextUserTasks(userTask), UserTask::getId);
        if (CollUtil.isEmpty(nextUserTaskKeys)) {
            throw exception(TASK_WITHDRAW_FAIL_NEXT_TASK_NOT_ALLOW);
        }
        Map<String, String> sourceReturnBranchContext = getHistoricTaskReturnBranchContext(taskId);
        String sourceReturnBatchId = sourceReturnBranchContext.get(taskInstance.getTaskDefinitionKey());

        long sourceTaskCreateTime = taskInstance.getCreateTime().getTime();

        // =================================================================================
        // 【第一道防线】：同批次隔离哨兵（处理同节点未结束撤回 / 多人排队撤回）
        // =================================================================================
        List<Task> rawActiveTasksInSourceNode = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKey(taskInstance.getTaskDefinitionKey())
                .active().list();

        List<Task> activeTasksInSourceNode = new ArrayList<>();
        if (CollUtil.isNotEmpty(rawActiveTasksInSourceNode)) {
            for (Task runningTask : rawActiveTasksInSourceNode) {
                if (Math.abs(runningTask.getCreateTime().getTime() - sourceTaskCreateTime) <= 1000) {
                    activeTasksInSourceNode.add(runningTask);
                }
            }
        }

        if (CollUtil.isNotEmpty(activeTasksInSourceNode)) {
            boolean alreadyHasMyTask = activeTasksInSourceNode.stream()
                    .anyMatch(t -> userId.toString().equals(t.getAssignee()));
            if (alreadyHasMyTask) {
                throw new RuntimeException("撤回失败：您在该环节已有运行中的任务，无需重复撤回");
            }

            updateTaskStatusAndReason(taskId, BpmTaskStatusEnum.CANCEL.getStatus(), BpmReasonEnum.CANCEL_BY_WITHDRAW.getReason());

            Map<String, Object> executionVariables = new HashMap<>();
            executionVariables.put("assignee", userId.toString());

            Execution newExecution = runtimeService.addMultiInstanceExecution(
                    taskInstance.getTaskDefinitionKey(),
                    processInstance.getProcessInstanceId(),
                    executionVariables
            );

            if (newExecution != null) {
                Task newlyCreatedTask = taskService.createTaskQuery().executionId(newExecution.getId()).singleResult();
                if (newlyCreatedTask != null) {
                    taskService.setAssignee(newlyCreatedTask.getId(), userId.toString());
                    taskService.setOwner(newlyCreatedTask.getId(), userId.toString());
                }
            }
            return;
        }

        // =================================================================================
        // 【前置推导】：推导节点真实流转时间
        // =================================================================================
        List<HistoricTaskInstance> historyTasksOfSourceNode = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKey(taskInstance.getTaskDefinitionKey())
                .finished()
                .list();

        long realNodeOutflowTime = taskInstance.getEndTime().getTime();

        if (CollUtil.isNotEmpty(historyTasksOfSourceNode)) {
            List<HistoricTaskInstance> sameBatchTasks = new ArrayList<>();
            for (HistoricTaskInstance ht : historyTasksOfSourceNode) {
                if (Math.abs(ht.getCreateTime().getTime() - sourceTaskCreateTime) <= 1000) {
                    sameBatchTasks.add(ht);
                }
            }
            if (CollUtil.isNotEmpty(sameBatchTasks)) {
                sameBatchTasks.sort((t1, t2) -> t2.getEndTime().compareTo(t1.getEndTime()));
                realNodeOutflowTime = sameBatchTasks.get(0).getEndTime().getTime();
            }
        }

        // =================================================================================
        // 【绝对拦截防线】：防“部分办结”漏洞！
        // =================================================================================
        List<HistoricTaskInstance> finishedDownstreamTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKeys(nextUserTaskKeys)
                .finished()
                .list();

        if (CollUtil.isNotEmpty(finishedDownstreamTasks)) {
            for (HistoricTaskInstance ht : finishedDownstreamTasks) {
                if (StrUtil.isNotBlank(sourceReturnBatchId)) {
                    Map<String, String> downstreamContext = getHistoricTaskReturnBranchContext(ht.getId());
                    String downstreamBatchId = downstreamContext.get(taskInstance.getTaskDefinitionKey());
                    if (StrUtil.equals(sourceReturnBatchId, downstreamBatchId)) {
                        throw new RuntimeException("撤回失败：下游已有节点完成审批，流程已部分流转，禁止撤回！");
                    }
                    continue;
                }
                long timeDiff = Math.abs(ht.getCreateTime().getTime() - realNodeOutflowTime);
                if (timeDiff <= 1000) {
                    throw new RuntimeException("撤回失败：下游已有节点完成审批，流程已部分流转，禁止撤回！");
                }
            }
        }

        // =================================================================================
        // 【第二道防线】：跨节点拉回，收集存活的分身
        // =================================================================================
        List<Task> allRunningTasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKeys(nextUserTaskKeys).active().list();
        if (CollUtil.isEmpty(allRunningTasks)) {
            throw exception(TASK_WITHDRAW_FAIL_NEXT_TASK_NOT_ALLOW);
        }

        List<Task> targetRunningTasks = new ArrayList<>();
        if (StrUtil.isBlank(sourceReturnBatchId)) {
            sourceReturnBatchId = inferUniqueReturnBatchIdFromRunningTasks(processInstance.getProcessInstanceId(),
                    taskInstance.getTaskDefinitionKey(), allRunningTasks);
        }
        for (Task task : allRunningTasks) {
            if (StrUtil.isNotBlank(sourceReturnBatchId)) {
                Map<String, String> runningContext = getReturnBranchContext(processInstance.getProcessInstanceId(), task.getExecutionId());
                String runningBatchId = runningContext.get(taskInstance.getTaskDefinitionKey());
                if (StrUtil.equals(sourceReturnBatchId, runningBatchId)) {
                    targetRunningTasks.add(task);
                }
                continue;
            }
            long targetCreateTime = task.getCreateTime().getTime();
            long timeDiff = Math.abs(targetCreateTime - realNodeOutflowTime);

            if (timeDiff <= 1000) {
                targetRunningTasks.add(task);
            } else {
                String currentExecId = task.getExecutionId();
                while (StrUtil.isNotBlank(currentExecId)) {
                    if (currentExecId.equals(taskInstance.getExecutionId())) {
                        targetRunningTasks.add(task);
                        break;
                    }
                    Execution exec = runtimeService.createExecutionQuery().executionId(currentExecId).singleResult();
                    currentExecId = exec != null ? exec.getParentId() : null;
                }
            }
        }

        if (CollUtil.isEmpty(targetRunningTasks)) {
            throw exception(TASK_WITHDRAW_FAIL_NEXT_TASK_NOT_ALLOW);
        }

        // =================================================================================
        // 【防裂变处理】：取消目标任务、收集血脉、精准提取 MI Root 进行执行流去重
        // =================================================================================
        List<String> executionIdsToMove = new ArrayList<>();
        Set<String> branchExecutionLineage = new HashSet<>();

        for (Task task : targetRunningTasks) {
            taskService.addComment(task.getId(), taskInstance.getProcessInstanceId(), BpmCommentTypeEnum.CANCEL.getType(),
                    BpmCommentTypeEnum.CANCEL.formatComment("前一节点撤回"));
            updateTaskStatusAndReason(task.getId(), BpmTaskStatusEnum.CANCEL.getStatus(), BpmReasonEnum.CANCEL_BY_WITHDRAW.getReason());

            // 1. 向上收集当前分支的执行流血统 (用于后续的局部变量清理)
            String tempId = task.getExecutionId();
            while (cn.hutool.core.util.StrUtil.isNotBlank(tempId)) {
                branchExecutionLineage.add(tempId);
                Execution exec = runtimeService.createExecutionQuery().executionId(tempId).singleResult();
                tempId = exec != null ? exec.getParentId() : null;
            }

            // 2. 提取多实例根节点 (MI Root)，防止多实例分支撤回时生成多条任务
            Execution taskExecution = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            String execIdToMove = taskExecution.getId();

            if (taskExecution.getParentId() != null) {
                Execution parentExecution = runtimeService.createExecutionQuery().executionId(taskExecution.getParentId()).singleResult();
                // 核心：含有 nrOfInstances 变量的即为多实例包裹容器
                if (parentExecution != null && runtimeService.hasVariableLocal(parentExecution.getId(), "nrOfInstances")) {
                    execIdToMove = parentExecution.getId();
                }
            }

            // 3. 去重：如果是同批次并发分支，MI Root 必然是同一个，完美合并为 1 个 Token
            if (!executionIdsToMove.contains(execIdToMove)) {
                executionIdsToMove.add(execIdToMove);
            }
        }

        // =================================================================================
        // 【对照实验组】：仅移除运行时局部变量，不触碰历史表！
        // =================================================================================
        org.flowable.bpmn.model.FlowElement currentFlowElement = bpmnModel.getFlowElement(taskInstance.getTaskDefinitionKey());
        if (currentFlowElement instanceof org.flowable.bpmn.model.FlowNode) {
            List<org.flowable.bpmn.model.SequenceFlow> outgoingFlows = ((org.flowable.bpmn.model.FlowNode) currentFlowElement).getOutgoingFlows();

            Object assigneesObj = runtimeService.getVariable(processInstance.getProcessInstanceId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES);
            Map<String, List<Long>> assigneesMap = null;
            if (assigneesObj instanceof Map) {
                assigneesMap = new HashMap<>((Map<String, List<Long>>) assigneesObj);
            }
            boolean assigneesChanged = false;

            for (org.flowable.bpmn.model.SequenceFlow flow : outgoingFlows) {
                // 1. 抹除目标节点残留的人员配置 (这也是通过重写全局变量实现的)
                String targetNodeId = flow.getTargetRef();
                if (assigneesMap != null && assigneesMap.containsKey(targetNodeId)) {
                    assigneesMap.remove(targetNodeId);
                    assigneesChanged = true;
                }

                // 2. 双重正则提取真正的路由变量
                String condition = flow.getConditionExpression();
                if (cn.hutool.core.util.StrUtil.isNotBlank(condition)) {
                    Set<String> variablesToRemove = new HashSet<>();

                    java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("variables:get\\(['\"]?([a-zA-Z0-9_]+)['\"]?\\)").matcher(condition);
                    while (m1.find()) { variablesToRemove.add(m1.group(1)); }

                    java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(?<!['\"])\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b(?!['\"])").matcher(condition);
                    while (m2.find()) {
                        String match = m2.group(1);
                        if (!Arrays.asList("variables", "get", "null", "empty", "true", "false").contains(match)) {
                            variablesToRemove.add(match);
                        }
                    }

                    // 3. 【核心对照组测试】：仅仅从运行时表 (ACT_RU_VARIABLE) 中移除
                    for (String varName : variablesToRemove) {
                        for (String branchExecId : branchExecutionLineage) {
//                            if (branchExecId.equals(processInstance.getProcessInstanceId())) {
//                                continue;
//                            }
                            // 只做这一步：移除运行时局部变量
                            if (runtimeService.hasVariableLocal(branchExecId, varName)) {
                                runtimeService.removeVariableLocal(branchExecId, varName);
                            }
                        }
                    }
                }
            }
            if (assigneesChanged) {
                runtimeService.setVariable(processInstance.getProcessInstanceId(), BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, assigneesMap);
            }
        }

        // =================================================================================
        // 【执行阶段】：单一主分身拉回 + 多余分身 EndEvent 合法蒸发防雪崩
        // =================================================================================
        List<String> singleUserList = new ArrayList<>();
        singleUserList.add(userId.toString());
        String miCollectionVarName = "coll_userList";

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstance.getProcessInstanceId())
                .moveExecutionsToSingleActivityId(executionIdsToMove, taskInstance.getTaskDefinitionKey())
                .processVariable(miCollectionVarName, singleUserList)
                .changeState();

        taskSortMapper.updateHistoricTaskLongVariable(taskId, BpmnVariableConstants.TASK_VARIABLE_STATUS,
                BpmTaskStatusEnum.CANCEL.getStatus());

        // =================================================================================
        // 【收尾】：强制覆盖重置办理人，重新夺回任务控制权
        // =================================================================================
        List<Task> revertedTasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskDefinitionKey(taskInstance.getTaskDefinitionKey())
                .active()
                .list();
        if (CollUtil.isEmpty(revertedTasks)) {
            revertedTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .active()
                    .list()
                    .stream()
                    .filter(task -> StrUtil.containsAny(task.getName(), "收文登记", "来文登记"))
                    .collect(Collectors.toList());
        }

        if (CollUtil.isNotEmpty(revertedTasks)) {
            for (Task revertedTask : revertedTasks) {
                taskService.setAssignee(revertedTask.getId(), userId.toString());
                taskService.setOwner(revertedTask.getId(), userId.toString());
                updateTaskStatus(revertedTask.getId(), BpmTaskStatusEnum.RUNNING.getStatus());
            }
        }
    }
    /**
     * 校验任务是否能被减签
     *
     * @param id 任务编号
     * @return 任务信息
     */
    private Task validateTaskCanSignDelete(String id) {
        Task task = validateTaskExist(id);
        if (task.getParentTaskId() == null) {
            throw exception(TASK_SIGN_DELETE_NO_PARENT);
        }
        Task parentTask = getTask(task.getParentTaskId());
        if (parentTask == null) {
            throw exception(TASK_SIGN_DELETE_NO_PARENT);
        }
        if (BpmTaskSignTypeEnum.of(parentTask.getScopeType()) == null) {
            throw exception(TASK_SIGN_DELETE_NO_PARENT);
        }
        return task;
    }

    // ========== Event 事件相关方法 ==========

    @Override
    public void processTaskCreated(Task task) {
        ReturnBranchContextCarrier returnBranchContextCarrier = RETURN_BRANCH_CONTEXT_CARRIER.get();
        if (returnBranchContextCarrier != null
                && StrUtil.equals(returnBranchContextCarrier.processInstanceId, task.getProcessInstanceId())
                && CollUtil.isNotEmpty(returnBranchContextCarrier.context)
                && StrUtil.isNotBlank(task.getExecutionId())) {
            runtimeService.setVariableLocal(task.getExecutionId(), RETURN_BRANCH_CONTEXT_VARIABLE,
                    new LinkedHashMap<>(returnBranchContextCarrier.context));
            taskService.setVariableLocal(task.getId(), RETURN_BRANCH_CONTEXT_VARIABLE,
                    new LinkedHashMap<>(returnBranchContextCarrier.context));
        }
        ReturnableTaskPathCarrier returnableTaskPathCarrier = RETURNABLE_TASK_PATH_CARRIER.get();
        if (returnableTaskPathCarrier != null
                && StrUtil.equals(returnableTaskPathCarrier.processInstanceId, task.getProcessInstanceId())
                && StrUtil.isNotBlank(task.getExecutionId())) {
            runtimeService.setVariableLocal(task.getExecutionId(), RETURNABLE_TASK_PATH_VARIABLE,
                    returnableTaskPathCarrier.path != null ? new ArrayList<>(returnableTaskPathCarrier.path) : new ArrayList<>());
            taskService.setVariableLocal(task.getId(), RETURNABLE_TASK_PATH_VARIABLE,
                    returnableTaskPathCarrier.path != null ? new ArrayList<>(returnableTaskPathCarrier.path) : new ArrayList<>());
        }

        // 1. 设置为待办中
        Integer status = (Integer) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
        if (status != null) {
            log.error("[updateTaskStatusWhenCreated][taskId({}) 已经有状态({})]", task.getId(), status);
            return;
        }
        updateTaskStatus(task.getId(), BpmTaskStatusEnum.RUNNING.getStatus());

        ProcessInstance processInstance = processInstanceService.getProcessInstance(task.getProcessInstanceId());
        if (processInstance == null) {
            log.error("[processTaskCreated][taskId({}) 没有找到流程实例]", task.getId());
            return;
        }
        BpmProcessDefinitionInfoDO processDefinitionInfo = bpmProcessDefinitionService.
                getProcessDefinitionInfo(processInstance.getProcessDefinitionId());
        if (processDefinitionInfo == null) {
            log.error("[processTaskCreated][processDefinitionId({}) 没有找到流程定义]", processInstance.getProcessDefinitionId());
            return;
        }

        // 2. 任务前置通知
        if (ObjUtil.isNotNull(processDefinitionInfo.getTaskBeforeTriggerSetting())) {
            BpmModelMetaInfoVO.HttpRequestSetting setting = processDefinitionInfo.getTaskBeforeTriggerSetting();
            BpmHttpRequestUtils.executeBpmHttpRequest(processInstance,
                    setting.getUrl(), setting.getHeader(), setting.getBody(), true, setting.getResponse());
        }

        // 3. 处理自动通过的情况，例如说：1）无审批人时，是否自动通过、不通过；2）非【人工审核】时，是否自动通过、不通过
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(processInstance.getProcessDefinitionId());
        FlowElement userTaskElement = BpmnModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        Integer approveType = BpmnModelUtils.parseApproveType(userTaskElement);
        Integer assignEmptyHandlerType = BpmnModelUtils.parseAssignEmptyHandlerType(userTaskElement);
        final Long tenantId = cn.hutool.core.util.NumberUtil.parseLong(task.getTenantId(), 1L);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            /**
             * 特殊情况：部分情况下，TransactionSynchronizationManager 注册 afterCommit 监听时，不会被调用，但是 afterCompletion 可以
             * 例如说：第一个 task 就是配置【自动通过】或者【自动拒绝】时
             * 参见 <a href="https://gitee.com/zhijiantianya/yudao-cloud/issues/IB7V7Q">issue</a> 反馈
             */
            @Override
            public void afterCompletion(int transactionStatus) {
                // 回滚情况，直接返回
                if (ObjectUtil.equal(transactionStatus, TransactionSynchronization.STATUS_ROLLED_BACK)) {
                    return;
                }
                cn.iocoder.yudao.framework.tenant.core.util.TenantUtils.execute(tenantId, () -> {
                    // 特殊情况：第一个 task 【自动通过】时，第二个任务设置审批人时 transactionStatus 会为 STATUS_UNKNOWN，不知道啥原因
                    if (ObjectUtil.equal(transactionStatus, TransactionSynchronization.STATUS_UNKNOWN)
                            && getTask(task.getId()) == null) {
                        return;
                    }
                    // 特殊情况一：【人工审核】审批人为空，根据配置是否要自动通过、自动拒绝
                    if (ObjectUtil.equal(approveType, BpmUserTaskApproveTypeEnum.USER.getType())) {
                        // 如果有审批人、拥有人、候选人，则说明不满足情况一，不自动通过、不自动拒绝
                        if (!ObjectUtil.isAllEmpty(task.getAssignee(), task.getOwner())
                                || hasCandidateUsers(task.getId())) {
                            return;
                        }
                        if (ObjectUtil.equal(assignEmptyHandlerType, BpmUserTaskAssignEmptyHandlerTypeEnum.APPROVE.getType())) {
                            getSelf().approveTask(null, new BpmTaskApproveReqVO()
                                    .setId(task.getId()).setReason(BpmReasonEnum.ASSIGN_EMPTY_APPROVE.getReason()));
                        } else if (ObjectUtil.equal(assignEmptyHandlerType, BpmUserTaskAssignEmptyHandlerTypeEnum.REJECT.getType())) {
                            getSelf().rejectTask(null, new BpmTaskRejectReqVO()
                                    .setId(task.getId()).setReason(BpmReasonEnum.ASSIGN_EMPTY_REJECT.getReason()));
                        }
                        // 特殊情况二：【自动审核】审批类型为自动通过、不通过
                    } else {
                        if (ObjectUtil.equal(approveType, BpmUserTaskApproveTypeEnum.AUTO_APPROVE.getType())) {
                            getSelf().approveTask(null, new BpmTaskApproveReqVO()
                                    .setId(task.getId()).setReason(BpmReasonEnum.APPROVE_TYPE_AUTO_APPROVE.getReason()));
                        } else if (ObjectUtil.equal(approveType, BpmUserTaskApproveTypeEnum.AUTO_REJECT.getType())) {
                            getSelf().rejectTask(null, new BpmTaskRejectReqVO()
                                    .setId(task.getId()).setReason(BpmReasonEnum.APPROVE_TYPE_AUTO_REJECT.getReason()));
                        }
                    }
                });

            }

        });
    }

    private boolean hasCandidateUsers(String taskId) {
        return taskService.getIdentityLinksForTask(taskId).stream()
                .anyMatch(link -> "candidate".equals(link.getType()) && StrUtil.isNotBlank(link.getUserId()));
    }

    /**
     * 重要补充说明：该方法目前主要有两个情况会调用到：
     * <p>
     * 1. 或签场景 + 审批通过：一个或签有多个审批时，如果 A 审批通过，其它或签 B、C 等任务会被 Flowable 自动删除，此时需要通过该方法更新状态为已取消
     * 2. 审批不通过：在 {@link #rejectTask(Long, BpmTaskRejectReqVO)} 不通过时，对于加签的任务，不会被 Flowable 删除，此时需要通过该方法更新状态为已取消
     */
    @Override
    public void processTaskCanceled(String taskId) {
        Task task = getTask(taskId);
        // 1. 可能只是活动，不是任务，所以查询不到
        if (task == null) {
            log.error("[updateTaskStatusWhenCanceled][taskId({}) 任务不存在]", taskId);
            return;
        }

        // 2. 更新 task 状态 + 原因
        Integer status = (Integer) task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS);
        if (BpmTaskStatusEnum.isEndStatus(status)) {
            log.error("[updateTaskStatusWhenCanceled][taskId({}) 处于结果({})，无需进行更新]", taskId, status);
            return;
        }
        updateTaskStatusAndReason(taskId, BpmTaskStatusEnum.CANCEL.getStatus(), BpmReasonEnum.CANCEL_BY_SYSTEM.getReason());
        // 补充说明：由于 Task 被删除成 HistoricTask 后，无法通过 taskService.addComment 添加理由，所以无法存储具体的取消理由
    }

    @Override
    @DataPermission(enable = false) // 忽略数据权限，避免因为过滤，导致找不到候选人
    public void processTaskAssigned(Task task) {
        String taskId = task.getId();
        String executionId = task.getExecutionId();
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String assignee = task.getAssignee();

        // 1. 【核心防御】：提取当前任务自带的租户 ID，防止后台线程执行时丢失上下文
        final Long tenantId = cn.hutool.core.util.NumberUtil.parseLong(task.getTenantId(), 1L);

        // 发送通知。在事务提交时，批量执行操作，所以直接查询会无法查询到 ProcessInstance，所以这里是通过监听事务的提交来实现。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCompletion(int transactionStatus) {
                // 回滚情况，直接返回
                if (cn.hutool.core.util.ObjectUtil.equal(transactionStatus, TransactionSynchronization.STATUS_ROLLED_BACK)) {
                    return;
                }

                // =========================================================================
                // 2. 【终极绝杀】：使用 TenantUtils 包裹整个 afterCompletion 的逻辑！
                // 这样无论是 Web 线程还是 Flowable 后台异步线程，查库时都绝对带有租户钢印！
                // =========================================================================
                TenantUtils.execute(tenantId, () -> {

                    // 特殊情况：第一个 task 【自动通过】时，第二个任务设置审批人时 transactionStatus 会为 STATUS_UNKNOWN
                    if (cn.hutool.core.util.ObjectUtil.equal(transactionStatus, TransactionSynchronization.STATUS_UNKNOWN)
                            && getTask(taskId) == null) {
                        return;
                    }
                    if (cn.hutool.core.util.StrUtil.isEmpty(assignee)) {
                        log.error("[processTaskAssigned][taskId({}) 没有分配到负责人]", task.getId());
                        return;
                    }
                    ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
                    if (processInstance == null) {
                        log.error("[processTaskAssigned][taskId({}) 没有找到流程实例]", task.getId());
                        return;
                    }

                    // 自动去重，通过自动审批的方式
                    BpmProcessDefinitionInfoDO processDefinitionInfo = bpmProcessDefinitionService.getProcessDefinitionInfo(processDefinitionId);
                    if (processDefinitionInfo == null) {
                        log.error("[processTaskAssigned][taskId({}) 没有找到流程定义({})]", task.getId(), task.getProcessDefinitionId());
                        return;
                    }

                    if (processDefinitionInfo.getAutoApprovalType() != null) {
                        HistoricTaskInstanceQuery sameAssigneeQuery = historyService.createHistoricTaskInstanceQuery()
                                .processInstanceId(processInstanceId)
                                .taskAssignee(assignee) // 相同审批人
                                .taskVariableValueEquals(BpmnVariableConstants.TASK_VARIABLE_STATUS, BpmTaskStatusEnum.APPROVE.getStatus())
                                .finished();
                        if (BpmAutoApproveTypeEnum.APPROVE_ALL.getType().equals(processDefinitionInfo.getAutoApprovalType())
                                && sameAssigneeQuery.count() > 0) {
                            getSelf().approveTask(Long.valueOf(assignee), new BpmTaskApproveReqVO().setId(taskId)
                                    .setReason(BpmAutoApproveTypeEnum.APPROVE_ALL.getName()));
                            return;
                        }
                        if (BpmAutoApproveTypeEnum.APPROVE_SEQUENT.getType().equals(processDefinitionInfo.getAutoApprovalType())) {
                            BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(processInstance.getProcessDefinitionId());
                            if (bpmnModel == null) {
                                log.error("[processTaskAssigned][taskId({}) 没有找到流程模型({})]", task.getId(), task.getProcessDefinitionId());
                                return;
                            }
                            List<String> sourceTaskIds = cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList(
                                    BpmnModelUtils.getElementIncomingFlows(BpmnModelUtils.getFlowElementById(bpmnModel, taskDefinitionKey)),
                                    org.flowable.bpmn.model.SequenceFlow::getSourceRef);
                            if (sameAssigneeQuery.taskDefinitionKeys(sourceTaskIds).count() > 0) {
                                getSelf().approveTask(Long.valueOf(assignee), new BpmTaskApproveReqVO().setId(taskId)
                                        .setReason(BpmAutoApproveTypeEnum.APPROVE_SEQUENT.getName()));
                                return;
                            }
                        }
                    }

                    // 获取发起人节点
                    BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(processInstance.getProcessDefinitionId());
                    if (bpmnModel == null) {
                        log.error("[processTaskAssigned][taskId({}) 没有找到流程模型]", task.getId());
                        return;
                    }
                    FlowElement userTaskElement = BpmnModelUtils.getFlowElementById(bpmnModel, taskDefinitionKey);

                    Boolean returnTaskFlag = null;
                    if (cn.hutool.core.util.StrUtil.isNotEmpty(executionId)) {
                        returnTaskFlag = runtimeService.getVariableLocal(executionId,
                                String.format(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RETURN_FLAG, taskDefinitionKey), Boolean.class);
                    }
                    Boolean skipStartUserNodeFlag = cn.hutool.core.convert.Convert.toBool(runtimeService.getVariable(processInstance.getProcessInstanceId(),
                            BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_SKIP_START_USER_NODE, String.class));

                    if (userTaskElement.getId().equals(START_USER_NODE_ID)
                            && (skipStartUserNodeFlag == null || cn.hutool.core.util.BooleanUtil.isTrue(skipStartUserNodeFlag))
                            && cn.hutool.core.util.ObjectUtil.notEqual(returnTaskFlag, Boolean.TRUE)) {
                        getSelf().approveTask(Long.valueOf(assignee), new BpmTaskApproveReqVO().setId(taskId)
                                .setReason(BpmReasonEnum.ASSIGN_START_USER_APPROVE_WHEN_SKIP_START_USER_NODE.getReason()));
                        return;
                    }

                    if (cn.hutool.core.util.ObjectUtil.notEqual(userTaskElement.getId(), START_USER_NODE_ID)
                            && cn.hutool.core.util.StrUtil.equals(assignee, processInstance.getStartUserId())) {
                        if (cn.hutool.core.util.ObjectUtil.notEqual(returnTaskFlag, Boolean.TRUE)) {
                            Integer assignStartUserHandlerType = BpmnModelUtils.parseAssignStartUserHandlerType(userTaskElement);

                            if (cn.iocoder.yudao.framework.common.util.object.ObjectUtils.equalsAny(assignStartUserHandlerType,
                                    BpmUserTaskAssignStartUserHandlerTypeEnum.SKIP.getType())) {
                                getSelf().approveTask(Long.valueOf(assignee), new BpmTaskApproveReqVO().setId(taskId)
                                        .setReason(BpmReasonEnum.ASSIGN_START_USER_APPROVE_WHEN_SKIP.getReason()));
                                return;
                            }
                            if (cn.iocoder.yudao.framework.common.util.object.ObjectUtils.equalsAny(assignStartUserHandlerType,
                                    BpmUserTaskAssignStartUserHandlerTypeEnum.TRANSFER_DEPT_LEADER.getType())) {
                                AdminUserRespDTO startUser = adminUserApi.getUser(Long.valueOf(processInstance.getStartUserId()));
                                cn.hutool.core.lang.Assert.notNull(startUser, "提交人({})信息为空", processInstance.getStartUserId());
                                DeptRespDTO dept = startUser.getDeptId() != null ? deptApi.getDept(startUser.getDeptId()) : null;
                                cn.hutool.core.lang.Assert.notNull(dept, "提交人({})部门({})信息为空", processInstance.getStartUserId(), startUser.getDeptId());

                                if (dept.getLeaderUserId() == null) {
                                    getSelf().approveTask(Long.valueOf(assignee), new BpmTaskApproveReqVO().setId(taskId)
                                            .setReason(BpmReasonEnum.ASSIGN_START_USER_APPROVE_WHEN_DEPT_LEADER_NOT_FOUND.getReason()));
                                    return;
                                }
                                if (cn.hutool.core.util.ObjectUtil.notEqual(dept.getLeaderUserId(), startUser.getId())) {
                                    getSelf().transferTask(Long.valueOf(assignee), new BpmTaskTransferReqVO()
                                            .setId(task.getId()).setAssigneeUserId(dept.getLeaderUserId())
                                            .setReason(BpmReasonEnum.ASSIGN_START_USER_TRANSFER_DEPT_LEADER.getReason()));
                                    return;
                                }
                            }
                        }
                    }

                    // 3. 【极速异步发短信】：剥离组装数据逻辑，只把网络请求扔到异步线程池！
                    AdminUserRespDTO startUser = adminUserApi.getUser(Long.valueOf(processInstance.getStartUserId()));
                    BpmMessageSendWhenTaskCreatedReqDTO messageDTO = BpmTaskConvert.INSTANCE.convert(processInstance, startUser, task);

                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        TenantUtils.execute(tenantId, () -> {
                            try {
                                // 此时子线程已经有了租户 ID，MyBatis-Plus 再也不会报 NullPointerException 了！
                                messageService.sendMessageWhenTaskAssigned(messageDTO);
                            } catch (Exception e) {
                                // 异步线程中的异常必须手动 catch 打印，否则会被吞掉
                                log.error("[processTaskAssigned][taskId({}) 异步发送分配通知失败]", messageDTO.getTaskId(), e);
                            }
                        });
                    });

                }); // ================ TenantUtils.execute 结束 ================
            }
        });
    }


    @Override
    public void processTaskCompleted(Task task) {
        ProcessInstance processInstance = processInstanceService.getProcessInstance(task.getProcessInstanceId());
        if (processInstance == null) {
            log.error("[processTaskCompleted][taskId({}) 没有找到流程实例]", task.getId());
            return;
        }
        BpmProcessDefinitionInfoDO processDefinitionInfo = bpmProcessDefinitionService.
                getProcessDefinitionInfo(processInstance.getProcessDefinitionId());
        if (processDefinitionInfo == null) {
            log.error("[processTaskCompleted][processDefinitionId({}) 没有找到流程定义]", processInstance.getProcessDefinitionId());
            return;
        }

        // 任务后置通知
        if (ObjUtil.isNotNull(processDefinitionInfo.getTaskAfterTriggerSetting())) {
            BpmModelMetaInfoVO.HttpRequestSetting setting = processDefinitionInfo.getTaskAfterTriggerSetting();
            BpmHttpRequestUtils.executeBpmHttpRequest(processInstance,
                    setting.getUrl(), setting.getHeader(), setting.getBody(), true, setting.getResponse());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTaskTimeout(String processInstanceId, String taskDefineKey, Integer handlerType) {
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        if (processInstance == null) {
            log.error("[processTaskTimeout][processInstanceId({}) 没有找到流程实例]", processInstanceId);
            return;
        }
        List<Task> taskList = getRunningTaskListByProcessInstanceId(processInstanceId, true, taskDefineKey);
        // TODO 优化：未来需要考虑加签的情况
        if (CollUtil.isEmpty(taskList)) {
            log.error("[processTaskTimeout][processInstanceId({}) 定义Key({}) 没有找到任务]", processInstanceId, taskDefineKey);
            return;
        }

        taskList.forEach(task -> FlowableUtils.execute(task.getTenantId(), () -> {
            // 情况一：自动提醒
            if (Objects.equals(handlerType, BpmUserTaskTimeoutHandlerTypeEnum.REMINDER.getType())) {
                messageService.sendMessageWhenTaskTimeout(new BpmMessageSendWhenTaskTimeoutReqDTO()
                        .setProcessInstanceId(processInstanceId).setProcessInstanceName(processInstance.getName())
                                .setDueDate(task.getDueDate())
                        .setTaskId(task.getId()).setTaskName(task.getName()).setAssigneeUserId(Long.parseLong(task.getAssignee())));
                return;
            }

            // 情况二：自动同意
            if (Objects.equals(handlerType, BpmUserTaskTimeoutHandlerTypeEnum.APPROVE.getType())) {
                approveTask(Long.parseLong(task.getAssignee()),
                        new BpmTaskApproveReqVO().setId(task.getId()).setReason(BpmReasonEnum.TIMEOUT_APPROVE.getReason()));
                return;
            }

            // 情况三：自动拒绝
            if (Objects.equals(handlerType, BpmUserTaskTimeoutHandlerTypeEnum.REJECT.getType())) {
                rejectTask(Long.parseLong(task.getAssignee()),
                        new BpmTaskRejectReqVO().setId(task.getId()).setReason(BpmReasonEnum.REJECT_TASK.getReason()));
            }
        }));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processChildProcessTimeout(String processInstanceId, String taskDefineKey) {
        List<ActivityInstance> activityInstances = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityId(taskDefineKey).list();
        activityInstances.forEach(activityInstance -> FlowableUtils.execute(activityInstance.getTenantId(),
                () -> moveTaskToEnd(activityInstance.getCalledProcessInstanceId(), BpmReasonEnum.TIMEOUT_APPROVE.getReason())));
    }

    @Override
    public void triggerTask(String processInstanceId, String taskDefineKey) {
        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .activityId(taskDefineKey)
                .singleResult();
        if (execution == null) {
            log.error("[triggerTask][processInstanceId({}) activityId({}) 没有找到执行活动]", processInstanceId, taskDefineKey);
            return;
        }

        // 若存在直接触发接收任务，执行后续节点
        FlowableUtils.execute(execution.getTenantId(),
                () -> runtimeService.trigger(execution.getId()));
    }

    @Override
    public BpmTaskCountRespVO getTaskCount(long userId){
        // 1. 查询待办数量 (Active 的任务)
        long todoCount = taskService.createTaskQuery()
                .or()
                .taskAssignee(String.valueOf(userId)) // 指派给自己
                .taskCandidateUser(String.valueOf(userId)) // 收文登记等候选任务
                .endOr()
                .active()
                .count();

        // 2. 查询已办数量 (历史任务中已完成的)
        long doneCount = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(String.valueOf(userId))
                .finished()
                .processVariableValueNotEquals(PROCESS_INSTANCE_VARIABLE_STATUS,
                        BpmProcessInstanceStatusEnum.INVALID.getStatus())
                .count();

        // 3. 组装返回
        BpmTaskCountRespVO vo = new BpmTaskCountRespVO();
        vo.setTodoCount(todoCount);
        vo.setDoneCount(doneCount);
        vo.setTotalCount(todoCount + doneCount);
        return vo;

    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private BpmTaskServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }


    private LocalDateTime convertDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // @DataPermission(enable = false) // 如果需要管理员操作任意部门的流程，建议加上此注解
    public void finishProcessInstanceByAdmin(Long userId, String processInstanceId, String reason) {
        // 1. 校验流程实例是否存在
        ProcessInstance processInstance = processInstanceService.getProcessInstance(processInstanceId);
        if (processInstance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }

        // 2. 【核心】设置流程结果为“审批通过”
        // RuoYi-Vue-Pro 依赖 PROCESS_RESULT 变量来判断流程最终状态
        // 如果不设置这个，直接跳到结束节点，状态可能是默认值或者空，导致业务表状态更新不正确
        runtimeService.setVariable(processInstanceId,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_RESULT,
                2);
        // 如果没有 BpmProcessInstanceResultEnum，这里直接填 2 (代表通过)

        // 3. 记录操作日志（可选，给当前正在运行的任务加个备注，说被管理员强制结束了）
        List<Task> runningTasks = getRunningTaskListByProcessInstanceId(processInstanceId, null, null);
        if (CollUtil.isNotEmpty(runningTasks)) {
            AdminUserRespDTO adminUser = adminUserApi.getUser(userId);
            String operateName = adminUser != null ? adminUser.getNickname() : "管理员";
            for (Task task : runningTasks) {
                String comment = StrUtil.format("流程被[{}]强制归档，原因：{}", operateName, reason);
                // 添加备注类型为“取消”或其他，视你的业务需求而定
                taskService.addComment(task.getId(), processInstanceId,
                        BpmCommentTypeEnum.CANCEL.getType(), comment);
            }
        }

        // 4. 调用现有的跳转逻辑，将所有活动节点移动到 EndEvent
        // 注意：你现有的 moveTaskToEnd 方法里会把 runningTasks 标记为 Cancel，这符合逻辑（因为这些任务确实没做完）
        // 但因为第2步我们设置了 Result=Approve，所以流程整体结果是“通过”
        moveTaskToEnd(processInstanceId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchApproveTaskIfEnd(Long userId, BpmTaskBatchApproveReqVO reqVO) {
        // 1. 遍历处理
        for (String taskId : reqVO.getIds()) {
            // 2. 校验任务存在性及权限（复用现有校验逻辑）
            Task task = validateTask(userId, taskId);

            // 3. 【核心校验】判断下一节点是否为“主流程的结束节点”
            if (!isNextNodeMainProcessEnd(task)) {
                // 如果不满足条件，直接抛出异常，提示具体的任务名称
                throw exception(PROCESS_INSTANCE_NOT_END, task.getName());
            }

            // 4. 执行审批（复用现有的 approveTask 方法）
            BpmTaskApproveReqVO approveReq = new BpmTaskApproveReqVO()
                    .setId(taskId)
                    .setReason(StrUtil.isBlank(reqVO.getReason()) ? "批量办结" : reqVO.getReason()); // 默认原因
            approveTask(userId, approveReq);
        }
    }

    private boolean isNextNodeMainProcessEnd(Task task) {
        // 1. 获取 BPMN 模型
        BpmnModel bpmnModel = modelService.getBpmnModelByDefinitionId(task.getProcessDefinitionId());
        if (bpmnModel == null) {
            return false;
        }

        // 2. 获取当前任务节点元素
        FlowElement source = BpmnModelUtils.getFlowElementById(bpmnModel, task.getTaskDefinitionKey());
        if (!(source instanceof FlowNode)) {
            return false;
        }
        // 3. 通过递归向下寻找，判断是否能直接/经过网关到达主流程的结束节点
        return checkPathToEnd((FlowNode) source, new HashSet<>());
    }

    private boolean checkPathToEnd(FlowNode node, Set<String> visited) {
        // 防止流程设计中存在环形路由导致死循环
        if (node == null || visited.contains(node.getId())) {
            return false;
        }
        visited.add(node.getId());

        // 获取当前节点的所有流出线
        List<SequenceFlow> outgoingFlows = node.getOutgoingFlows();
        if (CollUtil.isEmpty(outgoingFlows)) {
            return false;
        }

        for (SequenceFlow flow : outgoingFlows) {
            FlowElement target = flow.getTargetFlowElement();

            // 情况 1：直接遇到了结束节点 (EndEvent)
            if (target instanceof EndEvent) {
                // 判断该结束节点的父容器是否为 Process (即排除子流程里的结束节点)
                if (target.getParentContainer() instanceof org.flowable.bpmn.model.Process) {
                    return true;
                }
            }
            // 情况 2：遇到了网关 (Gateway)，继续穿透往下找
            else if (target instanceof Gateway) {
                if (checkPathToEnd((FlowNode) target, visited)) {
                    return true;
                }
            }
            // 如果遇到了 UserTask(用户任务) 或者 SubProcess(子流程)，说明这条路没直接走到大结局，直接跳过看下一条连线
        }

        return false;
    }


}
