package cn.iocoder.yudao.module.system.controller.admin.dutystaff;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.*;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.dutystaff.DutyStaffService;
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
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;


@Tag(name = "管理后台 - 值班")
@RestController
@RequestMapping("/system/duty/staff")
@Validated
public class DutyStaffController {

    @Resource
    private DutyStaffService staffService;

    @Resource
    private DeptService deptService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建值班")
    @PreAuthorize("@ss.hasPermission('duty:staff:create')")
    public CommonResult<Long> createStaff(@Valid @RequestBody DutyStaffSaveReqVO createReqVO) {
        return success(staffService.createStaff(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新值班")
    @PreAuthorize("@ss.hasPermission('duty:staff:update')")
    public CommonResult<Boolean> updateStaff(@Valid @RequestBody DutyStaffSaveReqVO updateReqVO) {
        staffService.updateStaff(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除值班")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('duty:staff:delete')")
    public CommonResult<Boolean> deleteStaff(@RequestParam("id") Long id) {
        staffService.deleteStaff(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除值班")
                @PreAuthorize("@ss.hasPermission('duty:staff:delete')")
    public CommonResult<Boolean> deleteStaffList(@RequestParam("ids") List<Long> ids) {
        staffService.deleteStaffListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得值班")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('duty:staff:query')")
    public CommonResult<DutyStaffRespVO> getStaff(@RequestParam("id") Long id) {
        DutyStaffDO staff = staffService.getStaff(id);
        return success(BeanUtils.toBean(staff, DutyStaffRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得值班分页")
    @PreAuthorize("@ss.hasPermission('duty:staff:query')")
    public CommonResult<PageResult<DutyStaffRespVO>> getStaffPage(@Valid DutyStaffPageReqVO pageReqVO) {
        PageResult<DutyStaffDO> pageResult = staffService.getStaffPage(pageReqVO);
        PageResult<DutyStaffRespVO> result = BeanUtils.toBean(pageResult, DutyStaffRespVO.class);
        Set<Long> userIds = CollectionUtils.convertSet(result.getList(), DutyStaffRespVO::getUserId);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = CollectionUtils.convertSet(userMap.values(), AdminUserRespDTO::getDeptId);
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(deptIds);
        result.getList().forEach(vo -> {
            // 获取用户
            AdminUserRespDTO user = userMap.get(vo.getUserId());
            if (user != null) {
                // 获取部门并设置名称
                DeptDO dept = deptMap.get(user.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
        });
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出值班 Excel")
    @PreAuthorize("@ss.hasPermission('duty:staff:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStaffExcel(@Valid DutyStaffPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DutyStaffDO> list = staffService.getStaffPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "值班.xls", "数据", DutyStaffRespVO.class,
                        BeanUtils.toBean(list, DutyStaffRespVO.class));
    }


    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入值班模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 手动创建导出 demo
        List<DutyStaffImportExcelVO> list = Arrays.asList(
                DutyStaffImportExcelVO.builder().dutyDate("2022-02-02").leader("张三").staff("李四").build(),
                DutyStaffImportExcelVO.builder().dutyDate("2022-02-03").leader("张三").staff("王五").build()
        );
        // 输出
        ExcelUtils.write(response, "值班导入模板.xls", "值班列表", DutyStaffImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入值班信息")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('duty:staff:import')")
    public CommonResult<DutyImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<DutyStaffImportExcelVO> list = ExcelUtils.read(file, DutyStaffImportExcelVO.class);
        return success(staffService.importDutyList(list, updateSupport));
    }

}