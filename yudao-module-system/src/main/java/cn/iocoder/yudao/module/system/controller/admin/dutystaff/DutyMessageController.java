package cn.iocoder.yudao.module.system.controller.admin.dutystaff;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.dict.vo.data.DictDataSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.sms.vo.template.SmsTemplateSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dict.DictDataDO;
import cn.iocoder.yudao.module.system.dal.dataobject.sms.SmsTemplateDO;
import cn.iocoder.yudao.module.system.service.dict.DictDataService;
import cn.iocoder.yudao.module.system.service.sms.SmsTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 值班消息配置")
@RestController
@RequestMapping("/system/duty/message")
public class DutyMessageController {

    private static final List<String> TEMPLATE_CODES = Arrays.asList("duty_1", "duty_2", "duty_ding_1", "duty_ding_2");
    private static final String PARAM_DICT_TYPE = "duty_params";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)}");

    @Resource
    private SmsTemplateService smsTemplateService;
    @Resource
    private DictDataService dictDataService;

    @GetMapping("/templates")
    @Operation(summary = "查询值班消息模板")
    @PreAuthorize("@ss.hasPermission('duty:staff:query')")
    public CommonResult<List<DutyTemplateRespVO>> getTemplates() {
        checkTenant();
        return success(TEMPLATE_CODES.stream().map(code -> {
            SmsTemplateDO template = smsTemplateService.getSmsTemplateByCodeFromCache(code);
            DutyTemplateRespVO result = new DutyTemplateRespVO();
            result.setCode(code);
            if (template != null) {
                result.setConfigured(true);
                result.setName(template.getName());
                result.setChannelCode(template.getChannelCode());
                result.setContent(template.getContent());
                result.setStatus(template.getStatus());
            }
            return result;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/template-content")
    @Operation(summary = "修改值班消息文案")
    @PreAuthorize("@ss.hasPermission('duty:staff:update')")
    public CommonResult<Boolean> updateTemplateContent(@Valid @RequestBody UpdateTemplateContentReqVO reqVO) {
        checkTenant();
        if (!TEMPLATE_CODES.contains(reqVO.getCode())) {
            throw exception0(BAD_REQUEST.getCode(), "只能修改值班消息模板");
        }
        SmsTemplateDO template = smsTemplateService.getSmsTemplateByCodeFromCache(reqVO.getCode());
        if (template == null) {
            throw exception0(BAD_REQUEST.getCode(), "值班消息模板不存在，请联系系统管理员完成首次配置");
        }
        Set<String> allowedParams = dictDataService.getDictDataList(CommonStatusEnum.ENABLE.getStatus(), PARAM_DICT_TYPE)
                .stream().map(DictDataDO::getLabel).collect(Collectors.toSet());
        allowedParams.add("dutydate");
        Matcher matcher = PLACEHOLDER.matcher(reqVO.getContent());
        while (matcher.find()) {
            if (!allowedParams.contains(matcher.group(1))) {
                throw exception0(BAD_REQUEST.getCode(), "未配置的值班消息变量：" + matcher.group(1));
            }
        }
        SmsTemplateSaveReqVO updateReqVO = BeanUtils.toBean(template, SmsTemplateSaveReqVO.class);
        updateReqVO.setContent(reqVO.getContent());
        smsTemplateService.updateSmsTemplate(updateReqVO);
        return success(true);
    }

    @GetMapping("/params")
    @Operation(summary = "查询值班消息变量")
    @PreAuthorize("@ss.hasPermission('duty:staff:query')")
    public CommonResult<List<DutyParamRespVO>> getParams() {
        checkTenant();
        return success(dictDataService.getDictDataList(CommonStatusEnum.ENABLE.getStatus(), PARAM_DICT_TYPE)
                .stream().filter(item -> !"dutydate".equals(item.getLabel())).map(item -> {
                    DutyParamRespVO result = new DutyParamRespVO();
                    result.setId(item.getId());
                    result.setLabel(item.getLabel());
                    result.setValue(item.getValue());
                    return result;
                }).collect(Collectors.toList()));
    }

    @PostMapping("/param")
    @Operation(summary = "新增值班消息变量")
    @PreAuthorize("@ss.hasPermission('duty:staff:update')")
    public CommonResult<Long> createParam(@Valid @RequestBody CreateParamReqVO reqVO) {
        checkTenant();
        if ("dutydate".equals(reqVO.getLabel())) {
            throw exception0(BAD_REQUEST.getCode(), "dutydate 是系统日期变量，不能新增");
        }
        List<DictDataDO> params = dictDataService.getDictDataListByDictType(PARAM_DICT_TYPE);
        if (params.stream().anyMatch(item -> reqVO.getLabel().equals(item.getLabel()))) {
            throw exception0(BAD_REQUEST.getCode(), "值班消息变量名已存在");
        }
        int nextSort = params.stream().map(DictDataDO::getSort).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).max().orElse(0) + 1;
        DictDataSaveReqVO createReqVO = new DictDataSaveReqVO();
        createReqVO.setDictType(PARAM_DICT_TYPE);
        createReqVO.setLabel(reqVO.getLabel());
        createReqVO.setValue(reqVO.getValue());
        createReqVO.setSort(nextSort);
        createReqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        Long id = dictDataService.createDictData(createReqVO);
        DictFrameworkUtils.clearCache();
        return success(id);
    }

    @PutMapping("/param-value")
    @Operation(summary = "修改值班消息变量值")
    @PreAuthorize("@ss.hasPermission('duty:staff:update')")
    public CommonResult<Boolean> updateParamValue(@Valid @RequestBody UpdateParamValueReqVO reqVO) {
        checkTenant();
        DictDataDO dictData = dictDataService.getDictData(reqVO.getId());
        if (dictData == null || !PARAM_DICT_TYPE.equals(dictData.getDictType())
                || !Objects.equals(dictData.getStatus(), CommonStatusEnum.ENABLE.getStatus())
                || "dutydate".equals(dictData.getLabel())) {
            throw exception0(BAD_REQUEST.getCode(), "只能修改已有的值班消息变量");
        }
        DictDataSaveReqVO updateReqVO = BeanUtils.toBean(dictData, DictDataSaveReqVO.class);
        updateReqVO.setValue(reqVO.getValue());
        dictDataService.updateDictData(updateReqVO);
        DictFrameworkUtils.clearCache();
        return success(true);
    }

    private void checkTenant() {
        // 值班发送任务只服务租户 1，模板和字典表本身不按租户隔离。
        if (!Objects.equals(TenantContextHolder.getTenantId(), 1L)) {
            throw exception0(FORBIDDEN.getCode(), "当前租户不能维护值班消息");
        }
    }

    @Data
    public static class DutyTemplateRespVO {
        private String code;
        private boolean configured;
        private String name;
        private String channelCode;
        private String content;
        private Integer status;
    }

    @Data
    public static class DutyParamRespVO {
        private Long id;
        private String label;
        private String value;
    }

    @Data
    public static class UpdateTemplateContentReqVO {
        @NotBlank
        private String code;
        @NotBlank
        private String content;
    }

    @Data
    public static class UpdateParamValueReqVO {
        @NotNull
        private Long id;
        @NotBlank
        @Size(max = 100)
        private String value;
    }

    @Data
    public static class CreateParamReqVO {
        @NotBlank
        @Size(max = 100)
        @javax.validation.constraints.Pattern(regexp = "[A-Za-z][A-Za-z0-9_]*", message = "变量名只能使用英文字母、数字和下划线，且须以字母开头")
        private String label;
        @NotBlank
        @Size(max = 100)
        private String value;
    }
}
