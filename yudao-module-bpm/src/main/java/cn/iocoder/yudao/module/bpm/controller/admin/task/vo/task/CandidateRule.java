package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public  class CandidateRule {
    private String type; // role, group
    private String value; // ids string
}
