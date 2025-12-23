package cn.iocoder.yudao.module.bpm.framework.print;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BpmProcessPrintDataFactory {
    private final Map<String, BpmProcessPrintDataHandler> handlerMap = new ConcurrentHashMap<>();

    // Spring 自动注入所有实现了 BpmProcessPrintDataHandler 接口的 Bean
    public BpmProcessPrintDataFactory(List<BpmProcessPrintDataHandler> handlers) {
        for (BpmProcessPrintDataHandler handler : handlers) {
            handlerMap.put(handler.getProcessDefinitionKey(), handler);
        }
    }

    /**
     * 根据流程定义 Key 获取对应的处理器
     */
    public BpmProcessPrintDataHandler getHandler(String processDefinitionKey) {
        return handlerMap.get(processDefinitionKey);
    }
}
