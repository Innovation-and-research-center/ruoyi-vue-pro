package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReceiveDocCreateNumberVO {
    @Schema(description = "单位类别")
    private String docClass;

    @Schema(description = "年份")
    private String  year;
}
