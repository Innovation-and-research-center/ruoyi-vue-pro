package cn.iocoder.yudao.module.bpm.job.Dto.receive.city;

import lombok.Data;

@Data
public class RemoteDocItem {
    private String ceadUuid; //数据 uuid
    private String ceadItemUuid; //主表数据 uuid
    private String ceadReceiverName; //接收单位
    private String ceadFilecode; //文件字号
    private String ceadCreateTime; //文件日期
    private String ceadTitle; //文件标题
    private String ceadState; //0：未签收；1已签收；2:已发送；3已退文；4已流转；5已办结；10已传阅



    // 其他字段根据需要添加
}