package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 假期申请审批分页 Request VO")
@Data
public class LeavePageReqVO extends PageParam {

    @Schema(description = "申请时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] applyDate;

    @Schema(description = "请假时间范围，与该范围有交集的记录")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] leaveTime;

    @Schema(description = "开始时间(请假时间段中最小的时间)")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] qxjStartDate;

    @Schema(description = "结束时间(请假时间段中最大的时间)")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] qxjEndDate;

    @Schema(description = "请(休)假种类", example = "1")
    private Integer qxjType;

    @Schema(description = "请假事由")
    private String sjReason;

    @Schema(description = "共计天数")
    private BigDecimal totalTs;

    @Schema(description = "文件地址")
    private String filepath;

    @Schema(description = "申请用户ID")
    private Long userId;

    @Schema(description = "申请用户昵称")
    private String nickName;

    @Schema(description = "办理状态")
    private Short spzt;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向，asc 或 desc")
    private String orderDirection;

}
