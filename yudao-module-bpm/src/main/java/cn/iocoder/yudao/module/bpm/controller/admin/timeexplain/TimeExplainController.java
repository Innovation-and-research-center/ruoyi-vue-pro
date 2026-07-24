package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.LeaveRespVO;
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

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.historyworkflow.HistoryWorkflowMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.timeexplain.TimeExplainService;
import org.flowable.task.api.Task;

@Tag(name = "管理后台 - 外出请假补假")
@RestController
@RequestMapping("/bpm/time-explain")
@Validated
public class TimeExplainController {

    @Resource
    private TimeExplainService timeExplainService;

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
    @Operation(summary = "创建外出请假补假")
    public CommonResult<Long> createTimeExplain(@Valid @RequestBody TimeExplainSaveReqVO createReqVO) {
        return success(timeExplainService.createTimeExplain(createReqVO));
    }

    @PostMapping("/createout")
    @Operation(summary = "创建外出")
    public CommonResult<Long> createOut(@Valid @RequestBody TimeExplainSaveReqVO createReqVO) {
        parseProcessVariables(createReqVO);
        return success(timeExplainService.createOut(getLoginUserId(),createReqVO));
    }

    @PostMapping("/save-out")
    @Operation(summary = "保存公出并生成登记待办")
    public CommonResult<TimeExplainSaveRespVO> saveOut(@Valid @RequestBody TimeExplainSaveReqVO createReqVO) {
        parseProcessVariables(createReqVO);
        Long userId = getLoginUserId();
        Long id = timeExplainService.saveOut(userId, createReqVO);
        TimeExplainDO out = timeExplainService.getTimeExplain(id);
        String processInstanceId = out != null ? out.getProcessInstanceId() : null;
        Task task = StrUtil.isBlank(processInstanceId) ? null : flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId).taskAssignee(String.valueOf(userId)).active().singleResult();
        return success(new TimeExplainSaveRespVO().setId(id).setProcessInstanceId(processInstanceId)
                .setTaskId(task != null ? task.getId() : null));
    }

    @PostMapping("/create-flow-out")
    @Operation(summary = "提交已保存的公出登记")
    public CommonResult<Boolean> createFlowOut(@Valid @RequestBody TimeExplainSaveReqVO reqVO) {
        parseProcessVariables(reqVO);
        timeExplainService.createFlowOut(getLoginUserId(), reqVO);
        return success(true);
    }

    private void parseProcessVariables(TimeExplainSaveReqVO reqVO) {
        if (StrUtil.isNotEmpty(reqVO.getProcessVariablesStr())) {
            reqVO.setProcessVariables(JsonUtils.parseObject(reqVO.getProcessVariablesStr(), Map.class));
        }
    }



    @PutMapping("/update")
    @Operation(summary = "更新外出请假补假")
    public CommonResult<Boolean> updateTimeExplain(@Valid @RequestBody TimeExplainSaveReqVO updateReqVO) {
        TimeExplainDO oldData = timeExplainService.getTimeExplain(updateReqVO.getId());
        List<TimeExplainAttachRespVO> oldAttachments = timeExplainService.getTimeExplainAttachListByTimeExplainId(updateReqVO.getId());
        timeExplainService.updateTimeExplain(updateReqVO);
        TimeExplainDO newData = timeExplainService.getTimeExplain(updateReqVO.getId());
        List<TimeExplainAttachRespVO> newAttachments = timeExplainService.getTimeExplainAttachListByTimeExplainId(updateReqVO.getId());
        bpmUpdateOperateLogService.recordUpdate("外出请假补假", updateReqVO.getId(), oldData, newData,
                oldAttachments, newAttachments);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外出请假补假")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteTimeExplain(@RequestParam("id") Long id,@RequestParam("reason") String reason) {
        timeExplainService.deleteTimeExplain(id,reason);
        bpmDeleteOperateLogService.recordDelete("外出请假补假", id, reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteTimeExplainList(@RequestParam("ids") List<Long> ids,@RequestParam("reason") String reason) {
        timeExplainService.deleteTimeExplainListByIds(ids,reason);
        bpmDeleteOperateLogService.recordDeleteBatch("外出请假补假", ids, reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得外出请假补假")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @DataPermission(enable = false)
    public CommonResult<TimeExplainRespVO> getTimeExplain(@RequestParam("id") Long id) {
        TimeExplainDO timeExplain = timeExplainService.getTimeExplain(id);
        TimeExplainRespVO result = BeanUtils.toBean(timeExplain, TimeExplainRespVO.class);
        Long creatorUserId = parseCreatorUserId(timeExplain.getCreator());
        if (creatorUserId != null) {
            AdminUserRespDTO startUser = adminUserApi.getUser(creatorUserId);
            DeptRespDTO dept = startUser != null && startUser.getDeptId() != null ? deptApi.getDept(startUser.getDeptId()) : null;
            result.setDeptName(dept != null ? dept.getName() : "");
            result.setUserName(startUser != null ? startUser.getNickname() : "");
        }
        // 查询附件列表
        List<TimeExplainAttachRespVO> attachList = timeExplainService.getTimeExplainAttachListByTimeExplainId(id);
        result.setFileList(attachList);
        normalizeHistoryStatus(Collections.singletonList(result));
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得外出请假补假分页")
    public CommonResult<PageResult<TimeExplainRespVO>> getTimeExplainPage(@Valid TimeExplainPageReqVO pageReqVO) {
        PageResult<TimeExplainDO> pageResult = timeExplainService.getTimeExplainPage(pageReqVO);
        PageResult<TimeExplainRespVO> result = BeanUtils.toBean(pageResult, TimeExplainRespVO.class);
        Set<Long> userIds = collectApplyUserIds(result.getList());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        normalizeHistoryStatus(result.getList());
        result.getList().forEach(vo ->{
            if (vo.getUserName() != null && !vo.getUserName().isEmpty()) {
                vo.setNickName(vo.getUserName());
                return;
            }
            AdminUserRespDTO user = userMap.get(getApplyUserId(vo));
            if (user != null) {
                vo.setNickName(user.getNickname());
                // 如果需要部门或其他信息，也可以在这里设置
            }
        });
        return success(result);
    }

    private void normalizeHistoryStatus(List<TimeExplainRespVO> timeExplains) {
        List<String> projectIds = timeExplains.stream()
                .map(TimeExplainRespVO::getProjectId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (projectIds.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> proinstMap = historyWorkflowMapper.selectProinstByProjectIds(projectIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> String.valueOf(item.get("projectId")), item -> item, (a, b) -> a));
        timeExplains.forEach(timeExplain -> {
            if (isFinishedHistoryProcess(proinstMap.get(timeExplain.getProjectId()))) {
                timeExplain.setStatus(BpmProcessInstanceStatusEnum.APPROVE.getStatus().longValue());
            }
        });
    }

    private boolean isFinishedHistoryProcess(Map<String, Object> proinst) {
        if (proinst == null || proinst.isEmpty()) {
            return false;
        }
        String proinstStatus = String.valueOf(proinst.get("proinstStatus"));
        return "2".equals(proinstStatus) || "8".equals(proinstStatus) || proinst.get("endDate") != null;
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出外出请假补假 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportTimeExplainExcel(@Valid TimeExplainPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<TimeExplainDO> list = timeExplainService.getTimeExplainPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "外出请假补假.xls", "数据", TimeExplainRespVO.class,
                        BeanUtils.toBean(list, TimeExplainRespVO.class));
    }

    @GetMapping("/time-explain-attach/list-by-time-explain-id")
    @Operation(summary = "获得外出请假补假附件列表")
    @Parameter(name = "timeExplainId", description = "外出请假补假编号(外键t_time_explain_attach.time_explain_id)")
    public CommonResult<List<TimeExplainAttachRespVO>> getTimeExplainAttachListByTimeExplainId(@RequestParam("timeExplainId") Long timeExplainId) {
        return success(timeExplainService.getTimeExplainAttachListByTimeExplainId(timeExplainId));
    }

    private Set<Long> collectApplyUserIds(List<TimeExplainRespVO> list) {
        Set<Long> userIds = new HashSet<>();
        for (TimeExplainRespVO vo : list) {
            Long userId = getApplyUserId(vo);
            if (userId != null) {
                userIds.add(userId);
            }
        }
        return userIds;
    }

    private Long getApplyUserId(TimeExplainRespVO vo) {
        if (vo.getUserId() != null) {
            return vo.getUserId();
        }
        return parseCreatorUserId(vo.getCreator());
    }

    private Long parseCreatorUserId(String creator) {
        if (creator == null || !creator.matches("\\d+")) {
            return null;
        }
        return Long.valueOf(creator);
    }

}
