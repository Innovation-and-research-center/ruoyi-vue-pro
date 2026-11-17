package cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Schema(description = "复制用户的角色和用户组")
public class BpmUserMembershipCopyReqVO {
    @NotNull @Positive
    private Long sourceUserId;
    @NotEmpty @Size(max = 50)
    private Set<@NotNull @Positive Long> targetUserIds;
    @NotNull
    private Set<@NotNull @Positive Long> roleIds = new LinkedHashSet<>();
    @NotNull
    private Set<@NotNull @Positive Long> groupIds = new LinkedHashSet<>();

    @JsonIgnore
    @AssertTrue(message = "请至少选择一个角色或用户组")
    public boolean isSelectionValid() {
        return roleIds != null && groupIds != null && (!roleIds.isEmpty() || !groupIds.isEmpty());
    }
}
