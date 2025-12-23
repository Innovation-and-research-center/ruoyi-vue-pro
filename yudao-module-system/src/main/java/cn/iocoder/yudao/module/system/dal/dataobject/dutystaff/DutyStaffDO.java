package cn.iocoder.yudao.module.system.dal.dataobject.dutystaff;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 值班 DO
 *
 * @author 芋道源码
 */
@TableName("t_duty_staff")
@KeySequence("SEQ_DUTY_STAFF_ID") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DutyStaffDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 值班日期
     */
    private LocalDateTime dutyDate;
    /**
     * 人员类型
     *
     * 枚举 {@link TODO task_type 对应的类}
     */
    private String staffType;
    /**
     * 人员ID
     */
    private Long userId;
    /**
     * 人员姓名
     */
    private String staffName;
    /**
     * 提醒次数
     */
    private Long smsCount;


}