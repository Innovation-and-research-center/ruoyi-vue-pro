package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 准备文件预览；fileId 优先，历史附件使用路径")
@Data
public class FilePreviewReqVO {

    @Positive
    private Long fileId;

    @Positive
    private Long configId;

    @Size(max = 4096)
    private String path;
}
