package cn.iocoder.yudao.module.bpm.dal.dataobject.land;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@TableName("bpm_land")
@KeySequence("bpm_land_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class LandDO extends TenantBaseDO {
    @TableId
    private Long id;
    private String projectName;
    private String location;
    private String deptOpinion;
    private LocalDateTime applyDate;
    private Long userId;
    private Long deptId;
    private String startUnit;
    private String startUserName;
    private String deptName;
    private String processInstanceId;
    private Integer status;
    private String filepath;
}
