package cn.iocoder.yudao.module.bpm.job.Dto.receive.st;

import lombok.Data;

@Data
public  class STResult<T> {
    private int code;
    private String msg;
    private T data;
}