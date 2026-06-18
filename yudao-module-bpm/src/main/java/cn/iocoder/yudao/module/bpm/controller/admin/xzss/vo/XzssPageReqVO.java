package cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 行政诉讼分页 Request VO")
@Data
public class XzssPageReqVO extends PageParam {

    @Schema(description = "来文号")
    private String swWh;

    @Schema(description = "来文机关")
    private String swJg;

    @Schema(description = "收文日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] swRq;

    @Schema(description = "原告")
    private String sqr;

    @Schema(description = "被告")
    private String bsqr;

    @Schema(description = "第三人")
    private String dsr;

    @Schema(description = "土地坐落")
    private String tdZl;

    @Schema(description = "诉讼类型：1一审，2二审，3再审，如果多次，继续记录")
    private Short ssLx;

    @Schema(description = "类别一")
    private String lb1;

    @Schema(description = "类别二")
    private String lb2;

    @Schema(description = "类别三")
    private String lb3;

    @Schema(description = "类别四")
    private String lb4;

    @Schema(description = "类别五")
    private String lb5;

    @Schema(description = "复议请求")
    private String ssNr;

    @Schema(description = "承办人")
    private String cbr;

    @Schema(description = "承办日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] cbRq;

    @Schema(description = "送法院日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] sfyjgRq;

    @Schema(description = "监督监管")
    private Short issupervise;

    @Schema(description = "是否已寄件提醒")
    private Short mailTip;

    @Schema(description = "办理状态")
    private Short status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向，asc 或 desc")
    private String orderDirection;

}
