package cn.iocoder.yudao.module.system.dal.dataobject.meetingroom;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 会议室 DO
 *
 * @author 管理员
 */
@TableName("t_meeting_room")
@KeySequence("t_meeting_room_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 会议室名称
     */
    private String roomName;
    /**
     * 会议室面积
     */
    private Integer roomArea;
    /**
     * 会议室座位数
     */
    private Integer seats;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sequence;
    /**
     * 删除日期
     */
    private LocalDateTime deleteTime;


}