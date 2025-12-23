package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;

import lombok.Data;

@Data
public class BpmUserOptionsReqVO {
    /** 筛选规则*/
    private String chooseRule;

    /** 规则值 */
    private String ruleValue;

}
