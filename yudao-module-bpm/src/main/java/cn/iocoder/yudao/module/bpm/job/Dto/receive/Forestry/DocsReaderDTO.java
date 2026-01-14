package cn.iocoder.yudao.module.bpm.job.Dto.receive.Forestry;

import cn.iocoder.yudao.module.bpm.job.ForestryDocJob;
import lombok.Data;

import java.util.List;

@Data
public  class DocsReaderDTO {
    private String id;
    private String dept;
    private String title;
    private String wenH;
    private String shouWRQ;
    private String jinJCD;
    private String fileUrl; // 主文件URL
    private List<IFileDTO> fileUrls; // 附件列表
}