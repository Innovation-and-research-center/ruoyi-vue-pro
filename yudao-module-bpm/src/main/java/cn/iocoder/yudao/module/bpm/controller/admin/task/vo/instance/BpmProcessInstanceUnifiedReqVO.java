package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.bpm.util.BpmQueryUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@Schema(description = "管理后台 - 流程实例统一查询 (OA风格)")
public class BpmProcessInstanceUnifiedReqVO extends PageParam {
    @Schema(description = "办件名称 (模糊查询)", example = "关于xxx的通知")
    private String name;

    public List<String> getNameKeywords() {
        return BpmQueryUtils.splitKeywords(name);
    }

    @Schema(description = "办件编号 (精准/模糊)", example = "260203-8287-0010")
    private String processInstanceId;

    @Schema(description = "办件类型/流程分类 (模糊)", example = "收文")
    private String category;

    @Schema(description = "流程定义Key (用于筛选办件类型)", example = "oa_incoming_doc")
    private String processDefinitionKey;

    @Schema(description = "来文单位/发起部门名称 (模糊)", example = "财政局")
    private String startDeptName;

    @Schema(description = "发起人姓名 (模糊)", example = "张三")
    private String startUserNickname;

    @Schema(description = "办件状态 (1:进行中, 2:已完成)", example = "1")
    private Integer status;

    @Schema(description = "受理日期/开始时间 (范围)")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    private Integer overdueDays;

    @Schema(description = "业务结果(1-处理中, 2-通过, 3-不通过, 4-撤销)", example = "2")
    private Integer processResult;

    @Schema(description = "数据类型：现有数据、历史已办结、历史未办结", example = "历史未办结")
    private String dataType;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向，asc 或 desc")
    private String orderDirection;
}
