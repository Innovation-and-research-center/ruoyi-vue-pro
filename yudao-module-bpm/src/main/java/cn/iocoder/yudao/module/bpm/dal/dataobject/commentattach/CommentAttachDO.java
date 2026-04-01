package cn.iocoder.yudao.module.bpm.dal.dataobject.commentattach;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 评论附件 DO
 *
 * @author 管理员
 */
@TableName("t_comment_attach")
@KeySequence("t_comment_attach_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAttachDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 关联任务ID
     */
    private String taskId;
    /**
     * 文件路径
     */
    private String filepath;
    /**
     * 文件名称
     */
    private String filename;
    /**
     * 文件扩展名
     */
    private String fileextension;
    /**
     * 文档类型
     */
    private String docType;
    /**
     * 文档ID
     */
    private String docId;
    /**
     * 评论类型
     */
    private String commentType;


}