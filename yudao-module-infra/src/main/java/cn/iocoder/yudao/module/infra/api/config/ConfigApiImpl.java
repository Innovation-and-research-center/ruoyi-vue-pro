package cn.iocoder.yudao.module.infra.api.config;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
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
