package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@Schema(description = "管理后台 - 申请人请假记录查询")
public class LeaveApplicantHistoryReqVO extends PageParam {
    @Schema(description = "请假类型")
    private Integer qxjType;

    @Schema(description = "请假时间起点，可单独指定")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime leaveTimeStart;

    @Schema(description = "请假时间终点，不传则不限制")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime leaveTimeEnd;

    @Schema(description = "请假时间范围，查询与范围有交集的记录")
    @Size(min = 2, max = 2)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] leaveTime;
}
