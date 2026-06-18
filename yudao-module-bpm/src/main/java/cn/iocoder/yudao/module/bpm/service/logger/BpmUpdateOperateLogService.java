package cn.iocoder.yudao.module.bpm.service.logger;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BPM 修改操作日志。
 */
@Service
public class BpmUpdateOperateLogService {

    private static final String TYPE = "BPM流程管理";

    private static final Set<String> IGNORE_FIELDS = new HashSet<>(Arrays.asList(
            "createTime", "updateTime", "creator", "updater", "deleted", "tenantId", "serialVersionUID"));

    @Resource
    private OperateLogCommonApi operateLogApi;

    public void recordUpdate(String businessName, Long bizId, Object oldData, Object newData,
                             Object oldAttachments, Object newAttachments) {
        List<Map<String, Object>> changes = buildChanges(oldData, newData);
        addAttachmentChange(changes, oldAttachments, newAttachments);
        if (CollUtil.isEmpty(changes)) {
            return;
        }

        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        reqDTO.setTraceId(TracerUtils.getTraceId());
        fillUserFields(reqDTO);
        fillRequestFields(reqDTO);

        reqDTO.setType(TYPE);
        reqDTO.setSubType("修改" + businessName);
        reqDTO.setBizId(bizId);
        reqDTO.setAction(buildAction(businessName, bizId, changes));

        Map<String, Object> extra = new HashMap<>();
        extra.put("businessName", businessName);
        extra.put("id", bizId);
        extra.put("changes", changes);
        reqDTO.setExtra(JsonUtils.toJsonString(extra));

        operateLogApi.createOperateLogAsync(reqDTO);
    }

    private List<Map<String, Object>> buildChanges(Object oldData, Object newData) {
        Map<String, Object> oldMap = flatten(oldData);
        Map<String, Object> newMap = flatten(newData);
        Set<String> keys = new HashSet<>();
        keys.addAll(oldMap.keySet());
        keys.addAll(newMap.keySet());
        return keys.stream()
                .sorted()
                .filter(key -> !Objects.equals(oldMap.get(key), newMap.get(key)))
                .map(key -> buildChange(key, oldMap.get(key), newMap.get(key)))
                .collect(Collectors.toList());
    }

    private Map<String, Object> flatten(Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (data == null) {
            return result;
        }
        Map<String, Object> map = JsonUtils.getObjectMapper().convertValue(data, new TypeReference<Map<String, Object>>() {});
        flatten("", map, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            if (IGNORE_FIELDS.contains(key)) {
                return;
            }
            String field = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map) {
                flatten(field, (Map<String, Object>) value, target);
                return;
            }
            target.put(field, normalizeValue(value));
        });
    }

    private Object normalizeValue(Object value) {
        if (value instanceof List) {
            return normalizeAttachments(value);
        }
        return value;
    }

    private void addAttachmentChange(List<Map<String, Object>> changes, Object oldAttachments, Object newAttachments) {
        Object oldValue = normalizeAttachments(oldAttachments);
        Object newValue = normalizeAttachments(newAttachments);
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(buildChange("附件", oldValue, newValue));
        }
    }

    private Object normalizeAttachments(Object attachments) {
        if (attachments == null) {
            return new ArrayList<>();
        }
        if (!(attachments instanceof List)) {
            return attachments;
        }
        List<Object> list = JsonUtils.getObjectMapper().convertValue(attachments, new TypeReference<List<Object>>() {});
        return list.stream()
                .map(this::normalizeAttachmentItem)
                .sorted(Comparator.comparing(JsonUtils::toJsonString))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Object normalizeAttachmentItem(Object item) {
        if (item instanceof Map) {
            return removeIgnoredFields((Map<String, Object>) item);
        }
        if (isSimpleValue(item)) {
            return item;
        }
        Map<String, Object> map = JsonUtils.getObjectMapper().convertValue(item, new TypeReference<Map<String, Object>>() {});
        return removeIgnoredFields(map);
    }

    private boolean isSimpleValue(Object value) {
        return value == null || value instanceof CharSequence || value instanceof Number || value instanceof Boolean;
    }

    private Map<String, Object> removeIgnoredFields(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (!IGNORE_FIELDS.contains(key)) {
                result.put(key, value);
            }
        });
        return result;
    }

    private Map<String, Object> buildChange(String field, Object oldValue, Object newValue) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("field", field);
        change.put("oldValue", oldValue);
        change.put("newValue", newValue);
        return change;
    }

    private String buildAction(String businessName, Long bizId, List<Map<String, Object>> changes) {
        String fields = changes.stream()
                .limit(6)
                .map(change -> String.valueOf(change.get("field")))
                .collect(Collectors.joining("、"));
        if (changes.size() > 6) {
            fields += "等";
        }
        return String.format("修改%s，编号：%s，变更%s项：%s", businessName, bizId, changes.size(), fields);
    }

    private void fillUserFields(OperateLogCreateReqDTO reqDTO) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null) {
            reqDTO.setUserId(loginUser.getId());
            reqDTO.setUserType(loginUser.getUserType());
            return;
        }
        reqDTO.setUserId(0L);
        reqDTO.setUserType(UserTypeEnum.ADMIN.getValue());
    }

    private void fillRequestFields(OperateLogCreateReqDTO reqDTO) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            reqDTO.setRequestMethod("-");
            reqDTO.setRequestUrl("-");
            reqDTO.setUserIp("-");
            reqDTO.setUserAgent("-");
            return;
        }
        reqDTO.setRequestMethod(request.getMethod());
        reqDTO.setRequestUrl(request.getRequestURI());
        reqDTO.setUserIp(ServletUtils.getClientIP(request));
        reqDTO.setUserAgent(ServletUtils.getUserAgent(request));
    }

}
