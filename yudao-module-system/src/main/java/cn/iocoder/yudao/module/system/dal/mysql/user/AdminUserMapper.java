package cn.iocoder.yudao.module.system.dal.mysql.user;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.userdeptmember.UserDeptMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(AdminUserDO::getUsername, username);
    }

    default AdminUserDO selectByEmail(String email) {
        return selectOne(AdminUserDO::getEmail, email);
    }

    default AdminUserDO selectByMobile(String mobile) {
        return selectOne(AdminUserDO::getMobile, mobile);
    }

    default PageResult<AdminUserDO> selectPage(UserPageReqVO reqVO, Long deptId, Collection<Long> userIds) {
        if (deptId == null) {
            MPJLambdaWrapperX<AdminUserDO> wrapper = new MPJLambdaWrapperX<>();
            wrapper.selectAll(AdminUserDO.class)
                    .selectAs(UserDeptMemberDO::getSort, AdminUserDO::getSort);
            wrapper.leftJoin(UserDeptMemberDO.class, (on, ext) -> on
                    .eq(UserDeptMemberDO::getUserId, AdminUserDO::getId)
                    .eq(UserDeptMemberDO::getDeptId, AdminUserDO::getDeptId));
            wrapper.leftJoin(DeptDO.class, DeptDO::getId, AdminUserDO::getDeptId);
            wrapper.likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                    .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                    .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                    .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                    .inIfPresent(AdminUserDO::getId, userIds)
                    .orderByAsc(DeptDO::getSort)
                    .orderByAsc(DeptDO::getId)
                    .orderByAsc(UserDeptMemberDO::getSort)
                    .orderByAsc(AdminUserDO::getId);
            return selectJoinPage(reqVO, AdminUserDO.class, wrapper);
        }
        MPJLambdaWrapperX<AdminUserDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(AdminUserDO.class)
                .selectAs(UserDeptMemberDO::getSort, AdminUserDO::getSort)
                .innerJoin(UserDeptMemberDO.class, UserDeptMemberDO::getUserId, AdminUserDO::getId)
                .eq(UserDeptMemberDO::getDeptId, deptId)
                .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AdminUserDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(AdminUserDO::getId, userIds)
                .orderByAsc(UserDeptMemberDO::getSort)
                .orderByAsc(AdminUserDO::getId);
        return selectJoinPage(reqVO, AdminUserDO.class, wrapper);
    }

    default List<AdminUserDO> selectListByNickname(String nickname) {
        return selectList(new LambdaQueryWrapperX<AdminUserDO>().like(AdminUserDO::getNickname, nickname));
    }

    default List<AdminUserDO> selectListByStatus(Integer status) {
        return selectList(AdminUserDO::getStatus, status);
    }

    default List<AdminUserDO> selectListByStatus() {
        return selectList();
    }

    default List<AdminUserDO> selectListByDeptIds(Collection<Long> deptIds) {
        MPJLambdaWrapperX<AdminUserDO> wrapper = new MPJLambdaWrapperX<>();
        wrapper.selectAll(AdminUserDO.class)
                .selectAs(UserDeptMemberDO::getSort, AdminUserDO::getSort)
                .innerJoin(UserDeptMemberDO.class, UserDeptMemberDO::getUserId, AdminUserDO::getId)
                .in(UserDeptMemberDO::getDeptId, deptIds)
                .orderByAsc(UserDeptMemberDO::getDeptId)
                .orderByAsc(UserDeptMemberDO::getSort)
                .orderByAsc(AdminUserDO::getId);
        return selectJoinList(AdminUserDO.class, wrapper);
    }

}
