package cn.iocoder.yudao.module.system.service.dutystaff;

import java.time.LocalDateTime;
import java.util.*;
import javax.validation.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;

/**
 * 值班 Service 接口
 *
 * @author 芋道源码
 */
public interface DutyStaffService {

    /**
     * 创建值班
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStaff(@Valid DutyStaffSaveReqVO createReqVO);

    /**
     * 更新值班
     *
     * @param updateReqVO 更新信息
     */
    void updateStaff(@Valid DutyStaffSaveReqVO updateReqVO);

    /**
     * 删除值班
     *
     * @param id 编号
     */
    void deleteStaff(Long id);

    /**
     * 批量删除值班
     *
     * @param ids 编号
     */
    void deleteStaffListByIds(List<Long> ids);

    /**
     * 获得值班
     *
     * @param id 编号
     * @return 值班
     */
    DutyStaffDO getStaff(Long id);

    /**
     * 获得值班分页
     *
     * @param pageReqVO 分页查询
     * @return 值班分页
     */
    PageResult<DutyStaffDO> getStaffPage(DutyStaffPageReqVO pageReqVO);

    DutyImportRespVO importDutyList(List<Map<String, Object>> importDutys, boolean isUpdateSupport);

    List<DutyStaffDO> getStaffListByDateRange(LocalDateTime startTime, LocalDateTime endTime);

}