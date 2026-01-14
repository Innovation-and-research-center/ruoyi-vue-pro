package cn.iocoder.yudao.module.bpm.framework.print;

import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.module.bpm.service.timeexplain.TimeExplainService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.OUT;

@Component
public class BpmOutPrintHandler implements BpmProcessPrintDataHandler{

    @Resource
    private TimeExplainService timeExplainService;

    @Override
    public String getProcessDefinitionKey() {
        return OUT;
    }

    @Resource
    private ObjectMapper objectMapper;


    @Override
    public Map<String, Object> getPrintData(String processInstanceId, String businessKey) {
        if (businessKey == null){
            return null;
        }
        TimeExplainDO out = timeExplainService.getTimeExplain(Long.valueOf(businessKey));
        if (out == null){
            return null;
        }
        Map<String, Object> map = objectMapper.convertValue(out, new TypeReference<Map<String, Object>>() {});
        // 2. 组装数据 (使用 LinkedHashMap 保证顺序)
        return  map;
    }
}
