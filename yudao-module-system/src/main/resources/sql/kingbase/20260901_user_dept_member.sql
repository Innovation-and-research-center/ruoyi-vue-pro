-- KingbaseES（PostgreSQL 兼容模式）
-- 员工实际所属多部门关系。与 system_user_dept（关联分管部门）完全独立。

BEGIN;

CREATE SEQUENCE IF NOT EXISTS system_user_dept_member_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS system_user_dept_member (
    id int8 NOT NULL DEFAULT nextval('system_user_dept_member_seq'),
    user_id int8 NOT NULL,
    dept_id int8 NOT NULL,
    sort int8 NOT NULL DEFAULT 0,
    creator varchar(64),
    create_time timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64),
    update_time timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted int2 NOT NULL DEFAULT 0,
    tenant_id int8 NOT NULL DEFAULT 0,
    CONSTRAINT pk_system_user_dept_member PRIMARY KEY (id)
);

COMMENT ON TABLE system_user_dept_member IS '员工实际所属部门关系';
COMMENT ON COLUMN system_user_dept_member.user_id IS '用户编号';
COMMENT ON COLUMN system_user_dept_member.dept_id IS '实际所属部门编号';
COMMENT ON COLUMN system_user_dept_member.sort IS '用户在该部门中的排序';

CREATE UNIQUE INDEX IF NOT EXISTS uk_system_user_dept_member_active
    ON system_user_dept_member (tenant_id, user_id, dept_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_system_user_dept_member_dept_sort
    ON system_user_dept_member (tenant_id, dept_id, sort, user_id)
    WHERE deleted = 0;

-- 先将数据库现有用户的主部门补为实际所属部门。
-- 这样不在旧 Excel 中的测试账号及其主部门也会保留。
INSERT INTO system_user_dept_member
    (id, user_id, dept_id, sort, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_user_dept_member_seq'), u.id, u.dept_id,
       row_number() OVER (PARTITION BY u.tenant_id, u.dept_id ORDER BY u.id),
       'user_dept_member_init', CURRENT_TIMESTAMP,
       'user_dept_member_init', CURRENT_TIMESTAMP, 0, u.tenant_id
FROM system_users u
WHERE u.deleted = 0
  AND u.dept_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM system_user_dept_member m
      WHERE m.tenant_id = u.tenant_id
        AND m.user_id = u.id
        AND m.dept_id = u.dept_id
        AND m.deleted = 0
  );

SELECT setval('system_user_dept_member_seq',
              GREATEST(COALESCE((SELECT max(id) FROM system_user_dept_member), 0) + 1, 1),
              false);

COMMIT;

SELECT count(*) AS active_member_relations
FROM system_user_dept_member
WHERE deleted = 0;
