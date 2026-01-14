package cn.iocoder.yudao.module.system.dal.dataobject.holiday;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 节假日 DO
 *
 * @author 管理员
 */
@TableName("t_holiday")
@KeySequence("t_holiday_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDO extends BaseDO {

    /**
     * 节假日内码
     */
    @TableId
    private Long id;
    /**
     * 设置日期
     */
    private LocalDateTime settingDate;
    /**
     * 是否是工作日
     *
     * 枚举 {@link TODO infra_boolean_string 对应的类}
     */
    private Short isworkday;
    /**
     * 假日描述
     */
    private String holiDesc;


}