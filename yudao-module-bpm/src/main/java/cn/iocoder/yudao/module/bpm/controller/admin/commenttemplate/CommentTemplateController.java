package cn.iocoder.yudao.module.bpm.controller.admin.commenttemplate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.bpm.controller.admin.commenttemplate.vo.CommentTemplateSaveVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commenttemplate.*;
import cn.iocoder.yudao.module.bpm.service.commenttemplate.CommentTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 意见模板")
@RestController
@RequestMapping("/bpm/comment-template")
@Validated
public class CommentTemplateController {
    @Resource private CommentTemplateService service;

    @GetMapping("/personal/list")
    public CommonResult<List<PersonalCommentTemplateDO>> personal(@RequestParam(required = false) String ideaType) {
        return success(service.personalList(getLoginUserId(), ideaType));
    }
    @PostMapping("/personal/save")
    public CommonResult<Long> savePersonal(@Valid @RequestBody CommentTemplateSaveVO.Personal vo) {
        return success(service.savePersonal(getLoginUserId(), vo));
    }
    @DeleteMapping("/personal/delete")
    public CommonResult<Boolean> deletePersonal(@RequestParam Long id) {
        service.deletePersonal(getLoginUserId(), id);
        return success(true);
    }
    @PutMapping("/personal/order")
    public CommonResult<Boolean> order(@RequestBody @NotEmpty @Size(max = 500) List<@NotNull Long> ids) {
        service.orderPersonal(getLoginUserId(), ids);
        return success(true);
    }
    @GetMapping("/options")
    public CommonResult<CommentTemplateService.Options> options(@RequestParam @NotBlank String taskId,
            @RequestParam(defaultValue = "Default") @NotBlank @Size(max = 50) String code) {
        return success(service.options(getLoginUserId(), taskId, code));
    }
    @GetMapping("/render")
    public CommonResult<String> render(@RequestParam @NotBlank String taskId, @RequestParam @NotBlank String id,
            @RequestParam(defaultValue = "Default") @NotBlank @Size(max = 50) String code) {
        return success(service.render(getLoginUserId(), taskId, id, code));
    }
    @GetMapping("/business/list")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<List<BizCommentTemplateDO>> business(@RequestParam @NotBlank String bizdefGuid) {
        return success(service.businessList(bizdefGuid));
    }
    @PostMapping("/business/save")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<String> saveBusiness(@Valid @RequestBody CommentTemplateSaveVO.Business vo) {
        return success(service.saveBusiness(vo));
    }
    @DeleteMapping("/business/delete")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<Boolean> deleteBusiness(@RequestParam String id) {
        service.deleteBusiness(id);
        return success(true);
    }
    @GetMapping("/binding/list")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<List<CommentTemplateBindingDO>> bindings() {
        return success(service.bindings());
    }
    @PostMapping("/binding/save")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<Boolean> saveBinding(@Valid @RequestBody CommentTemplateSaveVO.Binding vo) {
        service.saveBinding(vo);
        return success(true);
    }
    @DeleteMapping("/binding/delete")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<Boolean> deleteBinding(@RequestParam String processDefinitionKey) {
        service.deleteBinding(processDefinitionKey);
        return success(true);
    }
    @GetMapping("/fields")
    @PreAuthorize("@ss.hasPermission('bpm:comment-template:manage')")
    public CommonResult<Set<String>> fields(@RequestParam String sourceType) {
        return success(service.fields(sourceType));
    }
}
