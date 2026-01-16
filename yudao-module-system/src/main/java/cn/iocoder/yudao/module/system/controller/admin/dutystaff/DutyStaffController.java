package cn.iocoder.yudao.module.system.controller.admin.dutystaff;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.*;
import cn.iocoder.yudao.module.system.api.dict.DictDataApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.idev.excel.FastExcelFactory;
import cn.hutool.core.collection.CollUtil;
import java.net.URLEncoder;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
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

    @Resource
    private DictDataApi dictDataApi;

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
        // 1. 获取字典数据
        List<DictDataRespDTO> dictDataList = dictDataApi.getDictDataList("duty_staff_type");
        if (CollUtil.isEmpty(dictDataList)) {
            // 如果字典为空，至少保留基础列
            ExcelUtils.write(response, "值班导入模板.xls", "值班列表", DutyStaffImportExcelVO.class, Collections.emptyList());
            return;
        }

        // 2. 构建动态表头：日期 + 字典标签
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("日期"));
        dictDataList.forEach(dict -> head.add(Collections.singletonList(dict.getLabel())));

        // 3. 构建示例数据（可选，这里给一行空数据即可，或者具体示例）
        List<List<Object>> data = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        row.add("2023-01-01"); // 日期示例
        dictDataList.forEach(dict -> row.add("张三")); // 默认示例填充
        data.add(row);

        // 4. 手动使用 FastExcel 导出
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("值班导入模板.xls", "UTF-8"));
        FastExcelFactory.write(response.getOutputStream())
                .head(head)
                .sheet("值班列表")
                .doWrite(data);
    }

    @PostMapping("/import")
    @Operation(summary = "导入值班信息")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('duty:staff:import')")
    public CommonResult<DutyImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport)
            throws Exception {
        // 读取所有行，包括表头（headRowNumber(0)）
        List<Map<Integer, String>> list = FastExcelFactory.read(file.getInputStream()).sheet().headRowNumber(0)
                .doReadSync();
        if (CollUtil.isEmpty(list)) {
            return success(DutyImportRespVO.builder().failureDutyNames(Collections.emptyMap()).build());
        }

        // 第一行为表头
        Map<Integer, String> headerMap = list.get(0);
        List<Map<String, Object>> dataList = new ArrayList<>();

        // 从第二行开始遍历数据
        for (int i = 1; i < list.size(); i++) {
            Map<Integer, String> data = list.get(i);
            Map<String, Object> rowMap = new HashMap<>();

            // 遍历每一列数据，根据 headerMap 转换 key
            for (Map.Entry<Integer, String> entry : data.entrySet()) {
                Integer colIndex = entry.getKey();
                String val = entry.getValue();
                String headerName = headerMap.get(colIndex);
                if (headerName != null) {
                    rowMap.put(headerName, val);
                }
            }
            if (!rowMap.isEmpty()) {
                dataList.add(rowMap);
            }
        }

        return success(staffService.importDutyList(dataList, updateSupport));
    }

}