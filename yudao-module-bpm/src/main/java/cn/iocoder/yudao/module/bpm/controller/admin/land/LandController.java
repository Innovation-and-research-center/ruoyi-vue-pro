package cn.iocoder.yudao.module.bpm.controller.admin.land;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.*;
import cn.iocoder.yudao.module.bpm.service.land.LandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.NotBlank;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 土地用途认定")
@RestController
@RequestMapping("/bpm/land")
@Validated
public class LandController {
    @Resource
    private LandService landService;

    @PostMapping("/save")
    @Operation(summary = "保存土地用途认定并生成登记待办")
    public CommonResult<LandSaveRespVO> save(@Valid @RequestBody LandSaveReqVO request) {
        return success(landService.save(getLoginUserId(), request));
    }

    @PutMapping("/update")
    @Operation(summary = "修改本人当前登记任务的业务信息")
    public CommonResult<Boolean> update(@Valid @RequestBody LandSaveReqVO request) {
        landService.update(getLoginUserId(), request);
        return success(true);
    }

    @PutMapping("/management-update")
    @Operation(summary = "管理修改土地用途认定")
    @PreAuthorize("@ss.hasPermission('bpm:land:update')")
    public CommonResult<Boolean> managementUpdate(@Valid @RequestBody LandSaveReqVO request) {
        landService.managementUpdate(getLoginUserId(), request);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除土地用途认定（作废并取消运行中流程）")
    @PreAuthorize("@ss.hasPermission('bpm:land:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") @Positive Long id,
                                        @RequestParam("reason") @NotBlank String reason) {
        landService.delete(getLoginUserId(), id, reason);
        return success(true);
    }

    @PostMapping("/create-flow")
    @Operation(summary = "发送土地用途认定登记")
    public CommonResult<Boolean> submit(@Valid @RequestBody LandSaveReqVO request) {
        landService.submit(getLoginUserId(), request);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获取土地用途认定管理分页")
    @PreAuthorize("@ss.hasPermission('bpm:land:query')")
    public CommonResult<PageResult<LandPageRespVO>> getPage(@Valid LandPageReqVO request) {
        return success(landService.getPage(getLoginUserId(), request));
    }

    @GetMapping("/get")
    @Operation(summary = "获取土地用途认定阅办单及附件")
    public CommonResult<LandDetailRespVO> get(@RequestParam("id") @Positive Long id) {
        return success(landService.get(getLoginUserId(), id));
    }
}
