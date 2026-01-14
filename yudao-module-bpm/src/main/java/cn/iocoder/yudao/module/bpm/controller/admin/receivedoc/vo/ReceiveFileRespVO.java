package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import lombok.Data;

@Data
public class ReceiveFileRespVO {

    private String fileUrl; // 文件访问路径 (URL)

    private Long id;
    /**
     * 收文编号(外键T_RECEIVE_DOC.RECEIVE_DOC_ID)
     */
    private Long receiveDocId;
    /**
     * 附件编号(外键T_ATTACH_FILE.ATTACH_FILE_ID)
     */
    private Long attachFileId;
    /**
     * 附件名称
     */
    private String attachFileName;
    /**
     * 附件顺序
     */
    private Short attachOrder;
    /**
     * 显示类型（0都显示1仅在pc显示2仅在app显示）
     */
    private Short showType;
}
