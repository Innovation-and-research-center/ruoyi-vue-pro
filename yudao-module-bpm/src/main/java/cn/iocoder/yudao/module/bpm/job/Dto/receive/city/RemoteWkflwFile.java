package cn.iocoder.yudao.module.bpm.job.Dto.receive.city;

import lombok.Data;

@Data
public class RemoteWkflwFile {
    /**
     * 附件名称
     */
    private String wkfileName;

    /**
     * 扩展名
     */
    private String wkfileExtention;

    /**
     * 附件uuid
     */
    private String wkfileUuid;
}
