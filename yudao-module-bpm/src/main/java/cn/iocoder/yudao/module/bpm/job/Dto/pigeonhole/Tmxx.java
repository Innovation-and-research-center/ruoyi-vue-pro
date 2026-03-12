package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public  class Tmxx {
    @JsonProperty("公文标识")
    private String gwbz;
    @JsonProperty("文种")
    private String wz;
    @JsonProperty("密级和保密期限")
    private String mj;
    @JsonProperty("紧急程度")
    private String jjcd;
    @JsonProperty("标题")
    private String bt;
    @JsonProperty("主送机关")
    private String zsjg;
    @JsonProperty("成文日期")
    private String cwrq;
    @JsonProperty("档号")
    private String dh;
    @JsonProperty("年度")
    private String nd;
    @JsonProperty("保管期限")
    private String bgqx;
    @JsonProperty("机构或问题")
    private String jghwt;
    @JsonProperty("件号")
    private String jh;
    @JsonProperty("数字对象标识")
    private String szdxbz;
    @JsonProperty("处理类型")
    private String cllx;
    @JsonProperty("附件说明")
    private String fjsm;
    @JsonProperty("附注")
    private String fz;

    private List<Zllb> zllbs;
}
