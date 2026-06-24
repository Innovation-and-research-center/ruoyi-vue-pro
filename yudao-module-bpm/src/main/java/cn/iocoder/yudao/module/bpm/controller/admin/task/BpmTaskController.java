package cn.iocoder.yudao.module.bpm.controller.admin.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.NumberUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.*;
import cn.iocoder.yudao.module.bpm.convert.task.BpmTaskConvert;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmFormDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmFormService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 流程任务实例")
@RestController
@RequestMapping("/bpm/task")
@Validated
public class BpmTaskController {

    @Resource
    private BpmTaskService taskService;
    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmFormService formService;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    @Resource
    private HistoryService historyService;

    @Resource
    private RepositoryService repositoryService;


    @GetMapping("todo-page")
    @Operation(summary = "获取 Todo 待办任务分页")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<PageResult<BpmTaskRespVO>> getTaskTodoPage(@Valid BpmTaskPageReqVO pageVO) {
        PageResult<Task> pageResult = taskService.getTaskTodoPage(getLoginUserId(), pageVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }
        // 拼接数据
        Map<String, ProcessInstance> processInstanceMap = processInstanceService.getProcessInstanceMap(
                convertSet(pageResult.getList(), Task::getProcessInstanceId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(processInstanceMap.values(), instance -> Long.valueOf(instance.getStartUserId())));
        Map<String, BpmProcessDefinitionInfoDO> processDefinitionInfoMap = processDefinitionService.getProcessDefinitionInfoMap(
                convertSet(pageResult.getList(), Task::getProcessDefinitionId));
        return success(BpmTaskConvert.INSTANCE.buildTodoTaskPage(pageResult, processInstanceMap, userMap, processDefinitionInfoMap));
    }

    @GetMapping("done-page")
    @Operation(summary = "获取 Done 已办任务分页")
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<PageResult<BpmTaskRespVO>> getTaskDonePage(@Valid BpmTaskPageReqVO pageVO) {
        PageResult<HistoricTaskInstance> pageResult = taskService.getTaskDonePage(getLoginUserId(), pageVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }

        // 拼接数据
        Map<String, HistoricProcessInstance> processInstanceMap = processInstanceService.getHistoricProcessInstanceMap(
                convertSet(pageResult.getList(), HistoricTaskInstance::getProcessInstanceId));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(processInstanceMap.values(), instance -> Long.valueOf(instance.getStartUserId())));
        Map<String, BpmProcessDefinitionInfoDO> processDefinitionInfoMap = processDefinitionService.getProcessDefinitionInfoMap(
                convertSet(pageResult.getList(), HistoricTaskInstance::getProcessDefinitionId));
        return success(BpmTaskConvert.INSTANCE.buildTaskPage(pageResult, processInstanceMap, userMap, null, processDefinitionInfoMap));
    }

    @GetMapping("manager-page")
    @Operation(summary = "获取全部任务的分页", description = "用于【流程任务】菜单")
    @PreAuthorize("@ss.hasPermission('bpm:task:mananger-query')")
    public CommonResult<PageResult<BpmTaskRespVO>> getTaskManagerPage(@Valid BpmTaskPageReqVO pageVO) {
        PageResult<HistoricTaskInstance> pageResult = taskService.getTaskPage(getLoginUserId(), pageVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(PageResult.empty());
        }

        // 拼接数据
        Map<String, HistoricProcessInstance> processInstanceMap = processInstanceService.getHistoricProcessInstanceMap(
                convertSet(pageResult.getList(), HistoricTaskInstance::getProcessInstanceId));
        // 获得 User 和 Dept Map
        Set<Long> userIds = convertSet(processInstanceMap.values(), instance -> Long.valueOf(instance.getStartUserId()));
        userIds.addAll(convertSet(pageResult.getList(), task -> NumberUtils.parseLong(task.getAssignee())));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        Map<String, BpmProcessDefinitionInfoDO> processDefinitionInfoMap = processDefinitionService.getProcessDefinitionInfoMap(
                convertSet(pageResult.getList(), HistoricTaskInstance::getProcessDefinitionId));
        return success(BpmTaskConvert.INSTANCE.buildTaskPage(pageResult, processInstanceMap, userMap, deptMap, processDefinitionInfoMap));
    }

    @PutMapping("/claim")
    @Operation(summary = "认领候选任务")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> claimTask(@RequestParam("id") String id) {
        taskService.claimTask(getLoginUserId(), id);
        return success(true);
    }

    @GetMapping("/list-by-process-instance-id")
    @Operation(summary = "获得指定流程实例的任务列表", description = "包括完成的、未完成的")
    @Parameter(name = "processInstanceId", description = "流程实例的编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<List<BpmTaskRespVO>> getTaskListByProcessInstanceId(
            @RequestParam("processInstanceId") String processInstanceId) {
        List<HistoricTaskInstance> taskList = taskService.getTaskListByProcessInstanceId(processInstanceId, true);

        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (CollUtil.isEmpty(taskList)) {
            return success(Collections.emptyList());
        }

        // 拼接数据
        Set<Long> userIds = new HashSet<>();
        if (CollUtil.isNotEmpty(taskList)) {
            userIds.addAll(convertSetByFlatMap(taskList, task ->
                    Stream.of(NumberUtils.parseLong(task.getAssignee()), NumberUtils.parseLong(task.getOwner()))));
        }
        if (processInstance != null && processInstance.getStartUserId() != null) {
            userIds.add(NumberUtils.parseLong(processInstance.getStartUserId()));
        }
//        Set<Long> userIds = convertSetByFlatMap(taskList, task ->
//                Stream.of(NumberUtils.parseLong(task.getAssignee()), NumberUtils.parseLong(task.getOwner())));
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        // 获得 Form Map
        Map<Long, BpmFormDO> formMap = formService.getFormMap(
                convertSet(taskList, task -> NumberUtils.parseLong(task.getFormKey())));
        List<BpmTaskRespVO> resultList = BpmTaskConvert.INSTANCE.buildTaskListByProcessInstanceId(taskList,
                formMap, userMap, deptMap);

        if (processInstance != null) {
            BpmTaskRespVO startNode = new BpmTaskRespVO();
            String startNodeName = "流程发起";
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processInstance.getProcessDefinitionId());
            if (bpmnModel != null) {
                // 在主流程中查找类型为 StartEvent 的节点
                FlowElement startElement = bpmnModel.getMainProcess().getFlowElements().stream()
                        .filter(e -> e instanceof StartEvent)
                        .findFirst()
                        .orElse(null);

                if (startElement != null) {
                    // 如果画图时填了名称就用填的，没填就用默认的"流程发起"
                    if (StrUtil.isNotEmpty(startElement.getName())) {
                        startNodeName = startElement.getName();
                    }
                }
            }
            startNode.setId("StartEvent");      // 设置为 BPMN XML 里的真实 ID (例如 StartEvent_1)
            startNode.setName(startNodeName);
//            startNode.setName("流程发起"); // 节点名称
            if (processInstance.getStartTime() != null) {
                LocalDateTime startTime = processInstance.getStartTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                startNode.setCreateTime(startTime);
                startNode.setEndTime(startTime); // 发起即结束
            }
            startNode.setDurationInMillis(0L);
            startNode.setStatus(2); // 假设 2 代表“已完成/审批通过”，根据你的前端字典调整

            // 设置处理人信息 (利用之前查出来的 userMap)
            Long startUserId = NumberUtils.parseLong(processInstance.getStartUserId());
            AdminUserRespDTO startUser = userMap.get(startUserId);
            if (startUser != null) {
                // 根据 BpmTaskRespVO 的结构设置用户
                // 假设你的 VO 里有 assigneeUser 对象
                UserSimpleBaseVO userVO = new UserSimpleBaseVO();
                userVO.setId(startUser.getId());
                userVO.setNickname(startUser.getNickname());
                userVO.setDeptId(startUser.getDeptId());
                if (deptMap.get(startUser.getDeptId()) != null) {
                    userVO.setDeptName(deptMap.get(startUser.getDeptId()).getName());
                }
                startNode.setAssigneeUser(userVO);
            }

            // 插入到列表头部 (Index 0)
            resultList.add(0, startNode);
        }
        return success(resultList);
//        return success(BpmTaskConvert.INSTANCE.buildTaskListByProcessInstanceId(taskList,
//                formMap, userMap, deptMap));
    }

    @PutMapping("/approve")
    @Operation(summary = "通过任务")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> approveTask(@Valid @RequestBody BpmTaskApproveReqVO reqVO) {
        taskService.approveTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PostMapping("/add-comment")
    @Operation(summary = "通过任务")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> addComment(@Valid @RequestBody BpmTaskApproveReqVO reqVO) {
        taskService.addComment(getLoginUserId(), reqVO);
        return success(true);
    }


    @PutMapping("/reject")
    @Operation(summary = "不通过任务")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> rejectTask(@Valid @RequestBody BpmTaskRejectReqVO reqVO) {
        taskService.rejectTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @GetMapping("/list-by-return")
    @Operation(summary = "获取所有可退回的节点", description = "用于【流程详情】的【退回】按钮")
    @Parameter(name = "taskId", description = "当前任务ID", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<List<BpmTaskRespVO>> getTaskListByReturn(@RequestParam("id") String id) {
        List<UserTask> userTaskList = taskService.getUserTaskListByReturn(id);
        return success(convertList(userTaskList, userTask -> // 只返回 id 和 name
                new BpmTaskRespVO().setName(userTask.getName()).setTaskDefinitionKey(userTask.getId())));
    }

    @PutMapping("/return")
    @Operation(summary = "退回任务", description = "用于【流程详情】的【退回】按钮")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> returnTask(@Valid @RequestBody BpmTaskReturnReqVO reqVO) {
        taskService.returnTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/delegate")
    @Operation(summary = "委派任务", description = "用于【流程详情】的【委派】按钮")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> delegateTask(@Valid @RequestBody BpmTaskDelegateReqVO reqVO) {
        taskService.delegateTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/transfer")
    @Operation(summary = "转派任务", description = "用于【流程详情】的【转派】按钮")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> transferTask(@Valid @RequestBody BpmTaskTransferReqVO reqVO) {
        taskService.transferTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/create-sign")
    @Operation(summary = "加签", description = "before 前加签，after 后加签")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> createSignTask(@Valid @RequestBody BpmTaskSignCreateReqVO reqVO) {
        taskService.createSignTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @DeleteMapping("/delete-sign")
    @Operation(summary = "减签")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> deleteSignTask(@Valid @RequestBody BpmTaskSignDeleteReqVO reqVO) {
        taskService.deleteSignTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/copy")
    @Operation(summary = "抄送任务")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> copyTask(@Valid @RequestBody BpmTaskCopyReqVO reqVO) {
        taskService.copyTask(getLoginUserId(), reqVO);
        return success(true);
    }

    @PutMapping("/withdraw")
    @Operation(summary = "撤回任务")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> withdrawTask(@RequestParam("taskId") String taskId) {
        taskService.withdrawTask(getLoginUserId(), taskId);
        return success(true);
    }
    @GetMapping("/trace/{taskId}")
    public CommonResult<BpmTaskTraceDTO> getTaskTrace(
            @Parameter(description = "任务ID", required = true) @PathVariable("taskId") String taskId,
            @Parameter(description = "查询类型: 1-前置(来源), 2-后置(去向)", required = true) @RequestParam("type") Integer type,
            @RequestParam(value = "processInstanceId", required = false) String processInstanceId) {
        if (type == null || (type != 1 && type != 2)) {
           type=1;
        }
        BpmTaskTraceDTO trace = taskService.getTaskTrace(taskId,processInstanceId,type);
        return success(trace);
    }

    @GetMapping("/list-by-parent-task-id")
    @Operation(summary = "获得指定父级任务的子任务列表") // 目前用于，减签的时候，获得子任务列表
    @Parameter(name = "parentTaskId", description = "父级任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
    public CommonResult<List<BpmTaskRespVO>> getTaskListByParentTaskId(@RequestParam("parentTaskId") String parentTaskId) {
        List<Task> taskList = taskService.getTaskListByParentTaskId(parentTaskId);
        if (CollUtil.isEmpty(taskList)) {
            return success(Collections.emptyList());
        }
        // 拼接数据
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSetByFlatMap(taskList,
                user -> Stream.of(NumberUtils.parseLong(user.getAssignee()), NumberUtils.parseLong(user.getOwner()))));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(userMap.values(), AdminUserRespDTO::getDeptId));
        return success(BpmTaskConvert.INSTANCE.buildTaskListByParentTaskId(taskList, userMap, deptMap));
    }

    @GetMapping("/get-count")
    @Operation(summary = "获得当前用户的待办和已办任务数量")
    public CommonResult<BpmTaskCountRespVO> getTaskCount() {
        Long userId = getLoginUserId();
        BpmTaskCountRespVO respVO = taskService.getTaskCount(userId);
        return success(respVO);

    }

    @DeleteMapping("/finish-by-admin")
    @Operation(summary = "管理员强制结束流程（模拟正常完成）")
    @Parameter(name = "processInstanceId", description = "流程实例的编号", required = true)
    @Parameter(name = "reason", description = "结束原因", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:task:complate')") // 建议使用更高级别的权限，如 bpm:process-instance:cancel
    public CommonResult<Boolean> finishProcessInstanceByAdmin(
            @RequestParam("processInstanceId") String processInstanceId,
            @RequestParam("reason") String reason) {

        taskService.finishProcessInstanceByAdmin(getLoginUserId(), processInstanceId, reason);
        return success(true);
    }

    @PutMapping("/batch-approve-end")
    @Operation(summary = "批量办结任务（仅限下一节点为主流程结束的任务）")
    @PreAuthorize("@ss.hasPermission('bpm:task:update')")
    public CommonResult<Boolean> batchApproveTaskIfEnd(@Valid @RequestBody BpmTaskBatchApproveReqVO reqVO) {
        taskService.batchApproveTaskIfEnd(getLoginUserId(), reqVO);
        return success(true);
    }



//    @GetMapping("/next-node-list")
//    @Operation(summary = "获得指定任务下，可执行的节点列表")
//    @Parameter(name = "taskId", description = "任务编号", required = true)
//    @PreAuthorize("@ss.hasPermission('bpm:task:query')")
//    public CommonResult<List<BpmTaskRespVO>> getNextNodeList(@RequestParam("id") String id) {
//        List<UserTask> userTaskList = taskService.getNextNodeList(id);
//        return success(convertList(userTaskList, userTask -> // 只返回 id 和 name
//                new BpmTaskRespVO().setName(userTask.
//                        getName()).setTaskDefinitionKey(userTask.getId())));
//    }

}
