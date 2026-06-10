package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveAttachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 假期申请审批新增/修改 Request VO")
@Data
public class LeaveSaveReqVO {

    @Schema(description = "假期申请审批内码", requiredMode = Schema.RequiredMode.REQUIRED, example = "29970")
    private Long id;

    @Schema(description = "申请时间")
    private LocalDateTime applyDate;

    @Schema(description = "开始时间(请假时间段中最小的时间)" ,requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime qxjStartDate;

    @Schema(description = "开始时段",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时段不能为空")
    private String startPeriod;

    @Schema(description = "结束时间(请假时间段中最大的时间)",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime qxjEndDate;

    @Schema(description = "结束时段",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时段不能为空")
    private String endPeriod;

    @Schema(description = "请(休)假种类", example = "1",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请假类型不能为空")
    private Integer qxjType;

    @Schema(description = "事假理由", example = "不好",requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请假事由不能为空")
    private String sjReason;


    @Schema(description = "共计天数")
    private BigDecimal totalTs;


    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Schema(description = "文件地址")
    private String filepath;

    @Schema(description = "下一节点审批人", example = "assist")
    private Map<String, List<Long>> nextNodeAssignees;

    @Schema(description = "附件列表")
    private List<LeaveAttachDO> fileList;

}
