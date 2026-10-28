package cn.iocoder.yudao.module.bpm.dal.dataobject.land;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("bpm_land_attach")
@KeySequence("bpm_land_attach_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class LandAttachDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long landId;
    private Long attachFileId;
    private String filePath;
    private String fileName;
    private String fileExtension;
    private Integer sort;
}
