package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
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
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.fileexchange.FileExchangeMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.city.*;
import cn.iocoder.yudao.module.bpm.service.fileexchange.FileExchangeService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CityDocJob implements JobHandler {

    static final String RECEIVE_CITY_KEY = "url.receive.city";

    static final String RECEIVE_UUID_KEY = "key.receive.natural";

    static final String DEFAULT_USER_ID = "key.receive.user";

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
            log.info("当前租户[{}]非目标租户，跳过市局收文同步", currentTenantId);
            return "跳过非目标租户";
        }
        try {
            String listUrl = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/oa/api/public/service/showCoreExchgappData.do"
                    + "?ceadReceiverUuid=" + configApi.getConfigValueByKey(RECEIVE_UUID_KEY) + "&ceadState=0";

            log.info("【市局公文】列表请求 URL: {}", listUrl);
            String result = HttpUtil.get(listUrl, 30000);
            log.info("【市局公文】列表响应(长度={}): {}", result.length(), result);
            if (StrUtil.isEmpty(result)){
                log.error("【市局公文】接口返回结果为空");
                return "【市局公文】接口返回结果为空";
            }
            RemoteDocResult<List<RemoteDocItem>> resList = JSONUtil.toBean(result, new TypeReference<RemoteDocResult<List<RemoteDocItem>>>() {}, false);
            if (resList.isSuccess() && CollUtil.isNotEmpty(resList.getData())) {
                int successCount = 0;
                for (RemoteDocItem item : resList.getData()) {
                    try {
                        boolean synced = syncSingleDoc(item.getCeadUuid());
                        if (synced) {
                            successCount++;
                        }
                    } catch (Exception e) {
                        log.error("同步单条公文失败, UUID: {}", item.getCeadUuid(), e);

                    }
                }
                log.info("同步完成，总数：{}，成功：{}", resList.getData().size(), successCount);

                return String.format("同步完成，总数：%d，成功：%d", resList.getData().size(), successCount);
            }
            else{
                log.error("【市局公文】接口返回结果为空");
            }

        } catch (Exception e) {
            log.error("【市局公文】同步任务发生严重异常，请求可能失败！", e);
        }

        return param;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean syncSingleDoc(String ceadUuid) {
        // 1. 检查是否已经存在 (查询 FileExchange 表)
        // 假设 FileExchangeDO 中有一个字段存储外部UUID，例如 docUnique 或 exchangeId
        // 这里假设你在 FileExchangeDO 中添加了 `docUnique` 字段，或者使用 remarks 存储

        FileExchangeDO existExchange = fileExchangeMapper.selectOne(Wrappers.<FileExchangeDO>lambdaQuery()
                .eq(FileExchangeDO::getDocunique, ceadUuid));

        String url = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/oa/api/public/service/getSourceAndConvertData.do?ceadUuid=" + ceadUuid;
        log.info("【市局公文】详情请求 URL: {}", url);
        String result = HttpUtil.get(url, 30000);
        log.info("【市局公文】详情响应(长度={}): {}", result.length(), result);
        RemoteDocResult<RemoteDocDetail> resDetail = JSONUtil.toBean(result, new TypeReference<RemoteDocResult<RemoteDocDetail>>() {}, false);

        if (!resDetail.isSuccess() || resDetail.getData() == null) {
            return false;
        }

        RemoteDocDetail detail = resDetail.getData();
        RemoteSourceData sourceData = detail.getSourceData();

        if (existExchange != null) {
            // C# 逻辑复现：已签收的办件补充附件完整性
            Long receiveDocId = existExchange.getDocId(); // 假设你的 FileExchangeDO 关联收文ID的字段是 docId
            if (receiveDocId != null) {
                // 检查并下载缺失的附件
                syncMissingAttachments(receiveDocId, sourceData.getWkflwFiles());
            }

            // 更新远程状态为已签收
            updateRemoteState(ceadUuid);
            return false;
        }

        RemoteBean bean = detail.getBean();
        // 3. 准备收文参数 (receiveDocDO)
        ReceiveDocSaveReqVO receiveDocDO  =  new ReceiveDocSaveReqVO();
        receiveDocDO.setDocClass("7");
        receiveDocDO.setYear(detail.getSourceData().getOafdFileyear());
        Long numberReceiveNumber = receiveDocService.generateDocumentSequence("7",receiveDocDO.getYear());
        receiveDocDO.setDocSequence(numberReceiveNumber);

        receiveDocDO.setReceiveDocNumber(DateTime.now().year() + "-" +receiveDocDO.getDocClass()+ "-" + numberReceiveNumber);
        String urgency = "0";
        String remoteUrgency = sourceData.getOafdFileremergency();
        if (StrUtil.isNotEmpty(remoteUrgency)) {
            if (remoteUrgency.contains("平")) urgency = "1";
            else if (remoteUrgency.contains("急")) urgency = "2";
        }
        receiveDocDO.setUrgencyDegree(urgency);
        String fileCode = sourceData.getOafdFilecode();
        if (StrUtil.isNotEmpty(fileCode) && fileCode.contains("/")) {
            fileCode = fileCode.substring(fileCode.lastIndexOf('/') + 1);
        }
        receiveDocDO.setSendDocNumber(fileCode + "[" + sourceData.getOafdFileyear() + "]" + sourceData.getOafdFileno() + "号");
        receiveDocDO.setSubject(bean.getCeadTitle());
        if(StrUtil.isNotEmpty(receiveDocDO.getSubject())){
            receiveDocDO.setSubject(receiveDocDO.getSubject().trim());
        }
        receiveDocDO.setSendDept(sourceData.getOafdFileoragnise());
        //先写死
        receiveDocDO.setDocSecondClass(getDocClass(receiveDocDO.getSubject()));
        try {
            receiveDocDO.setSendTime(DateUtil.parse(bean.getCeadCreateTime()).toLocalDateTime());
        } catch (Exception e) {
            receiveDocDO.setSendTime(LocalDateTime.now());
        }
        receiveDocDO.setReceiveTime(LocalDateTime.now());
        receiveDocDO.setRemark(sourceData.getOafdRemark());
        receiveDocDO.setDocRange("PT"); // C# 中写死


        // 3.5 处理附件 (重点：下载流并转存到 FileService)
        List<ReceiveDocAttachDO> attachList = new ArrayList<>();
        if (CollUtil.isNotEmpty(sourceData.getWkflwFiles())) {
            for (RemoteWkflwFile remoteFile : sourceData.getWkflwFiles()) {
                ReceiveDocAttachDO attach = downloadAndUploadFile(remoteFile);
                if (attach != null) {
                    attachList.add(attach);
                }
            }
        }
        receiveDocDO.setFileList(attachList);

        // 3.6 设置流程启动人等参数
        // 这些参数在 Service.createReceiveDoc 中会用到
        // 如果你的流程需要指定下一节点处理人，需要在这里设置
        // createReq.setStartUserSelectAssignees(...)

        // 4. 创建收文 (这一步会插入数据库并启动流程)
        Long receiveDocId = receiveDocService.saveReceiveDoc(Long.valueOf(configApi.getConfigValueByKey(DEFAULT_USER_ID)), receiveDocDO);

        // 5. 记录到 FileExchange 表 (建立映射关系)
        FileExchangeSaveReqVO exchangeVO = new FileExchangeSaveReqVO();
        exchangeVO.setOperationDate(LocalDateTime.now());
        exchangeVO.setOperationPerson("系统自动");
        exchangeVO.setOperationInformation("市局公文");
        exchangeVO.setOperationType((short) 2); // 2 代表接收
        exchangeVO.setDocId(receiveDocId);
        exchangeVO.setSendDocNumber(receiveDocDO.getSendDocNumber());
        exchangeVO.setSubject(receiveDocDO.getSubject());
        exchangeVO.setDocunique(ceadUuid); // 关键：保存外部UUID
        fileExchangeService.createFileExchange(exchangeVO);

////        // 6. 更新远程状态
//        updateRemoteState(ceadUuid);

        return true;
    }

    /**
     * 下载远程附件并上传到本系统
     */
    private ReceiveDocAttachDO downloadAndUploadFile(RemoteWkflwFile remoteFile) {
        try {
            String downloadUrl = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/oa/api/public/risen/wkflw/download.do?CMD=DF&TYPE=stream&uuid=" + remoteFile.getWkfileUuid();
            log.info("【市局公文】附件下载请求 URL: {}", downloadUrl);

            // 1. 下载字节流
            byte[] fileBytes = HttpUtil.downloadBytes(downloadUrl);

            if (fileBytes == null || fileBytes.length == 0) return null;

            // 2. 上传到 FileService (获取内部 FileDO)
            // 假设 remoteFile.getWkfileName() 包含后缀，如果没有需要处理
            String fileName = remoteFile.getWkfileName();
            // 使用 createFileReturnId 获取完整对象，我们需要 ID 和 URL
            FileDO fileDO = fileService.createFileReturnId(fileBytes, fileName, null, null);

            // 3. 构建收文附件对象
            ReceiveDocAttachDO attachDO = new ReceiveDocAttachDO();
            attachDO.setAttachFileId(fileDO.getId());
            attachDO.setAttachFileName(fileName);
            attachDO.setShowType((short) 0);
            // attachDO.setReceiveDocId(...) // 这个在 Service 中会统一设置
            return attachDO;

        } catch (Exception e) {
            log.error("附件下载转存失败: {}", remoteFile.getWkfileName(), e);
            return null;
        }
    }

    /**
     * 调用远程接口更新状态为已签收 (1)
     */
    private void updateRemoteState(String uuid) {
        try {
            String url = configApi.getConfigValueByKey(RECEIVE_CITY_KEY) + "/oa/api/public/service/updateState.do?ceadUuid=" + uuid + "&ceadState=1";
            log.info("【市局公文】更新状态请求 URL: {}", url);
            HttpUtil.get(url, 10000);
        } catch (Exception e) {
            log.warn("更新市公文状态失败: {}", uuid, e);
        }
    }

    private void syncMissingAttachments(Long receiveDocId, List<RemoteWkflwFile> remoteFiles) {
        if (CollUtil.isEmpty(remoteFiles)) {
            return;
        }

        // 1. 获取当前收文已有的附件列表
        List<ReceiveDocAttachDO> existingAttaches = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);

        // 提取已有附件的文件名集合，用于比对 (C# 代码是根据文件名判重的)
        Set<String> existingFileNames = existingAttaches.stream()
                .map(ReceiveDocAttachDO::getAttachFileName)
                .collect(Collectors.toSet());

        // 2. 遍历远程附件列表
        for (RemoteWkflwFile remoteFile : remoteFiles) {
            String remoteFileName = remoteFile.getWkfileName();
            // 如果本地没有这个文件
            if (!existingFileNames.contains(remoteFileName)) {
                log.info("发现新附件，开始补充下载: {}", remoteFileName);

                // 3. 下载并上传到文件服务
                ReceiveDocAttachDO newAttach = downloadAndUploadFile(remoteFile);

                if (newAttach != null) {
                    // 4. 补全关联ID并插入数据库
                    newAttach.setReceiveDocId(receiveDocId);
                    receiveDocAttachMapper.insert(newAttach);
                }
            }
        }
    }

    private String getDocClass(String title) {
        // 1. 防御性判断
        if (StrUtil.isEmpty(title)) {
            return "";
        }

        // 2. 只有长度大于 4 才进行截取判断 (保持 C# 逻辑)
        if (title.length() > 4) {
            // 3. 获取字典数据列表
            // 注意：请将 "doc_second_class" 替换为你实际在 RuoYi 字典管理中配置的 字典类型
            List<DictDataRespDTO> dictList = DictFrameworkUtils.getDictDataList("doc_class");

            if (dictList == null || dictList.isEmpty()) {
                return "";
            }

            // 4. 截取最后四个字
            String suffix = title.substring(title.length() - 4);

            // 5. 遍历字典进行匹配
            for (DictDataRespDTO dict : dictList) {
                if (suffix.contains(dict.getLabel())) {
                    return dict.getLabel();
                }
            }
        }

        return "";
    }

}
