package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.framework.common.validation.Mobile;
import cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Schema(description = "管理后台 - 发送手机验证码 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthSmsSendReqVO extends CaptchaVerificationReqVO {

    @Schema(description = "手机号，非登录场景必填", example = "13800138000")
    @Mobile
    private String mobile;

    @Schema(description = "用户账号，登录场景必填", example = "yudao")
    private String username;

    @Schema(description = "短信场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "发送场景不能为空")
    @InEnum(SmsSceneEnum.class)
    private Integer scene;

    @AssertTrue(message = "短信登录必须填写用户账号，其他场景必须填写手机号")
    @JsonIgnore
    public boolean isAccountValid() {
        return Objects.equals(scene, SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene())
                ? StrUtil.isNotBlank(username) : StrUtil.isNotBlank(mobile);
    }

}
