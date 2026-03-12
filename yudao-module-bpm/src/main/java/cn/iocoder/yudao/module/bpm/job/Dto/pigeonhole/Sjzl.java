package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public  class Sjzl {
    private String sjzl;
    private String bucketName;
    @JsonProperty("m_object")
    private String mObject;
    private String clmc;
    private String cllx;
    private String sqfs;
    private String wjm;
    private String wjdx;
    private String gsxx;
    private String pch;
    private String jhrq;
    private String bsl;
    private String lddwmc;
    private String zrz;
    private String tm;
    private String dh;
    private String fjpath;
    private String tmid;
}
