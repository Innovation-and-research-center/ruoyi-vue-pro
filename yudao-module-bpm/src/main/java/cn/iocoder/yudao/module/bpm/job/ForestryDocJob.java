package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
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
import cn.iocoder.yudao.module.bpm.job.Dto.receive.Forestry.DocsReaderDTO;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.Forestry.ForestryResult;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.Forestry.IFileDTO;
import cn.iocoder.yudao.module.bpm.service.fileexchange.FileExchangeService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 林业局收文同步任务 (对应 C# SynchronizeForestry)
 */
@Slf4j
@Component
public class ForestryDocJob implements JobHandler {

    // 配置 Key
    static final String FORESTRY_DOMAIN = "url.receive.forestry";
    static final String FORESTRY_LOGIN = "key.forestry.loginname";
    static final String FORESTRY_PWD = "key.forestry.password";
    static final String DEFAULT_USER_ID = "key.receive.user"; // 本地接收人ID

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
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过林业局同步", currentTenantId);
            return "跳过非目标租户";
        }
        try {
            log.info("【林业局收文】开始同步...");
            String domain = configApi.getConfigValueByKey(FORESTRY_DOMAIN);
            String loginName = configApi.getConfigValueByKey(FORESTRY_LOGIN);
            String password = configApi.getConfigValueByKey(FORESTRY_PWD);

            // 1. 准备请求参数
            String md5Pwd = SecureUtil.md5(password); // 对应 C# GetMD5Encrypt32
//            String today = DateUtil.today(); // yyyy-MM-dd
            String today = "2025-08-07";

            String listUrl = domain + "/push/docsReader.do?sysCmd=getReader&dataTime=" + today
                    + "&loginName=" + loginName + "&passWord=" + md5Pwd;

            // 2. 发送请求并获取 Cookie (C# session = response.Headers["Set-Cookie"])
            HttpResponse response = HttpRequest.get(listUrl)
                    .timeout(10000)
                    .execute();

            if (!response.isOk()) {
                log.error("【林业局收文】接口请求失败: {}", response.getStatus());
                return "接口请求失败";
            }

            String result = response.body();
            // 获取 Session Cookie，用于后续下载附件
            String sessionCookie = response.getCookieStr();

            if (StrUtil.isEmpty(result)) return "返回结果为空";

            ForestryResult<List<DocsReaderDTO>> resData = JSONUtil.toBean(result, new TypeReference<ForestryResult<List<DocsReaderDTO>>>() {}, false);

            if (resData.getStatus() == 1 && CollUtil.isNotEmpty(resData.getData())) {
                int successCount = 0;
                for (DocsReaderDTO item : resData.getData()) {
                    try {
                        boolean synced = syncSingleDoc(item, sessionCookie, domain, loginName, md5Pwd);
                        if (synced) successCount++;
                    } catch (Exception e) {
                        log.error("【林业局收文】同步单条失败: {}", item.getTitle(), e);
                    }
                }
                log.info("【林业局收文】同步完成，接收：{}", successCount);
                return String.format("同步完成，接收：%d", successCount);
            } else {
                log.info("【林业局收文】无数据或接口状态错误: {}", resData.getMessage());
            }

        } catch (Exception e) {
            log.error("【林业局收文】任务异常", e);
            throw e;
        }
        return param;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean syncSingleDoc(DocsReaderDTO doc, String sessionCookie, String domain, String loginName, String md5Pwd) {
        // 1. 构造唯一标识 (对应 C# lyj- + id)
        String docUnique = "lyj-" + doc.getId();

        // 2. 查重
        FileExchangeDO existExchange = fileExchangeMapper.selectOne(Wrappers.<FileExchangeDO>lambdaQuery()
                .eq(FileExchangeDO::getDocunique, docUnique));

        // 3. 整理附件列表 (C# 逻辑：将主文件插入到 fileUrls 的第一个位置)
        List<IFileDTO> allFiles = new ArrayList<>();
        if (CollUtil.isNotEmpty(doc.getFileUrls())) {
            allFiles.addAll(doc.getFileUrls());
        }
        // 如果有主文件，插入到头部
        if (StrUtil.isNotEmpty(doc.getFileUrl())) {
            IFileDTO mainFile = new IFileDTO();
            // 默认文件名，后续下载时会尝试从 Header 读取真实文件名
            mainFile.setFileName(doc.getTitle() + ".pdf");
            mainFile.setFileUrl(doc.getFileUrl());
            allFiles.add(0, mainFile);
        }

        if (existExchange != null) {
            // C# 逻辑：已签收的办件补充附件完整性
            Long receiveDocId = existExchange.getDocId();
            if (receiveDocId != null) {
                syncMissingAttachments(receiveDocId, allFiles, sessionCookie);
            }

            // 更新远程状态
            //测试阶段注释
//            updateRemoteStatus(domain, loginName, md5Pwd, doc.getId());
            return false;
        }

        // 4. 创建新收文
        ReceiveDocSaveReqVO receiveDocDO = new ReceiveDocSaveReqVO();
        receiveDocDO.setDocClass("7"); // 县市来文
        Long numberReceiveNumber = receiveDocService.generateDocumentSequence("7");
        receiveDocDO.setDocSequence(numberReceiveNumber);

        // 处理时间
        LocalDateTime sendDate = LocalDateTime.now();
        if (StrUtil.isNotEmpty(doc.getShouWRQ())) {
            try {
                sendDate = DateUtil.parse(doc.getShouWRQ()).toLocalDateTime();
            } catch (Exception e) {
                // ignore
            }
        }

        receiveDocDO.setYear(String.valueOf(LocalDateTime.now().getYear()));
        receiveDocDO.setReceiveDocNumber(LocalDateTime.now().getYear() + "-" + receiveDocDO.getDocClass() + "-" + numberReceiveNumber);
        receiveDocDO.setUrgencyDegree("1");
        receiveDocDO.setSendDocNumber(doc.getWenH());
        receiveDocDO.setSubject(StrUtil.trim(doc.getTitle()));
        receiveDocDO.setSendDept(doc.getDept());
        receiveDocDO.setDocSecondClass(getDocClass(receiveDocDO.getSubject()));
        receiveDocDO.setSendTime(sendDate);
        receiveDocDO.setReceiveTime(LocalDateTime.now());
        receiveDocDO.setDocRange("PT");

        // 5. 下载并处理附件
        List<ReceiveDocAttachDO> attachList = new ArrayList<>();
        for (IFileDTO fileDTO : allFiles) {
            ReceiveDocAttachDO attach = downloadAndUploadFile(fileDTO, sessionCookie);
            if (attach != null) {
                attachList.add(attach);
            }
        }
        receiveDocDO.setFileList(attachList);

        // 6. 保存并启动流程
        Long userId = Long.valueOf(configApi.getConfigValueByKey(DEFAULT_USER_ID));
        Long receiveDocId = receiveDocService.saveReceiveDoc(userId, receiveDocDO);

        // 7. 创建 FileExchange 记录
        FileExchangeSaveReqVO exchangeVO = new FileExchangeSaveReqVO();
        exchangeVO.setOperationDate(LocalDateTime.now());
        exchangeVO.setOperationPerson("系统自动");
        exchangeVO.setOperationType((short) 2);
        exchangeVO.setDocId(receiveDocId);
        exchangeVO.setSendDocNumber(receiveDocDO.getSendDocNumber());
        exchangeVO.setSubject(receiveDocDO.getSubject());
        exchangeVO.setDocunique(docUnique);
        fileExchangeService.createFileExchange(exchangeVO);

        // 8. 更新远程状态 (对应 C# getReaderStatus)
//        updateRemoteStatus(domain, loginName, md5Pwd, doc.getId());

        return true;
    }

    /**
     * 下载附件并上传到文件服务
     * C# 逻辑重点：如果是主文件，需要从 Header 中解析文件名
     */
    private ReceiveDocAttachDO downloadAndUploadFile(IFileDTO fileDTO, String sessionCookie) {
        try {
            String url = fileDTO.getFileUrl();
            // C# 代码在 URL 是相对路径时没有拼接 Domain，这里假设 URL 可能是完整的或者需要处理
            // 如果 url 不以 http 开头，建议拼接 domain，视实际返回数据而定
            if (url != null && !url.startsWith("http")) {
                String domain = configApi.getConfigValueByKey(FORESTRY_DOMAIN);
                url = domain + (url.startsWith("/") ? "" : "/") + url;
            }

            // 发起请求下载
            HttpResponse response = HttpRequest.get(url)
                    .cookie(sessionCookie) // 关键：带上 Session
                    .execute();

            if (!response.isOk()) return null;

            byte[] fileBytes = response.bodyBytes();
            if (fileBytes == null || fileBytes.length == 0) return null;

            String finalFileName = fileDTO.getFileName();

            // C# 逻辑复现：尝试从 Content-Disposition 获取真实文件名
            // 只有当文件名看起来是默认构造的(如 title.pdf)或者明确需要解析时才覆盖
            String disposition = response.header("Content-Disposition");
            if (StrUtil.isNotEmpty(disposition) && disposition.contains("filename=")) {
                String extractName = StrUtil.subAfter(disposition, "filename=", true);
                // 去除可能存在的引号
                extractName = StrUtil.strip(extractName, "\"");
                // URL Decode
                extractName = URLUtil.decode(extractName);
                if (StrUtil.isNotEmpty(extractName)) {
                    finalFileName = extractName;
                    log.info("【林业局收文】从Header解析到文件名: {}", finalFileName);
                }
            }

            // 过滤非法字符
            finalFileName = FileUtil.cleanInvalid(finalFileName);

            // 上传
            FileDO fileDO = fileService.createFileReturnId(fileBytes, finalFileName, null, null);

            ReceiveDocAttachDO attachDO = new ReceiveDocAttachDO();
            attachDO.setAttachFileId(fileDO.getId());
            attachDO.setAttachFileName(finalFileName);
            attachDO.setShowType((short) 0);
            return attachDO;

        } catch (Exception e) {
            log.error("附件下载失败: {}", fileDTO.getFileName(), e);
            return null;
        }
    }

    /**
     * 补充缺失的附件
     */
    private void syncMissingAttachments(Long receiveDocId, List<IFileDTO> remoteFiles, String sessionCookie) {
        if (CollUtil.isEmpty(remoteFiles)) return;

        List<ReceiveDocAttachDO> existingAttaches = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);
        Set<String> existingFileNames = existingAttaches.stream()
                .map(ReceiveDocAttachDO::getAttachFileName)
                .collect(Collectors.toSet());

        for (IFileDTO remoteFile : remoteFiles) {
            // 注意：这里用 DTO 里的文件名做初步判断，但如果是主文件，文件名可能是下载后才确定的
            // 简单起见，如果 remoteFile.getFileName() (主要是附件) 不存在则下载
            // 对于主文件，最好下载下来比对，或者略过(假设主文件不会变)
            String checkName = FileUtil.cleanInvalid(remoteFile.getFileName());

            if (!existingFileNames.contains(checkName)) {
                ReceiveDocAttachDO newAttach = downloadAndUploadFile(remoteFile, sessionCookie);
                if (newAttach != null) {
                    // 二次检查：如果下载下来的文件名在库里已经有了（例如Header里的名字和DTO里的名字不一样）
                    if (!existingFileNames.contains(newAttach.getAttachFileName())) {
                        newAttach.setReceiveDocId(receiveDocId);
                        receiveDocAttachMapper.insert(newAttach);
                        log.info("【林业局收文】补充附件: {}", newAttach.getAttachFileName());
                    }
                }
            }
        }
    }

    /**
     * 更新远程状态
     */
    private void updateRemoteStatus(String domain, String loginName, String md5Pwd, String id) {
        try {
            // C# sysCmd=getReaderStatus
            String url = domain + "/push/docsReader.do?sysCmd=getReaderStatus"
                    + "&loginName=" + loginName + "&passWord=" + md5Pwd + "&id=" + id;
            HttpUtil.get(url);
        } catch (Exception e) {
            log.warn("更新林业局远程状态失败: {}", id, e);
        }
    }

    /**
     * 解析二级分类 (逻辑复用)
     */
    private String getDocClass(String title) {
        if (StrUtil.isEmpty(title)) return "";
        if (title.length() > 4) {
            List<DictDataRespDTO> dictList = DictFrameworkUtils.getDictDataList("doc_class");
            if (dictList == null || dictList.isEmpty()) return "";
            String suffix = title.substring(title.length() - 4);
            for (DictDataRespDTO dict : dictList) {
                if (suffix.contains(dict.getLabel())) {
                    try {
                        return dict.getLabel();
                    } catch (NumberFormatException e) {
                        return "";
                    }
                }
            }
        }
        return "";
    }


}