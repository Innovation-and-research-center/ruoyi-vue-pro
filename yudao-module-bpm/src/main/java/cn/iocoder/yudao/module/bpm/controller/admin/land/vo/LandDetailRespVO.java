package cn.iocoder.yudao.module.bpm.controller.admin.land.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LandDetailRespVO {
    private Long id;
    private String projectName;
    private String location;
    private String deptOpinion;
    private LocalDateTime applyDate;
    private String startUnit;
    private String startUserName;
    private String deptName;
    private String processInstanceId;
    private Integer status;
    private String taskId;
    private String filepath;
    private List<Attachment> fileList;

    @Data
    public static class Attachment {
        private Long id;
        private Long attachFileId;
        private String fileName;
        private String filePath;
        private String fileUrl;
        private String fileExtension;
    }
}
