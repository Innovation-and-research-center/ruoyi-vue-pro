package cn.iocoder.yudao.module.bpm.dal.dataobject.processfile;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bpm_process_file")
@KeySequence("bpm_process_file_seq")
public class BpmProcessFileDO extends BaseDO {

    @TableId
    private Long id;

    /** 文件路径（对应文件服务返回的 URL） */
    private String filePath;

    /** 流程实例 ID */
    private String processInstanceId;
}
