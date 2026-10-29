package cn.iocoder.yudao.module.bpm.service.land;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.land.vo.*;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmNextTaskRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.land.LandDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.land.LandMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmRegisterTaskService;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmTaskSortMapper;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.logger.BpmUpdateOperateLogService;
import cn.iocoder.yudao.module.bpm.service.logger.BpmDeleteOperateLogService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;

@Service
@Validated
@DataPermission(enable = false) // 按登记任务、流程参与关系及管理操作权限校验；租户过滤仍生效。
public class LandServiceImpl implements LandService {
    public static final String PROCESS_KEY = "lang_use";
    public static final String REGISTER_NODE = "Activity_1waprli";
    public static final String MAIN_NODE = "Activity_09u6oi7";

    @Resource private LandMapper landMapper;
    @Resource private LandAttachMapper landAttachMapper;
    @Resource private FileMapper fileMapper;
    @Resource private BpmProcessInstanceApi processInstanceApi;
    @Resource private BpmProcessInstanceService processInstanceService;
    @Resource private BpmRegisterTaskService registerTaskService;
    @Resource private TaskService taskService;
    @Resource private HistoryService historyService;
    @Resource private RuntimeService runtimeService;
    @Resource private AdminUserApi userApi;
    @Resource private DeptApi deptApi;
    @Resource private PermissionApi permissionApi;
    @Resource private BpmTaskSortMapper taskSortMapper;
    @Resource private BpmInvalidateHelper invalidateHelper;
    @Resource private BpmUpdateOperateLogService updateLogService;
    @Resource private BpmDeleteOperateLogService deleteLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LandSaveRespVO save(Long userId, LandSaveReqVO request) {
        if (request.getId() != null) {
            LandDO land = requireLand(request.getId(), true);
            Task task = requireRegistration(userId, land);
            updateData(userId, land, request);
            return saveResult(land, task);
        }
        AdminUserRespDTO user = userApi.getUser(userId);
        if (user == null) throw exception(LAND_ACCESS_DENIED);
        DeptRespDTO dept = user.getDeptId() != null ? deptApi.getDept(user.getDeptId()) : null;
        LandDO land = new LandDO().setProjectName(request.getProjectName().trim())
                .setLocation(request.getLocation().trim()).setDeptOpinion(StrUtil.nullToEmpty(request.getDeptOpinion()))
                .setApplyDate(request.getApplyDate() != null ? request.getApplyDate() : LocalDateTime.now())
                .setUserId(userId).setDeptId(user.getDeptId()).setStartUnit("登记中心")
                .setStartUserName(user.getNickname()).setDeptName(dept != null ? dept.getName() : "")
                .setStatus(BpmProcessInstanceStatusEnum.RUNNING.getStatus());
        landMapper.insert(land);
        land.setFilepath(syncAttachments(userId, land.getId(), request.getFileList()));
        // 流程发起范围仍由公共流程创建接口按模型配置校验，不硬编码账号。
        String processId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(PROCESS_KEY)
                        .setBusinessKey(String.valueOf(land.getId())).setVariables(variables(land)));
        land.setProcessInstanceId(processId);
        landMapper.updateById(new LandDO().setId(land.getId()).setProcessInstanceId(processId)
                .setFilepath(land.getFilepath()));
        syncProcessName(land);
        if (registerTaskService.claim(userId, processId) != 1) throw exception(LAND_REGISTER_NOT_ACTIVE);
        return saveResult(land, requireRegistration(userId, land));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, LandSaveReqVO request) {
        LandDO land = requireLand(request.getId(), true);
        requireRegistration(userId, land);
        updateData(userId, land, request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void managementUpdate(Long userId, LandSaveReqVO request) {
        requirePermission(userId, "bpm:land:update");
        LandDO land = requireLand(request.getId(), true);
        LandDO oldData = BeanUtils.toBean(land, LandDO.class);
        List<LandAttachDO> oldAttachments = landAttachMapper.selectByLandId(land.getId());
        updateData(userId, land, request, true);
        updateLogService.recordUpdate("土地用途认定", land.getId(), oldData, land,
                oldAttachments, landAttachMapper.selectByLandId(land.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id, String reason) {
        requirePermission(userId, "bpm:land:delete");
        if (StrUtil.isBlank(reason)) throw exception(LAND_DELETE_REASON_REQUIRED);
        LandDO land = requireLand(id, true);
        if (Objects.equals(land.getStatus(), BpmProcessInstanceStatusEnum.INVALID.getStatus())
                || Objects.equals(land.getStatus(), BpmProcessInstanceStatusEnum.CANCEL.getStatus())) {
            throw exception(LAND_CANNOT_DELETE);
        }
        invalidateHelper.executeInvalidate(userId, land.getProcessInstanceId(), land.getStatus(), reason.trim(),
                () -> landMapper.updateById(new LandDO().setId(id)
                        .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus())));
        deleteLogService.recordDelete("土地用途认定", id, reason.trim());
    }

    private void requirePermission(Long userId, String permission) {
        if (userId == null || !permissionApi.hasAnyPermissions(userId, permission)) {
            throw exception(LAND_ACCESS_DENIED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, LandSaveReqVO request) {
        // 同一办件串行校验登记任务，防止重复点击/并发提交重复派发。
        LandDO land = requireLand(request.getId(), true);
        Task registration = requireRegistration(userId, land);
        Map<String, List<Long>> assignees = validateMainAssignees(userId, land, registration, request);
        updateData(userId, land, request);
        Map<String, Object> variables = variables(land);
        variables.put(PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, assignees);
        if (registerTaskService.completeOnSubmit(userId, land.getProcessInstanceId(), variables,
                request.getSendSms()) != 1) throw exception(LAND_REGISTER_NOT_ACTIVE);
    }

    @Override
    public PageResult<LandPageRespVO> getPage(Long userId, LandPageReqVO request) {
        // 与业务管理列表一致，查询授权用户可查看当前租户办件；不授予办理、修改权限。
        if (!canManage(userId)) throw exception(LAND_ACCESS_DENIED);
        return BeanUtils.toBean(landMapper.selectPage(request), LandPageRespVO.class);
    }

    private boolean canManage(Long userId) {
        return userId != null && permissionApi.hasAnyPermissions(userId, "bpm:land:query");
    }

    @Override
    public LandDetailRespVO get(Long userId, Long id) {
        LandDO land = requireLand(id, false);
        if (!Objects.equals(userId, land.getUserId()) && (userId == null
                || StrUtil.isBlank(land.getProcessInstanceId())
                || historyService.createHistoricTaskInstanceQuery().processInstanceId(land.getProcessInstanceId())
                .or().taskAssignee(String.valueOf(userId)).taskOwner(String.valueOf(userId)).endOr().count() == 0)
                && !canManage(userId)
                && (userId == null || !permissionApi.hasAnyPermissions(userId, "bpm:land:update"))) {
            throw exception(LAND_ACCESS_DENIED);
        }
        LandDetailRespVO result = BeanUtils.toBean(land, LandDetailRespVO.class);
        List<LandAttachDO> attachments = landAttachMapper.selectByLandId(id);
        Map<Long, FileDO> files = files(attachments.stream().map(LandAttachDO::getAttachFileId).collect(Collectors.toSet()));
        result.setFileList(attachments.stream().map(attachment -> {
            LandDetailRespVO.Attachment item = BeanUtils.toBean(attachment, LandDetailRespVO.Attachment.class);
            FileDO file = files.get(attachment.getAttachFileId());
            item.setFileUrl(file != null && StrUtil.isNotBlank(file.getUrl()) ? file.getUrl() : attachment.getFilePath());
            return item;
        }).collect(Collectors.toList()));
        Task registration = findRegistration(userId, land);
        result.setTaskId(registration != null ? registration.getId() : null);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String processInstanceId, Integer status) {
        // 初次创建事件可能早于业务流程编号回写；已初始化为运行中，无需覆盖。
        if (id == null || StrUtil.isBlank(processInstanceId) || status == null) return;
        landMapper.update(new LandDO().setStatus(status), new LambdaQueryWrapper<LandDO>()
                .eq(LandDO::getId, id).eq(LandDO::getProcessInstanceId, processInstanceId)
                .ne(LandDO::getStatus, BpmProcessInstanceStatusEnum.INVALID.getStatus()));
    }

    private LandDO requireLand(Long id, boolean lock) {
        if (id == null) throw exception(LAND_NOT_EXISTS);
        LandDO land = lock ? landMapper.selectForUpdate(id) : landMapper.selectById(id);
        if (land == null) throw exception(LAND_NOT_EXISTS);
        return land;
    }

    private Task findRegistration(Long userId, LandDO land) {
        if (userId == null || StrUtil.isBlank(land.getProcessInstanceId())) return null;
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(land.getProcessInstanceId())
                .taskDefinitionKey(REGISTER_NODE).active().list();
        if (tasks.size() != 1) return null;
        Task task = tasks.get(0);
        return String.valueOf(userId).equals(task.getAssignee()) && task.getDelegationState() != DelegationState.PENDING
                ? task : null;
    }

    private Task requireRegistration(Long userId, LandDO land) {
        Task task = findRegistration(userId, land);
        if (task == null) throw exception(LAND_REGISTER_NOT_ACTIVE);
        return task;
    }

    private LandSaveRespVO saveResult(LandDO land, Task task) {
        return new LandSaveRespVO().setId(land.getId()).setProcessInstanceId(land.getProcessInstanceId()).setTaskId(task.getId());
    }

    private Map<String, Object> variables(LandDO land) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(PROCESS_CUSTOM_NAME, land.getProjectName());
        variables.put("projectName", land.getProjectName());
        variables.put("location", land.getLocation());
        variables.put("deptOpinion", land.getDeptOpinion());
        return variables;
    }

    private Map<String, List<Long>> validateMainAssignees(Long userId, LandDO land, Task registration, LandSaveReqVO request) {
        Map<String, List<Long>> requested = request.getNextNodeAssignees();
        if (requested == null || requested.size() != 1 || CollUtil.isEmpty(requested.get(MAIN_NODE))) {
            throw exception(LAND_ASSIGNEES_INVALID);
        }
        Set<Long> selected = new LinkedHashSet<>(requested.get(MAIN_NODE));
        Set<Long> allowed = new HashSet<>();
        List<BpmNextTaskRespVO> nodes = processInstanceService.getNextSelectNodes(userId,
                new BpmApprovalDetailReqVO().setProcessInstanceId(land.getProcessInstanceId()).setTaskId(registration.getId()));
        for (BpmNextTaskRespVO node : nodes) {
            if (!MAIN_NODE.equals(node.getTaskDefKey()) || node.getCandidateUsers() == null) continue;
            node.getCandidateUsers().forEach(group -> {
                if (group.getChildren() != null) group.getChildren().forEach(user -> allowed.add(user.getId()));
            });
        }
        if (selected.contains(null) || !allowed.containsAll(selected)) throw exception(LAND_ASSIGNEES_INVALID);
        return Collections.singletonMap(MAIN_NODE, new ArrayList<>(selected));
    }

    private void updateData(Long userId, LandDO land, LandSaveReqVO request) {
        updateData(userId, land, request, false);
    }

    private void updateData(Long userId, LandDO land, LandSaveReqVO request, boolean management) {
        land.setProjectName(request.getProjectName().trim()).setLocation(request.getLocation().trim())
                .setDeptOpinion(StrUtil.nullToEmpty(request.getDeptOpinion()));
        // 身份、日期、状态和流程编号沿用服务端记录，不能由页面越权修改。
        land.setFilepath(syncAttachments(userId, land.getId(), request.getFileList()));
        landMapper.updateById(new LandDO().setId(land.getId()).setProjectName(land.getProjectName())
                .setLocation(land.getLocation()).setDeptOpinion(land.getDeptOpinion()).setFilepath(land.getFilepath()));
        if (management) syncManagedProcessName(land);
        else syncProcessName(land);
    }

    private void syncManagedProcessName(LandDO land) {
        if (StrUtil.isBlank(land.getProcessInstanceId())) return;
        // 与行政复议一致：已结束流程也同步历史标题，运行中流程同时同步业务变量。
        taskSortMapper.updateRuntimeProcessInstanceName(land.getProcessInstanceId(), land.getProjectName());
        taskSortMapper.updateHistoricProcessInstanceName(land.getProcessInstanceId(), land.getProjectName());
        if (runtimeService.createProcessInstanceQuery().processInstanceId(land.getProcessInstanceId()).singleResult() != null) {
            runtimeService.setVariables(land.getProcessInstanceId(), variables(land));
        }
    }

    private void syncProcessName(LandDO land) {
        // 业务保存、标题变量和引擎流程名称同事务更新，避免只修改表单或只在发送时生效。
        runtimeService.setVariables(land.getProcessInstanceId(), variables(land));
        runtimeService.setProcessInstanceName(land.getProcessInstanceId(), land.getProjectName());
    }

    private Map<Long, FileDO> files(Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        return fileMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(FileDO::getId, file -> file));
    }

    private String syncAttachments(Long userId, Long landId, List<LandAttachSaveReqVO> requested) {
        List<LandAttachDO> previous = landAttachMapper.selectByLandId(landId);
        Map<Long, LandAttachDO> old = previous.stream().collect(Collectors.toMap(LandAttachDO::getAttachFileId,
                item -> item, (first, ignored) -> first));
        Set<Long> selected = new LinkedHashSet<>();
        if (requested != null) requested.forEach(item -> selected.add(item.getAttachFileId()));
        if (selected.contains(null)) throw exception(LAND_ATTACHMENT_INVALID);
        Map<Long, FileDO> files = files(selected);
        for (Long fileId : selected) {
            FileDO file = files.get(fileId);
            if (file == null || (!old.containsKey(fileId) && !String.valueOf(userId).equals(file.getCreator()))
                    || StrUtil.isBlank(file.getPath()) || StrUtil.isBlank(file.getName())) {
                throw exception(LAND_ATTACHMENT_INVALID);
            }
        }
        List<String> urls = new ArrayList<>();
        int sort = 0;
        for (Long fileId : selected) {
            FileDO file = files.get(fileId);
            LandAttachDO oldAttachment = old.get(fileId);
            LandAttachDO attachment = new LandAttachDO().setLandId(landId).setAttachFileId(fileId)
                    .setFilePath(file.getPath()).setFileName(file.getName()).setFileExtension(FileUtil.extName(file.getName()))
                    .setSort(sort++);
            if (oldAttachment == null) landAttachMapper.insert(attachment);
            else landAttachMapper.updateById(attachment.setId(oldAttachment.getId()));
            urls.add(StrUtil.blankToDefault(file.getUrl(), file.getPath()));
        }
        List<Long> removed = previous.stream().filter(item -> !selected.contains(item.getAttachFileId()))
                .map(LandAttachDO::getId).collect(Collectors.toList());
        if (!removed.isEmpty()) landAttachMapper.deleteBatchIds(removed);
        return String.join(",", urls);
    }
}
