package cn.iocoder.yudao.module.bpm.job.Dto.receive.st;

import lombok.Data;

@Data
public  class RecordDTO {
    private String bt;             // 标题
    private String fwzh;           // 发文字号
    private String jjcd;           // 紧急程度
    private String fwdw;           // 发文单位
    private String fwrq;           // 发文日期
    private String bz;             // 备注
    private String jbr;            // 经办人
    private String lxfs;           // 联系方式
    private String infoexchangeid; // 收文 id
}
