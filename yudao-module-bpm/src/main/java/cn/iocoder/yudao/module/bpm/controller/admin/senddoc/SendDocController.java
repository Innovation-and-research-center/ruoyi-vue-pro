package cn.iocoder.yudao.module.bpm.controller.admin.senddoc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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

import cn.iocoder.yudao.module.bpm.controller.admin.senddoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.senddoc.SendDocDO;
import cn.iocoder.yudao.module.bpm.service.senddoc.SendDocService;

@Tag(name = "管理后台 - 发文")
@RestController
@RequestMapping("/bpm/send-doc")
@Validated
public class SendDocController {

    @Resource
    private SendDocService sendDocService;

    @PostMapping("/create")
    @Operation(summary = "创建发文")
    @PreAuthorize("@ss.hasPermission('bpm:send-doc:create')")
    public CommonResult<Long> createSendDoc(@Valid @RequestBody SendDocSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        return success(sendDocService.createSendDoc(getLoginUserId(),createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新发文")
    @PreAuthorize("@ss.hasPermission('bpm:send-doc:update')")
    public CommonResult<Boolean> updateSendDoc(@Valid @RequestBody SendDocSaveReqVO updateReqVO) {
        sendDocService.updateSendDoc(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除发文")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:send-doc:delete')")
    public CommonResult<Boolean> deleteSendDoc(@RequestParam("id") Long id) {
        sendDocService.deleteSendDoc(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除发文")
                @PreAuthorize("@ss.hasPermission('bpm:send-doc:delete')")
    public CommonResult<Boolean> deleteSendDocList(@RequestParam("ids") List<Long> ids) {
        sendDocService.deleteSendDocListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得发文")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('bpm:send-doc:query')")
    public CommonResult<SendDocRespVO> getSendDoc(@RequestParam("id") Long id) {
        SendDocDO sendDoc = sendDocService.getSendDoc(id);
        return success(BeanUtils.toBean(sendDoc, SendDocRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得发文分页")
    @PreAuthorize("@ss.hasPermission('bpm:send-doc:query')")
    public CommonResult<PageResult<SendDocRespVO>> getSendDocPage(@Valid SendDocPageReqVO pageReqVO) {
        PageResult<SendDocDO> pageResult = sendDocService.getSendDocPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, SendDocRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出发文 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:send-doc:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSendDocExcel(@Valid SendDocPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SendDocDO> list = sendDocService.getSendDocPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "发文.xls", "数据", SendDocRespVO.class,
                        BeanUtils.toBean(list, SendDocRespVO.class));
    }

}