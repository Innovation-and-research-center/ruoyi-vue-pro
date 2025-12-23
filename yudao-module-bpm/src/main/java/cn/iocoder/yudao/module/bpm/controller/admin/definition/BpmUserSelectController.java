package cn.iocoder.yudao.module.bpm.controller.admin.definition;


import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.string.StrUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.group.BpmUserGroupRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmUserOptionsReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupService;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import cn.iocoder.yudao.module.system.convert.user.UserConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@RestController
@RequestMapping("/bpm/user-select")
@Validated
public class BpmUserSelectController {
    @Resource
    private BpmUserGroupService userGroupService;

    @Resource
    private AdminUserService userService;

    @GetMapping("/get-user-list")
    @Operation(summary = "获得流程可选用户列表")
    @DataPermission(enable = false) // 关闭数据权限，避免查询不到用户数据。相关案例：https://gitee.com/zhijiantianya/yudao-cloud/issues/ID1UYA
    public CommonResult<List<UserSimpleRespVO>> getUserGroupPage(@Valid BpmUserOptionsReqVO reqVO) {
        if(reqVO.getChooseRule().equals("role")){
            Set<Long> roleIds = StrUtils.splitToLongSet(reqVO.getRuleValue());
            List<AdminUserDO> list = userService.getUserListByRoleIds(roleIds);
            return success(UserConvert.INSTANCE.convertSimpleList(list, null));
        } else if (reqVO.getChooseRule().equals("group")) {
            Set<Long> groupIds = StrUtils.splitToLongSet(reqVO.getRuleValue());
            List<BpmUserGroupDO> groupList = userGroupService.getUserGroupList(groupIds);
            Set<Long> allUserIds = groupList.stream()
                    // 1. 提取每个对象中的 userIds 字段 (Stream<Set<Long>>)
                    .map(BpmUserGroupDO::getUserIds)
                    // 2. 过滤掉为 null 的集合，防止空指针异常
                    .filter(Objects::nonNull)
                    // 3. 将 Stream<Set<Long>> 扁平化为 Stream<Long>
                    .flatMap(Set::stream)
                    // 4. 收集结果，Collectors.toSet() 会自动完成去重
                    .collect(Collectors.toSet());
            List<AdminUserDO> list = userService.getUserList(allUserIds);
            return success(UserConvert.INSTANCE.convertSimpleList(list, null));
        }

        else {
            return success(null);
        }

    }
}
