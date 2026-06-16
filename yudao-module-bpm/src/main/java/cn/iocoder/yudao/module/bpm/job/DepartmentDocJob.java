package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo.FileExchangeSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ReceiveDocSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.fileexchange.FileExchangeMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.department.DepartmentDocDetail;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.department.DepartmentDocItem;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.department.DepartmentDocResult;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.department.DepartmentWkflwFile;
import cn.iocoder.yudao.module.bpm.service.fileexchange.FileExchangeService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DepartmentDocJob implements JobHandler {

    static final String DEPARTMENT_DOMAIN_KEY = "url.receive.department";
    static final String DEPARTMENT_UUID_KEY = "key.receive.department.uuid";
    static final String DEPARTMENT_USER_ID_KEY = "key.receive.department.user";

    private static final int PAGE_SIZE = 100;
    private static final int TIMEOUT = 30000;
    private static final int SAVE_RETRY_COUNT = 20;
    private static final String DOC_UNIQUE_PREFIX = "zbm-";
    @Resource
    private ConfigApi configApi;
    @Resource
    private ReceiveDocService receiveDocService;
    @Resource
    private FileExchangeService fileExchangeService;
    @Resource
    private FileExchangeMapper fileExchangeMapper;
    @Resource
    private ReceiveDocAttachMapper receiveDocAttachMapper;
    @Resource
    private FileService fileService;

    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        long startMillis = System.currentTimeMillis();
        log.info("【两办转部门收文】任务开始, tenantId: {}, param: {}", currentTenantId, param);
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过两办转部门收文同步", currentTenantId);
            return "跳过非目标租户";
        }

        String domain = configApi.getConfigValueByKey(DEPARTMENT_DOMAIN_KEY);
        String currentUuid = configApi.getConfigValueByKey(DEPARTMENT_UUID_KEY);
        if (StrUtil.isBlank(domain)) {
            log.warn("【两办转部门收文】未配置 {}", DEPARTMENT_DOMAIN_KEY);
            return "未配置两办接口地址";
        }
        if (StrUtil.isBlank(currentUuid)) {
            log.warn("【两办转部门收文】未配置 {}", DEPARTMENT_UUID_KEY);
            return "未配置两办接口参数";
        }
        String userIdConfig = configApi.getConfigValueByKey(DEPARTMENT_USER_ID_KEY);
        if (StrUtil.isBlank(userIdConfig)) {
            log.warn("【两办转部门收文】未配置 {}", DEPARTMENT_USER_ID_KEY);
            return "未配置默认处理人";
        }
        log.info("【两办转部门收文】配置读取完成, domain: {}, uuidConfigured: {}, defaultUserId: {}",
                domain, StrUtil.isNotBlank(currentUuid), userIdConfig);
        final Long userId;
        try {
            userId = Long.valueOf(userIdConfig);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("默认处理人配置不是有效数字: " + userIdConfig, e);
        }

        int page = 1;
        int totalCount = 0;
        int successCount = 0;
        int skippedCount = 0;

        try {
            while (true) {
                String listUrl = domain + "/oa/api/public/service/getMessageZbmList.do"
                        + "?cnCurrentuuid=" + URLUtil.encodeAll(currentUuid)
                        + "&page=" + page
                        + "&limit=" + PAGE_SIZE
                        + "&start=" + ((page - 1) * PAGE_SIZE);

                log.info("【两办转部门收文】列表请求 URL: {}", listUrl);
                String result = getResponseBody(listUrl, "列表");
                log.info("【两办转部门收文】列表响应长度: {}", StrUtil.length(result));
                log.info("【两办转部门收文】列表响应内容: {}", result);
                if (StrUtil.isEmpty(result)) {
                    log.warn("【两办转部门收文】接口返回结果为空");
                    break;
                }

                DepartmentDocResult<List<DepartmentDocItem>> resList = JSONUtil.toBean(result,
                        new TypeReference<DepartmentDocResult<List<DepartmentDocItem>>>() {}, false);
                if (resList == null || !resList.isSuccess()) {
                    throw new IllegalStateException("两办列表接口业务处理失败: "
                            + (resList != null ? resList.getMessages() : "空响应"));
                }
                if (CollUtil.isEmpty(resList.getData())) {
                    log.info("【两办转部门收文】无待办数据");
                    break;
                }
                log.info("【两办转部门收文】第 {} 页解析完成, remoteTotal: {}, pageSize: {}",
                        page, resList.getTotalCount(), resList.getData().size());

                int pageTargetCount = 0;
                int pageSuccessCount = 0;
                int pageSkippedCount = 0;
                for (DepartmentDocItem item : resList.getData()) {
                    if (!isTargetItem(item)) {
                        skippedCount++;
                        pageSkippedCount++;
                        log.info("【两办转部门收文】跳过无效列表数据, cnIdos: {}, title: {}",
                                item != null ? item.getCnIdos() : null, item != null ? item.getCnTitle() : null);
                        continue;
                    }
                    totalCount++;
                    pageTargetCount++;
                    try {
                        Boolean synced = syncSingleDoc(domain, userId, item);
                        if (Boolean.TRUE.equals(synced)) {
                            successCount++;
                            pageSuccessCount++;
                        }
                    } catch (Exception e) {
                        log.error("【两办转部门收文】同步单条失败, cnIdos: {}, 标题: {}", item.getCnIdos(), item.getCnTitle(), e);
                    }
                }
                log.info("【两办转部门收文】第 {} 页处理完成, 目标: {}, 成功: {}, 跳过: {}",
                        page, pageTargetCount, pageSuccessCount, pageSkippedCount);

                Integer remoteTotal = resList.getTotalCount();
                if (remoteTotal == null || page * PAGE_SIZE >= remoteTotal || resList.getData().size() < PAGE_SIZE) {
                    break;
                }
                page++;
            }
            log.info("【两办转部门收文】同步完成，目标数：{}，成功：{}，跳过：{}，耗时：{}ms",
                    totalCount, successCount, skippedCount, System.currentTimeMillis() - startMillis);
            return String.format("同步完成，目标数：%d，成功：%d，跳过：%d",
                    totalCount, successCount, skippedCount);
        } catch (Exception e) {
            log.error("【两办转部门收文】任务执行异常, 已处理目标数: {}, 成功: {}, 跳过: {}, 耗时: {}ms",
                    totalCount, successCount, skippedCount, System.currentTimeMillis() - startMillis, e);
            throw e;
        }
    }

    private boolean syncSingleDoc(String domain, Long userId, DepartmentDocItem item) {
        String cnIdos = item.getCnIdos();
        if (StrUtil.isEmpty(cnIdos)) {
            log.warn("【两办转部门收文】跳过 cnIdos 为空的数据, title: {}", item.getCnTitle());
            return false;
        }
        log.info("【两办转部门收文】开始同步单条, cnIdos: {}, title: {}, urgent: {}",
                cnIdos, item.getCnTitle(), item.getCnUrgentStatus());

        DepartmentDocDetail detail = getDetail(domain, cnIdos);
        if (detail == null) {
            log.warn("【两办转部门收文】详情为空，跳过, cnIdos: {}", cnIdos);
            return false;
        }

        String docUnique = buildDocUnique(detail, cnIdos);
        FileExchangeDO existExchange = fileExchangeMapper.selectOne(Wrappers.<FileExchangeDO>lambdaQuery()
                .eq(FileExchangeDO::getDocunique, docUnique));

        if (existExchange != null) {
            Long receiveDocId = existExchange.getDocId();
            log.info("【两办转部门收文】已同步过，检查附件, cnIdos: {}, docUnique: {}, receiveDocId: {}",
                    cnIdos, docUnique, receiveDocId);
            if (receiveDocId != null) {
                syncMissingAttachments(domain, receiveDocId, detail.getWkflwFilesList());
            }
            return true;
        }

        ReceiveDocSaveReqVO receiveDocDO = new ReceiveDocSaveReqVO();
        receiveDocDO.setDocClass("7");

        LocalDateTime sendTime = parseDocumentTime(detail);
        LocalDateTime receiveTime = LocalDateTime.now().withNano(0);
        fillReceiveDocNumber(receiveDocDO, receiveTime);

        receiveDocDO.setYear(String.valueOf(sendTime.getYear()));
        receiveDocDO.setUrgencyDegree(convertUrgency(item.getCnUrgentStatus()));
        receiveDocDO.setSendDocNumber(StrUtil.blankToDefault(detail.getOafrFilecode(), detail.getOafrFilereceiveno()));
        receiveDocDO.setSubject(StrUtil.trim(detail.getOafrTitle()));
        receiveDocDO.setSendDept(detail.getOafrFiledep());
        receiveDocDO.setDocSecondClass(ReceiveDocClassParser.parse(receiveDocDO.getSubject()));
        receiveDocDO.setSendTime(sendTime);
        receiveDocDO.setReceiveTime(receiveTime);
        receiveDocDO.setRemark(detail.getOafrFiletype());
        receiveDocDO.setDocRange("PT");

        List<ReceiveDocAttachDO> attachList = new ArrayList<>();
        int remoteAttachCount = CollUtil.size(detail.getWkflwFilesList());
        log.info("【两办转部门收文】准备创建收文, cnIdos: {}, docUnique: {}, subject: {}, sendDept: {}, sendTime: {}, remoteAttachCount: {}",
                cnIdos, docUnique, receiveDocDO.getSubject(), receiveDocDO.getSendDept(), sendTime, remoteAttachCount);
        if (CollUtil.isNotEmpty(detail.getWkflwFilesList())) {
            for (DepartmentWkflwFile remoteFile : detail.getWkflwFilesList()) {
                ReceiveDocAttachDO attach = downloadAndUploadFile(domain, remoteFile);
                if (attach != null) {
                    attachList.add(attach);
                }
            }
        }
        receiveDocDO.setFileList(attachList);

        Long receiveDocId = saveReceiveDocWithRetry(userId, receiveDocDO, receiveTime);

        FileExchangeSaveReqVO exchangeVO = new FileExchangeSaveReqVO();
        exchangeVO.setOperationDate(LocalDateTime.now().withNano(0));
        exchangeVO.setOperationPerson("系统自动");
        exchangeVO.setOperationInformation("两办转部门文件");
        exchangeVO.setOperationType((short) 2);
        exchangeVO.setDocId(receiveDocId);
        exchangeVO.setSendDocNumber(receiveDocDO.getSendDocNumber());
        exchangeVO.setSubject(receiveDocDO.getSubject());
        exchangeVO.setDocunique(docUnique);
        fileExchangeService.createFileExchange(exchangeVO);

        log.info("【两办转部门收文】创建收文完成, cnIdos: {}, docUnique: {}, receiveDocId: {}, attachCount: {}",
                cnIdos, docUnique, receiveDocId, attachList.size());
        return true;
    }

    private void fillReceiveDocNumber(ReceiveDocSaveReqVO receiveDocDO, LocalDateTime receiveTime) {
        Long numberReceiveNumber = receiveDocService.generateDocumentSequence(receiveDocDO.getDocClass(),
                String.valueOf(receiveTime.getYear()));
        receiveDocDO.setDocSequence(numberReceiveNumber);
        receiveDocDO.setReceiveDocNumber(String.format("%d-%s-%04d",
                receiveTime.getYear(), receiveDocDO.getDocClass(), numberReceiveNumber));
    }

    private Long saveReceiveDocWithRetry(Long userId, ReceiveDocSaveReqVO receiveDocDO, LocalDateTime receiveTime) {
        ServiceException lastException = null;
        for (int i = 1; i <= SAVE_RETRY_COUNT; i++) {
            try {
                return receiveDocService.saveReceiveDoc(userId, receiveDocDO);
            } catch (ServiceException e) {
                if (!StrUtil.equals(e.getMessage(), "收文编号重复")) {
                    throw e;
                }
                lastException = e;
                log.warn("【两办转部门收文】收文编号重复，递增编号后重试, attempt: {}, receiveDocNumber: {}, subject: {}",
                        i, receiveDocDO.getReceiveDocNumber(), receiveDocDO.getSubject());
                incrementReceiveDocNumber(receiveDocDO, receiveTime);
            }
        }
        throw lastException;
    }

    private void incrementReceiveDocNumber(ReceiveDocSaveReqVO receiveDocDO, LocalDateTime receiveTime) {
        Long nextSequence = receiveDocDO.getDocSequence() == null ? 1L : receiveDocDO.getDocSequence() + 1;
        receiveDocDO.setDocSequence(nextSequence);
        receiveDocDO.setReceiveDocNumber(String.format("%d-%s-%04d",
                receiveTime.getYear(), receiveDocDO.getDocClass(), nextSequence));
    }

    private DepartmentDocDetail getDetail(String domain, String cnIdos) {
        String detailUrl = domain + "/oa/api/public/service/getMessageZbmDetail.do?uuid=" + URLUtil.encodeAll(cnIdos);
        log.info("【两办转部门收文】详情请求 URL: {}", detailUrl);
        String result = getResponseBody(detailUrl, "详情");
        log.info("【两办转部门收文】详情响应长度: {}", StrUtil.length(result));
        log.info("【两办转部门收文】详情响应内容, cnIdos: {}, result: {}", cnIdos, result);
        if (StrUtil.isEmpty(result)) {
            return null;
        }

        DepartmentDocResult<DepartmentDocDetail> resDetail = JSONUtil.toBean(result,
                new TypeReference<DepartmentDocResult<DepartmentDocDetail>>() {}, false);
        if (resDetail == null || !resDetail.isSuccess() || resDetail.getData() == null) {
            log.warn("【两办转部门收文】详情获取失败, cnIdos: {}, message: {}", cnIdos,
                    resDetail != null ? resDetail.getMessages() : "空响应");
            return null;
        }
        return resDetail.getData();
    }

    private ReceiveDocAttachDO downloadAndUploadFile(String domain, DepartmentWkflwFile remoteFile) {
        try {
            if (remoteFile == null || StrUtil.isEmpty(remoteFile.getWkfileUuid())) {
                throw new IllegalArgumentException("附件 UUID 为空");
            }

            String downloadUrl = domain + "/oa/api/public/risen/wkflw/download.do?CMD=DF&TYPE=stream&uuid="
                    + StrUtil.trim(remoteFile.getWkfileUuid());
            log.info("【两办转部门收文】附件下载请求 URL: {}", downloadUrl);

            byte[] fileBytes;
            try (HttpResponse response = HttpRequest.get(downloadUrl)
                    .timeout(TIMEOUT)
                    .setFollowRedirects(true)
                    .execute()) {
                if (!response.isOk()) {
                    throw new IllegalStateException("附件下载失败, status=" + response.getStatus());
                }
                String contentType = response.header("Content-Type");
                String declaredContentType = remoteFile.getWkfileContentType();
                fileBytes = response.bodyBytes();
                if (isErrorContentType(contentType)) {
                    if (isLikelyErrorBody(fileBytes)
                            || (!isDeclaredFileContentType(declaredContentType) && !isLikelyFileContent(fileBytes))) {
                        log.warn("【两办转部门收文】附件下载返回错误内容, uuid: {}, fileName: {}, flowType: {}, declaredContentType: {}, responseContentType: {}, body: {}",
                                remoteFile.getWkfileUuid(), remoteFile.getWkfileName(), remoteFile.getWkfileFlowType(),
                                declaredContentType, contentType, StrUtil.maxLength(StrUtil.str(fileBytes, "UTF-8"), 2000));
                        throw new IllegalStateException("附件下载返回错误内容, Content-Type=" + contentType);
                    }
                    log.warn("【两办转部门收文】附件响应头疑似错误但接口声明/内容是文件，继续转存, uuid: {}, fileName: {}, flowType: {}, declaredContentType: {}, responseContentType: {}, size: {}",
                            remoteFile.getWkfileUuid(), remoteFile.getWkfileName(), remoteFile.getWkfileFlowType(),
                            declaredContentType, contentType, fileBytes.length);
                }
            }
            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalStateException("附件下载内容为空");
            }

            String fileName = buildFileName(remoteFile);
            FileDO fileDO = fileService.createFileReturnId(fileBytes, fileName, null, null);
            log.info("【两办转部门收文】附件下载转存完成, uuid: {}, fileName: {}, size: {}, fileId: {}",
                    remoteFile.getWkfileUuid(), fileName, fileBytes.length, fileDO.getId());

            ReceiveDocAttachDO attachDO = new ReceiveDocAttachDO();
            attachDO.setAttachFileId(fileDO.getId());
            attachDO.setAttachFileName(fileName);
            attachDO.setShowType((short) 0);
            return attachDO;
        } catch (Exception e) {
            log.error("【两办转部门收文】附件下载转存失败: {}", remoteFile != null ? remoteFile.getWkfileName() : null, e);
            throw new IllegalStateException("两办附件下载转存失败: "
                    + (remoteFile != null ? remoteFile.getWkfileName() : "未知附件"), e);
        }
    }

    private boolean isErrorContentType(String contentType) {
        return StrUtil.containsIgnoreCase(contentType, "text/html")
                || StrUtil.containsIgnoreCase(contentType, "application/json");
    }

    private boolean isDeclaredFileContentType(String contentType) {
        return StrUtil.startWithIgnoreCase(contentType, "image/")
                || StrUtil.equalsIgnoreCase(contentType, "application/pdf")
                || StrUtil.equalsIgnoreCase(contentType, "application/msword")
                || StrUtil.equalsIgnoreCase(contentType, "application/vnd.ms-excel")
                || StrUtil.equalsIgnoreCase(contentType, "application/vnd.ms-powerpoint")
                || StrUtil.containsIgnoreCase(contentType, "application/vnd.openxmlformats-officedocument")
                || StrUtil.equalsIgnoreCase(contentType, "application/zip")
                || StrUtil.equalsIgnoreCase(contentType, "application/octet-stream");
    }

    private boolean isLikelyErrorBody(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        int index = 0;
        while (index < bytes.length && bytes[index] <= 0x20) {
            index++;
        }
        if (index >= bytes.length) {
            return false;
        }
        byte first = bytes[index];
        return first == '<' || first == '{' || first == '[';
    }

    private boolean isLikelyFileContent(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        // 部分接口会把真实文件错标为 text/html，这里按常见文件头兜底识别。
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return true; // PNG
        }
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return true; // PDF
        }
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return true; // JPEG
        }
        if (bytes[0] == 0x50 && bytes[1] == 0x4B && (bytes[2] == 0x03 || bytes[2] == 0x05 || bytes[2] == 0x07)
                && (bytes[3] == 0x04 || bytes[3] == 0x06 || bytes[3] == 0x08)) {
            return true; // ZIP / Office
        }
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) {
            return true; // GIF
        }
        if (bytes[0] == 0x42 && bytes[1] == 0x4D) {
            return true; // BMP
        }
        return false;
    }

    private void syncMissingAttachments(String domain, Long receiveDocId, List<DepartmentWkflwFile> remoteFiles) {
        if (CollUtil.isEmpty(remoteFiles)) {
            return;
        }

        List<ReceiveDocAttachDO> existingAttaches = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);
        Set<String> existingFileNames = new HashSet<>();
        for (ReceiveDocAttachDO existingAttach : existingAttaches) {
            if (StrUtil.isNotBlank(existingAttach.getAttachFileName())) {
                existingFileNames.add(existingAttach.getAttachFileName());
            }
        }
        log.info("【两办转部门收文】检查缺失附件, receiveDocId: {}, remoteAttachCount: {}, existingAttachCount: {}",
                receiveDocId, CollUtil.size(remoteFiles), existingFileNames.size());

        for (DepartmentWkflwFile remoteFile : remoteFiles) {
            if (remoteFile == null) {
                continue;
            }
            String fileName = buildFileName(remoteFile);
            if (existingFileNames.contains(fileName)) {
                log.info("【两办转部门收文】附件已存在，跳过, receiveDocId: {}, fileName: {}", receiveDocId, fileName);
                continue;
            }

            ReceiveDocAttachDO newAttach = downloadAndUploadFile(domain, remoteFile);
            if (newAttach == null) {
                continue;
            }
            newAttach.setReceiveDocId(receiveDocId);
            receiveDocAttachMapper.insert(newAttach);
            existingFileNames.add(newAttach.getAttachFileName());
            log.info("【两办转部门收文】补充附件: {}", newAttach.getAttachFileName());
        }
    }

    private String getResponseBody(String url, String operation) {
        try (HttpResponse response = HttpRequest.get(url)
                .timeout(TIMEOUT)
                .setFollowRedirects(true)
                .execute()) {
            if (!response.isOk()) {
                log.warn("【两办转部门收文】{}接口请求失败, status: {}, url: {}", operation, response.getStatus(), url);
                throw new IllegalStateException("两办" + operation + "接口请求失败, status=" + response.getStatus());
            }
            return response.body();
        }
    }

    private boolean isTargetItem(DepartmentDocItem item) {
        return item != null && StrUtil.isNotBlank(item.getCnIdos());
    }

    private LocalDateTime parseDocumentTime(DepartmentDocDetail detail) {
        String time = StrUtil.blankToDefault(detail.getOafrFilereceivedate(), detail.getOafrCreateTime());
        if (StrUtil.isBlank(time)) {
            return LocalDateTime.now().withNano(0);
        }
        try {
            return DateUtil.parse(time).toLocalDateTime();
        } catch (Exception e) {
            log.warn("【两办转部门收文】来文时间解析失败: {}", time);
            return LocalDateTime.now().withNano(0);
        }
    }

    private String convertUrgency(String remoteUrgency) {
        if ("1".equals(remoteUrgency)) {
            return "2";
        }
        if ("2".equals(remoteUrgency)) {
            return "3";
        }
        return "1";
    }

    private String buildDocUnique(DepartmentDocDetail detail, String cnIdos) {
        return DOC_UNIQUE_PREFIX + StrUtil.blankToDefault(detail.getOafrUuid(), cnIdos);
    }

    private String buildFileName(DepartmentWkflwFile remoteFile) {
        String fileName = StrUtil.blankToDefault(remoteFile.getWkfileName(),
                StrUtil.blankToDefault(remoteFile.getWkfileUuid(), "未命名附件"));
        String extName = remoteFile.getWkfileExtention();
        if (StrUtil.isNotEmpty(extName)
                && StrUtil.isEmpty(FileUtil.extName(fileName))
                && !StrUtil.startWith(extName, ".")) {
            fileName = fileName + "." + extName;
        } else if (StrUtil.isNotEmpty(extName)
                && StrUtil.isEmpty(FileUtil.extName(fileName))) {
            fileName = fileName + extName;
        }
        return FileUtil.cleanInvalid(fileName);
    }

}
