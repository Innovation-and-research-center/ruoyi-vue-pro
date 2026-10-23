package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.Data;
import java.util.List;

/** 本任务实际历史路径上的退回节点和可选办理人。 */
@Data
public class BpmTaskReturnTargetRespVO {
    private String taskDefinitionKey;
    private String name;
    private String returnMode;
    private List<Candidate> assignees;
    @Data
    public static class Candidate {
        private Long id;
        private String taskId;
        private String name;
    }
}
