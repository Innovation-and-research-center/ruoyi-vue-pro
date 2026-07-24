package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import io.swagger.v3.oas.annotations.Parameters;
import jodd.util.StringUtil;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import javax.servlet.http.*;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_REGISTER_TASK;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.historyworkflow.HistoryWorkflowMapper;
import cn.iocoder.yudao.module.bpm.job.CityNoticeJob;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;

@Tag(name = "管理后台 - 收文")
@RestController
@RequestMapping("/bpm/receive-doc")
@Validated
public class ReceiveDocController {

    @Resource
    private ReceiveDocService receiveDocService;

    @Resource
    private BpmDeleteOperateLogService bpmDeleteOperateLogService;

    @Resource
    private BpmUpdateOperateLogService bpmUpdateOperateLogService;

    @Resource
    private org.flowable.engine.TaskService flowableTaskService;

    @Resource
    private Environment environment;

    @Resource
    private CityNoticeJob cityNoticeJob;

    @Resource
    private HistoryWorkflowMapper historyWorkflowMapper;

    @PostMapping("/create")
    @Operation(summary = "创建收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Long> createReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        return success(receiveDocService.createReceiveDoc(getLoginUserId(),createReqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<ReceiveDocSaveRespVO> saveReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO createReqVO) {
        Long userId = getLoginUserId();
        Long receiveDocId = receiveDocService.saveReceiveDoc(userId,createReqVO);
        ReceiveDocDO receiveDoc = receiveDocService.getReceiveDoc(receiveDocId);
        return success(new ReceiveDocSaveRespVO()
                .setId(receiveDocId)
                .setProcessInstanceId(receiveDoc != null ? receiveDoc.getProcessInstanceId() : null)
                .setTaskId(getReceiveRegisterTaskId(userId, receiveDoc)));
    }

    @PostMapping("/create-flow")
    @Operation(summary = "创建收文流程")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Boolean> createFlowReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        receiveDocService.createFlowReceiveDoc(getLoginUserId(),createReqVO);
        return success(true);
    }

    private String getReceiveRegisterTaskId(Long userId, ReceiveDocDO receiveDoc) {
        if (receiveDoc == null || StrUtil.isBlank(receiveDoc.getProcessInstanceId())) {
            return null;
        }
        String userIdStr = String.valueOf(userId);
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .processInstanceId(receiveDoc.getProcessInstanceId())
                .taskDefinitionKey(RECEIVE_REGISTER_TASK)
                .active()
                .list();
        Task receiveRegisterTask = tasks.stream()
                .filter(item -> StrUtil.equals(userIdStr, item.getAssignee()))
                .findFirst()
                .orElse(tasks.isEmpty() ? null : tasks.get(0));
        return receiveRegisterTask != null ? receiveRegisterTask.getId() : null;
    }

    @PostMapping("/local/mock-job-create")
    @Operation(summary = "本地模拟定时任务创建收文")
    @ApiAccessLog(enable = false)
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Map<String, Object>> mockJobCreateReceiveDoc(
            @RequestParam(value = "docClass", defaultValue = "7") String docClass,
            @RequestParam(value = "startUserId", required = false) Long startUserId) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (!activeProfiles.contains("local") && !activeProfiles.contains("dev")) {
            throw new IllegalStateException("仅 local/dev 环境允许模拟定时任务创建收文");
        }

        try {
        ReceiveDocCreateNumberVO numberReqVO = new ReceiveDocCreateNumberVO();
        numberReqVO.setDocClass(docClass);
        numberReqVO.setYear(String.valueOf(LocalDateTime.now().getYear()));

        ReceiveDocSaveReqVO reqVO = new ReceiveDocSaveReqVO();
        reqVO.setDocClass(docClass);
        reqVO.setYear(numberReqVO.getYear());
        reqVO.setReceiveDocNumber(receiveDocService.generateDocumentSequence(numberReqVO));
        reqVO.setReceiveTime(LocalDateTime.now());
        reqVO.setSendDept("本地模拟定时任务");
        reqVO.setSendDocNumber("LOCAL-JOB-" + System.currentTimeMillis());
        reqVO.setSubject("本地模拟定时任务收文-" + System.currentTimeMillis());
        reqVO.setUrgencyDegree("1");
        reqVO.setDocSecondClass("测试");
        reqVO.setRemark("本地模拟定时任务创建，用于验证收文登记候选待办");

        Long userId = startUserId != null ? startUserId : getLoginUserId();
        Long receiveDocId = receiveDocService.saveJobReceiveDoc(userId, reqVO);
        ReceiveDocDO receiveDoc = receiveDocService.getReceiveDoc(receiveDocId);
        List<Task> activeTasks = flowableTaskService.createTaskQuery()
                .processInstanceId(receiveDoc.getProcessInstanceId())
                .active()
                .list();
        List<String> candidateUserIds = activeTasks.stream()
                .flatMap(task -> flowableTaskService.getIdentityLinksForTask(task.getId()).stream())
                .filter(link -> "candidate".equals(link.getType()))
                .map(IdentityLink::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("receiveDocId", receiveDocId);
        result.put("processInstanceId", receiveDoc.getProcessInstanceId());
        result.put("receiveDocNumber", receiveDoc.getReceiveDocNumber());
        result.put("activeTaskNames", activeTasks.stream().map(Task::getName).collect(Collectors.toList()));
        result.put("activeTaskDefinitionKeys",
                activeTasks.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList()));
        result.put("activeTaskAssignees", activeTasks.stream().map(Task::getAssignee).collect(Collectors.toList()));
        result.put("candidateUserIds", candidateUserIds);
        return success(result);
        } catch (Throwable ex) {
            Map<String, Object> result = new HashMap<>();
            result.put("errorClass", ex.getClass().getName());
            result.put("errorMessage", ex.getMessage());
            result.put("stackTrace", Arrays.stream(ex.getStackTrace())
                    .limit(12)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.toList()));
            return success(result);
        }
    }

    @PostMapping("/local/mock-city-notice")
    @Operation(summary = "本地模拟市局公告定时任务创建收文")
    @ApiAccessLog(enable = false)
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Map<String, Object>> mockCityNoticeReceiveDoc(
            @RequestParam(value = "uuid", required = false) String uuid,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit,
            @RequestParam(value = "repeat", defaultValue = "true") Boolean repeat) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (!activeProfiles.contains("local") && !activeProfiles.contains("dev")) {
            throw new IllegalStateException("仅 local/dev 环境允许模拟定时任务创建收文");
        }

        if (StrUtil.isBlank(uuid)) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (String noticeUuid : cityNoticeJob.getMockNoticeUuids(limit)) {
                try {
                    Long receiveDocId = cityNoticeJob.syncSingleMockNotice(noticeUuid, Boolean.TRUE.equals(repeat));
                    items.add(buildMockCityNoticeResult(noticeUuid, receiveDocId));
                } catch (Exception ex) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("noticeUuid", noticeUuid);
                    item.put("created", false);
                    item.put("errorClass", ex.getClass().getName());
                    item.put("errorMessage", ex.getMessage());
                    items.add(item);
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("limit", limit);
            result.put("repeat", repeat);
            result.put("total", items.size());
            result.put("createdCount", items.stream()
                    .filter(item -> Boolean.TRUE.equals(item.get("created")))
                    .count());
            result.put("items", items);
            return success(result);
        }

        Long receiveDocId = cityNoticeJob.syncSingleMockNotice(uuid, Boolean.TRUE.equals(repeat));
        return success(buildMockCityNoticeResult(uuid, receiveDocId));
    }

    private Map<String, Object> buildMockCityNoticeResult(String noticeUuid, Long receiveDocId) {
        Map<String, Object> result = new HashMap<>();
        result.put("noticeUuid", noticeUuid);
        result.put("created", receiveDocId != null);
        result.put("receiveDocId", receiveDocId);
        if (receiveDocId == null) {
            result.put("message", "mock 公告已同步过，未重复创建");
            return result;
        }

        ReceiveDocDO receiveDoc = receiveDocService.getReceiveDoc(receiveDocId);
        result.put("processInstanceId", receiveDoc.getProcessInstanceId());
        result.put("receiveDocNumber", receiveDoc.getReceiveDocNumber());
        result.put("subject", receiveDoc.getSubject());
        List<Task> activeTasks = flowableTaskService.createTaskQuery()
                .processInstanceId(receiveDoc.getProcessInstanceId())
                .active()
                .list();
        result.put("activeTaskNames", activeTasks.stream().map(Task::getName).collect(Collectors.toList()));
        result.put("activeTaskDefinitionKeys",
                activeTasks.stream().map(Task::getTaskDefinitionKey).collect(Collectors.toList()));
        result.put("activeTaskAssignees", activeTasks.stream().map(Task::getAssignee).collect(Collectors.toList()));
        result.put("candidateUserIds", activeTasks.stream()
                .flatMap(task -> flowableTaskService.getIdentityLinksForTask(task.getId()).stream())
                .filter(link -> "candidate".equals(link.getType()))
                .map(IdentityLink::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList()));
        return result;
    }


    @PostMapping("/get-number")
    @Operation(summary = "获取收文编号")
    public CommonResult<String> getReceiveDocNumber(@RequestBody  ReceiveDocCreateNumberVO createReqVO) {
        return success(receiveDocService.generateDocumentSequence(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:update')")
    public CommonResult<Boolean> updateReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO updateReqVO) {
        ReceiveDocDO oldData = receiveDocService.getReceiveDoc(updateReqVO.getId());
        List<ReceiveFileRespVO> oldAttachments = receiveDocService.getReceiveDocAttachListByReceiveDocId(updateReqVO.getId());
        receiveDocService.updateReceiveDoc(updateReqVO);
        ReceiveDocDO newData = receiveDocService.getReceiveDoc(updateReqVO.getId());
        List<ReceiveFileRespVO> newAttachments = receiveDocService.getReceiveDocAttachListByReceiveDocId(updateReqVO.getId());
        bpmUpdateOperateLogService.recordUpdate("收文", updateReqVO.getId(), oldData, newData,
                oldAttachments, newAttachments);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收文")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:delete')")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteReceiveDoc(@RequestParam("id") Long id,@RequestParam("reason") String reason) {
        receiveDocService.deleteReceiveDoc(id,reason);
        bpmDeleteOperateLogService.recordDelete("收文", id, reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:delete')")
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteReceiveDocList(@RequestParam("ids") List<Long> ids,@RequestParam("reason") String reason) {
        receiveDocService.deleteReceiveDocListByIds(ids,reason);
        bpmDeleteOperateLogService.recordDeleteBatch("收文", ids, reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收文")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:query')")
    public CommonResult<ReceiveDocRespVO> getReceiveDoc(@RequestParam("id") Long id) {
        ReceiveDocDO receiveDoc = receiveDocService.getReceiveDoc(id);
        return success(normalizeHistoryStatus(BeanUtils.toBean(receiveDoc, ReceiveDocRespVO.class)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得收文分页")
//    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:query')")
    public CommonResult<PageResult<ReceiveDocRespVO>> getReceiveDocPage(@Valid ReceiveDocPageReqVO pageReqVO) {
        PageResult<ReceiveDocDO> pageResult = receiveDocService.getReceiveDocPage(pageReqVO);
        PageResult<ReceiveDocRespVO> result = BeanUtils.toBean(pageResult, ReceiveDocRespVO.class);
        normalizeHistoryStatus(result.getList());
        return success(result);
    }

    private void normalizeHistoryStatus(List<ReceiveDocRespVO> receiveDocs) {
        List<String> projectIds = receiveDocs.stream()
                .map(ReceiveDocRespVO::getProjectId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> proinstMap = historyWorkflowMapper.selectProinstByProjectIds(projectIds)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get("projectId")), item -> item, (a, b) -> a));
        receiveDocs.forEach(receiveDoc -> {
            Map<String, Object> proinst = proinstMap.get(receiveDoc.getProjectId());
            if (isFinishedHistoryProcess(proinst)) {
                receiveDoc.setStatus(BpmTaskStatusEnum.APPROVE.getStatus().shortValue());
            } else if (proinst != null) {
                receiveDoc.setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue());
            }
        });
    }

    private ReceiveDocRespVO normalizeHistoryStatus(ReceiveDocRespVO receiveDoc) {
        if (receiveDoc != null && StrUtil.isNotBlank(receiveDoc.getProjectId())) {
            Map<String, Object> proinst = historyWorkflowMapper.selectProinstByProjectId(receiveDoc.getProjectId());
            if (isFinishedHistoryProcess(proinst)) {
                receiveDoc.setStatus(BpmTaskStatusEnum.APPROVE.getStatus().shortValue());
            } else if (proinst != null) {
                receiveDoc.setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue());
            }
        }
        return receiveDoc;
    }

    private boolean isFinishedHistoryProcess(Map<String, Object> proinst) {
        if (proinst == null || proinst.isEmpty()) {
            return false;
        }
        String proinstStatus = String.valueOf(proinst.get("proinstStatus"));
        return "2".equals(proinstStatus) || "8".equals(proinstStatus) || proinst.get("endDate") != null;
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出收文 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceiveDocExcel(@Valid ReceiveDocPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ReceiveDocDO> list = receiveDocService.getReceiveDocPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "收文.xls", "数据", ReceiveDocRespVO.class,
                        BeanUtils.toBean(list, ReceiveDocRespVO.class));
    }


    @GetMapping("/receive-doc-attach/list-by-receive-doc-id")
    @Operation(summary = "获得收文附件列表")
    @Parameter(name = "receiveDocId", description = "收文编号(外键T_RECEIVE_DOC.RECEIVE_DOC_ID)")
//    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:query')")
    public CommonResult<List<ReceiveFileRespVO>> getReceiveDocAttachListByReceiveDocId(@RequestParam("receiveDocId") Long receiveDocId) {
        return success(receiveDocService.getReceiveDocAttachListByReceiveDocId(receiveDocId));
    }

    @GetMapping("/get-pending-count")
    @Operation(summary = "获得待收文数量")
    public CommonResult<Long> getPendingCount() {
        return success(receiveDocService.getPendingCount());
    }

    @PostMapping("/backfill-source-unit")
    @Operation(summary = "补设流程变量 PROCESS_SOURCE_UNIT（一次性修复）")
    public CommonResult<Integer> backfillSourceUnit() {
        return success(receiveDocService.backfillSourceUnit());
    }

}
