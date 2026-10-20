package cn.iocoder.yudao.module.infra.controller.admin.file.vo.file;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Schema(description = "管理后台 - 文件预览信息，地址相对于 admin-api，不包含原始路径")
@Data
@AllArgsConstructor
public class FilePreviewRespVO {

    private String previewPath;
    private String cacheName;
    private String name;
    private long expiresIn;
}
