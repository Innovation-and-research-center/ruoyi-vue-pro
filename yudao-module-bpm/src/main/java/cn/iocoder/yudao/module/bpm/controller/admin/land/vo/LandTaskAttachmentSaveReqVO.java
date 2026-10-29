package cn.iocoder.yudao.module.bpm.controller.admin.land.vo;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class LandTaskAttachmentSaveReqVO {
    @NotNull @Positive
    private Long landId;
    @NotBlank
    private String taskId;
    @NotNull @Positive
    private Long fileId;
}
