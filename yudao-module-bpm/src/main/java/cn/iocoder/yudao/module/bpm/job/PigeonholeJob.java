package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;
import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.ReceiveFileRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.job.Dto.pigeonhole.*;
import cn.iocoder.yudao.module.bpm.service.receivedoc.ReceiveDocService;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Slf4j
@Component
public class PigeonholeJob implements JobHandler {

    @Resource
    private ConfigApi configApi;

    @Resource
    private ReceiveDocMapper receiveDocMapper;

    @Resource
    private ReceiveDocAttachMapper receiveDocAttachMapper;

    // RuoYi-Vue-Pro 标准的流程任务服务，用于获取审批历史
    @Resource
    private BpmTaskService bpmTaskService;

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private DeptApi deptApi;

    @Resource
    private HistoryService historyService;

    @Resource
    private FileService fileService;

    @Resource
    private ReceiveDocService receiveDocService;

    @Resource
    private FileConfigService fileConfigService;


    @TenantJob
    @Override
    public String execute(String param) throws Exception {
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null || !currentTenantId.equals(1L)) {
            log.info("当前租户[{}]非目标租户，跳过档案归档任务", currentTenantId);
            return "跳过非目标租户";
        }
        try{
            log.info("开始执行档案归档任务...");
            int receiveSuccess = 0;
            try {
                FileConfigDO fileConfigDO = fileConfigService.getFileConfig(29L);
                LocalFileClientConfig localConfig = (LocalFileClientConfig) fileConfigDO.getConfig();
                receiveSuccess = synchronizeReceive(localConfig.getBasePath());
            } catch (Exception e) {
                log.error("收文档案归档时发生错误", e);
            }

            String msg = StrUtil.format("档案归档完成。收文成功：{} 个", receiveSuccess);
            log.info(msg);
            return msg;

        }catch (Exception e) {
            log.error("【档案归档任务】执行异常", e);
            throw e;
        }
    }
    // ============================ 收文归档逻辑 ============================
    private int synchronizeReceive(String baePath) {
        int success = 0;
        int limitCount = NumberUtil.parseInt(StrUtil.blankToDefault(configApi.getConfigValueByKey("archive.execute.timeCount"), "100"));

        // 1. 使用 MyBatis-Plus 直接查询待归档的 ReceiveDocDO
//        List<ReceiveDocDO> candidateDocs = receiveDocMapper.selectList(Wrappers.<ReceiveDocDO>lambdaQuery()
//                .and(w -> w.in(ReceiveDocDO::getDocRange, Arrays.asList("PT", "DZGG")).or().isNull(ReceiveDocDO::getDocRange))
//                .and(w -> w.eq(ReceiveDocDO::getIfpigeonhold, 0).or().isNull(ReceiveDocDO::getIfpigeonhold))
//                .orderByAsc(ReceiveDocDO::getReceiveTime)
//                .last("LIMIT " + (limitCount * 5)));
        List<Long> idList = new ArrayList<>();
        idList.add(2427L);
        List<ReceiveDocDO> unarchivedDocs = receiveDocMapper.selectList(Wrappers.<ReceiveDocDO>lambdaQuery()
                .in(ReceiveDocDO::getId, idList));

//        if (CollUtil.isEmpty(candidateDocs)) {
//            log.info("【收文归档】无待办数据");
//            return 0;
//        }
//
//        Set<String> processInstanceIds = candidateDocs.stream()
//                .map(ReceiveDocDO::getProcessInstanceId)
//                .filter(StrUtil::isNotBlank)
//                .collect(Collectors.toSet());
//
//        Set<String> finishedProcessIds;
//        if (CollUtil.isNotEmpty(processInstanceIds)) {
//            finishedProcessIds = historyService.createHistoricProcessInstanceQuery()
//                    .processInstanceIds(processInstanceIds)
//                    .finished() // 核心条件：只查询流程已经彻底结束的
//                    .list()
//                    .stream()
//                    .map(HistoricProcessInstance::getId)
//                    .collect(Collectors.toSet());
//        } else {
//            finishedProcessIds = new java.util.HashSet<>(); // 分支中初始化
//        }
//
//        List<ReceiveDocDO> unarchivedDocs = candidateDocs.stream()
//                .filter(doc -> finishedProcessIds.contains(doc.getProcessInstanceId()))
//                .limit(limitCount)
//                .collect(Collectors.toList());
//
//        if (CollUtil.isEmpty(unarchivedDocs)) {
//            log.info("【收文归档】当前有公文数据，但所属流程均未办理结束，暂不归档");
//            return 0;
//        }

        String unitName = StrUtil.blankToDefault(configApi.getConfigValueByKey("archive.unit.name"), "义乌市自然资源和规划局");
        String qzh = StrUtil.blankToDefault(configApi.getConfigValueByKey("archive.qzh"), "J240");

        for (ReceiveDocDO doc : unarchivedDocs) {
            // 假设你的 ReceiveDocDO 中保存了绑定的 BPM 流程实例 ID
            String processInstanceId = doc.getProcessInstanceId();
            String projectId = doc.getProjectId(); // 对应原 C# 的业务标识

            String year = doc.getYear();
            String number = "";
            String docNum = doc.getReceiveDocNumber();
            if (StrUtil.isNotEmpty(docNum) && docNum.contains("-")) {
                String[] arr = docNum.split("-");
                if (arr.length >= 3) {
                    year = arr[0];
                    number = arr[2];
                }
            }

            String bgqx = "Y";
            String dh = qzh + "-WS·" + year + "-" + bgqx + "-BGS-" + number;

            // 2. 构造主对象
            SubmitGd submitGd = new SubmitGd();
            submitGd.setProjectId(processInstanceId);
            submitGd.setSfwlx("收文");

            // 3. 构造元数据 Tmxx
            Tmxx tmxx = new Tmxx();
            //必填字段
            tmxx.setGwbz(processInstanceId);
            tmxx.setMj("内部");
            tmxx.setBt(doc.getSubject());
            tmxx.setCwrq(doc.getReceiveTime() != null ?
                    LocalDateTimeUtil.formatNormal(doc.getReceiveTime()) : "");
            tmxx.setDh(dh);
            tmxx.setNd(year);
            tmxx.setBgqx(bgqx);
            tmxx.setJghwt(doc.getSendDept());
            tmxx.setJh(docNum);
            tmxx.setSzdxbz(processInstanceId);

            tmxx.setWz(StrUtil.blankToDefault(doc.getDocSecondClass(), "其他")); // 简化分类
            String jjcd = "平急"; // 设置底线默认值
            if (StrUtil.isNotBlank(doc.getUrgencyDegree())) {
                String urgencyLabel = DictFrameworkUtils.parseDictDataLabel("emergency_degree", doc.getUrgencyDegree());
                if (StrUtil.isNotBlank(urgencyLabel)) {
                    jjcd = urgencyLabel; // 只有当字典里真的配了这个值，才去覆盖默认值
                }
            }
            tmxx.setJjcd(jjcd);
            tmxx.setFz(doc.getRemark());
            tmxx.setCllx(StrUtil.blankToDefault(doc.getDocRange(), "PT"));
            tmxx.setZllbs(new ArrayList<>());

            // 4. 查询并装配附件
            List<Sjzl> sjzlList = new ArrayList<>();
            List<MinioObject> minioObjects = new ArrayList<>();
            List<String> attachNames = new ArrayList<>();

            List<ReceiveFileRespVO> attaches = receiveDocService.getReceiveDocAttachListByReceiveDocId(doc.getId());

            String pch = getPCH("rec");
            for (ReceiveFileRespVO att : attaches) {
                String fileName = att.getAttachFileName();
                if (StrUtil.isEmpty(fileName)) {
                    continue;
                }
                attachNames.add(fileName);
                Sjzl sjzl = new Sjzl();
                sjzl.setSjzl(fileName);
                sjzl.setBucketName("oagd");
                sjzl.setClmc(FileUtil.mainName(fileName));
                // 假设你有物理路径字段，如果没有需要结合 FileService 转换
                String mObj = att.getPath().replace("\\", "/");
                sjzl.setMObject(mObj);
                sjzl.setCllx("附件");
                sjzl.setSqfs("电子收取");
                sjzl.setWjm(fileName);
                try {
                    // att.getFileSize() 是从 infra_file 表取出的字节大小
                    if (att.getSize() != null) {
                        // FileUtil.readableFileSize() 完美平替 C# 的 GetFileSize()
                        // 它会自动将 byte 转换为 "12 KB", "1.5 MB", "2 GB" 这种易读格式
                        sjzl.setWjdx(FileUtil.readableFileSize(att.getSize()));
                    }
                } catch (Exception e) {
                    // 平替 C# 的 CommonArchiveDoc.LogRecord
                    log.error("收文归档附件大小处理异常：{}，{}", sjzl.getMObject(), e.getMessage());
                }
                sjzl.setGsxx(FileUtil.extName(fileName));
                sjzl.setPch(pch);
                sjzl.setJhrq(DateUtil.today());
                sjzl.setLddwmc(unitName);
                sjzl.setZrz(unitName);
                sjzl.setTm(doc.getSubject());
                sjzl.setDh(dh);
                sjzl.setTmid(processInstanceId);
                sjzlList.add(sjzl);

                MinioObject mo = new MinioObject();
                mo.setBuckName("oagd");
                mo.setObjectName(mObj);
                mo.setPhysicalPath((baePath+"/"+mObj).replace("\\", "/"));
                minioObjects.add(mo);
            }
            tmxx.setFjsm(CollUtil.join(attachNames, ","));

            Zllb zllb = new Zllb();
            zllb.setZllx("附件");
            zllb.setSjzls(sjzlList);
            tmxx.getZllbs().add(zllb);
            // 5.收文阅办单
            String fname = doc.getSubject();
            if (StrUtil.length(fname) > 100) {
                fname = StrUtil.subPre(fname, 100);
            }
            String safeFileName = FileNameUtil.cleanInvalid(fname + "-阅办单.pdf");
            String currentYear = DateUtil.format(new Date(), "yyyyMMdd");
            String attachFilePath = "receive/" + currentYear +  "/";
            String mObjectPdf = attachFilePath + safeFileName;
            String physicalPathPdf = (baePath + "/" + mObjectPdf).replace("\\", "/");

            Sjzl sjzlPdf = new Sjzl();
            sjzlPdf.setSjzl(safeFileName);
            sjzlPdf.setBucketName("oagd");

            // 这里是否需要上传一下先？？？
            sjzlPdf.setMObject(mObjectPdf);
            sjzlPdf.setClmc(FileUtil.mainName(safeFileName));
            sjzlPdf.setCllx("阅办单"); // 或者 "正文"
            sjzlPdf.setSqfs("电子收取");
            sjzlPdf.setWjm(safeFileName);
            sjzlPdf.setCjsj(doc.getReceiveTime() != null ?
                    LocalDateTimeUtil.formatNormal(doc.getReceiveTime()) : "");
            sjzlPdf.setXgsj(doc.getReceiveTime() != null ?
                    LocalDateTimeUtil.formatNormal(doc.getReceiveTime()) : "");
            sjzlPdf.setGsxx("pdf");
            sjzlPdf.setPch(pch);
            sjzlPdf.setJhrq(DateUtil.today());
            sjzlPdf.setBsl("2");
            sjzlPdf.setLddwmc(unitName);
            sjzlPdf.setZrz(unitName);
            sjzlPdf.setTm(safeFileName);
            sjzlPdf.setDh(dh);
            sjzlPdf.setFjpath(mObjectPdf);
            sjzlPdf.setTmid(processInstanceId);

            try {
                // 如果文件在本地存储中不存在，则调用生成逻辑
                if (!FileUtil.exist(physicalPathPdf)) {
                    // 调用之前写的生成服务，获取 PDF 字节数组
                    // 【注意】如果你之前的 generatePdf 方法写死了 "YW收文.docx"，
                    // 建议修改该服务方法支持传入 printTemp，即：receiveDocService.generatePdf(String.valueOf(doc.getId()), printTemp);
                    byte[] pdfBytes = receiveDocService.generatePdf(doc.getId());

                    // 将生成的字节流直接写出到本地物理路径
                    FileUtil.writeBytes(pdfBytes, physicalPathPdf);
                }
                // 获取生成好的文件大小
                sjzlPdf.setWjdx(FileUtil.readableFileSize(FileUtil.file(physicalPathPdf).length()));
            } catch (Exception e) {
                log.error("收文归档：生成并保存阅办单异常：{}，{}", physicalPathPdf, e.getMessage());
            }

            // 加入归档数据包
            List<Sjzl> ybdList = new ArrayList<>();
            ybdList.add(sjzlPdf);
            Zllb ybdZllx =  new Zllb();
            ybdZllx.setZllx("阅办单");
            ybdZllx.setSjzls(ybdList);
            tmxx.getZllbs().add(ybdZllx);

            // 加入待上传 Minio 的对象集合
            MinioObject moPdf = new MinioObject();
            moPdf.setBuckName("oagd");
            moPdf.setObjectName(mObjectPdf);
            moPdf.setPhysicalPath(physicalPathPdf);
            minioObjects.add(moPdf);

            submitGd.setTmxx(tmxx);
            submitGd.setMinioObjects(minioObjects);

            // 6. 获取 BPMN 流程审批详情 (替代原先的 t_actinst 原生 SQL)
            submitGd.setLiucxx(getBpmApprovalHistory(processInstanceId));
            log.info("归档参数：{}", JSON.toJSONString(submitGd));
            String  test = "到最后了";
            ReceiveDocDO updateDO = new ReceiveDocDO();

            // 6. 执行归档
//            OagdResponse oagdResponse = executeArchive(submitGd);
//            String resultJson = JSONUtil.toJsonStr(oagdResponse).replace("'", "‘");
//
//            // 7. 更新实体状态
//            ReceiveDocDO updateDO = new ReceiveDocDO();
//            updateDO.setId(doc.getId());
//            if (oagdResponse.isStatus()) {
//                updateDO.setIfpigeonhold((short) 1);
//                updateDO.setPigeonholeNum(oagdResponse.getId());
//                updateDO.setPigeonholeResult(resultJson);
//                success++;
//            } else {
//                updateDO.setIfpigeonhold((short) -1);
//                updateDO.setPigeonholeResult(resultJson);
//            }
//            receiveDocMapper.updateById(updateDO);
        }
        return success;
    }


    private List<LiucxxItem> getBpmApprovalHistory(String processInstanceId) {
        List<LiucxxItem> list = new ArrayList<>();
        if (StrUtil.isEmpty(processInstanceId)) return list;

        // 1. 调用任务服务，获取流程实例下的所有历史任务 (true 表示按开始时间升序)
        List<HistoricTaskInstance> taskList = bpmTaskService.getTaskListByProcessInstanceId(processInstanceId, true);
        if (CollUtil.isEmpty(taskList)) return list;


        // 2. 批量查询办理人信息，用于把 Assignee (用户ID) 翻译成 姓名
        Set<Long> userIds = taskList.stream()
                .map(HistoricTaskInstance::getAssignee)
                .filter(StrUtil::isNotBlank)
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(
                convertSet(userMap.values(), AdminUserRespDTO::getDeptId)
        );

        // 3. 组装归档所需的流程信息节点
        for (HistoricTaskInstance task : taskList) {
            LiucxxItem item = new LiucxxItem();
            item.setNodeName(task.getName()); // 环节名称 (例如: 局长批示)

            // 翻译处理人
            String assigneeId = task.getAssignee();
            if (StrUtil.isNotBlank(assigneeId)) {
                AdminUserRespDTO user = userMap.get(Long.parseLong(assigneeId));
                if (user != null) {
                    String nickname = user.getNickname();
                    DeptRespDTO dept = deptMap.get(user.getDeptId());

                    // 最终显示效果：张三 (开发部)
                    String deptName = (dept != null)
                            ? dept.getName()
                            : "";

                    item.setDept(deptName);
                }
            } else {
                item.setAuthor("系统");
            }

            // 处理时间：优先结束时间，没有就用创建时间 (注意：这里如果是 JDK8 的 Date 类型，继续用 DateUtil)
            Date timeObj = task.getEndTime() != null ? task.getEndTime() : task.getCreateTime();
            item.setModified(timeObj != null ? DateUtil.formatDateTime(timeObj) : "");

            // 获取审批意见 (RuoYi-Vue-Pro 默认把审批意见存在 TaskLocalVariables 中)
            Map<String, Object> localVars = task.getTaskLocalVariables();
            String reason = null;
            if (localVars != null) {
                reason = (String) localVars.get(BpmnVariableConstants.TASK_VARIABLE_REASON);
            }
            item.setBody(StrUtil.blankToDefault(reason, "处理完毕"));

            list.add(item);
        }
        return list;
    }


    // ============================ 公共调用 ============================
    private OagdResponse executeArchive(SubmitGd submitGd) {
        OagdResponse response = new OagdResponse();
        try {
            // 1. 调用独立方法执行 MinIO 附件上传
            MinioResponse minioResp = uploadToMinio(submitGd.getMinioObjects());
            response.setMinioResponse(minioResp);

            // 2. 如果 Minio 上传成功，则继续向 OA 系统提交归档核心数据
            // 注意：这里请根据你 MinioResponse 实体类中定义的 success 字段 getter 调整方法名 (比如 isSuccess() 或 getSuccess())
            if (minioResp.isSuccess()) {
                String submitUrl = configApi.getConfigValueByKey("archive.oa.submitUrl");

                // 替换字段名适配 OA 接口
                String json = JSONUtil.toJsonStr(submitGd).replace("mObject", "object");
                String oaResult = HttpUtil.post(submitUrl, json);

                if (StrUtil.isNotEmpty(oaResult)) {
                    response = JSONUtil.toBean(oaResult, OagdResponse.class);
                }
            } else {
                // 如果 Minio 上传失败，直接阻断并返回错误信息
                response.setStatus(false);
                response.setMsg("文件上传至MinIO失败：" + minioResp.getMessage());
            }

        } catch (Exception e) {
            log.error("接口归档异常", e);
            response.setStatus(false);
            response.setMsg(e.getMessage());
        }
        return response;
    }

    /**
     * 将本地物理文件直接上传至 MinIO
     *
     * @param objects 待上传的 MinioObject 集合
     * @return MinioResponse 包含上传结果状态和统计信息
     */
    private MinioResponse uploadToMinio(List<MinioObject> objects) {
        MinioResponse minioResp = new MinioResponse();
        int total = objects != null ? objects.size() : 0;
        int uploadCount = 0;
        boolean minioSuccess = true;
        String minioMessage = "成功";

        minioResp.setTotal(total);

        if (total > 0) {
            try {
                // 1. 获取 Minio 配置信息
                String endpoint = configApi.getConfigValueByKey("archive.minio.endpoint");
                String accessKey = configApi.getConfigValueByKey("archive.minio.ak");
                String secretKey = configApi.getConfigValueByKey("archive.minio.sk");

                // 2. 初始化 Minio 客户端
                MinioClient minioClient = MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();

                // 3. 遍历执行本地文件直传
                for (MinioObject obj : objects) {
                    String buckName = obj.getBuckName();
                    String objectName = obj.getObjectName();
                    String physicalPath = obj.getPhysicalPath();

                    File file = new File(physicalPath);
                    if (file.exists()) {
                        // 检查 Bucket 是否存在，不存在则自动创建
                        boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(buckName).build());
                        if (!isExist) {
                            minioClient.makeBucket(MakeBucketArgs.builder().bucket(buckName).build());
                        }

                        // 获取 ContentType，获取不到则默认二进制流
                        String contentType = URLConnection.guessContentTypeFromName(file.getName());
                        if (StrUtil.isEmpty(contentType)) {
                            contentType = "application/octet-stream";
                        }

                        // 直接通过物理路径上传文件
                        minioClient.uploadObject(
                                UploadObjectArgs.builder()
                                        .bucket(buckName)
                                        .object(objectName)
                                        .filename(physicalPath) // 传入物理路径直传
                                        .contentType(contentType)
                                        .build()
                        );
                        uploadCount++;
                    } else {
                        log.warn("Minio上传跳过：本地物理文件不存在 {}", physicalPath);
                    }
                }
            } catch (Exception e) {
                log.error("MinIO本地文件直传异常", e);
                minioSuccess = false;
                minioMessage = e.getMessage();
            }
        }

        minioResp.setSuccess(minioSuccess);
        minioResp.setMessage(minioMessage);
        minioResp.setUpload(uploadCount);

        return minioResp;
    }

    public String getPCH(String type) {
        // 1. 定义配置键名（对应 C# 的 _configure 字段）
        String configKey = type.equals("rec") ? "archive.rec.pch" : "archive.send.pch";

        // 2. 从数据库获取当前存储的值 (格式：yyyyMMdd-NNN)
        String currentVal = configApi.getConfigValueByKey(configKey);

        String today = DateUtil.format(new Date(), "yyyyMMdd");
        int nextIdx = 1;

        // 3. 解析并计算流水号
        if (StrUtil.isNotBlank(currentVal) && currentVal.contains("-")) {
            String[] arr = currentVal.split("-");
            String lastDate = arr[0];
            int lastIdx = Integer.parseInt(arr[1]);

            if (today.equals(lastDate)) {
                // 如果是今天，流水号 +1
                nextIdx = lastIdx + 1;
            } else {
                // 如果跨天，重置为 1
                nextIdx = 1;
            }
        }

        // 4. 格式化流水号为 3 位（如 001, 012）
        String indexStr = String.format("%03d", nextIdx);

        // 5. 构造存储值 (带连字符，供下次对比)
        String storeVal = today + "-" + indexStr;

        // 6. 构造返回值 (不带连字符，对应 C# 返回逻辑)
        String result = today + indexStr;

        // 7. 同步更新配置存储 (对应 C# 的 _configure.Save())
        // 注意：这里建议直接更新数据库字段，保证下次读取是准确的
        updateConfigValue(configKey, storeVal);

        return result;
    }

    /**
     * 更新配置值（模拟 _configure.Save）
     * 你可以在数据库中维护对应的配置项
     */
    private void updateConfigValue(String key, String value) {
        configApi.getConfigValueByKey(key,value);
        log.info("更新批次号配置: {} -> {}", key, value);
    }
}
