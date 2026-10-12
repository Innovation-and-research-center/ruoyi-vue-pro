# 短信验证码登录生产更新 SQL

适用数据库：金仓 Kingbase。范围仅包含本次短信验证码登录功能，不包含其他业务改动。本文 SQL 尚未在生产执行。

登录方式为用户名加短信验证码；按用户名找到账号后，优先向 `sms_mobile` 发送短信，未配置则向原 `mobile` 发送。登录模板编码为 `login_code`，验证码为六位随机数。已移除 admin 特殊验证码登录。

## 1. 执行前检查

在应用实际使用的数据库和 schema 下执行，确认 `current_schema()` 与应用表所在 schema 一致。如不一致，先选择正确 schema，或给后续表名加上正确的 schema 前缀。不要对其他同名表执行。

```sql
SELECT current_database(), current_schema();

SELECT table_schema, table_name, column_name, data_type,
       character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND (
       (table_name = 'system_users' AND column_name IN ('mobile', 'sms_mobile'))
    OR (table_name = 'system_sms_code' AND column_name IN ('code', 'used'))
  )
ORDER BY table_name, column_name;
```

检查结果：

- `system_users` 应存在；`sms_mobile` 不存在时执行下一节新增 SQL。
- 已存在的 `sms_mobile` 应为可空 `varchar(11)`；若结构不同，先核对，不要直接覆盖。
- `system_sms_code.code` 至少能存储 6 位字符。
- 当前金仓库的 `system_sms_code.used` 为 `smallint`，保留该类型。新版后端按 0/1 读写，无需转换为 boolean。

## 2. 必需更新 SQL

部署新版后端前执行。脚本可以重复执行，不修改已有手机号，不填充真实用户数据。

```sql
BEGIN;

ALTER TABLE system_users
    ADD COLUMN IF NOT EXISTS sms_mobile varchar(11) DEFAULT NULL;

COMMENT ON COLUMN system_users.sms_mobile
    IS '短信登录验证码接收手机号，为空时使用 mobile';

COMMIT;
```

出现错误时执行 `ROLLBACK;`，确认原因后再重试。新增字段会短暂获取表锁，选择维护窗口执行。

## 3. 登录短信模板和臻善通道检查

以下查询不读取短信通道的账号或密钥，也不读取用户手机号。

```sql
SELECT t.id AS template_id,
       t.code AS template_code,
       t.status AS template_status,
       t.params,
       t.api_template_id,
       t.channel_id,
       t.channel_code AS template_channel_code,
       c.code AS actual_channel_code,
       c.status AS channel_status,
       CASE WHEN POSITION('{code}' IN t.content) > 0
            THEN '是' ELSE '否' END AS contains_code_parameter
FROM system_sms_template t
LEFT JOIN system_sms_channel c
       ON c.id = t.channel_id AND c.deleted = 0
WHERE t.code = 'login_code'
  AND t.deleted = 0;
```

应只有一条有效模板，且满足：

- 模板和通道的 `status` 均为 `0`（启用）。
- `actual_channel_code` 和 `template_channel_code` 均为 `ZHANSHAN`。这是代码中臻善通道的实际枚举值，请保留这个拼写。
- 模板内容包含 `{code}`，`params` 为 `["code"]`。
- `api_template_id` 已按现有臻善模板配置填写。

若查询无结果或配置不符，先在后台“短信模板管理”配置 `login_code` 并关联已有臻善通道。通道 ID、API 模板 ID 和正式短信内容不能凭空填写，因此本文不提供自动创建或覆盖模板的 SQL。

可以用下面的查询查看可用臻善通道，仅返回非敏感字段：

```sql
SELECT id, code, status
FROM system_sms_channel
WHERE code = 'ZHANSHAN' AND deleted = 0;
```

后台登录页面已只保留验证码登录。发布前务必完成模板配置和收短信验证，避免发布后无法登录。若后台当前无法进入，使用经核对的模板数据另行准备定向 SQL，不执行未经确认的通道修改。

## 4. 更新后确认

```sql
SELECT table_schema, table_name, column_name, data_type,
       character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND table_name = 'system_users'
  AND column_name = 'sms_mobile';
```

期望结果为一条记录：`character varying`、长度 `11`、允许为空。

发布顺序：

1. 执行新增字段 SQL，并确认 `login_code` 模板配置正确。
2. 部署并重启新版后端；同步发布新版前端。发送与登录请求已使用 `username`，前后端需要保持一致。
3. 确认最终生效配置为 `yudao.sms-code.begin-code=100000`、`end-code=999999`、`admin-login-template-code=login_code`，避免外部配置仍覆盖成固定 `9999`。
4. 在用户管理设置需要的验证码接收手机号；留空表示使用默认手机号。不要批量覆盖用户原手机号。
5. 用授权的测试账号验证：默认手机号发送、备用手机号发送、共用接收号码时登录正确账号、错误验证码拒绝、验证码使用后不能重复使用。

模板通过后台修改会走现有缓存处理；若另行用 SQL 修改模板，需要按现有运维方式刷新短信模板缓存后再验证。

## 5. 回滚说明

本次只新增可空字段。需要回滚应用时，前后端一起回滚至对应旧版本，保留 `sms_mobile` 字段和已录入的数据即可，无需删除列。不要为了回滚清空手机号或删除验证码记录。

本次不需要修改 `system_sms_code.used`，不需要新增特殊验证码、修改密码、角色、权限或其他业务表。
