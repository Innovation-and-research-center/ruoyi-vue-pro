# 收文 Job 列表接口测试

在服务器的 Bash 或 zsh 终端执行。先把下面的占位值替换成该环境中的实际配置；地址末尾不要带 `/`。在本地测试时，设置本地要访问的地址和相同的接口参数，再执行同一条 `curl` 命令。以下请求只查询列表，不触发 Job，也不调用签收接口。

## 1. CityDocJob：市局公文列表

配置项：`url.receive.city`、`key.receive.natural`。

```bash
CITY_URL='这里填 url.receive.city'
CITY_RECEIVER_UUID='这里填 key.receive.natural'
curl -sS -i --connect-timeout 5 --max-time 30 -G "$CITY_URL/oa/api/public/service/showCoreExchgappData.do" --data-urlencode "ceadReceiverUuid=$CITY_RECEIVER_UUID" --data-urlencode 'ceadState=0'
```

## 2. CityNoticeJob：市局公告列表

配置项：`url.receive.notice`、`key.receive.notice.uuid`。

```bash
NOTICE_URL='这里填 url.receive.notice'
NOTICE_USER_UUID='这里填 key.receive.notice.uuid'
curl -sS -i --connect-timeout 5 --max-time 30 -G "$NOTICE_URL/public/oaNotice/getPendingList.do" --data-urlencode "strMap.userUuid=$NOTICE_USER_UUID" --data-urlencode 'page=1' --data-urlencode 'limit=20' --data-urlencode 'start=0'
```

## 3. StDocJob：省厅待签收列表

配置项：`url.st.service`、`key.st.unit.id`。`sign` 按 Job 中的算法，由单位 ID 拼接固定字符串后计算 MD5。

```bash
ST_URL='这里填 url.st.service'
ST_UNIT_ID='这里填 key.st.unit.id'
ST_SIGN=$(printf '%s' "${ST_UNIT_ID}zrzytoa" | openssl dgst -md5 -r | awk '{print $1}')
curl -sS -i --connect-timeout 5 --max-time 30 -H 'Content-Type: application/json' -d "{\"id\":\"$ST_UNIT_ID\",\"sign\":\"$ST_SIGN\",\"page\":1,\"limit\":10000}" "$ST_URL/api6/infoexchange-table/DQSList"
```

## 4. ForestryDocJob：林业局收文列表

配置项：`url.receive.forestry`、`key.forestry.loginname`、`key.forestry.password`。密码与地址一样填入对应变量，填写配置项的原始值；命令会按 Job 逻辑计算一次 MD5。日期与 Job 一样取执行当天的 `yyyy-MM-dd`。

```bash
FORESTRY_URL='这里填 url.receive.forestry'
FORESTRY_LOGIN='这里填 key.forestry.loginname'
FORESTRY_PASSWORD='这里填 key.forestry.password'
FORESTRY_MD5=$(printf '%s' "$FORESTRY_PASSWORD" | openssl dgst -md5 -r | awk '{print $1}')
curl -sS -i --connect-timeout 5 --max-time 30 -G "$FORESTRY_URL/push/docsReader.do" --data-urlencode 'sysCmd=getReader' --data-urlencode "dataTime=$(date +%F)" --data-urlencode "loginName=$FORESTRY_LOGIN" --data-urlencode "passWord=$FORESTRY_MD5"
```

`-i` 会同时显示 HTTP 状态、响应头和响应体。复制结果用于本地对照时，遮盖其中的个人信息、Cookie 和其他敏感字段。
