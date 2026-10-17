# WMS — 仓储管理系统

前后端同仓的仓储管理系统（WMS），覆盖**基础主数据**与**核心业务闭环**两大块：
仓库/库位/SKU/客户主数据，库存台账与流水，入库（收货上架）、出库（分配锁定→波次→拣货→发货），
盘点（实盘→差异审批→自动调账），以及基于流水的报表看板。

| | 技术栈 |
|---|---|
| 后端 | Spring Boot 3.4 · MyBatis · PostgreSQL 16 · Flyway · Java 21 |
| 前端 | Vue 3 · Vite · Element Plus · TypeScript · ECharts |
| 构建 | Maven 3.9 · npm 11 |
| 规模 | 22 张表 · 61 个单元测试 · 前后端共 10 个页面 |

## 目录结构

```
wms/
├── server/          # 后端服务（独立 Maven 工程）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/wms/
│       │   ├── common/       # 统一响应包络、分页、异常处理
│       │   ├── entity/       # 实体，与表结构逐列对应
│       │   ├── dto/ vo/      # 请求 / 响应对象
│       │   ├── mapper/       # MyBatis Mapper 接口
│       │   └── module/       # warehouse / location / sku / customer
│       └── resources/
│           ├── application.yml
│           ├── mapper/       # MyBatis XML
│           └── db/migration/ # Flyway 迁移脚本
├── web/             # 前端应用（独立 Vite 工程）
│   └── src/
│       ├── api/      # axios 封装 + 各模块请求
│       ├── types/    # TypeScript 类型
│       ├── layouts/  # 整体布局
│       └── views/    # 四个 CRUD 页面
└── docs/
    ├── API_CONTRACT.md       # 基础主数据契约（前后端事实来源）
    └── API_CONTRACT_BIZ.md   # 业务模块契约 + 库存核心不变式
```

两个子目录是**完全独立**的工程，各自构建、各自发布，不共享构建配置。

## 功能模块

### 主数据

| 模块 | 说明 |
|---|---|
| 仓库 | 编码唯一，仓库下有库位时禁止删除（级联保护） |
| 库位 | 归属仓库，库位类型：存储位 / 收货暂存 / 发货暂存 / 不良品 / 退货位 |
| 商品 SKU | 批次管理、序列号管理、保质期，分类去重接口 |
| 客户 | 编码唯一，邮箱格式校验 |

### 业务闭环

| 模块 | 流程 |
|---|---|
| **入库** | 到货单 → 提交 → 收货（落收货暂存位，自动生成上架任务）→ 上架（移库） |
| **库存** | 台账按「库位 + SKU + 批次 + 序列号」记账，可用量 = 在库 − 锁定；全量流水留痕 |
| **出库** | 订单 → 提交 → 分配（FIFO 取可用量并锁定）→ 生成拣货任务或波次 → 拣货 → 发货 |
| **盘点** | 建单（快照账面数）→ 开始 → 逐行录实盘 → 结束 → 审批 → 自动调账 |
| **报表** | 看板汇总、库存汇总、出入库统计、库龄分析（全部基于流水聚合，不另建表） |

**库存核心不变式**（详见 `docs/API_CONTRACT_BIZ.md` 第 10 节）：可用量不落库实时算；
锁定量不得超过在库量；每一次库存变化**同事务**写流水；移库必写两条流水；
分配要么全成要么全败，绝不留下半锁。

## 数据库

用的是**本机 PostgreSQL 16**（Homebrew 安装，默认监听 `127.0.0.1:5432`）。

```bash
# 首次需要建库
psql -h 127.0.0.1 -d postgres -c "CREATE DATABASE wms ENCODING 'UTF8';"
```

数据库连接参数写在 `server/src/main/resources/application.yml`，本地开发用户是 `chehuizi`（superuser，无密码）。

**Schema 由 Flyway 管理**，改表结构请新增迁移文件，**不要手改已有脚本**：

```
server/src/main/resources/db/migration/V1__init_schema.sql    # 表结构
server/src/main/resources/db/migration/V2__seed_demo_data.sql # 演示数据
```

常用命令：

```bash
mvn -f server/pom.xml flyway:info     # 看迁移状态
mvn -f server/pom.xml flyway:repair   # 迁移校验和修复
```

## 快速启动

### 后端

```bash
cd server
mvn clean package -DskipTests
java -jar target/wms-server-*.jar
```

服务监听 `http://localhost:8080`，接口前缀 `/api/v1`。

```bash
curl http://localhost:8080/api/v1/health
```

### 前端

```bash
cd web
npm install
npm run dev
```

开发服务器默认 `http://localhost:5173`，已配置 proxy 把 `/api` 转发到 `localhost:8080`，
所以**前后端同时启动**才能看到真实数据。

生产构建：

```bash
npm run build     # 产出 dist/
```

## 接口

完整接口定义见 [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md)（主数据）与 [`docs/API_CONTRACT_BIZ.md`](docs/API_CONTRACT_BIZ.md)（业务模块）。

统一响应包络：

```json
{ "code": 0, "message": "ok", "data": { } }
```

`code = 0` 表示成功，非 0 为业务错误码（详见契约文档的错误码表）。

## 开发约定

- 数据库列名 `小写下划线`，JSON 字段名 `小驼峰`，由 MyBatis 的 `map-underscore-to-camel-case` 转换
- 所有业务表都是**软删除**，查询必须带 `deleted = 0`
- 唯一性冲突统一返回 `40901`，靠捕获 PostgreSQL `SQLState 23505` 实现，不要把原始异常抛给前端
- 改接口先改对应的 `docs/API_CONTRACT*.md`，前后端都照它写
