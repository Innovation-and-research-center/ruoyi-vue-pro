-- 可选历史回填：先执行新增字段脚本和 20261103_leave_department_source_precheck.sql。
-- 仅适用于已确认的两批中转表；执行前核对目标数据库和源数据所属租户。
-- 不从当前用户部门推测历史部门，不覆盖已有快照，不用 ACTINST_ID 是否为空筛选。
BEGIN;
WITH source_rows AS (
    SELECT "TIMEEXPLAIN_ID" AS old_id, "PROJECT_ID" AS project_id,
           "USER_ID" AS applicant_id, NULLIF(btrim("DEPTMENT"), '') AS dept_name
    FROM office_src."T_TIME_EXPLAIN" WHERE "FIRST_TYPE" = '因私请假'
    UNION ALL
    SELECT "TIMEEXPLAIN_ID", "PROJECT_ID", "USER_ID", NULLIF(btrim("DEPTMENT"), '')
    FROM office_unfinished_src."T_TIME_EXPLAIN" WHERE "FIRST_TYPE" = '因私请假'
), source_group AS (
    SELECT old_id, project_id, applicant_id, min(dept_name) AS dept_name
    FROM source_rows
    WHERE NULLIF(btrim(project_id), '') IS NOT NULL
    GROUP BY old_id, project_id, applicant_id
    HAVING count(DISTINCT dept_name) = 1
), target_keys AS (
    -- 源表没有租户信息，跨租户重复的业务键不自动回填。
    SELECT id, project_id, userid
    FROM public.bpm_leave
    WHERE COALESCE(deleted, 0) = 0
    GROUP BY id, project_id, userid
    HAVING count(*) = 1
)
UPDATE public.bpm_leave l
SET apply_dept_name = s.dept_name
FROM source_group s, target_keys k
WHERE l.id = s.old_id AND l.project_id = s.project_id AND l.userid = s.applicant_id
  AND l.id = k.id AND l.project_id = k.project_id AND l.userid = k.userid
  AND COALESCE(l.deleted, 0) = 0
  AND NULLIF(btrim(l.apply_dept_name), '') IS NULL
  AND EXISTS (SELECT 1 FROM hist_wf.proinst p
              WHERE p.project_id = l.project_id AND p.prodef_id = 9555);
COMMIT;
