package cn.iocoder.yudao.module.bpm.controller.admin.commentattach.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 评论附件分页 Request VO")
@Data
public class CommentAttachPageReqVO extends PageParam {

    @Schema(description = "关联任务ID", example = "9524")
    private String taskId;

    @Schema(description = "文件路径")
    private String filepath;

    @Schema(description = "文件名称", example = "芋艿")
    private String filename;

    @Schema(description = "文件扩展名")
    private String fileextension;

    @Schema(description = "文档类型", example = "2")
    private String docType;

    @Schema(description = "文档ID", example = "16965")
    private String docId;

    @Schema(description = "评论类型", example = "2")
    private String commentType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}