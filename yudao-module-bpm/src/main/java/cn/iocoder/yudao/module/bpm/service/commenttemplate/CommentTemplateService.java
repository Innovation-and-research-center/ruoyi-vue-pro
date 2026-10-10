package cn.iocoder.yudao.module.bpm.service.commenttemplate;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.commenttemplate.vo.CommentTemplateSaveVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.commenttemplate.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.commenttemplate.*;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.senddoc.SendDocMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.leave.LeaveMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.timeexplain.TimeExplainMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzfy.XzfyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.xzss.XzssMapper;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.senddoc.SendDocDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.timeexplain.TimeExplainDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzfy.XzfyDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.flowable.engine.RepositoryService;
import org.flowable.task.api.Task;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class CommentTemplateService {
    private static final ErrorCode INVALID = new ErrorCode(1_009_090_001, "意见模板：{}");
    // 沿用旧 CommentTemplate.xml 的正文及顺序，仅在没有个人模板时展示。
    private static final List<String> DEFAULT_COMMENTS = Collections.unmodifiableList(Arrays.asList(
            "已阅。", "拟同意。", "同意。", "请XX办理。", "请XX处阅。", "拟同意,请厅长签发。",
            "请陈厅长阅示。", "请全处阅。", "请XX处会同XX处办理。",
            "请厅领导阅,请厅机关各处（室、局）和直属事业单位阅。"));
    @Resource private PersonalCommentTemplateMapper personalMapper;
    @Resource private BizCommentTemplateMapper bizMapper;
    @Resource private CommentTemplateBindingMapper bindingMapper;
    @Resource private BpmTaskService taskService;
    @Resource private RepositoryService repositoryService;
    @Resource private ObjectMapper objectMapper;
    @Resource private ReceiveDocMapper receiveDocMapper;
    @Resource private SendDocMapper sendDocMapper;
    @Resource private LeaveMapper leaveMapper;
    @Resource private ConfflowMapper confflowMapper;
    @Resource private TimeExplainMapper timeExplainMapper;
    @Resource private XzfyMapper xzfyMapper;
    @Resource private XzssMapper xzssMapper;

    public List<PersonalCommentTemplateDO> personalList(Long userId, String type) {
        return personalMapper.selectList(new LambdaQueryWrapperX<PersonalCommentTemplateDO>()
                .eq(PersonalCommentTemplateDO::getUserId, userId)
                .eqIfPresent(PersonalCommentTemplateDO::getIdeaType, type == null || type.isEmpty() ? null : type)
                .orderByAsc(PersonalCommentTemplateDO::getIdeaOrder, PersonalCommentTemplateDO::getOaIdeaId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long savePersonal(Long userId, CommentTemplateSaveVO.Personal vo) {
        PersonalCommentTemplateDO row = BeanUtils.toBean(vo, PersonalCommentTemplateDO.class);
        row.setUserId(userId);
        row.setIdeaMessage(vo.getIdeaMessage().trim());
        row.setIdeaType(vo.getIdeaType() == null ? "" : vo.getIdeaType());
        if (row.getOaIdeaId() == null) personalMapper.insert(row);
        else {
            requirePersonal(userId, row.getOaIdeaId());
            personalMapper.updateById(row);
        }
        return row.getOaIdeaId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void orderPersonal(Long userId, List<Long> ids) {
        if (new HashSet<>(ids).size() != ids.size()) throw exception(INVALID, "排序中存在重复模板");
        List<PersonalCommentTemplateDO> rows = ids.stream().map(id -> requirePersonal(userId, id))
                .collect(Collectors.toList());
        if (rows.stream().map(PersonalCommentTemplateDO::getIdeaType).distinct().count() > 1)
            throw exception(INVALID, "不能对不同类型的模板混合排序");
        for (int i = 0; i < rows.size(); i++) {
            PersonalCommentTemplateDO row = rows.get(i);
            row.setIdeaOrder(i);
            personalMapper.updateById(row);
        }
    }

    public void deletePersonal(Long userId, Long id) {
        requirePersonal(userId, id);
        personalMapper.deleteById(id);
    }

    private PersonalCommentTemplateDO requirePersonal(Long userId, Long id) {
        PersonalCommentTemplateDO row = personalMapper.selectById(id);
        if (row == null || !Objects.equals(row.getUserId(), userId)) throw exception(INVALID, "模板不存在或无权操作");
        return row;
    }

    public List<BizCommentTemplateDO> businessList(String bizdefGuid) {
        return bizMapper.selectList(new LambdaQueryWrapperX<BizCommentTemplateDO>()
                .eq(BizCommentTemplateDO::getBizdefGuid, bizdefGuid)
                .orderByAsc(BizCommentTemplateDO::getSeqOrder, BizCommentTemplateDO::getCommentGuid));
    }

    public String saveBusiness(CommentTemplateSaveVO.Business vo) {
        BizCommentTemplateDO row = BeanUtils.toBean(vo, BizCommentTemplateDO.class);
        if (row.getCommentGuid() == null || row.getCommentGuid().isEmpty()) {
            row.setCommentGuid(UUID.randomUUID().toString());
            bizMapper.insert(row);
        } else {
            if (bizMapper.selectById(row.getCommentGuid()) == null) throw exception(INVALID, "业务模板不存在");
            bizMapper.updateById(row);
        }
        return row.getCommentGuid();
    }

    public void deleteBusiness(String id) {
        if (bizMapper.selectById(id) == null) throw exception(INVALID, "业务模板不存在");
        bizMapper.deleteById(id);
    }

    public List<CommentTemplateBindingDO> bindings() {
        return bindingMapper.selectList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveBinding(CommentTemplateSaveVO.Binding vo) {
        Set<String> fields = fields(vo.getSourceType());
        vo.getFieldMapping().forEach((key, value) -> {
            if (key == null || key.trim().isEmpty() || value == null || !fields.contains(value))
                throw exception(INVALID, "字段映射必须使用可用业务字段");
        });
        vo.getNodeMapping().forEach((key, value) -> {
            if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty())
                throw exception(INVALID, "环节映射不能为空");
        });
        CommentTemplateBindingDO row = BeanUtils.toBean(vo, CommentTemplateBindingDO.class);
        row.setNodeMapping(json(vo.getNodeMapping()));
        row.setFieldMapping(json(vo.getFieldMapping()));
        CommentTemplateBindingDO old = findBinding(row.getProcessDefinitionKey());
        if (old == null) bindingMapper.insert(row);
        else {
            row.setId(old.getId());
            bindingMapper.updateById(row);
        }
    }

    public void deleteBinding(String processKey) {
        CommentTemplateBindingDO row = findBinding(processKey);
        if (row != null) bindingMapper.deleteById(row.getId());
    }

    @Data
    public static class Options {
        private String ideaType;
        private List<PersonalCommentTemplateDO> personal;
        private List<BizCommentTemplateDO> business;
    }

    public Options options(Long userId, String taskId, String code) {
        Task task = requireTask(userId, taskId);
        CommentTemplateBindingDO binding = binding(task);
        Options result = new Options();
        // 个人模板可以独立迁移；无业务绑定时，收发文沿用旧 IDEA_TYPE。
        result.setIdeaType(binding == null ? personalIdeaType(processKey(task)) : binding.getIdeaType());
        result.setPersonal(personalList(userId, result.getIdeaType()));
        if (result.getPersonal().isEmpty()) {
            List<PersonalCommentTemplateDO> defaults = new ArrayList<>();
            for (int i = 0; i < DEFAULT_COMMENTS.size(); i++) {
                PersonalCommentTemplateDO row = new PersonalCommentTemplateDO();
                row.setIdeaMessage(DEFAULT_COMMENTS.get(i));
                row.setIdeaType(result.getIdeaType());
                row.setIdeaOrder(i);
                defaults.add(row);
            }
            result.setPersonal(defaults);
        }
        result.setBusiness(available(binding, task.getTaskDefinitionKey(), code));
        // 不把模板原文及旧条件函数发送给普通用户。
        result.getBusiness().forEach(row -> { row.setCommentContent(null); row.setMeetFunction(null); });
        return result;
    }

    public String render(Long userId, String taskId, String id, String code) {
        Task task = requireTask(userId, taskId);
        CommentTemplateBindingDO binding = binding(task);
        List<BizCommentTemplateDO> rows = available(binding, task.getTaskDefinitionKey(), code);
        BizCommentTemplateDO template = rows.stream().filter(row -> Objects.equals(row.getCommentGuid(), id))
                .findFirst().orElseThrow(() -> exception(INVALID, "模板不适用于当前业务或环节"));
        Object business = loadBusiness(binding.getSourceType(), task.getProcessInstanceId());
        if (business == null) throw exception(INVALID, "未找到当前流程的业务数据");
        Map<String, String> aliases = mapping(binding.getFieldMapping());
        Set<String> fields = fields(binding.getSourceType());
        BeanWrapper bean = new BeanWrapperImpl(business);
        return CommentTemplateSupport.render(template.getCommentContent(), key -> {
            String property = aliases.getOrDefault(key, key);
            if (!fields.contains(property)) throw exception(INVALID, "未配置字段映射：" + key);
            return bean.getPropertyValue(property);
        });
    }

    private List<BizCommentTemplateDO> available(CommentTemplateBindingDO binding, String node, String code) {
        if (binding == null) return new ArrayList<>();
        Map<String, String> nodes = mapping(binding.getNodeMapping());
        return businessList(binding.getBizdefGuid()).stream().filter(row -> CommentTemplateSupport.applies(
                row.getCommentCode(), row.getCommentAct(), code, node, nodes)).collect(Collectors.toList());
    }

    private Task requireTask(Long userId, String taskId) {
        Task task = taskService.validateTask(userId, taskId);
        // validateTask 对未认领任务放行；模板读取只允许当前实际办理人。
        if (!String.valueOf(userId).equals(task.getAssignee())
                || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(task.getTenantId()))
            throw exception(INVALID, "无权读取当前任务的模板");
        return task;
    }

    private String processKey(Task task) {
        return repositoryService.getProcessDefinition(task.getProcessDefinitionId()).getKey();
    }

    private String personalIdeaType(String processKey) {
        if (BpmTaskKeyConstants.RECEIVE.equals(processKey)
                || BpmTaskKeyConstants.ELECTRIC.equals(processKey)) return "receivedoc";
        if (BpmTaskKeyConstants.SEND.equals(processKey)) return "senddoc";
        return processKey;
    }

    private CommentTemplateBindingDO binding(Task task) {
        return findBinding(processKey(task));
    }

    private CommentTemplateBindingDO findBinding(String processKey) {
        return bindingMapper.selectOne(new LambdaQueryWrapperX<CommentTemplateBindingDO>()
                .eq(CommentTemplateBindingDO::getProcessDefinitionKey, processKey));
    }

    private Map<String, String> mapping(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyMap();
        try { return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {}); }
        catch (Exception e) { throw exception(INVALID, "映射配置不是有效的 JSON 对象"); }
    }

    private String json(Map<String, String> value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw exception(INVALID, "无法保存映射配置"); }
    }

    public Set<String> fields(String sourceType) {
        Class<?> type;
        switch (sourceType) {
            case "receivedoc": type = ReceiveDocDO.class; break;
            case "senddoc": type = SendDocDO.class; break;
            case "leave": type = LeaveDO.class; break;
            case "confflow": type = ConfflowDO.class; break;
            case "timeexplain": type = TimeExplainDO.class; break;
            case "xzfy": type = XzfyDO.class; break;
            case "xzss": type = XzssDO.class; break;
            default: throw exception(INVALID, "不支持该业务数据类型");
        }
        return Arrays.stream(type.getDeclaredFields()).filter(f -> scalar(f.getType()))
                .map(Field::getName).collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean scalar(Class<?> type) {
        return type == String.class || Number.class.isAssignableFrom(type) || type == Boolean.class
                || TemporalAccessor.class.isAssignableFrom(type);
    }

    private Object loadBusiness(String sourceType, String processInstanceId) {
        switch (sourceType) {
            case "receivedoc": return byProcess(receiveDocMapper, processInstanceId);
            case "senddoc": return byProcess(sendDocMapper, processInstanceId);
            case "leave": return byProcess(leaveMapper, processInstanceId);
            case "confflow": return byProcess(confflowMapper, processInstanceId);
            case "timeexplain": return byProcess(timeExplainMapper, processInstanceId);
            case "xzfy": return byProcess(xzfyMapper, processInstanceId);
            case "xzss": return byProcess(xzssMapper, processInstanceId);
            default: throw exception(INVALID, "不支持该业务数据类型");
        }
    }

    private <T> T byProcess(BaseMapperX<T> mapper, String processInstanceId) {
        // 表和列均来自固定 mapper；禁止模板拼接 SQL 或执行函数。
        return mapper.selectOne(new QueryWrapper<T>().eq("process_instance_id", processInstanceId));
    }
}
