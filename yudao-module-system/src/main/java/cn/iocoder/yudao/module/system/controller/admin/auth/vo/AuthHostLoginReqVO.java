package cn.iocoder.yudao.module.system.controller.admin.auth.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 宿主无感登录 Request VO")
@Data
public class AuthHostLoginReqVO {
    @Schema(description = "宿主环境提供的Token", requiredMode = Schema.RequiredMode.REQUIRED, example = "abc123xxx")
    @NotEmpty(message = "宿主Token不能为空")
    private String token;
}
