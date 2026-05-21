package cn.iocoder.yudao.module.system.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.system.api.sms.SmsSendApi;
import cn.iocoder.yudao.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dutystaff.DutyStaffMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DutySmsJob implements JobHandler {

    @Resource
    private DutyStaffMapper dutyStaffMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private SmsSendApi smsSendApi;
    @TenantJob
    @Override
    public String execute(String param) throws Exception {

        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过档案归档任务", currentTenantId);
            return "跳过非目标租户";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate targetDate = now.toLocalDate();
        if (now.getHour() >= 12) {
            targetDate = targetDate.plusDays(1); // 明天
        }

        LocalDateTime startTime = targetDate.atStartOfDay();
        LocalDateTime endTime = targetDate.plusDays(1).atStartOfDay();

        List<DutyStaffDO> staffList = dutyStaffMapper.selectList(
                new LambdaQueryWrapper<DutyStaffDO>()
                        .ge(DutyStaffDO::getDutyDate, startTime)
                        .lt(DutyStaffDO::getDutyDate, endTime) // 🚨 关键：使用 lt (less than，严格小于)
                        .eq(DutyStaffDO::getDeleted, 0)
        );

        if (staffList == null || staffList.isEmpty()) {
            return String.format("执行成功: %s 无值班人员，无需发送", targetDate);
        }
        List<DictDataRespDTO> dictDataList = DictFrameworkUtils.getDictDataList("duty_params");
        Map<String, Object> commonDictParams = new HashMap<>();
        if (CollUtil.isNotEmpty(dictDataList)) {
            // 假设字典的 Label 是模板变量名(如 address)，Value 是具体值(如 监控室101)
            dictDataList.forEach(dict -> commonDictParams.put(dict.getLabel(), dict.getValue()));
        }


        int successSmsCount = 0;
        int successDingCount = 0;
        for (DutyStaffDO staff : staffList) {
            if (staff.getUserId() == null) {
                continue;
            }
            AdminUserDO user = adminUserMapper.selectById(staff.getUserId());

            if (user == null) {
                continue;
            }

            // 【修改】拆分手机号与钉钉ID的判断逻辑
            boolean hasMobile = StrUtil.isNotBlank(user.getMobile());
            boolean hasDingId = StrUtil.isNotBlank(user.getDingId());

            if (!hasMobile && !hasDingId) {
                log.warn("值班人员 {} (ID:{}) 未配置手机号和钉钉ID，跳过发送", staff.getStaffName(), staff.getUserId());
                continue;
            }

            // 5. 组装消息模板参数 (钉钉和短信共用此参数)
            Map<String, Object> templateParams = new HashMap<>(commonDictParams);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
            templateParams.put("dutydate", targetDate.format(formatter));

            String templateCode = "duty_" + staff.getStaffType();
            String templateDingCode = "duty_ding_" + staff.getStaffType();
            boolean isSentAny = false; // 标记是否成功发送了任意一种消息

            // ================== 【新增】发送钉钉消息逻辑 ==================
            if (hasDingId) {
                try {
                    // TODO: 请将下方替换为你实际的钉钉发送方法调用
                    // dingTalkSendApi.sendDingMessage(user.getDingId(), templateCode, templateParams);

                    smsSendApi.sendSingleSmsToAdmin(new SmsSendSingleToUserReqDTO()
                            .setUserId(user.getId())
                            .setMobile(user.getDingId())
                            .setTemplateCode(templateDingCode)
                            .setTemplateParams(templateParams));
                    successDingCount++;
                    isSentAny = true;
                } catch (Exception e) {
                    log.error("给值班人员 {} 发送钉钉消息异常", user.getNickname(), e);
                }
            }
            // ================== 发送短信消息逻辑 ==================
            if (hasMobile) {
                try {
                    // 调用系统短信 API 投递
                    smsSendApi.sendSingleSmsToAdmin(new SmsSendSingleToUserReqDTO()
                            .setUserId(user.getId())
                            .setMobile(user.getMobile())
                            .setTemplateCode(templateCode)
                            .setTemplateParams(templateParams));

                    successSmsCount++;
                    isSentAny = true;
                } catch (Exception e) {
                    log.error("给值班人员 {} 发送短信异常", user.getNickname(), e);
                }
            }

            // ================== 6. 累加并更新发送次数 ==================
            // 【修改】只要短信或钉钉其中一项发送成功，就累加次数
            if (isSentAny) {
                Long currentCount = staff.getSmsCount() == null ? 0L : staff.getSmsCount();
                staff.setSmsCount(currentCount + 1);
                dutyStaffMapper.updateById(staff);
                try {
                    // 每次发送成功后，强制休眠 100 毫秒
                    // 如果列表人数特别多或依然出现遗漏，可以适当调整为 200
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("发送消息线程休眠被中断", e);
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                }
            }
        }

        return String.format("执行成功: 目标日期 %s，共找到 %d 人，成功发送短信 %d 条，钉钉消息 %d 条",
                targetDate, staffList.size(), successSmsCount, successDingCount);

    }
}
