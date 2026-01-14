package cn.iocoder.yudao.module.bpm.framework.print;

import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.service.leave.LeaveService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.LEAVE;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE;

@Component
public class BpmLeavePrintHandler implements BpmProcessPrintDataHandler{

    @Resource
    private LeaveService leaveService;

    @Override
    public String getProcessDefinitionKey() {
        return LEAVE;
    }

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getPrintData(String processInstanceId, String businessKey) {
        if (businessKey == null){
            return null;
        }
        LeaveDO leave = leaveService.getLeave(Long.valueOf(businessKey));
        if (leave == null){
            return null;
        }
        Map<String, Object> map = objectMapper.convertValue(leave, new TypeReference<Map<String, Object>>() {});
        // 2. 组装数据 (使用 LinkedHashMap 保证顺序)
        return  map;

    }
}
