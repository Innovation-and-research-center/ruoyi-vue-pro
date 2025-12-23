package cn.iocoder.yudao.module.bpm.framework.print;

import java.util.Map;

public interface BpmProcessPrintDataHandler {
    /**
     * 获取支持的流程定义 Key
     * 例如: "oa_leave", "oa_expense"
     *
     * @return 流程定义标识
     */
    String getProcessDefinitionKey();

    /**
     * 获取具体的业务打印数据
     *
     * @param processInstanceId 流程实例ID
     * @param businessKey       业务关联 Key (通常是业务表的主键 ID)
     * @return 业务数据 Map (Key为显示标签, Value为值)
     */
    Map<String, Object> getPrintData(String processInstanceId, String businessKey);
}
