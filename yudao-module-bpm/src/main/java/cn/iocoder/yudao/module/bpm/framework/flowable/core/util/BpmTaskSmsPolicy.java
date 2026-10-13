package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;

/**
 * 一次分派的短信选择。待生成成员按目标和人员保存，进入多实例时消费并快照，
 * 避免最后一个审批人覆盖其他人的选择，以及顺序成员继承上一成员的出线选择。
 */
public final class BpmTaskSmsPolicy {

    public static final String TASK_SEND_SMS = "bpm_task_send_sms";
    private static final String MEMBERS = "bpm_task_sms_members";
    private static final String PREFIX = "bpm_task_sms_pending_";

    private BpmTaskSmsPolicy() {
    }

    public static boolean isInternal(String name) {
        return name != null && (name.startsWith(PREFIX) || name.startsWith(MEMBERS) || TASK_SEND_SMS.equals(name));
    }

    private static String key(String node, Object user) {
        return PREFIX + node.length() + "_" + node + "_" + String.valueOf(user);
    }

    public static void prepare(RuntimeService runtimeService, String executionId,
                               Map<String, ? extends Collection<Long>> recipients, Boolean sendSms) {
        if (recipients == null) {
            return;
        }
        recipients.forEach((node, users) -> {
            if (users != null) {
                users.forEach(user -> runtimeService.setVariableLocal(executionId, key(node, user),
                        !Boolean.FALSE.equals(sendSms)));
            }
        });
    }

    public static void prepareDefault(RuntimeService runtimeService, String executionId,
                                      String targetNode, Boolean sendSms) {
        runtimeService.setVariableLocal(executionId, key(targetNode, "*"), !Boolean.FALSE.equals(sendSms));
    }

    /** 在人数和名单已经确定时调用；包括顺序会签还没有创建 Task 的成员。 */
    public static void captureMembers(DelegateExecution execution, Collection<Long> users) {
        Map<String, Boolean> policies = new LinkedHashMap<>();
        String defaultKey = key(execution.getCurrentActivityId(), "*");
        boolean defaultValue = !Boolean.FALSE.equals(execution.getVariable(defaultKey));
        for (Long user : users) {
            String key = key(execution.getCurrentActivityId(), user);
            policies.put(String.valueOf(user), execution.hasVariable(key)
                    ? !Boolean.FALSE.equals(execution.getVariable(key)) : defaultValue);
            execution.removeVariable(key);
        }
        execution.removeVariable(defaultKey);
        execution.setVariableLocal(MEMBERS + "_" + execution.getCurrentActivityId(), policies);
    }

    public static boolean forMember(DelegateExecution execution, Long user) {
        String key = key(execution.getCurrentActivityId(), user);
        // 动态新增成员没有参加初次快照，优先消费它自己的分派选择。
        if (execution.hasVariable(key)) {
            boolean enabled = !Boolean.FALSE.equals(execution.getVariable(key));
            execution.removeVariable(key);
            return enabled;
        }
        Object snapshot = execution.getVariable(MEMBERS + "_" + execution.getCurrentActivityId());
        if (snapshot instanceof Map) {
            return !Boolean.FALSE.equals(((Map<?, ?>) snapshot).get(String.valueOf(user)));
        }
        key = key(execution.getCurrentActivityId(), "*");
        boolean enabled = !Boolean.FALSE.equals(execution.getVariable(key));
        execution.removeVariable(key);
        return enabled;
    }
}
