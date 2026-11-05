-- 在部署包含 applyDeptName 的后端前执行；本脚本不执行历史回填。
BEGIN;
ALTER TABLE public.bpm_leave ADD COLUMN IF NOT EXISTS apply_dept_name text;
COMMENT ON COLUMN public.bpm_leave.apply_dept_name IS '请假部门名称，多个部门用顿号分隔；存量数据按回填时当前所属部门补齐';
COMMIT;

-- 回滚应用可保留此可空列，避免丢失已保存的部门快照。
