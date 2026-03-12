package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import lombok.Data;

import java.util.List;

@Data
public  class MinioRequest {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private List<MinioObject> objects;
}
