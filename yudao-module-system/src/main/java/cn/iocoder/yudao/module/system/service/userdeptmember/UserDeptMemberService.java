package cn.iocoder.yudao.module.system.service.userdeptmember;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserUpdateDeptSortReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserDeptMemberService {

    Set<Long> getDeptIdsByUserId(Long userId);

    void saveUserDeptMembers(Long userId, Long primaryDeptId, Collection<Long> deptIds);

    void ensurePrimaryDeptMember(Long userId, Long primaryDeptId);

    void deleteByUserId(Long userId);

    void updateDeptUserSort(UserUpdateDeptSortReqVO reqVO);

    void fillPrimaryDeptSort(List<AdminUserDO> users);

}
