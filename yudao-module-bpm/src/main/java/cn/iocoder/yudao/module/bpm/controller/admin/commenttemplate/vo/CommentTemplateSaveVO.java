package cn.iocoder.yudao.module.bpm.controller.admin.commenttemplate.vo;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.Map;

public final class CommentTemplateSaveVO {
    private CommentTemplateSaveVO() {}
    @Data
    public static class Personal {
        private Long oaIdeaId;
        @NotBlank @Size(max = 150) private String ideaMessage;
        @Size(max = 150) private String ideaType;
        @NotNull @Min(0) @Max(99999) private Integer ideaOrder;
    }
    @Data
    public static class Business {
        @Size(max = 50) private String commentGuid;
        @NotBlank @Size(max = 50) private String bizdefGuid;
        @NotBlank @Size(max = 50) private String commentName;
        @NotBlank @Size(max = 50) private String commentCode;
        @NotBlank @Size(max = 3000) private String commentContent;
        @Size(max = 500) private String meetFunction;
        @Size(max = 4000) private String commentAct;
        @NotNull @Min(0) private Integer seqOrder;
    }
    @Data
    public static class Binding {
        @NotBlank @Size(max = 255) private String processDefinitionKey;
        @NotBlank @Size(max = 50) private String bizdefGuid;
        @Size(max = 150) private String ideaType;
        @NotBlank private String sourceType;
        @NotNull @Size(max = 200) private Map<String, String> nodeMapping;
        @NotNull @Size(max = 200) private Map<String, String> fieldMapping;
    }
}
