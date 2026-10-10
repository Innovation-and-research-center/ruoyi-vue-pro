package cn.iocoder.yudao.module.infra.api.config.dto;

import lombok.Data;

/** 内部参数查询结果，不向浏览器公开配置值。 */
@Data
public class ConfigRespDTO {
    private Long id;
    private String name;
    private String key;
    private String value;
}
