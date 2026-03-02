package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 收文 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ReceiveDocRespVO {

    @Schema(description = "收文 id", requiredMode = Schema.RequiredMode.REQUIRED, example = "29970")
    @ExcelProperty("收文 id")
    private Long id;

    @Schema(description = "单位类别")
    @ExcelProperty(value = "单位类别", converter = DictConvert.class)
    @DictFormat("receive_class") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String docClass;

    @Schema(description = "来文单位")
    @ExcelProperty(value = "来文单位", converter = DictConvert.class)
    @DictFormat("agency_name") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String sendDept;

    @Schema(description = "来文字号")
    @ExcelProperty("来文字号")
    private String sendDocNumber;

    @Schema(description = "收文编号")
    @ExcelProperty("收文编号")
    private String receiveDocNumber;

    @Schema(description = "收文日期")
    @ExcelProperty("收文日期")
    private LocalDateTime receiveTime;

    @Schema(description = "主题")
    @ExcelProperty("主题")
    private String subject;

    @Schema(description = "紧急程度")
    @ExcelProperty(value = "紧急程度", converter = DictConvert.class)
    @DictFormat("emergency_degree") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String urgencyDegree;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "文件类别")
    @ExcelProperty(value = "文件类别", converter = DictConvert.class)
    @DictFormat("doc_class") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private String docSecondClass;

    @Schema(description = "主办办结时间")
    @ExcelProperty("主办办结时间")
    private LocalDateTime zhubandate;

    @Schema(description = "协办办结时间")
    @ExcelProperty("协办办结时间")
    private LocalDateTime xiebandate;


    /**
     * 附件路径
     */
    @Schema(description = "附件路径")
    private String attachFilePath;
    @Schema(description = "流程示例")
    private String processInstanceId;



}