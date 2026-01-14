package cn.iocoder.yudao.module.system.controller.admin.holiday.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 节日日导入Response VO")
@Data
@Builder
public class HolidayImportRespVO {

    @Schema(description = "创建成功的值班数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> createHolidayNames;

    @Schema(description = "更新成功的值班数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> updateHolidayNames;

    @Schema(description = "导入失败的值班集合，key 为日期，value 为失败原因", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureHolidayNames;
}
