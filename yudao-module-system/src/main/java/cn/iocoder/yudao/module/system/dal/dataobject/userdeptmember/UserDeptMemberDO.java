package cn.iocoder.yudao.module.system.dal.dataobject.userdeptmember;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 用户实际所属部门关系。
 *
 * 注意：本表与 system_user_dept（关联分管部门）是两个完全独立的业务概念。
 */
@TableName("system_user_dept_member")
@KeySequence("system_user_dept_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeptMemberDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long deptId;

    /** 用户在该部门中的排序 */
    private Long sort;

}
