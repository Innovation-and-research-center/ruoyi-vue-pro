package cn.iocoder.yudao.module.bpm.job.Dto.receive.notice;

import lombok.Data;

@Data
public  class OaNoticeDTO {
    private String uuid;       // 注意：C#代码中混用了 uuid 和 oanoUuid，JSON通常返回字段名
    private String oanoUuid;   // 建议两个都定义，JSON解析时会自动匹配
    private String oanoTitle;
    private String oanoDepName;
    private String oanoSendDate;
    private String oanoType;   // 用于生成文号
    private String oanoContent; // HTML内容
}
