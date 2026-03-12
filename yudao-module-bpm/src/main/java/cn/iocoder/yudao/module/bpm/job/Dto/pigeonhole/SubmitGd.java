package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import lombok.Data;

import java.util.List;

@Data
public  class SubmitGd {
    private String projectId;
    private String sfwlx;
    private List<LiucxxItem> liucxx;
    private Tmxx tmxx;
    private List<MinioObject> minioObjects;
}