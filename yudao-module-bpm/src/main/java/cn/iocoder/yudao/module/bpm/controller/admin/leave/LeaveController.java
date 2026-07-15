package cn.iocoder.yudao.module.bpm.controller.admin.leave;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.leave.LeaveService;
import org.flowable.task.api.Task;

@Slf4j
@Tag(name = "管理后台 - 假期申请审批")
@RestController
@RequestMapping("/bpm/leave")
@Validated
public class LeaveController {

    @Resource
    private LeaveService leaveService;

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

    @PostMapping("/create")
    @Operation(summary = "创建假期申请审批")
    public CommonResult<Long> createLeave(@Valid @RequestBody LeaveSaveReqVO createReqVO) {
        parseProcessVariables(createReqVO);
        return success(leaveService.createLeave(getLoginUserId(),createReqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存请假并生成登记待办")
    public CommonResult<LeaveSaveRespVO> saveLeave(@Valid @RequestBody LeaveSaveReqVO createReqVO) {
        parseProcessVariables(createReqVO);
        Long userId = getLoginUserId();
        Long leaveId = leaveService.saveLeave(userId, createReqVO);
        LeaveDO leave = leaveService.getLeave(leaveId);
        String processInstanceId = leave != null ? leave.getProcessInstanceId() : null;
        Task registerTask = StrUtil.isBlank(processInstanceId) ? null : flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskAssignee(String.valueOf(userId))
                .active()
                .singleResult();
        return success(new LeaveSaveRespVO()
                .setId(leaveId)
                .setProcessInstanceId(processInstanceId)
                .setTaskId(registerTask != null ? registerTask.getId() : null));
    }

    @PostMapping("/create-flow")
    @Operation(summary = "提交已保存的请假登记")
    public CommonResult<Boolean> createFlowLeave(@Valid @RequestBody LeaveSaveReqVO updateReqVO) {
        parseProcessVariables(updateReqVO);
        leaveService.createFlowLeave(getLoginUserId(), updateReqVO);
        return success(true);
    }

    private void parseProcessVariables(LeaveSaveReqVO reqVO) {
        if (StrUtil.isNotEmpty(reqVO.getProcessVariablesStr())) {
            reqVO.setProcessVariables(JsonUtils.parseObject(reqVO.getProcessVariablesStr(), Map.class));
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新假期申请审批")
    public CommonResult<Boolean> updateLeave(@Valid @RequestBody LeaveSaveReqVO updateReqVO) {
        LeaveDO oldData = leaveService.getLeave(updateReqVO.getId());
        List<LeaveAttachRespVO> oldAttachments = leaveService.getLeaveAttachListByLeaveId(updateReqVO.getId());
        leaveService.updateLeave(updateReqVO);
        LeaveDO newData = leaveService.getLeave(updateReqVO.getId());
        List<LeaveAttachRespVO> newAttachments = leaveService.getLeaveAttachListByLeaveId(updateReqVO.getId());
        bpmUpdateOperateLogService.recordUpdate("假期申请审批", updateReqVO.getId(), oldData, newData,
                oldAttachments, newAttachments);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除假期申请审批")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteLeave(@RequestParam("id") Long id,
                                             @RequestParam("reason") String reason) {
        // 注意：这里需要你同步修改 LeaveService 层，让它能接收并处理 reason 参数
        leaveService.deleteLeave(id, reason);
        bpmDeleteOperateLogService.recordDelete("假期申请审批", id, reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除假期申请审批")
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteLeaveList(@RequestParam("ids") List<Long> ids,
                                                 @RequestParam("reason") String reason) {
        // 注意：同步修改 LeaveService 层的批量删除逻辑
        leaveService.deleteLeaveListByIds(ids, reason);
        bpmDeleteOperateLogService.recordDeleteBatch("假期申请审批", ids, reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得假期申请审批")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @DataPermission(enable = false)
    public CommonResult<LeaveDetailRespVO> getLeave(@RequestParam("id") Long id) {
        LeaveDetailRespVO detail = leaveService.getLeaveDetail(id);
        Long creatorUserId = parseCreatorUserId(detail.getCreator());
        if (creatorUserId != null) {
            AdminUserRespDTO startUser = adminUserApi.getUser(creatorUserId);
            DeptRespDTO dept = startUser != null && startUser.getDeptId() != null ? deptApi.getDept(startUser.getDeptId()) : null;
            detail.setDeptName(dept != null ? dept.getName() : "");
            detail.setNickName(startUser != null ? startUser.getNickname() : "");
        }
        return success(detail);
    }

    @GetMapping("/page")
    @Operation(summary = "获得假期申请审批分页")
    public CommonResult<PageResult<LeaveRespVO>> getLeavePage(@Valid LeavePageReqVO pageReqVO) {
        PageResult<LeaveDO> pageResult = leaveService.getLeavePage(pageReqVO);
        PageResult<LeaveRespVO> result = BeanUtils.toBean(pageResult, LeaveRespVO.class);
        Set<Long> userIds = collectApplyUserIds(result.getList());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        result.getList().forEach(vo ->{
            AdminUserRespDTO user = userMap.get(getApplyUserId(vo));
            if (user != null) {
                vo.setNickName(user.getNickname());
                // 如果需要部门或其他信息，也可以在这里设置
            }
        });
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出假期申请审批 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLeaveExcel(@Valid LeavePageReqVO pageReqVO,
                                 HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LeaveDO> list = leaveService.getLeavePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "假期申请审批.xls", "数据", LeaveRespVO.class,
                BeanUtils.toBean(list, LeaveRespVO.class));
    }

    @GetMapping("/summary")
    @DataPermission(enable = false)
    @Operation(summary = "获得请假统计列表", description = "根据年份、月份、部门、人员统计请假数据")
    public CommonResult<List<LeaveSummaryRespVO>> getLeaveSummary(@Valid LeaveSummaryReqVO reqVO) {
        return success(leaveService.getLeaveSummary(reqVO));
    }


    @GetMapping("/detail-list")
    @Operation(summary = "获得请假详细记录", description = "用于点击统计数字后查看详情")
    public CommonResult<List<LeaveHistoryRespVO>> getLeaveDetailList(@Valid LeaveSummaryReqVO reqVO) {
        return success(leaveService.getLeaveDetailList(reqVO));
    }

    @GetMapping("/history")
    @Operation(summary = "获得请假历史记录", description = "用于获取请假历史记录")
    public CommonResult<PageResult<LeaveHistoryRespVO>> getLeaveHistoryList(@Valid LeavePageReqVO reqVO) {
        PageResult<LeaveHistoryRespVO> pageResult = leaveService.getLeaveHistoryPage(reqVO);
        return success(pageResult);
    }

    @GetMapping("/get-yearly-stat")
    @Operation(summary = "获得当前用户本年度的请假类型统计")
    public CommonResult<List<LeaveTypeStatRespVO>> getCurrentUserYearlyLeaveStat() {
        // 直接调用 service 获取统计结果
        List<LeaveTypeStatRespVO> statList = leaveService.getCurrentUserYearlyLeaveStat();
        return success(statList);
    }

    @GetMapping("/leave-attach/list-by-leave-id")
    @Operation(summary = "获得请假附件列表")
    @Parameter(name = "leaveId", description = "请假编号(外键t_leave_attact.leave_id)")
    public CommonResult<List<LeaveAttachRespVO>> getLeaveAttachListByLeaveId(@RequestParam("leaveId") Long leaveId) {
        return success(leaveService.getLeaveAttachListByLeaveId(leaveId));
    }

    private Set<Long> collectApplyUserIds(List<LeaveRespVO> list) {
        Set<Long> userIds = new HashSet<>();
        for (LeaveRespVO vo : list) {
            Long userId = getApplyUserId(vo);
            if (userId != null) {
                userIds.add(userId);
            }
        }
        return userIds;
    }

    private Long getApplyUserId(LeaveRespVO vo) {
        if (vo.getUserid() != null) {
            return vo.getUserid().longValue();
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
