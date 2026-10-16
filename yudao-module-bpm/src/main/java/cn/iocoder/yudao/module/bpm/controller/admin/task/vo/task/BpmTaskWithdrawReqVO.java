package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class BpmTaskWithdrawReqVO {
    @NotBlank private String sourceTaskId;
    @NotEmpty @Size(max = 200) @Valid private List<Member> members;
    @Size(max = 1000) private String reason;
    @Data
    public static class Member {
        @NotBlank private String executionId;
        @NotNull private Long userId;
    }
}
