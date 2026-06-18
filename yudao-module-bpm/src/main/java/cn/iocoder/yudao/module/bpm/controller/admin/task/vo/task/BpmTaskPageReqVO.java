package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.util.BpmQueryUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 流程任务的的分页 Request VO") // 待办、已办，都使用该分页
@Data
public class BpmTaskPageReqVO extends PageParam {

    @Schema(description = "流程任务名", example = "芋道")
    private String name;

    @Schema(description = "流程分类", example = "1")
    private String category;

    @Schema(description = "流程定义的标识", example = "2048")
    private String processDefinitionKey; // 精准匹配

    @Schema(description = "审批状态", example = "1")
    @InEnum(BpmTaskStatusEnum.class)
    private Integer status; // 仅【已办】使用

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "流程实例名称(办件名称)", example = "请假申请-张三")
    private String processInstanceName; // 对应前端 queryParams.processInstanceName

    public List<String> getProcessInstanceNameKeywords() {
        return BpmQueryUtils.splitKeywords(processInstanceName);
    }

    @Schema(description = "流程实例编号(办件编号)", example = "782301")
    private String processInstanceId; // 对应前端 queryParams.processInstanceId

    @Schema(description = "紧急程度", example = "1")
    private Integer urgencyDegree; // 对应前端 queryParams.urgencyDegree (注意类型匹配)

    @Schema(description = "来文单位", example = "研发部")
    private String sendingUnit; // 对应前端 queryParams.sendingUnit (需后端自行实现关联查询)

    @Schema(description = "任务截止时间(环节时限)")
    @DateTimeFormat(pattern = DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] dueDate; // 对应前端 queryParams.dueDate

    @Schema(description = "流程办结时限")
    @DateTimeFormat(pattern = DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] processDeadline; // 对应前端 queryParams.processDeadline (需后端自行实现比对)

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向，asc 或 desc")
    private String orderDirection;

}
