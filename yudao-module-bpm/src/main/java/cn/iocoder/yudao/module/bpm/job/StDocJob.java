package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
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
import cn.iocoder.yudao.module.bpm.service.fileexchange.FileExchangeService;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.st.STResult;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.st.RecordFileDTO;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.st.RecordDTO;
import cn.iocoder.yudao.module.bpm.job.Dto.receive.st.DQSList;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Component
public class StDocJob implements JobHandler {

    static final String ST_SERVICE_IP_KEY = "url.st.service";
    static final String ST_UNIT_ID_KEY = "key.st.unit.id";
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
    private FileService fileService;
    @Resource
    private ReceiveDocAttachMapper receiveDocAttachMapper;

    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过省厅收文同步", currentTenantId);
            return "跳过非目标租户";
        }
        log.info("开始省厅收文同步↓↓↓");
        try {
            // 获取省厅接口地址和单位ID
            String stServiceIp = configApi.getConfigValueByKey(ST_SERVICE_IP_KEY);
            String stUnitId = configApi.getConfigValueByKey(ST_UNIT_ID_KEY);

            // 对应 C# 中的 MD5Encrypt(_sign)
            String sign = SecureUtil.md5(stUnitId+"zrzytoa");

            String url = stServiceIp + "/api6/infoexchange-table/DQSList";

            // 构造请求参数获取待签收列表
            String paramStr = String.format("{\"id\":\"%s\",\"sign\":\"%s\",\"page\":1,\"limit\":10000}", stUnitId, sign);

            // 👇 --- 新增：打印 CURL 命令到日志 --- 👇
            log.info("====== [DQSList 接口 CURL] ======\ncurl -X POST \"{}\" \\\n     -H \"Content-Type: application/json\" \\\n     -d '{}'", url, paramStr);
            // 👆 ----------------------------------- 👆
            String result = HttpUtil.post(url, paramStr, 30000);
            log.info("【省厅收文】列表响应(长度={}): {}", result.length(), result);
//            String result = ResourceUtil.readUtf8Str("mock/st_list.json");;
            // 解析 JSON
            STResult<DQSList> stRes = JSONUtil.toBean(result, new cn.hutool.core.lang.TypeReference<STResult<DQSList>>() {}, false);

            if (stRes == null || stRes.getCode() != 200) {
                log.error("222获取待签收收文失败=>{}", stRes != null ? stRes.getMsg() : "未知错误");
                return "获取待签收收文失败";
            }

            if (stRes.getData() == null || CollUtil.isEmpty(stRes.getData().getRecords())) {
                log.info("222无可待签收收文");
                return "无可待签收收文";
            }

            int count = 0;
            List<RecordDTO> records = stRes.getData().getRecords();
            for (RecordDTO rec : records) {
                try {
                    // 同步单条记录
                    boolean success = syncSingleReceive(rec, stServiceIp, stUnitId, sign);
                    if (success) {
                        count++;
                    }
                } catch (Exception e) {
                    log.error("{} 导入失败，错误：{}", rec.getBt(), e.getMessage(), e);
                }
            }

            log.info("222已成功签收收文 {} 个，失败 {} 个！", count, (records.size() - count));
            return String.format("同步完成，总数：%d，成功：%d", records.size(), count);

        } catch (Exception e) {
            log.error("【省厅收文】任务执行异常", e);
            throw e;
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public boolean syncSingleReceive(RecordDTO rec, String stServiceIp, String stUnitId, String sign) {
        // 1. 判断公文交换记录是否存在
        FileExchangeDO existExchange = fileExchangeMapper.selectOne(Wrappers.<FileExchangeDO>lambdaQuery()
                .eq(FileExchangeDO::getDocunique, rec.getInfoexchangeid()));

        if (existExchange != null) {
            Long receiveDocId = existExchange.getDocId();
            if (receiveDocId != null) {
                supplementAttachments(receiveDocId, rec, stServiceIp, stUnitId, sign);
            }
            return false;

        }

        // 2. 创建收文信息
        ReceiveDocSaveReqVO receiveDocDO = new ReceiveDocSaveReqVO();

        // 标题特殊字符转换
        String subject = rec.getBt();
        if (StrUtil.isNotEmpty(subject)) {
            subject = subject.replace("&#40;", "（")
                    .replace("& #40;", "（")
                    .replace("&#41;", "）")
                    .replace("& #41;", "）");
        }
        receiveDocDO.setSubject(subject);

        // 收文类型默认是 41
        receiveDocDO.setDocClass("41");
        receiveDocDO.setDocSecondClass(ReceiveDocClassParser.parse(receiveDocDO.getSubject()));
        Long numberReceiveNumber = receiveDocService.generateDocumentSequence("41");
        receiveDocDO.setDocSequence(numberReceiveNumber);
        receiveDocDO.setYear(String.valueOf(LocalDateTime.now().getYear()));

        // 生成流水号，例如：2024-41-0001
        String sequenceStr = String.format("%04d", Integer.parseInt(String.valueOf(numberReceiveNumber)));
        receiveDocDO.setReceiveDocNumber(receiveDocDO.getYear() + "-41-" + sequenceStr);

        // 紧急程度
        String urgencyLabel = rec.getJjcd();
        if (StrUtil.isNotEmpty(urgencyLabel)) {
            // 注意：请将 "receive_urgency_degree" 替换为你系统字典管理中实际的字典类型编码 (dict_type)
            String  dictValue = DictFrameworkUtils.parseDictDataValue("emergency_degree", urgencyLabel);
            if (dictValue != null) {
                receiveDocDO.setUrgencyDegree(dictValue);
            } else {
                receiveDocDO.setUrgencyDegree(""); // 字典中未找到则置空
            }
        } else {
            receiveDocDO.setUrgencyDegree("");
        }
        receiveDocDO.setSendDocNumber(rec.getFwzh()); // 发文字号
        receiveDocDO.setSendDept(rec.getFwdw());      // 发文单位

        // 发文日期
        if (StrUtil.isNotEmpty(rec.getFwrq())) {
            try {
                receiveDocDO.setSendTime(DateUtil.parse(rec.getFwrq()).toLocalDateTime());
            } catch (Exception e) {
                log.warn("发文日期解析失败: {}", rec.getFwrq());
            }
        }
        receiveDocDO.setReceiveTime(LocalDateTime.now().withNano(0));

        // 备注及联系方式合并
        String remark = rec.getBz();
        if (StrUtil.isNotEmpty(rec.getLxfs())) {
            remark = (remark == null ? "" : remark) + "\r\n联系方式：" + rec.getLxfs();
        }
        receiveDocDO.setRemark(remark);
        receiveDocDO.setDocRange("PT"); // 存储路径范围 PT

        // ================== 核心修改点 ==================
        // 3. 附件下载与装填 (完全参照 CityNoticeJob 逻辑)
        List<ReceiveDocAttachDO> attachList = getAndDownloadAttachments(rec, stServiceIp, stUnitId, sign);
        receiveDocDO.setFileList(attachList);

        // 4. 统一保存收文记录并启动流程
        Long userId = Long.valueOf(configApi.getConfigValueByKey(DEFAULT_USER_ID));
        Long receiveDocId = receiveDocService.saveJobReceiveDoc(userId, receiveDocDO);
        // ===============================================

        // 5. 创建交换记录 (系统自动/OperationType=2)
        FileExchangeSaveReqVO exchangeVO = new FileExchangeSaveReqVO();
        exchangeVO.setOperationDate(LocalDateTime.now().withNano(0));
        exchangeVO.setOperationPerson("系统自动");
        exchangeVO.setOperationInformation("省厅收文");
        exchangeVO.setOperationType((short) 2);
        exchangeVO.setDocId(receiveDocId);
        exchangeVO.setSendDocNumber(receiveDocDO.getSendDocNumber());
        exchangeVO.setSubject(receiveDocDO.getSubject());
        exchangeVO.setDocunique(rec.getInfoexchangeid());
        fileExchangeService.createFileExchange(exchangeVO);

        // 6. C# 签收接口 QSJK
        String signOffParam = String.format("{\"id\":\"%s\",\"sign\":\"%s\",\"infoexchangeid\":\"%s\"}",
                stUnitId, sign, rec.getInfoexchangeid());
        String signOffUrl = stServiceIp + "/api6/infoexchange-table/QSJK";
        log.info("【省厅收文】签收请求 URL: {}, Body: {}", signOffUrl, signOffParam);
        String signOffResult = HttpUtil.post(signOffUrl, signOffParam, 10000);
        STResult<Object> qsjk = JSONUtil.toBean(signOffResult, new cn.hutool.core.lang.TypeReference<STResult<Object>>() {}, false);
        if (qsjk != null && qsjk.getCode() == 200) {
            log.info("省厅办件：{} 已签收!", rec.getBt());
        } else {
            log.error("省厅办件：{} 签收失败!", rec.getBt());
        }

        return true;
    }

    /**
     * 获取附件列表并逐个下载装配成 List
     */
    private List<ReceiveDocAttachDO> getAndDownloadAttachments(RecordDTO rec, String stServiceIp, String stUnitId, String sign) {
        List<ReceiveDocAttachDO> attachList = new ArrayList<>();
        try {
            // 获取附件列表请求
            String paramStr = String.format("{\"id\":\"%s\",\"sign\":\"%s\",\"infoexchangeid\":\"%s\"}",
                    stUnitId, sign, rec.getInfoexchangeid());
            String url = stServiceIp + "/api6/infoexchange-table/HQFJ";

            // 👇 --- 新增：打印 CURL 命令到日志 --- 👇
            log.info("====== [HQFJ 获取附件列表 CURL] ======\ncurl -X POST \"{}\" \\\n     -H \"Content-Type: application/json\" \\\n     -d '{}'", url, paramStr);
            // 👆 ----------------------------------- 👆
            String result = HttpUtil.post(url, paramStr, 30000);
//            String mockFileName = "mock/st_detail/" + rec.getInfoexchangeid() + ".json";
//            String result = ResourceUtil.readUtf8Str(mockFileName);

            STResult<List<RecordFileDTO>> stRes = JSONUtil.toBean(result,
                    new cn.hutool.core.lang.TypeReference<STResult<List<RecordFileDTO>>>() {}, false);

            if (stRes != null && stRes.getCode() == 200 && CollUtil.isNotEmpty(stRes.getData())) {
                for (RecordFileDTO rFile : stRes.getData()) {
                    // 调用下载单文件方法
                    ReceiveDocAttachDO attach = downloadAndUploadFile(rFile, stServiceIp);
                    if (attach != null) {
                        attachList.add(attach);
                    }
                }
            } else {
                log.warn("办件：{}，获取附件失败：{}", rec.getBt(), stRes != null ? stRes.getMsg() : "Empty Response");
            }
        } catch (Exception e) {
            log.error("办件：{}，获取附件失败：{}", rec.getBt(), e.getMessage(), e);
        }
        return attachList;
    }

    /**
     * 下载单个附件并上传至统一文件服务
     */
    private ReceiveDocAttachDO downloadAndUploadFile(RecordFileDTO rFile, String stServiceIp) {
        try {
            String downloadUrl = stServiceIp + "/api1/download?folder=INPUT_FOLDER&attachment_id=" + rFile.getFjid();
            // 👇 --- 新增：打印 CURL 命令到日志 --- 👇
            // 这里加了 -o 参数，方便你在 Linux 测试时直接保存为文件
            log.info("====== [Download 下载附件 CURL] ======\ncurl -X GET \"{}\" -o \"{}\"", downloadUrl, rFile.getRname());
            // 👆 ----------------------------------- 👆
            byte[] fileBytes = HttpUtil.downloadBytes(downloadUrl);
//            byte[] fileBytes = ResourceUtil.readBytes("mock/test.pdf");

            if (fileBytes != null && fileBytes.length > 0) {
                // 过滤不合法的文件名字符
                String safeFileName = rFile.getRname().replaceAll("[\\\\/:*?\"<>|]", "");

                // 上传至统一文件服务
                FileDO fileDO = fileService.createFileReturnId(fileBytes, safeFileName, null, null);

                // 构建附件对象返回
                ReceiveDocAttachDO attachDO = new ReceiveDocAttachDO();
                attachDO.setAttachFileId(fileDO.getId());
                attachDO.setAttachFileName(safeFileName);
                attachDO.setShowType((short) 0);
                return attachDO;
            }
        } catch (Exception e) {
            log.error("省厅附件下载失败: {}", rFile.getRname(), e);
        }
        return null;
    }

    /**
     * 已存在办件的附件完整性补充
     */
    private void supplementAttachments(Long receiveDocId, RecordDTO rec, String stServiceIp, String stUnitId, String sign) {
        try {
            // 1. 获取省厅接口的附件列表
            String paramStr = String.format("{\"id\":\"%s\",\"sign\":\"%s\",\"infoexchangeid\":\"%s\"}",
                    stUnitId, sign, rec.getInfoexchangeid());
            String url = stServiceIp + "/api6/infoexchange-table/HQFJ";
            log.info("【省厅收文】补充附件-获取附件列表请求 URL: {}, Body: {}", url, paramStr);
            String result = HttpUtil.post(url, paramStr, 30000);
//            String mockFileName = "mock/st_detail/" + rec.getInfoexchangeid() + ".json";
//            String result = ResourceUtil.readUtf8Str(mockFileName);

            STResult<List<RecordFileDTO>> stRes = JSONUtil.toBean(result,
                    new cn.hutool.core.lang.TypeReference<STResult<List<RecordFileDTO>>>() {}, false);

            if (stRes != null && stRes.getCode() == 200 && CollUtil.isNotEmpty(stRes.getData())) {

                // 2. 查询本地数据库已存在的附件
                List<ReceiveDocAttachDO> existAttaches = receiveDocAttachMapper.selectList(
                        Wrappers.<ReceiveDocAttachDO>lambdaQuery()
                                .eq(ReceiveDocAttachDO::getReceiveDocId, receiveDocId) // 假设实体类字段为 receiveDocId
                );

                List<String> existFileNames = existAttaches.stream()
                        .map(ReceiveDocAttachDO::getAttachFileName)
                        .collect(Collectors.toList());

                // 3. 遍历接口附件比对
                for (RecordFileDTO rFile : stRes.getData()) {
                    String safeFileName = rFile.getRname().replaceAll("[\\\\/:*?\"<>|]", "");

                    // 若本地不存在该文件名，则下载并插入记录
                    if (!existFileNames.contains(safeFileName)) {
                        ReceiveDocAttachDO attach = downloadAndUploadFile(rFile, stServiceIp);
                        if (attach != null) {
                            attach.setReceiveDocId(receiveDocId);
                            receiveDocAttachMapper.insert(attach);
                            log.info("办件：{} 成功补充缺失附件：{}", rec.getBt(), safeFileName);
                        }
                    }
                }
            } else {
                log.warn("办件：{}，获取补充附件失败：{}", rec.getBt(), stRes != null ? stRes.getMsg() : "Empty Response");
            }
        } catch (Exception e) {
            log.error("办件：{}，补充附件异常：{}", rec.getBt(), e.getMessage(), e);
        }
    }

}
