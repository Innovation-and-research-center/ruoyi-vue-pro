package cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 值班分页 Request VO")
@Data
public class DutyStaffPageReqVO extends PageParam {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "值班日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] dutyDate;

    @Schema(description = "人员类型", example = "2")
    private String staffType;

    @Schema(description = "人员ID", example = "14288")
    private Long userId;

    @Schema(description = "人员姓名(模糊匹配）", example = "王五")
    private String staffName;

    @Schema(description = "提醒次数", example = "19333")
    private Long smsCount;

    @Schema(description = "部门ID")
    private String deptId;

}