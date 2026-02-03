package cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 会议室新增/修改 Request VO")
@Data
public class MeetingRoomSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "7949")
    private Long id;

    @Schema(description = "会议室名称", example = "张三")
    private String roomName;

    @Schema(description = "会议室面积")
    private Integer roomArea;

    @Schema(description = "会议室座位数")
    private Integer seats;

    @Schema(description = "备注", example = "你说的对")
    private String remark;

    @Schema(description = "排序")
    private Integer sequence;

}