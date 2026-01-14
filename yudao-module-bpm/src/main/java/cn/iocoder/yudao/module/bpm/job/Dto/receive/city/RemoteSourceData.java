package cn.iocoder.yudao.module.bpm.job.Dto.receive.city;

import lombok.Data;

import java.util.List;

@Data
public class RemoteSourceData {
    /**
     * 文件类型
     */
    private String oafdFiletype;

    /**
     * 文件字号
     */
    private String oafdFilecode;

    /**
     * 公开范围
     */
    private String oafdFilepublic;

    /**
     * 缓急程度
     */
    private String oafdFileremergency;

    /**
     * 签发人
     */
    private String oafdSignleaderName;

    /**
     * 文件类目1
     */
    private String oafdDisfiletype;

    /**
     * 文件类目2
     */
    private String oafdStadntype;

    /**
     * 主送
     */
    private String oafdMainsendName;

    /**
     * 抄送
     */
    private String oafdCopysendName;

    /**
     * 发送范围
     */
    private String oafdOutersendscopename;

    /**
     * 备注
     */
    private String oafdRemark;

    /**
     * 文件编号
     */
    private String oafdFileno;

    /**
     * 文件年份
     */
    private String oafdFileyear;

    /**
     * 发件单位
     */
    private String oafdFileoragnise;

    /**
     * 附件列表
     */
    private List<RemoteWkflwFile> wkflwFiles; //附件列表
}
