package cn.iocoder.yudao.module.bpm.job.Dto.receive.Forestry;

import lombok.Data;

@Data
public  class ForestryResult<T> {
    private String message;
    private int status;
    private T data;
}
