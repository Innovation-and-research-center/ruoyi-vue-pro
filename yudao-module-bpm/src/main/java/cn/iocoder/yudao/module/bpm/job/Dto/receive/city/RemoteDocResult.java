package cn.iocoder.yudao.module.bpm.job.Dto.receive.city;

import lombok.Data;

import java.util.List;

@Data
public class RemoteDocResult<T> {
    private boolean success;
    private List<String> messages;
    private T data;
}


