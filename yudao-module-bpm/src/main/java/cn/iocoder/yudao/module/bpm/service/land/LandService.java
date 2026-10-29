package cn.iocoder.yudao.module.bpm.service.land;

import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.*;
import javax.validation.Valid;
import cn.iocoder.yudao.framework.common.pojo.PageResult;

public interface LandService {
    LandSaveRespVO save(Long userId, @Valid LandSaveReqVO request);
    void update(Long userId, @Valid LandSaveReqVO request);
    void managementUpdate(Long userId, @Valid LandSaveReqVO request);
    void delete(Long userId, Long id, String reason);
    void submit(Long userId, @Valid LandSaveReqVO request);
    PageResult<LandPageRespVO> getPage(Long userId, @Valid LandPageReqVO request);
    LandDetailRespVO get(Long userId, Long id);
    void updateStatus(Long id, String processInstanceId, Integer status);
}
