package cn.iocoder.yudao.module.bpm.controller.admin.timeexplain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimeExplainSaveRespVO {
    private Long id;
    private String processInstanceId;
    private String taskId;
}
