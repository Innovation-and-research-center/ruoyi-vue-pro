package cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class XzssSaveRespVO {
    private Long id;
    private String processInstanceId;
    private String taskId;
}
