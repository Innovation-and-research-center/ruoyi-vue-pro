-- 在部署包含 applyDeptName 的后端前执行；本脚本不执行历史回填。
BEGIN;
ALTER TABLE public.bpm_leave ADD COLUMN IF NOT EXISTS apply_dept_name varchar(512);
COMMENT ON COLUMN public.bpm_leave.apply_dept_name IS '请假时部门名称快照；历史来源缺失时为空';
COMMIT;

-- 回滚应用可保留此可空列，避免丢失已保存的部门快照。
