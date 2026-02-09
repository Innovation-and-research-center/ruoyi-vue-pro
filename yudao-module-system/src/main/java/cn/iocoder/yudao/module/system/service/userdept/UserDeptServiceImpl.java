package cn.iocoder.yudao.module.system.service.userdept;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import cn.iocoder.yudao.module.system.controller.admin.userdept.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.userdept.UserDeptDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.system.dal.mysql.userdept.UserDeptMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 用户部门关联 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class UserDeptServiceImpl implements UserDeptService {

    @Resource
    private UserDeptMapper userDeptMapper;

    @Resource
    private AdminUserMapper userMapper;

    @Override
    public Long createUserDept(UserDeptSaveReqVO createReqVO) {
        // 插入
        UserDeptDO userDept = BeanUtils.toBean(createReqVO, UserDeptDO.class);
        userDeptMapper.insert(userDept);

        // 返回
        return userDept.getId();
    }

    @Override
    public void updateUserDept(UserDeptSaveReqVO updateReqVO) {
        // 校验存在
        validateUserDeptExists(updateReqVO.getId());
        // 更新
        UserDeptDO updateObj = BeanUtils.toBean(updateReqVO, UserDeptDO.class);
        userDeptMapper.updateById(updateObj);
    }

    @Override
    public void deleteUserDept(Long id) {
        // 校验存在
        validateUserDeptExists(id);
        // 删除
        userDeptMapper.deleteById(id);
    }

    @Override
        public void deleteUserDeptListByIds(List<Long> ids) {
        // 删除
        userDeptMapper.deleteByIds(ids);
        }


    private void validateUserDeptExists(Long id) {
        if (userDeptMapper.selectById(id) == null) {
            throw exception(USER_DEPT_NOT_EXISTS);
        }
    }

    @Override
    public UserDeptDO getUserDept(Long id) {
        return userDeptMapper.selectById(id);
    }

    @Override
    public PageResult<UserDeptDO> getUserDeptPage(UserDeptPageReqVO pageReqVO) {
        return userDeptMapper.selectPage(pageReqVO);
    }

    @Override
    public Set<Long> getUserDeptIds(Long userId) {
        List<UserDeptDO> list = userDeptMapper.selectListByUserId(userId);
        return convertSet(list, UserDeptDO::getDeptId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUserDept(UserAssignDeptReqVO reqVO) {
        // 1. 校验用户是否存在
        AdminUserDO user = userMapper.selectById(reqVO.getUserId());
        if (user == null) {
            // 抛出“用户不存在”异常
            throw exception(USER_NOT_EXISTS);
        }

        // 2. 删除旧关系
        userDeptMapper.deleteByUserId(reqVO.getUserId());

        // 3. 插入新关系
        if (CollUtil.isNotEmpty(reqVO.getDeptIds())) {
            List<UserDeptDO> batchList = new ArrayList<>(reqVO.getDeptIds().size());
            for (Long deptId : reqVO.getDeptIds()) {
                batchList.add(UserDeptDO.builder()
                        .userId(reqVO.getUserId())
                        .deptId(deptId)
                        .build());
            }
            userDeptMapper.insertBatch(batchList);
        }
    }

    @Override
    public Set<Long> getUserIdsByDeptId(Long deptId) {
        // 1. 查询部门关联列表
        // 注意：这里需要在 UserDeptMapper 中确保有 selectListByDeptId 方法
        List<UserDeptDO> list = userDeptMapper.selectListByDeptId(deptId);

        // 2. 转换为用户 ID 集合
        return convertSet(list, UserDeptDO::getUserId);
    }

}