package cn.iocoder.yudao.module.bpm.dal.dataobject.task;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("bpm_process_view_scope")
@Data
@EqualsAndHashCode(callSuper = true)
public class BpmProcessViewScopeDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long roleId;
    private String processDefinitionKey;
}
