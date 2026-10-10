package cn.iocoder.yudao.module.bpm.dal.dataobject.commenttemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;

/** 保留旧系统模板字段；绑定表单独承载新旧系统映射。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_comment_template_binding")
@KeySequence("bpm_comment_template_binding_seq")

public class CommentTemplateBindingDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String processDefinitionKey;
    private String bizdefGuid;
    private String ideaType;
    private String sourceType;
    private String nodeMapping;
    private String fieldMapping;
}
