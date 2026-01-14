package cn.iocoder.yudao.module.system.service.dutystaff;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dutystaff.DutyStaffMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;


/**
 * 值班 Service 实现类
 *
 * @author 芋道源码
 */
@Service("dutyStaffService")
@Slf4j
public class DutyStaffServiceImpl implements DutyStaffService {

    @Resource
    private DutyStaffMapper staffMapper;

    @Resource
    private AdminUserMapper userMapper;

    @Override
    public Long createStaff(DutyStaffSaveReqVO createReqVO) {
        // 插入
        DutyStaffDO staff = BeanUtils.toBean(createReqVO, DutyStaffDO.class);
        staffMapper.insert(staff);

        // 返回
        return staff.getId();
    }

    @Override
    public void updateStaff(DutyStaffSaveReqVO updateReqVO) {
        // 校验存在
        validateStaffExists(updateReqVO.getId());
        AdminUserDO staff = userMapper.selectById(updateReqVO.getUserId());
        // 更新
        DutyStaffDO updateObj = BeanUtils.toBean(updateReqVO, DutyStaffDO.class);
        updateObj.setStaffName(staff.getUsername());
        staffMapper.updateById(updateObj);
    }

    @Override
    public void deleteStaff(Long id) {
        // 校验存在
        validateStaffExists(id);
        // 删除
        staffMapper.deleteById(id);
    }

    @Override
        public void deleteStaffListByIds(List<Long> ids) {
        // 删除
        staffMapper.deleteByIds(ids);
        }


    private void validateStaffExists(Long id) {
        if (staffMapper.selectById(id) == null) {
            throw exception(STAFF_NOT_EXISTS);
        }
    }

    @Override
    public DutyStaffDO getStaff(Long id) {
        return staffMapper.selectById(id);
    }

    @Override
    public PageResult<DutyStaffDO> getStaffPage(DutyStaffPageReqVO pageReqVO) {
        return staffMapper.selectPage(pageReqVO);
    }

    @Override
    public DutyImportRespVO importDutyList(List<DutyStaffImportExcelVO> importDutys, boolean isUpdateSupport) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(importDutys)) {
            throw exception(DUST_IMPORT_LIST_IS_EMPTY);
        }
        // 遍历，逐个创建 or 更新
        DutyImportRespVO respVO = DutyImportRespVO.builder().createDutyNames(new ArrayList<>())
                .updateDutyNames(new ArrayList<>())
                .failureDutyNames(new HashMap<>())
                .build();
        importDutys.forEach(importDuty -> {
            // 2.1.1 校验字段是否符合要求
            try{
                ValidationUtils.validate(BeanUtils.toBean(importDuty, DutyStaffImportExcelVO.class));
            }catch (ConstraintViolationException ex){
                respVO.getFailureDutyNames().put(importDuty.getDutyDate(), ex.getMessage());
                return;
            }
            // 2.1.2 校验，判断是否有不符合的原因
            try{
                validateDutyForCreateOrUpdate(importDuty.getDutyDate(),importDuty.getLeader(),importDuty.getStaff());

            }catch (ServiceException ex) {
                respVO.getFailureDutyNames().put(importDuty.getDutyDate(), ex.getMessage());
                return;
            }
            // 2.2.1 判断如果不存在，在进行插入
            List<DutyStaffDO> duty = staffMapper.selectByDate(importDuty.getDutyDate());
            AdminUserDO leader = userMapper.selectByUsername(importDuty.getLeader());
            AdminUserDO staff = userMapper.selectByUsername(importDuty.getStaff());
            String dateString = importDuty.getDutyDate();
            LocalDate dutyDate;
            if (dateString.contains("/")) {
                // 处理 2022/2/15 格式
                DateTimeFormatter slashFormatter = DateTimeFormatter.ofPattern("yyyy/M/d");
                dutyDate = LocalDate.parse(dateString, slashFormatter);
            } else {
                // 默认处理 2022-02-15 格式
                dutyDate = LocalDate.parse(dateString);
            }
            DutyStaffDO leaderDuty = DutyStaffDO.builder()
                    .dutyDate(dutyDate.atStartOfDay())
                    .staffName(leader.getUsername())
                    .staffType("1")
                    .userId(leader.getId())
                    .smsCount(0L)
                    .build();
            DutyStaffDO staffDuty = DutyStaffDO.builder()
                    .dutyDate(dutyDate.atStartOfDay())
                    .staffName(staff.getUsername())
                    .staffType("2")
                    .userId(staff.getId())
                    .smsCount(0L)
                    .build();
            if (duty.isEmpty()) {
                staffMapper.insert(leaderDuty);
                staffMapper.insert(staffDuty);
                respVO.getCreateDutyNames().add(importDuty.getDutyDate());
                return;
            }
            // 2.2.2 存在，则进行更新
            if(!isUpdateSupport){
                respVO.getFailureDutyNames().put(importDuty.getDutyDate(), DUTY_DATE_EXISTS.getMsg());
                return;
            }
            leaderDuty.setId(duty.get(0).getId());
            staffMapper.updateById(leaderDuty);
            respVO.getUpdateDutyNames().add(importDuty.getDutyDate());

        });
        return respVO;
    }

    private void validateDutyForCreateOrUpdate(String dutyDate, String leader, String staff) {
        DataPermissionUtils.executeIgnore(() -> {
            validateUserExists(leader, "leader");
            validateUserExists(staff, "staff");
            return null;
        });
    }


    void validateUserExists(String username,String staffType) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user == null) {
            if(staffType.equals("leader")){
                throw exception(LEADER_NOT_EXISTS);
            }
            else{
                throw exception(PERSON_NOT_EXISTS);
            }

        }

    }


}