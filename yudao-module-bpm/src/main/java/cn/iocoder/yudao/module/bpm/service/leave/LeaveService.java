package cn.iocoder.yudao.module.bpm.service.leave;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.leave.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
/**
 * 假期申请审批 Service 接口
 *
 * @author 芋道源码
 */
public interface LeaveService {

    /**
     * 创建假期申请审批
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLeave(Long userId,@Valid LeaveSaveReqVO createReqVO);

    /**
     * 更新假期申请审批
     *
     * @param updateReqVO 更新信息
     */
    void updateLeave(@Valid LeaveSaveReqVO updateReqVO);

    /**
     * 删除假期申请审批
     *
     * @param id 编号
     */
    void deleteLeave(Long id,String reason);

    /**
    * 批量删除假期申请审批
    *
    * @param ids 编号
    */
    void deleteLeaveListByIds(List<Long> ids,String reason);

    /**
     * 获得假期申请审批
     *
     * @param id 编号
     * @return 假期申请审批
     */
    LeaveDO getLeave(Long id);

    LeaveDetailRespVO getLeaveDetail(Long id);

    /**
     * 获得假期申请审批分页
     *
     * @param pageReqVO 分页查询
     * @return 假期申请审批分页
     */
    PageResult<LeaveDO> getLeavePage(LeavePageReqVO pageReqVO);


    void updateLeaveStatus(Long id, Integer status);

    List<LeaveSummaryRespVO> getLeaveSummary(LeaveSummaryReqVO reqVO);

    List<LeaveHistoryRespVO> getLeaveDetailList(LeaveSummaryReqVO reqVO);

    PageResult<LeaveHistoryRespVO> getLeaveHistoryPage(LeavePageReqVO pageReqVO);

    List<LeaveTypeStatRespVO> getCurrentUserYearlyLeaveStat();

    List<LeaveAttachRespVO> getLeaveAttachListByLeaveId(Long leaveId);
}