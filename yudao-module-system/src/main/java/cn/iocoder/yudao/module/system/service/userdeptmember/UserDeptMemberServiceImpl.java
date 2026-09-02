package cn.iocoder.yudao.module.system.service.userdeptmember;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdateDeptSortReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.userdeptmember.UserDeptMemberDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.userdeptmember.UserDeptMemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_DEPT_MEMBER_SORT_DUPLICATE;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_DEPT_MEMBER_SORT_MISMATCH;

@Service
@Validated
public class UserDeptMemberServiceImpl implements UserDeptMemberService {

    @Resource
    private UserDeptMemberMapper memberMapper;
    @Resource
    private DeptMapper deptMapper;

    @Override
    public Set<Long> getDeptIdsByUserId(Long userId) {
        return memberMapper.selectListByUserId(userId).stream()
                .map(UserDeptMemberDO::getDeptId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserDeptMembers(Long userId, Long primaryDeptId, Collection<Long> deptIds) {
        Set<Long> targetDeptIds = deptIds != null ? new LinkedHashSet<>(deptIds) : new LinkedHashSet<>();
        if (primaryDeptId != null) {
            targetDeptIds.add(primaryDeptId);
        }
        List<UserDeptMemberDO> oldMembers = memberMapper.selectListByUserId(userId);
        Map<Long, UserDeptMemberDO> oldMemberMap = oldMembers.stream().collect(Collectors.toMap(
                UserDeptMemberDO::getDeptId, Function.identity(), (first, ignored) -> first));
        List<Long> deleteIds = oldMembers.stream()
                .filter(member -> !targetDeptIds.contains(member.getDeptId()))
                .map(UserDeptMemberDO::getId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(deleteIds)) {
            memberMapper.deleteByIds(deleteIds);
        }
        for (Long deptId : targetDeptIds) {
            if (!oldMemberMap.containsKey(deptId)) {
                memberMapper.insert(UserDeptMemberDO.builder()
                        .userId(userId).deptId(deptId)
                        .sort(memberMapper.selectMaxSortByDeptId(deptId) + 1)
                        .build());
            }
        }
    }

    @Override
    public void ensurePrimaryDeptMember(Long userId, Long primaryDeptId) {
        if (primaryDeptId == null || memberMapper.selectByUserIdAndDeptId(userId, primaryDeptId) != null) {
            return;
        }
        memberMapper.insert(UserDeptMemberDO.builder()
                .userId(userId).deptId(primaryDeptId)
                .sort(memberMapper.selectMaxSortByDeptId(primaryDeptId) + 1)
                .build());
    }

    @Override
    public void deleteByUserId(Long userId) {
        memberMapper.deleteByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeptUserSort(UserUpdateDeptSortReqVO reqVO) {
        if (new HashSet<>(reqVO.getUserIds()).size() != reqVO.getUserIds().size()) {
            throw exception(USER_DEPT_MEMBER_SORT_DUPLICATE);
        }
        List<UserDeptMemberDO> members = memberMapper.selectListByDeptIdAndUserIds(
                reqVO.getDeptId(), reqVO.getUserIds());
        if (members.size() != reqVO.getUserIds().size()) {
            throw exception(USER_DEPT_MEMBER_SORT_MISMATCH);
        }
        Map<Long, UserDeptMemberDO> memberMap = members.stream().collect(Collectors.toMap(
                UserDeptMemberDO::getUserId, Function.identity()));
        List<Long> sortSlots = members.stream().map(UserDeptMemberDO::getSort).sorted()
                .collect(Collectors.toList());
        List<UserDeptMemberDO> updates = new ArrayList<>(reqVO.getUserIds().size());
        for (int i = 0; i < reqVO.getUserIds().size(); i++) {
            UserDeptMemberDO member = memberMap.get(reqVO.getUserIds().get(i));
            updates.add(new UserDeptMemberDO().setId(member.getId()).setSort(sortSlots.get(i)));
        }
        memberMapper.updateBatch(updates);
    }

    @Override
    public void fillPrimaryDeptSort(List<AdminUserDO> users) {
        if (CollUtil.isEmpty(users)) {
            return;
        }
        Set<Long> userIds = users.stream().map(AdminUserDO::getId).collect(Collectors.toSet());
        Map<String, Long> memberSortMap = memberMapper.selectListByUserIds(userIds).stream()
                .collect(Collectors.toMap(member -> key(member.getUserId(), member.getDeptId()),
                        UserDeptMemberDO::getSort, (first, ignored) -> first));
        Set<Long> deptIds = users.stream().map(AdminUserDO::getDeptId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DeptDO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap()
                : deptMapper.selectByIds(deptIds).stream().collect(Collectors.toMap(DeptDO::getId, Function.identity()));
        users.forEach(user -> user.setSort(
                memberSortMap.get(key(user.getId(), user.getDeptId()))));
        users.sort(Comparator
                .comparing((AdminUserDO user) -> {
                    DeptDO dept = user.getDeptId() != null ? deptMap.get(user.getDeptId()) : null;
                    return dept != null && dept.getSort() != null ? dept.getSort() : Integer.MAX_VALUE;
                })
                .thenComparing(user -> user.getDeptId() != null ? user.getDeptId() : Long.MAX_VALUE)
                .thenComparing(user -> user.getSort() != null ? user.getSort() : Long.MAX_VALUE)
                .thenComparing(AdminUserDO::getId));
    }

    private static String key(Long userId, Long deptId) {
        return userId + ":" + deptId;
    }

}
