package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 请假申请历史记录 Response VO")
@Data
public class LeaveHistoryRespVO  extends LeaveDO {
    @Schema(description = "当前审批环节名称")
    private String currentNodeName;

    @Schema(description = "当前办理人昵称")
    private String currentAssigneeNames;
}
