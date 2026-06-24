package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.string.StrUtils;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmUserGroupDO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.BpmTaskCandidateInvoker;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.iocoder.yudao.module.bpm.service.definition.BpmUserGroupService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.delegate.DelegateExecution;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_REGISTER_TASK;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_REGISTER_USER_GROUP_CONFIG_KEY;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.RECEIVE_REGISTER_USER_GROUP_NAME;

/**
 * 收文登记节点的候选办理人工具。
 */
public final class BpmReceiveRegisterTaskUtils {

    private BpmReceiveRegisterTaskUtils() {
    }

    public static boolean isReceiveRegisterTask(String taskDefinitionKey) {
        return StrUtil.equals(RECEIVE_REGISTER_TASK, taskDefinitionKey);
    }

    public static Set<Long> calculateCandidateUserIds(DelegateExecution execution,
                                                      BpmTaskCandidateInvoker taskCandidateInvoker) {
        FlowElement flowElement = execution.getCurrentFlowElement();
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(flowElement);
        if (strategy != null) {
            Set<Long> userIds = taskCandidateInvoker.calculateUsersByTask(execution);
            if (CollUtil.isNotEmpty(userIds)) {
                return userIds;
            }
        }

        return calculateCandidateUserIds(flowElement);
    }

    public static Set<Long> calculateCandidateUserIds(BpmnModel bpmnModel, String activityId,
                                                      BpmTaskCandidateInvoker taskCandidateInvoker,
                                                      Long startUserId, String processDefinitionId,
                                                      java.util.Map<String, Object> processVariables) {
        FlowElement flowElement = BpmnModelUtils.getFlowElementById(bpmnModel, activityId);
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(flowElement);
        if (strategy != null) {
            Set<Long> userIds = taskCandidateInvoker.calculateUsersByActivity(bpmnModel, activityId,
                    startUserId, processDefinitionId, processVariables);
            if (CollUtil.isNotEmpty(userIds)) {
                return userIds;
            }
        }
        return calculateCandidateUserIds(flowElement);
    }

    private static String getRegisterGroupIdsText(FlowElement flowElement) {
        Integer strategy = BpmnModelUtils.parseCandidateStrategy(flowElement);
        String param = BpmnModelUtils.parseCandidateParam(flowElement);
        if (BpmTaskCandidateStrategyEnum.USER_GROUP.getStrategy().equals(strategy) && StrUtil.isNotBlank(param)) {
            return param;
        }
        return null;
    }

    private static Set<Long> calculateCandidateUserIds(FlowElement flowElement) {
        String groupIdsText = getRegisterGroupIdsText(flowElement);
        BpmUserGroupService userGroupService = SpringUtil.getBean(BpmUserGroupService.class);
        List<BpmUserGroupDO> groups;
        if (StrUtil.isNotBlank(groupIdsText)) {
            Set<Long> groupIds = StrUtils.splitToLongSet(groupIdsText);
            if (CollUtil.isEmpty(groupIds)) {
                throw new IllegalStateException("收文登记组配置不正确：" + groupIdsText);
            }
            groups = userGroupService.getUserGroupList(groupIds);
        } else {
            groups = userGroupService.getUserGroupListByName(RECEIVE_REGISTER_USER_GROUP_NAME);
            if (CollUtil.isEmpty(groups)) {
                String configValue = SpringUtil.getBean(ConfigApi.class)
                        .getConfigValueByKey(RECEIVE_REGISTER_USER_GROUP_CONFIG_KEY);
                Set<Long> groupIds = StrUtils.splitToLongSet(configValue);
                groups = CollUtil.isNotEmpty(groupIds) ? userGroupService.getUserGroupList(groupIds) : java.util.Collections.emptyList();
            }
        }

        Set<Long> userIds = groups.stream()
                .filter(group -> CommonStatusEnum.ENABLE.getStatus().equals(group.getStatus()))
                .map(BpmUserGroupDO::getUserIds)
                .filter(CollUtil::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(userIds)) {
            throw new IllegalStateException("未配置启用的收文登记组或组内成员，请在流程管理-用户分组维护【"
                    + RECEIVE_REGISTER_USER_GROUP_NAME + "】");
        }
        return userIds;
    }

}
