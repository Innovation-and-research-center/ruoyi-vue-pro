package cn.iocoder.yudao.module.infra.api.config;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.api.config.dto.ConfigRespDTO;
import java.util.List;
import java.util.stream.Collectors;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 参数配置 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ConfigApiImpl implements ConfigApi {

    @Resource
    private ConfigService configService;

    @Override
    public List<ConfigRespDTO> getConfigsByKeyPrefix(String prefix) {
        return configService.getConfigsByKeyPrefix(prefix).stream().map(config -> {
            ConfigRespDTO dto = new ConfigRespDTO();
            dto.setId(config.getId());
            dto.setName(config.getName());
            dto.setKey(config.getConfigKey());
            dto.setValue(config.getValue());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public String getConfigValueByKey(String key) {
        ConfigDO config = configService.getConfigByKey(key);
        return config != null ? config.getValue() : null;
    }

    @Override
    public void getConfigValueByKey(String key,String value) {
        ConfigDO config = configService.getConfigByKey(key);
        ConfigSaveReqVO reqVO = BeanUtils.toBean(config, ConfigSaveReqVO.class);
        reqVO.setValue(value);
        reqVO.setKey(key);
        configService.updateConfig(reqVO);
    }


}
