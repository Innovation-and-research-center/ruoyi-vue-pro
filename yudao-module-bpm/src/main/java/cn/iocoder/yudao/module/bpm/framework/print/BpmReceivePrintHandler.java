package cn.iocoder.yudao.module.bpm.framework.print;

import cn.hutool.core.bean.BeanUtil;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
@Component
public class BpmReceivePrintHandler implements BpmProcessPrintDataHandler{

    @Resource
    private ReceiveDocService receiveDocService;

    @Override
    public String getProcessDefinitionKey() {
        return "receice_doc_v2";
    }

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getPrintData(String processInstanceId, String businessKey) {
        if (businessKey == null){
            return null;
        }
        ReceiveDocDO receiveDoc = receiveDocService.getReceiveDoc(Long.valueOf(businessKey));
        if (receiveDoc == null){
            return null;
        }
        Map<String, Object> map = objectMapper.convertValue(receiveDoc, new TypeReference<Map<String, Object>>() {});
        // 2. 组装数据 (使用 LinkedHashMap 保证顺序)
        return  map;

    }
}
