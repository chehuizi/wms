# AGENTS.md

给后续在这个仓库里干活的 AI / 人类协作者的上下文。

## 这是什么

WMS 仓储管理系统，前后端同仓。仓库根目录是 `wms`，下面两个**独立**的工程：

- `server/` — Spring Boot 3.4 + MyBatis + PostgreSQL（Maven）
- `web/` — Vue 3 + Vite + Element Plus（npm）

两者不共享构建配置，各自 `build` / `run`。不要试图在根目录加一个统一的 workspace 配置。

## 动手前必读

**`docs/API_CONTRACT.md`** —— 基础主数据（仓库/库位/SKU/客户）的前后端事实来源。
**`docs/API_CONTRACT_BIZ.md`** —— 业务模块（库存/入库/出库/盘点/报表）的事实来源，
其中**第 10 节「库存核心不变式」是整个 WMS 的地基**，涉及库存的写操作都要逐条对照。
**`docs/DEPLOY.md`** —— 线上部署事实（主机、目录、服务名、更新步骤、内存约束）。

改接口必须先改对应契约文件，前后端都照它写。绕过契约直接改代码是本仓库最容易出问题的做法。

## 数据库版本不一致（最容易翻车的一条）

**本机开发是 PostgreSQL 16，线上服务器是 PostgreSQL 11.15。SQL 必须同时兼容两边。**

已踩过的坑：`UNIQUE NULLS NOT DISTINCT` 是 PG 15+ 语法，在线上直接报错。
已改用表达式索引（`COALESCE` 把 NULL 归一），两边行为一致。

**改表结构后，不要只在本机 PG 16 验过就完事**，必须到 PG 11 上再跑一遍：

```bash
# 传到服务器临时库跑，跑完即删
psql -h 127.0.0.1 -d postgres -c "CREATE DATABASE wms_scratch;"
psql -h 127.0.0.1 -d wms_scratch -v ON_ERROR_STOP=1 -f <脚本>
psql -h 127.0.0.1 -d postgres -c "DROP DATABASE wms_scratch;"
```

## 线上部署

线上跑在 `root@101.37.146.194`，入口 **http://101.37.146.194/wms/**（子路径，因为裸 IP 已被 beeOS 占用）。
同机还跑着 beeos-kanban / domainbox-console / thebestpractice4ai 等**其它项目，不得影响**。

这台机器只有 **1.8 GB 内存且无 swap**，WMS 被 systemd `MemoryMax=560M` 硬限制住，
这样即使 JVM 失控也只会影响自己，不会触发内核 OOM killer 杀掉别人的服务。
加新服务前先算内存余量。详见 `docs/DEPLOY.md`。

## 子路径部署的三个坑

前端通过 `VITE_BASE=/wms/` 构建以支持挂在子路径下。改这三处时务必一起改：

1. `vite.config.ts` 的 `base` —— 决定静态资源路径
2. `src/api/request.ts` 的 `baseURL` —— 用 `import.meta.env.BASE_URL` 拼
3. `src/router.ts` 的 `createWebHistory(...)` —— **必须传 base**

第 3 条漏掉的后果特别隐蔽：页面能正常加载（nginx 照样返回 index.html，HTTP 200），
但应用渲染出自己的 404 页。**HTTP 200 不等于能用。**

## Maven 镜像（本机网络限制，必读）

**这台机器连不上 Maven Central** —— 实测 SSL 握手被中断，直连返回失败。

项目里已经配好阿里云镜像，**只对 `server/` 生效**，不影响全局：

- `server/.mvn/settings.xml` — 阿里云镜像
- `server/.mvn/maven.config` — 让 `cd server && mvn` 自动带上它

`cd server && mvn` 开箱即用，**不要去改用户的全局 `~/.m2/settings.xml`**。
也不要用 `--settings` 参数去指向别的配置文件，绕开会破坏这个约定。

依赖已经缓存过一次，后续构建应该是快的。

### npm 同理：官方源也不通

`registry.npmjs.org` 同样连不上（实测 curl 超时）。`web/` 已配 `.npmrc` 指向
`https://registry.npmmirror.com`，所以 `cd web && npm install` 可以直接用。
**不要**去改用户的全局 `~/.npmrc`。

> 教训：本机对 Maven Central 和 npm 官方源都不可达。
> 遇到任何「依赖装不上 / 超时」，先想到换镜像，别去反复重试官方源。

## 本机环境（已验证，2026-10-17）

| 项 | 状态 |
|---|---|
| JDK | OpenJDK 21.0.5 (Corretto)，`java`/`mvn` 直接可用，无需设 `JAVA_HOME` |
| Maven | 3.9.11 |
| PostgreSQL | 16.14 (Homebrew)，**已在跑**，监听 `127.0.0.1:5432` |
| 数据库凭据 | 用户 `chehuizi`（superuser），**无密码**，库 `wms` |
| Node / npm | v24.10.0 / 11.6.0 |
| Maven 镜像 | Maven Central **不可达**，用项目内的阿里云镜像（见上） |

**没有 Docker，没有 MySQL。** 不要建议用 Docker 起数据库，本机 PostgreSQL 已经就绪。

```bash
psql -h 127.0.0.1 -d wms        # 连库验证
```

⚠️ **绝不要 `DROP DATABASE wms`。** 里面是 Flyway 迁移的产物，drop 掉会导致迁移校验和对不上。
要验迁移语法就用临时库，跑完就删：

```bash
psql -h 127.0.0.1 -d postgres -c "CREATE DATABASE wms_scratch;"
psql -h 127.0.0.1 -d wms_scratch -v ON_ERROR_STOP=1 -f <脚本>
psql -h 127.0.0.1 -d postgres -c "DROP DATABASE wms_scratch;"
```

## 数据库约定

- Schema 由 **Flyway** 管理，迁移脚本在 `server/src/main/resources/db/migration/`
- **改表结构新增迁移文件（`V3__xxx.sql`），绝不修改已执行过的 `V1` / `V2`**
- 所有业务表软删除：查询必须带 `deleted = 0`
- 命名：表 `wms_` 前缀，列 `小写下划线`；Java 字段 `小驼峰`，靠 MyBatis camel-case 映射转换

## 错误处理约定

唯一性冲突（编码重复）捕获 PostgreSQL `SQLState 23505`（Spring 侧即 `DuplicateKeyException`），
转成 `BizException(40901, ...)`。**不要把底层 SQL 异常直接抛给前端** —— 那会变成 500 加上堆栈泄漏。

完整错误码见契约文档第 9 节。

## 验证方式（不要只靠编译通过）

编译过 ≠ 能跑。每个改动至少做到：

**后端**
```bash
cd server && mvn clean package
java -jar target/wms-server-*.jar &     # 启动
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/warehouses?page=1&size=10"
```
首次 `mvn package` 要下载依赖，**给足 3-5 分钟**，别误判成卡死。

**前端**
```bash
cd web && npm run build     # 类型检查 + 打包，必须无错
```
首次 `npm install` 同样要给足时间。

**收工前**：把自己启动的后台进程关掉，别把 8080 / 5173 占着留给用户。

## 目录归属

写 `docs/`、根 `README.md`、本文件、以及 git 操作时由主会话负责；
如果是被指派做单一子系统的 worker，只碰分配到的那半个仓库。

## 约定优于配置，但别过度抽象

四个模块（仓库 / 库位 / SKU / 客户）的 CRUD 结构高度相似，这是刻意的重复。
**不要急着抽一个泛型 BaseMapper / BaseService** —— 四个模块的业务规则各不相同
（库位要校验仓库、仓库有级联删除保护、SKU 有分类去重接口），过早抽象会挡住这些差异，
等真的出现第五个结构完全一致的模块时再说。
