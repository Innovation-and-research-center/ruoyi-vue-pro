package cn.iocoder.yudao.module.bpm.job.Dto.receive.department;

import lombok.Data;

@Data
public class DepartmentDocItem {

    /**
     * 应用类型
     */
    private String cnApplication;

    /**
     * 办件类型
     */
    private String cnType;

    /**
     * 办理状态
     */
    private String cnStatus;

    /**
     * 阅读状态
     */
    private String cnReadStatus;

    /**
     * 紧急程度
     */
    private String cnUrgentStatus;

    /**
     * 标题
     */
    private String cnTitle;

    /**
     * 来源
     */
    private String cnPreprocessorName;

    /**
     * 创建时间
     */
    private String cnCreateTime;

    /**
     * 待办 UUID
     */
    private String cnIdos;

}
