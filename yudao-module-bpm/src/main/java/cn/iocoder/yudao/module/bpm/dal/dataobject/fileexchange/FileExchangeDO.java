package cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 文件交换 DO
 *
 * @author 管理员
 */
@TableName("t_file_exchange")
@KeySequence("t_file_exchange_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileExchangeDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * OA内码
     */
    private Long docId;
    /**
     * 唯一编码
     */
    private String docunique;
    /**
     * OA发文字号
     */
    private String sendDocNumber;
    /**
     * OA发文标题
     */
    private String subject;
    /**
     * 操作日期
     */
    private LocalDateTime operationDate;
    /**
     * 操作类型
     */
    private Short operationType;
    /**
     * 操作人
     */
    private String operationPerson;
    /**
     * 操作信息
     */
    private String operationInformation;


}