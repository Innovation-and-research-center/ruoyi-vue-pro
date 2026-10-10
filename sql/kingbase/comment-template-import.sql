-- KingbaseES（项目现有 PostgreSQL 兼容写法）。
-- 先执行同目录 comment-template.sql；暂存数据与正式数据均由执行者核对。
-- 旧模板迁移骨架。只准备独立 staging 表，不自动导入真实用户数据。
-- 用户映射及 tenant_id 必须由实际迁移清单填写，不按用户名猜测。
-- staging 保留旧表名；正式表使用 bpm_personal_comment_template / bpm_biz_comment_template。
BEGIN;
CREATE SCHEMA IF NOT EXISTS comment_template_migration;
CREATE TABLE IF NOT EXISTS comment_template_migration.user_map (
    old_user_id bigint PRIMARY KEY, new_user_id bigint NOT NULL, tenant_id bigint NOT NULL
);
CREATE TABLE IF NOT EXISTS comment_template_migration.t_oa_idea (
    oa_idea_id bigint PRIMARY KEY, user_id bigint, idea_message varchar(150),
    idea_type varchar(150), idea_order integer
);
CREATE TABLE IF NOT EXISTS comment_template_migration.zpt_bizdef_comment_def (
    comment_guid varchar(50) PRIMARY KEY, bizdef_guid varchar(50), comment_name varchar(50),
    comment_code varchar(50), comment_content varchar(3000), meet_function varchar(500),
    comment_act varchar(4000), seq_order integer, tenant_id bigint NOT NULL
);
COMMIT;

-- 将旧表指定字段导出后，导入上述 staging 表；业务模板额外明确 tenant_id。
-- 完成映射和核对后，单独执行下面的事务。遇到主键冲突直接报错回滚，不覆盖现有模板。
-- BEGIN;
-- DO $$ BEGIN
--   IF EXISTS (SELECT 1 FROM comment_template_migration.t_oa_idea s
--              LEFT JOIN comment_template_migration.user_map m ON m.old_user_id = s.user_id
--              WHERE m.old_user_id IS NULL) THEN
--     RAISE EXCEPTION '存在未映射的旧用户，停止导入';
--   END IF;
-- END $$;
-- INSERT INTO public.bpm_personal_comment_template(oa_idea_id,user_id,idea_message,idea_type,idea_order,tenant_id)
-- SELECT s.oa_idea_id,m.new_user_id,s.idea_message,s.idea_type,COALESCE(s.idea_order,0),m.tenant_id
-- FROM comment_template_migration.t_oa_idea s
-- JOIN comment_template_migration.user_map m ON m.old_user_id=s.user_id;
-- INSERT INTO public.bpm_biz_comment_template
-- (comment_guid,bizdef_guid,comment_name,comment_code,comment_content,meet_function,comment_act,seq_order,tenant_id)
-- SELECT comment_guid,bizdef_guid,comment_name,comment_code,comment_content,meet_function,comment_act,
--        COALESCE(seq_order,0),tenant_id FROM comment_template_migration.zpt_bizdef_comment_def;
-- SELECT setval('public.bpm_personal_comment_template_seq',
--   GREATEST((SELECT CAST(last_value AS bigint) FROM public.bpm_personal_comment_template_seq), COALESCE((SELECT max(oa_idea_id) FROM public.bpm_personal_comment_template),1)), true);
-- COMMIT;
-- 导入后在模板管理页配置 processDefinitionKey → bizdefGuid、ideaType，
-- nodeMapping 示例 {"旧环节ID":"当前taskDefinitionKey"}；
-- fieldMapping 示例 {"旧表名.SUBJECT":"subject"}。占位符正文及旧环节ID保持原样。
-- 其他旧表 T_COMMONBIZ_COMMENT_DEF、T_IDEA_MESSAGE 需先核对是否仍在使用，不混入本批。
