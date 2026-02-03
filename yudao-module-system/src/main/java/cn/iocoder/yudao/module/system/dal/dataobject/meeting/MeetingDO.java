package cn.iocoder.yudao.module.system.dal.dataobject.meeting;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 会议记录 DO
 *
 * @author 管理员
 */
@TableName("t_meeting")
@KeySequence("t_meeting_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 会议室主键
     */
    private Long meetingRoomId;
    /**
     * 记录时间
     */
    private LocalDateTime recordTime;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 预约人ID
     */
    private Long userId;
    /**
     * 预约人姓名
     */
    private String staffName;
    /**
     * 预约人部门
     */
    private String department;
    /**
     * 预约人电话
     */
    private String telephone;
    /**
     * 参会人数
     */
    private Integer attendNumber;
    /**
     * 会议摘要
     */
    @TableField("abstract")
    private String meetingAbstract;


}