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
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
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
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DepartmentDocJob implements JobHandler {

    static final String RECEIVE_CITY_KEY = "url.receive.city";
    static final String RECEIVE_UUID_KEY = "key.receive.natural";
    static final String DEFAULT_USER_ID = "key.receive.user";

    private static final int PAGE_SIZE = 100;
    private static final int TIMEOUT = 30000;
    private static final String DOC_UNIQUE_PREFIX = "zbm-";
    private static final String TARGET_APPLICATION_PREFIX = "TRANSDEPT_FILE";

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
    @Resource
    private TransactionTemplate transactionTemplate;

    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过两办转部门收文同步", currentTenantId);
            return "跳过非目标租户";
        }

        String domain = configApi.getConfigValueByKey(RECEIVE_CITY_KEY);
        String currentUuid = configApi.getConfigValueByKey(RECEIVE_UUID_KEY);
        if (StrUtil.isBlank(domain)) {
            log.warn("【两办转部门收文】未配置 {}", RECEIVE_CITY_KEY);
            return "未配置两办接口地址";
        }
        if (StrUtil.isBlank(currentUuid)) {
            log.warn("【两办转部门收文】未配置 {}", RECEIVE_UUID_KEY);
            return "未配置两办接口参数";
        }
        String userIdConfig = configApi.getConfigValueByKey(DEFAULT_USER_ID);
        if (StrUtil.isBlank(userIdConfig)) {
            log.warn("【两办转部门收文】未配置 {}", DEFAULT_USER_ID);
            return "未配置默认处理人";
        }
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

                for (DepartmentDocItem item : resList.getData()) {
                    if (!isTargetItem(item)) {
                        skippedCount++;
                        log.info("【两办转部门收文】跳过非转部门办件, cnIdos: {}, application: {}, type: {}",
                                item.getCnIdos(), item.getCnApplication(), item.getCnType());
                        continue;
                    }
                    totalCount++;
                    try {
                        Boolean synced = transactionTemplate.execute(status ->
                                syncSingleDoc(domain, userId, item));
                        if (Boolean.TRUE.equals(synced)) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.error("【两办转部门收文】同步单条失败, cnIdos: {}, 标题: {}", item.getCnIdos(), item.getCnTitle(), e);
                    }
                }

                Integer remoteTotal = resList.getTotalCount();
                if (remoteTotal == null || page * PAGE_SIZE >= remoteTotal || resList.getData().size() < PAGE_SIZE) {
                    break;
                }
                page++;
            }
            log.info("【两办转部门收文】同步完成，目标数：{}，成功：{}，跳过：{}",
                    totalCount, successCount, skippedCount);
            return String.format("同步完成，目标数：%d，成功：%d，跳过：%d",
                    totalCount, successCount, skippedCount);
        } catch (Exception e) {
            log.error("【两办转部门收文】任务执行异常", e);
            throw e;
        }
    }

    private boolean syncSingleDoc(String domain, Long userId, DepartmentDocItem item) {
        String cnIdos = item.getCnIdos();
        if (StrUtil.isEmpty(cnIdos)) {
            return false;
        }

        DepartmentDocDetail detail = getDetail(domain, cnIdos);
        if (detail == null) {
            return false;
        }

        String docUnique = buildDocUnique(detail, cnIdos);
        FileExchangeDO existExchange = fileExchangeMapper.selectOne(Wrappers.<FileExchangeDO>lambdaQuery()
                .eq(FileExchangeDO::getDocunique, docUnique));

        if (existExchange != null) {
            Long receiveDocId = existExchange.getDocId();
            if (receiveDocId != null) {
                syncMissingAttachments(domain, receiveDocId, detail.getWkflwFilesList());
            }
            return true;
        }

        ReceiveDocSaveReqVO receiveDocDO = new ReceiveDocSaveReqVO();
        receiveDocDO.setDocClass("7");
        Long numberReceiveNumber = receiveDocService.generateDocumentSequence("7");
        receiveDocDO.setDocSequence(numberReceiveNumber);

        LocalDateTime sendTime = parseDocumentTime(detail);

        receiveDocDO.setYear(String.valueOf(sendTime.getYear()));
        receiveDocDO.setReceiveDocNumber(sendTime.getYear() + "-" + receiveDocDO.getDocClass() + "-" + numberReceiveNumber);
        receiveDocDO.setUrgencyDegree(convertUrgency(item.getCnUrgentStatus()));
        receiveDocDO.setSendDocNumber(StrUtil.blankToDefault(detail.getOafrFilecode(), detail.getOafrFilereceiveno()));
        receiveDocDO.setSubject(StrUtil.trim(detail.getOafrTitle()));
        receiveDocDO.setSendDept(detail.getOafrFiledep());
        receiveDocDO.setDocSecondClass(getDocClass(receiveDocDO.getSubject()));
        receiveDocDO.setSendTime(sendTime);
        receiveDocDO.setReceiveTime(LocalDateTime.now().withNano(0));
        receiveDocDO.setRemark(detail.getOafrFiletype());
        receiveDocDO.setDocRange("PT");

        List<ReceiveDocAttachDO> attachList = new ArrayList<>();
        if (CollUtil.isNotEmpty(detail.getWkflwFilesList())) {
            for (DepartmentWkflwFile remoteFile : detail.getWkflwFilesList()) {
                attachList.add(downloadAndUploadFile(domain, remoteFile));
            }
        }
        receiveDocDO.setFileList(attachList);

        Long receiveDocId = receiveDocService.saveReceiveDoc(userId, receiveDocDO);

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

        return true;
    }

    private DepartmentDocDetail getDetail(String domain, String cnIdos) {
        String detailUrl = domain + "/oa/api/public/service/getMessageZbmDetail.do?uuid=" + URLUtil.encodeAll(cnIdos);
        log.info("【两办转部门收文】详情请求 URL: {}", detailUrl);
        String result = getResponseBody(detailUrl, "详情");
        log.info("【两办转部门收文】详情响应长度: {}", StrUtil.length(result));
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
                    + URLUtil.encodeAll(remoteFile.getWkfileUuid());
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
                if (StrUtil.containsIgnoreCase(contentType, "text/html")
                        || StrUtil.containsIgnoreCase(contentType, "application/json")) {
                    throw new IllegalStateException("附件下载返回错误内容, Content-Type=" + contentType);
                }
                fileBytes = response.bodyBytes();
            }
            if (fileBytes == null || fileBytes.length == 0) {
                throw new IllegalStateException("附件下载内容为空");
            }

            String fileName = buildFileName(remoteFile);
            FileDO fileDO = fileService.createFileReturnId(fileBytes, fileName, null, null);

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

        for (DepartmentWkflwFile remoteFile : remoteFiles) {
            if (remoteFile == null) {
                continue;
            }
            String fileName = buildFileName(remoteFile);
            if (existingFileNames.contains(fileName)) {
                continue;
            }

            ReceiveDocAttachDO newAttach = downloadAndUploadFile(domain, remoteFile);
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
                throw new IllegalStateException("两办" + operation + "接口请求失败, status=" + response.getStatus());
            }
            return response.body();
        }
    }

    private boolean isTargetItem(DepartmentDocItem item) {
        if (item == null || StrUtil.isBlank(item.getCnIdos())) {
            return false;
        }
        if (StrUtil.isNotBlank(item.getCnApplication())) {
            return StrUtil.startWithIgnoreCase(item.getCnApplication(), TARGET_APPLICATION_PREFIX);
        }
        return StrUtil.isBlank(item.getCnType()) || "C".equalsIgnoreCase(item.getCnType());
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

    private String getDocClass(String title) {
        if (StrUtil.isEmpty(title)) {
            return "";
        }
        if (title.length() > 4) {
            List<DictDataRespDTO> dictList = DictFrameworkUtils.getDictDataList("doc_class");
            if (dictList == null || dictList.isEmpty()) {
                return "";
            }
            String suffix = title.substring(title.length() - 4);
            for (DictDataRespDTO dict : dictList) {
                if (suffix.contains(dict.getLabel())) {
                    return dict.getLabel();
                }
            }
        }
        return "";
    }

}
