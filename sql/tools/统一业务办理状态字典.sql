-- 会议报告单、行政复议、行政诉讼、请假、公出统一使用 bpm_process_instance_status。
-- 仅调整字典展示，不修改任何业务表的 status/spzt 数据。

BEGIN;

UPDATE system_dict_data
SET label = CASE value
        WHEN '1' THEN '办理中'
        WHEN '2' THEN '办理完成'
        WHEN '3' THEN '办理不通过'
        WHEN '4' THEN '已取消'
        WHEN '5' THEN '已作废'
    END,
    sort = CASE value
        WHEN '1' THEN 1 WHEN '2' THEN 2 WHEN '3' THEN 3 WHEN '4' THEN 4 WHEN '5' THEN 5
    END,
    color_type = CASE value
        WHEN '1' THEN 'primary'
        WHEN '2' THEN 'success'
        WHEN '3' THEN 'danger'
        WHEN '4' THEN 'warning'
        WHEN '5' THEN 'danger'
    END,
    updater = '1',
    update_time = CURRENT_TIMESTAMP
WHERE dict_type = 'bpm_process_instance_status'
  AND deleted = 0
  AND value IN ('1', '2', '3', '4', '5');

INSERT INTO system_dict_data (
    id, sort, label, value, dict_type, status, color_type, css_class, remark,
    creator, create_time, updater, update_time, deleted
)
SELECT
    nextval('system_dict_data_seq'), 0, '待办', '0', 'bpm_process_instance_status',
    0, 'info', '', '业务数据已保存，等待登记发送',
    '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data
    WHERE dict_type = 'bpm_process_instance_status' AND value = '0' AND deleted = 0
);

COMMIT;
