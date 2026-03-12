package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import lombok.Data;

@Data
public  class OagdResponse {
    private boolean status;
    private String msg;
    private String id;
    private MinioResponse minioResponse;
}
