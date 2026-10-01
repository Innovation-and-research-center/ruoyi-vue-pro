-- PostgreSQL: execute before using the role's 办件查看范围 configuration.
CREATE TABLE IF NOT EXISTS bpm_process_view_scope (
    id bigserial PRIMARY KEY,
    role_id bigint NOT NULL,
    process_definition_key varchar(128) NOT NULL,
    tenant_id bigint NOT NULL DEFAULT 0,
    creator varchar(64) DEFAULT '',
    create_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater varchar(64) DEFAULT '',
    update_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted smallint NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, role_id, process_definition_key)
);

COMMENT ON TABLE bpm_process_view_scope IS '角色可查看的办件流程类型';
COMMENT ON COLUMN bpm_process_view_scope.role_id IS '系统角色编号';
COMMENT ON COLUMN bpm_process_view_scope.process_definition_key IS '流程定义标识（含历史办件类型）';
