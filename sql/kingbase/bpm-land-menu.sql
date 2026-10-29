-- KingbaseES / PostgreSQL 兼容：土地用途认定管理菜单及查询、修改、删除权限。
-- 配套页面：bpm/land/index；接口：GET /bpm/land/page。
-- 不建业务表、不自动给角色授权；执行后在角色管理中分配菜单及所需操作权限并重新登录。
-- bpm:land:query 查看当前租户办件；bpm:land:update 管理修改业务字段及附件；
-- bpm:land:delete 作废并取消运行中流程，删除原因进入操作日志；均不自动授权。
-- 默认与请假管理放在同一目录；找不到请假管理则放到 /bpm 工作流程目录。
BEGIN;
LOCK TABLE public.system_menu IN SHARE ROW EXCLUSIVE MODE;
DO $$
DECLARE
    parent_menu bigint;
    land_menu bigint;
    query_menu bigint;
    permission_item record;
BEGIN
    SELECT id INTO land_menu FROM public.system_menu
    WHERE component = 'bpm/land/index' AND deleted = 0 ORDER BY id LIMIT 1;

    IF land_menu IS NULL THEN
        SELECT m.parent_id INTO parent_menu FROM public.system_menu m
        JOIN public.system_menu p ON p.id = m.parent_id AND p.deleted = 0 AND p.type = 1
        WHERE m.component = 'bpm/leave/index' AND m.type = 2 AND m.deleted = 0
        ORDER BY m.id LIMIT 1;
        IF parent_menu IS NULL THEN
            SELECT id INTO parent_menu FROM public.system_menu
            WHERE path = '/bpm' AND type = 1 AND deleted = 0 ORDER BY id LIMIT 1;
        END IF;
        IF parent_menu IS NULL THEN
            RAISE EXCEPTION '未找到父目录，请先配置工作流程目录，或手动新增组件 bpm/land/index 的菜单';
        END IF;
        IF EXISTS (SELECT 1 FROM public.system_menu
                   WHERE parent_id = parent_menu AND path = 'land' AND type <> 3 AND deleted = 0) THEN
            RAISE EXCEPTION '父目录下 land 路由已占用，请在菜单管理中核对，未新增任何菜单';
        END IF;
        -- 显式使用系统序列；跳过已占用 ID，不重置共享序列。
        LOOP
            land_menu := nextval('public.system_menu_seq');
            EXIT WHEN NOT EXISTS (SELECT 1 FROM public.system_menu WHERE id = land_menu);
        END LOOP;
        INSERT INTO public.system_menu
            (id, name, permission, type, sort, parent_id, path, icon, component, component_name,
             status, visible, keep_alive, always_show, creator, updater)
        VALUES
            (land_menu, '土地用途认定管理', '', 2, 95, parent_menu, 'land', 'ep:document',
             'bpm/land/index', 'BpmLand', 0, true, true, true, '1', '1');
    END IF;

    FOR permission_item IN
        SELECT * FROM (VALUES
            ('土地用途认定查询', 'bpm:land:query', 1),
            ('土地用途认定修改', 'bpm:land:update', 2),
            ('土地用途认定删除', 'bpm:land:delete', 3)
        ) AS permissions(name, code, sort)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM public.system_menu
                       WHERE parent_id = land_menu AND permission = permission_item.code AND deleted = 0) THEN
            LOOP
                query_menu := nextval('public.system_menu_seq');
                EXIT WHEN NOT EXISTS (SELECT 1 FROM public.system_menu WHERE id = query_menu);
            END LOOP;
            INSERT INTO public.system_menu
                (id, name, permission, type, sort, parent_id, path, icon, status,
                 visible, keep_alive, always_show, creator, updater)
            VALUES
                (query_menu, permission_item.name, permission_item.code, 3, permission_item.sort, land_menu, '', '#',
                 0, true, true, true, '1', '1');
        END IF;
    END LOOP;
END $$;
COMMIT;

-- 回退：在菜单管理中撤下上述菜单及角色授权，保留土地业务表和数据。
