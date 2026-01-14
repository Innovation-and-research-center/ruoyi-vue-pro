package cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 文件交换 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FileExchangeRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15310")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "OA内码", example = "28236")
    @ExcelProperty("OA内码")
    private Long docId;

    @Schema(description = "唯一编码")
    @ExcelProperty("唯一编码")
    private String docunique;

    @Schema(description = "OA发文字号")
    @ExcelProperty("OA发文字号")
    private String sendDocNumber;

    @Schema(description = "OA发文标题")
    @ExcelProperty("OA发文标题")
    private String subject;

    @Schema(description = "操作日期")
    @ExcelProperty("操作日期")
    private LocalDateTime operationDate;

    @Schema(description = "操作类型", example = "1")
    @ExcelProperty("操作类型")
    private Short operationType;

    @Schema(description = "操作人")
    @ExcelProperty("操作人")
    private String operationPerson;

    @Schema(description = "操作信息")
    @ExcelProperty("操作信息")
    private String operationInformation;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}