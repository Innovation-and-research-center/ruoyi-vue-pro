package cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 行政复议分页 Request VO")
@Data
public class XzfyPageReqVO extends PageParam {

    @Schema(description = "来文号")
    private String swWh;

    @Schema(description = "来文机关")
    private String swJg;

    @Schema(description = "来文日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] swRq;

    @Schema(description = "申请人")
    private String sqr;

    @Schema(description = "第三人")
    private String dsr;

    @Schema(description = "类别一")
    private String lb1;

    @Schema(description = "类别二")
    private String lb2;

    @Schema(description = "类别三")
    private String lb3;

}