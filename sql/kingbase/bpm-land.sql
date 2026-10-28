-- 土地用途认定（流程标识 lang_use）业务表。
-- 方言：KingbaseES PostgreSQL 兼容模式 / PostgreSQL，目标 schema：public。
-- 仅提供建表脚本，未连接或执行数据库。请先在对应版本的测试库验证。
-- 首次初始化使用：对象已存在时直接报错并回滚，不删除或覆盖已有表及数据。
-- 后续 DO 对应序列：bpm_land_seq、bpm_land_attach_seq。
-- fileList、taskId、fileUrl 为接口关联/派生数据，不作为主表字段。
-- 后续办理意见、选人和会签记录沿用工作流，不在业务表重复存储。

BEGIN;

CREATE SEQUENCE public.bpm_land_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE public.bpm_land (
    id                  bigint PRIMARY KEY DEFAULT nextval('public.bpm_land_seq'),
    project_name        varchar(200) NOT NULL,
    location            varchar(500) NOT NULL,
    dept_opinion        varchar(4000) NOT NULL DEFAULT '',
    apply_date          timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id             bigint NOT NULL,
    dept_id             bigint,
    start_unit          varchar(128) NOT NULL DEFAULT '登记中心',
    start_user_name     varchar(128),
    dept_name           varchar(128),
    process_instance_id varchar(64),
    status              smallint NOT NULL DEFAULT 1,
    filepath            text,
    creator             varchar(64) NOT NULL DEFAULT '',
    create_time         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64) NOT NULL DEFAULT '',
    update_time         timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             smallint NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    tenant_id           bigint NOT NULL DEFAULT 0
);

ALTER SEQUENCE public.bpm_land_seq OWNED BY public.bpm_land.id;

COMMENT ON TABLE public.bpm_land IS '土地用途认定业务表，关联流程 lang_use';
COMMENT ON COLUMN public.bpm_land.id IS '业务主键，作为流程 businessKey';
COMMENT ON COLUMN public.bpm_land.project_name IS '项目名称，对应 projectName';
COMMENT ON COLUMN public.bpm_land.location IS '土地坐落，对应 location';
COMMENT ON COLUMN public.bpm_land.dept_opinion IS '发起科室意见，对应 deptOpinion；不覆盖后续任务意见';
COMMENT ON COLUMN public.bpm_land.apply_date IS '登记日期，对应 applyDate';
COMMENT ON COLUMN public.bpm_land.user_id IS '发起人用户编号，由服务端根据登录身份写入';
COMMENT ON COLUMN public.bpm_land.dept_id IS '发起时账号主部门编号，与业务发起单位区分';
COMMENT ON COLUMN public.bpm_land.start_unit IS '业务发起单位，默认登记中心';
COMMENT ON COLUMN public.bpm_land.start_user_name IS '发起人姓名快照，对应 startUserName';
COMMENT ON COLUMN public.bpm_land.dept_name IS '发起时所属科室名称快照，对应 deptName';
COMMENT ON COLUMN public.bpm_land.process_instance_id IS '流程实例编号，对应 processInstanceId；尚未关联流程时为 NULL';
COMMENT ON COLUMN public.bpm_land.status IS '流程实例状态：-1未开始，1审批中，2审批通过，3审批不通过，4已取消，5已作废；与 BpmProcessInstanceStatusEnum 一致';
COMMENT ON COLUMN public.bpm_land.filepath IS '兼容上传组件的文件地址字符串；附件关联以 bpm_land_attach 为准';
COMMENT ON COLUMN public.bpm_land.creator IS '创建人用户编号';
COMMENT ON COLUMN public.bpm_land.create_time IS '创建时间';
COMMENT ON COLUMN public.bpm_land.updater IS '更新人用户编号';
COMMENT ON COLUMN public.bpm_land.update_time IS '更新时间，由应用更新时维护';
COMMENT ON COLUMN public.bpm_land.deleted IS '逻辑删除：0正常，1已删除';
COMMENT ON COLUMN public.bpm_land.tenant_id IS '租户编号';

CREATE UNIQUE INDEX uk_bpm_land_process
    ON public.bpm_land (tenant_id, process_instance_id)
    WHERE deleted = 0 AND process_instance_id IS NOT NULL;

CREATE INDEX idx_bpm_land_user_created
    ON public.bpm_land (tenant_id, deleted, user_id, create_time DESC, id DESC);

CREATE SEQUENCE public.bpm_land_attach_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE public.bpm_land_attach (
    id              bigint PRIMARY KEY DEFAULT nextval('public.bpm_land_attach_seq'),
    land_id         bigint NOT NULL,
    attach_file_id  bigint NOT NULL,
    file_path       varchar(2048) NOT NULL,
    file_name       varchar(512) NOT NULL,
    file_extension  varchar(50),
    sort            integer NOT NULL DEFAULT 0,
    creator         varchar(64) NOT NULL DEFAULT '',
    create_time     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64) NOT NULL DEFAULT '',
    update_time     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         smallint NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    tenant_id       bigint NOT NULL DEFAULT 0
);

ALTER SEQUENCE public.bpm_land_attach_seq OWNED BY public.bpm_land_attach.id;

COMMENT ON TABLE public.bpm_land_attach IS '土地用途认定附件关联表，附件本体复用现有文件服务';
COMMENT ON COLUMN public.bpm_land_attach.id IS '附件关联主键';
COMMENT ON COLUMN public.bpm_land_attach.land_id IS '所属土地用途认定业务编号，对应 bpm_land.id';
COMMENT ON COLUMN public.bpm_land_attach.attach_file_id IS '现有文件服务文件编号，对应 attachFileId';
COMMENT ON COLUMN public.bpm_land_attach.file_path IS '文件存储路径，对应 filePath；访问地址由文件服务生成';
COMMENT ON COLUMN public.bpm_land_attach.file_name IS '附件名称，对应 fileName';
COMMENT ON COLUMN public.bpm_land_attach.file_extension IS '文件扩展名，对应 fileExtension';
COMMENT ON COLUMN public.bpm_land_attach.sort IS '附件顺序，后端按 fileList 顺序写入';
COMMENT ON COLUMN public.bpm_land_attach.creator IS '创建人用户编号';
COMMENT ON COLUMN public.bpm_land_attach.create_time IS '创建时间';
COMMENT ON COLUMN public.bpm_land_attach.updater IS '更新人用户编号';
COMMENT ON COLUMN public.bpm_land_attach.update_time IS '更新时间，由应用更新时维护';
COMMENT ON COLUMN public.bpm_land_attach.deleted IS '逻辑删除：0正常，1已删除';
COMMENT ON COLUMN public.bpm_land_attach.tenant_id IS '租户编号，与所属业务一致';

CREATE INDEX idx_bpm_land_attach_land
    ON public.bpm_land_attach (tenant_id, land_id, deleted, sort, id);

COMMIT;

-- 与现有业务表一致，业务和文件关联由应用层校验，未对已有用户/文件表添加外键。
-- 未包含菜单、权限、流程发布、用户组或真实业务数据。
-- 回退说明：若应用回退，保留上述两表及数据；不自动执行 DROP 或清理业务数据。
