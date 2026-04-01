package cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 评论附件新增/修改 Request VO")
@Data
public class CommentAttachSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "9246")
    private Long id;

    @Schema(description = "关联任务ID", example = "9524")
    private String taskId;

    @Schema(description = "文件路径")
    private String filepath;

    @Schema(description = "文件名称", example = "芋艿")
    private String filename;

    @Schema(description = "文件扩展名")
    private String fileextension;

    @Schema(description = "文档类型", example = "2")
    private String docType;

    @Schema(description = "文档ID", example = "16965")
    private String docId;

    @Schema(description = "评论类型", example = "2")
    private String commentType;

}