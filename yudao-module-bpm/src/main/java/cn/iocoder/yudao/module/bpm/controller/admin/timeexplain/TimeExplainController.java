package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain;

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

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.module.bpm.service.timeexplain.TimeExplainService;

@Tag(name = "管理后台 - 外出请假补假")
@RestController
@RequestMapping("/bpm/time-explain")
@Validated
public class TimeExplainController {

    @Resource
    private TimeExplainService timeExplainService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建外出请假补假")
    public CommonResult<Long> createTimeExplain(@Valid @RequestBody TimeExplainSaveReqVO createReqVO) {
        return success(timeExplainService.createTimeExplain(createReqVO));
    }

    @PostMapping("/createout")
    @Operation(summary = "创建外出")
    public CommonResult<Long> createOut(@Valid @RequestBody TimeExplainSaveReqVO createReqVO) {
        return success(timeExplainService.createOut(getLoginUserId(),createReqVO));
    }



    @PutMapping("/update")
    @Operation(summary = "更新外出请假补假")
    public CommonResult<Boolean> updateTimeExplain(@Valid @RequestBody TimeExplainSaveReqVO updateReqVO) {
        timeExplainService.updateTimeExplain(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除外出请假补假")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteTimeExplain(@RequestParam("id") Long id) {
        timeExplainService.deleteTimeExplain(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除外出请假补假")
    public CommonResult<Boolean> deleteTimeExplainList(@RequestParam("ids") List<Long> ids) {
        timeExplainService.deleteTimeExplainListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得外出请假补假")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @DataPermission(enable = false)
    public CommonResult<TimeExplainRespVO> getTimeExplain(@RequestParam("id") Long id) {
        TimeExplainDO timeExplain = timeExplainService.getTimeExplain(id);
        AdminUserRespDTO startUser = adminUserApi.getUser(Long.valueOf(timeExplain.getCreator()));
        DeptRespDTO dept = deptApi.getDept(startUser.getDeptId());
        TimeExplainRespVO result = BeanUtils.toBean(timeExplain, TimeExplainRespVO.class);
        result.setDeptName(dept.getName());
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得外出请假补假分页")
    public CommonResult<PageResult<TimeExplainRespVO>> getTimeExplainPage(@Valid TimeExplainPageReqVO pageReqVO) {
        PageResult<TimeExplainDO> pageResult = timeExplainService.getTimeExplainPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TimeExplainRespVO.class));
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

}