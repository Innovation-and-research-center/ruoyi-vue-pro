package cn.iocoder.yudao.module.bpm.service.historyworkflow;

import java.util.Map;

public interface HistoryWorkflowService {

    Map<String, Object> getHistoryWorkflowDetail(String processInstanceId, String projectId);

}
