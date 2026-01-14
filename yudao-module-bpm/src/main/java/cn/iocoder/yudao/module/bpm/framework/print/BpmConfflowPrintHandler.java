package cn.iocoder.yudao.module.bpm.framework.print;

import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.module.bpm.service.confflow.ConfflowService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.CONFLOW_REPORT;

@Component
public class BpmConfflowPrintHandler implements BpmProcessPrintDataHandler{
    @Override
    public String getProcessDefinitionKey() {
        return CONFLOW_REPORT;
    }

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ConfflowService confflowService;

    @Override
    public Map<String, Object> getPrintData(String processInstanceId, String businessKey) {
        if (businessKey == null){
            return null;
        }
        ConfflowDO confflow = confflowService.getConfflow(Long.valueOf(businessKey));
        if (confflow == null){
            return null;
        }
        Map<String, Object> map = objectMapper.convertValue(confflow, new TypeReference<Map<String, Object>>() {});
        // 2. 组装数据 (使用 LinkedHashMap 保证顺序)
        return  map;
    }
}
