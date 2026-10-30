package cn.iocoder.yudao.module.bpm.controller.admin.land.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 土地用途认定分页查询")
@Data
@EqualsAndHashCode(callSuper = true)
public class LandPageReqVO extends PageParam {
    private String projectName;
    private String location;
    private String startUserName;
    private Integer status;
    private String orderField;
    private String orderDirection;

    @Size(max = 2)
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] applyDate;
}
