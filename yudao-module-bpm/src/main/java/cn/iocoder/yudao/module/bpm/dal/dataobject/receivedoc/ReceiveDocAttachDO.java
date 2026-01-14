package cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 收文附件 DO
 *
 * @author 管理员
 */
@TableName("t_receive_doc_attach")
@KeySequence("t_receive_doc_attach_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiveDocAttachDO extends BaseDO {

    /**
     *  主键
     */
    @TableId
    private Long id;
    /**
     * 收文编号(外键T_RECEIVE_DOC.RECEIVE_DOC_ID)
     */
    private Long receiveDocId;
    /**
     * 附件编号(外键T_ATTACH_FILE.ATTACH_FILE_ID)
     */
    private Long attachFileId;
    /**
     * 附件名称
     */
    private String attachFileName;
    /**
     * 附件顺序
     */
    private Short attachOrder;
    /**
     * 显示类型（0都显示1仅在pc显示2仅在app显示）
     */
    private Short showType;

}