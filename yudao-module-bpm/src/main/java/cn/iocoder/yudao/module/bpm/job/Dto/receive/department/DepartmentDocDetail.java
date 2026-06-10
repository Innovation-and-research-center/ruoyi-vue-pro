package cn.iocoder.yudao.module.bpm.job.Dto.receive.department;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentDocDetail {

    /**
     * 标题
     */
    private String oafrTitle;

    /**
     * 文件字号
     */
    private String oafrFilecode;

    /**
     * 来文单位
     */
    private String oafrFiledep;

    /**
     * 文件编号
     */
    private String oafrFilereceiveno;

    /**
     * 文件类型
     */
    private String oafrFiletype;

    /**
     * 创建时间
     */
    private String oafrCreateTime;

    /**
     * 来文日期
     */
    private String oafrFilereceivedate;

    /**
     * 文件 UUID
     */
    private String oafrUuid;

    /**
     * 文件列表
     */
    private List<DepartmentWkflwFile> wkflwFilesList;

}
