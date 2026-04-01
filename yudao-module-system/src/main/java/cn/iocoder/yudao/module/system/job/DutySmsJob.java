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
        LocalDateTime endTime = targetDate.atTime(LocalTime.MAX);

        List<DutyStaffDO> staffList = dutyStaffMapper.selectList(
                new LambdaQueryWrapper<DutyStaffDO>()
                        .ge(DutyStaffDO::getDutyDate, startTime)
                        .le(DutyStaffDO::getDutyDate, endTime)
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


        int successCount = 0;
        for (DutyStaffDO staff : staffList) {
            if (staff.getUserId() == null) {
                continue;
            }
            AdminUserDO user = adminUserMapper.selectById(staff.getUserId());
            // 校验人员及手机号
            if (user == null || StrUtil.isBlank(user.getMobile())) {
                log.warn("值班人员 {} (ID:{}) 未配置手机号，跳过发送", staff.getStaffName(), staff.getUserId());
                continue;
            }
            // 5. 组装短信模板参数
            // 请确保这部分 Key 与你在【消息中心 -> 短信模板】中设置的变量名一致
            Map<String, Object> templateParams = new HashMap<>(commonDictParams);
//            templateParams.put("name", user.getNickname());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
            templateParams.put("dutydate", targetDate.format(formatter));
//            templateParams.put("dutydate", targetDate.toString());
            // 如果你的模板需要显示值班类型（带班领导/值班员），也可以传参
//             templateParams.put("type", staff.getStaffType());

            String templateCode = "duty_"+staff.getStaffType();

            try {
                // 调用系统短信 API 投递
                smsSendApi.sendSingleSmsToAdmin(new SmsSendSingleToUserReqDTO()
                        .setUserId(user.getId())
                        .setMobile(user.getMobile())
                        .setTemplateCode(templateCode)
                        .setTemplateParams(templateParams));

                successCount++;

                // 6. 累加并更新短信发送次数 (利用了你在实体类中的 smsCount 字段)
                Long currentCount = staff.getSmsCount() == null ? 0L : staff.getSmsCount();
                staff.setSmsCount(currentCount + 1);
                dutyStaffMapper.updateById(staff);

            } catch (Exception e) {
                log.error("给值班人员 {} 发送短信异常", user.getNickname(), e);
            }
        }

        return String.format("执行成功: 目标日期 %s，共找到 %d 人，成功发送短信 %d 条", targetDate, staffList.size(), successCount);

    }
}
