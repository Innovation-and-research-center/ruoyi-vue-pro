package cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo;

import lombok.Data;

import java.util.Date;

@Data
public   class PdfCommentInfo {
    private String commentDetail;
    private String userName;
    private Date commentDate;

    public PdfCommentInfo(String commentDetail, String userName, Date commentDate) {
        this.commentDetail = commentDetail;
        this.userName = userName;
        this.commentDate = commentDate;
    }
}