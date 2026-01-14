package cn.iocoder.yudao.module.bpm.job.Dto.receive.notice;

import lombok.Data;

import java.util.List;

@Data
public  class NoticeDetailDTO {
    private OaNoticeDTO notice;
    private List<OaFileDTO> fileList;
}
