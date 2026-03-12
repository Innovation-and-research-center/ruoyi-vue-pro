package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import lombok.Data;

@Data
public  class MinioResponse {
    private int total;
    private int upload;
    private boolean success;
    private String message;
}
