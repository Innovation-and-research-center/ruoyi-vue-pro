package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public  class Sjzl {
    private String sjzl; //材料名称
    private String bucketName; //minio存储桶名称
    @JsonProperty("m_object")
    private String mObject;  //minio存储文件对象
    private String clmc; //材料名称
    private String cllx; //材料类型  版式文件签发稿、电子收文件、拟办单、承办单、附件
    private String sqfs; //收取方式 未收取、纸质收取、电子收取、归档后补充
    private String wbssm;//未（补）收说明
    private String wjm;//计算机文件名
    private String cjsj; //计算机文件创建时间
    private String xgsj;//计算机文件修改时间
    private String wjdx;//计算机文件大小
    private String gsxx;//计算机文件格式信息
    private String wjszzy;//文件数字摘要值
    private String qzh; //全宗号
    private String pch;//批次号
    private String jhrq;//交换（移交）日期
    private String bsl;//交换（移交）日期
    private String bz;  //备注
    private String lddwmc; //立档单位名称
    private String zrz; //责任者
    private String tm;//题名
    private String wjbh; //文件编号
    private String rq; //日期
    private String dh; //文件编号
    private String szzy; //数字摘要值
    private String fjpath; //本地附件读取路径
    private String tmid; //条目ID，关联字段
}
