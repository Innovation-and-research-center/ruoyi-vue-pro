package cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo;

import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowAttachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 会议报告单新增/修改 Request VO")
@Data
public class ConfflowSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "8620")
    private Long id;

    @Schema(description = "申请人ID", example = "10425")
    private Long userId;

    @Schema(description = "申请人", example = "李四")
    private String userName;

    @Schema(description = "申请人部门ID", example = "15842")
    private Long deptId;

    @Schema(description = "申请人部门", example = "李四")
    private String deptName;

    @Schema(description = "申请日期")
    private LocalDateTime applyDate;

    @Schema(description = "会议时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "会议时间不能为空")
    private LocalDateTime startDate;

    @Schema(description = "会议名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "会议名称不能为空")
    private String title;

    @Schema(description = "提议内容")
    private String content;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "会议地点", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "会议地点不能为空")
    private String venue;

    @Schema(description = "召集单位及召集人")
    private String joinUnit;

    @Schema(description = "我局参会科室")
    private String offerUnit;

    @Schema(description = "我局参会人员")
    private String offerPerson;

    @Schema(description = "参会人员会议情况及建议")
    private String situation;

    @Schema(description = "附件路径")
    private String attachFilePath;

    @Schema(description = "附件列表")
    private List<ConfflowAttachDO> fileList;

    @Schema(description = "下一个节点", example = "assist")
    private String selectNode;

    @Schema(description = "下一节点审批人", example = "assist")
    private Map<String, List<Long>> nextNodeAssignees;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Schema(description = "流程变量")
    private String processVariablesStr;

    @Schema(description = "流程变量")
    private Map<String, Object> processVariables;
}