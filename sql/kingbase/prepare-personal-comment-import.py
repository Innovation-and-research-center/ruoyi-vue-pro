#!/usr/bin/env python3
"""Convert an Oracle T_OA_IDEA CSV to a Kingbase import (user IDs unchanged).
Generated files contain source data: keep them outside the Git repository.
"""
import argparse
import collections
import csv
import hashlib
import io
import json
from pathlib import Path

COLUMNS = ('OA_IDEA_ID', 'USER_ID', 'IDEA_MESSAGE', 'IDEA_TYPE', 'IDEA_ORDER')


def load_csv(path):
    raw = Path(path).read_bytes()
    candidates = ['utf-16'] if raw.startswith((b'\xff\xfe', b'\xfe\xff')) else ['utf-8-sig', 'gb18030']
    for encoding in candidates:
        try:
            text = raw.decode(encoding)
            break
        except UnicodeDecodeError:
            continue
    else:
        raise ValueError('CSV编码无法识别，请导出UTF-8版本')
    reader = csv.DictReader(io.StringIO(text, newline=''))
    names = reader.fieldnames or []
    named = [name.strip().upper() for name in names if name.strip()]
    if len(named) != len(set(named)) or set(named) != set(COLUMNS):
        raise ValueError('CSV表头必须包含T_OA_IDEA的五个原始字段')
    header = {name.strip().upper(): name for name in names if name.strip()}
    rows, ids = [], set()
    for line, record in enumerate(reader, 2):
        if None in record or any(value is None for value in record.values()):
            raise ValueError(f'CSV第{line}条记录的列数不正确')
        row = {name: record[header[name]] for name in COLUMNS}
        try:
            for name in ('OA_IDEA_ID', 'USER_ID'):
                row[name] = int(row[name])
            row['IDEA_ORDER'] = int(row['IDEA_ORDER']) if row['IDEA_ORDER'].strip() else 0
        except ValueError:
            raise ValueError(f'CSV第{line}条记录的ID或排序不是整数') from None
        if any(not 0 < row[name] <= 9223372036854775807 for name in ('OA_IDEA_ID', 'USER_ID')):
            raise ValueError(f'CSV第{line}条记录的ID超出范围')
        if not 0 <= row['IDEA_ORDER'] <= 99999:
            raise ValueError(f'CSV第{line}条记录的排序超出旧表范围')
        if not row['IDEA_MESSAGE'].strip() or len(row['IDEA_MESSAGE']) > 150:
            raise ValueError(f'CSV第{line}条记录的意见为空或超过150字')
        if len(row['IDEA_TYPE']) > 150 or any('\x00' in row[name] for name in ('IDEA_MESSAGE', 'IDEA_TYPE')):
            raise ValueError(f'CSV第{line}条记录的字符串格式不合法')
        if row['OA_IDEA_ID'] in ids:
            raise ValueError(f'CSV第{line}条记录存在重复模板ID')
        ids.add(row['OA_IDEA_ID'])
        rows.append(row)
    if not rows:
        raise ValueError('CSV没有模板记录')
    return rows, encoding, hashlib.sha256(raw).hexdigest()


def literal(value):
    return "E'" + value.replace('\\', '\\\\').replace("'", "''") + "'"


def generate_sql(rows, checksum):
    schema = 'comment_template_idea_' + checksum[:12]
    values = []
    for row in rows:
        values.append('(%d,%d,%s,%s,%d)' % (
            row['OA_IDEA_ID'], row['USER_ID'], literal(row['IDEA_MESSAGE']),
            literal(row['IDEA_TYPE']), row['IDEA_ORDER']))
    return f'''-- Oracle T_OA_IDEA -> KingbaseES（项目现有PG兼容写法）。
-- 用户已确认旧、新用户ID一致；租户从新系统system_users读取，不猜测。
-- 执行前先运行comment-template.sql；导入期间暂停个人模板新增/编辑。
-- 本文件含导出的模板数据，请勿提交Git。
-- 源CSV SHA256: {checksum}
-- 严格一次性导入：缺失用户、缺失租户或模板ID冲突会回滚，不覆盖已有模板。
BEGIN;
DO $$ BEGIN
    IF to_regclass('public.bpm_personal_comment_template') IS NULL
       OR to_regclass('public.bpm_personal_comment_template_seq') IS NULL
       OR to_regclass('public.system_users') IS NULL THEN
        RAISE EXCEPTION '目标表或序列不存在，请先执行Kingbase建表脚本';
    END IF;
END $$;
CREATE SCHEMA IF NOT EXISTS {schema};
CREATE TABLE {schema}.t_oa_idea (
    oa_idea_id bigint PRIMARY KEY, user_id bigint NOT NULL,
    idea_message varchar(150) NOT NULL, idea_type varchar(150), idea_order integer NOT NULL
);
INSERT INTO {schema}.t_oa_idea(oa_idea_id,user_id,idea_message,idea_type,idea_order) VALUES
''' + ',\n'.join(values) + f''';
LOCK TABLE public.bpm_personal_comment_template IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM {schema}.t_oa_idea s
        LEFT JOIN public.system_users u ON u.id=s.user_id AND COALESCE(u.deleted,0)=0
        WHERE u.id IS NULL OR u.tenant_id IS NULL
    ) THEN
        RAISE EXCEPTION '存在未找到的新系统用户或用户缺失租户，停止导入';
    END IF;
    IF EXISTS (
        SELECT 1 FROM {schema}.t_oa_idea s
        JOIN public.bpm_personal_comment_template t ON t.oa_idea_id=s.oa_idea_id
    ) THEN
        RAISE EXCEPTION '模板ID与目标表已有记录冲突，停止导入';
    END IF;
END $$;
INSERT INTO public.bpm_personal_comment_template
    (oa_idea_id,user_id,idea_message,idea_type,idea_order,tenant_id,creator,updater)
SELECT s.oa_idea_id,s.user_id,s.idea_message,s.idea_type,s.idea_order,u.tenant_id,'legacy-import','legacy-import'
FROM {schema}.t_oa_idea s
JOIN public.system_users u ON u.id=s.user_id AND COALESCE(u.deleted,0)=0;
SELECT setval('public.bpm_personal_comment_template_seq',
    GREATEST((SELECT CAST(last_value AS bigint) FROM public.bpm_personal_comment_template_seq),
             COALESCE((SELECT max(oa_idea_id) FROM public.bpm_personal_comment_template),1)), true);
COMMIT;
-- 保留本批暂存表以便核对；不操作业务动态模板表。
SELECT count(*) AS imported_count FROM {schema}.t_oa_idea;
'''


def prepare(source, output):
    rows, encoding, checksum = load_csv(source)
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    sql = output / 'idea-import.sql'
    if sql.exists():
        raise ValueError('输出目录已有idea-import.sql，请使用新的输出目录')
    sql.write_text(generate_sql(rows, checksum), encoding='utf-8')
    sql.chmod(0o600)
    summary = {'encoding': encoding, 'rows': len(rows),
               'unique_users': len({row['USER_ID'] for row in rows}),
               'type_counts': dict(collections.Counter(row['IDEA_TYPE'] for row in rows)),
               'sha256': checksum, 'import_sql': str(sql)}
    (output / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
    return summary


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(prepare(args.source, args.out), ensure_ascii=False))
    except ValueError as exc:
        parser.exit(1, str(exc) + '\n')
