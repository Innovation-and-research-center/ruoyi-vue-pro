package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.bpm.controller.admin.confflow.vo.ConfflowSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowAttachDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.confflow.ConfflowDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.confflow.ConfflowMapper;
import cn.iocoder.yudao.module.bpm.service.confflow.ConfflowService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Resource;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MeetingDocJob implements JobHandler {

    static final String MEETING_WSDL_KEY = "url.meeting.wsdl";
    static final String MEETING_LIST_PARAM_KEY = "key.meeting.natural";
    static final String RECEIVE_UUID_KEY = "key.receive.natural";
    static final String DEFAULT_USER_ID = "key.receive.user";
    static final String SOAP_PARAM_NAME_KEY = "key.meeting.soap.param";

    private static final String DOC_TYPE = "ywbgd";
    private static final int TIMEOUT = 30000;
    private static final ConcurrentHashMap<String, String> NAMESPACE_CACHE = new ConcurrentHashMap<>();

    @Resource
    private ConfigApi configApi;
    @Resource
    private ConfflowService confflowService;
    @Resource
    private ConfflowMapper confflowMapper;
    @Resource
    private ConfflowAttachMapper confflowAttachMapper;
    @Resource
    private FileService fileService;
    @Resource
    private TransactionTemplate transactionTemplate;

    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过会议报告单同步", currentTenantId);
            return "跳过非目标租户";
        }

        String wsdl = configApi.getConfigValueByKey(MEETING_WSDL_KEY);
        if (StrUtil.isBlank(wsdl)) {
            log.warn("【会议报告单】未配置 {}", MEETING_WSDL_KEY);
            return "未配置会议接口地址";
        }

        String listParam = configApi.getConfigValueByKey(MEETING_LIST_PARAM_KEY);
        if (StrUtil.isBlank(listParam)) {
            listParam = configApi.getConfigValueByKey(RECEIVE_UUID_KEY);
        }
        if (StrUtil.isBlank(listParam)) {
            log.warn("【会议报告单】未配置 {} 或 {}", MEETING_LIST_PARAM_KEY, RECEIVE_UUID_KEY);
            return "未配置会议列表参数";
        }
        String userIdConfig = configApi.getConfigValueByKey(DEFAULT_USER_ID);
        if (StrUtil.isBlank(userIdConfig)) {
            log.warn("【会议报告单】未配置 {}", DEFAULT_USER_ID);
            return "未配置默认处理人";
        }
        final Long userId;
        try {
            userId = Long.valueOf(userIdConfig);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("默认处理人配置不是有效数字: " + userIdConfig, e);
        }

        int totalCount = 0;
        int successCount = 0;
        try {
            List<String> uuids = getMeetingUuidList(wsdl, listParam);
            totalCount = uuids.size();
            for (String uuid : uuids) {
                try {
                    Boolean synced = transactionTemplate.execute(
                            status -> syncSingleMeeting(wsdl, uuid, userId));
                    if (Boolean.TRUE.equals(synced)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("【会议报告单】同步单条失败, uuid: {}", uuid, e);
                }
            }
            log.info("【会议报告单】同步完成，总数：{}，成功：{}", totalCount, successCount);
            return String.format("同步完成，总数：%d，成功：%d", totalCount, successCount);
        } catch (Exception e) {
            log.error("【会议报告单】任务执行异常", e);
            throw e;
        }
    }

    private boolean syncSingleMeeting(String wsdl, String uuid, Long userId) {
        if (StrUtil.isBlank(uuid)) {
            return false;
        }

        MeetingDetail detail = getMeetingDetail(wsdl, uuid);
        if (detail == null || StrUtil.isBlank(detail.uuid)) {
            return false;
        }

        ConfflowDO existing = confflowMapper.selectOne(Wrappers.<ConfflowDO>lambdaQuery()
                .eq(ConfflowDO::getDocGuid, detail.uuid)
                .eq(ConfflowDO::getDocType, DOC_TYPE)
                .last("LIMIT 1"));
        if (existing != null) {
            syncMissingAttachments(existing.getId(), detail.uuid, detail.attachments);
            return true;
        }

        ConfflowSaveReqVO saveReqVO = new ConfflowSaveReqVO();
        saveReqVO.setDocGuid(detail.uuid);
        saveReqVO.setDocType(DOC_TYPE);
        saveReqVO.setApplyDate(LocalDateTime.now().withNano(0));
        saveReqVO.setStartDate(detail.startDate);
        saveReqVO.setTitle(StrUtil.blankToDefault(detail.title, "会议报告单"));
        saveReqVO.setContent(detail.content);
        saveReqVO.setRemark(buildRemark(detail));
        saveReqVO.setVenue(detail.place);
        saveReqVO.setJoinUnit(buildJoinUnit(detail));
        saveReqVO.setSituation(null);

        List<ConfflowAttachDO> attachList = new ArrayList<>();
        if (CollUtil.isNotEmpty(detail.attachments)) {
            for (MeetingAttachment attachment : detail.attachments) {
                ConfflowAttachDO attachDO = downloadAndUploadFile(attachment);
                if (attachDO != null) {
                    attachDO.setDocGuid(detail.uuid);
                    attachDO.setDocType(DOC_TYPE);
                    attachList.add(attachDO);
                }
            }
        }
        saveReqVO.setFileList(attachList);

        confflowService.saveConfflow(userId, saveReqVO);
        return true;
    }

    private List<String> getMeetingUuidList(String wsdl, String listParam) {
        String result = invokeMeetingService(wsdl, "getMeetingList", listParam);
        Document document = parseResultXml(result);
        validateBusinessResult(document, "getMeetingList");
        NodeList nodes = XmlUtil.getNodeListByXPath("//*[local-name()='dataMeeting']", document);
        List<String> uuids = new ArrayList<>();
        Set<String> seenUuids = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            String uuid = childText((Element) node, "uuid");
            if (StrUtil.isNotBlank(uuid) && seenUuids.add(uuid)) {
                uuids.add(uuid);
            }
        }
        log.info("【会议报告单】列表获取 UUID 数量: {}", uuids.size());
        return uuids;
    }

    private MeetingDetail getMeetingDetail(String wsdl, String uuid) {
        String result = invokeMeetingService(wsdl, "getMeetingDetail", uuid);
        Document document = parseResultXml(result);
        validateBusinessResult(document, "getMeetingDetail");
        Node detailNode = XmlUtil.getNodeByXPath("//*[local-name()='dataMeetingDetail']", document);
        if (!(detailNode instanceof Element)) {
            log.warn("【会议报告单】详情为空, uuid: {}", uuid);
            return null;
        }

        Element detailElement = (Element) detailNode;
        MeetingDetail detail = new MeetingDetail();
        detail.content = childText(detailElement, "oamtContent");
        detail.linkman = childText(detailElement, "oamtLinkman");
        detail.linktel = childText(detailElement, "oamtLinktel");
        detail.meetingDept = childText(detailElement, "oamtMeetingdept");
        detail.personNum = childText(detailElement, "oamtPersonnum");
        detail.place = childText(detailElement, "oamtPlace");
        detail.remoteRemark = childText(detailElement, "oamtRemark");
        detail.scopeName = childText(detailElement, "oamtScopename");
        detail.startDate = parseDateTime(childText(detailElement, "oamtSdate"));
        detail.sendUnit = childText(detailElement, "oamtSendunit");
        detail.signer = childText(detailElement, "oamtSigner");
        detail.title = childText(detailElement, "oamtTitle");
        detail.uuid = StrUtil.blankToDefault(childText(detailElement, "oamtUuid"), uuid);

        NodeList fileNodes = XmlUtil.getNodeListByXPath("//*[local-name()='dataAttachment']", document);
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Node node = fileNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element fileElement = (Element) node;
            MeetingAttachment attachment = new MeetingAttachment();
            attachment.filename = childText(fileElement, "filename");
            attachment.url = resolveRemoteUrl(wsdl, childText(fileElement, "url"));
            if (StrUtil.isNotBlank(attachment.url)) {
                detail.attachments.add(attachment);
            }
        }
        return detail;
    }

    private String invokeMeetingService(String wsdl, String method, String argument) {
        log.info("【会议报告单】WebService 请求 method={}, argument={}", method, argument);
        String endpoint = StrUtil.removeSuffixIgnoreCase(wsdl, "?wsdl");
        String namespace = resolveTargetNamespace(wsdl);
        String paramName = StrUtil.blankToDefault(configApi.getConfigValueByKey(SOAP_PARAM_NAME_KEY), "in0");
        paramName = StrUtil.subAfter(paramName, ':', true);
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\""
                + XmlUtil.escape(namespace) + "\">"
                + "<soapenv:Header/><soapenv:Body><ser:" + method + ">"
                + "<ser:" + paramName + ">" + XmlUtil.escape(argument) + "</ser:" + paramName + ">"
                + "</ser:" + method + "></soapenv:Body></soapenv:Envelope>";

        String soapResult;
        try (HttpResponse response = HttpRequest.post(endpoint)
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("SOAPAction", "")
                .body(requestBody)
                .timeout(TIMEOUT)
                .execute()) {
            if (!response.isOk()) {
                throw new IllegalStateException("会议接口请求失败, method=" + method + ", status=" + response.getStatus());
            }
            soapResult = response.body();
        }
        log.info("【会议报告单】WebService 响应 method={}, 长度={}", method, StrUtil.length(soapResult));

        String payload = extractSoapPayload(soapResult);
        if (StrUtil.isBlank(payload)) {
            payload = soapResult;
        }
        return payload;
    }

    private String resolveRemoteUrl(String wsdl, String remoteUrl) {
        if (StrUtil.isBlank(remoteUrl)) {
            return null;
        }
        try {
            URI remoteUri = URI.create(remoteUrl);
            if (remoteUri.isAbsolute()) {
                return remoteUrl;
            }
            return URI.create(StrUtil.removeSuffixIgnoreCase(wsdl, "?wsdl")).resolve(remoteUri).toString();
        } catch (Exception e) {
            log.warn("【会议报告单】附件地址解析失败: {}", remoteUrl);
            return remoteUrl;
        }
    }

    private String resolveTargetNamespace(String wsdl) {
        String cached = NAMESPACE_CACHE.get(wsdl);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        try {
            String wsdlXml = HttpUtil.get(wsdl, TIMEOUT);
            Document wsdlDoc = XmlUtil.parseXml(wsdlXml);
            String namespace = wsdlDoc.getDocumentElement().getAttribute("targetNamespace");
            if (StrUtil.isBlank(namespace)) {
                throw new IllegalArgumentException("WSDL 未声明 targetNamespace");
            }
            NAMESPACE_CACHE.put(wsdl, namespace);
            return namespace;
        } catch (Exception e) {
            throw new IllegalStateException("读取会议接口 WSDL 失败: " + wsdl, e);
        }
    }

    private String extractSoapPayload(String soapResult) {
        try {
            Document soapDoc = XmlUtil.parseXml(soapResult);
            NodeList nodes = XmlUtil.getNodeListByXPath("//*[contains(local-name(), 'return') or contains(local-name(), 'out')]", soapDoc);
            for (int i = 0; i < nodes.getLength(); i++) {
                String text = nodes.item(i).getTextContent();
                if (StrUtil.isNotBlank(text) && text.contains("<result")) {
                    return text;
                }
            }
            String text = soapDoc.getDocumentElement().getTextContent();
            if (StrUtil.isNotBlank(text) && text.contains("<result")) {
                return text;
            }
        } catch (Exception e) {
            log.warn("【会议报告单】SOAP 响应解析失败，尝试直接解析业务 XML", e);
        }
        return soapResult;
    }

    private Document parseResultXml(String xml) {
        if (StrUtil.isBlank(xml)) {
            throw new IllegalArgumentException("会议接口返回 XML 为空");
        }
        String cleanXml = xml.trim();
        int resultStart = cleanXml.indexOf("<result");
        if (resultStart > 0) {
            cleanXml = cleanXml.substring(resultStart);
        }
        return XmlUtil.parseXml(cleanXml);
    }

    private void validateBusinessResult(Document document, String method) {
        Node successNode = XmlUtil.getNodeByXPath("//*[local-name()='success']", document);
        String success = successNode != null ? StrUtil.trim(successNode.getTextContent()) : null;
        if ("1".equals(success) || "true".equalsIgnoreCase(success)) {
            return;
        }
        Node messageNode = XmlUtil.getNodeByXPath("//*[local-name()='msg']", document);
        String message = messageNode != null ? StrUtil.trim(messageNode.getTextContent()) : null;
        throw new IllegalStateException("会议接口业务处理失败, method=" + method
                + ", success=" + StrUtil.blankToDefault(success, "未返回")
                + ", message=" + StrUtil.blankToDefault(message, "未返回"));
    }

    private ConfflowAttachDO downloadAndUploadFile(MeetingAttachment attachment) {
        try {
            if (attachment == null || StrUtil.isBlank(attachment.url)) {
                return null;
            }
            log.info("【会议报告单】附件下载 URL: {}", attachment.url);
            byte[] fileBytes;
            try (HttpResponse response = HttpRequest.get(attachment.url)
                    .timeout(TIMEOUT)
                    .setFollowRedirects(true)
                    .execute()) {
                if (!response.isOk()) {
                    throw new IllegalStateException("附件下载失败, status=" + response.getStatus());
                }
                String contentType = response.header("Content-Type");
                if (StrUtil.containsIgnoreCase(contentType, "text/html")) {
                    throw new IllegalStateException("附件下载返回 HTML，可能是错误页或登录页");
                }
                fileBytes = response.bodyBytes();
            }
            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalStateException("附件下载内容为空");
            }

            String fileName = StrUtil.blankToDefault(attachment.filename, FileUtilLikeName.fromUrl(attachment.url));
            FileDO fileDO = fileService.createFileReturnId(fileBytes, fileName, null, null);

            ConfflowAttachDO attachDO = new ConfflowAttachDO();
            attachDO.setFileName(fileName);
            attachDO.setFileExtension(fileExtension(fileName));
            attachDO.setFilePath(StrUtil.blankToDefault(fileDO.getUrl(), fileDO.getPath()));
            return attachDO;
        } catch (Exception e) {
            log.error("【会议报告单】附件下载转存失败: {}", attachment != null ? attachment.filename : null, e);
            throw new IllegalStateException("会议附件下载转存失败: "
                    + (attachment != null ? attachment.filename : "未知附件"), e);
        }
    }

    private void syncMissingAttachments(Long confflowId, String docGuid,
                                        List<MeetingAttachment> remoteAttachments) {
        if (confflowId == null || CollUtil.isEmpty(remoteAttachments)) {
            return;
        }
        List<ConfflowAttachDO> existingAttaches = confflowAttachMapper.selectListByCommId(confflowId);
        Set<String> existingFileNames = new HashSet<>();
        for (ConfflowAttachDO existingAttach : existingAttaches) {
            if (StrUtil.isNotBlank(existingAttach.getFileName())) {
                existingFileNames.add(existingAttach.getFileName());
            }
        }

        for (MeetingAttachment remoteAttachment : remoteAttachments) {
            if (remoteAttachment == null || StrUtil.isBlank(remoteAttachment.filename) || existingFileNames.contains(remoteAttachment.filename)) {
                continue;
            }
            ConfflowAttachDO attachDO = downloadAndUploadFile(remoteAttachment);
            if (attachDO != null) {
                attachDO.setCommId(confflowId);
                attachDO.setDocGuid(docGuid);
                attachDO.setDocType(DOC_TYPE);
                confflowAttachMapper.insert(attachDO);
                existingFileNames.add(attachDO.getFileName());
            }
        }
    }

    private String childText(Element element, String childName) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && childName.equals(child.getLocalName() != null ? child.getLocalName() : child.getNodeName())) {
                return StrUtil.trim(child.getTextContent());
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return DateUtil.parse(value).toLocalDateTime();
        } catch (Exception e) {
            log.warn("【会议报告单】会议时间解析失败: {}", value);
            return null;
        }
    }

    private String buildRemark(MeetingDetail detail) {
        List<String> parts = new ArrayList<>();
        addRemarkPart(parts, "备注", detail.remoteRemark);
        addRemarkPart(parts, "联系人", detail.linkman);
        addRemarkPart(parts, "联系电话", detail.linktel);
        addRemarkPart(parts, "签发人", detail.signer);
        return StrUtil.join("；", parts);
    }

    private String buildJoinUnit(MeetingDetail detail) {
        String unit = StrUtil.blankToDefault(detail.meetingDept, detail.sendUnit);
        if (StrUtil.isBlank(unit)) {
            return detail.signer;
        }
        if (StrUtil.isBlank(detail.signer)) {
            return unit;
        }
        return unit + "（" + detail.signer + "）";
    }

    private void addRemarkPart(List<String> parts, String label, String value) {
        if (StrUtil.isNotBlank(value)) {
            parts.add(label + "：" + value);
        }
    }

    private String fileExtension(String fileName) {
        if (StrUtil.isBlank(fileName) || !fileName.contains(".")) {
            return null;
        }
        return StrUtil.subAfter(fileName, '.', true);
    }

    private static class MeetingDetail {
        private String content;
        private String linkman;
        private String linktel;
        private String meetingDept;
        private String personNum;
        private String place;
        private String remoteRemark;
        private String scopeName;
        private LocalDateTime startDate;
        private String sendUnit;
        private String signer;
        private String title;
        private String uuid;
        private final List<MeetingAttachment> attachments = new ArrayList<>();
    }

    private static class MeetingAttachment {
        private String filename;
        private String url;
    }

    private static class FileUtilLikeName {
        private static String fromUrl(String url) {
            if (StrUtil.isBlank(url)) {
                return "会议附件";
            }
            String cleanUrl = StrUtil.subBefore(url, '?', false);
            String name = StrUtil.subAfter(cleanUrl, '/', true);
            return StrUtil.blankToDefault(name, "会议附件");
        }
    }
}
