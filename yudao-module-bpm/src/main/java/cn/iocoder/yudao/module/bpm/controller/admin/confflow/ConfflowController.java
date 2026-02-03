package cn.iocoder.yudao.module.bpm.controller.admin.confflow;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.LeaveRespVO;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.module.bpm.service.confflow.ConfflowService;

@Tag(name = "管理后台 - 会议报告单")
@RestController
@RequestMapping("/bpm/confflow")
@Validated
public class ConfflowController {

    @Resource
    private ConfflowService confflowService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @PostMapping("/create")
    @Operation(summary = "创建会议报告单")
    @PreAuthorize("@ss.hasPermission('bpm:confflow:create')")
    public CommonResult<Long> createConfflow(@Valid @RequestBody ConfflowSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        return success(confflowService.createConfflow(getLoginUserId(),createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会议报告单")
    @PreAuthorize("@ss.hasPermission('bpm:confflow:update')")
    public CommonResult<Boolean> updateConfflow(@Valid @RequestBody ConfflowSaveReqVO updateReqVO) {
        confflowService.updateConfflow(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议报告单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:confflow:delete')")
    public CommonResult<Boolean> deleteConfflow(@RequestParam("id") Long id) {
        confflowService.deleteConfflow(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除会议报告单")
                @PreAuthorize("@ss.hasPermission('bpm:confflow:delete')")
    public CommonResult<Boolean> deleteConfflowList(@RequestParam("ids") List<Long> ids) {
        confflowService.deleteConfflowListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议报告单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('bpm:confflow:query')")
    public CommonResult<ConfflowRespVO> getConfflow(@RequestParam("id") Long id) {
        ConfflowDO confflow = confflowService.getConfflow(id);
        return success(BeanUtils.toBean(confflow, ConfflowRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议报告单分页")
    @PreAuthorize("@ss.hasPermission('bpm:confflow:query')")
    public CommonResult<PageResult<ConfflowRespVO>> getConfflowPage(@Valid ConfflowPageReqVO pageReqVO) {
        PageResult<ConfflowDO> pageResult = confflowService.getConfflowPage(pageReqVO);
        PageResult<ConfflowRespVO> result = BeanUtils.toBean(pageResult, ConfflowRespVO.class);
        Set<Long> userIds = convertSet(result.getList(), ConfflowRespVO::getCreator);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        result.getList().forEach(vo ->{
            AdminUserRespDTO user = userMap.get(vo.getCreator());

            if (user != null) {
                vo.setUserName(user.getNickname());
                DeptRespDTO deptInfo = deptApi.getDept(user.getDeptId());
                vo.setDeptName(deptInfo.getName());
                // 如果需要部门或其他信息，也可以在这里设置
            }
        });
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会议报告单 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:confflow:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConfflowExcel(@Valid ConfflowPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ConfflowDO> list = confflowService.getConfflowPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "会议报告单.xls", "数据", ConfflowRespVO.class,
                        BeanUtils.toBean(list, ConfflowRespVO.class));
    }

}