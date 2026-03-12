package cn.iocoder.yudao.module.bpm.job.Dto.receive.st;

import lombok.Data;

import java.util.List;

@Data
public  class DQSList {
    private List<RecordDTO> records;
    private int total;
    private int size;
    private int current;
    private boolean searchCount;
    private int pages;
}