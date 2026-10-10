package cn.iocoder.yudao.module.bpm.dal.dataobject.commenttemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;

/** 保留旧系统模板字段；绑定表单独承载新旧系统映射。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bpm_biz_comment_template")

public class BizCommentTemplateDO extends TenantBaseDO {
    @TableId(type = IdType.INPUT)
    private String commentGuid;
    private String bizdefGuid;
    private String commentName;
    private String commentCode;
    private String commentContent;
    private String meetFunction;
    private String commentAct;
    private Integer seqOrder;
}
