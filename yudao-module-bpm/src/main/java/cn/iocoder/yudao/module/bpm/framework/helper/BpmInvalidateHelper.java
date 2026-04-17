package cn.iocoder.yudao.module.bpm.framework.helper;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class BpmInvalidateHelper {

    @Resource
    private BpmProcessInstanceService processInstanceService;

    public void executeInvalidate(Long userId,String processInstanceId, Integer currentResult, String reason, Runnable updateBusinessDbAction) {
        // 1. 无论流程是否结束，先执行调用方传进来的数据库更新逻辑（保存原因、改状态为已作废）
        updateBusinessDbAction.run();

        // 2. 只有当流程还在“处理中” (状态码 1) 时，才调用工作流引擎的取消接口
        if (BpmProcessInstanceStatusEnum.RUNNING.getStatus().equals(currentResult)) {
            if (processInstanceId != null && !processInstanceId.trim().isEmpty()) {

                // 组装框架所需的 ReqVO
                BpmProcessInstanceCancelReqVO cancelReqVO = new BpmProcessInstanceCancelReqVO();
                cancelReqVO.setId(processInstanceId);
                cancelReqVO.setReason(reason);

                // 传入 userId 和 VO 进行取消操作
                processInstanceService.cancelProcessInstanceByAdmin(userId, cancelReqVO);
            } else {
                log.warn("业务作废：状态为审批中，但 processInstanceId 为空，无法取消工作流。");
            }
        }
    }
}
