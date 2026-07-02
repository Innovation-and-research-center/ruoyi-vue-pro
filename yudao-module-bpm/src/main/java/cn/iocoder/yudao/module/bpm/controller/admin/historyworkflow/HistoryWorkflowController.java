package cn.iocoder.yudao.module.bpm.controller.admin.historyworkflow;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.service.historyworkflow.HistoryWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 历史流程")
@RestController
@RequestMapping("/bpm/history-workflow")
@Validated
public class HistoryWorkflowController {

    @Resource
    private HistoryWorkflowService historyWorkflowService;

    @GetMapping("/detail")
    @Operation(summary = "获得历史流程详情")
    public CommonResult<Map<String, Object>> getHistoryWorkflowDetail(
            @RequestParam(value = "processInstanceId", required = false) String processInstanceId,
            @RequestParam(value = "projectId", required = false) String projectId) {
        return success(historyWorkflowService.getHistoryWorkflowDetail(processInstanceId, projectId));
    }

}
