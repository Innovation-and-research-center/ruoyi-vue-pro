package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;
import java.util.List;

@Data
public class BpmTaskAppendTargetRespVO {
    private String executionId;
    private String nodeId;
    private String nodeName;
    private String flowName;
    private Integer flowSort;
    private boolean sequential;
    private boolean newRound;
    private List<Member> candidates;
    @Data
    public static class Member {
        private Long id;
        private String nickname;
        private Long deptId;
        private String deptName;
        private boolean disabled;
        private String disabledReason;
    }
}
