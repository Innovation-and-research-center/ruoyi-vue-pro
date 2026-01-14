package cn.iocoder.yudao.module.bpm.job.Dto.receive.notice;

import lombok.Data;

@Data
public  class NoticeResult<T> {
    private boolean success;
    private int totalCount;
    private T data;
    private String message;
}
