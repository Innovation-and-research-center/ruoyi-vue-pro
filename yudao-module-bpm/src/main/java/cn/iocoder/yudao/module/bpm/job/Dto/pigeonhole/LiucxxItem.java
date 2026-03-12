package cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LiucxxItem {
    @JsonProperty("NodeName")
    private String nodeName;
    @JsonProperty("Author")
    private String author;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Body")
    private String body;
    @JsonProperty("Modified")
    private String modified;
    @JsonProperty("Dept")
    private String dept;
}
