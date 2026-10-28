package cn.iocoder.yudao.module.bpm.service.land;

import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.*;
import javax.validation.Valid;

public interface LandService {
    LandSaveRespVO save(Long userId, @Valid LandSaveReqVO request);
    void update(Long userId, @Valid LandSaveReqVO request);
    void submit(Long userId, @Valid LandSaveReqVO request);
    LandDetailRespVO get(Long userId, Long id);
    void updateStatus(Long id, String processInstanceId, Integer status);
}
