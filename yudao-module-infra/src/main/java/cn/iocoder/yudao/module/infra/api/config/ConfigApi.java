package cn.iocoder.yudao.module.infra.api.config;

import cn.iocoder.yudao.module.infra.api.config.dto.ConfigRespDTO;
import java.util.List;

/**
 * 参数配置 API 接口
 *
 * @author 芋道源码
 */
public interface ConfigApi {

    /**
     * 根据参数键查询参数值
     *
     * @param key 参数键
     * @return 参数值
     */
    String getConfigValueByKey(String key);

    /** 按参数键前缀查询，按配置 ID 升序返回。 */
    List<ConfigRespDTO> getConfigsByKeyPrefix(String prefix);

    void getConfigValueByKey(String key,String value);

}
