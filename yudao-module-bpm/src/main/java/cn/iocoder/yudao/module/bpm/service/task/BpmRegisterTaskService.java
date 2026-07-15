package cn.iocoder.yudao.module.bpm.service.task;

import java.util.Map;

/**
 * 登记类任务 Service。
 */
public interface BpmRegisterTaskService {

    /**
     * 将当前流程中的登记任务领取给业务数据保存人。
     *
     * @return 领取的登记任务数量
     */
    int claim(Long userId, String processInstanceId);

    /**
     * 完成当前流程中配置为 complete_on_submit 的登记任务。
     *
     * @return 完成的登记任务数量
     */
    int completeOnSubmit(Long userId, String processInstanceId, Map<String, Object> processVariables);

}
