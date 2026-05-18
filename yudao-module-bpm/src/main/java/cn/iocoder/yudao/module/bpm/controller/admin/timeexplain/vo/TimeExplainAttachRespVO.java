package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 外出请假补假附件 Response VO")
@Data
public class TimeExplainAttachRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "项目编号(外出请假补假单ID)", example = "2048")
    private Long timeExplainId;

    @Schema(description = "存储路径/文件地址", example = "https://xxx.oss-cn-hangzhou.aliyuncs.com/file.pdf")
    private String filePath;

    @Schema(description = "附件名称", example = "外出证明.jpg")
    private String fileName;

    @Schema(description = "附件类型", example = "jpg")
    private String fileExtension;

    @Schema(description = "文件访问URL (为了兼容前端统一组件，单独提供一个专门的URL字段)")
    private String fileUrl;
}
