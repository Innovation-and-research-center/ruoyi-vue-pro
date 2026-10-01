package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 本地市局公告测试任务：从 mock/notice_list.json 和 mock/detail/*.json 创建收文。
 * 参数为 repeat 时允许重复导入第一条样例，便于反复验证待认领通知。
 */
@Slf4j
@Component
@Profile({"local", "dev"})
public class CityNoticeJsonJob implements JobHandler {

    @Resource
    private CityNoticeJob cityNoticeJob;

    @TenantJob
    @Override
    public String execute(String param) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (!Long.valueOf(1L).equals(tenantId)) {
            return "跳过非目标租户";
        }

        String noticeUuid = cityNoticeJob.getFirstMockNoticeUuid();
        boolean repeat = StrUtil.equalsIgnoreCase("repeat", StrUtil.trim(param));
        Long receiveDocId = cityNoticeJob.syncSingleMockNotice(noticeUuid, repeat);
        if (receiveDocId == null) {
            return "样例公告已导入，未创建新收文；需要重复测试时将任务参数设为 repeat";
        }
        log.info("本地公告 JSON 测试任务已创建收文，receiveDocId: {}", receiveDocId);
        return "已创建收文，receiveDocId: " + receiveDocId;
    }

}
