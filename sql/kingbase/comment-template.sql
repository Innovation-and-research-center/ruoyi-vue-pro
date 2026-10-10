-- KingbaseES：按项目 sql/kingbase/public.sql 的 PostgreSQL 兼容写法整理。
-- 目标 schema 为 public；先在同版本 Kingbase 测试库执行。
-- 未连接或修改业务数据库；此前 PostgreSQL 验证不代表已通过实际 Kingbase 验证。
-- 意见模板：表名遵循 bpm_ 前缀，保留旧系统字段，增加当前系统审计、租户及逻辑删除字段。
-- 本脚本只新增独立对象，不读取或覆盖已有业务数据。先在测试库执行。
BEGIN;
CREATE SEQUENCE IF NOT EXISTS public.bpm_personal_comment_template_seq;
CREATE TABLE IF NOT EXISTS public.bpm_personal_comment_template (
    oa_idea_id bigint PRIMARY KEY DEFAULT nextval('public.bpm_personal_comment_template_seq'),
    user_id bigint NOT NULL,
    idea_message varchar(150) NOT NULL,
    idea_type varchar(150),
    idea_order integer NOT NULL DEFAULT 0,
    creator varchar(64) DEFAULT '', create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '', update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted smallint NOT NULL DEFAULT 0, tenant_id bigint NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_bpm_personal_comment_template_user_type ON public.bpm_personal_comment_template(tenant_id, user_id, idea_type, idea_order);
CREATE TABLE IF NOT EXISTS public.bpm_biz_comment_template (
    comment_guid varchar(50) PRIMARY KEY,
    bizdef_guid varchar(50) NOT NULL,
    comment_name varchar(50), comment_code varchar(50), comment_content varchar(3000),
    meet_function varchar(500), comment_act varchar(4000), seq_order integer NOT NULL DEFAULT 0,
    creator varchar(64) DEFAULT '', create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '', update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted smallint NOT NULL DEFAULT 0, tenant_id bigint NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_bpm_biz_comment_template_biz ON public.bpm_biz_comment_template(tenant_id, bizdef_guid, seq_order);
CREATE SEQUENCE IF NOT EXISTS public.bpm_comment_template_binding_seq;
CREATE TABLE IF NOT EXISTS public.bpm_comment_template_binding (
    id bigint PRIMARY KEY DEFAULT nextval('public.bpm_comment_template_binding_seq'),
    process_definition_key varchar(255) NOT NULL,
    bizdef_guid varchar(50) NOT NULL,
    idea_type varchar(150), source_type varchar(30) NOT NULL,
    node_mapping text NOT NULL DEFAULT '{}', field_mapping text NOT NULL DEFAULT '{}',
    creator varchar(64) DEFAULT '', create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '', update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted smallint NOT NULL DEFAULT 0, tenant_id bigint NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_comment_binding_process ON public.bpm_comment_template_binding(tenant_id, process_definition_key) WHERE deleted = 0;
COMMIT;

-- 管理菜单通过系统“菜单管理”新增：组件 bpm/commentTemplate/index，
-- 路径 comment-template，权限 bpm:comment-template:manage，分配给模板管理员。
-- 不自动给普通用户授予管理权限；个人模板接口只允许管理本人数据。
-- 回退：撤下菜单及前后端版本即可；保留三张表和数据，避免丢失已保存模板。

-- 可选：在工作流程目录下新增管理菜单；只新增菜单，不替任何角色授权。
BEGIN;
DO $$
DECLARE
    parent_menu bigint;
    menu_id bigint;
BEGIN
    SELECT id INTO parent_menu FROM public.system_menu WHERE path = '/bpm' AND type = 1 AND deleted = 0 ORDER BY id LIMIT 1;
    IF parent_menu IS NULL THEN
        RAISE EXCEPTION '未找到工作流程目录，请在菜单管理中手动配置 bpm/commentTemplate/index';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.system_menu WHERE permission = 'bpm:comment-template:manage' AND deleted = 0) THEN
        -- Kingbase 现有 system_menu.id 没有列默认值，必须显式从序列取号。
        -- 若序列落后于已导入的菜单ID，跳过已占用ID，不重置共享序列。
        LOOP
            menu_id := nextval('public.system_menu_seq');
            EXIT WHEN NOT EXISTS (SELECT 1 FROM public.system_menu WHERE id = menu_id);
        END LOOP;
        INSERT INTO public.system_menu(id, name, permission, type, sort, parent_id, path, icon, component, component_name,
                                       status, visible, keep_alive, always_show)
        VALUES (menu_id, '意见模板定义', 'bpm:comment-template:manage', 2, 90, parent_menu, 'comment-template',
                'ep:document', 'bpm/commentTemplate/index', 'BpmCommentTemplate', 0, true, true, true);
    END IF;
END $$;
COMMIT;
