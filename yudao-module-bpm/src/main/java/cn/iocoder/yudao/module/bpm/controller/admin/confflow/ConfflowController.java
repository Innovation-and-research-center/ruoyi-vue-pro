package cn.iocoder.yudao.module.bpm.controller.admin.confflow;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Parameters;
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
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.historyworkflow.HistoryWorkflowMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.confflow.ConfflowService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import org.flowable.task.api.Task;

@Tag(name = "管理后台 - 会议报告单")
@RestController
@RequestMapping("/bpm/confflow")
@Validated
public class ConfflowController {

    @Resource
    private ConfflowService confflowService;

    @Resource
    private BpmDeleteOperateLogService bpmDeleteOperateLogService;

    @Resource
    private BpmUpdateOperateLogService bpmUpdateOperateLogService;

    @Resource
    private org.flowable.engine.TaskService flowableTaskService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private HistoryWorkflowMapper historyWorkflowMapper;

    @PostMapping("/create")
    @Operation(summary = "创建会议报告单")
//    @PreAuthorize("@ss.hasPermission('bpm:confflow:create')")
    public CommonResult<Long> createConfflow(@Valid @RequestBody ConfflowSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        return success(confflowService.createConfflow(getLoginUserId(),createReqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存会议报告单草稿")
//    @PreAuthorize("@ss.hasPermission('bpm:confflow:create')")
    public CommonResult<ConfflowSaveRespVO> saveConfflow(@Valid @RequestBody ConfflowSaveReqVO createReqVO) {
        Long userId = getLoginUserId();
        Long confflowId = confflowService.saveConfflow(userId, createReqVO);
        ConfflowDO confflow = confflowService.getConfflow(confflowId);
        String processInstanceId = confflow != null ? confflow.getProcessInstanceId() : null;
        Task registerTask = StrUtil.isBlank(processInstanceId) ? null : flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .active()
                .singleResult();
        return success(new ConfflowSaveRespVO()
                .setId(confflowId)
                .setProcessInstanceId(processInstanceId)
                .setTaskId(registerTask != null ? registerTask.getId() : null));
    }

    @PostMapping("/create-flow")
    @Operation(summary = "草稿发起会议报告单流程")
//    @PreAuthorize("@ss.hasPermission('bpm:confflow:create')")
    public CommonResult<Boolean> createFlowConfflow(@Valid @RequestBody ConfflowSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        confflowService.createFlowConfflow(getLoginUserId(), createReqVO);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新会议报告单")
//    @PreAuthorize("@ss.hasPermission('bpm:confflow:update')")
    public CommonResult<Boolean> updateConfflow(@Valid @RequestBody ConfflowSaveReqVO updateReqVO) {
        ConfflowDO oldData = confflowService.getConfflow(updateReqVO.getId());
        List<ConfflowAttachRespVO> oldAttachments = confflowService.getConfflowAttachListByCommId(updateReqVO.getId());
        confflowService.updateConfflow(updateReqVO);
        ConfflowDO newData = confflowService.getConfflow(updateReqVO.getId());
        List<ConfflowAttachRespVO> newAttachments = confflowService.getConfflowAttachListByCommId(updateReqVO.getId());
        bpmUpdateOperateLogService.recordUpdate("会议报告单", updateReqVO.getId(), oldData, newData,
                oldAttachments, newAttachments);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议报告单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:confflow:delete')")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteConfflow(@RequestParam("id") Long id,@RequestParam("reason") String reason) {
        confflowService.deleteConfflow(id,reason);
        bpmDeleteOperateLogService.recordDelete("会议报告单", id, reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除会议报告单")
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    @PreAuthorize("@ss.hasPermission('bpm:confflow:delete')")
    public CommonResult<Boolean> deleteConfflowList(@RequestParam("ids") List<Long> ids,@RequestParam("reason") String reason) {
        confflowService.deleteConfflowListByIds(ids,reason);
        bpmDeleteOperateLogService.recordDeleteBatch("会议报告单", ids, reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议报告单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('bpm:confflow:query')")
    public CommonResult<ConfflowRespVO> getConfflow(@RequestParam("id") Long id) {
        ConfflowDO confflow = confflowService.getConfflow(id);
        ConfflowRespVO result = BeanUtils.toBean(confflow, ConfflowRespVO.class);
        normalizeHistoryStatus(result);
        // 查询附件列表
        List<ConfflowAttachRespVO> attachList = confflowService.getConfflowAttachListByCommId(id);
        result.setFileList(attachList);
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议报告单分页")
//    @PreAuthorize("@ss.hasPermission('bpm:confflow:query')")
    public CommonResult<PageResult<ConfflowRespVO>> getConfflowPage(@Valid ConfflowPageReqVO pageReqVO) {
        PageResult<ConfflowDO> pageResult = confflowService.getConfflowPage(pageReqVO);
        PageResult<ConfflowRespVO> result = BeanUtils.toBean(pageResult, ConfflowRespVO.class);
        normalizeHistoryStatus(result.getList());
        Set<Long> userIds = collectCreatorUserIds(result.getList());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = userMap.values().stream()
                .map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        result.getList().forEach(vo ->{
            AdminUserRespDTO user = userMap.get(parseCreatorUserId(vo.getCreator()));

            if (user != null) {
                vo.setUserName(user.getNickname());
                DeptRespDTO deptInfo = deptMap.get(user.getDeptId());
                if (deptInfo != null) {
                    vo.setDeptName(deptInfo.getName());
                }
            }
        });
        return success(result);
    }

    private void normalizeHistoryStatus(List<ConfflowRespVO> confflows) {
        List<String> projectIds = confflows.stream()
                .map(ConfflowRespVO::getProjectId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> proinstMap = historyWorkflowMapper.selectProinstByProjectIds(projectIds)
                .stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.get("projectId")), item -> item, (a, b) -> a));
        confflows.forEach(confflow -> {
            if (isFinishedHistoryProcess(proinstMap.get(confflow.getProjectId()))) {
                confflow.setStatus(BpmTaskStatusEnum.APPROVE.getStatus().shortValue());
            }
        });
    }

    private ConfflowRespVO normalizeHistoryStatus(ConfflowRespVO confflow) {
        if (confflow != null && StrUtil.isNotBlank(confflow.getProjectId())
                && isFinishedHistoryProcess(historyWorkflowMapper.selectProinstByProjectId(confflow.getProjectId()))) {
            confflow.setStatus(BpmTaskStatusEnum.APPROVE.getStatus().shortValue());
        }
        return confflow;
    }

    private boolean isFinishedHistoryProcess(Map<String, Object> proinst) {
        if (proinst == null || proinst.isEmpty()) {
            return false;
        }
        String proinstStatus = String.valueOf(proinst.get("proinstStatus"));
        return "2".equals(proinstStatus) || "8".equals(proinstStatus) || proinst.get("endDate") != null;
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会议报告单 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:confflow:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfflowExcel(@Valid ConfflowPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ConfflowDO> list = confflowService.getConfflowPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "会议报告单.xls", "数据", ConfflowRespVO.class,
                        BeanUtils.toBean(list, ConfflowRespVO.class));
    }

    @GetMapping("/confflow-attach/list-by-comm-id")
    @Operation(summary = "获得会议报告单附件列表")
    @Parameter(name = "commId", description = "会议报告单编号(外键t_confflow_attach.comm_id)")
    public CommonResult<List<ConfflowAttachRespVO>> getConfflowAttachListByCommId(@RequestParam("commId") Long commId) {
        return success(confflowService.getConfflowAttachListByCommId(commId));
    }

    private Set<Long> collectCreatorUserIds(List<ConfflowRespVO> list) {
        Set<Long> userIds = new HashSet<>();
        for (ConfflowRespVO vo : list) {
            Long userId = parseCreatorUserId(vo.getCreator());
            if (userId != null) {
                userIds.add(userId);
            }
        }
        return userIds;
    }

    private Long parseCreatorUserId(String creator) {
        if (creator == null || !creator.matches("\\d+")) {
            return null;
        }
        return Long.valueOf(creator);
    }

}
