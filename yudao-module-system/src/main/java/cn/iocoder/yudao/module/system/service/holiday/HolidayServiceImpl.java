package cn.iocoder.yudao.module.system.service.holiday;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.system.dal.dataobject.dutystaff.DutyStaffDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;

import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.system.controller.admin.holiday.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.holiday.HolidayDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;

import cn.iocoder.yudao.module.system.dal.mysql.holiday.HolidayMapper;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 节假日 Service 实现类
 *
 * @author 管理员
 */
@Service
@Validated
public class HolidayServiceImpl implements HolidayService {

    @Resource
    private HolidayMapper holidayMapper;

    @Override
    public Long createHoliday(HolidaySaveReqVO createReqVO) {
        // 插入
        HolidayDO holiday = BeanUtils.toBean(createReqVO, HolidayDO.class);
        holidayMapper.insert(holiday);

        // 返回
        return holiday.getId();
    }

    @Override
    public void updateHoliday(HolidaySaveReqVO updateReqVO) {
        // 校验存在
        validateHolidayExists(updateReqVO.getId());
        // 更新
        HolidayDO updateObj = BeanUtils.toBean(updateReqVO, HolidayDO.class);
        holidayMapper.updateById(updateObj);
    }

    @Override
    public void deleteHoliday(Long id) {
        // 校验存在
        validateHolidayExists(id);
        // 删除
        holidayMapper.deleteById(id);
    }

    @Override
        public void deleteHolidayListByIds(List<Long> ids) {
        // 删除
        holidayMapper.deleteByIds(ids);
        }


    private void validateHolidayExists(Long id) {
        if (holidayMapper.selectById(id) == null) {
            throw exception(HOLIDAY_NOT_EXISTS);
        }
    }

    @Override
    public HolidayDO getHoliday(Long id) {
        return holidayMapper.selectById(id);
    }

    @Override
    public PageResult<HolidayDO> getHolidayPage(HolidayPageReqVO pageReqVO) {
        return holidayMapper.selectPage(pageReqVO);
    }

    @Override
    public HolidayImportRespVO importHolidayList(List<HolidayImportExcelVO> importHolidays, boolean isUpdateSupport) {
        if(CollUtil.isEmpty(importHolidays)) {
            throw exception(HOLIDAY_IMPORT_LIST_IS_EMPTY);
        }
        HolidayImportRespVO importRespVO = HolidayImportRespVO.builder().createHolidayNames(new ArrayList<>())
                .updateHolidayNames(new ArrayList<>())
                .failureHolidayNames(new HashMap<>())
                .build();
        importHolidays.forEach(excelVO -> {
            try{
                ValidationUtils.validate(BeanUtils.toBean(excelVO, HolidayImportExcelVO.class));
            }catch (ConstraintViolationException ex){
                importRespVO.getFailureHolidayNames().put(excelVO.getSettingDate(), ex.getMessage());
                return;
            }

            String dateString = excelVO.getSettingDate();
            LocalDate holidayDate;
            if (dateString.contains("/")) {
                // 处理 2022/2/15 格式
                DateTimeFormatter slashFormatter = DateTimeFormatter.ofPattern("yyyy/M/d");
                holidayDate = LocalDate.parse(dateString, slashFormatter);
            } else {
                // 默认处理 2022-02-15 格式
                holidayDate = LocalDate.parse(dateString);
            }
            List<HolidayDO> holidayDo = holidayMapper.selectByDate(String.valueOf(holidayDate));

            if (CollUtil.isEmpty(holidayDo)) {
                HolidayDO holiday = HolidayDO.builder()
                        .settingDate(holidayDate.atStartOfDay())
                        .isworkday(excelVO.getIsworkday())
                        .holiDesc(excelVO.getHoliDesc())
                        .build();
                holidayMapper.insert(holiday);
                importRespVO.getCreateHolidayNames().add(excelVO.getSettingDate());
            }
            else {
                HolidayDO holiday = HolidayDO.builder()
                        .id(holidayDo.get(0).getId())
                        .settingDate(holidayDate.atStartOfDay())
                        .isworkday(excelVO.getIsworkday())
                        .holiDesc(excelVO.getHoliDesc())
                        .build();
                holidayMapper.updateById(holiday);
                importRespVO.getUpdateHolidayNames().add(excelVO.getSettingDate());
            }

        });
        return importRespVO;
    }

    @Override
    public List<Map<String, String>> getAllHolidaySummary() {
        // 1. 查询所有节假日数据（按时间排序，方便后续处理）
        List<HolidayDO> allList = holidayMapper.selectList(
                new LambdaQueryWrapper<HolidayDO>().orderByAsc(HolidayDO::getSettingDate)
        );

        if (allList.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按年份分组 Map<Integer, List<HolidayDO>>
        // 假设 getSettingDate() 返回的是 LocalDateTime。如果是 Date 类型，请看代码下方的注释修改。
        Map<Integer, List<HolidayDO>> groupedByYear = allList.stream()
                .collect(Collectors.groupingBy(item -> item.getSettingDate().getYear()));

        // 3. 构建结果列表
        List<Map<String, String>> resultList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 4. 遍历每个年份的数据
        groupedByYear.forEach((year, list) -> {
            // 提取工作日 (isworkday = 1)
            String workDayStr = list.stream()
                    .filter(item -> item.getIsworkday() != null && item.getIsworkday() == 1)
                    .map(item -> item.getSettingDate().format(formatter))
                    .collect(Collectors.joining(","));

            // 提取休息日 (isworkday = 0) -> 对应 key "restFat"
            String restFatStr = list.stream()
                    .filter(item -> item.getIsworkday() != null && item.getIsworkday() == 0)
                    .map(item -> item.getSettingDate().format(formatter))
                    .collect(Collectors.joining(","));

            // 组装单个年份的 Map
            Map<String, String> map = new HashMap<>();
            map.put("year", String.valueOf(year));
            map.put("workDay", workDayStr);
            map.put("restDay", restFatStr);

            resultList.add(map);
        });

        // 5. 按年份排序返回 (比如 2025, 2026...)
        resultList.sort(Comparator.comparing(m -> m.get("year")));

        return resultList;
    }

    @Override
    public LocalDateTime addWorkingDays(LocalDateTime startDate, int workDays) {
        if (startDate == null) {
            return null;
        }
        if (workDays <= 0) {
            return startDate;
        }

        // 1. 初始化数据：第一次查询
        // 假设初始步长为 workDays 的 2 倍，至少 30 天
        int queryStep = Math.max(workDays * 2, 30);
        LocalDateTime currentRangeEnd = startDate.plusDays(queryStep);

        // 获取初始节假日数据
        Map<LocalDate, Short> holidayMap = fetchHolidayMap(startDate, currentRangeEnd);

        LocalDateTime result = startDate;
        int addedDays = 0;

        // 2. 逐日推算
        while (addedDays < workDays) {
            result = result.plusDays(1);
            LocalDate currentDate = result.toLocalDate();

            // 【兜底逻辑】：如果当前日期已经接近或超过了 Map 的最大范围
            if (currentDate.isAfter(currentRangeEnd.toLocalDate()) || currentDate.isEqual(currentRangeEnd.toLocalDate())) {
                // 动态向后延长范围，再次查询数据库并合并到 Map 中
                LocalDateTime nextRangeEnd = currentRangeEnd.plusDays(queryStep);
                Map<LocalDate, Short> nextMap = fetchHolidayMap(currentRangeEnd.plusDays(1), nextRangeEnd);
                holidayMap.putAll(nextMap);
                currentRangeEnd = nextRangeEnd; // 更新当前边界
            }

            // 3. 核心计算逻辑
            if (holidayMap.containsKey(currentDate)) {
                Short isWorkDayConfig = holidayMap.get(currentDate);
                // 调休补班 (isworkday=1) -> 计入工作日
                if (isWorkDayConfig != null && isWorkDayConfig == 1) {
                    addedDays++;
                }
                // 法定假日 (isworkday=0) -> 跳过，不计入 addedDays
            } else {
                // 无配置：默认排除周六日
                java.time.DayOfWeek dayOfWeek = result.getDayOfWeek();
                if (dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY) {
                    addedDays++;
                }
            }
        }
        return result;
    }

    /**
     * 提取出来的私有辅助方法：根据范围查询节假日配置
     */
    private Map<LocalDate, Short> fetchHolidayMap(LocalDateTime start, LocalDateTime end) {
        List<HolidayDO> list = holidayMapper.selectList(
                new LambdaQueryWrapper<HolidayDO>()
                        .ge(HolidayDO::getSettingDate, start.toLocalDate().atStartOfDay())
                        .le(HolidayDO::getSettingDate, end.toLocalDate().atTime(23, 59, 59))
        );
        return list.stream().collect(Collectors.toMap(
                h -> h.getSettingDate().toLocalDate(),
                HolidayDO::getIsworkday,
                (v1, v2) -> v1
        ));
    }




}