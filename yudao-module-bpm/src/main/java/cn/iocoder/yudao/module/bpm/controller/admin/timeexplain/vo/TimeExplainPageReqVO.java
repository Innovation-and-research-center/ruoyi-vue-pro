package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 外出请假补假分页 Request VO")
@Data
public class TimeExplainPageReqVO extends PageParam {

    @Schema(description = "流程内码ID", example = "12282")
    private String actinstId;

    @Schema(description = "人员编号", example = "16761")
    private Long userId;

    @Schema(description = "人员姓名", example = "王五")
    private String userName;

    @Schema(description = "登记时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] checkDate;

    @Schema(description = "开始时间")
    private LocalDateTime checkBegin;

    @Schema(description = "结束时间")
    private LocalDateTime checkEnd;

    @Schema(description = "审核状态（0审批中 1审核完毕 2删除）", example = "2")
    private Long status;

    @Schema(description = "请假天数")
    private BigDecimal days;

    @Schema(description = "年份")
    private Short year;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}