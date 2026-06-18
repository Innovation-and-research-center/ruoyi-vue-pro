package cn.iocoder.yudao.module.bpm.service.logger;

import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BPM 删除操作日志。
 */
@Service
public class BpmDeleteOperateLogService {

    private static final String TYPE = "BPM流程管理";

    @Resource
    private OperateLogCommonApi operateLogApi;

    public void recordDelete(String businessName, Long bizId, String reason) {
        OperateLogCreateReqDTO reqDTO = new OperateLogCreateReqDTO();
        reqDTO.setTraceId(TracerUtils.getTraceId());
        fillUserFields(reqDTO);
        fillRequestFields(reqDTO);

        reqDTO.setType(TYPE);
        reqDTO.setSubType("删除" + businessName);
        reqDTO.setBizId(bizId);
        reqDTO.setAction(String.format("删除%s，编号：%s，删除原因：%s", businessName, bizId, reason));

        Map<String, Object> extra = new HashMap<>();
        extra.put("businessName", businessName);
        extra.put("id", bizId);
        extra.put("reason", reason);
        reqDTO.setExtra(JsonUtils.toJsonString(extra));

        operateLogApi.createOperateLogAsync(reqDTO);
    }

    public void recordDeleteBatch(String businessName, List<Long> bizIds, String reason) {
        if (bizIds == null) {
            return;
        }
        bizIds.forEach(id -> recordDelete(businessName, id, reason));
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
