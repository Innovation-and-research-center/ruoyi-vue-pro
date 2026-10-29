package cn.iocoder.yudao.module.bpm.controller.admin.land.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 土地用途认定列表项")
@Data
public class LandPageRespVO {
    private Long id;
    private String projectName;
    private String location;
    private String startUserName;
    private String deptName;
    private LocalDateTime applyDate;
    private Integer status;
    private String processInstanceId;
}
