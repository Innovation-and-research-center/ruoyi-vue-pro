package cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 会议室 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MeetingRoomRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "7949")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "会议室名称", example = "张三")
    @ExcelProperty("会议室名称")
    private String roomName;

    @Schema(description = "会议室面积")
    @ExcelProperty("会议室面积")
    private Integer roomArea;

    @Schema(description = "会议室座位数")
    @ExcelProperty("会议室座位数")
    private Integer seats;

    @Schema(description = "备注", example = "你说的对")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序")
    @ExcelProperty("排序")
    private Integer sequence;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}