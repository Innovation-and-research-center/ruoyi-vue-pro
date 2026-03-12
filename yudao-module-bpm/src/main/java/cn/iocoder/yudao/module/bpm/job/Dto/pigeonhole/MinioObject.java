package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import lombok.Data;

@Data
public  class MinioObject {
    private String buckName;
    private String objectName;
    private String physicalPath;
}
