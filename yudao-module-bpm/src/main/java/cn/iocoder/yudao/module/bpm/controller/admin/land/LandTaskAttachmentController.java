package cn.iocoder.yudao.module.bpm.controller.admin.land;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.LandTaskAttachmentSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach.CommentAttachDO;
import cn.iocoder.yudao.module.bpm.service.land.LandTaskAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 土地用途认定办理附件")
@RestController
@RequestMapping("/bpm/land/task-attachment")
@Validated
public class LandTaskAttachmentController {
    @Resource private LandTaskAttachmentService attachmentService;

    @GetMapping("/list")
    @Operation(summary = "查询办件各任务附件")
    public CommonResult<List<CommentAttachDO>> list(@RequestParam("landId") @Positive Long landId) {
        return success(attachmentService.getList(getLoginUserId(), landId));
    }

    @PostMapping("/create")
    @Operation(summary = "当前办理人上传任务附件")
    public CommonResult<Long> create(@Valid @RequestBody LandTaskAttachmentSaveReqVO request) {
        return success(attachmentService.create(getLoginUserId(), request));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除本人当前任务上传的附件")
    public CommonResult<Boolean> delete(@RequestParam("id") @Positive Long id) {
        attachmentService.delete(getLoginUserId(), id);
        return success(true);
    }
}
