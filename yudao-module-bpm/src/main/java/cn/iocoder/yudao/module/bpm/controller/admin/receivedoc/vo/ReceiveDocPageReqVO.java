package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 收文分页 Request VO")
@Data
public class ReceiveDocPageReqVO extends PageParam {

    @Schema(description = "单位类别")
    private String docClass;

    @Schema(description = "来文单位")
    private String sendDept;

    @Schema(description = "来文字号")
    private String sendDocNumber;

    @Schema(description = "收文编号")
    private String receiveDocNumber;

    @Schema(description = "收文日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] receiveTime;

    @Schema(description = "主题")
    private String subject;

    @Schema(description = "紧急程度")
    private String urgencyDegree;

    @Schema(description = "文件类别")
    private String docSecondClass;

    @Schema(description = "状态", example = "0")
    private Short status;

    @Schema(description = "来源")
    private String source;

}
