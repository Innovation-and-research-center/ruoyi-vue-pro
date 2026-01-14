package cn.iocoder.yudao.module.bpm.controller.admin.xzfy;

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

import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyKzDO;
import cn.iocoder.yudao.module.bpm.service.xzfy.XzfyService;

@Tag(name = "管理后台 - 行政复议")
@RestController
@RequestMapping("/bpm/xzfy")
@Validated
public class XzfyController {

    @Resource
    private XzfyService xzfyService;

    @PostMapping("/create")
    @Operation(summary = "创建行政复议")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:create')")
    public CommonResult<Long> createXzfy(@Valid @RequestBody XzfySaveReqVO createReqVO) {
        return success(xzfyService.createXzfy(getLoginUserId(),createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新行政复议")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:update')")
    public CommonResult<Boolean> updateXzfy(@Valid @RequestBody XzfySaveReqVO updateReqVO) {
        xzfyService.updateXzfy(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除行政复议")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:delete')")
    public CommonResult<Boolean> deleteXzfy(@RequestParam("id") Long id) {
        xzfyService.deleteXzfy(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除行政复议")
                @PreAuthorize("@ss.hasPermission('bpm:xzfy:delete')")
    public CommonResult<Boolean> deleteXzfyList(@RequestParam("ids") List<Long> ids) {
        xzfyService.deleteXzfyListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得行政复议")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:query')")
    public CommonResult<XzfyRespVO> getXzfy(@RequestParam("id") Long id) {
        XzfyDO xzfy = xzfyService.getXzfy(id);
        return success(BeanUtils.toBean(xzfy, XzfyRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得行政复议分页")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:query')")
    public CommonResult<PageResult<XzfyRespVO>> getXzfyPage(@Valid XzfyPageReqVO pageReqVO) {
        PageResult<XzfyDO> pageResult = xzfyService.getXzfyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, XzfyRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出行政复议 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportXzfyExcel(@Valid XzfyPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<XzfyDO> list = xzfyService.getXzfyPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "行政复议.xls", "数据", XzfyRespVO.class,
                        BeanUtils.toBean(list, XzfyRespVO.class));
    }

    // ==================== 子表（行政复议扩展） ====================

    @GetMapping("/xzfy-kz/get-by-xm-guid")
    @Operation(summary = "获得行政复议扩展")
    @Parameter(name = "xmGuid", description = "备用主键")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:query')")
    public CommonResult<XzfyKzDO> getXzfyKzByXmGuid(@RequestParam("xmGuid") String xmGuid) {
        return success(xzfyService.getXzfyKzByXmGuid(xmGuid));
    }

}