package cn.iocoder.yudao.module.bpm.controller.admin.land.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LandAttachSaveReqVO {
    // 文件路径和名称由服务端按文件编号读取，不信任客户端提交的存储元数据。
    @NotNull(message = "附件文件编号不能为空")
    @Positive(message = "附件文件编号无效")
    private Long attachFileId;
}
