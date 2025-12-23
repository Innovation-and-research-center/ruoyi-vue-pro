package cn.iocoder.yudao.module.system.dal.mysql.dutystaff;

import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffPageReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import org.apache.ibatis.annotations.Mapper;


/**
 * 值班 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DutyStaffMapper extends BaseMapperX<DutyStaffDO> {


    default List<DutyStaffDO> selectByDate(String dutyDate){
        return selectList(DutyStaffDO::getDutyDate, dutyDate);
    }

    default PageResult<DutyStaffDO> selectPage(DutyStaffPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DutyStaffDO>()
                .betweenIfPresent(DutyStaffDO::getDutyDate, reqVO.getDutyDate())
                .eqIfPresent(DutyStaffDO::getStaffType, reqVO.getStaffType())
                .eqIfPresent(DutyStaffDO::getUserId, reqVO.getUserId())
                .likeIfPresent(DutyStaffDO::getStaffName, reqVO.getStaffName())
                .eqIfPresent(DutyStaffDO::getSmsCount, reqVO.getSmsCount())
                .orderByDesc(DutyStaffDO::getId));
    }

}