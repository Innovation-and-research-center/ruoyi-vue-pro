package cn.iocoder.yudao.module.bpm.job.Dto.receive.city;

import lombok.Data;

@Data
public class RemoteBean {
    private String ceadTitle; //文件标题
    private String ceadFilecode; //文件字号
    private String ceadCreateTime; // 文件日期
    private String ceadDeptName; // 发文部门
    private String ceadUuid; //uuid
}
