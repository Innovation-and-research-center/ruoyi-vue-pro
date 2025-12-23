package cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 会议报告单分页 Request VO")
@Data
public class ConfflowPageReqVO extends PageParam {

    @Schema(description = "申请人ID", example = "10425")
    private Long userId;

    @Schema(description = "申请人", example = "李四")
    private String userName;

    @Schema(description = "申请人部门ID", example = "15842")
    private Long deptId;

    @Schema(description = "申请人部门", example = "李四")
    private String deptName;

    @Schema(description = "申请日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] applyDate;

    @Schema(description = "会议时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startDate;

    @Schema(description = "提议内容")
    private String content;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "会议地点")
    private String venue;

}