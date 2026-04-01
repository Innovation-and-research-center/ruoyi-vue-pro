package cn.iocoder.yudao.module.bpm.controller.admin.commentattach;

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

import cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import cn.iocoder.yudao.module.bpm.service.commentattach.CommentAttachService;

@Tag(name = "管理后台 - 评论附件")
@RestController
@RequestMapping("/bpm/comment-attach")
@Validated
public class CommentAttachController {

    @Resource
    private CommentAttachService commentAttachService;

    @PostMapping("/create")
    @Operation(summary = "创建评论附件")
    public CommonResult<Long> createCommentAttach(@Valid @RequestBody CommentAttachSaveReqVO createReqVO) {
        return success(commentAttachService.createCommentAttach(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新评论附件")
    public CommonResult<Boolean> updateCommentAttach(@Valid @RequestBody CommentAttachSaveReqVO updateReqVO) {
        commentAttachService.updateCommentAttach(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除评论附件")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteCommentAttach(@RequestParam("id") Long id) {
        commentAttachService.deleteCommentAttach(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除评论附件")
                @PreAuthorize("@ss.hasPermission('bpm:comment-attach:delete')")
    public CommonResult<Boolean> deleteCommentAttachList(@RequestParam("ids") List<Long> ids) {
        commentAttachService.deleteCommentAttachListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得评论附件")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<CommentAttachRespVO> getCommentAttach(@RequestParam("id") Long id) {
        CommentAttachDO commentAttach = commentAttachService.getCommentAttach(id);
        return success(BeanUtils.toBean(commentAttach, CommentAttachRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得评论附件分页")
    @PreAuthorize("@ss.hasPermission('bpm:comment-attach:query')")
    public CommonResult<PageResult<CommentAttachRespVO>> getCommentAttachPage(@Valid CommentAttachPageReqVO pageReqVO) {
        PageResult<CommentAttachDO> pageResult = commentAttachService.getCommentAttachPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CommentAttachRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出评论附件 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:comment-attach:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCommentAttachExcel(@Valid CommentAttachPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CommentAttachDO> list = commentAttachService.getCommentAttachPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "评论附件.xls", "数据", CommentAttachRespVO.class,
                        BeanUtils.toBean(list, CommentAttachRespVO.class));
    }


    @GetMapping("/list-by-doc")
    @Operation(summary = "根据业务主键和类型获得附件列表")
    @Parameters({
            @Parameter(name = "docId", description = "业务编号", required = true, example = "1024"),
            @Parameter(name = "docType", description = "业务类型(如: XZFY, XZSS)", required = true, example = "XZFY")
    })
    public CommonResult<List<CommentAttachDO>> getAttachListByDoc(
            @RequestParam("docId") String docId,
            @RequestParam("docType") String docType) {

        // 动态接收前端传来的 docId 和 docType
        List<CommentAttachDO> attachList = commentAttachService.getCommentAttachList(docId, docType);
        return success(attachList);
    }

}