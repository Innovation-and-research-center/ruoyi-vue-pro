package cn.iocoder.yudao.module.bpm.job.Dto.receive.st;

import lombok.Data;

@Data
public  class RecordFileDTO {
    private String fname;
    private String rname;
    private String path;
    private String filesize;
    private String type;
    private String fjid;
}
