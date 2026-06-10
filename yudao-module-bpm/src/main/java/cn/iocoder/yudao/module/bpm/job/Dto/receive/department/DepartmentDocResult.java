package cn.iocoder.yudao.module.bpm.job.Dto.receive.department;

import lombok.Data;

@Data
public class DepartmentDocResult<T> {

    private boolean success;
    private String filedErrors;
    private String actionErrors;
    private String messages;
    private Integer totalCount;
    private T data;

}
