package cn.iocoder.yudao.module.system.service.holiday;

import java.util.*;
import javax.validation.*;

import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.holiday.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.holiday.HolidayDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 节假日 Service 接口
 *
 * @author 管理员
 */
public interface HolidayService {

    /**
     * 创建节假日
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createHoliday(@Valid HolidaySaveReqVO createReqVO);

    /**
     * 更新节假日
     *
     * @param updateReqVO 更新信息
     */
    void updateHoliday(@Valid HolidaySaveReqVO updateReqVO);

    /**
     * 删除节假日
     *
     * @param id 编号
     */
    void deleteHoliday(Long id);

    /**
    * 批量删除节假日
    *
    * @param ids 编号
    */
    void deleteHolidayListByIds(List<Long> ids);

    /**
     * 获得节假日
     *
     * @param id 编号
     * @return 节假日
     */
    HolidayDO getHoliday(Long id);

    /**
     * 获得节假日分页
     *
     * @param pageReqVO 分页查询
     * @return 节假日分页
     */
    PageResult<HolidayDO> getHolidayPage(HolidayPageReqVO pageReqVO);


    HolidayImportRespVO importHolidayList(List<HolidayImportExcelVO> importHolidays, boolean isUpdateSupport);

}