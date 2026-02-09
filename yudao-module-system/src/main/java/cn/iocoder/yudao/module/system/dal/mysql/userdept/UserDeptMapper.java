package cn.iocoder.yudao.module.system.dal.mysql.userdept;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.userdept.UserDeptDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.system.controller.admin.userdept.vo.*;

/**
 * 用户部门关联 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface UserDeptMapper extends BaseMapperX<UserDeptDO> {

    default PageResult<UserDeptDO> selectPage(UserDeptPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<UserDeptDO>()
                .eqIfPresent(UserDeptDO::getUserId, reqVO.getUserId())
                .eqIfPresent(UserDeptDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(UserDeptDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(UserDeptDO::getId));
    }

    default List<UserDeptDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<UserDeptDO>()
                .eq(UserDeptDO::getUserId, userId));
    }

    default void deleteByUserId(Long userId) {
        delete(new LambdaQueryWrapperX<UserDeptDO>()
                .eq(UserDeptDO::getUserId, userId));
    }

    default List<UserDeptDO> selectListByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<UserDeptDO>()
                .eq(UserDeptDO::getDeptId, deptId));
    }

}