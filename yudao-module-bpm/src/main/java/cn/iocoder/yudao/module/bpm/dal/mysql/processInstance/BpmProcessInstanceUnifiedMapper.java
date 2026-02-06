package cn.iocoder.yudao.module.bpm.dal.mysql.processInstance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceUnifiedReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance.BpmProcessInstanceUnifiedRespVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BPM 流程实例统一查询 Mapper
 * 用于实现“办件查询”功能（包含我发起的、待办、已办、经办）
 */
@Mapper
public interface BpmProcessInstanceUnifiedMapper  {

    List<BpmProcessInstanceUnifiedRespVO> selectUnifiedList(@Param("userId") Long userId,
                                                            @Param("reqVO") BpmProcessInstanceUnifiedReqVO reqVO);

    /**
     * 2. 查总数
     */
    Long selectUnifiedCount(@Param("userId") Long userId,
                            @Param("reqVO") BpmProcessInstanceUnifiedReqVO reqVO);
}