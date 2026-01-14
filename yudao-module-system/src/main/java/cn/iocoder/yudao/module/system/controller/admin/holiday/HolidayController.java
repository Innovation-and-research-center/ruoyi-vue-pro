package cn.iocoder.yudao.module.system.controller.admin.holiday;

import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffImportExcelVO;
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

import cn.iocoder.yudao.module.system.controller.admin.holiday.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.holiday.HolidayDO;
import cn.iocoder.yudao.module.system.service.holiday.HolidayService;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "管理后台 - 节假日")
@RestController
@RequestMapping("/system/holiday")
@Validated
public class HolidayController {

    @Resource
    private HolidayService holidayService;

    @PostMapping("/create")
    @Operation(summary = "创建节假日")
    @PreAuthorize("@ss.hasPermission('system:holiday:create')")
    public CommonResult<Long> createHoliday(@Valid @RequestBody HolidaySaveReqVO createReqVO) {
        return success(holidayService.createHoliday(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新节假日")
    @PreAuthorize("@ss.hasPermission('system:holiday:update')")
    public CommonResult<Boolean> updateHoliday(@Valid @RequestBody HolidaySaveReqVO updateReqVO) {
        holidayService.updateHoliday(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除节假日")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:holiday:delete')")
    public CommonResult<Boolean> deleteHoliday(@RequestParam("id") Long id) {
        holidayService.deleteHoliday(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除节假日")
                @PreAuthorize("@ss.hasPermission('system:holiday:delete')")
    public CommonResult<Boolean> deleteHolidayList(@RequestParam("ids") List<Long> ids) {
        holidayService.deleteHolidayListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得节假日")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:holiday:query')")
    public CommonResult<HolidayRespVO> getHoliday(@RequestParam("id") Long id) {
        HolidayDO holiday = holidayService.getHoliday(id);
        return success(BeanUtils.toBean(holiday, HolidayRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得节假日分页")
    @PreAuthorize("@ss.hasPermission('system:holiday:query')")
    public CommonResult<PageResult<HolidayRespVO>> getHolidayPage(@Valid HolidayPageReqVO pageReqVO) {
        PageResult<HolidayDO> pageResult = holidayService.getHolidayPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HolidayRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出节假日 Excel")
    @PreAuthorize("@ss.hasPermission('system:holiday:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHolidayExcel(@Valid HolidayPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HolidayDO> list = holidayService.getHolidayPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "节假日.xls", "数据", HolidayRespVO.class,
                        BeanUtils.toBean(list, HolidayRespVO.class));
    }


    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入节假日模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 手动创建导出 demo
        List<HolidayImportExcelVO> list = Arrays.asList(
                HolidayImportExcelVO.builder().settingDate("2026-01-01").isworkday((short) 0).holiDesc("元旦").build(),
                HolidayImportExcelVO.builder().settingDate("2026-01-02").isworkday((short) 0).holiDesc("元旦").build()
        );
        // 输出
        ExcelUtils.write(response, "值班导入模板.xls", "值班列表", HolidayImportExcelVO.class, list);
    }


    @PostMapping("/import")
    @Operation(summary = "导入节假日信息")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('system:holiday:import')")
    public CommonResult<HolidayImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<HolidayImportExcelVO> list = ExcelUtils.read(file, HolidayImportExcelVO.class);
        return success(holidayService.importHolidayList(list, updateSupport));
    }

}