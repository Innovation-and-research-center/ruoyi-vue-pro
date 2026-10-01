package cn.iocoder.yudao.module.bpm.dal.mysql.processInstance;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmProcessViewScopeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface BpmProcessViewScopeMapper extends BaseMapperX<BpmProcessViewScopeDO> {

    @Select("SELECT process_definition_key FROM bpm_process_view_scope WHERE role_id = #{roleId}")
    List<String> selectKeysByRoleId(@Param("roleId") Long roleId);

    @Select("<script>SELECT DISTINCT process_definition_key FROM bpm_process_view_scope " +
            "WHERE role_id IN <foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>#{roleId}</foreach></script>")
    List<String> selectKeysByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Delete("DELETE FROM bpm_process_view_scope WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("INSERT INTO bpm_process_view_scope (role_id, process_definition_key) VALUES (#{roleId}, #{key})")
    int insert(@Param("roleId") Long roleId, @Param("key") String key);
}
