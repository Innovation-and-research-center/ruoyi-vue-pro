package cn.iocoder.yudao.module.bpm.dal.dataobject.leave;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 外出请假补假附件 DO
 */
@TableName("t_leave_attact")
@KeySequence("t_leave_attach_seq") // 对应序列
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveAttachDO extends BaseDO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 项目编号 (关联 LeaveDO 的 id)
     */
    private Long leaveId;

    private Long attachFileId;

    /**
     * 存储路径
     */
    @TableField("FILE_PATH")
    private String filePath;

    /**
     * 附件名称
     */
    @TableField("FILE_NAME")
    private String fileName;

    /**
     * 附件类型
     */
    @TableField("FILE_EXTENSION")
    private String fileExtension;


}