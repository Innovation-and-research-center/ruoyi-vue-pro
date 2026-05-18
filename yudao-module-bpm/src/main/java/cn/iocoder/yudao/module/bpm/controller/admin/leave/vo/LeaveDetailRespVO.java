package cn.iocoder.yudao.module.bpm.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;


@Schema(description = "管理后台 - 请假申请详情 Response VO")
@Data
public class LeaveDetailRespVO  extends LeaveRespVO{
    @Schema(description = "附件列表")
    private List<LeaveAttachRespVO> fileList; // 绑定上一步做好的附件VO
}
