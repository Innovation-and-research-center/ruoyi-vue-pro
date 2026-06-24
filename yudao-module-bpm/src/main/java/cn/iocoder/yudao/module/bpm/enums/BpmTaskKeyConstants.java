package cn.iocoder.yudao.module.bpm.enums;


public interface BpmTaskKeyConstants {
    // 请假
    String LEAVE = "oa_leave";
    // 收文
//    String RECEIVE = "receice_doc_v2_copy";

    String RECEIVE = "receice_doc_v2_copy_copy";
    String RECEIVE_REGISTER_TASK = "Activity_04ykbd0";
    String RECEIVE_REGISTER_USER_GROUP_NAME = "收文登记";
    String RECEIVE_REGISTER_USER_GROUP_CONFIG_KEY = "bpm.receive-doc.register-user-group-id";
    //会议报告单
    String CONFLOW_REPORT = "conference_report";
    //因公外出
    String OUT = "oa_out";

    String ELECTRIC = "oa_electric";
    //行政复议
    String XZFY = "oa_review";
    //行政诉讼
    String XZSS = "oa_lawsuit";
    //发文
    String SEND = "send_doc";

}
