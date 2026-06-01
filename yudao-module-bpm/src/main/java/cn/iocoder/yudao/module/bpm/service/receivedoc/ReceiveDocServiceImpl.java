package cn.iocoder.yudao.module.bpm.service.receivedoc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceCancelReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.leave.LeaveDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.framework.helper.BpmInvalidateHelper;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.aspose.words.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;

/**
 * 收文 Service 实现类
 *
 * @author 芋道
 */
@Service
@Validated
@Slf4j
public class ReceiveDocServiceImpl implements ReceiveDocService {

    public static final String PROCESS_KEY = RECEIVE;

    public static final String PROCESS_KEY_CHANGE = ELECTRIC;

    @Resource
    private ReceiveDocMapper receiveDocMapper;

    @Resource
    private ReceiveDocAttachMapper receiveDocAttachMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private FileApi fileApi; // 注入文件 API

    @Resource
    private FileMapper fileMapper;

    @Resource
    private BpmTaskService taskService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private BpmInvalidateHelper bpmInvalidateHelper;

    @Resource
    private RuntimeService runtimeService;

    @Override
    public Long createReceiveDoc(Long userId,ReceiveDocSaveReqVO createReqVO) {

        if (checkReceiveDocNumberExists(createReqVO.getReceiveDocNumber())) {
            throw exception(RECEIVE_DOC_EXISTS);
        }
        ReceiveDocDO receiveDoc = BeanUtils.toBean(createReqVO, ReceiveDocDO.class);
        Long sequence = extractSequenceFromNumber(receiveDoc.getReceiveDocNumber());
        if (sequence != null) {
            receiveDoc.setDocSequence(sequence);
        } else {
            // 如果解析失败（比如格式不对），建议抛出异常阻止保存，否则下次生成会出错
            throw exception(RECEIVE_DOC_ERROR);
        }
        // 插入
        receiveDocMapper.insert(receiveDoc);
        createReceiveDocAttachList(receiveDoc.getId(), createReqVO.getFileList());
        Map<String, Object> processInstanceVariables = new HashMap<>();

        if (CollUtil.isNotEmpty(createReqVO.getProcessVariables())) {
            processInstanceVariables.putAll(createReqVO.getProcessVariables());
        }
        String realKey=PROCESS_KEY;
        String processName = "收文";
        if (!StringUtil.isEmpty(createReqVO.getDocRange())){
            realKey=PROCESS_KEY_CHANGE;
            processName="电子公告";
        }

//        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        //自定义标题
        String customName =StringUtil.isEmpty(receiveDoc.getSubject()) ? processName:receiveDoc.getSubject();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(PROCESS_URGENCY_DEGREE, receiveDoc.getUrgencyDegree());
        String timeKey = "receive";
        String timeoutLabel = DictFrameworkUtils.parseDictDataLabel("bpm_process_timeout_config", timeKey);
        if (StrUtil.isNotBlank(timeoutLabel) && NumberUtil.isNumber(timeoutLabel)) {
            int hours = Integer.parseInt(timeoutLabel);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = now.plusHours(hours);
            processInstanceVariables.put(PROCESS_FINISH_TIME, timeoutLabel);
            processInstanceVariables.put(PROCESS_DEADLINE_DATE, DateUtils.of(deadline));
        }
        processInstanceVariables.put(PROCESS_SOURCE_UNIT,createReqVO.getSendDept());

        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(realKey)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(receiveDoc.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        receiveDocMapper.updateById(new ReceiveDocDO().setId(receiveDoc.getId()).setProcessInstanceId(processInstanceId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue()));


        // 返回
        return receiveDoc.getId();
    }


    @Override
    public Long saveReceiveDoc(Long userId,ReceiveDocSaveReqVO createReqVO) {

        if (checkReceiveDocNumberExists(createReqVO.getReceiveDocNumber())) {
            throw exception(RECEIVE_DOC_EXISTS);
        }
        ReceiveDocDO receiveDoc = BeanUtils.toBean(createReqVO, ReceiveDocDO.class);
        receiveDoc.setCreator(String.valueOf(userId));
        Long sequence = extractSequenceFromNumber(receiveDoc.getReceiveDocNumber());
        if (sequence != null) {
            receiveDoc.setDocSequence(sequence);
        } else {
            // 如果解析失败（比如格式不对），建议抛出异常阻止保存，否则下次生成会出错
            throw exception(RECEIVE_DOC_ERROR);
        }
        // 插入

        receiveDocMapper.insert(receiveDoc);
        createReceiveDocAttachList(receiveDoc.getId(), createReqVO.getFileList());
        // 返回
        return receiveDoc.getId();
    }

    private Long extractSequenceFromNumber(String docNumber) {
        try {
            if (StrUtil.isEmpty(docNumber)) return null;

            // 方式1：如果你确定最后4位一定是数字
            // 截取最后4位
            // String numStr = docNumber.substring(docNumber.length() - 4);

            // 方式2 (推荐)：截取最后一个横杠 "-" 后面的所有内容，更稳健
            int lastDashIndex = docNumber.lastIndexOf("-");
            if (lastDashIndex == -1 || lastDashIndex == docNumber.length() - 1) {
                return null; // 格式不对，没有横杠
            }

            String numStr = docNumber.substring(lastDashIndex + 1);

            // 解析为 Long (自动去除前导零，例如 "0005" -> 5)
            return Long.parseLong(numStr);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkReceiveDocNumberExists(String docNumber) {
        if (StrUtil.isEmpty(docNumber)) {
            return false;
        }
        // 使用 MyBatis Plus 查询是否存在
        return receiveDocMapper.selectCount(
                Wrappers.<ReceiveDocDO>lambdaQuery().eq(ReceiveDocDO::getReceiveDocNumber, docNumber)
        ) > 0;
    }

    @Override
    public String generateDocumentSequence(ReceiveDocCreateNumberVO createReqVO) {
        if(StringUtil.isEmpty(createReqVO.getYear())){
            createReqVO.setYear(String.valueOf(LocalDate.now().getYear()));
        }
        return getNextDocSequence(createReqVO.getDocClass(), createReqVO.getYear());
    }

    @Override
    public Long generateDocumentSequence(String docClass) {
        return getNextDocSequenceForJob(docClass, String.valueOf(LocalDate.now().getYear()));
    }

    @Override
    public Long generateDocumentSequence(String docClass, String year) {
        return getNextDocSequenceForJob(docClass, year);
    }


    private String getNextDocSequence(String docClass, String year) {
        int yearInt = Integer.parseInt(year);
        LocalDateTime startOfYear = LocalDateTime.of(yearInt, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(yearInt, 12, 31, 23, 59, 59);
        QueryWrapper<ReceiveDocDO> query = Wrappers.query();
        query.select("MAX(DOC_SEQUENCE)")
                .eq("DOC_CLASS", docClass)
                .and(wrapper -> wrapper
                        // 情况1：YEAR 字段明确等于传入的年份
                        .eq("YEAR", year)
                        // 情况2：或者 (YEAR 为空 且 RECEIVE_TIME 在该年份范围内)
                        .or(orWrapper -> orWrapper
                                .isNull("YEAR") // 或者使用 .eq("YEAR", "") 取决于你数据库存的是 NULL 还是空字符串
                                .ge("RECEIVE_TIME", startOfYear)
                                .le("RECEIVE_TIME", endOfYear)
                        )
                );

        long nextVal = executeMaxQuery(query);
        return String.format("%d-%s-%04d", yearInt, docClass, nextVal);
    }

    private long getNextDocSequenceForJob(String docClass, String year) {
        int yearInt = Integer.parseInt(year);
        LocalDateTime startOfYear = LocalDateTime.of(yearInt, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(yearInt, 12, 31, 23, 59, 59);
        QueryWrapper<ReceiveDocDO> query = Wrappers.query();
        query.select("MAX(DOC_SEQUENCE)")
                .eq("DOC_CLASS", docClass)
                .and(wrapper -> wrapper
                        // 情况1：YEAR 字段明确等于传入的年份
                        .eq("YEAR", year)
                        // 情况2：或者 (YEAR 为空 且 RECEIVE_TIME 在该年份范围内)
                        .or(orWrapper -> orWrapper
                                .isNull("YEAR") // 或者使用 .eq("YEAR", "") 取决于你数据库存的是 NULL 还是空字符串
                                .ge("RECEIVE_TIME", startOfYear)
                                .le("RECEIVE_TIME", endOfYear)
                        )
                );

        return  executeMaxQuery(query);
    }

    private boolean isSequenceOccupied(String documentSequence) {
        if (!StringUtils.hasText(documentSequence)) return false;

        LambdaQueryWrapper<ReceiveDocDO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReceiveDocDO::getReceiveDocNumber, documentSequence);

        // selectCount > 0 表示已被占用
        return receiveDocMapper.selectCount(wrapper) > 0;
    }

    private long executeMaxQuery(QueryWrapper<ReceiveDocDO> query) {
        // 使用 selectObjs 获取第一列结果
        List<Object> result = receiveDocMapper.selectObjs(query);

        // 【核心修改】增加为空判断
        if (result != null && !result.isEmpty()) {
            Object maxVal = result.get(0);
            // 数据库如果有记录，maxVal 可能为 Long, Integer, BigDecimal，需安全转换
            if (maxVal != null) {
                return Long.parseLong(maxVal.toString()) + 1;
            }
        }

        // 如果数据库没有查到记录 (List为空 或 maxVal为null)，说明是该年份该文种的第一条
        return 1L;
    }

    @Override
    public void createFlowReceiveDoc(Long userId,ReceiveDocSaveReqVO updateReqVO) {
        // 校验存在
        validateReceiveDocExists(updateReqVO.getId());
        // 更新
        ReceiveDocDO updateObj = BeanUtils.toBean(updateReqVO, ReceiveDocDO.class);

        updateObj.setReceiveDocNumber(null);
        updateObj.setDocSequence(null);
        receiveDocMapper.updateById(updateObj);

        // 更新子表
        updateReceiveDocAttachList(updateReqVO.getId(), updateReqVO.getFileList());

        Map<String, Object> processInstanceVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(updateReqVO.getProcessVariables())) {
            processInstanceVariables.putAll(updateReqVO.getProcessVariables());
        }
        String realKey=PROCESS_KEY;
        if (!StringUtil.isEmpty(updateReqVO.getDocRange())){
            realKey=PROCESS_KEY_CHANGE;
        }
//        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        processInstanceVariables.put(PROCESS_SOURCE_UNIT, updateReqVO.getSendDept());
        String customName =StringUtil.isEmpty(updateReqVO.getSubject()) ? "收文":updateReqVO.getSubject();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(PROCESS_URGENCY_DEGREE, updateReqVO.getUrgencyDegree());
        String timeKey = "receive";
        String timeoutLabel = DictFrameworkUtils.parseDictDataLabel("bpm_process_timeout_config", timeKey);
        if (StrUtil.isNotBlank(timeoutLabel) && NumberUtil.isNumber(timeoutLabel)) {
            // 2. 将字符串转换为整数小时
            int hours = Integer.parseInt(timeoutLabel);

            // 3. 计算截止时间：当前时间 + 小时数
            // 使用 LocalDateTime 计算
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = now.plusHours(hours);

            // 4. 存入流程变量
            // 方式 B：双变量策略（强烈推荐 ★★★）
            // 变量1：存字符串 "24"，用于前端展示 "限时：24小时"
            processInstanceVariables.put(PROCESS_FINISH_TIME, timeoutLabel);

            // 变量2：存具体时间对象 (Date类型)，用于 Flowable 原生查询和后端比对
            // 注意：Flowable 对 java.util.Date 的查询支持最好，建议转为 Date
            processInstanceVariables.put(PROCESS_DEADLINE_DATE, DateUtils.of(deadline));
        }
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, updateReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(realKey)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(updateObj.getId()))
                        .setStartUserSelectAssignees(updateReqVO.getStartUserSelectAssignees()));
        receiveDocMapper.updateById(new ReceiveDocDO().setId(updateObj.getId()).setProcessInstanceId(processInstanceId).setStatus(BpmTaskStatusEnum.RUNNING.getStatus().shortValue()));
    }


    @Override
    public void updateReceiveDoc(ReceiveDocSaveReqVO updateReqVO) {
        // 校验存在
        validateReceiveDocExists(updateReqVO.getId());
        // 更新
        ReceiveDocDO updateObj = BeanUtils.toBean(updateReqVO, ReceiveDocDO.class);

        updateObj.setReceiveDocNumber(null);
        updateObj.setDocSequence(null);
        receiveDocMapper.updateById(updateObj);

        // 更新子表
        updateReceiveDocAttachList(updateReqVO.getId(), updateReqVO.getFileList());
    }

    private void createReceiveDocAttachList(Long receiveDocId, List<ReceiveDocAttachDO> list) {
        list.forEach(o -> o.setReceiveDocId(receiveDocId).clean());
        receiveDocAttachMapper.insertBatch(list);
    }


    private void updateReceiveDocAttachList(Long receiveDocId, List<ReceiveDocAttachDO> list) {
        list.forEach(o -> o.setReceiveDocId(receiveDocId).clean());
        List<ReceiveDocAttachDO> oldList = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);
        List<List<ReceiveDocAttachDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).clean(); // 解决更新情况下：updateTime 不更新
            }
            return same;
        });

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            receiveDocAttachMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            receiveDocAttachMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            receiveDocAttachMapper.deleteByIds(convertList(diffList.get(2), ReceiveDocAttachDO::getId));
        }
    }


    @Override
    public void deleteReceiveDoc(Long id, String reason) {
        // 校验存在
        ReceiveDocDO receiveDoc = receiveDocMapper.selectById(id);
        if (receiveDoc == null) {
            throw exception(RECEIVE_DOC_NOT_EXISTS);
        }
        if (StrUtil.isBlank(receiveDoc.getProcessInstanceId())) {
            ReceiveDocDO updateObj = new ReceiveDocDO()
                    .setId(id)
                    // 注意这里的类型转换，根据你实体类 status 的具体类型(Short/Integer)进行保留
                    .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue())
                    .setCancelReason(reason);
            receiveDocMapper.updateById(updateObj);

        }else{
            Integer currentStatus = receiveDoc.getStatus() != null ? Integer.valueOf(receiveDoc.getStatus()) : null;
            Long userId = getLoginUserId();
            bpmInvalidateHelper.executeInvalidate(
                    userId,
                    receiveDoc.getProcessInstanceId(),
                    currentStatus,
                    reason,
                    () -> {
                        ReceiveDocDO updateObj = new ReceiveDocDO()
                                .setId(id)
                                .setStatus(BpmProcessInstanceStatusEnum.INVALID.getStatus().shortValue()) // 设置为 5(已作废)
                                .setCancelReason(reason); // 写入作废原因
                        receiveDocMapper.updateById(updateObj);
                    }
            );

        }

    }

    @Override
    public void deleteReceiveDocListByIds(List<Long> ids,String reason) {
        // 删除
        for (Long id : ids) {
            deleteReceiveDoc( id, reason);
        }
    }


    private void validateReceiveDocExists(Long id) {
        if (receiveDocMapper.selectById(id) == null) {
            throw exception(RECEIVE_DOC_NOT_EXISTS);
        }
    }

    @Override
    public ReceiveDocDO getReceiveDoc(Long id) {
        return receiveDocMapper.selectById(id);
    }

    @Override
    public PageResult<ReceiveDocDO> getReceiveDocPage(ReceiveDocPageReqVO pageReqVO) {
        return receiveDocMapper.selectPage(pageReqVO);
    }


    @Override
    public List<ReceiveFileRespVO> getReceiveDocAttachListByReceiveDocId(Long receiveDocId) {
        // 1. 查询数据库，获取 DO 列表
        List<ReceiveDocAttachDO> doList = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);

        // 2. 如果为空，直接返回空列表
        if (CollUtil.isEmpty(doList)) {
            return Collections.emptyList();
        }

        // 3. 转换为 VO
        List<ReceiveFileRespVO> voList = BeanUtils.toBean(doList, ReceiveFileRespVO.class);

        // 4. 获取所有文件ID集合
        Set<Long> fileIds = CollectionUtils.convertSet(voList, ReceiveFileRespVO::getAttachFileId);

        Map<Long, String> fileUrlMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(fileIds)) {
            List<FileDO> files = fileMapper.selectBatchIds(fileIds);
            // 提取 ID -> URL 的映射
            Map<Long, FileDO> fileMap = CollectionUtils.convertMap(files, FileDO::getId);

            // 7. 回填 URL 和 Path
            voList.forEach(vo -> {
                FileDO file = fileMap.get(vo.getAttachFileId());
                if (file != null) {
                    vo.setFileUrl(file.getUrl());
                    vo.setPath(file.getPath()); // 塞入 path
                }
            });
        }

        // 5. 回填 URL
//        Map<Long, String> finalFileUrlMap = fileUrlMap; // lambda需要final变量
//        voList.forEach(vo -> vo.setFileUrl(finalFileUrlMap.get(vo.getAttachFileId())));

        return voList;
    }

    public byte[] generatePdf(Long receiveDocId) throws Exception {
        // 1. 获取主表数据
        ReceiveDocDO docData = receiveDocMapper.selectById(receiveDocId);
        if (docData == null) {
            throw new RuntimeException("未能找到对应的收文记录数据");
        }
        // 2. 加载 Word 模板
        try (InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream("templates/YW收文.docx")) {
            if (templateStream == null) {
                throw new RuntimeException("模板文件 YW收文.docx 不存在");
            }

            Document doc = new Document(templateStream);
            DocumentBuilder builder = new DocumentBuilder(doc);
            BookmarkCollection bookmarks = doc.getRange().getBookmarks();

            // 3. 替换单行文本书签
            replaceBookmarkText(bookmarks, "Swh", docData.getReceiveDocNumber());
            String dictUrgencyLabel = DictFrameworkUtils.parseDictDataLabel("emergency_degree", docData.getUrgencyDegree());
            replaceBookmarkText(bookmarks, "cd", StrUtil.isNotBlank(dictUrgencyLabel) ? dictUrgencyLabel : "平急");
            replaceBookmarkText(bookmarks, "bh", docData.getSendDept());
            replaceBookmarkText(bookmarks, "zh", docData.getSendDocNumber());
            replaceBookmarkText(bookmarks, "bt", docData.getSubject());

            if (docData.getReceiveTime() != null) {
                String dateStr = cn.hutool.core.date.DateUtil.format(docData.getReceiveTime(), "yyyy年M月d日");
                replaceBookmarkText(bookmarks, "Rq", dateStr);
            }

            // 4. 获取流程的审批意见
            String processInstanceId = docData.getProcessInstanceId();
            if (StrUtil.isNotBlank(processInstanceId)) {
                fillProcessComments(builder, doc,bookmarks, processInstanceId);
            }

            // 5. 转换为 PDF 字节流输出
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfSaveOptions saveOptions = new PdfSaveOptions();
            // 确保字体完整嵌入
            saveOptions.setEmbedFullFonts(true);
            doc.save(out, saveOptions);

            return out.toByteArray();
        }
    }


    /**
     * 从流程历史提取意见，并根据 taskName 分发到不同的书签
     */
    private void fillProcessComments(DocumentBuilder builder,Document doc, BookmarkCollection bookmarks,
                                     String processInstanceId) throws Exception {

        // 1. 获取该流程实例下的所有历史任务 (true 表示包括全局任务)
        List<HistoricTaskInstance> historyTasks = taskService.getTaskListByProcessInstanceId(processInstanceId, true);
        if (CollUtil.isEmpty(historyTasks)) {
            return;
        }

        // 【优化点 1：批量获取用户信息】提取所有 assignee，批量查 userMap，消除 for 循环查库带来的 N+1 性能瓶颈
        Set<Long> userIds = historyTasks.stream()
                .map(HistoricTaskInstance::getAssignee)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        // 2. 准备各类意见的归集列表
        List<PdfCommentInfo> fgldComments = new ArrayList<>(); // 分管领导意见
        List<PdfCommentInfo> ybzComments = new ArrayList<>();  // 阅办者意见
        List<PdfCommentInfo> ldyjComments = new ArrayList<>(); // 局长/主要领导意见
        PdfCommentInfo lastNbComment = null; // 主任拟办
        PdfCommentInfo lastPsComment = null; // 局长批示

        for (HistoricTaskInstance task : historyTasks) {
            // 跳过未完成的任务 (还在待办中)
//            if (task.getEndTime() == null) {
//                continue;
//            }

            String taskName = task.getName();
            Date endTime = task.getEndTime();

            // 从 userMap 中匹配真实的中文姓名
            String userName = task.getAssignee();
            if (StrUtil.isNotBlank(task.getAssignee())) {
                AdminUserRespDTO user = userMap.get(Long.valueOf(task.getAssignee()));
                if (user != null) {
                    userName = user.getNickname();
                }
            }

            // 【优化点 2：使用框架底层的工具类提取意见】
            String commentText = FlowableUtils.getTaskReason(task);
            if (StrUtil.isBlank(commentText)) {
                commentText = "已阅"; // 默认兜底意见
            }
            PdfCommentInfo commentInfo = new PdfCommentInfo(commentText, userName, endTime);
            // 根据节点名称进行路由分发 (注意判空)
            if (StrUtil.isNotBlank(taskName)) {
                if (taskName.contains("主任") || taskName.contains("拟办")) {
                    lastNbComment = commentInfo;
                }else if (taskName.contains("局长") || taskName.contains("批示")) {
                    lastPsComment = commentInfo;
                }else if (taskName.contains("局领导") || taskName.contains("副局长")) {
                    fgldComments.add(commentInfo);
                } else if (taskName.contains("领导意见")) {
                    ldyjComments.add(commentInfo);
                } else if (taskName.contains("全局阅") || taskName.contains("主办") || taskName.contains("协办")|| taskName.contains("办公室")) {
                    ybzComments.add(commentInfo);
                }
            }
        }

        // 3. 将归集好的意见写入对应书签
        fillDynamicTableRows(builder, doc, "分管领导意见","分管领导","分管领日期", fgldComments);
        fillDynamicTableRows(builder, doc, "ybzyj","ybz","ybzrq", ybzComments);
        fillDynamicTableRows(builder, doc, "ldzyj","ldyj","ldrq", ldyjComments);
        writeSingleComment(bookmarks, "拟办意见", "Nbr", "nbrq", lastNbComment);
        writeSingleComment(bookmarks, "批示意见", "blr", "blrq", lastPsComment);
    }


    private void writeSingleComment(BookmarkCollection bookmarks,
                                    String opinionBm, String nameBm, String dateBm,
                                    PdfCommentInfo comment) throws Exception {
        // 如果没有找到对应的审批记录，直接清空这三个书签的占位符
        if (comment == null) {
            replaceBookmarkText(bookmarks, opinionBm, "");
            replaceBookmarkText(bookmarks, nameBm, "");
            replaceBookmarkText(bookmarks, dateBm, "");
            return;
        }

        // 格式化日期
        String dateStr = "";
        if (comment.getCommentDate() != null) {
            dateStr = cn.hutool.core.date.DateUtil.format(comment.getCommentDate(), "yyyy年M月d日");
        }

        // 依次替换三个书签的内容
        replaceBookmarkText(bookmarks, opinionBm, comment.getCommentDetail());
        replaceBookmarkText(bookmarks, nameBm, comment.getUserName());
        replaceBookmarkText(bookmarks, dateBm, dateStr);
    }

    /**
     * 动态克隆表格行，并填入多条意见
     * * @param builder DocumentBuilder对象
     * @param doc 当前Word文档对象
     * @param opinionBmName 意见对应的书签名称（如："阅办者意见"）
     * @param nameBmName 姓名对应的书签名称（如："阅办者姓名"）
     * @param dateBmName 日期对应的书签名称（如："阅办者日期"）
     * @param comments 流程意见集合
     */
    private void fillDynamicTableRows(DocumentBuilder builder, Document doc,
                                      String opinionBmName, String nameBmName, String dateBmName,
                                      List<PdfCommentInfo> comments) throws Exception {

        Bookmark opinionBookmark = doc.getRange().getBookmarks().get(opinionBmName);
        if (opinionBookmark == null) {
            return; // 模板里没有这个书签，直接跳过
        }

        // 1. 获取基础书签所在的 单元格(Cell) 和 行(Row)
        Cell opinionCell = (Cell) opinionBookmark.getBookmarkStart().getAncestor(NodeType.CELL);
        if (opinionCell == null) return;
        Row baseRow = opinionCell.getParentRow();
        Table table = baseRow.getParentTable();

        // 2. 获取这三个数据项分别在第几列 (利用辅助方法)
        int opinionIdx = getCellIndexByBookmark(doc, opinionBmName);
        int nameIdx = getCellIndexByBookmark(doc, nameBmName);
        int dateIdx = getCellIndexByBookmark(doc, dateBmName);

        // 3. 如果没有任何意见，清空这些单元格的内容即可
        if (CollUtil.isEmpty(comments)) {
            clearCellContent(baseRow, opinionIdx);
            clearCellContent(baseRow, nameIdx);
            clearCellContent(baseRow, dateIdx);
            return;
        }

        // 4. 开始遍历意见，动态生成表格行
        Row currentRow = baseRow;
        for (int i = 0; i < comments.size(); i++) {
            PdfCommentInfo comment = comments.get(i);
            String dateStr = comment.getCommentDate() != null ?
                    cn.hutool.core.date.DateUtil.format(comment.getCommentDate(), "yyyy年M月d日") : "";

            // 如果不是第一条意见，则需要克隆基础行并插入到表格中
            if (i > 0) {
                // deepClone(true) 表示连带单元格里的格式、字体一起克隆
                Row clonedRow = (Row) baseRow.deepClone(true);
                table.insertAfter(clonedRow, currentRow);
                currentRow = clonedRow;
            }

            // 5. 往当前行的指定列中填入文字
            setCellValue(builder, currentRow, opinionIdx, comment.getCommentDetail(),false);
            setCellValue(builder, currentRow, nameIdx, comment.getUserName(),true);
            setCellValue(builder, dateIdx != -1 ? currentRow : null, dateIdx, dateStr,true);
        }
    }

    /**
     * 辅助方法：通过书签名称，寻找它在表格行中的列索引 (Cell Index)
     */
    private int getCellIndexByBookmark(Document doc, String bookmarkName) throws Exception {
        Bookmark bookmark = doc.getRange().getBookmarks().get(bookmarkName);
        if (bookmark != null) {
            Cell cell = (Cell) bookmark.getBookmarkStart().getAncestor(NodeType.CELL);
            if (cell != null) {
                return cell.getParentRow().indexOf(cell);
            }
        }
        return -1; // 找不到返回-1
    }

    /**
     * 辅助方法：清空某个单元格的内容，同时保留单元格结构
     */
    private void clearCellContent(Row row, int cellIndex) {
        if (cellIndex >= 0 && cellIndex < row.getCells().getCount()) {
            Cell cell = row.getCells().get(cellIndex);
            cell.removeAllChildren();
            cell.ensureMinimum(); // 确保单元格内至少有一个空段落，防止Word结构损坏
        }
    }

    /**
     * 辅助方法：利用 DocumentBuilder 向指定单元格安全地写入文字
     */
    private void setCellValue(DocumentBuilder builder, Row row, int cellIndex, String text,boolean isCenter) {
        if (row != null && cellIndex >= 0 && cellIndex < row.getCells().getCount()) {
            Cell cell = row.getCells().get(cellIndex);
            // 写入前先清空单元格里的原内容（比如原来的书签或者占位字）
            cell.removeAllChildren();
            cell.ensureMinimum();
            if (isCenter) {
                // 水平居中
                cell.getFirstParagraph().getParagraphFormat().setAlignment(ParagraphAlignment.CENTER);
                // 垂直居中（让表格看起来更整齐，推荐保留）
                cell.getCellFormat().setVerticalAlignment(CellVerticalAlignment.CENTER);
            } else {
                // 默认左对齐（或根据你模板原本的设置）
                cell.getFirstParagraph().getParagraphFormat().setAlignment(ParagraphAlignment.LEFT);
                cell.getCellFormat().setVerticalAlignment(CellVerticalAlignment.CENTER);
            }

            // 将光标移动到该单元格并写入文本，这样能继承模板原有的字体和字号
            builder.moveTo(cell.getFirstParagraph());
            builder.write(StrUtil.blankToDefault(text, ""));
        }
    }


    /**
     * 辅助方法：安全替换单行书签文本
     */
    private void replaceBookmarkText(BookmarkCollection bookmarks, String bookmarkName, String text) throws Exception {
        Bookmark bookmark = bookmarks.get(bookmarkName);
        if (bookmark != null) {
            bookmark.setText(text == null ? "" : text.trim());
        }
    }

    @Override
    public void updateReceiveStatus(Long id, Integer status) {
        ReceiveDocDO receive = receiveDocMapper.selectById(id);
        if (receive == null) {
            return;
        }
        if (BpmProcessInstanceStatusEnum.INVALID.getStatus().equals(Integer.valueOf(receive.getStatus()))) {
            return;
        }
        // 正常更新状态
        receiveDocMapper.updateById(new ReceiveDocDO().setId(id).setStatus(status.shortValue()));
    }

    @Override
    public Long getPendingCount() {
        return receiveDocMapper.selectCount(
                Wrappers.<ReceiveDocDO>lambdaQuery().eq(ReceiveDocDO::getStatus, BpmTaskStatusEnum.WAIT.getStatus())
        );
    }

    @Override
    public int backfillSourceUnit() {
        List<ReceiveDocDO> list = receiveDocMapper.selectList(
                Wrappers.<ReceiveDocDO>lambdaQuery()
                        .isNotNull(ReceiveDocDO::getProcessInstanceId)
                        .ne(ReceiveDocDO::getProcessInstanceId, "")
                        .isNotNull(ReceiveDocDO::getSendDept)
                        .ne(ReceiveDocDO::getSendDept, "")
        );
        int count = 0;
        for (ReceiveDocDO doc : list) {
            try {
                Object exist = runtimeService.getVariable(doc.getProcessInstanceId(), PROCESS_SOURCE_UNIT);
                if (exist == null) {
                    runtimeService.setVariable(doc.getProcessInstanceId(), PROCESS_SOURCE_UNIT, doc.getSendDept());
                    count++;
                    log.info("补设 PROCESS_SOURCE_UNIT: processInstanceId={}, sendDept={}", doc.getProcessInstanceId(), doc.getSendDept());
                }
            } catch (Exception e) {
                log.warn("补设 PROCESS_SOURCE_UNIT 失败: processInstanceId={}", doc.getProcessInstanceId(), e);
            }
        }
        log.info("补设 PROCESS_SOURCE_UNIT 完成, 总数={}, 修复={}", list.size(), count);
        return count;
    }

}