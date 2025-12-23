package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;
import lombok.Data;
import java.util.Map;

@Data
public class BpmNextTaskRespVO {
    /** 目标任务定义的 Key (id) */
    private String taskDefKey;

    /** 目标任务名称 */
    private String taskName;

    /** 只有满足该条件表达式时，才会走到该节点 (例如 ${auditStatus == 1}) */
    private String conditionExpression;

    /** 目标节点的拓展属性 (Map形式) */
    private Map<String, String> extensionProperties;
}
