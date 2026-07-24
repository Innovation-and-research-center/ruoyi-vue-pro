package cn.iocoder.yudao.module.bpm.controller.admin.xzss;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
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
import cn.iocoder.yudao.module.bpm.dal.mysql.historyworkflow.HistoryWorkflowMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.commentattach.CommentAttachService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.xzss.XzssService;
import org.flowable.task.api.Task;

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

    @Resource
    private HistoryWorkflowMapper historyWorkflowMapper;

    @Resource
    private org.flowable.engine.TaskService flowableTaskService;

    @PostMapping("/create")
    @Operation(summary = "创建行政诉讼")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:create')")
    public CommonResult<Long> createXzss(@Valid @RequestBody XzssSaveReqVO createReqVO) {
        parseProcessVariables(createReqVO);
        return success(xzssService.createXzss(getLoginUserId(),createReqVO));
    }

    @PostMapping("/save")
    @Operation(summary = "保存行政诉讼并生成登记待办")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:create')")
    public CommonResult<XzssSaveRespVO> saveXzss(@Valid @RequestBody XzssSaveReqVO reqVO) {
        parseProcessVariables(reqVO);
        Long userId = getLoginUserId();
        Long id = xzssService.saveXzss(userId, reqVO);
        XzssDO xzss = xzssService.getXzss(id);
        String processInstanceId = xzss != null ? xzss.getProcessInstanceId() : null;
        Task task = StrUtil.isBlank(processInstanceId) ? null : flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId).taskAssignee(String.valueOf(userId)).active().singleResult();
        return success(new XzssSaveRespVO().setId(id).setProcessInstanceId(processInstanceId)
                .setTaskId(task != null ? task.getId() : null));
    }

    @PostMapping("/create-flow")
    @Operation(summary = "提交已保存的行政诉讼登记")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:create')")
    public CommonResult<Boolean> createFlowXzss(@Valid @RequestBody XzssSaveReqVO reqVO) {
        parseProcessVariables(reqVO);
        xzssService.createFlowXzss(getLoginUserId(), reqVO);
        return success(true);
    }

    private void parseProcessVariables(XzssSaveReqVO reqVO) {
        if (StrUtil.isNotEmpty(reqVO.getProcessVariablesStr())) {
            reqVO.setProcessVariables(JsonUtils.parseObject(reqVO.getProcessVariablesStr(), Map.class));
        }
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
        fillProjectId(respVO);
        if (xzss.getFyGuid() != null && !xzss.getFyGuid().isEmpty()) {
            // 需要在 XzfyService 中实现 getXzfyListByXmGuid 方法
            List<XzfyDO> xzfyList = xzfyService.getXzfyListByXmGuid(xzss.getFyGuid());
            respVO.setXzfyList(BeanUtils.toBean(xzfyList, XzfyRespVO.class));
        }

        // 4. 根据当前记录的 ssGuid 向前追溯上一审、上上一审等历史诉讼。
        if (xzss.getSsGuid() != null && !xzss.getSsGuid().isEmpty()) {
            List<XzssDO> historyList = xzssService.getXzssHistoryList(xzss.getSsGuid());
            respVO.setHistoryXzssList(BeanUtils.toBean(historyList, XzssRespVO.class));
        }

        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得行政诉讼分页")
    @PreAuthorize("@ss.hasPermission('bpm:xzss:query')")
    public CommonResult<PageResult<XzssRespVO>> getXzssPage(@Valid XzssPageReqVO pageReqVO) {
        PageResult<XzssDO> pageResult = xzssService.getXzssPage(pageReqVO);
        PageResult<XzssRespVO> result = BeanUtils.toBean(pageResult, XzssRespVO.class);
        fillProjectIds(result.getList());
        return success(result);
    }

    private void fillProjectIds(List<XzssRespVO> respVOs) {
        List<String> xmGuids = respVOs.stream()
                .map(XzssRespVO::getXmGuid)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
        if (xmGuids.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> infoMap = historyWorkflowMapper
                .selectBizProjectInfoByGuids("xzss", xmGuids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> String.valueOf(item.get("bizinstGuid")), item -> item, (a, b) -> a));
        respVOs.forEach(respVO -> {
            Map<String, Object> info = infoMap.get(respVO.getXmGuid());
            if (info == null) {
                return;
            }
            respVO.setProjectId(String.valueOf(info.get("projectId")));
            if (isFinishedHistoryProcess(info)) {
                respVO.setStatus(BpmTaskStatusEnum.APPROVE.getStatus().shortValue());
            } else {
                respVO.setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue());
            }
        });
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

    private void fillProjectId(XzssRespVO respVO) {
        if (respVO == null || respVO.getXmGuid() == null) {
            return;
        }
        String projectId = historyWorkflowMapper.selectProjectIdByBizinstGuid("xzss", respVO.getXmGuid());
        respVO.setProjectId(projectId);
        if (StrUtil.isNotBlank(projectId)) {
            Map<String, Object> proinst = historyWorkflowMapper.selectProinstByProjectId(projectId);
            if (isFinishedHistoryProcess(proinst)) {
                respVO.setStatus(BpmTaskStatusEnum.APPROVE.getStatus().shortValue());
            } else if (proinst != null) {
                respVO.setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue());
            }
        }
    }

    private boolean isFinishedHistoryProcess(Map<String, Object> proinst) {
        if (proinst == null || proinst.isEmpty()) {
            return false;
        }
        String proinstStatus = String.valueOf(proinst.get("proinstStatus"));
        return "2".equals(proinstStatus) || "8".equals(proinstStatus) || proinst.get("endDate") != null;
    }

}
