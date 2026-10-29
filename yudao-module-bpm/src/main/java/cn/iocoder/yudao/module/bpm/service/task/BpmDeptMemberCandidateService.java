package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmRegisterTaskUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.flowable.bpmn.model.BpmnModel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.TASK_APPEND_INVALID;

/** 手动选人的部门范围：当前办理人的主部门和兼任科室。 */
@Service
public class BpmDeptMemberCandidateService {

    public static final String RULE = "dept_members";

    @Resource
    private AdminUserService userService;

    @DataPermission(enable = false)
    public List<AdminUserDO> getCandidates(Long operatorId) {
        AdminUserDO operator = userService.getUser(operatorId);
        if (operator == null) {
            return Collections.emptyList();
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        if (operator.getDeptId() != null) {
            deptIds.add(operator.getDeptId());
        }
        Set<Long> memberDeptIds = userService.getUserDeptIds(operatorId);
        if (memberDeptIds != null) {
            deptIds.addAll(memberDeptIds);
        }
        deptIds.remove(null);
        Map<Long, AdminUserDO> candidates = new LinkedHashMap<>();
        for (Long deptId : deptIds) {
            // 按实际成员关系查询，兼任人员显示在共同科室，而非其账号的其他主部门。
            for (AdminUserDO member : userService.getUserListByDeptIds(Collections.singleton(deptId))) {
                // 无论成员展示在哪个部门，都按用户编号排除当前办理人。
                if (!Objects.equals(operatorId, member.getId())
                        && CommonStatusEnum.ENABLE.getStatus().equals(member.getStatus())) {
                    candidates.putIfAbsent(member.getId(),
                            BeanUtils.toBean(member, AdminUserDO.class).setDeptId(deptId));
                }
            }
        }
        return new ArrayList<>(candidates.values());
    }

    /** 仅校验本次名单；会签中此前其他办理人的合法名单不受本次部门范围限制。 */
    @DataPermission(enable = false)
    public void validateSelections(Long operatorId, BpmnModel model, Map<String, List<Long>> selections) {
        if (selections == null || selections.isEmpty()) {
            return;
        }
        Set<Long> allowed = null;
        for (Map.Entry<String, List<Long>> selection : selections.entrySet()) {
            if (!RULE.equals(BpmRegisterTaskUtils.getProperty(model.getFlowElement(selection.getKey()), "choose_rule"))) {
                continue;
            }
            if (allowed == null) {
                allowed = new HashSet<>();
                for (AdminUserDO user : getCandidates(operatorId)) {
                    allowed.add(user.getId());
                }
            }
            if (selection.getValue() != null && !allowed.containsAll(selection.getValue())) {
                throw exception(TASK_APPEND_INVALID, "部门人员只能选择当前办理人主部门或兼任科室的其他有效人员，不能选择本人");
            }
        }
    }
}
