package cn.iocoder.yudao.module.bpm.controller.admin.fileexchange;

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

import cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.module.bpm.service.fileexchange.FileExchangeService;

@Tag(name = "管理后台 - 文件交换")
@RestController
@RequestMapping("/bpm/file-exchange")
@Validated
public class FileExchangeController {

    @Resource
    private FileExchangeService fileExchangeService;

    @PostMapping("/create")
    @Operation(summary = "创建文件交换")
    @PreAuthorize("@ss.hasPermission('bpm:file-exchange:create')")
    public CommonResult<Long> createFileExchange(@Valid @RequestBody FileExchangeSaveReqVO createReqVO) {
        return success(fileExchangeService.createFileExchange(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新文件交换")
    @PreAuthorize("@ss.hasPermission('bpm:file-exchange:update')")
    public CommonResult<Boolean> updateFileExchange(@Valid @RequestBody FileExchangeSaveReqVO updateReqVO) {
        fileExchangeService.updateFileExchange(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件交换")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:file-exchange:delete')")
    public CommonResult<Boolean> deleteFileExchange(@RequestParam("id") Long id) {
        fileExchangeService.deleteFileExchange(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除文件交换")
                @PreAuthorize("@ss.hasPermission('bpm:file-exchange:delete')")
    public CommonResult<Boolean> deleteFileExchangeList(@RequestParam("ids") List<Long> ids) {
        fileExchangeService.deleteFileExchangeListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得文件交换")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('bpm:file-exchange:query')")
    public CommonResult<FileExchangeRespVO> getFileExchange(@RequestParam("id") Long id) {
        FileExchangeDO fileExchange = fileExchangeService.getFileExchange(id);
        return success(BeanUtils.toBean(fileExchange, FileExchangeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得文件交换分页")
    @PreAuthorize("@ss.hasPermission('bpm:file-exchange:query')")
    public CommonResult<PageResult<FileExchangeRespVO>> getFileExchangePage(@Valid FileExchangePageReqVO pageReqVO) {
        PageResult<FileExchangeDO> pageResult = fileExchangeService.getFileExchangePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, FileExchangeRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出文件交换 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:file-exchange:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFileExchangeExcel(@Valid FileExchangePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<FileExchangeDO> list = fileExchangeService.getFileExchangePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "文件交换.xls", "数据", FileExchangeRespVO.class,
                        BeanUtils.toBean(list, FileExchangeRespVO.class));
    }

}