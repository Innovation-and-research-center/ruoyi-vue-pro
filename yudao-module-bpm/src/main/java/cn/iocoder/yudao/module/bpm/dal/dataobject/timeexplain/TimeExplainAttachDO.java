package cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 外出请假补假附件 DO
 */
@TableName("t_time_explain_attach")
@KeySequence("t_time_explain_attach_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeExplainAttachDO extends BaseDO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 关联 TimeExplainDO 的 id
     */
    private Long timeExplainId;

    private Long attachFileId;

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
