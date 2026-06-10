package cn.iocoder.yudao.module.bpm.service.process;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.PdfCommentInfo;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessPrintDataRespVO;
import cn.iocoder.yudao.module.bpm.convert.task.BpmProcessInstanceConvert;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.framework.print.BpmProcessPrintDataFactory;
import cn.iocoder.yudao.module.bpm.framework.print.BpmProcessPrintDataHandler;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.aspose.words.Bookmark;
import com.aspose.words.Cell;
import com.aspose.words.CellVerticalAlignment;
import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.NodeType;
import com.aspose.words.Paragraph;
import com.aspose.words.ParagraphAlignment;
import com.aspose.words.Row;
import com.aspose.words.SaveFormat;
import com.aspose.words.Table;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.PROCESS_INSTANCE_NOT_EXISTS;

@Service
@Validated
public class BpmProcessWordServiceImpl implements BpmProcessWordService {

    @Resource
    private BpmProcessInstanceService processInstanceService;
    @Resource
    private BpmTaskService taskService;
    @Resource
    private BpmProcessDefinitionService processDefinitionService;
    @Resource
    private BpmProcessPrintDataFactory processPrintDataFactory;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;

    // ==================== 流程 Key → 模板路径 ====================

    private static final Map<String, String> TEMPLATE_MAP = new LinkedHashMap<>();

    static {
        TEMPLATE_MAP.put("receice_doc_v2_copy_copy", "templates/YW收文.docx");
        TEMPLATE_MAP.put("oa_leave", "templates/YW请假单_qj.docx");
        TEMPLATE_MAP.put("conference_report", "templates/YW会议报告单.docx");
        TEMPLATE_MAP.put("oa_out", "templates/YW请假单_gc.docx");
    }

    // ==================== 书签 → DO字段名 映射 ====================

    /** 收文: YW收文.docx 书签 → ReceiveDocDO 字段 */
    private static final Map<String, String> RECEIVE_BOOKMARK = new LinkedHashMap<>();
    static {
        RECEIVE_BOOKMARK.put("Swh", "receiveDocNumber");
        RECEIVE_BOOKMARK.put("cd", "urgencyDegree");
        RECEIVE_BOOKMARK.put("bh", "sendDept");
        RECEIVE_BOOKMARK.put("zh", "sendDocNumber");
        RECEIVE_BOOKMARK.put("bt", "subject");
        RECEIVE_BOOKMARK.put("Rq", "receiveTime");
    }

    /** 会议报告单: YW会议报告单.docx 书签 → ConfflowDO 字段 */
    private static final Map<String, String> CONFFLOW_BOOKMARK = new LinkedHashMap<>();
    static {
        CONFFLOW_BOOKMARK.put("title", "title");
        CONFFLOW_BOOKMARK.put("start_date", "startDate");
        CONFFLOW_BOOKMARK.put("venue", "venue");
        CONFFLOW_BOOKMARK.put("join_unit", "joinUnit");
        CONFFLOW_BOOKMARK.put("offer_unit", "offerUnit");
        CONFFLOW_BOOKMARK.put("offer_person", "offerPerson");
        CONFFLOW_BOOKMARK.put("situation", "situation");
        CONFFLOW_BOOKMARK.put("content", "content");
    }

    /** 请假: YW请假单_qj.docx 书签 → LeaveDO 字段 */
    private static final Map<String, String> LEAVE_BOOKMARK = new LinkedHashMap<>();
    static {
        LEAVE_BOOKMARK.put("sqrq", "applyDate");
        LEAVE_BOOKMARK.put("qjlx", "qxjType");
        LEAVE_BOOKMARK.put("qjsy", "sjReason");
        LEAVE_BOOKMARK.put("qjts", "totalTs");
        LEAVE_BOOKMARK.put("ksrq", "qxjStartDate");
        LEAVE_BOOKMARK.put("jsrq", "qxjEndDate");
    }

    /** 外出: YW请假单_gc.docx 书签 → TimeExplainDO 字段 */
    private static final Map<String, String> OUT_BOOKMARK = new LinkedHashMap<>();
    static {
        OUT_BOOKMARK.put("sqr", "userName");
        // szks（所在科室）由 fillProcessInstanceInfo 从流程上下文填入，不走 DO 字段
        OUT_BOOKMARK.put("ksrq", "checkBegin");
        OUT_BOOKMARK.put("jsrq", "checkEnd");
        OUT_BOOKMARK.put("mdd", "endPlace");
        OUT_BOOKMARK.put("cfd", "startPlace");
        OUT_BOOKMARK.put("gcsy", "reason");
        OUT_BOOKMARK.put("gcts", "days");
    }

    /** 流程 Key → 书签映射 */
    private static final Map<String, Map<String, String>> BOOKMARK_MAPPING = new HashMap<>();
    static {
        BOOKMARK_MAPPING.put("receice_doc_v2_copy_copy", RECEIVE_BOOKMARK);
        BOOKMARK_MAPPING.put("conference_report", CONFFLOW_BOOKMARK);
        BOOKMARK_MAPPING.put("oa_leave", LEAVE_BOOKMARK);
        BOOKMARK_MAPPING.put("oa_out", OUT_BOOKMARK);
    }

    // ==================== 任务定义 Key → 分类 映射（参照手机端 ZGJOA-mobile）====================

    private enum CommentCategory {
        NIBAN, PISHI, DEPT_HEAD, OFFICE, DEPT_VICE_LEADER, VICE_LEADER, MAIN_LEADER, LEADER_OPINION, REVIEWER
    }

    /** 流程 Key → (taskDefinitionKey → 分类) */
    private static final Map<String, Map<String, CommentCategory>> TASK_KEY_MAPPING = new HashMap<>();

    static {
        // 收文
        Map<String, CommentCategory> receiveMap = new LinkedHashMap<>();
        receiveMap.put("Activity_1m15g69", CommentCategory.NIBAN);
        receiveMap.put("Activity_1iehl4q", CommentCategory.PISHI);
        receiveMap.put("Activity_1bm8630", CommentCategory.VICE_LEADER);
        receiveMap.put("Activity_093fgmu", CommentCategory.REVIEWER);
        receiveMap.put("Activity_06khbtu", CommentCategory.LEADER_OPINION);
        TASK_KEY_MAPPING.put("receice_doc_v2_copy_copy", receiveMap);

        // 会议报告单
        Map<String, CommentCategory> confflowMap = new LinkedHashMap<>();
        confflowMap.put("Activity_1h0l7v8", CommentCategory.DEPT_HEAD);
        confflowMap.put("Activity_0zb91fz", CommentCategory.DEPT_VICE_LEADER);
        confflowMap.put("Activity_0v3nk6f", CommentCategory.VICE_LEADER);
        confflowMap.put("Activity_1q3gsuj", CommentCategory.MAIN_LEADER);
        confflowMap.put("Activity_0q3ei99", CommentCategory.REVIEWER);
        TASK_KEY_MAPPING.put("conference_report", confflowMap);

        // 请假
        Map<String, CommentCategory> leaveMap = new LinkedHashMap<>();
        leaveMap.put("Activity_1s93b00", CommentCategory.DEPT_HEAD);
        leaveMap.put("Activity_1rwud4u", CommentCategory.OFFICE);
        leaveMap.put("Activity_1dmw74i", CommentCategory.VICE_LEADER);
        leaveMap.put("Activity_093fgmu", CommentCategory.MAIN_LEADER);
        TASK_KEY_MAPPING.put("oa_leave", leaveMap);

        // 外出
        Map<String, CommentCategory> outMap = new LinkedHashMap<>();
        outMap.put("Activity_0578ggz", CommentCategory.DEPT_HEAD);
        outMap.put("Activity_0j6zjr4", CommentCategory.VICE_LEADER);
        outMap.put("Activity_0nnqstj", CommentCategory.MAIN_LEADER);
        TASK_KEY_MAPPING.put("oa_out", outMap);
    }

    private static final String DATE_FORMAT = "yyyy年MM月dd日";

    @Override
    public byte[] generateWord(String processInstanceId) throws Exception {
        Document doc =

                fillDocument(processInstanceId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out, SaveFormat.DOCX);
        return out.toByteArray();
    }

    @Override
    public String generateHtml(String processInstanceId) throws Exception {
        Document doc = fillDocument(processInstanceId);
        com.aspose.words.HtmlSaveOptions options = new com.aspose.words.HtmlSaveOptions();
        options.setExportImagesAsBase64(true);
        options.setPrettyFormat(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out, options);
        return out.toString("UTF-8");
    }

    /** 加载模板、填充数据，返回已填充的 Document */
    private Document fillDocument(String processInstanceId) throws Exception {
        HistoricProcessInstance historicProcessInstance = processInstanceService.getHistoricProcessInstance(processInstanceId);
        if (historicProcessInstance == null) {
            throw exception(PROCESS_INSTANCE_NOT_EXISTS);
        }

        AdminUserRespDTO startUser = adminUserApi.getUser(Long.valueOf(historicProcessInstance.getStartUserId()));
        if (startUser == null) startUser = new AdminUserRespDTO();
        DeptRespDTO dept = startUser.getDeptId() != null ? deptApi.getDept(startUser.getDeptId()) : null;
        if (dept == null) dept = new DeptRespDTO();
        List<HistoricTaskInstance> tasks = taskService.getFinishedTaskListByProcessInstanceIdWithoutCancel(processInstanceId);
        Set<Long> assigneeIds = tasks.stream()
                .map(HistoricTaskInstance::getAssignee)
                .filter(StrUtil::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(assigneeIds);

        BpmProcessDefinitionInfoDO processDefinitionInfo = processDefinitionService
                .getProcessDefinitionInfo(historicProcessInstance.getProcessDefinitionId());

        BpmProcessPrintDataRespVO printData = BpmProcessInstanceConvert.INSTANCE.buildProcessInstancePrintData(
                historicProcessInstance, processDefinitionInfo, tasks, userMap,
                new UserSimpleBaseVO().setNickname(startUser.getNickname()).setDeptName(dept.getName()));

        String processDefinitionKey = historicProcessInstance.getProcessDefinitionKey();
        BpmProcessPrintDataHandler handler = processPrintDataFactory.getHandler(processDefinitionKey);
        Map<String, Object> businessData = null;
        if (handler != null) {
            String businessKey = historicProcessInstance.getBusinessKey();
            if (businessKey != null) {
                businessData = handler.getPrintData(processInstanceId, businessKey);
            }
        }
        // 字典字段转换（如紧急程度 dict value → label）
        processDictFields(businessData);

        String templatePath = TEMPLATE_MAP.get(processDefinitionKey);
        if (templatePath == null) {
            throw new RuntimeException("未找到流程定义 [" + processDefinitionKey + "] 对应的 Word 模板");
        }

        InputStream templateStream = this.getClass().getClassLoader().getResourceAsStream(templatePath);
        if (templateStream == null) {
            throw new RuntimeException("模板文件 " + templatePath + " 不存在");
        }

        Document doc = new Document(templateStream);

        doc.setWarningCallback(warningInfo -> {
            if (warningInfo.getWarningType() == com.aspose.words.WarningType.FONT_SUBSTITUTION) {
                System.out.println("⚠️ 触发字体替换: " + warningInfo.getDescription());
            }
        });
        templateStream.close();

        DocumentBuilder builder = new DocumentBuilder(doc);

        fillProcessInstanceInfo(builder, historicProcessInstance, startUser, dept);
        if (businessData == null) {
            businessData = new HashMap<>();
        }
        fillBusinessData(builder, businessData, processDefinitionKey);
        fillApprovalComments(builder, doc, printData, processDefinitionKey);

        return doc;
    }

    // ==================== 字典处理 ====================

    /** 将 businessData 中的字典值转换为 label，并为时段书签补充数据 */
    private void processDictFields(Map<String, Object> businessData) {
        if (businessData == null) return;
        // 紧急程度
        Object urgency = businessData.get("urgencyDegree");
        if (urgency != null) {
            String label = DictFrameworkUtils.parseDictDataLabel("emergency_degree", String.valueOf(urgency));
            if (StrUtil.isNotBlank(label)) {
                businessData.put("urgencyDegree", label);
            }
        }
        // 请假/外出 开始时段 & 结束时段（kssd / jssd）
        addPeriodField(businessData, "qxjStartDate", "kssd");
        addPeriodField(businessData, "qxjEndDate", "jssd");
        addPeriodField(businessData, "checkBegin", "kssd");
        addPeriodField(businessData, "checkEnd", "jssd");
    }

    private void addPeriodField(Map<String, Object> data, String dateField, String periodField) {
        Object dateObj = data.get(dateField);
        if (dateObj != null && !data.containsKey(periodField)) {
            int hour = -1;
            if (dateObj instanceof LocalDateTime) {
                hour = ((LocalDateTime) dateObj).getHour();
            } else if (dateObj instanceof Number) {
                hour = new java.util.Date(((Number) dateObj).longValue()).getHours();
            } else if (dateObj instanceof String) {
                String str = (String) dateObj;
                try {
                    if (str.matches("\\d+")) { // 时间戳字符串如 "1716361395000"
                        long ts = Long.parseLong(str);
                        if (str.length() == 10) ts *= 1000;
                        hour = new java.util.Date(ts).getHours();
                    } else {
                        hour = cn.hutool.core.date.DateUtil.parseLocalDateTime(str).getHour();
                    }
                } catch (Exception ignored) {}
            } else if (dateObj instanceof List && ((List<?>) dateObj).size() >= 4) {
                hour = ((Number) ((List<?>) dateObj).get(3)).intValue();
            }
            if (hour >= 0) {
                data.put(periodField, hour < 12 ? "上午" : "下午");
            }
        }
    }

    // ==================== 模板填充 ====================

    private void fillProcessInstanceInfo(DocumentBuilder builder,
                                          HistoricProcessInstance processInstance,
                                          AdminUserRespDTO startUser,
                                          DeptRespDTO dept) throws Exception {
        replaceBookmarkText(builder, "流程名称", processInstance.getName());
        replaceBookmarkText(builder, "流程编号", processInstance.getId());
        replaceBookmarkText(builder, "发起人", startUser.getNickname());
        replaceBookmarkText(builder, "发起部门", dept.getName());
        if (processInstance.getStartTime() != null) {
            replaceBookmarkText(builder, "发起时间",
                    cn.hutool.core.date.DateUtil.format(processInstance.getStartTime(), DATE_FORMAT));
        }
        if (processInstance.getEndTime() != null) {
            replaceBookmarkText(builder, "结束时间",
                    cn.hutool.core.date.DateUtil.format(processInstance.getEndTime(), DATE_FORMAT));
        }
        replaceBookmarkText(builder, "sqr", startUser.getNickname());
        replaceBookmarkText(builder, "szks", dept.getName());
    }

    private void fillBusinessData(DocumentBuilder builder, Map<String, Object> businessData,
                                   String processDefinitionKey) throws Exception {
        Map<String, String> fieldMapping = BOOKMARK_MAPPING.get(processDefinitionKey);

        if (fieldMapping != null) {
            for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
                String bookmarkName = entry.getKey();
                String fieldName = entry.getValue();
                Object value = businessData.get(fieldName);
                String text = value != null ? formatValue(value) : "";
                replaceBookmarkText(builder, bookmarkName, text);
            }
        }

        for (Map.Entry<String, Object> entry : businessData.entrySet()) {
            Object value = entry.getValue();
            String text = value != null ? formatValue(value) : "";
            replaceBookmarkText(builder, entry.getKey(), text);
        }
    }

    /**
     * 按任务名称分类填充审批意见到对应书签区域。
     * 不同模板使用不同的书签命名后缀（_idea / _detail / 中文），
     * 通过尝试多种模式来兼容。
     */
    /** 按 taskDefinitionKey（手机端同款）分类，taskName 兜底 */
    private void fillApprovalComments(DocumentBuilder builder, Document doc,
                                       BpmProcessPrintDataRespVO printData,
                                       String processDefinitionKey) throws Exception {
        List<BpmProcessPrintDataRespVO.Task> taskList =
                (printData != null && printData.getTasks() != null) ? printData.getTasks() : new ArrayList<>();

        Map<String, CommentCategory> keyMapping = TASK_KEY_MAPPING.get(processDefinitionKey);

        // 分类
        List<PdfCommentInfo> deptHeadComments = new ArrayList<>();
        List<PdfCommentInfo> deptViceLeaderComments = new ArrayList<>();
        List<PdfCommentInfo> viceLeaderComments = new ArrayList<>();
        List<PdfCommentInfo> mainLeaderComments = new ArrayList<>();
        List<PdfCommentInfo> leaderOpinionComments = new ArrayList<>();
        List<PdfCommentInfo> readerComments = new ArrayList<>();
        List<PdfCommentInfo> officeComments = new ArrayList<>();
        List<PdfCommentInfo> otherComments = new ArrayList<>();
        PdfCommentInfo nibanComment = null;
        PdfCommentInfo pishiComment = null;

        for (BpmProcessPrintDataRespVO.Task task : taskList) {
            String userName = StrUtil.blankToDefault(task.getApproveName(), "");
            String commentText = StrUtil.blankToDefault(task.getContent(), "已阅");
            Date commentDate = parseDate(task.getApproveDate());
            PdfCommentInfo info = new PdfCommentInfo(commentText, userName, commentDate);

            // 优先用 taskDefinitionKey 匹配
            CommentCategory category = (keyMapping != null && task.getTaskDefinitionKey() != null)
                    ? keyMapping.get(task.getTaskDefinitionKey()) : null;

            if (category != null) {
                switch (category) {
                    case NIBAN: nibanComment = info; break;
                    case PISHI: pishiComment = info; break;
                    case DEPT_HEAD: deptHeadComments.add(info); break;
                    case DEPT_VICE_LEADER: deptViceLeaderComments.add(info); break;
                    case VICE_LEADER: viceLeaderComments.add(info); break;
                    case MAIN_LEADER: mainLeaderComments.add(info); break;
                    case LEADER_OPINION: leaderOpinionComments.add(info); break;
                    case REVIEWER: readerComments.add(info); break;
                    case OFFICE: officeComments.add(info); break;
                }
            } else {
                // 兜底：按 taskName 匹配
                String taskName = StrUtil.blankToDefault(task.getName(), "");
                if (taskName.contains("拟办") || taskName.equals("主任拟办")) {
                    if (nibanComment == null) nibanComment = info;
                } else if (taskName.contains("批示")) {
                    if (pishiComment == null) pishiComment = info;
                } else if (taskName.contains("办公室")) {
                    officeComments.add(info);
                } else if (taskName.contains("科室") && taskName.contains("负责人")) {
                    deptHeadComments.add(info);
                } else if (taskName.contains("分管领导") || taskName.contains("局领导") || taskName.contains("副局长")) {
                    viceLeaderComments.add(info);
                } else if (taskName.contains("主要领导") || taskName.contains("局长")) {
                    mainLeaderComments.add(info);
                } else if (taskName.contains("领导意见")) {
                    leaderOpinionComments.add(info);
                } else if (taskName.contains("全局阅") || taskName.contains("主办") || taskName.contains("协办")) {
                    readerComments.add(info);
                } else {
                    otherComments.add(info);
                }
            }
        }

        // ---------- 填充书签 ----------
        writeSingleComment(builder, "拟办意见", "Nbr", "nbrq", nibanComment);
        writeSingleComment(builder, "批示意见", "blr", "blrq", pishiComment);

        // 科室分管领导合并到分管领导
        viceLeaderComments.addAll(0, deptViceLeaderComments);

        // ====== 独立书签（_detail/_user/_date 或 _idea/_user/_date 在不同行，用 writeSingleComment） ======
        writeSingleFromList(builder, "ksfzr_idea", "ksfzr_user", "ksfzr_date", deptHeadComments);
        writeSingleFromList(builder, "tybm_detail", "tybm_user", "tybm_date", deptHeadComments);
        writeSingleFromList(builder, "rjk_idea", "rjk_user", "rjk_date", officeComments);
        writeSingleFromList(builder, "fgld_idea", "fgld_user", "fgld_date", viceLeaderComments);
        writeSingleFromList(builder, "fgld_detail", "fgld_user", "fgld_date", viceLeaderComments);
        writeSingleFromList(builder, "zyld_idea", "zyld_user", "zyld_date", mainLeaderComments);
        writeSingleFromList(builder, "zgld_detail", "zgld_user", "zgld_date", mainLeaderComments);

        // ====== 同行动态表格书签（意见/人名/日期在同一行，可多行克隆） ======
        fillDynamicTableRows(builder, doc, "分管领导意见", "分管领导", "分管领日期", viceLeaderComments);
        fillDynamicTableRows(builder, doc, "ldzyj", "ldyj", "ldrq", leaderOpinionComments);
        fillDynamicTableRows(builder, doc, "ybzyj", "ybz", "ybzrq", readerComments);
        fillDynamicTableRows(builder, doc, "审批意见", "审批人", "审批日期", readerComments);

        if (!otherComments.isEmpty()) {
            fillDynamicTableRows(builder, doc, "审批意见", "审批人", "审批日期", otherComments);
        }
    }

    /** 填充独立书签（不在同一行），取列表第一条 */
    private void writeSingleFromList(DocumentBuilder builder,
                                      String detailBm, String userBm, String dateBm,
                                      List<PdfCommentInfo> comments) throws Exception {
        PdfCommentInfo first = CollUtil.isEmpty(comments) ? null : comments.get(0);
        writeSingleComment(builder, detailBm, userBm, dateBm, first);
    }

    // ==================== Aspose.Words 辅助 ====================

    private String formatValue(Object value) {
        if (value instanceof LocalDateTime) {
            return cn.hutool.core.date.DateUtil.format((LocalDateTime) value, DATE_FORMAT);
        }
        if (value instanceof Date) {
            return cn.hutool.core.date.DateUtil.format((Date) value, DATE_FORMAT);
        }
        // Number 类型时间戳（毫秒或秒级），通过数值大小区分是否为时间戳
        if (value instanceof Number) {
            long num = ((Number) value).longValue();
            if (num > 100000000000L) { // 13位毫秒级
                return cn.hutool.core.date.DateUtil.format(new Date(num), DATE_FORMAT);
            }
            if (String.valueOf(num).length() == 10 && num > 1000000000L) { // 10位秒级
                return cn.hutool.core.date.DateUtil.format(new Date(num * 1000), DATE_FORMAT);
            }
            return value.toString();
        }
        // String 类型：仅纯数字时间戳才转日期，其余保持原文
        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.matches("^\\d{10}$|^\\d{13}$")) {
                try {
                    long timestamp = Long.parseLong(strVal);
                    if (strVal.length() == 10) timestamp *= 1000;
                    return cn.hutool.core.date.DateUtil.format(new Date(timestamp), DATE_FORMAT);
                } catch (Exception ignored) {
                }
            }
            return strVal;
        }
        // Jackson 可能将 LocalDateTime 转为 List [2026, 5, 22, ...]
        if (value instanceof List && ((List<?>) value).size() >= 3) {
            List<?> list = (List<?>) value;
            try {
                int year = ((Number) list.get(0)).intValue();
                int month = ((Number) list.get(1)).intValue();
                int day = ((Number) list.get(2)).intValue();
                return cn.hutool.core.date.DateUtil.format(
                        LocalDateTime.of(year, month, day, 0, 0), DATE_FORMAT);
            } catch (Exception ignored) {
            }
        }
        return value.toString();
    }

    private Date parseDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        try {
            return cn.hutool.core.date.DateUtil.parse(dateStr);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 使用 DocumentBuilder 替换书签文本，保留模板字体格式 */
    private void replaceBookmarkText(DocumentBuilder builder, String bookmarkName, String text) throws Exception {
        Bookmark bookmark = builder.getDocument().getRange().getBookmarks().get(bookmarkName);
        if (bookmark == null) return;
        bookmark.setText(""); // 先清空原书签占位文字
        if (builder.moveToBookmark(bookmarkName)) {
            builder.getFont().clearFormatting();
            builder.getFont().setName("宋体");
            builder.getFont().setSize(12.0);
            builder.write(text == null ? "" : text);
        }
    }

    private void writeSingleComment(DocumentBuilder builder,
                                     String opinionBm, String nameBm, String dateBm,
                                     PdfCommentInfo comment) throws Exception {
        if (comment == null) {
            replaceBookmarkText(builder, opinionBm, "");
            replaceBookmarkText(builder, nameBm, "");
            replaceBookmarkText(builder, dateBm, "");
            return;
        }
        String dateStr = "";
        if (comment.getCommentDate() != null) {
            dateStr = cn.hutool.core.date.DateUtil.format(comment.getCommentDate(), DATE_FORMAT);
        }
        replaceBookmarkText(builder, opinionBm, comment.getCommentDetail());
        replaceBookmarkText(builder, nameBm, comment.getUserName());
        replaceBookmarkText(builder, dateBm, dateStr);
    }

    private void fillDynamicTableRows(DocumentBuilder builder, Document doc,
                                       String opinionBmName, String nameBmName, String dateBmName,
                                       List<PdfCommentInfo> comments) throws Exception {
        Bookmark opinionBookmark = doc.getRange().getBookmarks().get(opinionBmName);
        if (opinionBookmark == null) {
            return;
        }

        Cell opinionCell = (Cell) opinionBookmark.getBookmarkStart().getAncestor(NodeType.CELL);
        if (opinionCell == null) return;
        Row baseRow = opinionCell.getParentRow();
        Table table = baseRow.getParentTable();

        int opinionIdx = getCellIndexByBookmark(doc, opinionBmName);
        int nameIdx = getCellIndexByBookmark(doc, nameBmName);
        int dateIdx = getCellIndexByBookmark(doc, dateBmName);

        if (CollUtil.isEmpty(comments)) {
            clearCellContent(baseRow, opinionIdx);
            clearCellContent(baseRow, nameIdx);
            clearCellContent(baseRow, dateIdx);
            return;
        }

        Row currentRow = baseRow;
        for (int i = 0; i < comments.size(); i++) {
            PdfCommentInfo comment = comments.get(i);
            String dateStr = comment.getCommentDate() != null
                    ? cn.hutool.core.date.DateUtil.format(comment.getCommentDate(), DATE_FORMAT) : "";

            if (i > 0) {
                Row clonedRow = (Row) baseRow.deepClone(true);
                table.insertAfter(clonedRow, currentRow);
                currentRow = clonedRow;
            }

            setCellValue(builder, currentRow, opinionIdx, comment.getCommentDetail(), true);
            setCellValue(builder, currentRow, nameIdx, comment.getUserName(), true);
            setCellValue(builder, dateIdx != -1 ? currentRow : null, dateIdx, dateStr, true);
        }
    }

    private int getCellIndexByBookmark(Document doc, String bookmarkName) throws Exception {
        Bookmark bookmark = doc.getRange().getBookmarks().get(bookmarkName);
        if (bookmark != null) {
            Cell cell = (Cell) bookmark.getBookmarkStart().getAncestor(NodeType.CELL);
            if (cell != null) {
                return cell.getParentRow().indexOf(cell);
            }
        }
        return -1;
    }

    private void clearCellContent(Row row, int cellIndex) {
        if (cellIndex >= 0 && cellIndex < row.getCells().getCount()) {
            Cell cell = row.getCells().get(cellIndex);
            cell.removeAllChildren();
            cell.ensureMinimum();
        }
    }

    private void setCellValue(DocumentBuilder builder, Row row, int cellIndex, String text, boolean isCenter) {
        if (row != null && cellIndex >= 0 && cellIndex < row.getCells().getCount()) {
            Cell cell = row.getCells().get(cellIndex);
            // 保留段落样式，只清空文本内容
            Paragraph p = cell.getFirstParagraph();
            if (p != null) {
                p.getRuns().clear();
            } else {
                cell.ensureMinimum();
            }
            if (isCenter) {
                cell.getFirstParagraph().getParagraphFormat().setAlignment(ParagraphAlignment.CENTER);
                cell.getCellFormat().setVerticalAlignment(CellVerticalAlignment.CENTER);
            } else {
                cell.getFirstParagraph().getParagraphFormat().setAlignment(ParagraphAlignment.LEFT);
                cell.getCellFormat().setVerticalAlignment(CellVerticalAlignment.CENTER);
            }
            builder.moveTo(cell.getFirstParagraph());
            builder.getFont().clearFormatting();
            builder.getFont().setName("宋体");
            builder.getFont().setSize(12.0);
            builder.write(StrUtil.blankToDefault(text, ""));
        }
    }
}
