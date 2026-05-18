package cn.iocoder.yudao.module.bpm.dal.dataobject.confflow;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 会议报告单附件 DO
 */
@TableName("t_confflow_attach")
@KeySequence("seq_confflow_attach_id")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfflowAttachDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long confflowAttachId;

    /**
     * 关联 ConfflowDO 的 id
     */
    private Long commId;

    private String commType;

    private String docGuid;

    private String docType;

    /**
     * 存储路径
     */
    private String filePath;

    /**
     * 附件名称
     */
    private String fileName;

    /**
     * 附件类型
     */
    private String fileExtension;

}
