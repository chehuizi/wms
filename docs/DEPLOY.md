# WMS 部署文档

线上地址：**http://101.37.146.194/wms/**

> 挂 `/wms/` 子路径而不是根路径，是因为裸 IP 的 80 端口已被 beeOS 看板占用
> （`/etc/nginx/conf.d/beeos.conf` 的 `server_name 101.37.146.194`）。
> WMS 的 location 是**追加**进去的，没有改动 beeOS 任何原有路由。

## 服务器现状

| 项 | 值 |
|---|---|
| 主机 | `root@101.37.146.194`（Alibaba Cloud Linux 3） |
| 内存 | **1.8 GB，且已有多套服务在跑** —— 这是本部署最主要的约束 |
| JDK | `/opt/jdk21`（Temurin 21.0.12，源内只有 JDK 8，装不了 Spring Boot 3） |
| 数据库 | 复用已有的 PostgreSQL **11.15**，新建独立库 `wms` + 独立角色 `wms` |
| 前端 | nginx 直接发静态文件，落在 `/opt/wms/public/wms/` |
| 后端 | systemd `wms.service`，仅监听 `127.0.0.1:8080` |

**同机的其它服务（本部署不得影响）**：`beeos-kanban`(:8765)、`domainbox-console`(:4002)、redis(:6379)、`thebestpractice4ai.com`。

## 目录

```
/opt/wms/app/wms-server.jar        后端 jar
/opt/wms/public/wms/               前端产物（构建时 VITE_BASE=/wms/）
/etc/wms/wms.env                   环境变量含数据库密码，权限 600 root:root
/var/log/wms/{stdout,stderr}.log   日志
/etc/systemd/system/wms.service    服务定义
```

## 常用运维命令

```bash
systemctl status wms              # 状态
systemctl restart wms             # 重启
journalctl -u wms -f             # 实时日志
tail -f /var/log/wms/stdout.log   # 应用日志（含 SQL）

curl -s http://127.0.0.1:8080/api/v1/health   # 健康检查
```

## 更新发布

**后端**（本机构建，服务器上没有 Maven）：

```bash
# 本机
cd server && mvn clean package
scp server/target/wms-server-0.1.0.jar root@101.37.146.194:/tmp/wms-server.jar
# 服务器
systemctl stop wms
mv /tmp/wms-server.jar /opt/wms/app/wms-server.jar
systemctl start wms
```

**前端**（注意 `VITE_BASE`，漏了会渲染成 404）：

```bash
cd web && VITE_BASE=/wms/ npm run build
cd dist && tar czf /tmp/wms-web.tar.gz . && \
  scp /tmp/wms-web.tar.gz root@101.37.146.194:/tmp/wms-web.tar.gz
# 服务器
cd /opt/wms/public && rm -rf wms && mkdir wms && \
  tar -xzf /tmp/wms-web.tar.gz -C wms && chmod -R a+rX wms
nginx -t && systemctl reload nginx
```

## 内存保护（重要）

这台机器只有 1.8 GB，且同时跑着其他项目。JVM 如果失控，**内核 OOM killer 可能杀掉 beeOS 等其它服务**。

所以 unit 里做了硬限制：

```
JAVA_OPTS  -Xms96m -Xmx320m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC
MemoryMax=560M      # cgroup 上限
MemoryHigh=480M
```

即使 WMS 内存打满，被限制的也只会是 WMS 自己，不会外溢影响别的服务。
实测稳态 RSS 约 **207 MB**。

监控：

```bash
systemctl show wms -p MemoryMax      # 应为 MemoryMax=587202560
ps -o rss=,cmd= -C java              # 看实际占用
free -m
```

若后续还要在这台机器上加服务，**先算内存余量**，不要盲目加。

## 数据库注意事项

⚠️ **线上是 PostgreSQL 11，本机开发是 PostgreSQL 16 —— SQL 必须同时兼容两边。**

已经踩过一次：`UNIQUE NULLS NOT DISTINCT` 是 PG 15+ 语法，在 PG 11 上直接报错。
现已改用表达式索引，两边行为一致：

```sql
CREATE UNIQUE INDEX uk_inventory_slot
    ON wms_inventory (location_id, sku_id, COALESCE(batch_no,''), COALESCE(serial_no,''));
```

**改表结构前，先在 PG 11 上验一遍**，不要只在本机 PG 16 验：

```bash
# 传脚本到服务器临时库跑，跑完即删
psql -h 127.0.0.1 -d postgres -c "CREATE DATABASE wms_scratch;"
psql -h 127.0.0.1 -d wms_scratch -v ON_ERROR_STOP=1 -f <脚本>
psql -h 127.0.0.1 -d postgres -c "DROP DATABASE wms_scratch;"
```

重置数据（会清空全部业务数据）：

```bash
systemctl stop wms
psql -c "DROP DATABASE IF EXISTS wms;" -c "CREATE DATABASE wms OWNER wms ENCODING 'UTF8';"
systemctl start wms     # Flyway 自动重建表并灌入种子数据
```

> 必须先 `systemctl stop wms`，否则 DROP DATABASE 会报
> `database is being accessed by other users`。

## nginx 配置要点

`/wms/api/` 那个块**刻意不开 `proxy_intercept_errors`**。接口该 404/409 就如实返回；
一旦被 `error_page` 兜底改写成 302 + HTML，前端会把 HTML 当 JSON 解析，
报出「Unexpected token `<`」这种指向 JS 的误导性错误，而真实原因是接口路径不存在。

`/wms/` 用 `root` 而不是 `alias`：`alias` 配 `try_files` 时 `$uri` 与磁盘路径的拼接有坑，
`root` 的规则是一一对应（`/wms/xxx` → `/opt/wms/public/wms/xxx`）。

改 nginx 后**务必**：`nginx -t` 通过再 `reload`，然后回归检查 beeOS：

```bash
curl -o /dev/null -w "%{http_code}\n" http://101.37.146.194/          # 应为 200
curl -o /dev/null -w "%{http_code}\n" http://101.37.146.194/api/boxes # 应为 200
```

## ⚠️ 当前已知风险

**系统没有任何认证鉴权。** 任何能访问该地址的人都可以：

- `POST /wms/api/v1/inventories/adjust` 直接改库存
- 调用盘点审批接口
- 增删改主数据

上生产前必须加登录 + 角色权限。另外建议在阿里云安全组把 80 端口的来源
IP 限制为办公网段，从网络层面先挡一道。
