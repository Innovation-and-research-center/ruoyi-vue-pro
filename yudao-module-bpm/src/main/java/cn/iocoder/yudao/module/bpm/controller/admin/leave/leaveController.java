package cn.iocoder.yudao.module.bpm.controller.admin.leave;

import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.service.leave.LeaveService;

@Tag(name = "管理后台 - 假期申请审批")
@RestController
@RequestMapping("/bpm/leave")
@Validated
public class LeaveController {

    @Resource
    private LeaveService leaveService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建假期申请审批")
    @PreAuthorize("@ss.hasPermission('bpm:leave:create')")
    public CommonResult<Long> createLeave(@Valid @RequestBody LeaveSaveReqVO createReqVO) {
        return success(leaveService.createLeave(getLoginUserId(),createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新假期申请审批")
    @PreAuthorize("@ss.hasPermission('bpm:leave:update')")
    public CommonResult<Boolean> updateLeave(@Valid @RequestBody LeaveSaveReqVO updateReqVO) {
        leaveService.updateLeave(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除假期申请审批")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:leave:delete')")
    public CommonResult<Boolean> deleteLeave(@RequestParam("id") Long id) {
        leaveService.deleteLeave(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除假期申请审批")
    @PreAuthorize("@ss.hasPermission('bpm:leave:delete')")
    public CommonResult<Boolean> deleteLeaveList(@RequestParam("ids") List<Long> ids) {
        leaveService.deleteLeaveListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得假期申请审批")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('bpm:leave:query')")
    @DataPermission(enable = false)
    public CommonResult<LeaveRespVO> getLeave(@RequestParam("id") Long id) {
        LeaveDO leave = leaveService.getLeave(id);
        AdminUserRespDTO startUser = adminUserApi.getUser(Long.valueOf(leave.getCreator()));
        DeptRespDTO dept = deptApi.getDept(startUser.getDeptId());
        LeaveRespVO result = BeanUtils.toBean(leave, LeaveRespVO.class);
        result.setDeptName(dept.getName());
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得假期申请审批分页")
    @PreAuthorize("@ss.hasPermission('bpm:leave:query')")
    public CommonResult<PageResult<LeaveRespVO>> getLeavePage(@Valid LeavePageReqVO pageReqVO) {
        PageResult<LeaveDO> pageResult = leaveService.getLeavePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LeaveRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出假期申请审批 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:leave:export')")
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
    @PreAuthorize("@ss.hasPermission('bpm:leave:summary')")
    @Operation(summary = "获得请假统计列表", description = "根据年份、月份、部门、人员统计请假数据")
    public CommonResult<List<LeaveSummaryRespVO>> getLeaveSummary(@Valid LeaveSummaryReqVO reqVO) {
        return success(leaveService.getLeaveSummary(reqVO));
    }


    @GetMapping("/detail-list")
    @Operation(summary = "获得请假详细记录", description = "用于点击统计数字后查看详情")
    @PreAuthorize("@ss.hasPermission('bpm:leave:summary')")
    public CommonResult<List<LeaveDO>> getLeaveDetailList(@Valid LeaveSummaryReqVO reqVO) {
        return success(leaveService.getLeaveDetailList(reqVO));
    }



}