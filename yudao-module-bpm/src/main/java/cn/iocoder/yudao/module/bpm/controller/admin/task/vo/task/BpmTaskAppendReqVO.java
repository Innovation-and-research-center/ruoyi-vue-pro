package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class BpmTaskAppendReqVO {
    @NotBlank private String sourceTaskId;
    // 保留单目标字段供旧页面调用；新页面使用 targets，两种格式不能混用。
    private String targetExecutionId;
    @Size(max = 200) private List<@NotNull Long> userIds;
    @Valid @Size(max = 50) private List<@NotNull Target> targets;
    @Size(max = 1000) private String reason;
    private Boolean sendSms;

    @Data
    public static class Target {
        @NotBlank private String targetExecutionId;
        @NotEmpty @Size(max = 200) private List<@NotNull Long> userIds;
    }
}
