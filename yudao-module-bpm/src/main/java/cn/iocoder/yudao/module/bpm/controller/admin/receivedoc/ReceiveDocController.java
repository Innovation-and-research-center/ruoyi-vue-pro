package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import io.swagger.v3.oas.annotations.Parameters;
import jodd.util.StringUtil;
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

import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;

@Tag(name = "管理后台 - 收文")
@RestController
@RequestMapping("/bpm/receive-doc")
@Validated
public class ReceiveDocController {

    @Resource
    private ReceiveDocService receiveDocService;

    @PostMapping("/create")
    @Operation(summary = "创建收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Long> createReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        return success(receiveDocService.createReceiveDoc(getLoginUserId(),createReqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Long> saveReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO createReqVO) {
        return success(receiveDocService.saveReceiveDoc(getLoginUserId(),createReqVO));
    }

    @PostMapping("/create-flow")
    @Operation(summary = "创建收文流程")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:create')")
    public CommonResult<Boolean> createFlowReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO createReqVO) {
        if (StrUtil.isNotEmpty(createReqVO.getProcessVariablesStr())) {
            createReqVO.setProcessVariables(JsonUtils.parseObject(createReqVO.getProcessVariablesStr(), Map.class));
        }
        receiveDocService.createFlowReceiveDoc(getLoginUserId(),createReqVO);
        return success(true);
    }


    @PostMapping("/get-number")
    @Operation(summary = "获取收文编号")
    public CommonResult<String> getReceiveDocNumber(@RequestBody  ReceiveDocCreateNumberVO createReqVO) {
        return success(receiveDocService.generateDocumentSequence(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:update')")
    public CommonResult<Boolean> updateReceiveDoc(@Valid @RequestBody ReceiveDocSaveReqVO updateReqVO) {
        receiveDocService.updateReceiveDoc(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除收文")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:delete')")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "作废原因", required = true)
    })
    public CommonResult<Boolean> deleteReceiveDoc(@RequestParam("id") Long id,@RequestParam("reason") String reason) {
        receiveDocService.deleteReceiveDoc(id,reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除收文")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:delete')")
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "作废原因", required = true)
    })
    public CommonResult<Boolean> deleteReceiveDocList(@RequestParam("ids") List<Long> ids,@RequestParam("reason") String reason) {
        receiveDocService.deleteReceiveDocListByIds(ids,reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收文")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:query')")
    public CommonResult<ReceiveDocRespVO> getReceiveDoc(@RequestParam("id") Long id) {
        ReceiveDocDO receiveDoc = receiveDocService.getReceiveDoc(id);
        return success(BeanUtils.toBean(receiveDoc, ReceiveDocRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得收文分页")
//    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:query')")
    public CommonResult<PageResult<ReceiveDocRespVO>> getReceiveDocPage(@Valid ReceiveDocPageReqVO pageReqVO) {
        PageResult<ReceiveDocDO> pageResult = receiveDocService.getReceiveDocPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ReceiveDocRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出收文 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReceiveDocExcel(@Valid ReceiveDocPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ReceiveDocDO> list = receiveDocService.getReceiveDocPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "收文.xls", "数据", ReceiveDocRespVO.class,
                        BeanUtils.toBean(list, ReceiveDocRespVO.class));
    }


    @GetMapping("/receive-doc-attach/list-by-receive-doc-id")
    @Operation(summary = "获得收文附件列表")
    @Parameter(name = "receiveDocId", description = "收文编号(外键T_RECEIVE_DOC.RECEIVE_DOC_ID)")
//    @PreAuthorize("@ss.hasPermission('bpm:receive-doc:query')")
    public CommonResult<List<ReceiveFileRespVO>> getReceiveDocAttachListByReceiveDocId(@RequestParam("receiveDocId") Long receiveDocId) {
        return success(receiveDocService.getReceiveDocAttachListByReceiveDocId(receiveDocId));
    }

}