package cn.iocoder.yudao.module.bpm.service.receivedoc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocAttachDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocAttachMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jodd.util.StringUtil;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import cn.iocoder.yudao.module.bpm.controller.admin.receivedoc.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.receivedoc.ReceiveDocDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.bpm.dal.mysql.receivedoc.ReceiveDocMapper;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.diffList;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.enums.BpmTaskKeyConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_CUSTOM_NAME;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.PROCESS_URGENCY_DEGREE;

/**
 * 收文 Service 实现类
 *
 * @author 芋道
 */
@Service
@Validated
public class ReceiveDocServiceImpl implements ReceiveDocService {

    public static final String PROCESS_KEY = RECEIVE;

    public static final String PROCESS_KEY_CHANGE = ELECTRIC;

    @Resource
    private ReceiveDocMapper receiveDocMapper;

    @Resource
    private ReceiveDocAttachMapper receiveDocAttachMapper;

    @Resource
    private BpmProcessInstanceApi processInstanceApi;

    @Resource
    private FileApi fileApi; // 注入文件 API

    @Resource
    private FileMapper fileMapper;

    @Override
    public Long createReceiveDoc(Long userId,ReceiveDocSaveReqVO createReqVO) {

        if (checkReceiveDocNumberExists(createReqVO.getReceiveDocNumber())) {
            throw exception(RECEIVE_DOC_EXISTS);
        }
        ReceiveDocDO receiveDoc = BeanUtils.toBean(createReqVO, ReceiveDocDO.class);
        Long sequence = extractSequenceFromNumber(receiveDoc.getReceiveDocNumber());
        if (sequence != null) {
            receiveDoc.setDocSequence(sequence);
        } else {
            // 如果解析失败（比如格式不对），建议抛出异常阻止保存，否则下次生成会出错
            throw exception(RECEIVE_DOC_ERROR);
        }
        // 插入

        receiveDocMapper.insert(receiveDoc);
        createReceiveDocAttachList(receiveDoc.getId(), createReqVO.getFileList());
        Map<String, Object> processInstanceVariables = new HashMap<>();

        if (CollUtil.isNotEmpty(createReqVO.getProcessVariables())) {
            processInstanceVariables.putAll(createReqVO.getProcessVariables());
        }
        String realKey=PROCESS_KEY;
        String processName = "收文";
        if (!StringUtil.isEmpty(createReqVO.getDocRange())){
            realKey=PROCESS_KEY_CHANGE;
            processName="电子公告";
        }

//        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        //自定义标题
        String customName =StringUtil.isEmpty(receiveDoc.getSubject()) ? processName:receiveDoc.getSubject();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(PROCESS_URGENCY_DEGREE, receiveDoc.getUrgencyDegree());
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, createReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(realKey)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(receiveDoc.getId()))
                        .setStartUserSelectAssignees(createReqVO.getStartUserSelectAssignees()));
        receiveDocMapper.updateById(new ReceiveDocDO().setId(receiveDoc.getId()).setProcessInstanceId(processInstanceId));


        // 返回
        return receiveDoc.getId();
    }


    @Override
    public Long saveReceiveDoc(Long userId,ReceiveDocSaveReqVO createReqVO) {

        if (checkReceiveDocNumberExists(createReqVO.getReceiveDocNumber())) {
            throw exception(RECEIVE_DOC_EXISTS);
        }
        ReceiveDocDO receiveDoc = BeanUtils.toBean(createReqVO, ReceiveDocDO.class);
        Long sequence = extractSequenceFromNumber(receiveDoc.getReceiveDocNumber());
        if (sequence != null) {
            receiveDoc.setDocSequence(sequence);
        } else {
            // 如果解析失败（比如格式不对），建议抛出异常阻止保存，否则下次生成会出错
            throw exception(RECEIVE_DOC_ERROR);
        }
        // 插入

        receiveDocMapper.insert(receiveDoc);
        createReceiveDocAttachList(receiveDoc.getId(), createReqVO.getFileList());
        // 返回
        return receiveDoc.getId();
    }

    private Long extractSequenceFromNumber(String docNumber) {
        try {
            if (StrUtil.isEmpty(docNumber)) return null;

            // 方式1：如果你确定最后4位一定是数字
            // 截取最后4位
            // String numStr = docNumber.substring(docNumber.length() - 4);

            // 方式2 (推荐)：截取最后一个横杠 "-" 后面的所有内容，更稳健
            int lastDashIndex = docNumber.lastIndexOf("-");
            if (lastDashIndex == -1 || lastDashIndex == docNumber.length() - 1) {
                return null; // 格式不对，没有横杠
            }

            String numStr = docNumber.substring(lastDashIndex + 1);

            // 解析为 Long (自动去除前导零，例如 "0005" -> 5)
            return Long.parseLong(numStr);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean checkReceiveDocNumberExists(String docNumber) {
        if (StrUtil.isEmpty(docNumber)) {
            return false;
        }
        // 使用 MyBatis Plus 查询是否存在
        return receiveDocMapper.selectCount(
                Wrappers.<ReceiveDocDO>lambdaQuery().eq(ReceiveDocDO::getReceiveDocNumber, docNumber)
        ) > 0;
    }

    @Override
    public String generateDocumentSequence(ReceiveDocCreateNumberVO createReqVO) {
        if(StringUtil.isEmpty(createReqVO.getYear())){
            createReqVO.setYear(String.valueOf(LocalDate.now().getYear()));
        }
        return getNextDocSequence(createReqVO.getDocClass(), createReqVO.getYear());
    }

    @Override
    public Long generateDocumentSequence(String docClass) {
        return getNextDocSequenceForJob(docClass, String.valueOf(LocalDate.now().getYear()));
    }

    private String getNextDocSequence(String docClass, String year) {
        int yearInt = Integer.parseInt(year);
        LocalDateTime startOfYear = LocalDateTime.of(yearInt, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(yearInt, 12, 31, 23, 59, 59);
        QueryWrapper<ReceiveDocDO> query = Wrappers.query();
        query.select("MAX(DOC_SEQUENCE)")
                .eq("DOC_CLASS", docClass)
                .ge("RECEIVE_TIME", startOfYear)
                .le("RECEIVE_TIME", endOfYear);// 动态拼接年份查询

        long nextVal = executeMaxQuery(query);
        return String.format("%d-%s-%04d", yearInt, docClass, nextVal);
    }

    private long getNextDocSequenceForJob(String docClass, String year) {
        int yearInt = Integer.parseInt(year);
        LocalDateTime startOfYear = LocalDateTime.of(yearInt, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(yearInt, 12, 31, 23, 59, 59);
        QueryWrapper<ReceiveDocDO> query = Wrappers.query();
        query.select("MAX(DOC_SEQUENCE)")
                .eq("DOC_CLASS", docClass)
                .ge("RECEIVE_TIME", startOfYear)
                .le("RECEIVE_TIME", endOfYear);// 动态拼接年份查询

        return  executeMaxQuery(query);
    }

    private boolean isSequenceOccupied(String documentSequence) {
        if (!StringUtils.hasText(documentSequence)) return false;

        LambdaQueryWrapper<ReceiveDocDO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ReceiveDocDO::getReceiveDocNumber, documentSequence);

        // selectCount > 0 表示已被占用
        return receiveDocMapper.selectCount(wrapper) > 0;
    }

    private long executeMaxQuery(QueryWrapper<ReceiveDocDO> query) {
        // 使用 selectObjs 获取第一列结果
        List<Object> result = receiveDocMapper.selectObjs(query);

        // 【核心修改】增加为空判断
        if (result != null && !result.isEmpty()) {
            Object maxVal = result.get(0);
            // 数据库如果有记录，maxVal 可能为 Long, Integer, BigDecimal，需安全转换
            if (maxVal != null) {
                return Long.parseLong(maxVal.toString()) + 1;
            }
        }

        // 如果数据库没有查到记录 (List为空 或 maxVal为null)，说明是该年份该文种的第一条
        return 1L;
    }

    @Override
    public void createFlowReceiveDoc(Long userId,ReceiveDocSaveReqVO updateReqVO) {
        // 校验存在
        validateReceiveDocExists(updateReqVO.getId());
        // 更新
        ReceiveDocDO updateObj = BeanUtils.toBean(updateReqVO, ReceiveDocDO.class);
        receiveDocMapper.updateById(updateObj);

        // 更新子表
        updateReceiveDocAttachList(updateReqVO.getId(), updateReqVO.getFileList());

        Map<String, Object> processInstanceVariables = new HashMap<>();
        if (CollUtil.isNotEmpty(updateReqVO.getProcessVariables())) {
            processInstanceVariables.putAll(updateReqVO.getProcessVariables());
        }
        String realKey=PROCESS_KEY;
        if (!StringUtil.isEmpty(updateReqVO.getDocRange())){
            realKey=PROCESS_KEY_CHANGE;
        }
//        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_NEXT_NODE, createReqVO.getSelectNode());
        String customName =StringUtil.isEmpty(updateReqVO.getSubject()) ? "收文":updateReqVO.getSubject();
        processInstanceVariables.put(PROCESS_CUSTOM_NAME, customName);
        processInstanceVariables.put(PROCESS_URGENCY_DEGREE, updateReqVO.getUrgencyDegree());
        processInstanceVariables.put(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_NODE_SELECT_ASSIGNEES, updateReqVO.getNextNodeAssignees());
        String processInstanceId = processInstanceApi.createProcessInstance(userId,
                new BpmProcessInstanceCreateReqDTO().setProcessDefinitionKey(realKey)
                        .setVariables(processInstanceVariables).setBusinessKey(String.valueOf(updateObj.getId()))
                        .setStartUserSelectAssignees(updateReqVO.getStartUserSelectAssignees()));
        receiveDocMapper.updateById(new ReceiveDocDO().setId(updateObj.getId()).setProcessInstanceId(processInstanceId));
    }


    @Override
    public void updateReceiveDoc(ReceiveDocSaveReqVO updateReqVO) {
        // 校验存在
        validateReceiveDocExists(updateReqVO.getId());
        // 更新
        ReceiveDocDO updateObj = BeanUtils.toBean(updateReqVO, ReceiveDocDO.class);
        receiveDocMapper.updateById(updateObj);

        // 更新子表
        updateReceiveDocAttachList(updateReqVO.getId(), updateReqVO.getFileList());
    }

    private void createReceiveDocAttachList(Long receiveDocId, List<ReceiveDocAttachDO> list) {
        list.forEach(o -> o.setReceiveDocId(receiveDocId).clean());
        receiveDocAttachMapper.insertBatch(list);
    }


    private void updateReceiveDocAttachList(Long receiveDocId, List<ReceiveDocAttachDO> list) {
        list.forEach(o -> o.setReceiveDocId(receiveDocId).clean());
        List<ReceiveDocAttachDO> oldList = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);
        List<List<ReceiveDocAttachDO>> diffList = diffList(oldList, list, (oldVal, newVal) -> {
            boolean same = ObjectUtil.equal(oldVal.getId(), newVal.getId());
            if (same) {
                newVal.setId(oldVal.getId()).clean(); // 解决更新情况下：updateTime 不更新
            }
            return same;
        });

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            receiveDocAttachMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            receiveDocAttachMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            receiveDocAttachMapper.deleteByIds(convertList(diffList.get(2), ReceiveDocAttachDO::getId));
        }
    }


    @Override
    public void deleteReceiveDoc(Long id) {
        // 校验存在
        validateReceiveDocExists(id);
        // 删除
        receiveDocMapper.deleteById(id);
    }

    @Override
        public void deleteReceiveDocListByIds(List<Long> ids) {
        // 删除
        receiveDocMapper.deleteByIds(ids);
        }


    private void validateReceiveDocExists(Long id) {
        if (receiveDocMapper.selectById(id) == null) {
            throw exception(RECEIVE_DOC_NOT_EXISTS);
        }
    }

    @Override
    public ReceiveDocDO getReceiveDoc(Long id) {
        return receiveDocMapper.selectById(id);
    }

    @Override
    public PageResult<ReceiveDocDO> getReceiveDocPage(ReceiveDocPageReqVO pageReqVO) {
        return receiveDocMapper.selectPage(pageReqVO);
    }


    @Override
    public List<ReceiveFileRespVO> getReceiveDocAttachListByReceiveDocId(Long receiveDocId) {
        // 1. 查询数据库，获取 DO 列表
        List<ReceiveDocAttachDO> doList = receiveDocAttachMapper.selectListByReceiveDocId(receiveDocId);

        // 2. 如果为空，直接返回空列表
        if (CollUtil.isEmpty(doList)) {
            return Collections.emptyList();
        }

        // 3. 转换为 VO
        List<ReceiveFileRespVO> voList = BeanUtils.toBean(doList, ReceiveFileRespVO.class);

        // 4. 获取所有文件ID集合
        Set<Long> fileIds = CollectionUtils.convertSet(voList, ReceiveFileRespVO::getAttachFileId);

        Map<Long, String> fileUrlMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(fileIds)) {
            List<FileDO> files = fileMapper.selectBatchIds(fileIds);
            // 提取 ID -> URL 的映射
            fileUrlMap = CollectionUtils.convertMap(files, FileDO::getId, FileDO::getUrl);
        }

        // 5. 回填 URL
        Map<Long, String> finalFileUrlMap = fileUrlMap; // lambda需要final变量
        voList.forEach(vo -> vo.setFileUrl(finalFileUrlMap.get(vo.getAttachFileId())));

        return voList;
    }


}