-- 部署新版后端前执行；仅新增字段，不修改已有手机号。
ALTER TABLE system_users ADD COLUMN IF NOT EXISTS sms_mobile varchar(11) DEFAULT NULL;
COMMENT ON COLUMN system_users.sms_mobile IS '短信登录验证码接收手机号，为空时使用 mobile';
