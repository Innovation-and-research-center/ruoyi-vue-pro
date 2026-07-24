package cn.iocoder.yudao.module.bpm.dal.mysql.historyworkflow;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface HistoryWorkflowMapper {

    @Select("SELECT CAST(p.proinst_id AS varchar) AS \"proinstId\", p.proinst_id AS \"proinstIdRaw\", p.source_schema AS \"sourceSchema\", p.project_id AS \"projectId\", " +
            "p.proinst_name AS \"name\", p.proinst_name AS \"proinstName\", p.start_date AS \"startTime\", " +
            "p.start_date AS \"startDate\", p.end_date AS \"endTime\", p.end_date AS \"endDate\", " +
            "p.proinst_status AS \"status\", p.proinst_status AS \"proinstStatus\", p.responsible_dept AS \"responsibleDept\", " +
            "p.deadline, p.emergency, p.creator, p.creator AS \"startUserName\", p.intransactor, p.inact " +
            "FROM hist_wf.proinst p " +
            "WHERE p.project_id = #{projectId} " +
            "ORDER BY p.end_date DESC NULLS LAST, p.start_date DESC NULLS LAST LIMIT 1")
    Map<String, Object> selectProinstByProjectId(@Param("projectId") String projectId);

    @Select("<script>" +
            "SELECT DISTINCT ON (p.project_id) p.project_id AS \"projectId\", p.proinst_status AS \"proinstStatus\", p.end_date AS \"endDate\" " +
            "FROM hist_wf.proinst p " +
            "WHERE p.project_id IN " +
            "<foreach collection='projectIds' item='projectId' open='(' separator=',' close=')'>#{projectId}</foreach> " +
            "ORDER BY p.project_id, p.end_date DESC NULLS LAST, p.start_date DESC NULLS LAST" +
            "</script>")
    List<Map<String, Object>> selectProinstByProjectIds(@Param("projectIds") List<String> projectIds);

    @Select("<script>" +
            "SELECT DISTINCT ON (m.bizinst_guid) m.bizinst_guid AS \"bizinstGuid\", " +
            "m.project_id AS \"projectId\", p.proinst_status AS \"proinstStatus\", p.end_date AS \"endDate\" " +
            "FROM hist_wf.biz_project_map m " +
            "LEFT JOIN hist_wf.proinst p ON p.project_id = m.project_id " +
            "WHERE m.business_type = #{businessType} AND m.bizinst_guid IN " +
            "<foreach collection='bizinstGuids' item='bizinstGuid' open='(' separator=',' close=')'>#{bizinstGuid}</foreach> " +
            "ORDER BY m.bizinst_guid, p.end_date DESC NULLS LAST, p.start_date DESC NULLS LAST" +
            "</script>")
    List<Map<String, Object>> selectBizProjectInfoByGuids(@Param("businessType") String businessType,
                                                          @Param("bizinstGuids") List<String> bizinstGuids);

    @Select("SELECT CAST(p.proinst_id AS varchar) AS \"proinstId\", p.proinst_id AS \"proinstIdRaw\", p.source_schema AS \"sourceSchema\", p.project_id AS \"projectId\", " +
            "p.proinst_name AS \"name\", p.proinst_name AS \"proinstName\", p.start_date AS \"startTime\", " +
            "p.start_date AS \"startDate\", p.end_date AS \"endTime\", p.end_date AS \"endDate\", " +
            "p.proinst_status AS \"status\", p.proinst_status AS \"proinstStatus\", p.responsible_dept AS \"responsibleDept\", " +
            "p.deadline, p.emergency, p.creator, p.creator AS \"startUserName\", p.intransactor, p.inact " +
            "FROM hist_wf.proinst p " +
            "WHERE p.proinst_id = #{proinstId} " +
            "ORDER BY p.end_date DESC NULLS LAST, p.start_date DESC NULLS LAST LIMIT 1")
    Map<String, Object> selectProinstByProinstId(@Param("proinstId") Long proinstId);

    @Select("SELECT CAST(a.actinst_id AS varchar) AS id, a.actinst_id AS \"actinstId\", a.actinst_name AS \"name\", a.actinst_name AS \"actinstName\", " +
            "a.transactor, CAST(a.user_id AS varchar) AS \"userId\", a.start_date AS \"startTime\", " +
            "a.start_date AS \"startDate\", a.start_date AS \"createTime\", a.end_date AS \"endTime\", a.end_date AS \"endDate\", " +
            "a.will_finish_date AS \"dueDate\", CASE WHEN a.end_date IS NULL THEN 1 ELSE 2 END AS \"status\", " +
            "a.actinst_comment AS \"reason\", a.actinst_comment AS \"comment\", " +
            "a.actinst_comment AS \"actinstComment\" " +
            "FROM hist_wf.actinst a WHERE a.proinst_id = #{proinstId} AND a.source_schema = #{sourceSchema} " +
            "ORDER BY a.start_date ASC NULLS LAST, a.actinst_id ASC")
    List<Map<String, Object>> selectActinstRecords(@Param("proinstId") Long proinstId, @Param("sourceSchema") String sourceSchema);

    @Select("SELECT r.routeinst_id AS \"routeinstId\", r.from_actinst AS \"fromActinst\", r.to_actinst AS \"toActinst\", " +
            "COALESCE(f.actinst_name, CAST(r.from_actinst AS varchar)) AS \"fromActinstName\", " +
            "COALESCE(t.actinst_name, CAST(r.to_actinst AS varchar)) AS \"toActinstName\", " +
            "CONCAT(COALESCE(f.actinst_name, CAST(r.from_actinst AS varchar)), ' -> ', COALESCE(t.actinst_name, CAST(r.to_actinst AS varchar))) AS \"name\", " +
            "r.pass_time AS \"passTime\", r.pass_time AS \"endTime\", r.pass_comment AS \"comment\", " +
            "r.pass_comment AS \"passComment\", r.invalid AS \"status\" " +
            "FROM hist_wf.routeinst r " +
            "LEFT JOIN hist_wf.actinst f ON f.source_schema = r.source_schema AND f.actinst_id = r.from_actinst " +
            "LEFT JOIN hist_wf.actinst t ON t.source_schema = r.source_schema AND t.actinst_id = r.to_actinst " +
            "WHERE r.proinst_id = #{proinstId} AND r.source_schema = #{sourceSchema} " +
            "ORDER BY r.pass_time ASC NULLS LAST, r.routeinst_id ASC")
    List<Map<String, Object>> selectRouteRecords(@Param("proinstId") Long proinstId, @Param("sourceSchema") String sourceSchema);

    @Select("SELECT id, subject AS \"title\", subject, project_id AS \"projectId\", process_instance_id AS \"processInstanceId\", " +
            "attach_file_path AS \"attachFilePath\", " +
            "directoridea AS \"directorIdea\", directorname AS \"directorName\", directordate AS \"directorDate\", " +
            "fugleidea AS \"fugleIdea\", fuglename AS \"fugleName\", fugledate AS \"fugleDate\", " +
            "dept_director_idea AS \"deptDirectorIdea\", dept_director AS \"deptDirector\", dept_director_date AS \"deptDirectorDate\", " +
            "leader_idea AS \"leaderIdea\", leader_person AS \"leaderPerson\", leader_date AS \"leaderDate\" " +
            "FROM bpm_receive_doc WHERE project_id = #{projectId} AND COALESCE(deleted, 0) = 0 ORDER BY id DESC LIMIT 1")
    List<Map<String, Object>> selectReceiveDoc(@Param("projectId") String projectId);

    @Select("SELECT id, project_id AS \"projectId\", process_instance_id AS \"processInstanceId\", sj_reason AS \"title\", " +
            "unit_opinion AS \"unitOpinion\", unit_opinion_shr AS \"unitOpinionShr\", unit_opinion_date AS \"unitOpinionDate\", " +
            "jbgs_opinion AS \"jbgsOpinion\", jbgs_opinion_shr AS \"jbgsOpinionShr\", jbgs_opinion_date AS \"jbgsOpinionDate\", " +
            "fjz_opinion AS \"fjzOpinion\", fjz_opinion_shr AS \"fjzOpinionShr\", fjz_opinion_date AS \"fjzOpinionDate\", " +
            "cwfjz_opinion AS \"cwfjzOpinion\", cwfjz_opinion_shr AS \"cwfjzOpinionShr\", cwfjz_opinion_date AS \"cwfjzOpinionDate\", " +
            "jz_opinion AS \"jzOpinion\", jz_opinion_shr AS \"jzOpinionShr\", jz_opinion_date AS \"jzOpinionDate\" " +
            "FROM bpm_leave WHERE project_id = #{projectId} AND COALESCE(deleted, 0) = 0 ORDER BY id DESC LIMIT 1")
    List<Map<String, Object>> selectLeave(@Param("projectId") String projectId);

    @Select("SELECT id, project_id AS \"projectId\", process_instance_id AS \"processInstanceId\", reason AS \"title\", " +
            "dept_director_idea AS \"deptDirectorIdea\", dept_director AS \"deptDirector\", dept_director_date AS \"deptDirectorDate\", " +
            "jub_director_idea AS \"jubDirectorIdea\", jub_director AS \"jubDirector\", jub_director_date AS \"jubDirectorDate\", " +
            "charge_director_idea AS \"chargeDirectorIdea\", charge_director AS \"chargeDirector\", charge_director_date AS \"chargeDirectorDate\", " +
            "core_director_idea AS \"coreDirectorIdea\", core_director AS \"coreDirector\", core_director_date AS \"coreDirectorDate\", " +
            "juz_director_idea AS \"juzDirectorIdea\", juz_director AS \"juzDirector\", juz_director_date AS \"juzDirectorDate\" " +
            "FROM t_time_explain WHERE project_id = #{projectId} AND COALESCE(deleted, 0) = 0 ORDER BY id DESC LIMIT 1")
    List<Map<String, Object>> selectTimeExplain(@Param("projectId") String projectId);

    @Select("SELECT id, doc_guid AS \"docGuid\", project_id AS \"projectId\", process_instance_id AS \"processInstanceId\", title " +
            "FROM t_confflow WHERE project_id = #{projectId} AND COALESCE(deleted, 0) = 0 ORDER BY id DESC LIMIT 1")
    List<Map<String, Object>> selectConfflow(@Param("projectId") String projectId);

    @Select("SELECT x.id, x.xm_guid AS \"xmGuid\", x.process_instance_id AS \"processInstanceId\", " +
            "m.project_id AS \"projectId\", COALESCE(x.sw_wh, x.sqr, x.dsr) AS \"title\" " +
            "FROM hist_wf.biz_project_map m JOIN t_xzfy_list x ON x.xm_guid = m.bizinst_guid " +
            "WHERE m.project_id = #{projectId} AND m.business_type = 'xzfy' AND COALESCE(x.deleted, 0) = 0 " +
            "ORDER BY x.id DESC LIMIT 1")
    List<Map<String, Object>> selectXzfy(@Param("projectId") String projectId);

    @Select("SELECT x.id, x.xm_guid AS \"xmGuid\", x.process_instance_id AS \"processInstanceId\", " +
            "m.project_id AS \"projectId\", COALESCE(x.sw_wh, x.sqr, x.dsr) AS \"title\" " +
            "FROM hist_wf.biz_project_map m JOIN t_xzss_list x ON x.xm_guid = m.bizinst_guid " +
            "WHERE m.project_id = #{projectId} AND m.business_type = 'xzss' AND COALESCE(x.deleted, 0) = 0 " +
            "ORDER BY x.id DESC LIMIT 1")
    List<Map<String, Object>> selectXzss(@Param("projectId") String projectId);

    @Select("SELECT id, form_page_name AS \"formPageName\", form_multi_record_name AS \"formMultiRecordName\", " +
            "doc_id AS \"docId\", doc_type AS \"docType\", user_id AS \"userId\", comment_detail AS \"commentDetail\", " +
            "comment_date AS \"commentDate\", user_name AS \"userName\", closed, closed AS \"nodeName\" " +
            "FROM t_doc_comment WHERE doc_id = #{docId} " +
            "AND (#{docType} IS NULL OR doc_type = #{docType}) " +
            "AND COALESCE(deleted, 0) = 0 " +
            "ORDER BY comment_date ASC NULLS LAST, user_seq ASC NULLS LAST, id ASC")
    List<Map<String, Object>> selectComments(@Param("docId") String docId, @Param("docType") String docType);

    @Select("SELECT id, attach_file_name AS \"filename\", attach_file_id AS \"attachFileId\" " +
            "FROM t_receive_doc_attach WHERE receive_doc_id = CAST(#{id} AS numeric) AND COALESCE(deleted, 0) = 0 ORDER BY attach_order ASC NULLS LAST, id ASC")
    List<Map<String, Object>> selectReceiveDocAttachments(@Param("id") String id);

    @Select("SELECT id, file_path AS \"filePath\", file_name AS \"fileName\", file_extension AS \"fileExtension\" " +
            "FROM t_leave_attach WHERE leave_id = CAST(#{id} AS numeric) AND COALESCE(deleted, 0) = 0 ORDER BY id ASC")
    List<Map<String, Object>> selectLeaveAttachments(@Param("id") String id);

    @Select("SELECT id, file_path AS \"filePath\", file_name AS \"fileName\", file_extension AS \"fileExtension\" " +
            "FROM t_time_explain_attach WHERE time_explain_id = CAST(#{id} AS numeric) AND COALESCE(deleted, 0) = 0 ORDER BY id ASC")
    List<Map<String, Object>> selectTimeExplainAttachments(@Param("id") String id);

    @Select("SELECT confflow_attach_id AS id, file_path AS \"filePath\", file_name AS \"fileName\", file_extension AS \"fileExtension\" " +
            "FROM t_confflow_attach WHERE (doc_guid = #{docGuid} OR comm_id = CAST(#{id} AS numeric)) " +
            "AND COALESCE(deleted, 0) = 0 ORDER BY confflow_attach_id ASC")
    List<Map<String, Object>> selectConfflowAttachments(@Param("id") String id,
                                                        @Param("docGuid") String docGuid);

    @Select("SELECT id, task_id AS \"taskId\", filepath, filename, fileextension AS \"fileExtension\", doc_type AS \"docType\", doc_id AS \"docId\" " +
            "FROM t_comment_attach WHERE doc_id = #{docId} AND upper(doc_type) = upper(#{docType}) AND COALESCE(deleted, 0) = 0 ORDER BY id ASC")
    List<Map<String, Object>> selectCommentAttachments(@Param("docId") String docId, @Param("docType") String docType);

    @Select("SELECT project_id FROM hist_wf.biz_project_map " +
            "WHERE business_type = #{businessType} AND bizinst_guid = #{bizinstGuid} LIMIT 1")
    String selectProjectIdByBizinstGuid(@Param("businessType") String businessType,
                                        @Param("bizinstGuid") String bizinstGuid);

}
