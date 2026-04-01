package cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 评论附件 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CommentAttachRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "9246")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "关联任务ID", example = "9524")
    @ExcelProperty("关联任务ID")
    private String taskId;

    @Schema(description = "文件路径")
    @ExcelProperty("文件路径")
    private String filepath;

    @Schema(description = "文件名称", example = "芋艿")
    @ExcelProperty("文件名称")
    private String filename;

    @Schema(description = "文件扩展名")
    @ExcelProperty("文件扩展名")
    private String fileextension;

    @Schema(description = "文档类型", example = "2")
    @ExcelProperty("文档类型")
    private String docType;

    @Schema(description = "文档ID", example = "16965")
    @ExcelProperty("文档ID")
    private String docId;

    @Schema(description = "评论类型", example = "2")
    @ExcelProperty("评论类型")
    private String commentType;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}