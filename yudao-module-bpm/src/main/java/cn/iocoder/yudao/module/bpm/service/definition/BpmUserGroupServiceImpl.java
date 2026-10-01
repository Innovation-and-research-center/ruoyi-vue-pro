package cn.iocoder.yudao.module.bpm.service.definition;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.definition.BpmUserGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.USER_GROUP_IS_DISABLE;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.USER_GROUP_NOT_EXISTS;

/**
 * 用户组 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class BpmUserGroupServiceImpl implements BpmUserGroupService {

    @Resource
    private BpmUserGroupMapper userGroupMapper;
    @Resource
    private BpmUserGroupMembershipService membershipService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUserGroup(BpmUserGroupSaveReqVO createReqVO) {
        BpmUserGroupDO userGroup = BeanUtils.toBean(createReqVO, BpmUserGroupDO.class);
        userGroup.setUserIds(Collections.emptySet());
        userGroupMapper.insert(userGroup);
        membershipService.replaceManualMembers(userGroup.getId(), createReqVO.getUserIds());
        return userGroup.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserGroup(BpmUserGroupSaveReqVO updateReqVO) {
        // 校验存在
        validateUserGroupExists(updateReqVO.getId());
        // 更新
        BpmUserGroupDO updateObj = BeanUtils.toBean(updateReqVO, BpmUserGroupDO.class);
        updateObj.setUserIds(null);
        userGroupMapper.updateById(updateObj);
        membershipService.replaceManualMembers(updateReqVO.getId(), updateReqVO.getUserIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserGroup(Long id) {
        // 校验存在
        this.validateUserGroupExists(id);
        membershipService.deleteGroup(id);
        // 删除
        userGroupMapper.deleteById(id);
    }

    private void validateUserGroupExists(Long id) {
        if (userGroupMapper.selectById(id) == null) {
            throw exception(USER_GROUP_NOT_EXISTS);
        }
    }

    @Override
    public BpmUserGroupDO getUserGroup(Long id) {
        BpmUserGroupDO group = userGroupMapper.selectById(id);
        if (group != null) {
            group.setUserIds(membershipService.getUserIds(Collections.singleton(id)));
        }
        return group;
    }

    @Override
    public List<BpmUserGroupDO> getUserGroupList(Collection<Long> ids) {
        return fillMembers(userGroupMapper.selectByIds(ids));
    }


    @Override
    public List<BpmUserGroupDO> getUserGroupListByStatus(Integer status) {
        return fillMembers(userGroupMapper.selectListByStatus(status));
    }

    @Override
    public List<BpmUserGroupDO> getUserGroupListByName(String name) {
        return fillMembers(userGroupMapper.selectListByName(name));
    }

    @Override
    public PageResult<BpmUserGroupDO> getUserGroupPage(BpmUserGroupPageReqVO pageReqVO) {
        PageResult<BpmUserGroupDO> page = userGroupMapper.selectPage(pageReqVO);
        fillMembers(page.getList());
        return page;
    }

    private List<BpmUserGroupDO> fillMembers(List<BpmUserGroupDO> groups) {
        if (CollUtil.isEmpty(groups)) {
            return groups;
        }
        Set<Long> ids = convertSet(groups, BpmUserGroupDO::getId);
        Map<Long, Set<Long>> members = membershipService.getUsersByGroups(ids);
        groups.forEach(group -> group.setUserIds(members.getOrDefault(group.getId(), Collections.emptySet())));
        return groups;
    }

    @Override
    public void validUserGroups(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得用户组信息
        List<BpmUserGroupDO> userGroups = userGroupMapper.selectByIds(ids);
        Map<Long, BpmUserGroupDO> userGroupMap = convertMap(userGroups, BpmUserGroupDO::getId);
        // 校验
        ids.forEach(id -> {
            BpmUserGroupDO userGroup = userGroupMap.get(id);
            if (userGroup == null) {
                throw exception(USER_GROUP_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(userGroup.getStatus())) {
                throw exception(USER_GROUP_IS_DISABLE, userGroup.getName());
            }
        });
    }

}
