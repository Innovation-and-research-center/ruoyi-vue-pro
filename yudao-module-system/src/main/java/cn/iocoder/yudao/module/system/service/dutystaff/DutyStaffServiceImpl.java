package cn.iocoder.yudao.module.system.service.dutystaff;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dutystaff.vo.DutyStaffSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dutystaff.DutyStaffMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import cn.iocoder.yudao.module.system.api.dict.DictDataApi;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import java.time.LocalDateTime;
import java.util.*;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
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

    @Resource
    private DictDataApi dictDataApi;

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
    public List<DutyStaffDO> getStaffListByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        return staffMapper.selectList(new LambdaQueryWrapper<DutyStaffDO>()
                .ge(startTime != null, DutyStaffDO::getDutyDate, startTime)
                .le(endTime != null, DutyStaffDO::getDutyDate, endTime)
                .orderByAsc(DutyStaffDO::getDutyDate));
    }

    @Override
    public DutyImportRespVO importDutyList(List<Map<String, Object>> importDutys, boolean isUpdateSupport) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(importDutys)) {
            throw exception(DUST_IMPORT_LIST_IS_EMPTY);
        }
        // 1.2 获取字典映射: Label -> Value
        List<DictDataRespDTO> dictDataList = dictDataApi.getDictDataList("duty_staff_type");
        Map<String, String> dictLabelToValue = new HashMap<>();
        if (CollUtil.isNotEmpty(dictDataList)) {
            dictDataList.forEach(d -> dictLabelToValue.put(d.getLabel(), d.getValue()));
        }

        // 2. 遍历，逐个创建 or 更新
        DutyImportRespVO respVO = DutyImportRespVO.builder()
                .createDutyNames(new ArrayList<>())
                .updateDutyNames(new ArrayList<>())
                .failureDutyNames(new HashMap<>())
                .build();

        importDutys.forEach(importDuty -> {
            String dateString = (String) importDuty.get("日期");
            if (StrUtil.isBlank(dateString)) {
                // 尝试 "值班日期"
                dateString = (String) importDuty.get("值班日期");
            }
            if (StrUtil.isBlank(dateString)) {
                return; // 跳过无日期行
            }

            LocalDate dutyDate;
            try {
                if (dateString.contains("/")) {
                    dutyDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy/M/d"));
                } else if (dateString.contains("-")) {
                    dutyDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-M-d"));
                }else {
                    dutyDate = LocalDate.parse(dateString);
                }
            } catch (Exception e) {
                respVO.getFailureDutyNames().put(dateString, "日期格式错误: " + dateString);
                return;
            }
            LocalDateTime queryTime = dutyDate.atStartOfDay();
            List<DutyStaffDO> existingList = staffMapper.selectList(
                    new LambdaQueryWrapper<DutyStaffDO>()
                            .eq(DutyStaffDO::getDutyDate, queryTime) // 这里会自动处理类型转换
                            .eq(DutyStaffDO::getDeleted, 0) // 显式加上未删除条件（如果全局没配置逻辑删除）
            );
            // 遍历所有列，匹配字典
            for (Map.Entry<String, Object> entry : importDuty.entrySet()) {
                String header = entry.getKey();
                Object val = entry.getValue();
                String staffName = val != null ? String.valueOf(val) : "";

                if (!dictLabelToValue.containsKey(header)) {
                    continue; // 非值班类型列
                }
                String staffType = dictLabelToValue.get(header);

                if (StrUtil.isBlank(staffName)) {
                    continue; // 名字为空
                }

                // 校验用户
                List<AdminUserDO> users = userMapper.selectListByNickname(staffName);
                if (CollUtil.isEmpty(users)) {
                    // 使用 merge 记录错误，防止覆盖
                    respVO.getFailureDutyNames().merge(
                            dateString,
                            header + "用户不存在:" + staffName,
                            (oldVal, newVal) -> oldVal + "; " + newVal
                    );
                    continue; // 跳过当前循环
                }
                AdminUserDO user = users.get(0);

                // 查找该日期下该类型的记录
                DutyStaffDO matchedDuty = null;
                if (CollUtil.isNotEmpty(existingList)) {
                    matchedDuty = existingList.stream()
                            .filter(d -> Objects.equals(d.getStaffType(), staffType))
                            .findFirst()
                            .orElse(null);
                }

                if (matchedDuty == null) {
                    // 插入
                    DutyStaffDO newDuty = DutyStaffDO.builder()
                            .dutyDate(dutyDate.atStartOfDay())
                            .staffName(user.getNickname())
                            .staffType(staffType)
                            .userId(user.getId())
                            .smsCount(0L)
                            .build();
                    staffMapper.insert(newDuty);
                    respVO.getCreateDutyNames().add(dateString + "-" + header);
                } else {
                    // 更新
                    if (!isUpdateSupport) {
                        respVO.getFailureDutyNames().put(dateString, "已存在不能更新");
                        continue;
                    }
                    if (!Objects.equals(matchedDuty.getUserId(), user.getId())) {
                        matchedDuty.setStaffName(user.getNickname());
                        matchedDuty.setUserId(user.getId());
                        staffMapper.updateById(matchedDuty);
                        respVO.getUpdateDutyNames().add(dateString + "-" + header);
                    }
                }
            }
        });
        return respVO;
    }

//    private void validateDutyForCreateOrUpdate(String dutyDate, String leader, String staff) {
//        DataPermissionUtils.executeIgnore(() -> {
//            validateUserExists(leader, "leader");
//            validateUserExists(staff, "staff");
//            return null;
//        });
//    }

//    void validateUserExists(String username, String staffType) {
//        if (StrUtil.isBlank(username)) {
//            return;
//        }
//        AdminUserDO user = userMapper.selectByUsername(username);
//        if (user == null) {
//            if (staffType.equals("leader")) {
//                throw exception(LEADER_NOT_EXISTS);
//            } else {
//                throw exception(PERSON_NOT_EXISTS);
//            }
//
//        }
//
//    }

}