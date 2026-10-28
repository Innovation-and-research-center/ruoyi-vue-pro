package cn.iocoder.yudao.module.bpm.controller.admin.land.vo;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LandSaveReqVO {
    @Positive
    private Long id;
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称不能超过200字")
    private String projectName;
    @NotBlank(message = "坐落不能为空")
    @Size(max = 500, message = "坐落不能超过500字")
    private String location;
    @Size(max = 4000, message = "科室意见不能超过4000字")
    private String deptOpinion;
    private LocalDateTime applyDate;
    @Valid
    @NotNull
    private List<@NotNull LandAttachSaveReqVO> fileList = new ArrayList<>();
    private Map<String, List<@NotNull @Positive Long>> nextNodeAssignees;
    private Boolean sendSms;
    // 保留前端提交契约；登记提交的流程变量由服务端构建，禁止任意覆盖引擎内部变量。
    private String processVariablesStr;
}
