package cn.iocoder.yudao.module.bpm.controller.admin.xzfy;

import cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo.XzssRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.service.xzss.XzssService;
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

import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyKzDO;
import cn.iocoder.yudao.module.bpm.service.commentattach.CommentAttachService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.xzfy.XzfyService;

@Tag(name = "管理后台 - 行政复议")
@RestController
@RequestMapping("/bpm/xzfy")
@Validated
public class XzfyController {

    @Resource
    private XzfyService xzfyService;

    @Resource
    private BpmDeleteOperateLogService bpmDeleteOperateLogService;

    @Resource
    private BpmUpdateOperateLogService bpmUpdateOperateLogService;

    @Resource
    private CommentAttachService commentAttachService;

    @Resource
    private XzssService xzssService;

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
        XzfyDO oldXzfy = xzfyService.getXzfy(updateReqVO.getId());
        Map<String, Object> oldData = buildXzfySnapshot(oldXzfy);
        List<?> oldAttachments = oldXzfy != null ? commentAttachService.getCommentAttachList(oldXzfy.getXmGuid(), "XZFY") : Collections.emptyList();
        xzfyService.updateXzfy(updateReqVO);
        XzfyDO newXzfy = xzfyService.getXzfy(updateReqVO.getId());
        Map<String, Object> newData = buildXzfySnapshot(newXzfy);
        List<?> newAttachments = newXzfy != null ? commentAttachService.getCommentAttachList(newXzfy.getXmGuid(), "XZFY") : Collections.emptyList();
        bpmUpdateOperateLogService.recordUpdate("行政复议", updateReqVO.getId(), oldData, newData,
                oldAttachments, newAttachments);
        return success(true);
    }

    private Map<String, Object> buildXzfySnapshot(XzfyDO xzfy) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("主表", xzfy);
        snapshot.put("扩展表", xzfy != null ? xzfyService.getXzfyKzByXmGuid(xzfy.getXmGuid()) : null);
        return snapshot;
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除行政复议")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:delete')")
    @Parameters({
            @Parameter(name = "id", description = "编号", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteXzfy(@RequestParam("id") Long id,@RequestParam("reason") String reason) {
        xzfyService.deleteXzfy(id,reason);
        bpmDeleteOperateLogService.recordDelete("行政复议", id, reason);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除行政复议")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:delete')")
    @Parameters({
            @Parameter(name = "ids", description = "编号列表", required = true),
            @Parameter(name = "reason", description = "删除原因", required = true)
    })
    public CommonResult<Boolean> deleteXzfyList(@RequestParam("ids") List<Long> ids,@RequestParam("reason") String reason) {
        xzfyService.deleteXzfyListByIds(ids,reason);
        bpmDeleteOperateLogService.recordDeleteBatch("行政复议", ids, reason);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得行政复议")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('bpm:xzfy:query')")
    public CommonResult<XzfyRespVO> getXzfy(@RequestParam("id") Long id) {
        XzfyDO xzfy = xzfyService.getXzfy(id);
        if (xzfy == null) {
            return success(null);
        }
        // 2. 转换为 RespVO
        XzfyRespVO respVO = BeanUtils.toBean(xzfy, XzfyRespVO.class);

        // 3. 获取关联的行政诉讼列表
        // 逻辑：通过行政复议的 xmGuid 匹配行政诉讼的 fyGuid
        if (xzfy.getXmGuid() != null && !xzfy.getXmGuid().isEmpty()) {
            // 假设 xzssService 中有名为 getXzssListByFyGuid 的方法
            List<XzssDO> xzssList = xzssService.getXzssListByFyGuid(xzfy.getXmGuid());

            // 将 DO 列表转换为 VO 列表并设置到返回对象中
            respVO.setXzssList(BeanUtils.toBean(xzssList, XzssRespVO.class));
        }

        return success(respVO);
//        return success(BeanUtils.toBean(xzfy, XzfyRespVO.class));
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
    public CommonResult<XzfyKzDO> getXzfyKzByXmGuid(@RequestParam("xmGuid") String xmGuid) {
        return success(xzfyService.getXzfyKzByXmGuid(xmGuid));
    }

    @GetMapping("/page-unlinked")
    @Operation(summary = "获得未关联行政诉讼的行政复议分页")
    @PreAuthorize("@ss.hasPermission('bpm:xzfy:query')")
    public CommonResult<PageResult<XzfyRespVO>> getUnlinkedXzfyPage(@Valid XzfyPageReqVO pageReqVO) {
        // 调用 Service 的分页方法
        PageResult<XzfyDO> pageResult = xzfyService.getUnlinkedXzfyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, XzfyRespVO.class));
    }






}
