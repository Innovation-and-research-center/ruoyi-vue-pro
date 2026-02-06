package cn.iocoder.yudao.module.system.controller.admin.userdept;

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

import cn.iocoder.yudao.module.system.controller.admin.userdept.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.userdept.UserDeptDO;
import cn.iocoder.yudao.module.system.service.userdept.UserDeptService;

@Tag(name = "管理后台 - 用户部门关联")
@RestController
@RequestMapping("/system/user-dept")
@Validated
public class UserDeptController {

    @Resource
    private UserDeptService userDeptService;

    @PostMapping("/create")
    @Operation(summary = "创建用户部门关联")
    @PreAuthorize("@ss.hasPermission('system:user-dept:create')")
    public CommonResult<Long> createUserDept(@Valid @RequestBody UserDeptSaveReqVO createReqVO) {
        return success(userDeptService.createUserDept(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户部门关联")
    @PreAuthorize("@ss.hasPermission('system:user-dept:update')")
    public CommonResult<Boolean> updateUserDept(@Valid @RequestBody UserDeptSaveReqVO updateReqVO) {
        userDeptService.updateUserDept(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户部门关联")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:user-dept:delete')")
    public CommonResult<Boolean> deleteUserDept(@RequestParam("id") Long id) {
        userDeptService.deleteUserDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除用户部门关联")
                @PreAuthorize("@ss.hasPermission('system:user-dept:delete')")
    public CommonResult<Boolean> deleteUserDeptList(@RequestParam("ids") List<Long> ids) {
        userDeptService.deleteUserDeptListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户部门关联")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user-dept:query')")
    public CommonResult<UserDeptRespVO> getUserDept(@RequestParam("id") Long id) {
        UserDeptDO userDept = userDeptService.getUserDept(id);
        return success(BeanUtils.toBean(userDept, UserDeptRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户部门关联分页")
    @PreAuthorize("@ss.hasPermission('system:user-dept:query')")
    public CommonResult<PageResult<UserDeptRespVO>> getUserDeptPage(@Valid UserDeptPageReqVO pageReqVO) {
        PageResult<UserDeptDO> pageResult = userDeptService.getUserDeptPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, UserDeptRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出用户部门关联 Excel")
    @PreAuthorize("@ss.hasPermission('system:user-dept:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserDeptExcel(@Valid UserDeptPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<UserDeptDO> list = userDeptService.getUserDeptPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "用户部门关联.xls", "数据", UserDeptRespVO.class,
                        BeanUtils.toBean(list, UserDeptRespVO.class));
    }
    @GetMapping("/get-dept-ids")
    @Operation(summary = "获得用户关联的部门ID列表")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:user-dept:query')")
    public CommonResult<Set<Long>> getUserDeptIds(@RequestParam("userId") Long userId) {
        return success(userDeptService.getUserDeptIds(userId));
    }

    @PutMapping("/assign-dept")
    @Operation(summary = "赋予用户关联部门(分管)")
    @PreAuthorize("@ss.hasPermission('system:user-dept:update')")
    public CommonResult<Boolean> assignUserDept(@Validated @RequestBody UserAssignDeptReqVO reqVO) {
        userDeptService.assignUserDept(reqVO);
        return success(true);
    }



}