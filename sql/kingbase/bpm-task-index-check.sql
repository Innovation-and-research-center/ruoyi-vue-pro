-- 待办/已办索引检查：仅查询元数据，不读取业务记录，不创建或删除索引。
-- 适用人大金仓 PostgreSQL 兼容模式（使用 pg_catalog 兼容视图）。
-- 尚未在目标金仓版本执行；请先单独运行下面的 version() 和兼容视图探测。
-- SELECT version();
-- SELECT to_regclass('pg_catalog.pg_index'), to_regclass('pg_catalog.pg_indexes'),
--        to_regclass('pg_catalog.pg_stat_user_tables');
-- 如果任一视图不存在，请停止，将版本和探测结果交给维护者适配，勿直接执行后续语句。
-- 执行后保留三个结果集：数据库版本、索引覆盖检查、现有索引/表统计。
-- “待核对”仅表示未找到有效的无条件 B-tree 左前缀索引；并非必须新增。
-- 部分索引、表达式索引可能适用，应结合实际 SQL 的 EXPLAIN 再判断。
-- 如需优化，先比较索引定义（包括列顺序、条件和有效性），避免按名称重复建索引。

BEGIN READ ONLY;
SET LOCAL statement_timeout = '15s';

SELECT version() AS database_version, current_database() AS database_name;

WITH expected(schema_name, table_name, leading_columns, query_use) AS (
    VALUES
        ('public', 'act_ru_task', ARRAY['assignee_', 'create_time_'], '待办：办理人和默认创建时间排序'),
        ('public', 'act_ru_identitylink', ARRAY['user_id_', 'type_', 'task_id_'], '候选任务：按候选人定位'),
        ('public', 'act_ru_identitylink', ARRAY['task_id_', 'type_', 'user_id_'], '候选任务：按任务执行 EXISTS；需结合计划判断是否需要反向索引'),
        ('public', 'act_ru_variable', ARRAY['proc_inst_id_', 'name_'], '待办：流程变量筛选'),
        ('public', 'act_ru_variable', ARRAY['execution_id_'], '待办：Flowable 批量加载实例变量，通常由引擎自带索引覆盖'),
        ('public', 'act_hi_taskinst', ARRAY['assignee_', 'end_time_'], '已办：办理人和完成时间'),
        ('public', 'act_hi_varinst', ARRAY['proc_inst_id_', 'name_'], '已办：流程变量和作废状态'),
        ('public', 'act_hi_varinst', ARRAY['task_id_', 'name_'], '已办：任务状态和意见'),
        ('public', 't_file_exchange', ARRAY['doc_id'], '待办展示：收文来源关联交换记录'),
        ('public', 'bpm_receive_doc', ARRAY['process_instance_id', 'status'], '已办：排除作废收文'),
        ('public', 'bpm_leave', ARRAY['process_instance_id', 'spzt'], '已办：排除作废请假'),
        ('public', 't_time_explain', ARRAY['process_instance_id', 'status'], '已办：排除作废公出'),
        ('public', 't_confflow', ARRAY['process_instance_id', 'status'], '已办：排除作废会议'),
        ('public', 't_xzfy_list', ARRAY['process_instance_id', 'status'], '已办：排除作废复议'),
        ('public', 't_xzss_list', ARRAY['process_instance_id', 'status'], '已办：排除作废诉讼'),
        ('public', 't_send_doc', ARRAY['process_instance_id', 'status'], '已办：排除作废发文'),
        ('hist_wf', 'actinst', ARRAY['user_id', 'end_date'], '历史已办：办理人和结束时间'),
        ('hist_wf', 'proinst', ARRAY['source_schema', 'proinst_id'], '历史已办：流程关联，一般由唯一约束覆盖'),
        ('public', 'bpm_receive_doc', ARRAY['project_id'], '历史已办：业务分类；聚合可能仍选择全表扫描'),
        ('public', 'bpm_leave', ARRAY['project_id'], '历史已办：业务分类；聚合可能仍选择全表扫描'),
        ('public', 't_time_explain', ARRAY['project_id'], '历史已办：业务分类；聚合可能仍选择全表扫描'),
        ('public', 't_confflow', ARRAY['project_id'], '历史已办：业务分类；聚合可能仍选择全表扫描')
), actual AS (
    SELECT n.nspname AS schema_name, t.relname AS table_name,
           idx.relname AS index_name,
           ARRAY(
               SELECT lower(a.attname)::text
               FROM unnest(i.indkey::smallint[]) WITH ORDINALITY AS k(attnum, position)
               LEFT JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = k.attnum
               ORDER BY k.position
           ) AS columns,
           pg_get_indexdef(i.indexrelid) AS index_definition
    FROM pg_index i
    JOIN pg_class t ON t.oid = i.indrelid
    JOIN pg_namespace n ON n.oid = t.relnamespace
    JOIN pg_class idx ON idx.oid = i.indexrelid
    JOIN pg_am am ON am.oid = idx.relam
    WHERE i.indisvalid AND i.indisready AND i.indpred IS NULL AND am.amname = 'btree'
      AND n.nspname IN ('public', 'hist_wf')
)
SELECT e.schema_name, e.table_name, e.leading_columns, e.query_use,
       CASE WHEN to_regclass(format('%I.%I', e.schema_name, e.table_name)) IS NULL THEN '表不存在或名称需核对'
            WHEN count(a.index_name) > 0 THEN '已有可用左前缀索引'
            ELSE '待核对执行计划及其他索引' END AS coverage,
       string_agg(a.index_definition, E'\n' ORDER BY a.index_name) AS matching_indexes
FROM expected e
LEFT JOIN actual a ON a.schema_name = e.schema_name AND a.table_name = e.table_name
                 AND a.columns[1:cardinality(e.leading_columns)] = e.leading_columns
GROUP BY e.schema_name, e.table_name, e.leading_columns, e.query_use
ORDER BY e.schema_name, e.table_name, e.query_use;

-- 完整索引清单包含部分索引/表达式索引，便于人工判断是否已覆盖上述条件。
SELECT s.schemaname, s.relname AS table_name, s.n_live_tup AS approximate_live_rows,
       s.last_analyze, s.last_autoanalyze,
       indexes.indexname, indexes.indexdef
FROM pg_stat_user_tables s
LEFT JOIN pg_indexes indexes ON indexes.schemaname = s.schemaname AND indexes.tablename = s.relname
WHERE (s.schemaname = 'public' AND s.relname IN (
        'act_ru_task', 'act_ru_identitylink', 'act_ru_variable', 'act_ru_execution',
        'act_hi_taskinst', 'act_hi_varinst', 'act_hi_procinst', 'system_users',
        'bpm_receive_doc', 'bpm_leave', 't_time_explain', 't_confflow',
        't_xzfy_list', 't_xzss_list', 't_send_doc', 't_file_exchange'))
   OR (s.schemaname = 'hist_wf' AND s.relname IN ('actinst', 'proinst', 'biz_project_map'))
ORDER BY s.schemaname, s.relname, indexes.indexname;

ROLLBACK;

-- 下一步：取得实际慢 SQL（含参数绑定类型），先运行 EXPLAIN (COSTS, VERBOSE)。
-- 不要把 SQL/参数中的真实用户数据或凭据粘贴到公开渠道。
-- EXPLAIN ANALYZE 会实际执行查询，应先在测试库/只读副本运行，设置合理超时。
-- 索引建议优先复用：
--   db/migration/all/05_optimize_todo_done_indexes.sql
--   db/migration/all/06_optimize_business_list_indexes.sql（其中 t_file_exchange 相关索引）
--   db/migration/history-workflow/20_历史详情查询索引优化.sql（其中历史流程、project_id 相关索引）
-- 这些脚本不要整批盲目重跑：IF NOT EXISTS 只检查索引名称，不判断是否已有等价索引。
-- 新增索引仍需确认目标数据库、版本、锁表影响、空间及创建方式；请逐条审核后执行。
