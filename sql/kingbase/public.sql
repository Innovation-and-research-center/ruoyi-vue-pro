/*
 Navicat Premium Data Transfer

 Source Server         : oa
 Source Server Type    : PostgreSQL
 Source Server Version : 120001 (120001)
 Source Host           : 192.168.10.29:54321
 Source Catalog        : test
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 120001 (120001)
 File Encoding         : 65001

 Date: 25/11/2025 16:33:31
*/


-- ----------------------------
-- Sequence structure for act_evt_log_log_nr__seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."act_evt_log_log_nr__seq";
CREATE SEQUENCE "public"."act_evt_log_log_nr__seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;
ALTER SEQUENCE "public"."act_evt_log_log_nr__seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for act_hi_tsk_log_id__seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."act_hi_tsk_log_id__seq";
CREATE SEQUENCE "public"."act_hi_tsk_log_id__seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;
ALTER SEQUENCE "public"."act_hi_tsk_log_id__seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_api_access_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_api_access_log_seq";
CREATE SEQUENCE "public"."infra_api_access_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_api_access_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_api_error_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_api_error_log_seq";
CREATE SEQUENCE "public"."infra_api_error_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_api_error_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_codegen_column_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_codegen_column_seq";
CREATE SEQUENCE "public"."infra_codegen_column_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_codegen_column_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_codegen_table_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_codegen_table_seq";
CREATE SEQUENCE "public"."infra_codegen_table_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_codegen_table_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_config_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_config_seq";
CREATE SEQUENCE "public"."infra_config_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 14
CACHE 1;
ALTER SEQUENCE "public"."infra_config_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_data_source_config_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_data_source_config_seq";
CREATE SEQUENCE "public"."infra_data_source_config_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_data_source_config_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_file_config_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_file_config_seq";
CREATE SEQUENCE "public"."infra_file_config_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 31
CACHE 1;
ALTER SEQUENCE "public"."infra_file_config_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_file_content_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_file_content_seq";
CREATE SEQUENCE "public"."infra_file_content_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_file_content_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_file_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_file_seq";
CREATE SEQUENCE "public"."infra_file_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_file_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_job_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_job_log_seq";
CREATE SEQUENCE "public"."infra_job_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."infra_job_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for infra_job_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."infra_job_seq";
CREATE SEQUENCE "public"."infra_job_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 36
CACHE 1;
ALTER SEQUENCE "public"."infra_job_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_dept_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_dept_seq";
CREATE SEQUENCE "public"."system_dept_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 114
CACHE 1;
ALTER SEQUENCE "public"."system_dept_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_dict_data_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_dict_data_seq";
CREATE SEQUENCE "public"."system_dict_data_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 3003
CACHE 1;
ALTER SEQUENCE "public"."system_dict_data_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_dict_type_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_dict_type_seq";
CREATE SEQUENCE "public"."system_dict_type_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1014
CACHE 1;
ALTER SEQUENCE "public"."system_dict_type_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_login_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_login_log_seq";
CREATE SEQUENCE "public"."system_login_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_login_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_mail_account_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_mail_account_seq";
CREATE SEQUENCE "public"."system_mail_account_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 5
CACHE 1;
ALTER SEQUENCE "public"."system_mail_account_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_mail_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_mail_log_seq";
CREATE SEQUENCE "public"."system_mail_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_mail_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_mail_template_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_mail_template_seq";
CREATE SEQUENCE "public"."system_mail_template_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 16
CACHE 1;
ALTER SEQUENCE "public"."system_mail_template_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_menu_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_menu_seq";
CREATE SEQUENCE "public"."system_menu_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 5013
CACHE 1;
ALTER SEQUENCE "public"."system_menu_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_notice_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_notice_seq";
CREATE SEQUENCE "public"."system_notice_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 5
CACHE 1;
ALTER SEQUENCE "public"."system_notice_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_notify_message_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_notify_message_seq";
CREATE SEQUENCE "public"."system_notify_message_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 11
CACHE 1;
ALTER SEQUENCE "public"."system_notify_message_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_notify_template_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_notify_template_seq";
CREATE SEQUENCE "public"."system_notify_template_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_notify_template_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_oauth2_access_token_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_oauth2_access_token_seq";
CREATE SEQUENCE "public"."system_oauth2_access_token_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_oauth2_access_token_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_oauth2_approve_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_oauth2_approve_seq";
CREATE SEQUENCE "public"."system_oauth2_approve_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_oauth2_approve_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_oauth2_client_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_oauth2_client_seq";
CREATE SEQUENCE "public"."system_oauth2_client_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 43
CACHE 1;
ALTER SEQUENCE "public"."system_oauth2_client_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_oauth2_code_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_oauth2_code_seq";
CREATE SEQUENCE "public"."system_oauth2_code_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_oauth2_code_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_oauth2_refresh_token_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_oauth2_refresh_token_seq";
CREATE SEQUENCE "public"."system_oauth2_refresh_token_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_oauth2_refresh_token_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_operate_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_operate_log_seq";
CREATE SEQUENCE "public"."system_operate_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_operate_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_post_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_post_seq";
CREATE SEQUENCE "public"."system_post_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 6
CACHE 1;
ALTER SEQUENCE "public"."system_post_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_role_menu_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_role_menu_seq";
CREATE SEQUENCE "public"."system_role_menu_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 6139
CACHE 1;
ALTER SEQUENCE "public"."system_role_menu_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_role_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_role_seq";
CREATE SEQUENCE "public"."system_role_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 159
CACHE 1;
ALTER SEQUENCE "public"."system_role_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_sms_channel_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_sms_channel_seq";
CREATE SEQUENCE "public"."system_sms_channel_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 8
CACHE 1;
ALTER SEQUENCE "public"."system_sms_channel_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_sms_code_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_sms_code_seq";
CREATE SEQUENCE "public"."system_sms_code_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_sms_code_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_sms_log_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_sms_log_seq";
CREATE SEQUENCE "public"."system_sms_log_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_sms_log_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_sms_template_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_sms_template_seq";
CREATE SEQUENCE "public"."system_sms_template_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 20
CACHE 1;
ALTER SEQUENCE "public"."system_sms_template_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_social_client_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_social_client_seq";
CREATE SEQUENCE "public"."system_social_client_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 45
CACHE 1;
ALTER SEQUENCE "public"."system_social_client_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_social_user_bind_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_social_user_bind_seq";
CREATE SEQUENCE "public"."system_social_user_bind_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_social_user_bind_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_social_user_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_social_user_seq";
CREATE SEQUENCE "public"."system_social_user_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;
ALTER SEQUENCE "public"."system_social_user_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_tenant_package_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_tenant_package_seq";
CREATE SEQUENCE "public"."system_tenant_package_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 113
CACHE 1;
ALTER SEQUENCE "public"."system_tenant_package_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_tenant_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_tenant_seq";
CREATE SEQUENCE "public"."system_tenant_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 123
CACHE 1;
ALTER SEQUENCE "public"."system_tenant_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_user_post_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_user_post_seq";
CREATE SEQUENCE "public"."system_user_post_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 126
CACHE 1;
ALTER SEQUENCE "public"."system_user_post_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_user_role_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_user_role_seq";
CREATE SEQUENCE "public"."system_user_role_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 49
CACHE 1;
ALTER SEQUENCE "public"."system_user_role_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for system_users_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."system_users_seq";
CREATE SEQUENCE "public"."system_users_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 142
CACHE 1;
ALTER SEQUENCE "public"."system_users_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for yudao_demo01_contact_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."yudao_demo01_contact_seq";
CREATE SEQUENCE "public"."yudao_demo01_contact_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 2
CACHE 1;
ALTER SEQUENCE "public"."yudao_demo01_contact_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for yudao_demo02_category_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."yudao_demo02_category_seq";
CREATE SEQUENCE "public"."yudao_demo02_category_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 7
CACHE 1;
ALTER SEQUENCE "public"."yudao_demo02_category_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for yudao_demo03_course_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."yudao_demo03_course_seq";
CREATE SEQUENCE "public"."yudao_demo03_course_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 21
CACHE 1;
ALTER SEQUENCE "public"."yudao_demo03_course_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for yudao_demo03_grade_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."yudao_demo03_grade_seq";
CREATE SEQUENCE "public"."yudao_demo03_grade_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 10
CACHE 1;
ALTER SEQUENCE "public"."yudao_demo03_grade_seq" OWNER TO "system";

-- ----------------------------
-- Sequence structure for yudao_demo03_student_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."yudao_demo03_student_seq";
CREATE SEQUENCE "public"."yudao_demo03_student_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 10
CACHE 1;
ALTER SEQUENCE "public"."yudao_demo03_student_seq" OWNER TO "system";

-- ----------------------------
-- Table structure for act_evt_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_evt_log";
CREATE TABLE "public"."act_evt_log" (
  "log_nr_" int4 NOT NULL DEFAULT nextval('act_evt_log_log_nr__seq'::regclass),
  "type_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "time_stamp_" timestamp(6) NOT NULL,
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "data_" bytea,
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "lock_time_" timestamp(6),
  "is_processed_" int2 DEFAULT 0
)
;
ALTER TABLE "public"."act_evt_log" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ge_bytearray
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ge_bytearray";
CREATE TABLE "public"."act_ge_bytearray" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "deployment_id_" varchar(64) COLLATE "pg_catalog"."default",
  "bytes_" bytea,
  "generated_" bool
)
;
ALTER TABLE "public"."act_ge_bytearray" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ge_property
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ge_property";
CREATE TABLE "public"."act_ge_property" (
  "name_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "value_" varchar(300) COLLATE "pg_catalog"."default",
  "rev_" int4
)
;
ALTER TABLE "public"."act_ge_property" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_actinst
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_actinst";
CREATE TABLE "public"."act_hi_actinst" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4 DEFAULT 1,
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "act_id_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "call_proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "act_name_" varchar(255) COLLATE "pg_catalog"."default",
  "act_type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "assignee_" varchar(255) COLLATE "pg_catalog"."default",
  "start_time_" timestamp(6) NOT NULL,
  "end_time_" timestamp(6),
  "transaction_order_" int4,
  "duration_" int8,
  "delete_reason_" varchar(4000) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_hi_actinst" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_attachment
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_attachment";
CREATE TABLE "public"."act_hi_attachment" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "description_" varchar(4000) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "url_" varchar(4000) COLLATE "pg_catalog"."default",
  "content_id_" varchar(64) COLLATE "pg_catalog"."default",
  "time_" timestamp(6)
)
;
ALTER TABLE "public"."act_hi_attachment" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_comment
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_comment";
CREATE TABLE "public"."act_hi_comment" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "type_" varchar(255) COLLATE "pg_catalog"."default",
  "time_" timestamp(6) NOT NULL,
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "action_" varchar(255) COLLATE "pg_catalog"."default",
  "message_" varchar(4000) COLLATE "pg_catalog"."default",
  "full_msg_" bytea
)
;
ALTER TABLE "public"."act_hi_comment" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_detail
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_detail";
CREATE TABLE "public"."act_hi_detail" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "act_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "name_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "var_type_" varchar(64) COLLATE "pg_catalog"."default",
  "rev_" int4,
  "time_" timestamp(6) NOT NULL,
  "bytearray_id_" varchar(64) COLLATE "pg_catalog"."default",
  "double_" float8,
  "long_" int8,
  "text_" varchar(4000) COLLATE "pg_catalog"."default",
  "text2_" varchar(4000) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_hi_detail" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_entitylink
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_entitylink";
CREATE TABLE "public"."act_hi_entitylink" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "link_type_" varchar(255) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "parent_element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "ref_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "ref_scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "ref_scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "root_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "root_scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "hierarchy_type_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_hi_entitylink" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_identitylink
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_identitylink";
CREATE TABLE "public"."act_hi_identitylink" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "group_id_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default",
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_hi_identitylink" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_procinst
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_procinst";
CREATE TABLE "public"."act_hi_procinst" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4 DEFAULT 1,
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "business_key_" varchar(255) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "start_time_" timestamp(6) NOT NULL,
  "end_time_" timestamp(6),
  "duration_" int8,
  "start_user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "start_act_id_" varchar(255) COLLATE "pg_catalog"."default",
  "end_act_id_" varchar(255) COLLATE "pg_catalog"."default",
  "super_process_instance_id_" varchar(64) COLLATE "pg_catalog"."default",
  "delete_reason_" varchar(4000) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "callback_id_" varchar(255) COLLATE "pg_catalog"."default",
  "callback_type_" varchar(255) COLLATE "pg_catalog"."default",
  "reference_id_" varchar(255) COLLATE "pg_catalog"."default",
  "reference_type_" varchar(255) COLLATE "pg_catalog"."default",
  "propagated_stage_inst_id_" varchar(255) COLLATE "pg_catalog"."default",
  "business_status_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_hi_procinst" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_taskinst
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_taskinst";
CREATE TABLE "public"."act_hi_taskinst" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4 DEFAULT 1,
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_def_key_" varchar(255) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "propagated_stage_inst_id_" varchar(255) COLLATE "pg_catalog"."default",
  "state_" varchar(255) COLLATE "pg_catalog"."default",
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "parent_task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "description_" varchar(4000) COLLATE "pg_catalog"."default",
  "owner_" varchar(255) COLLATE "pg_catalog"."default",
  "assignee_" varchar(255) COLLATE "pg_catalog"."default",
  "start_time_" timestamp(6) NOT NULL,
  "in_progress_time_" timestamp(6),
  "in_progress_started_by_" varchar(255) COLLATE "pg_catalog"."default",
  "claim_time_" timestamp(6),
  "claimed_by_" varchar(255) COLLATE "pg_catalog"."default",
  "suspended_time_" timestamp(6),
  "suspended_by_" varchar(255) COLLATE "pg_catalog"."default",
  "end_time_" timestamp(6),
  "completed_by_" varchar(255) COLLATE "pg_catalog"."default",
  "duration_" int8,
  "delete_reason_" varchar(4000) COLLATE "pg_catalog"."default",
  "priority_" int4,
  "in_progress_due_date_" timestamp(6),
  "due_date_" timestamp(6),
  "form_key_" varchar(255) COLLATE "pg_catalog"."default",
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "last_updated_time_" timestamp(6)
)
;
ALTER TABLE "public"."act_hi_taskinst" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_tsk_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_tsk_log";
CREATE TABLE "public"."act_hi_tsk_log" (
  "id_" int4 NOT NULL DEFAULT nextval('act_hi_tsk_log_id__seq'::regclass),
  "type_" varchar(64) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "time_stamp_" timestamp(6) NOT NULL,
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "data_" varchar(4000) COLLATE "pg_catalog"."default",
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_hi_tsk_log" OWNER TO "system";

-- ----------------------------
-- Table structure for act_hi_varinst
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_hi_varinst";
CREATE TABLE "public"."act_hi_varinst" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4 DEFAULT 1,
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "name_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "var_type_" varchar(100) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "bytearray_id_" varchar(64) COLLATE "pg_catalog"."default",
  "double_" float8,
  "long_" int8,
  "text_" varchar(4000) COLLATE "pg_catalog"."default",
  "text2_" varchar(4000) COLLATE "pg_catalog"."default",
  "meta_info_" varchar(4000) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "last_updated_time_" timestamp(6)
)
;
ALTER TABLE "public"."act_hi_varinst" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_bytearray
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_bytearray";
CREATE TABLE "public"."act_id_bytearray" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "bytes_" bytea
)
;
ALTER TABLE "public"."act_id_bytearray" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_group
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_group";
CREATE TABLE "public"."act_id_group" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_id_group" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_info
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_info";
CREATE TABLE "public"."act_id_info" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "user_id_" varchar(64) COLLATE "pg_catalog"."default",
  "type_" varchar(64) COLLATE "pg_catalog"."default",
  "key_" varchar(255) COLLATE "pg_catalog"."default",
  "value_" varchar(255) COLLATE "pg_catalog"."default",
  "password_" bytea,
  "parent_id_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_id_info" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_membership
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_membership";
CREATE TABLE "public"."act_id_membership" (
  "user_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "group_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL
)
;
ALTER TABLE "public"."act_id_membership" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_priv
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_priv";
CREATE TABLE "public"."act_id_priv" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL
)
;
ALTER TABLE "public"."act_id_priv" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_priv_mapping
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_priv_mapping";
CREATE TABLE "public"."act_id_priv_mapping" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "priv_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "group_id_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_id_priv_mapping" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_property
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_property";
CREATE TABLE "public"."act_id_property" (
  "name_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "value_" varchar(300) COLLATE "pg_catalog"."default",
  "rev_" int4
)
;
ALTER TABLE "public"."act_id_property" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_token
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_token";
CREATE TABLE "public"."act_id_token" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "token_value_" varchar(255) COLLATE "pg_catalog"."default",
  "token_date_" timestamp(6),
  "ip_address_" varchar(255) COLLATE "pg_catalog"."default",
  "user_agent_" varchar(255) COLLATE "pg_catalog"."default",
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "token_data_" varchar(2000) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_id_token" OWNER TO "system";

-- ----------------------------
-- Table structure for act_id_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_id_user";
CREATE TABLE "public"."act_id_user" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "first_" varchar(255) COLLATE "pg_catalog"."default",
  "last_" varchar(255) COLLATE "pg_catalog"."default",
  "display_name_" varchar(255) COLLATE "pg_catalog"."default",
  "email_" varchar(255) COLLATE "pg_catalog"."default",
  "pwd_" varchar(255) COLLATE "pg_catalog"."default",
  "picture_id_" varchar(64) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_id_user" OWNER TO "system";

-- ----------------------------
-- Table structure for act_procdef_info
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_procdef_info";
CREATE TABLE "public"."act_procdef_info" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "info_json_id_" varchar(64) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_procdef_info" OWNER TO "system";

-- ----------------------------
-- Table structure for act_re_deployment
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_re_deployment";
CREATE TABLE "public"."act_re_deployment" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "key_" varchar(255) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "deploy_time_" timestamp(6),
  "derived_from_" varchar(64) COLLATE "pg_catalog"."default",
  "derived_from_root_" varchar(64) COLLATE "pg_catalog"."default",
  "parent_deployment_id_" varchar(255) COLLATE "pg_catalog"."default",
  "engine_version_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_re_deployment" OWNER TO "system";

-- ----------------------------
-- Table structure for act_re_model
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_re_model";
CREATE TABLE "public"."act_re_model" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "key_" varchar(255) COLLATE "pg_catalog"."default",
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "last_update_time_" timestamp(6),
  "version_" int4,
  "meta_info_" varchar(4000) COLLATE "pg_catalog"."default",
  "deployment_id_" varchar(64) COLLATE "pg_catalog"."default",
  "editor_source_value_id_" varchar(64) COLLATE "pg_catalog"."default",
  "editor_source_extra_value_id_" varchar(64) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_re_model" OWNER TO "system";

-- ----------------------------
-- Table structure for act_re_procdef
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_re_procdef";
CREATE TABLE "public"."act_re_procdef" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "key_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "version_" int4 NOT NULL,
  "deployment_id_" varchar(64) COLLATE "pg_catalog"."default",
  "resource_name_" varchar(4000) COLLATE "pg_catalog"."default",
  "dgrm_resource_name_" varchar(4000) COLLATE "pg_catalog"."default",
  "description_" varchar(4000) COLLATE "pg_catalog"."default",
  "has_start_form_key_" bool,
  "has_graphical_notation_" bool,
  "suspension_state_" int4,
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "derived_from_" varchar(64) COLLATE "pg_catalog"."default",
  "derived_from_root_" varchar(64) COLLATE "pg_catalog"."default",
  "derived_version_" int4 NOT NULL DEFAULT 0,
  "engine_version_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_re_procdef" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_actinst
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_actinst";
CREATE TABLE "public"."act_ru_actinst" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4 DEFAULT 1,
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "act_id_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "call_proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "act_name_" varchar(255) COLLATE "pg_catalog"."default",
  "act_type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "assignee_" varchar(255) COLLATE "pg_catalog"."default",
  "start_time_" timestamp(6) NOT NULL,
  "end_time_" timestamp(6),
  "duration_" int8,
  "transaction_order_" int4,
  "delete_reason_" varchar(4000) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_actinst" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_deadletter_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_deadletter_job";
CREATE TABLE "public"."act_ru_deadletter_job" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "exclusive_" bool,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "process_instance_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "element_name_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "correlation_id_" varchar(255) COLLATE "pg_catalog"."default",
  "exception_stack_id_" varchar(64) COLLATE "pg_catalog"."default",
  "exception_msg_" varchar(4000) COLLATE "pg_catalog"."default",
  "duedate_" timestamp(6),
  "repeat_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_type_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_cfg_" varchar(4000) COLLATE "pg_catalog"."default",
  "custom_values_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_deadletter_job" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_entitylink
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_entitylink";
CREATE TABLE "public"."act_ru_entitylink" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "create_time_" timestamp(6),
  "link_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "parent_element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "ref_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "ref_scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "ref_scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "root_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "root_scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "hierarchy_type_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_ru_entitylink" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_event_subscr
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_event_subscr";
CREATE TABLE "public"."act_ru_event_subscr" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "event_type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "event_name_" varchar(255) COLLATE "pg_catalog"."default",
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "activity_id_" varchar(64) COLLATE "pg_catalog"."default",
  "configuration_" varchar(255) COLLATE "pg_catalog"."default",
  "created_" timestamp(6) NOT NULL,
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_definition_key_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(64) COLLATE "pg_catalog"."default",
  "lock_time_" timestamp(6),
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_event_subscr" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_execution
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_execution";
CREATE TABLE "public"."act_ru_execution" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "business_key_" varchar(255) COLLATE "pg_catalog"."default",
  "parent_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "super_exec_" varchar(64) COLLATE "pg_catalog"."default",
  "root_proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "act_id_" varchar(255) COLLATE "pg_catalog"."default",
  "is_active_" bool,
  "is_concurrent_" bool,
  "is_scope_" bool,
  "is_event_scope_" bool,
  "is_mi_root_" bool,
  "suspension_state_" int4,
  "cached_ent_state_" int4,
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "start_act_id_" varchar(255) COLLATE "pg_catalog"."default",
  "start_time_" timestamp(6),
  "start_user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "lock_time_" timestamp(6),
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "is_count_enabled_" bool,
  "evt_subscr_count_" int4,
  "task_count_" int4,
  "job_count_" int4,
  "timer_job_count_" int4,
  "susp_job_count_" int4,
  "deadletter_job_count_" int4,
  "external_worker_job_count_" int4,
  "var_count_" int4,
  "id_link_count_" int4,
  "callback_id_" varchar(255) COLLATE "pg_catalog"."default",
  "callback_type_" varchar(255) COLLATE "pg_catalog"."default",
  "reference_id_" varchar(255) COLLATE "pg_catalog"."default",
  "reference_type_" varchar(255) COLLATE "pg_catalog"."default",
  "propagated_stage_inst_id_" varchar(255) COLLATE "pg_catalog"."default",
  "business_status_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_ru_execution" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_external_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_external_job";
CREATE TABLE "public"."act_ru_external_job" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "lock_exp_time_" timestamp(6),
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "exclusive_" bool,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "process_instance_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "element_name_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "correlation_id_" varchar(255) COLLATE "pg_catalog"."default",
  "retries_" int4,
  "exception_stack_id_" varchar(64) COLLATE "pg_catalog"."default",
  "exception_msg_" varchar(4000) COLLATE "pg_catalog"."default",
  "duedate_" timestamp(6),
  "repeat_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_type_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_cfg_" varchar(4000) COLLATE "pg_catalog"."default",
  "custom_values_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_external_job" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_history_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_history_job";
CREATE TABLE "public"."act_ru_history_job" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "lock_exp_time_" timestamp(6),
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "retries_" int4,
  "exception_stack_id_" varchar(64) COLLATE "pg_catalog"."default",
  "exception_msg_" varchar(4000) COLLATE "pg_catalog"."default",
  "handler_type_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_cfg_" varchar(4000) COLLATE "pg_catalog"."default",
  "custom_values_id_" varchar(64) COLLATE "pg_catalog"."default",
  "adv_handler_cfg_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_history_job" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_identitylink
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_identitylink";
CREATE TABLE "public"."act_ru_identitylink" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "group_id_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default",
  "user_id_" varchar(255) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_ru_identitylink" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_job";
CREATE TABLE "public"."act_ru_job" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "lock_exp_time_" timestamp(6),
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "exclusive_" bool,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "process_instance_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "element_name_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "correlation_id_" varchar(255) COLLATE "pg_catalog"."default",
  "retries_" int4,
  "exception_stack_id_" varchar(64) COLLATE "pg_catalog"."default",
  "exception_msg_" varchar(4000) COLLATE "pg_catalog"."default",
  "duedate_" timestamp(6),
  "repeat_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_type_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_cfg_" varchar(4000) COLLATE "pg_catalog"."default",
  "custom_values_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_job" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_suspended_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_suspended_job";
CREATE TABLE "public"."act_ru_suspended_job" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "exclusive_" bool,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "process_instance_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "element_name_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "correlation_id_" varchar(255) COLLATE "pg_catalog"."default",
  "retries_" int4,
  "exception_stack_id_" varchar(64) COLLATE "pg_catalog"."default",
  "exception_msg_" varchar(4000) COLLATE "pg_catalog"."default",
  "duedate_" timestamp(6),
  "repeat_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_type_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_cfg_" varchar(4000) COLLATE "pg_catalog"."default",
  "custom_values_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_suspended_job" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_task
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_task";
CREATE TABLE "public"."act_ru_task" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "propagated_stage_inst_id_" varchar(255) COLLATE "pg_catalog"."default",
  "state_" varchar(255) COLLATE "pg_catalog"."default",
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "parent_task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "description_" varchar(4000) COLLATE "pg_catalog"."default",
  "task_def_key_" varchar(255) COLLATE "pg_catalog"."default",
  "owner_" varchar(255) COLLATE "pg_catalog"."default",
  "assignee_" varchar(255) COLLATE "pg_catalog"."default",
  "delegation_" varchar(64) COLLATE "pg_catalog"."default",
  "priority_" int4,
  "create_time_" timestamp(6),
  "in_progress_time_" timestamp(6),
  "in_progress_started_by_" varchar(255) COLLATE "pg_catalog"."default",
  "claim_time_" timestamp(6),
  "claimed_by_" varchar(255) COLLATE "pg_catalog"."default",
  "suspended_time_" timestamp(6),
  "suspended_by_" varchar(255) COLLATE "pg_catalog"."default",
  "in_progress_due_date_" timestamp(6),
  "due_date_" timestamp(6),
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "suspension_state_" int4,
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "form_key_" varchar(255) COLLATE "pg_catalog"."default",
  "is_count_enabled_" bool,
  "var_count_" int4,
  "id_link_count_" int4,
  "sub_task_count_" int4
)
;
ALTER TABLE "public"."act_ru_task" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_timer_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_timer_job";
CREATE TABLE "public"."act_ru_timer_job" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "lock_exp_time_" timestamp(6),
  "lock_owner_" varchar(255) COLLATE "pg_catalog"."default",
  "exclusive_" bool,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "process_instance_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_def_id_" varchar(64) COLLATE "pg_catalog"."default",
  "element_id_" varchar(255) COLLATE "pg_catalog"."default",
  "element_name_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_definition_id_" varchar(255) COLLATE "pg_catalog"."default",
  "correlation_id_" varchar(255) COLLATE "pg_catalog"."default",
  "retries_" int4,
  "exception_stack_id_" varchar(64) COLLATE "pg_catalog"."default",
  "exception_msg_" varchar(4000) COLLATE "pg_catalog"."default",
  "duedate_" timestamp(6),
  "repeat_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_type_" varchar(255) COLLATE "pg_catalog"."default",
  "handler_cfg_" varchar(4000) COLLATE "pg_catalog"."default",
  "custom_values_id_" varchar(64) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."act_ru_timer_job" OWNER TO "system";

-- ----------------------------
-- Table structure for act_ru_variable
-- ----------------------------
DROP TABLE IF EXISTS "public"."act_ru_variable";
CREATE TABLE "public"."act_ru_variable" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "type_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "execution_id_" varchar(64) COLLATE "pg_catalog"."default",
  "proc_inst_id_" varchar(64) COLLATE "pg_catalog"."default",
  "task_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(255) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(255) COLLATE "pg_catalog"."default",
  "bytearray_id_" varchar(64) COLLATE "pg_catalog"."default",
  "double_" float8,
  "long_" int8,
  "text_" varchar(4000) COLLATE "pg_catalog"."default",
  "text2_" varchar(4000) COLLATE "pg_catalog"."default",
  "meta_info_" varchar(4000) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."act_ru_variable" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_channel_definition
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_channel_definition";
CREATE TABLE "public"."flw_channel_definition" (
  "id_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "version_" int4,
  "key_" varchar(255) COLLATE "pg_catalog"."default",
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "deployment_id_" varchar(255) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(3),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default",
  "resource_name_" varchar(255) COLLATE "pg_catalog"."default",
  "description_" varchar(255) COLLATE "pg_catalog"."default",
  "type_" varchar(255) COLLATE "pg_catalog"."default",
  "implementation_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."flw_channel_definition" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_ev_databasechangelog
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_ev_databasechangelog";
CREATE TABLE "public"."flw_ev_databasechangelog" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "author" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "filename" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "dateexecuted" timestamp(6) NOT NULL,
  "orderexecuted" int4 NOT NULL,
  "exectype" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "md5sum" varchar(35) COLLATE "pg_catalog"."default",
  "description" varchar(255) COLLATE "pg_catalog"."default",
  "comments" varchar(255) COLLATE "pg_catalog"."default",
  "tag" varchar(255) COLLATE "pg_catalog"."default",
  "liquibase" varchar(20) COLLATE "pg_catalog"."default",
  "contexts" varchar(255) COLLATE "pg_catalog"."default",
  "labels" varchar(255) COLLATE "pg_catalog"."default",
  "deployment_id" varchar(10) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."flw_ev_databasechangelog" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_ev_databasechangeloglock
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_ev_databasechangeloglock";
CREATE TABLE "public"."flw_ev_databasechangeloglock" (
  "id" int4 NOT NULL,
  "locked" bool NOT NULL,
  "lockgranted" timestamp(6),
  "lockedby" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."flw_ev_databasechangeloglock" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_event_definition
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_event_definition";
CREATE TABLE "public"."flw_event_definition" (
  "id_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "version_" int4,
  "key_" varchar(255) COLLATE "pg_catalog"."default",
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "deployment_id_" varchar(255) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default",
  "resource_name_" varchar(255) COLLATE "pg_catalog"."default",
  "description_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."flw_event_definition" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_event_deployment
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_event_deployment";
CREATE TABLE "public"."flw_event_deployment" (
  "id_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "category_" varchar(255) COLLATE "pg_catalog"."default",
  "deploy_time_" timestamp(3),
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default",
  "parent_deployment_id_" varchar(255) COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."flw_event_deployment" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_event_resource
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_event_resource";
CREATE TABLE "public"."flw_event_resource" (
  "id_" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name_" varchar(255) COLLATE "pg_catalog"."default",
  "deployment_id_" varchar(255) COLLATE "pg_catalog"."default",
  "resource_bytes_" bytea
)
;
ALTER TABLE "public"."flw_event_resource" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_ru_batch
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_ru_batch";
CREATE TABLE "public"."flw_ru_batch" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "type_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "search_key_" varchar(255) COLLATE "pg_catalog"."default",
  "search_key2_" varchar(255) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6) NOT NULL,
  "complete_time_" timestamp(6),
  "status_" varchar(255) COLLATE "pg_catalog"."default",
  "batch_doc_id_" varchar(64) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."flw_ru_batch" OWNER TO "system";

-- ----------------------------
-- Table structure for flw_ru_batch_part
-- ----------------------------
DROP TABLE IF EXISTS "public"."flw_ru_batch_part";
CREATE TABLE "public"."flw_ru_batch_part" (
  "id_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "rev_" int4,
  "batch_id_" varchar(64) COLLATE "pg_catalog"."default",
  "type_" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "scope_id_" varchar(64) COLLATE "pg_catalog"."default",
  "sub_scope_id_" varchar(64) COLLATE "pg_catalog"."default",
  "scope_type_" varchar(64) COLLATE "pg_catalog"."default",
  "search_key_" varchar(255) COLLATE "pg_catalog"."default",
  "search_key2_" varchar(255) COLLATE "pg_catalog"."default",
  "create_time_" timestamp(6) NOT NULL,
  "complete_time_" timestamp(6),
  "status_" varchar(255) COLLATE "pg_catalog"."default",
  "result_doc_id_" varchar(64) COLLATE "pg_catalog"."default",
  "tenant_id_" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar
)
;
ALTER TABLE "public"."flw_ru_batch_part" OWNER TO "system";

-- ----------------------------
-- Table structure for infra_api_access_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_api_access_log";
CREATE TABLE "public"."infra_api_access_log" (
  "id" int8 NOT NULL,
  "trace_id" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "user_id" int8 NOT NULL DEFAULT 0,
  "user_type" int2 NOT NULL DEFAULT 0,
  "application_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "request_method" varchar(16) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "request_url" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "request_params" text COLLATE "pg_catalog"."default",
  "response_body" text COLLATE "pg_catalog"."default",
  "user_ip" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "user_agent" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "operate_module" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "operate_name" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "operate_type" int2 DEFAULT 0,
  "begin_time" timestamp(6) NOT NULL,
  "end_time" timestamp(6) NOT NULL,
  "duration" int4 NOT NULL,
  "result_code" int4 NOT NULL DEFAULT 0,
  "result_msg" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_api_access_log" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_api_access_log"."id" IS '日志主键';
COMMENT ON COLUMN "public"."infra_api_access_log"."trace_id" IS '链路追踪编号';
COMMENT ON COLUMN "public"."infra_api_access_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."infra_api_access_log"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."infra_api_access_log"."application_name" IS '应用名';
COMMENT ON COLUMN "public"."infra_api_access_log"."request_method" IS '请求方法名';
COMMENT ON COLUMN "public"."infra_api_access_log"."request_url" IS '请求地址';
COMMENT ON COLUMN "public"."infra_api_access_log"."request_params" IS '请求参数';
COMMENT ON COLUMN "public"."infra_api_access_log"."response_body" IS '响应结果';
COMMENT ON COLUMN "public"."infra_api_access_log"."user_ip" IS '用户 IP';
COMMENT ON COLUMN "public"."infra_api_access_log"."user_agent" IS '浏览器 UA';
COMMENT ON COLUMN "public"."infra_api_access_log"."operate_module" IS '操作模块';
COMMENT ON COLUMN "public"."infra_api_access_log"."operate_name" IS '操作名';
COMMENT ON COLUMN "public"."infra_api_access_log"."operate_type" IS '操作分类';
COMMENT ON COLUMN "public"."infra_api_access_log"."begin_time" IS '开始请求时间';
COMMENT ON COLUMN "public"."infra_api_access_log"."end_time" IS '结束请求时间';
COMMENT ON COLUMN "public"."infra_api_access_log"."duration" IS '执行时长';
COMMENT ON COLUMN "public"."infra_api_access_log"."result_code" IS '结果码';
COMMENT ON COLUMN "public"."infra_api_access_log"."result_msg" IS '结果提示';
COMMENT ON COLUMN "public"."infra_api_access_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_api_access_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_api_access_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_api_access_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_api_access_log"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."infra_api_access_log"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."infra_api_access_log" IS 'API 访问日志表';

-- ----------------------------
-- Table structure for infra_api_error_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_api_error_log";
CREATE TABLE "public"."infra_api_error_log" (
  "id" int8 NOT NULL,
  "trace_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8 NOT NULL DEFAULT 0,
  "user_type" int2 NOT NULL DEFAULT 0,
  "application_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "request_method" varchar(16) COLLATE "pg_catalog"."default" NOT NULL,
  "request_url" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "request_params" varchar(8000) COLLATE "pg_catalog"."default" NOT NULL,
  "user_ip" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "user_agent" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "exception_time" timestamp(6) NOT NULL,
  "exception_name" varchar(128) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "exception_message" text COLLATE "pg_catalog"."default",
  "exception_root_cause_message" text COLLATE "pg_catalog"."default",
  "exception_stack_trace" text COLLATE "pg_catalog"."default",
  "exception_class_name" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "exception_file_name" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "exception_method_name" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "exception_line_number" int4 NOT NULL,
  "process_status" int2 NOT NULL,
  "process_time" timestamp(6),
  "process_user_id" int4 DEFAULT 0,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_api_error_log" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_api_error_log"."id" IS '编号';
COMMENT ON COLUMN "public"."infra_api_error_log"."trace_id" IS '链路追踪编号';
COMMENT ON COLUMN "public"."infra_api_error_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."infra_api_error_log"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."infra_api_error_log"."application_name" IS '应用名';
COMMENT ON COLUMN "public"."infra_api_error_log"."request_method" IS '请求方法名';
COMMENT ON COLUMN "public"."infra_api_error_log"."request_url" IS '请求地址';
COMMENT ON COLUMN "public"."infra_api_error_log"."request_params" IS '请求参数';
COMMENT ON COLUMN "public"."infra_api_error_log"."user_ip" IS '用户 IP';
COMMENT ON COLUMN "public"."infra_api_error_log"."user_agent" IS '浏览器 UA';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_time" IS '异常发生时间';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_name" IS '异常名';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_message" IS '异常导致的消息';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_root_cause_message" IS '异常导致的根消息';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_stack_trace" IS '异常的栈轨迹';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_class_name" IS '异常发生的类全名';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_file_name" IS '异常发生的类文件';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_method_name" IS '异常发生的方法名';
COMMENT ON COLUMN "public"."infra_api_error_log"."exception_line_number" IS '异常发生的方法所在行';
COMMENT ON COLUMN "public"."infra_api_error_log"."process_status" IS '处理状态';
COMMENT ON COLUMN "public"."infra_api_error_log"."process_time" IS '处理时间';
COMMENT ON COLUMN "public"."infra_api_error_log"."process_user_id" IS '处理用户编号';
COMMENT ON COLUMN "public"."infra_api_error_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_api_error_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_api_error_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_api_error_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_api_error_log"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."infra_api_error_log"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."infra_api_error_log" IS '系统异常日志';

-- ----------------------------
-- Table structure for infra_codegen_column
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_codegen_column";
CREATE TABLE "public"."infra_codegen_column" (
  "id" int8 NOT NULL,
  "table_id" int8 NOT NULL,
  "column_name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "data_type" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "column_comment" varchar(500) COLLATE "pg_catalog"."default" NOT NULL,
  "nullable" bool NOT NULL,
  "primary_key" bool NOT NULL,
  "ordinal_position" int4 NOT NULL,
  "java_type" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "java_field" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "dict_type" varchar(200) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "example" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_operation" bool NOT NULL,
  "update_operation" bool NOT NULL,
  "list_operation" bool NOT NULL,
  "list_operation_condition" varchar(32) COLLATE "pg_catalog"."default" NOT NULL DEFAULT '='::varchar,
  "list_operation_result" bool NOT NULL,
  "html_type" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_codegen_column" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_codegen_column"."id" IS '编号';
COMMENT ON COLUMN "public"."infra_codegen_column"."table_id" IS '表编号';
COMMENT ON COLUMN "public"."infra_codegen_column"."column_name" IS '字段名';
COMMENT ON COLUMN "public"."infra_codegen_column"."data_type" IS '字段类型';
COMMENT ON COLUMN "public"."infra_codegen_column"."column_comment" IS '字段描述';
COMMENT ON COLUMN "public"."infra_codegen_column"."nullable" IS '是否允许为空';
COMMENT ON COLUMN "public"."infra_codegen_column"."primary_key" IS '是否主键';
COMMENT ON COLUMN "public"."infra_codegen_column"."ordinal_position" IS '排序';
COMMENT ON COLUMN "public"."infra_codegen_column"."java_type" IS 'Java 属性类型';
COMMENT ON COLUMN "public"."infra_codegen_column"."java_field" IS 'Java 属性名';
COMMENT ON COLUMN "public"."infra_codegen_column"."dict_type" IS '字典类型';
COMMENT ON COLUMN "public"."infra_codegen_column"."example" IS '数据示例';
COMMENT ON COLUMN "public"."infra_codegen_column"."create_operation" IS '是否为 Create 创建操作的字段';
COMMENT ON COLUMN "public"."infra_codegen_column"."update_operation" IS '是否为 Update 更新操作的字段';
COMMENT ON COLUMN "public"."infra_codegen_column"."list_operation" IS '是否为 List 查询操作的字段';
COMMENT ON COLUMN "public"."infra_codegen_column"."list_operation_condition" IS 'List 查询操作的条件类型';
COMMENT ON COLUMN "public"."infra_codegen_column"."list_operation_result" IS '是否为 List 查询操作的返回字段';
COMMENT ON COLUMN "public"."infra_codegen_column"."html_type" IS '显示类型';
COMMENT ON COLUMN "public"."infra_codegen_column"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_codegen_column"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_codegen_column"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_codegen_column"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_codegen_column"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_codegen_column" IS '代码生成表字段定义';

-- ----------------------------
-- Table structure for infra_codegen_table
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_codegen_table";
CREATE TABLE "public"."infra_codegen_table" (
  "id" int8 NOT NULL,
  "data_source_config_id" int8 NOT NULL,
  "scene" int2 NOT NULL DEFAULT 1,
  "table_name" varchar(200) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "table_comment" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "module_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "business_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "class_name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "class_comment" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "author" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "template_type" int2 NOT NULL DEFAULT 1,
  "front_type" int2 NOT NULL,
  "parent_menu_id" int8,
  "master_table_id" int8,
  "sub_join_column_id" int8,
  "sub_join_many" bool,
  "tree_parent_column_id" int8,
  "tree_name_column_id" int8,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_codegen_table" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_codegen_table"."id" IS '编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."data_source_config_id" IS '数据源配置的编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."scene" IS '生成场景';
COMMENT ON COLUMN "public"."infra_codegen_table"."table_name" IS '表名称';
COMMENT ON COLUMN "public"."infra_codegen_table"."table_comment" IS '表描述';
COMMENT ON COLUMN "public"."infra_codegen_table"."remark" IS '备注';
COMMENT ON COLUMN "public"."infra_codegen_table"."module_name" IS '模块名';
COMMENT ON COLUMN "public"."infra_codegen_table"."business_name" IS '业务名';
COMMENT ON COLUMN "public"."infra_codegen_table"."class_name" IS '类名称';
COMMENT ON COLUMN "public"."infra_codegen_table"."class_comment" IS '类描述';
COMMENT ON COLUMN "public"."infra_codegen_table"."author" IS '作者';
COMMENT ON COLUMN "public"."infra_codegen_table"."template_type" IS '模板类型';
COMMENT ON COLUMN "public"."infra_codegen_table"."front_type" IS '前端类型';
COMMENT ON COLUMN "public"."infra_codegen_table"."parent_menu_id" IS '父菜单编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."master_table_id" IS '主表的编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."sub_join_column_id" IS '子表关联主表的字段编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."sub_join_many" IS '主表与子表是否一对多';
COMMENT ON COLUMN "public"."infra_codegen_table"."tree_parent_column_id" IS '树表的父字段编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."tree_name_column_id" IS '树表的名字字段编号';
COMMENT ON COLUMN "public"."infra_codegen_table"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_codegen_table"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_codegen_table"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_codegen_table"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_codegen_table"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_codegen_table" IS '代码生成表定义';

-- ----------------------------
-- Table structure for infra_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_config";
CREATE TABLE "public"."infra_config" (
  "id" int8 NOT NULL,
  "category" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "type" int2 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "config_key" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "value" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "visible" bool NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_config" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_config"."id" IS '参数主键';
COMMENT ON COLUMN "public"."infra_config"."category" IS '参数分组';
COMMENT ON COLUMN "public"."infra_config"."type" IS '参数类型';
COMMENT ON COLUMN "public"."infra_config"."name" IS '参数名称';
COMMENT ON COLUMN "public"."infra_config"."config_key" IS '参数键名';
COMMENT ON COLUMN "public"."infra_config"."value" IS '参数键值';
COMMENT ON COLUMN "public"."infra_config"."visible" IS '是否可见';
COMMENT ON COLUMN "public"."infra_config"."remark" IS '备注';
COMMENT ON COLUMN "public"."infra_config"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_config"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_config"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_config"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_config"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_config" IS '参数配置表';

-- ----------------------------
-- Table structure for infra_data_source_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_data_source_config";
CREATE TABLE "public"."infra_data_source_config" (
  "id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "url" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
  "username" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_data_source_config" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_data_source_config"."id" IS '主键编号';
COMMENT ON COLUMN "public"."infra_data_source_config"."name" IS '参数名称';
COMMENT ON COLUMN "public"."infra_data_source_config"."url" IS '数据源连接';
COMMENT ON COLUMN "public"."infra_data_source_config"."username" IS '用户名';
COMMENT ON COLUMN "public"."infra_data_source_config"."password" IS '密码';
COMMENT ON COLUMN "public"."infra_data_source_config"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_data_source_config"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_data_source_config"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_data_source_config"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_data_source_config"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_data_source_config" IS '数据源配置表';

-- ----------------------------
-- Table structure for infra_file
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_file";
CREATE TABLE "public"."infra_file" (
  "id" int8 NOT NULL,
  "config_id" int8,
  "name" varchar(256) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "path" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "url" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(128) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "size" int4 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_file" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_file"."id" IS '文件编号';
COMMENT ON COLUMN "public"."infra_file"."config_id" IS '配置编号';
COMMENT ON COLUMN "public"."infra_file"."name" IS '文件名';
COMMENT ON COLUMN "public"."infra_file"."path" IS '文件路径';
COMMENT ON COLUMN "public"."infra_file"."url" IS '文件 URL';
COMMENT ON COLUMN "public"."infra_file"."type" IS '文件类型';
COMMENT ON COLUMN "public"."infra_file"."size" IS '文件大小';
COMMENT ON COLUMN "public"."infra_file"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_file"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_file"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_file"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_file"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_file" IS '文件表';

-- ----------------------------
-- Table structure for infra_file_config
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_file_config";
CREATE TABLE "public"."infra_file_config" (
  "id" int8 NOT NULL,
  "name" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "storage" int2 NOT NULL,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "master" bool NOT NULL,
  "config" varchar(4096) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_file_config" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_file_config"."id" IS '编号';
COMMENT ON COLUMN "public"."infra_file_config"."name" IS '配置名';
COMMENT ON COLUMN "public"."infra_file_config"."storage" IS '存储器';
COMMENT ON COLUMN "public"."infra_file_config"."remark" IS '备注';
COMMENT ON COLUMN "public"."infra_file_config"."master" IS '是否为主配置';
COMMENT ON COLUMN "public"."infra_file_config"."config" IS '存储配置';
COMMENT ON COLUMN "public"."infra_file_config"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_file_config"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_file_config"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_file_config"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_file_config"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_file_config" IS '文件配置表';

-- ----------------------------
-- Table structure for infra_file_content
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_file_content";
CREATE TABLE "public"."infra_file_content" (
  "id" int8 NOT NULL,
  "config_id" int8 NOT NULL,
  "path" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "content" bytea NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_file_content" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_file_content"."id" IS '编号';
COMMENT ON COLUMN "public"."infra_file_content"."config_id" IS '配置编号';
COMMENT ON COLUMN "public"."infra_file_content"."path" IS '文件路径';
COMMENT ON COLUMN "public"."infra_file_content"."content" IS '文件内容';
COMMENT ON COLUMN "public"."infra_file_content"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_file_content"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_file_content"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_file_content"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_file_content"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_file_content" IS '文件表';

-- ----------------------------
-- Table structure for infra_job
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_job";
CREATE TABLE "public"."infra_job" (
  "id" int8 NOT NULL,
  "name" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL,
  "handler_name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "handler_param" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "cron_expression" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "retry_count" int4 NOT NULL DEFAULT 0,
  "retry_interval" int4 NOT NULL DEFAULT 0,
  "monitor_timeout" int4 NOT NULL DEFAULT 0,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_job" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_job"."id" IS '任务编号';
COMMENT ON COLUMN "public"."infra_job"."name" IS '任务名称';
COMMENT ON COLUMN "public"."infra_job"."status" IS '任务状态';
COMMENT ON COLUMN "public"."infra_job"."handler_name" IS '处理器的名字';
COMMENT ON COLUMN "public"."infra_job"."handler_param" IS '处理器的参数';
COMMENT ON COLUMN "public"."infra_job"."cron_expression" IS 'CRON 表达式';
COMMENT ON COLUMN "public"."infra_job"."retry_count" IS '重试次数';
COMMENT ON COLUMN "public"."infra_job"."retry_interval" IS '重试间隔';
COMMENT ON COLUMN "public"."infra_job"."monitor_timeout" IS '监控超时时间';
COMMENT ON COLUMN "public"."infra_job"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_job"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_job"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_job"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_job"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_job" IS '定时任务表';

-- ----------------------------
-- Table structure for infra_job_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."infra_job_log";
CREATE TABLE "public"."infra_job_log" (
  "id" int8 NOT NULL,
  "job_id" int8 NOT NULL,
  "handler_name" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "handler_param" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "execute_index" int2 NOT NULL DEFAULT 1,
  "begin_time" timestamp(6) NOT NULL,
  "end_time" timestamp(6),
  "duration" int4,
  "status" int2 NOT NULL,
  "result" varchar(4000) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."infra_job_log" OWNER TO "system";
COMMENT ON COLUMN "public"."infra_job_log"."id" IS '日志编号';
COMMENT ON COLUMN "public"."infra_job_log"."job_id" IS '任务编号';
COMMENT ON COLUMN "public"."infra_job_log"."handler_name" IS '处理器的名字';
COMMENT ON COLUMN "public"."infra_job_log"."handler_param" IS '处理器的参数';
COMMENT ON COLUMN "public"."infra_job_log"."execute_index" IS '第几次执行';
COMMENT ON COLUMN "public"."infra_job_log"."begin_time" IS '开始执行时间';
COMMENT ON COLUMN "public"."infra_job_log"."end_time" IS '结束执行时间';
COMMENT ON COLUMN "public"."infra_job_log"."duration" IS '执行时长';
COMMENT ON COLUMN "public"."infra_job_log"."status" IS '任务状态';
COMMENT ON COLUMN "public"."infra_job_log"."result" IS '结果数据';
COMMENT ON COLUMN "public"."infra_job_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."infra_job_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."infra_job_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."infra_job_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."infra_job_log"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."infra_job_log" IS '定时任务日志表';

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_blob_triggers";
CREATE TABLE "public"."qrtz_blob_triggers" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "blob_data" bytea
)
;
ALTER TABLE "public"."qrtz_blob_triggers" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_calendars";
CREATE TABLE "public"."qrtz_calendars" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "calendar_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "calendar" bytea NOT NULL
)
;
ALTER TABLE "public"."qrtz_calendars" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_cron_triggers";
CREATE TABLE "public"."qrtz_cron_triggers" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "cron_expression" text COLLATE "pg_catalog"."default" NOT NULL,
  "time_zone_id" text COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."qrtz_cron_triggers" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_fired_triggers";
CREATE TABLE "public"."qrtz_fired_triggers" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "entry_id" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "instance_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "fired_time" int8 NOT NULL,
  "sched_time" int8 NOT NULL,
  "priority" int4 NOT NULL,
  "state" text COLLATE "pg_catalog"."default" NOT NULL,
  "job_name" text COLLATE "pg_catalog"."default",
  "job_group" text COLLATE "pg_catalog"."default",
  "is_nonconcurrent" bool NOT NULL,
  "requests_recovery" bool
)
;
ALTER TABLE "public"."qrtz_fired_triggers" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_job_details";
CREATE TABLE "public"."qrtz_job_details" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "job_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "job_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "job_class_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "is_durable" bool NOT NULL,
  "is_nonconcurrent" bool NOT NULL,
  "is_update_data" bool NOT NULL,
  "requests_recovery" bool NOT NULL,
  "job_data" bytea
)
;
ALTER TABLE "public"."qrtz_job_details" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_locks";
CREATE TABLE "public"."qrtz_locks" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "lock_name" text COLLATE "pg_catalog"."default" NOT NULL
)
;
ALTER TABLE "public"."qrtz_locks" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_paused_trigger_grps";
CREATE TABLE "public"."qrtz_paused_trigger_grps" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL
)
;
ALTER TABLE "public"."qrtz_paused_trigger_grps" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_scheduler_state";
CREATE TABLE "public"."qrtz_scheduler_state" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "instance_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "last_checkin_time" int8 NOT NULL,
  "checkin_interval" int8 NOT NULL
)
;
ALTER TABLE "public"."qrtz_scheduler_state" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_simple_triggers";
CREATE TABLE "public"."qrtz_simple_triggers" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "repeat_count" int8 NOT NULL,
  "repeat_interval" int8 NOT NULL,
  "times_triggered" int8 NOT NULL
)
;
ALTER TABLE "public"."qrtz_simple_triggers" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_simprop_triggers";
CREATE TABLE "public"."qrtz_simprop_triggers" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "str_prop_1" text COLLATE "pg_catalog"."default",
  "str_prop_2" text COLLATE "pg_catalog"."default",
  "str_prop_3" text COLLATE "pg_catalog"."default",
  "int_prop_1" int4,
  "int_prop_2" int4,
  "long_prop_1" int8,
  "long_prop_2" int8,
  "dec_prop_1" numeric,
  "dec_prop_2" numeric,
  "bool_prop_1" bool,
  "bool_prop_2" bool,
  "time_zone_id" text COLLATE "pg_catalog"."default"
)
;
ALTER TABLE "public"."qrtz_simprop_triggers" OWNER TO "system";

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS "public"."qrtz_triggers";
CREATE TABLE "public"."qrtz_triggers" (
  "sched_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "job_name" text COLLATE "pg_catalog"."default" NOT NULL,
  "job_group" text COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "next_fire_time" int8,
  "prev_fire_time" int8,
  "priority" int4,
  "trigger_state" text COLLATE "pg_catalog"."default" NOT NULL,
  "trigger_type" text COLLATE "pg_catalog"."default" NOT NULL,
  "start_time" int8 NOT NULL,
  "end_time" int8,
  "calendar_name" text COLLATE "pg_catalog"."default",
  "misfire_instr" int2,
  "job_data" bytea
)
;
ALTER TABLE "public"."qrtz_triggers" OWNER TO "system";

-- ----------------------------
-- Table structure for system_dept
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_dept";
CREATE TABLE "public"."system_dept" (
  "id" int8 NOT NULL,
  "name" varchar(30) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "parent_id" int8 NOT NULL DEFAULT 0,
  "sort" int4 NOT NULL DEFAULT 0,
  "leader_user_id" int8,
  "phone" varchar(11) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "email" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_dept" OWNER TO "system";
COMMENT ON COLUMN "public"."system_dept"."id" IS '部门id';
COMMENT ON COLUMN "public"."system_dept"."name" IS '部门名称';
COMMENT ON COLUMN "public"."system_dept"."parent_id" IS '父部门id';
COMMENT ON COLUMN "public"."system_dept"."sort" IS '显示顺序';
COMMENT ON COLUMN "public"."system_dept"."leader_user_id" IS '负责人';
COMMENT ON COLUMN "public"."system_dept"."phone" IS '联系电话';
COMMENT ON COLUMN "public"."system_dept"."email" IS '邮箱';
COMMENT ON COLUMN "public"."system_dept"."status" IS '部门状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_dept"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_dept"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_dept"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_dept"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_dept"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_dept"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_dept" IS '部门表';

-- ----------------------------
-- Table structure for system_dict_data
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_dict_data";
CREATE TABLE "public"."system_dict_data" (
  "id" int8 NOT NULL,
  "sort" int4 NOT NULL DEFAULT 0,
  "label" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "value" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "dict_type" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL DEFAULT 0,
  "color_type" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "css_class" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_dict_data" OWNER TO "system";
COMMENT ON COLUMN "public"."system_dict_data"."id" IS '字典编码';
COMMENT ON COLUMN "public"."system_dict_data"."sort" IS '字典排序';
COMMENT ON COLUMN "public"."system_dict_data"."label" IS '字典标签';
COMMENT ON COLUMN "public"."system_dict_data"."value" IS '字典键值';
COMMENT ON COLUMN "public"."system_dict_data"."dict_type" IS '字典类型';
COMMENT ON COLUMN "public"."system_dict_data"."status" IS '状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_dict_data"."color_type" IS '颜色类型';
COMMENT ON COLUMN "public"."system_dict_data"."css_class" IS 'css 样式';
COMMENT ON COLUMN "public"."system_dict_data"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_dict_data"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_dict_data"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_dict_data"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_dict_data"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_dict_data"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_dict_data" IS '字典数据表';

-- ----------------------------
-- Table structure for system_dict_type
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_dict_type";
CREATE TABLE "public"."system_dict_type" (
  "id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "type" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL DEFAULT 0,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "deleted_time" timestamp(6)
)
;
ALTER TABLE "public"."system_dict_type" OWNER TO "system";
COMMENT ON COLUMN "public"."system_dict_type"."id" IS '字典主键';
COMMENT ON COLUMN "public"."system_dict_type"."name" IS '字典名称';
COMMENT ON COLUMN "public"."system_dict_type"."type" IS '字典类型';
COMMENT ON COLUMN "public"."system_dict_type"."status" IS '状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_dict_type"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_dict_type"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_dict_type"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_dict_type"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_dict_type"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_dict_type"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_dict_type"."deleted_time" IS '删除时间';
COMMENT ON TABLE "public"."system_dict_type" IS '字典类型表';

-- ----------------------------
-- Table structure for system_login_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_login_log";
CREATE TABLE "public"."system_login_log" (
  "id" int8 NOT NULL,
  "log_type" int8 NOT NULL,
  "trace_id" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "user_id" int8 NOT NULL DEFAULT 0,
  "user_type" int2 NOT NULL DEFAULT 0,
  "username" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "result" int2 NOT NULL,
  "user_ip" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "user_agent" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_login_log" OWNER TO "system";
COMMENT ON COLUMN "public"."system_login_log"."id" IS '访问ID';
COMMENT ON COLUMN "public"."system_login_log"."log_type" IS '日志类型';
COMMENT ON COLUMN "public"."system_login_log"."trace_id" IS '链路追踪编号';
COMMENT ON COLUMN "public"."system_login_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_login_log"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_login_log"."username" IS '用户账号';
COMMENT ON COLUMN "public"."system_login_log"."result" IS '登陆结果';
COMMENT ON COLUMN "public"."system_login_log"."user_ip" IS '用户 IP';
COMMENT ON COLUMN "public"."system_login_log"."user_agent" IS '浏览器 UA';
COMMENT ON COLUMN "public"."system_login_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_login_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_login_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_login_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_login_log"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_login_log"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_login_log" IS '系统访问记录';

-- ----------------------------
-- Table structure for system_mail_account
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_mail_account";
CREATE TABLE "public"."system_mail_account" (
  "id" int8 NOT NULL,
  "mail" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "username" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "host" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "port" int4 NOT NULL,
  "ssl_enable" bool NOT NULL DEFAULT false,
  "starttls_enable" bool NOT NULL DEFAULT false,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_mail_account" OWNER TO "system";
COMMENT ON COLUMN "public"."system_mail_account"."id" IS '主键';
COMMENT ON COLUMN "public"."system_mail_account"."mail" IS '邮箱';
COMMENT ON COLUMN "public"."system_mail_account"."username" IS '用户名';
COMMENT ON COLUMN "public"."system_mail_account"."password" IS '密码';
COMMENT ON COLUMN "public"."system_mail_account"."host" IS 'SMTP 服务器域名';
COMMENT ON COLUMN "public"."system_mail_account"."port" IS 'SMTP 服务器端口';
COMMENT ON COLUMN "public"."system_mail_account"."ssl_enable" IS '是否开启 SSL';
COMMENT ON COLUMN "public"."system_mail_account"."starttls_enable" IS '是否开启 STARTTLS';
COMMENT ON COLUMN "public"."system_mail_account"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_mail_account"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_mail_account"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_mail_account"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_mail_account"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_mail_account" IS '邮箱账号表';

-- ----------------------------
-- Table structure for system_mail_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_mail_log";
CREATE TABLE "public"."system_mail_log" (
  "id" int8 NOT NULL,
  "user_id" int8,
  "user_type" int2,
  "to_mail" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "account_id" int8 NOT NULL,
  "from_mail" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "template_id" int8 NOT NULL,
  "template_code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "template_nickname" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "template_title" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "template_content" varchar(10240) COLLATE "pg_catalog"."default" NOT NULL,
  "template_params" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "send_status" int2 NOT NULL DEFAULT 0,
  "send_time" timestamp(6),
  "send_message_id" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "send_exception" varchar(4096) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_mail_log" OWNER TO "system";
COMMENT ON COLUMN "public"."system_mail_log"."id" IS '编号';
COMMENT ON COLUMN "public"."system_mail_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_mail_log"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_mail_log"."to_mail" IS '接收邮箱地址';
COMMENT ON COLUMN "public"."system_mail_log"."account_id" IS '邮箱账号编号';
COMMENT ON COLUMN "public"."system_mail_log"."from_mail" IS '发送邮箱地址';
COMMENT ON COLUMN "public"."system_mail_log"."template_id" IS '模板编号';
COMMENT ON COLUMN "public"."system_mail_log"."template_code" IS '模板编码';
COMMENT ON COLUMN "public"."system_mail_log"."template_nickname" IS '模版发送人名称';
COMMENT ON COLUMN "public"."system_mail_log"."template_title" IS '邮件标题';
COMMENT ON COLUMN "public"."system_mail_log"."template_content" IS '邮件内容';
COMMENT ON COLUMN "public"."system_mail_log"."template_params" IS '邮件参数';
COMMENT ON COLUMN "public"."system_mail_log"."send_status" IS '发送状态';
COMMENT ON COLUMN "public"."system_mail_log"."send_time" IS '发送时间';
COMMENT ON COLUMN "public"."system_mail_log"."send_message_id" IS '发送返回的消息 ID';
COMMENT ON COLUMN "public"."system_mail_log"."send_exception" IS '发送异常';
COMMENT ON COLUMN "public"."system_mail_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_mail_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_mail_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_mail_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_mail_log"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_mail_log" IS '邮件日志表';

-- ----------------------------
-- Table structure for system_mail_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_mail_template";
CREATE TABLE "public"."system_mail_template" (
  "id" int8 NOT NULL,
  "name" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "account_id" int8 NOT NULL,
  "nickname" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "title" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "content" varchar(10240) COLLATE "pg_catalog"."default" NOT NULL,
  "params" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_mail_template" OWNER TO "system";
COMMENT ON COLUMN "public"."system_mail_template"."id" IS '编号';
COMMENT ON COLUMN "public"."system_mail_template"."name" IS '模板名称';
COMMENT ON COLUMN "public"."system_mail_template"."code" IS '模板编码';
COMMENT ON COLUMN "public"."system_mail_template"."account_id" IS '发送的邮箱账号编号';
COMMENT ON COLUMN "public"."system_mail_template"."nickname" IS '发送人名称';
COMMENT ON COLUMN "public"."system_mail_template"."title" IS '模板标题';
COMMENT ON COLUMN "public"."system_mail_template"."content" IS '模板内容';
COMMENT ON COLUMN "public"."system_mail_template"."params" IS '参数数组';
COMMENT ON COLUMN "public"."system_mail_template"."status" IS '开启状态';
COMMENT ON COLUMN "public"."system_mail_template"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_mail_template"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_mail_template"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_mail_template"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_mail_template"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_mail_template"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_mail_template" IS '邮件模版表';

-- ----------------------------
-- Table structure for system_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_menu";
CREATE TABLE "public"."system_menu" (
  "id" int8 NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "permission" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "type" int2 NOT NULL,
  "sort" int4 NOT NULL DEFAULT 0,
  "parent_id" int8 NOT NULL DEFAULT 0,
  "path" varchar(200) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "icon" varchar(100) COLLATE "pg_catalog"."default" DEFAULT '#'::varchar,
  "component" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "component_name" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL DEFAULT 0,
  "visible" bool NOT NULL DEFAULT true,
  "keep_alive" bool NOT NULL DEFAULT true,
  "always_show" bool NOT NULL DEFAULT true,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_menu" OWNER TO "system";
COMMENT ON COLUMN "public"."system_menu"."id" IS '菜单ID';
COMMENT ON COLUMN "public"."system_menu"."name" IS '菜单名称';
COMMENT ON COLUMN "public"."system_menu"."permission" IS '权限标识';
COMMENT ON COLUMN "public"."system_menu"."type" IS '菜单类型';
COMMENT ON COLUMN "public"."system_menu"."sort" IS '显示顺序';
COMMENT ON COLUMN "public"."system_menu"."parent_id" IS '父菜单ID';
COMMENT ON COLUMN "public"."system_menu"."path" IS '路由地址';
COMMENT ON COLUMN "public"."system_menu"."icon" IS '菜单图标';
COMMENT ON COLUMN "public"."system_menu"."component" IS '组件路径';
COMMENT ON COLUMN "public"."system_menu"."component_name" IS '组件名';
COMMENT ON COLUMN "public"."system_menu"."status" IS '菜单状态';
COMMENT ON COLUMN "public"."system_menu"."visible" IS '是否可见';
COMMENT ON COLUMN "public"."system_menu"."keep_alive" IS '是否缓存';
COMMENT ON COLUMN "public"."system_menu"."always_show" IS '是否总是显示';
COMMENT ON COLUMN "public"."system_menu"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_menu"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_menu"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_menu"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_menu"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_menu" IS '菜单权限表';

-- ----------------------------
-- Table structure for system_notice
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_notice";
CREATE TABLE "public"."system_notice" (
  "id" int8 NOT NULL,
  "title" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "content" text COLLATE "pg_catalog"."default",
  "type" int2 NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_notice" OWNER TO "system";
COMMENT ON COLUMN "public"."system_notice"."id" IS '公告ID';
COMMENT ON COLUMN "public"."system_notice"."title" IS '公告标题';
COMMENT ON COLUMN "public"."system_notice"."content" IS '公告内容';
COMMENT ON COLUMN "public"."system_notice"."type" IS '公告类型（1通知 2公告）';
COMMENT ON COLUMN "public"."system_notice"."status" IS '公告状态（0正常 1关闭）';
COMMENT ON COLUMN "public"."system_notice"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_notice"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_notice"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_notice"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_notice"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_notice"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_notice" IS '通知公告表';

-- ----------------------------
-- Table structure for system_notify_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_notify_message";
CREATE TABLE "public"."system_notify_message" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_type" int2 NOT NULL,
  "template_id" int8 NOT NULL,
  "template_code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "template_nickname" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "template_content" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
  "template_type" int4 NOT NULL,
  "template_params" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "read_status" bool NOT NULL,
  "read_time" timestamp(6),
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_notify_message" OWNER TO "system";
COMMENT ON COLUMN "public"."system_notify_message"."id" IS '用户ID';
COMMENT ON COLUMN "public"."system_notify_message"."user_id" IS '用户id';
COMMENT ON COLUMN "public"."system_notify_message"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_notify_message"."template_id" IS '模版编号';
COMMENT ON COLUMN "public"."system_notify_message"."template_code" IS '模板编码';
COMMENT ON COLUMN "public"."system_notify_message"."template_nickname" IS '模版发送人名称';
COMMENT ON COLUMN "public"."system_notify_message"."template_content" IS '模版内容';
COMMENT ON COLUMN "public"."system_notify_message"."template_type" IS '模版类型';
COMMENT ON COLUMN "public"."system_notify_message"."template_params" IS '模版参数';
COMMENT ON COLUMN "public"."system_notify_message"."read_status" IS '是否已读';
COMMENT ON COLUMN "public"."system_notify_message"."read_time" IS '阅读时间';
COMMENT ON COLUMN "public"."system_notify_message"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_notify_message"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_notify_message"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_notify_message"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_notify_message"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_notify_message"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_notify_message" IS '站内信消息表';

-- ----------------------------
-- Table structure for system_notify_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_notify_template";
CREATE TABLE "public"."system_notify_template" (
  "id" int8 NOT NULL,
  "name" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "content" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
  "type" int2 NOT NULL,
  "params" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_notify_template" OWNER TO "system";
COMMENT ON COLUMN "public"."system_notify_template"."id" IS '主键';
COMMENT ON COLUMN "public"."system_notify_template"."name" IS '模板名称';
COMMENT ON COLUMN "public"."system_notify_template"."code" IS '模版编码';
COMMENT ON COLUMN "public"."system_notify_template"."nickname" IS '发送人名称';
COMMENT ON COLUMN "public"."system_notify_template"."content" IS '模版内容';
COMMENT ON COLUMN "public"."system_notify_template"."type" IS '类型';
COMMENT ON COLUMN "public"."system_notify_template"."params" IS '参数数组';
COMMENT ON COLUMN "public"."system_notify_template"."status" IS '状态';
COMMENT ON COLUMN "public"."system_notify_template"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_notify_template"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_notify_template"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_notify_template"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_notify_template"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_notify_template"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_notify_template" IS '站内信模板表';

-- ----------------------------
-- Table structure for system_oauth2_access_token
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_oauth2_access_token";
CREATE TABLE "public"."system_oauth2_access_token" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_type" int2 NOT NULL,
  "user_info" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "access_token" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "refresh_token" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "scopes" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "expires_time" timestamp(6) NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_oauth2_access_token" OWNER TO "system";
COMMENT ON COLUMN "public"."system_oauth2_access_token"."id" IS '编号';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."user_info" IS '用户信息';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."access_token" IS '访问令牌';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."refresh_token" IS '刷新令牌';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."client_id" IS '客户端编号';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."scopes" IS '授权范围';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."expires_time" IS '过期时间';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_oauth2_access_token"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_oauth2_access_token" IS 'OAuth2 访问令牌';

-- ----------------------------
-- Table structure for system_oauth2_approve
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_oauth2_approve";
CREATE TABLE "public"."system_oauth2_approve" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_type" int2 NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "scope" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "approved" bool NOT NULL DEFAULT false,
  "expires_time" timestamp(6) NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_oauth2_approve" OWNER TO "system";
COMMENT ON COLUMN "public"."system_oauth2_approve"."id" IS '编号';
COMMENT ON COLUMN "public"."system_oauth2_approve"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_oauth2_approve"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_oauth2_approve"."client_id" IS '客户端编号';
COMMENT ON COLUMN "public"."system_oauth2_approve"."scope" IS '授权范围';
COMMENT ON COLUMN "public"."system_oauth2_approve"."approved" IS '是否接受';
COMMENT ON COLUMN "public"."system_oauth2_approve"."expires_time" IS '过期时间';
COMMENT ON COLUMN "public"."system_oauth2_approve"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_oauth2_approve"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_oauth2_approve"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_oauth2_approve"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_oauth2_approve"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_oauth2_approve"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_oauth2_approve" IS 'OAuth2 批准表';

-- ----------------------------
-- Table structure for system_oauth2_client
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_oauth2_client";
CREATE TABLE "public"."system_oauth2_client" (
  "id" int8 NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "secret" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "logo" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "description" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL,
  "access_token_validity_seconds" int4 NOT NULL,
  "refresh_token_validity_seconds" int4 NOT NULL,
  "redirect_uris" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "authorized_grant_types" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "scopes" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "auto_approve_scopes" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "authorities" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "resource_ids" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "additional_information" varchar(4096) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_oauth2_client" OWNER TO "system";
COMMENT ON COLUMN "public"."system_oauth2_client"."id" IS '编号';
COMMENT ON COLUMN "public"."system_oauth2_client"."client_id" IS '客户端编号';
COMMENT ON COLUMN "public"."system_oauth2_client"."secret" IS '客户端密钥';
COMMENT ON COLUMN "public"."system_oauth2_client"."name" IS '应用名';
COMMENT ON COLUMN "public"."system_oauth2_client"."logo" IS '应用图标';
COMMENT ON COLUMN "public"."system_oauth2_client"."description" IS '应用描述';
COMMENT ON COLUMN "public"."system_oauth2_client"."status" IS '状态';
COMMENT ON COLUMN "public"."system_oauth2_client"."access_token_validity_seconds" IS '访问令牌的有效期';
COMMENT ON COLUMN "public"."system_oauth2_client"."refresh_token_validity_seconds" IS '刷新令牌的有效期';
COMMENT ON COLUMN "public"."system_oauth2_client"."redirect_uris" IS '可重定向的 URI 地址';
COMMENT ON COLUMN "public"."system_oauth2_client"."authorized_grant_types" IS '授权类型';
COMMENT ON COLUMN "public"."system_oauth2_client"."scopes" IS '授权范围';
COMMENT ON COLUMN "public"."system_oauth2_client"."auto_approve_scopes" IS '自动通过的授权范围';
COMMENT ON COLUMN "public"."system_oauth2_client"."authorities" IS '权限';
COMMENT ON COLUMN "public"."system_oauth2_client"."resource_ids" IS '资源';
COMMENT ON COLUMN "public"."system_oauth2_client"."additional_information" IS '附加信息';
COMMENT ON COLUMN "public"."system_oauth2_client"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_oauth2_client"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_oauth2_client"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_oauth2_client"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_oauth2_client"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_oauth2_client" IS 'OAuth2 客户端表';

-- ----------------------------
-- Table structure for system_oauth2_code
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_oauth2_code";
CREATE TABLE "public"."system_oauth2_code" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_type" int2 NOT NULL,
  "code" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "scopes" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "expires_time" timestamp(6) NOT NULL,
  "redirect_uri" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "state" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_oauth2_code" OWNER TO "system";
COMMENT ON COLUMN "public"."system_oauth2_code"."id" IS '编号';
COMMENT ON COLUMN "public"."system_oauth2_code"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_oauth2_code"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_oauth2_code"."code" IS '授权码';
COMMENT ON COLUMN "public"."system_oauth2_code"."client_id" IS '客户端编号';
COMMENT ON COLUMN "public"."system_oauth2_code"."scopes" IS '授权范围';
COMMENT ON COLUMN "public"."system_oauth2_code"."expires_time" IS '过期时间';
COMMENT ON COLUMN "public"."system_oauth2_code"."redirect_uri" IS '可重定向的 URI 地址';
COMMENT ON COLUMN "public"."system_oauth2_code"."state" IS '状态';
COMMENT ON COLUMN "public"."system_oauth2_code"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_oauth2_code"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_oauth2_code"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_oauth2_code"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_oauth2_code"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_oauth2_code"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_oauth2_code" IS 'OAuth2 授权码表';

-- ----------------------------
-- Table structure for system_oauth2_refresh_token
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_oauth2_refresh_token";
CREATE TABLE "public"."system_oauth2_refresh_token" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "refresh_token" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "user_type" int2 NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "scopes" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "expires_time" timestamp(6) NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_oauth2_refresh_token" OWNER TO "system";
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."id" IS '编号';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."refresh_token" IS '刷新令牌';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."client_id" IS '客户端编号';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."scopes" IS '授权范围';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."expires_time" IS '过期时间';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_oauth2_refresh_token"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_oauth2_refresh_token" IS 'OAuth2 刷新令牌';

-- ----------------------------
-- Table structure for system_operate_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_operate_log";
CREATE TABLE "public"."system_operate_log" (
  "id" int8 NOT NULL,
  "trace_id" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "user_id" int8 NOT NULL,
  "user_type" int2 NOT NULL DEFAULT 0,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sub_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "biz_id" int8 NOT NULL,
  "action" varchar(2000) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "success" bool NOT NULL DEFAULT true,
  "extra" varchar(2000) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "request_method" varchar(16) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "request_url" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "user_ip" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "user_agent" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_operate_log" OWNER TO "system";
COMMENT ON COLUMN "public"."system_operate_log"."id" IS '日志主键';
COMMENT ON COLUMN "public"."system_operate_log"."trace_id" IS '链路追踪编号';
COMMENT ON COLUMN "public"."system_operate_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_operate_log"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_operate_log"."type" IS '操作模块类型';
COMMENT ON COLUMN "public"."system_operate_log"."sub_type" IS '操作名';
COMMENT ON COLUMN "public"."system_operate_log"."biz_id" IS '操作数据模块编号';
COMMENT ON COLUMN "public"."system_operate_log"."action" IS '操作内容';
COMMENT ON COLUMN "public"."system_operate_log"."success" IS '操作结果';
COMMENT ON COLUMN "public"."system_operate_log"."extra" IS '拓展字段';
COMMENT ON COLUMN "public"."system_operate_log"."request_method" IS '请求方法名';
COMMENT ON COLUMN "public"."system_operate_log"."request_url" IS '请求地址';
COMMENT ON COLUMN "public"."system_operate_log"."user_ip" IS '用户 IP';
COMMENT ON COLUMN "public"."system_operate_log"."user_agent" IS '浏览器 UA';
COMMENT ON COLUMN "public"."system_operate_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_operate_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_operate_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_operate_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_operate_log"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_operate_log"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_operate_log" IS '操作日志记录 V2 版本';

-- ----------------------------
-- Table structure for system_post
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_post";
CREATE TABLE "public"."system_post" (
  "id" int8 NOT NULL,
  "code" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "sort" int4 NOT NULL,
  "status" int2 NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_post" OWNER TO "system";
COMMENT ON COLUMN "public"."system_post"."id" IS '岗位ID';
COMMENT ON COLUMN "public"."system_post"."code" IS '岗位编码';
COMMENT ON COLUMN "public"."system_post"."name" IS '岗位名称';
COMMENT ON COLUMN "public"."system_post"."sort" IS '显示顺序';
COMMENT ON COLUMN "public"."system_post"."status" IS '状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_post"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_post"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_post"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_post"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_post"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_post"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_post"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_post" IS '岗位信息表';

-- ----------------------------
-- Table structure for system_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_role";
CREATE TABLE "public"."system_role" (
  "id" int8 NOT NULL,
  "name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "sort" int4 NOT NULL,
  "data_scope" int2 NOT NULL DEFAULT 1,
  "data_scope_dept_ids" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL,
  "type" int2 NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_role" OWNER TO "system";
COMMENT ON COLUMN "public"."system_role"."id" IS '角色ID';
COMMENT ON COLUMN "public"."system_role"."name" IS '角色名称';
COMMENT ON COLUMN "public"."system_role"."code" IS '角色权限字符串';
COMMENT ON COLUMN "public"."system_role"."sort" IS '显示顺序';
COMMENT ON COLUMN "public"."system_role"."data_scope" IS '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）';
COMMENT ON COLUMN "public"."system_role"."data_scope_dept_ids" IS '数据范围 ( 指定部门数组)';
COMMENT ON COLUMN "public"."system_role"."status" IS '角色状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_role"."type" IS '角色类型';
COMMENT ON COLUMN "public"."system_role"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_role"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_role"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_role"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_role"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_role"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_role"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_role" IS '角色信息表';

-- ----------------------------
-- Table structure for system_role_menu
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_role_menu";
CREATE TABLE "public"."system_role_menu" (
  "id" int8 NOT NULL,
  "role_id" int8 NOT NULL,
  "menu_id" int8 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_role_menu" OWNER TO "system";
COMMENT ON COLUMN "public"."system_role_menu"."id" IS '自增编号';
COMMENT ON COLUMN "public"."system_role_menu"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."system_role_menu"."menu_id" IS '菜单ID';
COMMENT ON COLUMN "public"."system_role_menu"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_role_menu"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_role_menu"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_role_menu"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_role_menu"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_role_menu"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_role_menu" IS '角色和菜单关联表';

-- ----------------------------
-- Table structure for system_sms_channel
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_sms_channel";
CREATE TABLE "public"."system_sms_channel" (
  "id" int8 NOT NULL,
  "signature" varchar(12) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "api_key" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "api_secret" varchar(128) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "callback_url" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_sms_channel" OWNER TO "system";
COMMENT ON COLUMN "public"."system_sms_channel"."id" IS '编号';
COMMENT ON COLUMN "public"."system_sms_channel"."signature" IS '短信签名';
COMMENT ON COLUMN "public"."system_sms_channel"."code" IS '渠道编码';
COMMENT ON COLUMN "public"."system_sms_channel"."status" IS '开启状态';
COMMENT ON COLUMN "public"."system_sms_channel"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_sms_channel"."api_key" IS '短信 API 的账号';
COMMENT ON COLUMN "public"."system_sms_channel"."api_secret" IS '短信 API 的秘钥';
COMMENT ON COLUMN "public"."system_sms_channel"."callback_url" IS '短信发送回调 URL';
COMMENT ON COLUMN "public"."system_sms_channel"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_sms_channel"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_sms_channel"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_sms_channel"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_sms_channel"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_sms_channel" IS '短信渠道';

-- ----------------------------
-- Table structure for system_sms_code
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_sms_code";
CREATE TABLE "public"."system_sms_code" (
  "id" int8 NOT NULL,
  "mobile" varchar(11) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(6) COLLATE "pg_catalog"."default" NOT NULL,
  "create_ip" varchar(15) COLLATE "pg_catalog"."default" NOT NULL,
  "scene" int2 NOT NULL,
  "today_index" int2 NOT NULL,
  "used" int2 NOT NULL,
  "used_time" timestamp(6),
  "used_ip" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_sms_code" OWNER TO "system";
COMMENT ON COLUMN "public"."system_sms_code"."id" IS '编号';
COMMENT ON COLUMN "public"."system_sms_code"."mobile" IS '手机号';
COMMENT ON COLUMN "public"."system_sms_code"."code" IS '验证码';
COMMENT ON COLUMN "public"."system_sms_code"."create_ip" IS '创建 IP';
COMMENT ON COLUMN "public"."system_sms_code"."scene" IS '发送场景';
COMMENT ON COLUMN "public"."system_sms_code"."today_index" IS '今日发送的第几条';
COMMENT ON COLUMN "public"."system_sms_code"."used" IS '是否使用';
COMMENT ON COLUMN "public"."system_sms_code"."used_time" IS '使用时间';
COMMENT ON COLUMN "public"."system_sms_code"."used_ip" IS '使用 IP';
COMMENT ON COLUMN "public"."system_sms_code"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_sms_code"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_sms_code"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_sms_code"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_sms_code"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_sms_code"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_sms_code" IS '手机验证码';

-- ----------------------------
-- Table structure for system_sms_log
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_sms_log";
CREATE TABLE "public"."system_sms_log" (
  "id" int8 NOT NULL,
  "channel_id" int8 NOT NULL,
  "channel_code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "template_id" int8 NOT NULL,
  "template_code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "template_type" int2 NOT NULL,
  "template_content" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "template_params" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "api_template_id" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "mobile" varchar(11) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int8,
  "user_type" int2,
  "send_status" int2 NOT NULL DEFAULT 0,
  "send_time" timestamp(6),
  "api_send_code" varchar(63) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "api_send_msg" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "api_request_id" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "api_serial_no" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "receive_status" int2 NOT NULL DEFAULT 0,
  "receive_time" timestamp(6),
  "api_receive_code" varchar(63) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "api_receive_msg" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_sms_log" OWNER TO "system";
COMMENT ON COLUMN "public"."system_sms_log"."id" IS '编号';
COMMENT ON COLUMN "public"."system_sms_log"."channel_id" IS '短信渠道编号';
COMMENT ON COLUMN "public"."system_sms_log"."channel_code" IS '短信渠道编码';
COMMENT ON COLUMN "public"."system_sms_log"."template_id" IS '模板编号';
COMMENT ON COLUMN "public"."system_sms_log"."template_code" IS '模板编码';
COMMENT ON COLUMN "public"."system_sms_log"."template_type" IS '短信类型';
COMMENT ON COLUMN "public"."system_sms_log"."template_content" IS '短信内容';
COMMENT ON COLUMN "public"."system_sms_log"."template_params" IS '短信参数';
COMMENT ON COLUMN "public"."system_sms_log"."api_template_id" IS '短信 API 的模板编号';
COMMENT ON COLUMN "public"."system_sms_log"."mobile" IS '手机号';
COMMENT ON COLUMN "public"."system_sms_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_sms_log"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_sms_log"."send_status" IS '发送状态';
COMMENT ON COLUMN "public"."system_sms_log"."send_time" IS '发送时间';
COMMENT ON COLUMN "public"."system_sms_log"."api_send_code" IS '短信 API 发送结果的编码';
COMMENT ON COLUMN "public"."system_sms_log"."api_send_msg" IS '短信 API 发送失败的提示';
COMMENT ON COLUMN "public"."system_sms_log"."api_request_id" IS '短信 API 发送返回的唯一请求 ID';
COMMENT ON COLUMN "public"."system_sms_log"."api_serial_no" IS '短信 API 发送返回的序号';
COMMENT ON COLUMN "public"."system_sms_log"."receive_status" IS '接收状态';
COMMENT ON COLUMN "public"."system_sms_log"."receive_time" IS '接收时间';
COMMENT ON COLUMN "public"."system_sms_log"."api_receive_code" IS 'API 接收结果的编码';
COMMENT ON COLUMN "public"."system_sms_log"."api_receive_msg" IS 'API 接收结果的说明';
COMMENT ON COLUMN "public"."system_sms_log"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_sms_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_sms_log"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_sms_log"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_sms_log"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_sms_log" IS '短信日志';

-- ----------------------------
-- Table structure for system_sms_template
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_sms_template";
CREATE TABLE "public"."system_sms_template" (
  "id" int8 NOT NULL,
  "type" int2 NOT NULL,
  "status" int2 NOT NULL,
  "code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "content" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "params" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "api_template_id" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "channel_id" int8 NOT NULL,
  "channel_code" varchar(63) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_sms_template" OWNER TO "system";
COMMENT ON COLUMN "public"."system_sms_template"."id" IS '编号';
COMMENT ON COLUMN "public"."system_sms_template"."type" IS '模板类型';
COMMENT ON COLUMN "public"."system_sms_template"."status" IS '开启状态';
COMMENT ON COLUMN "public"."system_sms_template"."code" IS '模板编码';
COMMENT ON COLUMN "public"."system_sms_template"."name" IS '模板名称';
COMMENT ON COLUMN "public"."system_sms_template"."content" IS '模板内容';
COMMENT ON COLUMN "public"."system_sms_template"."params" IS '参数数组';
COMMENT ON COLUMN "public"."system_sms_template"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_sms_template"."api_template_id" IS '短信 API 的模板编号';
COMMENT ON COLUMN "public"."system_sms_template"."channel_id" IS '短信渠道编号';
COMMENT ON COLUMN "public"."system_sms_template"."channel_code" IS '短信渠道编码';
COMMENT ON COLUMN "public"."system_sms_template"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_sms_template"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_sms_template"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_sms_template"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_sms_template"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_sms_template" IS '短信模板';

-- ----------------------------
-- Table structure for system_social_client
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_social_client";
CREATE TABLE "public"."system_social_client" (
  "id" int8 NOT NULL,
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "social_type" int2 NOT NULL,
  "user_type" int2 NOT NULL,
  "client_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "client_secret" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "agent_id" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_social_client" OWNER TO "system";
COMMENT ON COLUMN "public"."system_social_client"."id" IS '编号';
COMMENT ON COLUMN "public"."system_social_client"."name" IS '应用名';
COMMENT ON COLUMN "public"."system_social_client"."social_type" IS '社交平台的类型';
COMMENT ON COLUMN "public"."system_social_client"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_social_client"."client_id" IS '客户端编号';
COMMENT ON COLUMN "public"."system_social_client"."client_secret" IS '客户端密钥';
COMMENT ON COLUMN "public"."system_social_client"."agent_id" IS '代理编号';
COMMENT ON COLUMN "public"."system_social_client"."status" IS '状态';
COMMENT ON COLUMN "public"."system_social_client"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_social_client"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_social_client"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_social_client"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_social_client"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_social_client"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_social_client" IS '社交客户端表';

-- ----------------------------
-- Table structure for system_social_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_social_user";
CREATE TABLE "public"."system_social_user" (
  "id" int8 NOT NULL,
  "type" int2 NOT NULL,
  "openid" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "token" varchar(256) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "raw_token_info" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
  "nickname" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "avatar" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "raw_user_info" varchar(1024) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(256) COLLATE "pg_catalog"."default" NOT NULL,
  "state" varchar(256) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_social_user" OWNER TO "system";
COMMENT ON COLUMN "public"."system_social_user"."id" IS '主键 ( 自增策略)';
COMMENT ON COLUMN "public"."system_social_user"."type" IS '社交平台的类型';
COMMENT ON COLUMN "public"."system_social_user"."openid" IS '社交 openid';
COMMENT ON COLUMN "public"."system_social_user"."token" IS '社交 token';
COMMENT ON COLUMN "public"."system_social_user"."raw_token_info" IS '原始 Token 数据，一般是 JSON 格式';
COMMENT ON COLUMN "public"."system_social_user"."nickname" IS '用户昵称';
COMMENT ON COLUMN "public"."system_social_user"."avatar" IS '用户头像';
COMMENT ON COLUMN "public"."system_social_user"."raw_user_info" IS '原始用户数据，一般是 JSON 格式';
COMMENT ON COLUMN "public"."system_social_user"."code" IS '最后一次的认证 code';
COMMENT ON COLUMN "public"."system_social_user"."state" IS '最后一次的认证 state';
COMMENT ON COLUMN "public"."system_social_user"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_social_user"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_social_user"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_social_user"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_social_user"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_social_user"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_social_user" IS '社交用户表';

-- ----------------------------
-- Table structure for system_social_user_bind
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_social_user_bind";
CREATE TABLE "public"."system_social_user_bind" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "user_type" int2 NOT NULL,
  "social_type" int2 NOT NULL,
  "social_user_id" int8 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_social_user_bind" OWNER TO "system";
COMMENT ON COLUMN "public"."system_social_user_bind"."id" IS '主键 ( 自增策略)';
COMMENT ON COLUMN "public"."system_social_user_bind"."user_id" IS '用户编号';
COMMENT ON COLUMN "public"."system_social_user_bind"."user_type" IS '用户类型';
COMMENT ON COLUMN "public"."system_social_user_bind"."social_type" IS '社交平台的类型';
COMMENT ON COLUMN "public"."system_social_user_bind"."social_user_id" IS '社交用户的编号';
COMMENT ON COLUMN "public"."system_social_user_bind"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_social_user_bind"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_social_user_bind"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_social_user_bind"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_social_user_bind"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_social_user_bind"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_social_user_bind" IS '社交绑定表';

-- ----------------------------
-- Table structure for system_tenant
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_tenant";
CREATE TABLE "public"."system_tenant" (
  "id" int8 NOT NULL,
  "name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "contact_user_id" int8,
  "contact_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "contact_mobile" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL DEFAULT 0,
  "websites" varchar(256) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "package_id" int8 NOT NULL,
  "expire_time" timestamp(6) NOT NULL,
  "account_count" int4 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_tenant" OWNER TO "system";
COMMENT ON COLUMN "public"."system_tenant"."id" IS '租户编号';
COMMENT ON COLUMN "public"."system_tenant"."name" IS '租户名';
COMMENT ON COLUMN "public"."system_tenant"."contact_user_id" IS '联系人的用户编号';
COMMENT ON COLUMN "public"."system_tenant"."contact_name" IS '联系人';
COMMENT ON COLUMN "public"."system_tenant"."contact_mobile" IS '联系手机';
COMMENT ON COLUMN "public"."system_tenant"."status" IS '租户状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_tenant"."websites" IS '绑定域名数组';
COMMENT ON COLUMN "public"."system_tenant"."package_id" IS '租户套餐编号';
COMMENT ON COLUMN "public"."system_tenant"."expire_time" IS '过期时间';
COMMENT ON COLUMN "public"."system_tenant"."account_count" IS '账号数量';
COMMENT ON COLUMN "public"."system_tenant"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_tenant"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_tenant"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_tenant"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_tenant"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_tenant" IS '租户表';

-- ----------------------------
-- Table structure for system_tenant_package
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_tenant_package";
CREATE TABLE "public"."system_tenant_package" (
  "id" int8 NOT NULL,
  "name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "status" int2 NOT NULL DEFAULT 0,
  "remark" varchar(256) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "menu_ids" varchar(4096) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_tenant_package" OWNER TO "system";
COMMENT ON COLUMN "public"."system_tenant_package"."id" IS '套餐编号';
COMMENT ON COLUMN "public"."system_tenant_package"."name" IS '套餐名';
COMMENT ON COLUMN "public"."system_tenant_package"."status" IS '租户状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_tenant_package"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_tenant_package"."menu_ids" IS '关联的菜单编号';
COMMENT ON COLUMN "public"."system_tenant_package"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_tenant_package"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_tenant_package"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_tenant_package"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_tenant_package"."deleted" IS '是否删除';
COMMENT ON TABLE "public"."system_tenant_package" IS '租户套餐表';

-- ----------------------------
-- Table structure for system_user_post
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_user_post";
CREATE TABLE "public"."system_user_post" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL DEFAULT 0,
  "post_id" int8 NOT NULL DEFAULT 0,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_user_post" OWNER TO "system";
COMMENT ON COLUMN "public"."system_user_post"."id" IS 'id';
COMMENT ON COLUMN "public"."system_user_post"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."system_user_post"."post_id" IS '岗位ID';
COMMENT ON COLUMN "public"."system_user_post"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_user_post"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_user_post"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_user_post"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_user_post"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_user_post"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_user_post" IS '用户岗位表';

-- ----------------------------
-- Table structure for system_user_role
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_user_role";
CREATE TABLE "public"."system_user_role" (
  "id" int8 NOT NULL,
  "user_id" int8 NOT NULL,
  "role_id" int8 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_user_role" OWNER TO "system";
COMMENT ON COLUMN "public"."system_user_role"."id" IS '自增编号';
COMMENT ON COLUMN "public"."system_user_role"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."system_user_role"."role_id" IS '角色ID';
COMMENT ON COLUMN "public"."system_user_role"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_user_role"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_user_role"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_user_role"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_user_role"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_user_role"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_user_role" IS '用户和角色关联表';

-- ----------------------------
-- Table structure for system_users
-- ----------------------------
DROP TABLE IF EXISTS "public"."system_users";
CREATE TABLE "public"."system_users" (
  "id" int8 NOT NULL,
  "username" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "nickname" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(500) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "dept_id" int8,
  "post_ids" varchar(255) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "email" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "mobile" varchar(11) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "sex" int2 DEFAULT 0,
  "avatar" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "status" int2 NOT NULL DEFAULT 0,
  "login_ip" varchar(50) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "login_date" timestamp(6),
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."system_users" OWNER TO "system";
COMMENT ON COLUMN "public"."system_users"."id" IS '用户ID';
COMMENT ON COLUMN "public"."system_users"."username" IS '用户账号';
COMMENT ON COLUMN "public"."system_users"."password" IS '密码';
COMMENT ON COLUMN "public"."system_users"."nickname" IS '用户昵称';
COMMENT ON COLUMN "public"."system_users"."remark" IS '备注';
COMMENT ON COLUMN "public"."system_users"."dept_id" IS '部门ID';
COMMENT ON COLUMN "public"."system_users"."post_ids" IS '岗位编号数组';
COMMENT ON COLUMN "public"."system_users"."email" IS '用户邮箱';
COMMENT ON COLUMN "public"."system_users"."mobile" IS '手机号码';
COMMENT ON COLUMN "public"."system_users"."sex" IS '用户性别';
COMMENT ON COLUMN "public"."system_users"."avatar" IS '头像地址';
COMMENT ON COLUMN "public"."system_users"."status" IS '帐号状态（0正常 1停用）';
COMMENT ON COLUMN "public"."system_users"."login_ip" IS '最后登录IP';
COMMENT ON COLUMN "public"."system_users"."login_date" IS '最后登录时间';
COMMENT ON COLUMN "public"."system_users"."creator" IS '创建者';
COMMENT ON COLUMN "public"."system_users"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."system_users"."updater" IS '更新者';
COMMENT ON COLUMN "public"."system_users"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."system_users"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."system_users"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."system_users" IS '用户信息表';

-- ----------------------------
-- Table structure for yudao_demo01_contact
-- ----------------------------
DROP TABLE IF EXISTS "public"."yudao_demo01_contact";
CREATE TABLE "public"."yudao_demo01_contact" (
  "id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "sex" int2 NOT NULL,
  "birthday" timestamp(6) NOT NULL,
  "description" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "avatar" varchar(512) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."yudao_demo01_contact" OWNER TO "system";
COMMENT ON COLUMN "public"."yudao_demo01_contact"."id" IS '编号';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."name" IS '名字';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."sex" IS '性别';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."birthday" IS '出生年';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."description" IS '简介';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."avatar" IS '头像';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."creator" IS '创建者';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."updater" IS '更新者';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."yudao_demo01_contact"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."yudao_demo01_contact" IS '示例联系人表';

-- ----------------------------
-- Table structure for yudao_demo02_category
-- ----------------------------
DROP TABLE IF EXISTS "public"."yudao_demo02_category";
CREATE TABLE "public"."yudao_demo02_category" (
  "id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "parent_id" int8 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."yudao_demo02_category" OWNER TO "system";
COMMENT ON COLUMN "public"."yudao_demo02_category"."id" IS '编号';
COMMENT ON COLUMN "public"."yudao_demo02_category"."name" IS '名字';
COMMENT ON COLUMN "public"."yudao_demo02_category"."parent_id" IS '父级编号';
COMMENT ON COLUMN "public"."yudao_demo02_category"."creator" IS '创建者';
COMMENT ON COLUMN "public"."yudao_demo02_category"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."yudao_demo02_category"."updater" IS '更新者';
COMMENT ON COLUMN "public"."yudao_demo02_category"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."yudao_demo02_category"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."yudao_demo02_category"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."yudao_demo02_category" IS '示例分类表';

-- ----------------------------
-- Table structure for yudao_demo03_course
-- ----------------------------
DROP TABLE IF EXISTS "public"."yudao_demo03_course";
CREATE TABLE "public"."yudao_demo03_course" (
  "id" int8 NOT NULL,
  "student_id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "score" int2 NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."yudao_demo03_course" OWNER TO "system";
COMMENT ON COLUMN "public"."yudao_demo03_course"."id" IS '编号';
COMMENT ON COLUMN "public"."yudao_demo03_course"."student_id" IS '学生编号';
COMMENT ON COLUMN "public"."yudao_demo03_course"."name" IS '名字';
COMMENT ON COLUMN "public"."yudao_demo03_course"."score" IS '分数';
COMMENT ON COLUMN "public"."yudao_demo03_course"."creator" IS '创建者';
COMMENT ON COLUMN "public"."yudao_demo03_course"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."yudao_demo03_course"."updater" IS '更新者';
COMMENT ON COLUMN "public"."yudao_demo03_course"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."yudao_demo03_course"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."yudao_demo03_course"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."yudao_demo03_course" IS '学生课程表';

-- ----------------------------
-- Table structure for yudao_demo03_grade
-- ----------------------------
DROP TABLE IF EXISTS "public"."yudao_demo03_grade";
CREATE TABLE "public"."yudao_demo03_grade" (
  "id" int8 NOT NULL,
  "student_id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "teacher" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."yudao_demo03_grade" OWNER TO "system";
COMMENT ON COLUMN "public"."yudao_demo03_grade"."id" IS '编号';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."student_id" IS '学生编号';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."name" IS '名字';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."teacher" IS '班主任';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."creator" IS '创建者';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."updater" IS '更新者';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."yudao_demo03_grade"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."yudao_demo03_grade" IS '学生班级表';

-- ----------------------------
-- Table structure for yudao_demo03_student
-- ----------------------------
DROP TABLE IF EXISTS "public"."yudao_demo03_student";
CREATE TABLE "public"."yudao_demo03_student" (
  "id" int8 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "sex" int2 NOT NULL,
  "birthday" timestamp(6) NOT NULL,
  "description" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "creator" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "create_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updater" varchar(64) COLLATE "pg_catalog"."default" DEFAULT NULL::varchar,
  "update_time" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" int2 NOT NULL DEFAULT 0,
  "tenant_id" int8 NOT NULL DEFAULT 0
)
;
ALTER TABLE "public"."yudao_demo03_student" OWNER TO "system";
COMMENT ON COLUMN "public"."yudao_demo03_student"."id" IS '编号';
COMMENT ON COLUMN "public"."yudao_demo03_student"."name" IS '名字';
COMMENT ON COLUMN "public"."yudao_demo03_student"."sex" IS '性别';
COMMENT ON COLUMN "public"."yudao_demo03_student"."birthday" IS '出生日期';
COMMENT ON COLUMN "public"."yudao_demo03_student"."description" IS '简介';
COMMENT ON COLUMN "public"."yudao_demo03_student"."creator" IS '创建者';
COMMENT ON COLUMN "public"."yudao_demo03_student"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."yudao_demo03_student"."updater" IS '更新者';
COMMENT ON COLUMN "public"."yudao_demo03_student"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."yudao_demo03_student"."deleted" IS '是否删除';
COMMENT ON COLUMN "public"."yudao_demo03_student"."tenant_id" IS '租户编号';
COMMENT ON TABLE "public"."yudao_demo03_student" IS '学生表';

-- ----------------------------
-- Function structure for sys_stat_statements
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sys_stat_statements"("showtext" bool, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8);
CREATE OR REPLACE FUNCTION "public"."sys_stat_statements"(IN "showtext" bool, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8)
  RETURNS SETOF "pg_catalog"."record" AS '$libdir/sys_stat_statements', 'sys_stat_statements_1_8'
  LANGUAGE c VOLATILE STRICT
  COST 1
  ROWS 1000;
ALTER FUNCTION "public"."sys_stat_statements"("showtext" bool, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8) OWNER TO "system";

-- ----------------------------
-- Function structure for sys_stat_statements_all
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sys_stat_statements_all"("showtext" bool, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "parent_queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8);
CREATE OR REPLACE FUNCTION "public"."sys_stat_statements_all"(IN "showtext" bool, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "parent_queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8)
  RETURNS SETOF "pg_catalog"."record" AS '$libdir/sys_stat_statements', 'sys_stat_statements_1_10'
  LANGUAGE c VOLATILE STRICT
  COST 1
  ROWS 1000;
ALTER FUNCTION "public"."sys_stat_statements_all"("showtext" bool, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "parent_queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8) OWNER TO "system";

-- ----------------------------
-- Function structure for sys_stat_statements_get_reset_time
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sys_stat_statements_get_reset_time"(OUT "reset_time" timestamptz);
CREATE OR REPLACE FUNCTION "public"."sys_stat_statements_get_reset_time"(OUT "reset_time" timestamptz)
  RETURNS "pg_catalog"."timestamptz" AS '$libdir/sys_stat_statements', 'sys_stat_statements_get_reset_time'
  LANGUAGE c VOLATILE STRICT
  COST 1;
ALTER FUNCTION "public"."sys_stat_statements_get_reset_time"(OUT "reset_time" timestamptz) OWNER TO "system";

-- ----------------------------
-- Function structure for sys_stat_statements_limit_len
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sys_stat_statements_limit_len"("showtext" bool, "limit_query_len" int4, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8);
CREATE OR REPLACE FUNCTION "public"."sys_stat_statements_limit_len"(IN "showtext" bool, IN "limit_query_len" int4, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8)
  RETURNS SETOF "pg_catalog"."record" AS '$libdir/sys_stat_statements', 'sys_stat_statements_1_9'
  LANGUAGE c VOLATILE STRICT
  COST 1
  ROWS 1000;
ALTER FUNCTION "public"."sys_stat_statements_limit_len"("showtext" bool, "limit_query_len" int4, OUT "userid" oid, OUT "dbid" oid, OUT "queryid" int8, OUT "query" text, OUT "parses" int8, OUT "total_parse_time" float8, OUT "min_parse_time" float8, OUT "max_parse_time" float8, OUT "mean_parse_time" float8, OUT "stddev_parse_time" float8, OUT "plans" int8, OUT "total_plan_time" float8, OUT "min_plan_time" float8, OUT "max_plan_time" float8, OUT "mean_plan_time" float8, OUT "stddev_plan_time" float8, OUT "calls" int8, OUT "total_exec_time" float8, OUT "min_exec_time" float8, OUT "max_exec_time" float8, OUT "mean_exec_time" float8, OUT "stddev_exec_time" float8, OUT "rows" int8, OUT "shared_blks_hit" int8, OUT "shared_blks_read" int8, OUT "shared_blks_dirtied" int8, OUT "shared_blks_written" int8, OUT "local_blks_hit" int8, OUT "local_blks_read" int8, OUT "local_blks_dirtied" int8, OUT "local_blks_written" int8, OUT "temp_blks_read" int8, OUT "temp_blks_written" int8, OUT "blk_read_time" float8, OUT "blk_write_time" float8) OWNER TO "system";

-- ----------------------------
-- Function structure for sys_stat_statements_reset
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."sys_stat_statements_reset"("userid" oid, "dbid" oid, "queryid" int8);
CREATE OR REPLACE FUNCTION "public"."sys_stat_statements_reset"("userid" oid=0, "dbid" oid=0, "queryid" int8=0)
  RETURNS "pg_catalog"."void" AS '$libdir/sys_stat_statements', 'sys_stat_statements_reset_1_7'
  LANGUAGE c VOLATILE STRICT
  COST 1;
ALTER FUNCTION "public"."sys_stat_statements_reset"("userid" oid, "dbid" oid, "queryid" int8) OWNER TO "system";

-- ----------------------------
-- View structure for sys_stat_statements_all
-- ----------------------------
DROP VIEW IF EXISTS "public"."sys_stat_statements_all";
CREATE VIEW "public"."sys_stat_statements_all" AS  SELECT sys_stat_statements_all.userid,
    sys_stat_statements_all.dbid,
    sys_stat_statements_all.queryid,
    sys_stat_statements_all.parent_queryid,
    sys_stat_statements_all.query,
    sys_stat_statements_all.parses,
    sys_stat_statements_all.total_parse_time,
    sys_stat_statements_all.min_parse_time,
    sys_stat_statements_all.max_parse_time,
    sys_stat_statements_all.mean_parse_time,
    sys_stat_statements_all.stddev_parse_time,
    sys_stat_statements_all.plans,
    sys_stat_statements_all.total_plan_time,
    sys_stat_statements_all.min_plan_time,
    sys_stat_statements_all.max_plan_time,
    sys_stat_statements_all.mean_plan_time,
    sys_stat_statements_all.stddev_plan_time,
    sys_stat_statements_all.calls,
    sys_stat_statements_all.total_exec_time,
    sys_stat_statements_all.min_exec_time,
    sys_stat_statements_all.max_exec_time,
    sys_stat_statements_all.mean_exec_time,
    sys_stat_statements_all.stddev_exec_time,
    sys_stat_statements_all.rows,
    sys_stat_statements_all.shared_blks_hit,
    sys_stat_statements_all.shared_blks_read,
    sys_stat_statements_all.shared_blks_dirtied,
    sys_stat_statements_all.shared_blks_written,
    sys_stat_statements_all.local_blks_hit,
    sys_stat_statements_all.local_blks_read,
    sys_stat_statements_all.local_blks_dirtied,
    sys_stat_statements_all.local_blks_written,
    sys_stat_statements_all.temp_blks_read,
    sys_stat_statements_all.temp_blks_written,
    sys_stat_statements_all.blk_read_time,
    sys_stat_statements_all.blk_write_time
   FROM sys_stat_statements_all(true) sys_stat_statements_all(userid, dbid, queryid, parent_queryid, query, parses, total_parse_time, min_parse_time, max_parse_time, mean_parse_time, stddev_parse_time, plans, total_plan_time, min_plan_time, max_plan_time, mean_plan_time, stddev_plan_time, calls, total_exec_time, min_exec_time, max_exec_time, mean_exec_time, stddev_exec_time, rows, shared_blks_hit, shared_blks_read, shared_blks_dirtied, shared_blks_written, local_blks_hit, local_blks_read, local_blks_dirtied, local_blks_written, temp_blks_read, temp_blks_written, blk_read_time, blk_write_time);
ALTER TABLE "public"."sys_stat_statements_all" OWNER TO "system";

-- ----------------------------
-- View structure for sys_stat_statements
-- ----------------------------
DROP VIEW IF EXISTS "public"."sys_stat_statements";
CREATE VIEW "public"."sys_stat_statements" AS  SELECT sys_stat_statements_all.userid,
    sys_stat_statements_all.dbid,
    sys_stat_statements_all.queryid,
    sys_stat_statements_all.query,
    sys_stat_statements_all.parses,
    sys_stat_statements_all.total_parse_time,
    sys_stat_statements_all.min_parse_time,
    sys_stat_statements_all.max_parse_time,
    sys_stat_statements_all.mean_parse_time,
    sys_stat_statements_all.stddev_parse_time,
    sys_stat_statements_all.plans,
    sys_stat_statements_all.total_plan_time,
    sys_stat_statements_all.min_plan_time,
    sys_stat_statements_all.max_plan_time,
    sys_stat_statements_all.mean_plan_time,
    sys_stat_statements_all.stddev_plan_time,
    sys_stat_statements_all.calls,
    sys_stat_statements_all.total_exec_time,
    sys_stat_statements_all.min_exec_time,
    sys_stat_statements_all.max_exec_time,
    sys_stat_statements_all.mean_exec_time,
    sys_stat_statements_all.stddev_exec_time,
    sys_stat_statements_all.rows,
    sys_stat_statements_all.shared_blks_hit,
    sys_stat_statements_all.shared_blks_read,
    sys_stat_statements_all.shared_blks_dirtied,
    sys_stat_statements_all.shared_blks_written,
    sys_stat_statements_all.local_blks_hit,
    sys_stat_statements_all.local_blks_read,
    sys_stat_statements_all.local_blks_dirtied,
    sys_stat_statements_all.local_blks_written,
    sys_stat_statements_all.temp_blks_read,
    sys_stat_statements_all.temp_blks_written,
    sys_stat_statements_all.blk_read_time,
    sys_stat_statements_all.blk_write_time
   FROM sys_stat_statements_all
  WHERE sys_stat_statements_all.parent_queryid = 0 OR sys_stat_statements_all.queryid IS NULL;
ALTER TABLE "public"."sys_stat_statements" OWNER TO "system";

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."act_evt_log_log_nr__seq"
OWNED BY "public"."act_evt_log"."log_nr_";
SELECT setval('"public"."act_evt_log_log_nr__seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."act_hi_tsk_log_id__seq"
OWNED BY "public"."act_hi_tsk_log"."id_";
SELECT setval('"public"."act_hi_tsk_log_id__seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_api_access_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_api_error_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_codegen_column_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_codegen_table_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_config_seq"', 14, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_data_source_config_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_file_config_seq"', 31, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_file_content_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_file_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_job_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."infra_job_seq"', 36, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_dept_seq"', 114, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_dict_data_seq"', 3003, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_dict_type_seq"', 1014, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_login_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_mail_account_seq"', 5, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_mail_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_mail_template_seq"', 16, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_menu_seq"', 5013, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_notice_seq"', 5, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_notify_message_seq"', 11, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_notify_template_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_oauth2_access_token_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_oauth2_approve_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_oauth2_client_seq"', 43, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_oauth2_code_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_oauth2_refresh_token_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_operate_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_post_seq"', 6, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_role_menu_seq"', 6139, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_role_seq"', 159, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_sms_channel_seq"', 8, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_sms_code_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_sms_log_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_sms_template_seq"', 20, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_social_client_seq"', 45, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_social_user_bind_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_social_user_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_tenant_package_seq"', 113, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_tenant_seq"', 123, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_user_post_seq"', 126, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_user_role_seq"', 49, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."system_users_seq"', 142, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."yudao_demo01_contact_seq"', 2, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."yudao_demo02_category_seq"', 7, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."yudao_demo03_course_seq"', 21, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."yudao_demo03_grade_seq"', 10, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
SELECT setval('"public"."yudao_demo03_student_seq"', 10, false);

-- ----------------------------
-- Primary Key structure for table act_evt_log
-- ----------------------------
ALTER TABLE "public"."act_evt_log" ADD CONSTRAINT "act_evt_log_pkey" PRIMARY KEY ("log_nr_");

-- ----------------------------
-- Indexes structure for table act_ge_bytearray
-- ----------------------------
CREATE INDEX "act_idx_bytear_depl" ON "public"."act_ge_bytearray" USING btree (
  "deployment_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ge_bytearray
-- ----------------------------
ALTER TABLE "public"."act_ge_bytearray" ADD CONSTRAINT "act_ge_bytearray_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_ge_property
-- ----------------------------
ALTER TABLE "public"."act_ge_property" ADD CONSTRAINT "act_ge_property_pkey" PRIMARY KEY ("name_");

-- ----------------------------
-- Indexes structure for table act_hi_actinst
-- ----------------------------
CREATE INDEX "act_idx_hi_act_inst_end" ON "public"."act_hi_actinst" USING btree (
  "end_time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_act_inst_exec" ON "public"."act_hi_actinst" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "act_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_act_inst_procinst" ON "public"."act_hi_actinst" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "act_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_act_inst_start" ON "public"."act_hi_actinst" USING btree (
  "start_time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_hi_actinst
-- ----------------------------
ALTER TABLE "public"."act_hi_actinst" ADD CONSTRAINT "act_hi_actinst_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_hi_attachment
-- ----------------------------
ALTER TABLE "public"."act_hi_attachment" ADD CONSTRAINT "act_hi_attachment_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_hi_comment
-- ----------------------------
ALTER TABLE "public"."act_hi_comment" ADD CONSTRAINT "act_hi_comment_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_hi_detail
-- ----------------------------
CREATE INDEX "act_idx_hi_detail_act_inst" ON "public"."act_hi_detail" USING btree (
  "act_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_detail_name" ON "public"."act_hi_detail" USING btree (
  "name_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_detail_proc_inst" ON "public"."act_hi_detail" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_detail_task_id" ON "public"."act_hi_detail" USING btree (
  "task_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_detail_time" ON "public"."act_hi_detail" USING btree (
  "time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_hi_detail
-- ----------------------------
ALTER TABLE "public"."act_hi_detail" ADD CONSTRAINT "act_hi_detail_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_hi_entitylink
-- ----------------------------
CREATE INDEX "act_idx_hi_ent_lnk_ref_scope" ON "public"."act_hi_entitylink" USING btree (
  "ref_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "ref_scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ent_lnk_root_scope" ON "public"."act_hi_entitylink" USING btree (
  "root_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "root_scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ent_lnk_scope" ON "public"."act_hi_entitylink" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ent_lnk_scope_def" ON "public"."act_hi_entitylink" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_hi_entitylink
-- ----------------------------
ALTER TABLE "public"."act_hi_entitylink" ADD CONSTRAINT "act_hi_entitylink_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_hi_identitylink
-- ----------------------------
CREATE INDEX "act_idx_hi_ident_lnk_procinst" ON "public"."act_hi_identitylink" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ident_lnk_scope" ON "public"."act_hi_identitylink" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ident_lnk_scope_def" ON "public"."act_hi_identitylink" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ident_lnk_sub_scope" ON "public"."act_hi_identitylink" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ident_lnk_task" ON "public"."act_hi_identitylink" USING btree (
  "task_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_ident_lnk_user" ON "public"."act_hi_identitylink" USING btree (
  "user_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_hi_identitylink
-- ----------------------------
ALTER TABLE "public"."act_hi_identitylink" ADD CONSTRAINT "act_hi_identitylink_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_hi_procinst
-- ----------------------------
CREATE INDEX "act_idx_hi_pro_i_buskey" ON "public"."act_hi_procinst" USING btree (
  "business_key_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_pro_inst_end" ON "public"."act_hi_procinst" USING btree (
  "end_time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_pro_super_procinst" ON "public"."act_hi_procinst" USING btree (
  "super_process_instance_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table act_hi_procinst
-- ----------------------------
ALTER TABLE "public"."act_hi_procinst" ADD CONSTRAINT "act_hi_procinst_proc_inst_id__key" UNIQUE ("proc_inst_id_");

-- ----------------------------
-- Primary Key structure for table act_hi_procinst
-- ----------------------------
ALTER TABLE "public"."act_hi_procinst" ADD CONSTRAINT "act_hi_procinst_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_hi_taskinst
-- ----------------------------
CREATE INDEX "act_idx_hi_task_inst_procinst" ON "public"."act_hi_taskinst" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_task_scope" ON "public"."act_hi_taskinst" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_task_scope_def" ON "public"."act_hi_taskinst" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_task_sub_scope" ON "public"."act_hi_taskinst" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_hi_taskinst
-- ----------------------------
ALTER TABLE "public"."act_hi_taskinst" ADD CONSTRAINT "act_hi_taskinst_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_hi_tsk_log
-- ----------------------------
ALTER TABLE "public"."act_hi_tsk_log" ADD CONSTRAINT "act_hi_tsk_log_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_hi_varinst
-- ----------------------------
CREATE INDEX "act_idx_hi_procvar_exe" ON "public"."act_hi_varinst" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_procvar_name_type" ON "public"."act_hi_varinst" USING btree (
  "name_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "var_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_procvar_proc_inst" ON "public"."act_hi_varinst" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_procvar_task_id" ON "public"."act_hi_varinst" USING btree (
  "task_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_var_scope_id_type" ON "public"."act_hi_varinst" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_hi_var_sub_id_type" ON "public"."act_hi_varinst" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_hi_varinst
-- ----------------------------
ALTER TABLE "public"."act_hi_varinst" ADD CONSTRAINT "act_hi_varinst_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_id_bytearray
-- ----------------------------
ALTER TABLE "public"."act_id_bytearray" ADD CONSTRAINT "act_id_bytearray_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_id_group
-- ----------------------------
ALTER TABLE "public"."act_id_group" ADD CONSTRAINT "act_id_group_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_id_info
-- ----------------------------
ALTER TABLE "public"."act_id_info" ADD CONSTRAINT "act_id_info_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_id_membership
-- ----------------------------
CREATE INDEX "act_idx_memb_group" ON "public"."act_id_membership" USING btree (
  "group_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_memb_user" ON "public"."act_id_membership" USING btree (
  "user_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_id_membership
-- ----------------------------
ALTER TABLE "public"."act_id_membership" ADD CONSTRAINT "act_id_membership_pkey" PRIMARY KEY ("user_id_", "group_id_");

-- ----------------------------
-- Uniques structure for table act_id_priv
-- ----------------------------
ALTER TABLE "public"."act_id_priv" ADD CONSTRAINT "act_uniq_priv_name" UNIQUE ("name_");

-- ----------------------------
-- Primary Key structure for table act_id_priv
-- ----------------------------
ALTER TABLE "public"."act_id_priv" ADD CONSTRAINT "act_id_priv_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_id_priv_mapping
-- ----------------------------
CREATE INDEX "act_idx_priv_group" ON "public"."act_id_priv_mapping" USING btree (
  "group_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_priv_mapping" ON "public"."act_id_priv_mapping" USING btree (
  "priv_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_priv_user" ON "public"."act_id_priv_mapping" USING btree (
  "user_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_id_priv_mapping
-- ----------------------------
ALTER TABLE "public"."act_id_priv_mapping" ADD CONSTRAINT "act_id_priv_mapping_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_id_property
-- ----------------------------
ALTER TABLE "public"."act_id_property" ADD CONSTRAINT "act_id_property_pkey" PRIMARY KEY ("name_");

-- ----------------------------
-- Primary Key structure for table act_id_token
-- ----------------------------
ALTER TABLE "public"."act_id_token" ADD CONSTRAINT "act_id_token_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_id_user
-- ----------------------------
ALTER TABLE "public"."act_id_user" ADD CONSTRAINT "act_id_user_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_procdef_info
-- ----------------------------
CREATE INDEX "act_idx_procdef_info_json" ON "public"."act_procdef_info" USING btree (
  "info_json_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_procdef_info_proc" ON "public"."act_procdef_info" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table act_procdef_info
-- ----------------------------
ALTER TABLE "public"."act_procdef_info" ADD CONSTRAINT "act_uniq_info_procdef" UNIQUE ("proc_def_id_");

-- ----------------------------
-- Primary Key structure for table act_procdef_info
-- ----------------------------
ALTER TABLE "public"."act_procdef_info" ADD CONSTRAINT "act_procdef_info_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_re_deployment
-- ----------------------------
ALTER TABLE "public"."act_re_deployment" ADD CONSTRAINT "act_re_deployment_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_re_model
-- ----------------------------
CREATE INDEX "act_idx_model_deployment" ON "public"."act_re_model" USING btree (
  "deployment_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_model_source" ON "public"."act_re_model" USING btree (
  "editor_source_value_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_model_source_extra" ON "public"."act_re_model" USING btree (
  "editor_source_extra_value_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_re_model
-- ----------------------------
ALTER TABLE "public"."act_re_model" ADD CONSTRAINT "act_re_model_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Uniques structure for table act_re_procdef
-- ----------------------------
ALTER TABLE "public"."act_re_procdef" ADD CONSTRAINT "act_uniq_procdef" UNIQUE ("key_", "version_", "derived_version_", "tenant_id_");

-- ----------------------------
-- Primary Key structure for table act_re_procdef
-- ----------------------------
ALTER TABLE "public"."act_re_procdef" ADD CONSTRAINT "act_re_procdef_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_actinst
-- ----------------------------
CREATE INDEX "act_idx_ru_acti_end" ON "public"."act_ru_actinst" USING btree (
  "end_time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_acti_exec" ON "public"."act_ru_actinst" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_acti_exec_act" ON "public"."act_ru_actinst" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "act_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_acti_proc" ON "public"."act_ru_actinst" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_acti_proc_act" ON "public"."act_ru_actinst" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "act_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_acti_start" ON "public"."act_ru_actinst" USING btree (
  "start_time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_acti_task" ON "public"."act_ru_actinst" USING btree (
  "task_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_actinst
-- ----------------------------
ALTER TABLE "public"."act_ru_actinst" ADD CONSTRAINT "act_ru_actinst_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_deadletter_job
-- ----------------------------
CREATE INDEX "act_idx_deadletter_job_correlation_id" ON "public"."act_ru_deadletter_job" USING btree (
  "correlation_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_deadletter_job_custom_values_id" ON "public"."act_ru_deadletter_job" USING btree (
  "custom_values_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_deadletter_job_exception_stack_id" ON "public"."act_ru_deadletter_job" USING btree (
  "exception_stack_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_deadletter_job_execution_id" ON "public"."act_ru_deadletter_job" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_deadletter_job_proc_def_id" ON "public"."act_ru_deadletter_job" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_deadletter_job_process_instance_id" ON "public"."act_ru_deadletter_job" USING btree (
  "process_instance_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_djob_scope" ON "public"."act_ru_deadletter_job" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_djob_scope_def" ON "public"."act_ru_deadletter_job" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_djob_sub_scope" ON "public"."act_ru_deadletter_job" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_deadletter_job
-- ----------------------------
ALTER TABLE "public"."act_ru_deadletter_job" ADD CONSTRAINT "act_ru_deadletter_job_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_entitylink
-- ----------------------------
CREATE INDEX "act_idx_ent_lnk_ref_scope" ON "public"."act_ru_entitylink" USING btree (
  "ref_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "ref_scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ent_lnk_root_scope" ON "public"."act_ru_entitylink" USING btree (
  "root_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "root_scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ent_lnk_scope" ON "public"."act_ru_entitylink" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ent_lnk_scope_def" ON "public"."act_ru_entitylink" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "link_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_entitylink
-- ----------------------------
ALTER TABLE "public"."act_ru_entitylink" ADD CONSTRAINT "act_ru_entitylink_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_event_subscr
-- ----------------------------
CREATE INDEX "act_idx_event_subscr" ON "public"."act_ru_event_subscr" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_event_subscr_config_" ON "public"."act_ru_event_subscr" USING btree (
  "configuration_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_event_subscr_scoperef_" ON "public"."act_ru_event_subscr" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_event_subscr
-- ----------------------------
ALTER TABLE "public"."act_ru_event_subscr" ADD CONSTRAINT "act_ru_event_subscr_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_execution
-- ----------------------------
CREATE INDEX "act_idx_exe_parent" ON "public"."act_ru_execution" USING btree (
  "parent_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_exe_procdef" ON "public"."act_ru_execution" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_exe_procinst" ON "public"."act_ru_execution" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_exe_root" ON "public"."act_ru_execution" USING btree (
  "root_proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_exe_super" ON "public"."act_ru_execution" USING btree (
  "super_exec_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_exec_buskey" ON "public"."act_ru_execution" USING btree (
  "business_key_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_exec_ref_id_" ON "public"."act_ru_execution" USING btree (
  "reference_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_execution
-- ----------------------------
ALTER TABLE "public"."act_ru_execution" ADD CONSTRAINT "act_ru_execution_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_external_job
-- ----------------------------
CREATE INDEX "act_idx_ejob_scope" ON "public"."act_ru_external_job" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ejob_scope_def" ON "public"."act_ru_external_job" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ejob_sub_scope" ON "public"."act_ru_external_job" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_external_job_correlation_id" ON "public"."act_ru_external_job" USING btree (
  "correlation_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_external_job_custom_values_id" ON "public"."act_ru_external_job" USING btree (
  "custom_values_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_external_job_exception_stack_id" ON "public"."act_ru_external_job" USING btree (
  "exception_stack_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_external_job
-- ----------------------------
ALTER TABLE "public"."act_ru_external_job" ADD CONSTRAINT "act_ru_external_job_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table act_ru_history_job
-- ----------------------------
ALTER TABLE "public"."act_ru_history_job" ADD CONSTRAINT "act_ru_history_job_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_identitylink
-- ----------------------------
CREATE INDEX "act_idx_athrz_procedef" ON "public"."act_ru_identitylink" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ident_lnk_group" ON "public"."act_ru_identitylink" USING btree (
  "group_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ident_lnk_scope" ON "public"."act_ru_identitylink" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ident_lnk_scope_def" ON "public"."act_ru_identitylink" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ident_lnk_sub_scope" ON "public"."act_ru_identitylink" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ident_lnk_user" ON "public"."act_ru_identitylink" USING btree (
  "user_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_idl_procinst" ON "public"."act_ru_identitylink" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_tskass_task" ON "public"."act_ru_identitylink" USING btree (
  "task_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_identitylink
-- ----------------------------
ALTER TABLE "public"."act_ru_identitylink" ADD CONSTRAINT "act_ru_identitylink_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_job
-- ----------------------------
CREATE INDEX "act_idx_job_correlation_id" ON "public"."act_ru_job" USING btree (
  "correlation_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_custom_values_id" ON "public"."act_ru_job" USING btree (
  "custom_values_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_exception_stack_id" ON "public"."act_ru_job" USING btree (
  "exception_stack_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_execution_id" ON "public"."act_ru_job" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_proc_def_id" ON "public"."act_ru_job" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_process_instance_id" ON "public"."act_ru_job" USING btree (
  "process_instance_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_scope" ON "public"."act_ru_job" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_scope_def" ON "public"."act_ru_job" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_job_sub_scope" ON "public"."act_ru_job" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_job
-- ----------------------------
ALTER TABLE "public"."act_ru_job" ADD CONSTRAINT "act_ru_job_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_suspended_job
-- ----------------------------
CREATE INDEX "act_idx_sjob_scope" ON "public"."act_ru_suspended_job" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_sjob_scope_def" ON "public"."act_ru_suspended_job" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_sjob_sub_scope" ON "public"."act_ru_suspended_job" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_suspended_job_correlation_id" ON "public"."act_ru_suspended_job" USING btree (
  "correlation_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_suspended_job_custom_values_id" ON "public"."act_ru_suspended_job" USING btree (
  "custom_values_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_suspended_job_exception_stack_id" ON "public"."act_ru_suspended_job" USING btree (
  "exception_stack_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_suspended_job_execution_id" ON "public"."act_ru_suspended_job" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_suspended_job_proc_def_id" ON "public"."act_ru_suspended_job" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_suspended_job_process_instance_id" ON "public"."act_ru_suspended_job" USING btree (
  "process_instance_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_suspended_job
-- ----------------------------
ALTER TABLE "public"."act_ru_suspended_job" ADD CONSTRAINT "act_ru_suspended_job_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_task
-- ----------------------------
CREATE INDEX "act_idx_task_create" ON "public"."act_ru_task" USING btree (
  "create_time_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_task_exec" ON "public"."act_ru_task" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_task_procdef" ON "public"."act_ru_task" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_task_procinst" ON "public"."act_ru_task" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_task_scope" ON "public"."act_ru_task" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_task_scope_def" ON "public"."act_ru_task" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_task_sub_scope" ON "public"."act_ru_task" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_task
-- ----------------------------
ALTER TABLE "public"."act_ru_task" ADD CONSTRAINT "act_ru_task_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_timer_job
-- ----------------------------
CREATE INDEX "act_idx_timer_job_correlation_id" ON "public"."act_ru_timer_job" USING btree (
  "correlation_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_timer_job_custom_values_id" ON "public"."act_ru_timer_job" USING btree (
  "custom_values_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_timer_job_duedate" ON "public"."act_ru_timer_job" USING btree (
  "duedate_" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_timer_job_exception_stack_id" ON "public"."act_ru_timer_job" USING btree (
  "exception_stack_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_timer_job_execution_id" ON "public"."act_ru_timer_job" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_timer_job_proc_def_id" ON "public"."act_ru_timer_job" USING btree (
  "proc_def_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_timer_job_process_instance_id" ON "public"."act_ru_timer_job" USING btree (
  "process_instance_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_tjob_scope" ON "public"."act_ru_timer_job" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_tjob_scope_def" ON "public"."act_ru_timer_job" USING btree (
  "scope_definition_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_tjob_sub_scope" ON "public"."act_ru_timer_job" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_timer_job
-- ----------------------------
ALTER TABLE "public"."act_ru_timer_job" ADD CONSTRAINT "act_ru_timer_job_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table act_ru_variable
-- ----------------------------
CREATE INDEX "act_idx_ru_var_scope_id_type" ON "public"."act_ru_variable" USING btree (
  "scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_ru_var_sub_id_type" ON "public"."act_ru_variable" USING btree (
  "sub_scope_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "scope_type_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_var_bytearray" ON "public"."act_ru_variable" USING btree (
  "bytearray_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_var_exe" ON "public"."act_ru_variable" USING btree (
  "execution_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_var_procinst" ON "public"."act_ru_variable" USING btree (
  "proc_inst_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "act_idx_variable_task_id" ON "public"."act_ru_variable" USING btree (
  "task_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table act_ru_variable
-- ----------------------------
ALTER TABLE "public"."act_ru_variable" ADD CONSTRAINT "act_ru_variable_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table flw_channel_definition
-- ----------------------------
CREATE UNIQUE INDEX "act_idx_channel_def_uniq" ON "public"."flw_channel_definition" USING btree (
  "key_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "version_" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "tenant_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table flw_channel_definition
-- ----------------------------
ALTER TABLE "public"."flw_channel_definition" ADD CONSTRAINT "FLW_CHANNEL_DEFINITION_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table flw_ev_databasechangeloglock
-- ----------------------------
ALTER TABLE "public"."flw_ev_databasechangeloglock" ADD CONSTRAINT "flw_ev_databasechangeloglock_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table flw_event_definition
-- ----------------------------
CREATE UNIQUE INDEX "act_idx_event_def_uniq" ON "public"."flw_event_definition" USING btree (
  "key_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "version_" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "tenant_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table flw_event_definition
-- ----------------------------
ALTER TABLE "public"."flw_event_definition" ADD CONSTRAINT "FLW_EVENT_DEFINITION_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table flw_event_deployment
-- ----------------------------
ALTER TABLE "public"."flw_event_deployment" ADD CONSTRAINT "FLW_EVENT_DEPLOYMENT_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table flw_event_resource
-- ----------------------------
ALTER TABLE "public"."flw_event_resource" ADD CONSTRAINT "FLW_EVENT_RESOURCE_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Primary Key structure for table flw_ru_batch
-- ----------------------------
ALTER TABLE "public"."flw_ru_batch" ADD CONSTRAINT "flw_ru_batch_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table flw_ru_batch_part
-- ----------------------------
CREATE INDEX "flw_idx_batch_part" ON "public"."flw_ru_batch_part" USING btree (
  "batch_id_" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table flw_ru_batch_part
-- ----------------------------
ALTER TABLE "public"."flw_ru_batch_part" ADD CONSTRAINT "flw_ru_batch_part_pkey" PRIMARY KEY ("id_");

-- ----------------------------
-- Indexes structure for table infra_api_access_log
-- ----------------------------
CREATE INDEX "idx_infra_api_access_log_01" ON "public"."infra_api_access_log" USING btree (
  "create_time" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table infra_api_access_log
-- ----------------------------
ALTER TABLE "public"."infra_api_access_log" ADD CONSTRAINT "pk_infra_api_access_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_api_error_log
-- ----------------------------
ALTER TABLE "public"."infra_api_error_log" ADD CONSTRAINT "pk_infra_api_error_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_codegen_column
-- ----------------------------
ALTER TABLE "public"."infra_codegen_column" ADD CONSTRAINT "pk_infra_codegen_column" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_codegen_table
-- ----------------------------
ALTER TABLE "public"."infra_codegen_table" ADD CONSTRAINT "pk_infra_codegen_table" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_config
-- ----------------------------
ALTER TABLE "public"."infra_config" ADD CONSTRAINT "pk_infra_config" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_data_source_config
-- ----------------------------
ALTER TABLE "public"."infra_data_source_config" ADD CONSTRAINT "pk_infra_data_source_config" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_file
-- ----------------------------
ALTER TABLE "public"."infra_file" ADD CONSTRAINT "pk_infra_file" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_file_config
-- ----------------------------
ALTER TABLE "public"."infra_file_config" ADD CONSTRAINT "pk_infra_file_config" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_file_content
-- ----------------------------
ALTER TABLE "public"."infra_file_content" ADD CONSTRAINT "pk_infra_file_content" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_job
-- ----------------------------
ALTER TABLE "public"."infra_job" ADD CONSTRAINT "pk_infra_job" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table infra_job_log
-- ----------------------------
ALTER TABLE "public"."infra_job_log" ADD CONSTRAINT "pk_infra_job_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table qrtz_blob_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_blob_triggers" ADD CONSTRAINT "qrtz_blob_triggers_pkey" PRIMARY KEY ("sched_name", "trigger_name", "trigger_group");

-- ----------------------------
-- Primary Key structure for table qrtz_calendars
-- ----------------------------
ALTER TABLE "public"."qrtz_calendars" ADD CONSTRAINT "qrtz_calendars_pkey" PRIMARY KEY ("sched_name", "calendar_name");

-- ----------------------------
-- Primary Key structure for table qrtz_cron_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_cron_triggers" ADD CONSTRAINT "qrtz_cron_triggers_pkey" PRIMARY KEY ("sched_name", "trigger_name", "trigger_group");

-- ----------------------------
-- Indexes structure for table qrtz_fired_triggers
-- ----------------------------
CREATE INDEX "idx_qrtz_ft_job_group" ON "public"."qrtz_fired_triggers" USING btree (
  "job_group" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_ft_job_name" ON "public"."qrtz_fired_triggers" USING btree (
  "job_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_ft_job_req_recovery" ON "public"."qrtz_fired_triggers" USING btree (
  "requests_recovery" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_ft_trig_group" ON "public"."qrtz_fired_triggers" USING btree (
  "trigger_group" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_ft_trig_inst_name" ON "public"."qrtz_fired_triggers" USING btree (
  "instance_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_ft_trig_name" ON "public"."qrtz_fired_triggers" USING btree (
  "trigger_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_ft_trig_nm_gp" ON "public"."qrtz_fired_triggers" USING btree (
  "sched_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "trigger_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "trigger_group" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table qrtz_fired_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_fired_triggers" ADD CONSTRAINT "qrtz_fired_triggers_pkey" PRIMARY KEY ("sched_name", "entry_id");

-- ----------------------------
-- Indexes structure for table qrtz_job_details
-- ----------------------------
CREATE INDEX "idx_qrtz_j_req_recovery" ON "public"."qrtz_job_details" USING btree (
  "requests_recovery" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table qrtz_job_details
-- ----------------------------
ALTER TABLE "public"."qrtz_job_details" ADD CONSTRAINT "qrtz_job_details_pkey" PRIMARY KEY ("sched_name", "job_name", "job_group");

-- ----------------------------
-- Primary Key structure for table qrtz_locks
-- ----------------------------
ALTER TABLE "public"."qrtz_locks" ADD CONSTRAINT "qrtz_locks_pkey" PRIMARY KEY ("sched_name", "lock_name");

-- ----------------------------
-- Primary Key structure for table qrtz_paused_trigger_grps
-- ----------------------------
ALTER TABLE "public"."qrtz_paused_trigger_grps" ADD CONSTRAINT "qrtz_paused_trigger_grps_pkey" PRIMARY KEY ("sched_name", "trigger_group");

-- ----------------------------
-- Primary Key structure for table qrtz_scheduler_state
-- ----------------------------
ALTER TABLE "public"."qrtz_scheduler_state" ADD CONSTRAINT "qrtz_scheduler_state_pkey" PRIMARY KEY ("sched_name", "instance_name");

-- ----------------------------
-- Primary Key structure for table qrtz_simple_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_simple_triggers" ADD CONSTRAINT "qrtz_simple_triggers_pkey" PRIMARY KEY ("sched_name", "trigger_name", "trigger_group");

-- ----------------------------
-- Primary Key structure for table qrtz_simprop_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_simprop_triggers" ADD CONSTRAINT "qrtz_simprop_triggers_pkey" PRIMARY KEY ("sched_name", "trigger_name", "trigger_group");

-- ----------------------------
-- Indexes structure for table qrtz_triggers
-- ----------------------------
CREATE INDEX "idx_qrtz_t_next_fire_time" ON "public"."qrtz_triggers" USING btree (
  "next_fire_time" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_t_nft_st" ON "public"."qrtz_triggers" USING btree (
  "next_fire_time" "pg_catalog"."int8_ops" ASC NULLS LAST,
  "trigger_state" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_qrtz_t_state" ON "public"."qrtz_triggers" USING btree (
  "trigger_state" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table qrtz_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_triggers" ADD CONSTRAINT "qrtz_triggers_pkey" PRIMARY KEY ("sched_name", "trigger_name", "trigger_group");

-- ----------------------------
-- Primary Key structure for table system_dept
-- ----------------------------
ALTER TABLE "public"."system_dept" ADD CONSTRAINT "pk_system_dept" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_dict_data
-- ----------------------------
ALTER TABLE "public"."system_dict_data" ADD CONSTRAINT "pk_system_dict_data" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_dict_type
-- ----------------------------
ALTER TABLE "public"."system_dict_type" ADD CONSTRAINT "pk_system_dict_type" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_login_log
-- ----------------------------
ALTER TABLE "public"."system_login_log" ADD CONSTRAINT "pk_system_login_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_mail_account
-- ----------------------------
ALTER TABLE "public"."system_mail_account" ADD CONSTRAINT "pk_system_mail_account" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_mail_log
-- ----------------------------
ALTER TABLE "public"."system_mail_log" ADD CONSTRAINT "pk_system_mail_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_mail_template
-- ----------------------------
ALTER TABLE "public"."system_mail_template" ADD CONSTRAINT "pk_system_mail_template" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_menu
-- ----------------------------
ALTER TABLE "public"."system_menu" ADD CONSTRAINT "pk_system_menu" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_notice
-- ----------------------------
ALTER TABLE "public"."system_notice" ADD CONSTRAINT "pk_system_notice" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_notify_message
-- ----------------------------
ALTER TABLE "public"."system_notify_message" ADD CONSTRAINT "pk_system_notify_message" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_notify_template
-- ----------------------------
ALTER TABLE "public"."system_notify_template" ADD CONSTRAINT "pk_system_notify_template" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table system_oauth2_access_token
-- ----------------------------
CREATE INDEX "idx_system_oauth2_access_token_01" ON "public"."system_oauth2_access_token" USING btree (
  "access_token" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_system_oauth2_access_token_02" ON "public"."system_oauth2_access_token" USING btree (
  "refresh_token" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table system_oauth2_access_token
-- ----------------------------
ALTER TABLE "public"."system_oauth2_access_token" ADD CONSTRAINT "pk_system_oauth2_access_token" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_oauth2_approve
-- ----------------------------
ALTER TABLE "public"."system_oauth2_approve" ADD CONSTRAINT "pk_system_oauth2_approve" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_oauth2_client
-- ----------------------------
ALTER TABLE "public"."system_oauth2_client" ADD CONSTRAINT "pk_system_oauth2_client" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_oauth2_code
-- ----------------------------
ALTER TABLE "public"."system_oauth2_code" ADD CONSTRAINT "pk_system_oauth2_code" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_oauth2_refresh_token
-- ----------------------------
ALTER TABLE "public"."system_oauth2_refresh_token" ADD CONSTRAINT "pk_system_oauth2_refresh_token" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_operate_log
-- ----------------------------
ALTER TABLE "public"."system_operate_log" ADD CONSTRAINT "pk_system_operate_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_post
-- ----------------------------
ALTER TABLE "public"."system_post" ADD CONSTRAINT "pk_system_post" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_role
-- ----------------------------
ALTER TABLE "public"."system_role" ADD CONSTRAINT "pk_system_role" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_role_menu
-- ----------------------------
ALTER TABLE "public"."system_role_menu" ADD CONSTRAINT "pk_system_role_menu" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_sms_channel
-- ----------------------------
ALTER TABLE "public"."system_sms_channel" ADD CONSTRAINT "pk_system_sms_channel" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table system_sms_code
-- ----------------------------
CREATE INDEX "idx_system_sms_code_01" ON "public"."system_sms_code" USING btree (
  "mobile" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table system_sms_code
-- ----------------------------
ALTER TABLE "public"."system_sms_code" ADD CONSTRAINT "pk_system_sms_code" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_sms_log
-- ----------------------------
ALTER TABLE "public"."system_sms_log" ADD CONSTRAINT "pk_system_sms_log" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_sms_template
-- ----------------------------
ALTER TABLE "public"."system_sms_template" ADD CONSTRAINT "pk_system_sms_template" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_social_client
-- ----------------------------
ALTER TABLE "public"."system_social_client" ADD CONSTRAINT "pk_system_social_client" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_social_user
-- ----------------------------
ALTER TABLE "public"."system_social_user" ADD CONSTRAINT "pk_system_social_user" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_social_user_bind
-- ----------------------------
ALTER TABLE "public"."system_social_user_bind" ADD CONSTRAINT "pk_system_social_user_bind" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_tenant
-- ----------------------------
ALTER TABLE "public"."system_tenant" ADD CONSTRAINT "pk_system_tenant" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_tenant_package
-- ----------------------------
ALTER TABLE "public"."system_tenant_package" ADD CONSTRAINT "pk_system_tenant_package" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_user_post
-- ----------------------------
ALTER TABLE "public"."system_user_post" ADD CONSTRAINT "pk_system_user_post" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_user_role
-- ----------------------------
ALTER TABLE "public"."system_user_role" ADD CONSTRAINT "pk_system_user_role" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table system_users
-- ----------------------------
ALTER TABLE "public"."system_users" ADD CONSTRAINT "pk_system_users" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table yudao_demo01_contact
-- ----------------------------
ALTER TABLE "public"."yudao_demo01_contact" ADD CONSTRAINT "pk_yudao_demo01_contact" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table yudao_demo02_category
-- ----------------------------
ALTER TABLE "public"."yudao_demo02_category" ADD CONSTRAINT "pk_yudao_demo02_category" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table yudao_demo03_course
-- ----------------------------
ALTER TABLE "public"."yudao_demo03_course" ADD CONSTRAINT "pk_yudao_demo03_course" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table yudao_demo03_grade
-- ----------------------------
ALTER TABLE "public"."yudao_demo03_grade" ADD CONSTRAINT "pk_yudao_demo03_grade" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table yudao_demo03_student
-- ----------------------------
ALTER TABLE "public"."yudao_demo03_student" ADD CONSTRAINT "pk_yudao_demo03_student" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table act_ge_bytearray
-- ----------------------------
ALTER TABLE "public"."act_ge_bytearray" ADD CONSTRAINT "act_fk_bytearr_depl" FOREIGN KEY ("deployment_id_") REFERENCES "public"."act_re_deployment" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_id_membership
-- ----------------------------
ALTER TABLE "public"."act_id_membership" ADD CONSTRAINT "act_fk_memb_group" FOREIGN KEY ("group_id_") REFERENCES "public"."act_id_group" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_id_membership" ADD CONSTRAINT "act_fk_memb_user" FOREIGN KEY ("user_id_") REFERENCES "public"."act_id_user" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_id_priv_mapping
-- ----------------------------
ALTER TABLE "public"."act_id_priv_mapping" ADD CONSTRAINT "act_fk_priv_mapping" FOREIGN KEY ("priv_id_") REFERENCES "public"."act_id_priv" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_procdef_info
-- ----------------------------
ALTER TABLE "public"."act_procdef_info" ADD CONSTRAINT "act_fk_info_json_ba" FOREIGN KEY ("info_json_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_procdef_info" ADD CONSTRAINT "act_fk_info_procdef" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_re_model
-- ----------------------------
ALTER TABLE "public"."act_re_model" ADD CONSTRAINT "act_fk_model_deployment" FOREIGN KEY ("deployment_id_") REFERENCES "public"."act_re_deployment" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_re_model" ADD CONSTRAINT "act_fk_model_source" FOREIGN KEY ("editor_source_value_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_re_model" ADD CONSTRAINT "act_fk_model_source_extra" FOREIGN KEY ("editor_source_extra_value_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_deadletter_job
-- ----------------------------
ALTER TABLE "public"."act_ru_deadletter_job" ADD CONSTRAINT "act_fk_deadletter_job_custom_values" FOREIGN KEY ("custom_values_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_deadletter_job" ADD CONSTRAINT "act_fk_deadletter_job_exception" FOREIGN KEY ("exception_stack_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_deadletter_job" ADD CONSTRAINT "act_fk_deadletter_job_execution" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_deadletter_job" ADD CONSTRAINT "act_fk_deadletter_job_proc_def" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_deadletter_job" ADD CONSTRAINT "act_fk_deadletter_job_process_instance" FOREIGN KEY ("process_instance_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_event_subscr
-- ----------------------------
ALTER TABLE "public"."act_ru_event_subscr" ADD CONSTRAINT "act_fk_event_exec" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_execution
-- ----------------------------
ALTER TABLE "public"."act_ru_execution" ADD CONSTRAINT "act_fk_exe_parent" FOREIGN KEY ("parent_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_execution" ADD CONSTRAINT "act_fk_exe_procdef" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_execution" ADD CONSTRAINT "act_fk_exe_procinst" FOREIGN KEY ("proc_inst_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_execution" ADD CONSTRAINT "act_fk_exe_super" FOREIGN KEY ("super_exec_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_external_job
-- ----------------------------
ALTER TABLE "public"."act_ru_external_job" ADD CONSTRAINT "act_fk_external_job_custom_values" FOREIGN KEY ("custom_values_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_external_job" ADD CONSTRAINT "act_fk_external_job_exception" FOREIGN KEY ("exception_stack_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_identitylink
-- ----------------------------
ALTER TABLE "public"."act_ru_identitylink" ADD CONSTRAINT "act_fk_athrz_procedef" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_identitylink" ADD CONSTRAINT "act_fk_idl_procinst" FOREIGN KEY ("proc_inst_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_identitylink" ADD CONSTRAINT "act_fk_tskass_task" FOREIGN KEY ("task_id_") REFERENCES "public"."act_ru_task" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_job
-- ----------------------------
ALTER TABLE "public"."act_ru_job" ADD CONSTRAINT "act_fk_job_custom_values" FOREIGN KEY ("custom_values_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_job" ADD CONSTRAINT "act_fk_job_exception" FOREIGN KEY ("exception_stack_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_job" ADD CONSTRAINT "act_fk_job_execution" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_job" ADD CONSTRAINT "act_fk_job_proc_def" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_job" ADD CONSTRAINT "act_fk_job_process_instance" FOREIGN KEY ("process_instance_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_suspended_job
-- ----------------------------
ALTER TABLE "public"."act_ru_suspended_job" ADD CONSTRAINT "act_fk_suspended_job_custom_values" FOREIGN KEY ("custom_values_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_suspended_job" ADD CONSTRAINT "act_fk_suspended_job_exception" FOREIGN KEY ("exception_stack_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_suspended_job" ADD CONSTRAINT "act_fk_suspended_job_execution" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_suspended_job" ADD CONSTRAINT "act_fk_suspended_job_proc_def" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_suspended_job" ADD CONSTRAINT "act_fk_suspended_job_process_instance" FOREIGN KEY ("process_instance_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_task
-- ----------------------------
ALTER TABLE "public"."act_ru_task" ADD CONSTRAINT "act_fk_task_exe" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_task" ADD CONSTRAINT "act_fk_task_procdef" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_task" ADD CONSTRAINT "act_fk_task_procinst" FOREIGN KEY ("proc_inst_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_timer_job
-- ----------------------------
ALTER TABLE "public"."act_ru_timer_job" ADD CONSTRAINT "act_fk_timer_job_custom_values" FOREIGN KEY ("custom_values_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_timer_job" ADD CONSTRAINT "act_fk_timer_job_exception" FOREIGN KEY ("exception_stack_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_timer_job" ADD CONSTRAINT "act_fk_timer_job_execution" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_timer_job" ADD CONSTRAINT "act_fk_timer_job_proc_def" FOREIGN KEY ("proc_def_id_") REFERENCES "public"."act_re_procdef" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_timer_job" ADD CONSTRAINT "act_fk_timer_job_process_instance" FOREIGN KEY ("process_instance_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table act_ru_variable
-- ----------------------------
ALTER TABLE "public"."act_ru_variable" ADD CONSTRAINT "act_fk_var_bytearray" FOREIGN KEY ("bytearray_id_") REFERENCES "public"."act_ge_bytearray" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_variable" ADD CONSTRAINT "act_fk_var_exe" FOREIGN KEY ("execution_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "public"."act_ru_variable" ADD CONSTRAINT "act_fk_var_procinst" FOREIGN KEY ("proc_inst_id_") REFERENCES "public"."act_ru_execution" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table flw_ru_batch_part
-- ----------------------------
ALTER TABLE "public"."flw_ru_batch_part" ADD CONSTRAINT "flw_fk_batch_part_parent" FOREIGN KEY ("batch_id_") REFERENCES "public"."flw_ru_batch" ("id_") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table qrtz_blob_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_blob_triggers" ADD CONSTRAINT "qrtz_blob_triggers_sched_name_trigger_name_trigger_group_fkey" FOREIGN KEY ("sched_name", "trigger_name", "trigger_group") REFERENCES "public"."qrtz_triggers" ("sched_name", "trigger_name", "trigger_group") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table qrtz_cron_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_cron_triggers" ADD CONSTRAINT "qrtz_cron_triggers_sched_name_trigger_name_trigger_group_fkey" FOREIGN KEY ("sched_name", "trigger_name", "trigger_group") REFERENCES "public"."qrtz_triggers" ("sched_name", "trigger_name", "trigger_group") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table qrtz_simple_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_simple_triggers" ADD CONSTRAINT "qrtz_simple_triggers_sched_name_trigger_name_trigger_group_fkey" FOREIGN KEY ("sched_name", "trigger_name", "trigger_group") REFERENCES "public"."qrtz_triggers" ("sched_name", "trigger_name", "trigger_group") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table qrtz_simprop_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_simprop_triggers" ADD CONSTRAINT "qrtz_simprop_triggers_sched_name_trigger_name_trigger_grou_fkey" FOREIGN KEY ("sched_name", "trigger_name", "trigger_group") REFERENCES "public"."qrtz_triggers" ("sched_name", "trigger_name", "trigger_group") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table qrtz_triggers
-- ----------------------------
ALTER TABLE "public"."qrtz_triggers" ADD CONSTRAINT "qrtz_triggers_sched_name_job_name_job_group_fkey" FOREIGN KEY ("sched_name", "job_name", "job_group") REFERENCES "public"."qrtz_job_details" ("sched_name", "job_name", "job_group") ON DELETE NO ACTION ON UPDATE NO ACTION;
