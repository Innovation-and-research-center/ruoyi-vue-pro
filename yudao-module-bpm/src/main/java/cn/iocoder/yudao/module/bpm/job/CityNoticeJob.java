package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.bpm.controller.admin.fileexchange.vo.FileExchangeSaveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ReceiveDocSaveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.fileexchange.FileExchangeDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.fileexchange.FileExchangeMapper;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.notice.NoticeDetailDTO;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.notice.NoticeResult;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.notice.OaFileDTO;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.notice.OaNoticeDTO;
import cn.iocoder.yudao.module.bpm.service.fileexchange.FileExchangeService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.SaveFormat;
import com.aspose.words.FontSettings;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CityNoticeJob implements JobHandler {

    static final String RECEIVE_CITY_KEY = "url.receive.notice"; // 域名
    static final String NOTICE_USER_UUID = "key.receive.notice.uuid";  // 用户UUID (对应 C# _setting.Notice.UUID)
    static final String DEFAULT_USER_ID = "key.receive.user";  // 本地处理人ID

    @Resource
    private ConfigApi configApi;
    @Resource
    private ReceiveDocService receiveDocService;
    @Resource
    private FileExchangeService fileExchangeService;
    @Resource
    private FileExchangeMapper fileExchangeMapper;
    @Resource
    private FileService fileService;
    @Override
    @TenantJob
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过市局公告同步", currentTenantId);
            return "跳过非目标租户";
        }
        try{
            String listUrl = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/public/oaNotice/getPendingList.do"
                    + "?strMap.userUuid=" + configApi.getConfigValueByKey(NOTICE_USER_UUID)
                    + "&page=1&limit=20&start=0";

            log.info("【市局公告】列表请求 URL: {}", listUrl);
            String result = HttpUtil.get(listUrl, 30000);
            log.info("【市局公告】列表响应(长度={}): {}", result.length(), result);
            if (StrUtil.isEmpty(result)) {
                log.warn("【市局公告】接口返回结果为空");
                return "接口返回为空";
            }
            NoticeResult<List<OaNoticeDTO>> resList = JSONUtil.toBean(result, new TypeReference<NoticeResult<List<OaNoticeDTO>>>() {}, false);
            if (resList.isSuccess() && resList.getTotalCount() > 0 && CollUtil.isNotEmpty(resList.getData())) {
                int successCount = 0;
                for (OaNoticeDTO item : resList.getData()) {
                    try {
                        // 传入 oanoUuid 获取详情并同步
                        boolean synced = syncSingleNotice(item.getOanoUuid());
                        if (synced) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.error("同步单条公告失败, UUID: {}", item.getOanoUuid(), e);
                    }
                }
                log.info("【市局公告】同步完成，总数：{}，成功入库：{}", resList.getData().size(), successCount);
                return String.format("同步完成，总数：%d，成功：%d", resList.getData().size(), successCount);
            } else {
                log.info("【市局公告】无待办数据");
            }
        }catch (Exception e) {
            log.error("【市局公告】任务执行异常", e);
            throw e;
        }
        return param;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean syncSingleNotice(String noticeUuid) {
        String url = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/public/oaNotice/showOaNoticeDetail.do?oanoUuid=" + noticeUuid;
        log.info("【市局公告】详情请求 URL: {}", url);
        String result = HttpUtil.get(url, 30000);
        log.info("【市局公告】详情响应(长度={}): {}", result.length(), result);
        if (StrUtil.isEmpty(result)) return false;

        // 注意：C# 返回的是 ResultData<NoticeDetail>
        NoticeResult<NoticeDetailDTO> resDetail = JSONUtil.toBean(result, new TypeReference<NoticeResult<NoticeDetailDTO>>() {}, false);

        if (!resDetail.isSuccess() || resDetail.getData() == null) {
            return false;
        }

        return syncNoticeDetail(noticeUuid, resDetail.getData(), false) != null;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long syncSingleMockNotice(String noticeUuid) {
        return syncSingleMockNotice(noticeUuid, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long syncSingleMockNotice(String noticeUuid, boolean repeatable) {
        String effectiveUuid = StrUtil.blankToDefault(noticeUuid, getFirstMockNoticeUuid());
        String result = ResourceUtil.readUtf8Str("mock/detail/" + effectiveUuid + ".json");
        NoticeResult<NoticeDetailDTO> resDetail = JSONUtil.toBean(result,
                new TypeReference<NoticeResult<NoticeDetailDTO>>() {}, false);
        if (!resDetail.isSuccess() || resDetail.getData() == null) {
            return null;
        }
        String docunique = repeatable ? effectiveUuid + "-mock-" + System.currentTimeMillis() : effectiveUuid;
        return syncNoticeDetail(docunique, resDetail.getData(), true);
    }

    public String getFirstMockNoticeUuid() {
        return getMockNoticeUuids(1).get(0);
    }

    public List<String> getMockNoticeUuids(int limit) {
        String result = ResourceUtil.readUtf8Str("mock/notice_list.json");
        NoticeResult<List<OaNoticeDTO>> resList = JSONUtil.toBean(result,
                new TypeReference<NoticeResult<List<OaNoticeDTO>>>() {}, false);
        if (!resList.isSuccess() || CollUtil.isEmpty(resList.getData())) {
            throw new IllegalStateException("mock/notice_list.json 未读取到公告数据");
        }
        int actualLimit = Math.max(1, Math.min(limit, resList.getData().size()));
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < actualLimit; i++) {
            OaNoticeDTO notice = resList.getData().get(i);
            if (StrUtil.isNotBlank(notice.getOanoUuid())) {
                uuids.add(notice.getOanoUuid());
            }
        }
        if (CollUtil.isEmpty(uuids)) {
            throw new IllegalStateException("mock/notice_list.json 未读取到有效公告 UUID");
        }
        return uuids;
    }

    private Long syncNoticeDetail(String noticeUuid, NoticeDetailDTO detail, boolean mockAttachment) {
        FileExchangeDO existExchange = fileExchangeMapper.selectOne(Wrappers.<FileExchangeDO>lambdaQuery()
                .eq(FileExchangeDO::getDocunique, noticeUuid));
        if (existExchange != null) {
            return null;
        }

        OaNoticeDTO notice = detail.getNotice();

        // 3. 准备收文参数
        ReceiveDocSaveReqVO receiveDocDO = new ReceiveDocSaveReqVO();

        // 3.1 基础字段映射
        receiveDocDO.setDocClass("7"); // 收文类型：县市来文
        Long numberReceiveNumber = receiveDocService.generateDocumentSequence("7");
        receiveDocDO.setDocSequence(numberReceiveNumber);

        // 处理发文时间
        LocalDateTime sendDate = LocalDateTime.now().withNano(0);
        if (StrUtil.isNotEmpty(notice.getOanoSendDate())) {
            try {
                sendDate = DateUtil.parse(notice.getOanoSendDate()).toLocalDateTime();
            } catch (Exception e) {
                log.warn("时间解析失败，使用当前时间: {}", notice.getOanoSendDate());
            }
        }
        receiveDocDO.setYear(String.valueOf(sendDate.getYear()));

        String sequenceStr = String.format("%04d", Integer.parseInt(String.valueOf(numberReceiveNumber)));

        // 拼接最终编号：2023-7-0001
        receiveDocDO.setReceiveDocNumber(
                sendDate.getYear()+ "-" + receiveDocDO.getDocClass() + "-" + sequenceStr
        );

        receiveDocDO.setUrgencyDegree("1"); // C# 代码中先设0又设1，最终是1(平件)
        receiveDocDO.setDocRange("PT");     // C# 逻辑：普通收文

        // 来文文号：oanoType + [年份]
        String sendNum = StrUtil.format("{}[{}]", notice.getOanoType(), sendDate.getYear());
        receiveDocDO.setSendDocNumber(sendNum);

        receiveDocDO.setSubject(notice.getOanoTitle());
        if (StrUtil.isNotEmpty(receiveDocDO.getSubject())) {
            receiveDocDO.setSubject(receiveDocDO.getSubject().trim());
        }

        receiveDocDO.setSendDept(notice.getOanoDepName()); // 发文单位
        receiveDocDO.setDocSecondClass(ReceiveDocClassParser.parse(receiveDocDO.getSubject())); // 根据标题解析二级分类

        receiveDocDO.setSendTime(sendDate);
        receiveDocDO.setReceiveTime(LocalDateTime.now().withNano(0));

        // 备注处理 (C# 中有去问号逻辑，这里简化处理)
        // receiveDocDO.setRemark(...);

        // 4. 处理附件
        List<ReceiveDocAttachDO> attachList = new ArrayList<>();
        boolean havePageFile = false; // 是否包含正文文件

        // 4.1 下载列表中的附件
        if (CollUtil.isNotEmpty(detail.getFileList())) {
            Map<String, String> validFileNamesMap = new HashMap<>();
            for (OaFileDTO oaFile : detail.getFileList()) {
                String fileName = oaFile.getFileName();
                if ("电子公告页.pdf".equals(fileName)) {
                    havePageFile = true;
                }

                String baseName = FileUtil.mainName(fileName); // Hutool: 获取无后缀文件名
                String extName = FileUtil.extName(fileName);

                if (validFileNamesMap.containsKey(baseName)) {
                    // C# 逻辑：如果已存在同名key，且当前文件是 doc/docx，则覆盖之前的（优先保留Word）
                    if ("doc".equalsIgnoreCase(extName) || "docx".equalsIgnoreCase(extName)) {
                        validFileNamesMap.put(baseName, fileName);
                    }
                } else {
                    validFileNamesMap.put(baseName, fileName);
                }
            }
            for (OaFileDTO oaFile : detail.getFileList()) {
                String baseName = FileUtil.mainName(oaFile.getFileName());

                // 检查当前文件是否是 Map 中记录的那个“优胜者”
                String targetFileName = validFileNamesMap.get(baseName);
                if (!StrUtil.equals(targetFileName, oaFile.getFileName())) {
                    continue; // 跳过被淘汰的文件
                }

                ReceiveDocAttachDO attach = downloadAndUploadFile(oaFile.getUuid(), oaFile.getFileName(), mockAttachment);
                if (attach != null) {
                    attachList.add(attach);
                }
            }
        }

        // 4.2 处理正文 (HTML 转文件)
        // C# 逻辑：如果没有"电子公告页.pdf"且有 HTML 内容，生成 word。
        // Java 简化逻辑：生成 .html 文件 (Ruoyi 预览组件通常支持 html)
        if (!havePageFile && StrUtil.isNotEmpty(notice.getOanoContent())) {
            // C# 生成的是 "电子公告页.doc"
            ReceiveDocAttachDO contentAttach = createHtmlContentFile(notice.getOanoContent());
            if (contentAttach != null) {
                attachList.add(contentAttach);
            }
        }

        receiveDocDO.setFileList(attachList);

        // 5. 创建收文 (入库 + 启动流程)
        // 使用配置的默认用户ID启动
        Long userId = Long.valueOf(configApi.getConfigValueByKey(DEFAULT_USER_ID));
        Long receiveDocId = receiveDocService.saveJobReceiveDoc(userId, receiveDocDO);

        // 6. 记录 FileExchange (映射关系)
        FileExchangeSaveReqVO exchangeVO = new FileExchangeSaveReqVO();
        exchangeVO.setOperationDate(LocalDateTime.now().withNano(0));
        exchangeVO.setOperationPerson("系统自动");
        exchangeVO.setOperationInformation("通知公告");
        exchangeVO.setOperationType((short) 2);
        exchangeVO.setDocId(receiveDocId);
        exchangeVO.setSendDocNumber(receiveDocDO.getSendDocNumber());
        exchangeVO.setSubject(receiveDocDO.getSubject());
        exchangeVO.setDocunique(noticeUuid); // 关键：保存外部UUID

        fileExchangeService.createFileExchange(exchangeVO);

        return receiveDocId;
    }

    /**
     * 下载附件
     */
    private ReceiveDocAttachDO downloadAndUploadFile(String fileUuid, String fileName) {
        return downloadAndUploadFile(fileUuid, fileName, false);
    }

    private ReceiveDocAttachDO downloadAndUploadFile(String fileUuid, String fileName, boolean mockAttachment) {
        try {
            byte[] fileBytes;
            if (mockAttachment) {
                fileBytes = ResourceUtil.readBytes("mock/test.pdf");
            } else {
                // C# Url: /public/oaNotice/loadFile.do?CMD=DF&uuid=...
                String downloadUrl = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/public/oaNotice/loadFile.do?CMD=DF&uuid=" + fileUuid;
                log.info("【市局公告】附件下载请求 URL: {}", downloadUrl);
                fileBytes = HttpUtil.downloadBytes(downloadUrl);
            }
            if (fileBytes == null || fileBytes.length == 0) return null;

            // 上传到 FileService
            FileDO fileDO = fileService.createFileReturnId(fileBytes, fileName, null, null);

            ReceiveDocAttachDO attachDO = new ReceiveDocAttachDO();
            attachDO.setAttachFileId(fileDO.getId());
            attachDO.setAttachFileName(fileName);
            attachDO.setShowType((short) 0);
            return attachDO;

        } catch (Exception e) {
            log.error("公告附件下载失败: {}", fileName, e);
            return null;
        }
    }

    /**
     * 将 HTML 内容生成为文件
     */
    private ReceiveDocAttachDO createHtmlContentFile(String htmlContent) {
        try {
            // 1. 设置文件名 (C# 逻辑是 .doc)
            String fileName = "电子公告页.doc";

            // 2. 预处理 HTML
            // Aspose 对 HTML 容错性很好，但为了防止中文乱码，建议包裹简单的 head 并指定编码
            // 如果你的服务器是 Linux 且没有中文字体，生成的 Word 可能会乱码，需要配置 FontSettings
            String formattedHtml = "<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8' /></head><body>"
                    + htmlContent + "</body></html>";

            // 3. 创建 Aspose 文档对象 (内存中)
            Document doc = new Document();
            DocumentBuilder builder = new DocumentBuilder(doc);

            // 4. 插入 HTML
            // 这是核心方法，能够解析 HTML 中的表格、字体、图片等
            builder.insertHtml(formattedHtml);

            // 5. 保存为字节流
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            // SaveFormat.DOC 对应 .doc 格式 (Word 97-2003)
            // 如果想生成 .docx，这里改用 SaveFormat.DOCX，并将文件名后缀改为 .docx
            doc.save(outStream, SaveFormat.DOC);

            byte[] fileBytes = outStream.toByteArray();
            outStream.close();

            // 6. 上传到 RuoYi 的文件服务
            // MIME 类型使用 application/msword 对应 .doc
            FileDO fileDO = fileService.createFileReturnId(fileBytes, fileName, null, null);

            // 7. 构建附件对象返回
            ReceiveDocAttachDO attachDO = new ReceiveDocAttachDO();
            attachDO.setAttachFileId(fileDO.getId());
            attachDO.setAttachFileName(fileName);
            attachDO.setShowType((short) 0);

            return attachDO;



        } catch (Exception e) {
            log.error("生成公告正文 Word 文件失败 (Aspose)", e);
            // 这里根据业务需要，如果不生成文件就算失败，可以返回 null；
            // 也可以选择生成一个只有纯文本的 txt 作为兜底。
            return null;
        }
    }

}
