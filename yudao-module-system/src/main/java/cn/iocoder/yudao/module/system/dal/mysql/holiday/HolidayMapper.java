package cn.iocoder.yudao.module.system.dal.mysql.holiday;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.holiday.HolidayDO;
import org.apache.ibatis.annotations.Mapper;
import cn.iocoder.yudao.module.system.controller.admin.holiday.vo.*;

/**
 * 节假日 Mapper
 *
 * @author 管理员
 */
@Mapper
public interface HolidayMapper extends BaseMapperX<HolidayDO> {


    default List<HolidayDO> selectByDate(String holidayDate){
        return selectList(HolidayDO::getSettingDate, holidayDate);
    }

    default PageResult<HolidayDO> selectPage(HolidayPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HolidayDO>()
                .betweenIfPresent(HolidayDO::getSettingDate, reqVO.getSettingDate())
                .eqIfPresent(HolidayDO::getIsworkday, reqVO.getIsworkday())
                .likeIfPresent(HolidayDO::getHoliDesc, reqVO.getHoliDesc())
                .betweenIfPresent(HolidayDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(HolidayDO::getId));
    }

}