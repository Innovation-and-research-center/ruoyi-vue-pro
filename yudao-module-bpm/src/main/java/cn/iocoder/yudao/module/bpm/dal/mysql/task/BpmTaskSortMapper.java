package cn.iocoder.yudao.module.bpm.dal.mysql.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BpmTaskSortMapper {

    List<String> selectTodoTaskIds(@Param("userId") Long userId,
                                   @Param("reqVO") BpmTaskPageReqVO reqVO);

    Long selectTodoTaskCount(@Param("userId") Long userId,
                             @Param("reqVO") BpmTaskPageReqVO reqVO);

    List<String> selectDoneTaskIds(@Param("userId") Long userId,
                                   @Param("reqVO") BpmTaskPageReqVO reqVO);

    Long selectDoneTaskCount(@Param("userId") Long userId,
                             @Param("reqVO") BpmTaskPageReqVO reqVO);

}
