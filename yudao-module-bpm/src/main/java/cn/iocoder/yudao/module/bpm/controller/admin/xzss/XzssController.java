package cn.iocoder.yudao.module.bpm.controller.admin.xzss;

import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.XzfyRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.service.xzfy.XzfyService;
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
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

import cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssKzDO;
import cn.iocoder.yudao.module.bpm.service.commentattach.CommentAttachService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.xzss.XzssService;

@Tag(name = "管理后台 - 行政诉讼")
@RestController
@RequestMapping("/bpm/xzss")
@Validated
public class XzssController {

    @Resource
    private XzssService xzssService;

    @Resource
    private BpmDeleteOperateLogService bpmDeleteOperateLogService;

    @Resource
    private BpmUpdateOperateLogService bpmUpdateOperateLogService;

    @Resource
    private CommentAttachService commentAttachService;

    @Resource
    private XzfyService xzfyService;

    @PostMapping("/create")
    @Operation(summary = "创建行政诉讼")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:create')")
    public CommonResult<Long> createXzss(@Valid @RequestBody XzssSaveReqVO createReqVO) {
        return success(xzssService.createXzss(getLoginUserId(),createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新行政诉讼")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:update')")
    public CommonResult<Boolean> updateXzss(@Valid @RequestBody XzssSaveReqVO updateReqVO) {
        XzssDO oldXzss = xzssService.getXzss(updateReqVO.getId());
        Map<String, Object> oldData = buildXzssSnapshot(oldXzss);
        List<?> oldAttachments = oldXzss != null ? commentAttachService.getCommentAttachList(oldXzss.getXmGuid(), "XZSS") : Collections.emptyList();
        xzssService.updateXzss(updateReqVO);
        XzssDO newXzss = xzssService.getXzss(updateReqVO.getId());
        Map<String, Object> newData = buildXzssSnapshot(newXzss);
        List<?> newAttachments = newXzss != null ? commentAttachService.getCommentAttachList(newXzss.getXmGuid(), "XZSS") : Collections.emptyList();
        bpmUpdateOperateLogService.recordUpdate("行政诉讼", updateReqVO.getId(), oldData, newData,
                oldAttachments, newAttachments);
        return success(true);
    }

    private Map<String, Object> buildXzssSnapshot(XzssDO xzss) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("主表", xzss);
        snapshot.put("扩展表", xzss != null ? xzssService.getXzssKzByXmGuid(xzss.getXmGuid()) : null);
        return snapshot;
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除行政诉讼")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:xzss:delete')")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteXzss(@RequestParam("id") Long id,@RequestParam("reason") String reason) {
        xzssService.deleteXzss(id,reason);
        bpmDeleteOperateLogService.recordDelete("行政诉讼", id, reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除行政诉讼")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:delete')")
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteXzssList(@RequestParam("ids") List<Long> ids,@RequestParam("reason") String reason) {
        xzssService.deleteXzssListByIds(ids,reason);
        bpmDeleteOperateLogService.recordDeleteBatch("行政诉讼", ids, reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得行政诉讼")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('bpm:xzss:query')")
    public CommonResult<XzssRespVO> getXzss(@RequestParam("id") Long id) {
        XzssDO xzss = xzssService.getXzss(id);
        if (xzss == null) {
            return success(null);
        }
        XzssRespVO respVO = BeanUtils.toBean(xzss, XzssRespVO.class);
        if (xzss.getFyGuid() != null && !xzss.getFyGuid().isEmpty()) {
            // 需要在 XzfyService 中实现 getXzfyListByXmGuid 方法
            List<XzfyDO> xzfyList = xzfyService.getXzfyListByXmGuid(xzss.getFyGuid());
            respVO.setXzfyList(BeanUtils.toBean(xzfyList, XzfyRespVO.class));
        }

        // 4. 获取历史诉讼列表
        // 逻辑：历史诉讼则是 ss_guid 和 xmid 匹配
        // 理解为：查找其他诉讼记录，其 ssGuid 等于当前的 xmGuid (即查找关联到本案的记录)
        if (xzss.getXmGuid() != null && !xzss.getXmGuid().isEmpty()) {
            // 需要在 XzssService 中实现 getXzssListBySsGuid 方法
            // 这里假设数据库中字段为 ss_guid，对应实体字段为 ssGuid
            List<XzssDO> historyList = xzssService.getXzssListBySsGuid(xzss.getXmGuid());
            respVO.setHistoryXzssList(BeanUtils.toBean(historyList, XzssRespVO.class));
        }

        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得行政诉讼分页")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:query')")
    public CommonResult<PageResult<XzssRespVO>> getXzssPage(@Valid XzssPageReqVO pageReqVO) {
        PageResult<XzssDO> pageResult = xzssService.getXzssPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, XzssRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出行政诉讼 Excel")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportXzssExcel(@Valid XzssPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<XzssDO> list = xzssService.getXzssPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "行政诉讼.xls", "数据", XzssRespVO.class,
                        BeanUtils.toBean(list, XzssRespVO.class));
    }

    // ==================== 子表（行政诉讼拓展） ====================

    @GetMapping("/xzss-kz/get-by-xm-guid")
    @Operation(summary = "获得行政诉讼拓展")
    @Parameter(name = "xmGuid", description = "备用主键")
    public CommonResult<XzssKzDO> getXzssKzByXmGuid(@RequestParam("xmGuid") String xmGuid) {
        return success(xzssService.getXzssKzByXmGuid(xmGuid));
    }

}
