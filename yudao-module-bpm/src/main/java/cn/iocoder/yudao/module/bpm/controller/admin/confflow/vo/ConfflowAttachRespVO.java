package cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 会议报告单附件 Response VO")
@Data
public class ConfflowAttachRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long confflowAttachId;

    @Schema(description = "关联会议报告单ID", example = "2048")
    private Long commId;

    @Schema(description = "存储路径/文件地址", example = "https://xxx.oss-cn-hangzhou.aliyuncs.com/file.pdf")
    private String filePath;

    @Schema(description = "附件名称", example = "会议文件.jpg")
    private String fileName;

    @Schema(description = "附件类型", example = "jpg")
    private String fileExtension;

    @Schema(description = "文件访问URL")
    private String fileUrl;
}
