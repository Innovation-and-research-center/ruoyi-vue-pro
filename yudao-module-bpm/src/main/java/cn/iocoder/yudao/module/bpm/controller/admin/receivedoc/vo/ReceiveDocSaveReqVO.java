package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 收文新增/修改 Request VO")
@Data
public class ReceiveDocSaveReqVO {

    @Schema(description = "收文 id", requiredMode = Schema.RequiredMode.REQUIRED, example = "29970")
    private Long id;

    @Schema(description = "单位类别")
    private String docClass;

    @Schema(description = "来文单位")
    private String sendDept;

    @Schema(description = "来文字号")
    private String sendDocNumber;

    @Schema(description = "收文编号")
    private String receiveDocNumber;

    @Schema(description = "收文日期")
    private LocalDateTime receiveTime;

    @Schema(description = "主题")
    private String subject;

    @Schema(description = "紧急程度")
    private String urgencyDegree;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "文件类别")
    private Short docSecondClass;

    @Schema(description = "附件路径")
    private String attachFilePath;

    @Schema(description = "主办办结时间")
    private LocalDateTime zhubandate;

    @Schema(description = "协办办结时间")
    private LocalDateTime xiebandate;

    @Schema(description = "发起人自选审批人 Map", example = "{taskKey1: [1, 2]}")
    private Map<String, List<Long>> startUserSelectAssignees;

    @Schema(description = "下一个节点", example = "assist")
    private String selectNode;

    @Schema(description = "下一节点审批人", example = "assist")
    private Map<String, List<Long>> nextNodeAssignees;


}