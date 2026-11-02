package cn.iocoder.yudao.module.bpm.service.leave;

import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.LeaveSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.LeaveTypeStatRespVO;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;

@Service
public class LeaveRecuperationQuotaService {
    @Resource
    private LeaveMapper leaveMapper;

    /** 必须与请假保存共用事务，锁保持到写入提交，防止同时提交绕过额度。 */
    @Transactional(propagation = Propagation.MANDATORY)
    @DataPermission(enable = false)
    public void lockAndValidate(Long userId, LeaveSaveReqVO request, Long excludeId, boolean countsTowardsQuota) {
        if (userId == null || leaveMapper.lockApplicantForQuota(userId) == null) {
            throw exception(FORBIDDEN);
        }
        if (!Objects.equals(request.getQxjType(), 10) || !countsTowardsQuota) {
            return;
        }
        BigDecimal days = request.getTotalTs();
        if (days == null || days.signum() <= 0 || days.remainder(new BigDecimal("0.5")).signum() != 0) {
            throw exception(LEAVE_RECUPERATION_DAYS_INVALID);
        }
        LocalDateTime begin = request.getQxjStartDate().withDayOfYear(1).toLocalDate().atStartOfDay();
        LeaveTypeStatRespVO used = leaveMapper.selectRecuperationQuota(userId, begin, begin.plusYears(1), excludeId);
        if (used.getLeaveCount() >= 3) {
            throw exception(LEAVE_RECUPERATION_COUNT_EXCEEDED);
        }
        BigDecimal remaining = BigDecimal.valueOf(5).subtract(used.getTotalDays()).max(BigDecimal.ZERO);
        if (days.compareTo(remaining) > 0) {
            throw exception(LEAVE_RECUPERATION_DAYS_EXCEEDED, remaining.stripTrailingZeros().toPlainString(),
                    days.stripTrailingZeros().toPlainString());
        }
    }
}
