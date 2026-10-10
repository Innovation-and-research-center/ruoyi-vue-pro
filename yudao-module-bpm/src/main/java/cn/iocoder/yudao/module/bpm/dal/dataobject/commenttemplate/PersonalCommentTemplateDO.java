package cn.iocoder.yudao.module.bpm.dal.dataobject.commenttemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;

/** 保留旧系统模板字段；绑定表单独承载新旧系统映射。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_personal_comment_template")
@KeySequence("bpm_personal_comment_template_seq")
public class PersonalCommentTemplateDO extends TenantBaseDO {
    @TableId
    private Long oaIdeaId;
    private Long userId;
    private String ideaMessage;
    private String ideaType;
    private Integer ideaOrder;
}
