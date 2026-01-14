package cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 文件交换新增/修改 Request VO")
@Data
public class FileExchangeSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "15310")
    private Long id;

    @Schema(description = "OA内码", example = "28236")
    private Long docId;

    @Schema(description = "唯一编码")
    private String docunique;

    @Schema(description = "OA发文字号")
    private String sendDocNumber;

    @Schema(description = "OA发文标题")
    private String subject;

    @Schema(description = "操作日期")
    private LocalDateTime operationDate;

    @Schema(description = "操作类型", example = "1")
    private Short operationType;

    @Schema(description = "操作人")
    private String operationPerson;

    @Schema(description = "操作信息")
    private String operationInformation;

}