package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理后台 - 角色成员")
public class RoleMemberRespVO {
    private Long id;
    private String username;
    private String nickname;
    private String deptName;
    private Integer status;
}
