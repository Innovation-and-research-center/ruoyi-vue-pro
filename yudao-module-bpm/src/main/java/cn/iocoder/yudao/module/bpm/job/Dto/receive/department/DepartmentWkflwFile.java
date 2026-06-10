package cn.iocoder.yudao.module.bpm.job.Dto.receive.department;

import lombok.Data;

@Data
public class DepartmentWkflwFile {

    /**
     * M 表示正文，MO 表示盖章文件，2 表示附件，QPD 表示签批文件
     */
    private String wkfileFlowType;

    /**
     * 文件名称
     */
    private String wkfileName;

    /**
     * 文件后缀名
     */
    private String wkfileExtention;

    /**
     * 文件 UUID
     */
    private String wkfileUuid;

}
