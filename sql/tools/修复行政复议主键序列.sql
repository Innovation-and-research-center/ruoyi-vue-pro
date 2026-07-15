-- 行政复议历史数据导入或手工插入后，Kingbase 序列可能落后于表中最大主键。
-- 使用 false 表示下一次 nextval 直接返回这里设置的值（即当前最大 ID + 1）。

SELECT setval(
    'public.t_xzfy_list_seq',
    GREATEST(COALESCE((SELECT MAX(id) FROM public.t_xzfy_list), 0) + 1, 1),
    false
);

SELECT setval(
    'public.t_xzfy_kz_seq',
    GREATEST(COALESCE((SELECT MAX(id) FROM public.t_xzfy_kz), 0) + 1, 1),
    false
);

-- 行政复议附件通过通用附件表保存；历史附件数据也可能导致其序列落后。
SELECT setval(
    'public.t_comment_attach_seq',
    GREATEST(COALESCE((SELECT MAX(id) FROM public.t_comment_attach), 0) + 1, 1),
    false
);

-- 校验：next_value 应大于对应表的 max_id。
SELECT 't_xzfy_list' AS table_name,
       COALESCE((SELECT MAX(id) FROM public.t_xzfy_list), 0) AS max_id,
       last_value AS next_value
FROM public.t_xzfy_list_seq;

SELECT 't_xzfy_kz' AS table_name,
       COALESCE((SELECT MAX(id) FROM public.t_xzfy_kz), 0) AS max_id,
       last_value AS next_value
FROM public.t_xzfy_kz_seq;

SELECT 't_comment_attach' AS table_name,
       COALESCE((SELECT MAX(id) FROM public.t_comment_attach), 0) AS max_id,
       last_value AS next_value
FROM public.t_comment_attach_seq;
