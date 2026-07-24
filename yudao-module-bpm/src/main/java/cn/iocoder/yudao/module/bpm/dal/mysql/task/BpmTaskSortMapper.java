package cn.iocoder.yudao.module.bpm.dal.mysql.task;

import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
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

    List<BpmTaskRespVO> selectUnifiedDoneTaskList(@Param("userId") Long userId,
                                                  @Param("reqVO") BpmTaskPageReqVO reqVO);

    Long selectUnifiedDoneTaskCount(@Param("userId") Long userId,
                                    @Param("reqVO") BpmTaskPageReqVO reqVO);

    int updateHistoricTaskLongVariable(@Param("taskId") String taskId,
                                       @Param("name") String name,
                                       @Param("value") Integer value);

    int updateHistoricProcessLongVariable(@Param("processInstanceId") String processInstanceId,
                                          @Param("name") String name,
                                          @Param("value") Integer value);

    int updateRuntimeProcessInstanceName(@Param("processInstanceId") String processInstanceId,
                                         @Param("name") String name);

    int updateHistoricProcessInstanceName(@Param("processInstanceId") String processInstanceId,
                                          @Param("name") String name);

}
