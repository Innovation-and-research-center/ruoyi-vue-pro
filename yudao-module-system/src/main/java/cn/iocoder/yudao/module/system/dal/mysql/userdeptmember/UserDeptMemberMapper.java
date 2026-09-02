package cn.iocoder.yudao.module.system.dal.mysql.userdeptmember;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.userdeptmember.UserDeptMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface UserDeptMemberMapper extends BaseMapperX<UserDeptMemberDO> {

    default List<UserDeptMemberDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<UserDeptMemberDO>()
                .eq(UserDeptMemberDO::getUserId, userId)
                .orderByAsc(UserDeptMemberDO::getDeptId));
    }

    default List<UserDeptMemberDO> selectListByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<UserDeptMemberDO>()
                .in(UserDeptMemberDO::getUserId, userIds));
    }

    default List<UserDeptMemberDO> selectListByDeptIdAndUserIds(Long deptId, Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<UserDeptMemberDO>()
                .eq(UserDeptMemberDO::getDeptId, deptId)
                .in(UserDeptMemberDO::getUserId, userIds)
                .orderByAsc(UserDeptMemberDO::getSort)
                .orderByAsc(UserDeptMemberDO::getUserId));
    }

    default UserDeptMemberDO selectByUserIdAndDeptId(Long userId, Long deptId) {
        return selectOne(new LambdaQueryWrapperX<UserDeptMemberDO>()
                .eq(UserDeptMemberDO::getUserId, userId)
                .eq(UserDeptMemberDO::getDeptId, deptId));
    }

    default long selectMaxSortByDeptId(Long deptId) {
        UserDeptMemberDO member = selectOne(new LambdaQueryWrapperX<UserDeptMemberDO>()
                .eq(UserDeptMemberDO::getDeptId, deptId)
                .orderByDesc(UserDeptMemberDO::getSort)
                .orderByDesc(UserDeptMemberDO::getId)
                .last("LIMIT 1"));
        return member != null && member.getSort() != null ? member.getSort() : 0L;
    }

    default void deleteByUserId(Long userId) {
        delete(UserDeptMemberDO::getUserId, userId);
    }

}
