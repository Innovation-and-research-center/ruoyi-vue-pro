package cn.iocoder.yudao.module.system.service.userdept;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.system.controller.admin.userdept.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.userdept.UserDeptDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 用户部门关联 Service 接口
 *
 * @author 管理员
 */
public interface UserDeptService {

    /**
     * 创建用户部门关联
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createUserDept(@Valid UserDeptSaveReqVO createReqVO);

    /**
     * 更新用户部门关联
     *
     * @param updateReqVO 更新信息
     */
    void updateUserDept(@Valid UserDeptSaveReqVO updateReqVO);

    /**
     * 删除用户部门关联
     *
     * @param id 编号
     */
    void deleteUserDept(Long id);

    /**
    * 批量删除用户部门关联
    *
    * @param ids 编号
    */
    void deleteUserDeptListByIds(List<Long> ids);

    /**
     * 获得用户部门关联
     *
     * @param id 编号
     * @return 用户部门关联
     */
    UserDeptDO getUserDept(Long id);

    /**
     * 获得用户部门关联分页
     *
     * @param pageReqVO 分页查询
     * @return 用户部门关联分页
     */
    PageResult<UserDeptDO> getUserDeptPage(UserDeptPageReqVO pageReqVO);

    /**
     * 获得用户拥有的关联部门ID集合
     * @param userId 用户ID
     * @return 部门ID集合
     */
    Set<Long> getUserDeptIds(Long userId);

    /**
     * 赋予用户关联部门
     * @param reqVO 请求对象
     */
    void assignUserDept(UserAssignDeptReqVO reqVO);

    Set<Long> getUserIdsByDeptId(Long deptId);

}