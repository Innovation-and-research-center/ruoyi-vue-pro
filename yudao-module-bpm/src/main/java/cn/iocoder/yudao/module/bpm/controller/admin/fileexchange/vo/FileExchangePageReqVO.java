package cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 文件交换分页 Request VO")
@Data
public class FileExchangePageReqVO extends PageParam {

    @Schema(description = "OA内码", example = "28236")
    private Long docId;

    @Schema(description = "唯一编码")
    private String docunique;

    @Schema(description = "OA发文字号")
    private String sendDocNumber;

    @Schema(description = "OA发文标题")
    private String subject;

    @Schema(description = "操作日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] operationDate;

    @Schema(description = "操作类型", example = "1")
    private Short operationType;

    @Schema(description = "操作人")
    private String operationPerson;

    @Schema(description = "操作信息")
    private String operationInformation;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}