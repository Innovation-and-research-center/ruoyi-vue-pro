package cn.iocoder.yudao.module.bpm.service.historyworkflow;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.dal.mysql.historyworkflow.HistoryWorkflowMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class HistoryWorkflowServiceImpl implements HistoryWorkflowService {

    @Resource
    private HistoryWorkflowMapper historyWorkflowMapper;

    @Resource
    private FileConfigService fileConfigService;

    @Override
    public Map<String, Object> getHistoryWorkflowDetail(String processInstanceId, String projectId) {
        Map<String, Object> proinst = selectProinst(processInstanceId, projectId);
        if (MapUtil.isEmpty(proinst)) {
            return null;
        }

        String resolvedProjectId = str(proinst.get("projectId"));
        Long proinstId = longValue(proinst.get("proinstIdRaw"));
        String sourceSchema = str(proinst.get("sourceSchema"));
        Map<String, Object> business = resolveBusiness(resolvedProjectId);
        List<Map<String, Object>> records = buildRecords(proinst, proinstId, sourceSchema);
        fillStartUserName(proinst, records);
        List<Map<String, Object>> comments = queryComments(business);
        List<Map<String, Object>> attachments = queryAttachments(business);
        fillAttachmentUrls(business, attachments);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstance", proinst);
        result.put("business", business);
        result.put("records", records);
        result.put("routeRecords", proinstId == null ? new ArrayList<>() : historyWorkflowMapper.selectRouteRecords(proinstId, sourceSchema));
        result.put("comments", comments);
        result.put("attachments", attachments);
        result.put("activityNodes", buildActivityNodes(records));
        return result;
    }

    private Map<String, Object> selectProinst(String processInstanceId, String projectId) {
        if (StrUtil.isNotBlank(projectId)) {
            return historyWorkflowMapper.selectProinstByProjectId(projectId);
        }
        Long proinstId = longValue(processInstanceId);
        if (proinstId == null) {
            return null;
        }
        return historyWorkflowMapper.selectProinstByProinstId(proinstId);
    }

    private void fillStartUserName(Map<String, Object> proinst, List<Map<String, Object>> records) {
        Object startUserName = firstNonBlank(proinst.get("startUserName"), proinst.get("creator"));
        if (startUserName == null) {
            startUserName = records.stream()
                    .filter(record -> !StrUtil.equals("StartEvent", str(record.get("id"))))
                    .map(record -> firstNonBlank(record.get("transactor"), record.get("userName")))
                    .filter(value -> StrUtil.isNotBlank(str(value)))
                    .findFirst()
                    .orElse(null);
        }
        if (startUserName != null) {
            proinst.put("startUserName", startUserName);
            if (CollUtil.isNotEmpty(records)) {
                Map<String, Object> first = records.get(0);
                if (StrUtil.equals("StartEvent", str(first.get("id")))) {
                    Map<String, Object> assigneeUser = new LinkedHashMap<>();
                    assigneeUser.put("nickname", startUserName);
                    first.put("assigneeUser", assigneeUser);
                }
            }
        }
    }

    private Map<String, Object> resolveBusiness(String projectId) {
        if (StrUtil.isBlank(projectId)) {
            return null;
        }
        Map<String, Object> row = firstWithType(historyWorkflowMapper.selectReceiveDoc(projectId),
                "receive_doc", "收文", "bpm/receivedoc/detail.vue");
        if (MapUtil.isNotEmpty(row)) {
            return row;
        }
        row = firstWithType(historyWorkflowMapper.selectLeave(projectId),
                "leave", "请假", "bpm/leave/detail.vue");
        if (MapUtil.isNotEmpty(row)) {
            return row;
        }
        row = firstWithType(historyWorkflowMapper.selectTimeExplain(projectId),
                "time_explain", "公出", "bpm/timeexplain/detail.vue");
        if (MapUtil.isNotEmpty(row)) {
            return row;
        }
        row = firstWithType(historyWorkflowMapper.selectConfflow(projectId),
                "confflow", "会议报告单", "bpm/confflow/detail.vue");
        if (MapUtil.isNotEmpty(row)) {
            return row;
        }
        row = firstWithType(historyWorkflowMapper.selectXzfy(projectId),
                "xzfy", "行政复议", "bpm/xzfy/detail.vue");
        if (MapUtil.isNotEmpty(row)) {
            return row;
        }
        return firstWithType(historyWorkflowMapper.selectXzss(projectId),
                "xzss", "行政诉讼", "bpm/xzss/detail.vue");
    }

    private Map<String, Object> firstWithType(List<Map<String, Object>> rows, String type, String title, String viewPath) {
        if (CollUtil.isEmpty(rows)) {
            return null;
        }
        Map<String, Object> row = new LinkedHashMap<>(rows.get(0));
        row.put("type", type);
        row.put("businessType", type);
        row.put("title", title);
        row.put("viewPath", viewPath);
        row.put("data", new LinkedHashMap<>(rows.get(0)));
        return row;
    }

    private List<Map<String, Object>> buildRecords(Map<String, Object> proinst, Long proinstId, String sourceSchema) {
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(buildStartRecord(proinst));
        if (proinstId == null) {
            return records;
        }
        List<Map<String, Object>> actinstRecords = historyWorkflowMapper.selectActinstRecords(proinstId, sourceSchema);
        if (CollUtil.isNotEmpty(actinstRecords)) {
            records.addAll(actinstRecords);
        }
        return records;
    }

    private Map<String, Object> buildStartRecord(Map<String, Object> proinst) {
        Map<String, Object> start = new LinkedHashMap<>();
        start.put("id", "StartEvent");
        start.put("name", "流程发起");
        start.put("createTime", proinst.get("startTime"));
        start.put("endTime", proinst.get("startTime"));
        start.put("status", 2);
        start.put("durationInMillis", 0L);
        Map<String, Object> assigneeUser = new LinkedHashMap<>();
        assigneeUser.put("nickname", firstNonBlank(proinst.get("startUserName"), proinst.get("intransactor")));
        start.put("assigneeUser", assigneeUser);
        return start;
    }

    private List<Map<String, Object>> buildActivityNodes(List<Map<String, Object>> records) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", record.getOrDefault("actinstId", record.get("routeinstId")));
            node.put("name", firstNonBlank(record.get("name"), record.get("actinstName"), record.get("toActinstName")));
            node.put("status", 2);
            node.put("tasks", buildNodeTasks(record));
            nodes.add(node);
        }
        return nodes;
    }

    private List<Map<String, Object>> buildNodeTasks(Map<String, Object> record) {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("id", record.getOrDefault("actinstId", record.get("routeinstId")));
        task.put("status", 2);
        task.put("reason", firstNonBlank(record.get("comment"), record.get("actinstComment"), record.get("passComment")));
        Map<String, Object> assigneeUser = new LinkedHashMap<>();
        assigneeUser.put("nickname", firstNonBlank(record.get("transactor"), record.get("userName")));
        task.put("assigneeUser", assigneeUser);
        return CollUtil.newArrayList(task);
    }

    private List<Map<String, Object>> queryComments(Map<String, Object> business) {
        String docId = getBusinessDocId(business);
        if (StrUtil.isBlank(docId)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> comments = new ArrayList<>();
        if (!StrUtil.equals("leave", str(business.get("type")))) {
            comments.addAll(historyWorkflowMapper.selectComments(docId, getBusinessDocType(business)));
        }
        appendReceiveDocMainOpinions(business, comments);
        appendLeaveMainOpinions(business, comments);
        appendTimeExplainMainOpinions(business, comments);
        return comments;
    }

    private void appendReceiveDocMainOpinions(Map<String, Object> business, List<Map<String, Object>> comments) {
        if (MapUtil.isEmpty(business) || !StrUtil.equals("receive_doc", str(business.get("type")))) {
            return;
        }
        String docId = str(business.get("id"));
        appendMainOpinion(comments, "DIRECTOR", docId, business.get("directorIdea"),
                business.get("directorName"), business.get("directorDate"));
        appendMainOpinion(comments, "FUGLE", docId, business.get("fugleIdea"),
                business.get("fugleName"), business.get("fugleDate"));
        appendMainOpinion(comments, "DealInfo", docId, business.get("deptDirectorIdea"),
                business.get("deptDirector"), business.get("deptDirectorDate"));
        appendMainOpinion(comments, "DealInfo2", docId, business.get("leaderIdea"),
                business.get("leaderPerson"), business.get("leaderDate"));
    }

    private void appendLeaveMainOpinions(Map<String, Object> business, List<Map<String, Object>> comments) {
        if (MapUtil.isEmpty(business) || !StrUtil.equals("leave", str(business.get("type")))) {
            return;
        }
        String docId = str(business.get("id"));
        appendMainOpinion(comments, "unitOpinion", docId, business.get("unitOpinion"),
                business.get("unitOpinionShr"), business.get("unitOpinionDate"));
        appendMainOpinion(comments, "jbgsOpinion", docId, business.get("jbgsOpinion"),
                business.get("jbgsOpinionShr"), business.get("jbgsOpinionDate"));
        appendMainOpinion(comments, "fjzOpinion", docId, business.get("fjzOpinion"),
                business.get("fjzOpinionShr"), business.get("fjzOpinionDate"));
        appendMainOpinion(comments, "cwfjzOpinion", docId, business.get("cwfjzOpinion"),
                business.get("cwfjzOpinionShr"), business.get("cwfjzOpinionDate"));
        appendMainOpinion(comments, "jzOpinion", docId, business.get("jzOpinion"),
                business.get("jzOpinionShr"), business.get("jzOpinionDate"));
    }

    private void appendTimeExplainMainOpinions(Map<String, Object> business, List<Map<String, Object>> comments) {
        if (MapUtil.isEmpty(business) || !StrUtil.equals("time_explain", str(business.get("type")))) {
            return;
        }
        String docId = str(business.get("id"));
        appendMainOpinion(comments, "deptDirectorIdea", docId, business.get("deptDirectorIdea"),
                business.get("deptDirector"), business.get("deptDirectorDate"));
        appendMainOpinion(comments, "jubDirectorIdea", docId, business.get("jubDirectorIdea"),
                business.get("jubDirector"), business.get("jubDirectorDate"));
        appendMainOpinion(comments, "chargeDirectorIdea", docId, business.get("chargeDirectorIdea"),
                business.get("chargeDirector"), business.get("chargeDirectorDate"));
        appendMainOpinion(comments, "coreDirectorIdea", docId, business.get("coreDirectorIdea"),
                business.get("coreDirector"), business.get("coreDirectorDate"));
        appendMainOpinion(comments, "juzDirectorIdea", docId, business.get("juzDirectorIdea"),
                business.get("juzDirector"), business.get("juzDirectorDate"));
    }

    private void appendMainOpinion(List<Map<String, Object>> comments, String recordName, String docId,
                                   Object detail, Object userName, Object commentDate) {
        if (StrUtil.isBlank(str(detail)) || hasSameComment(comments, recordName, detail, userName, commentDate)) {
            return;
        }
        Map<String, Object> comment = new LinkedHashMap<>();
        comment.put("id", "receive-main-" + recordName + "-" + docId);
        comment.put("formPageName", "ReceiveDoc");
        comment.put("formMultiRecordName", recordName);
        comment.put("docId", docId);
        comment.put("docType", "1");
        comment.put("commentDetail", detail);
        comment.put("commentDate", commentDate);
        comment.put("userName", userName);
        comments.add(comment);
    }

    private boolean hasSameComment(List<Map<String, Object>> comments, String recordName,
                                   Object detail, Object userName, Object commentDate) {
        return comments.stream().anyMatch(comment ->
                StrUtil.equals(recordName, str(comment.get("formMultiRecordName")))
                        && StrUtil.equals(str(detail), str(comment.get("commentDetail")))
                        && StrUtil.equals(str(userName), str(comment.get("userName")))
                        && StrUtil.equals(str(commentDate), str(comment.get("commentDate"))));
    }

    private List<Map<String, Object>> queryAttachments(Map<String, Object> business) {
        if (MapUtil.isEmpty(business)) {
            return new ArrayList<>();
        }
        String type = str(business.get("type"));
        String id = str(business.get("id"));
        String docId = getBusinessDocId(business);
        switch (type) {
            case "receive_doc":
                return historyWorkflowMapper.selectReceiveDocAttachments(id);
            case "leave":
                return historyWorkflowMapper.selectLeaveAttachments(id);
            case "time_explain":
                return historyWorkflowMapper.selectTimeExplainAttachments(id);
            case "confflow":
                return historyWorkflowMapper.selectConfflowAttachments(id, docId);
            case "xzfy":
                return historyWorkflowMapper.selectCommentAttachments(docId, "XZFY");
            case "xzss":
                return historyWorkflowMapper.selectCommentAttachments(docId, "XZSS");
            default:
                return new ArrayList<>();
        }
    }

    private void fillAttachmentUrls(Map<String, Object> business, List<Map<String, Object>> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return;
        }
        String businessType = str(business.get("type"));
        String receiveDocDirectory = str(business.get("attachFilePath"));
        String masterFileDownloadBaseUrl = getMasterFileDownloadBaseUrl();
        for (Map<String, Object> attachment : attachments) {
            String filePath = firstNonBlankString(attachment.get("fileUrl"), attachment.get("filepath"),
                    attachment.get("filePath"));
            if (StrUtil.isBlank(filePath) && StrUtil.equals("receive_doc", businessType)) {
                filePath = joinPath(receiveDocDirectory,
                        firstNonBlankString(attachment.get("filename"), attachment.get("fileName")));
                if (StrUtil.isNotBlank(filePath)) {
                    attachment.put("filePath", filePath);
                }
            }
            if (StrUtil.isBlank(filePath)) {
                continue;
            }
            String normalizedPath = filePath.replace('\\', '/');
            attachment.put("fileUrl", isAbsoluteUrl(normalizedPath)
                    ? normalizedPath : joinPath(masterFileDownloadBaseUrl, normalizedPath));
        }
    }

    private String getMasterFileDownloadBaseUrl() {
        FileConfigDO masterConfig = fileConfigService.getMasterFileConfig();
        if (masterConfig == null || masterConfig.getConfig() == null) {
            return "";
        }
        String domain = str(BeanUtil.getFieldValue(masterConfig.getConfig(), "domain"));
        if (StrUtil.isBlank(domain)) {
            return "";
        }
        return StrUtil.format("{}/admin-api/infra/file/{}/get",
                StrUtil.removeSuffix(domain, "/"), masterConfig.getId());
    }

    private String firstNonBlankString(Object... values) {
        return str(firstNonBlank(values));
    }

    private boolean isAbsoluteUrl(String path) {
        return StrUtil.startWithIgnoreCase(path, "http://") || StrUtil.startWithIgnoreCase(path, "https://");
    }

    private String joinPath(String prefix, String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String normalizedPath = path.replace('\\', '/');
        if (StrUtil.isBlank(prefix)) {
            return normalizedPath;
        }
        String normalizedPrefix = prefix.replace('\\', '/');
        return StrUtil.removeSuffix(normalizedPrefix, "/") + "/" + StrUtil.removePrefix(normalizedPath, "/");
    }

    private String getBusinessDocId(Map<String, Object> business) {
        if (MapUtil.isEmpty(business)) {
            return null;
        }
        String type = str(business.get("type"));
        if (StrUtil.equalsAny(type, "xzfy", "xzss")) {
            return str(business.get("xmGuid"));
        }
        if (StrUtil.equals(type, "confflow")) {
            return str(business.get("docGuid"));
        }
        return str(business.get("id"));
    }

    private String getBusinessDocType(Map<String, Object> business) {
        if (MapUtil.isEmpty(business)) {
            return null;
        }
        String type = str(business.get("type"));
        switch (type) {
            case "receive_doc":
                return "1";
            case "confflow":
                return "ywbgd";
            case "xzfy":
                return "xzfy";
            case "xzss":
                return "xzss";
            default:
                return null;
        }
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (StrUtil.isNotBlank(str(value))) {
                return value;
            }
        }
        return null;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        String text = str(value);
        if (StrUtil.isBlank(text) || !text.matches("\\d+")) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

}
