# WMS 后端服务

仓储管理系统后端，Spring Boot 3.4.5 + MyBatis + PostgreSQL 16。

接口契约见 `../docs/API_CONTRACT.md`，本实现严格按该契约。

## 技术栈

| 组件 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| MyBatis | mybatis-spring-boot-starter 3.0.4（XML 映射，非注解） |
| PostgreSQL | 16（驱动由 Spring Boot 管理） |
| Flyway | 由 Spring Boot 管理（flyway-core + flyway-database-postgresql） |
| 构建 | Maven |

## 前置条件

1. JDK 21 已安装
2. Maven 3.9+ 已安装
3. **PostgreSQL 16 正在运行**，且 `wms` 数据库已存在

数据库连接参数写在 `src/main/resources/application.yml`：

```yaml
url: jdbc:postgresql://127.0.0.1:5432/wms
username: chehuizi
password:            # 空密码
```

> 本机网络无法直连 Maven Central，`server/.mvn/settings.xml` 配置了阿里云镜像，
> 通过 `server/.mvn/maven.config` 自动生效，**不影响你的全局 `~/.m2` 配置**。

## 启动

```bash
cd server
mvn clean package            # 首次会下载依赖，约 3-5 分钟
mvn spring-boot:run          # 开发模式
```

或构建后直接跑 jar：

```bash
java -jar target/wms-server-0.1.0.jar
```

服务监听 **8080** 端口，Base URL：`http://localhost:8080/api/v1`

健康检查：`curl http://localhost:8080/api/v1/health`
→ `{"status":"UP","database":"UP"}`

## 数据库迁移

表结构与演示数据由 Flyway 管理，位于 `src/main/resources/db/migration/`：

- `V1__init_schema.sql` — 四张表 `wms_warehouse` / `wms_location` / `wms_sku` / `wms_customer`
- `V2__seed_demo_data.sql` — 演示种子数据（3 仓库 / 9 库位 / 6 SKU / 4 客户）

首次启动自动执行。若要重置演示数据：

```bash
psql -h 127.0.0.1 -U chehuizi -d wms -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

## 测试

```bash
mvn test
```

覆盖 service 层业务规则：仓库级联删除保护、库位仓库有效性校验、
SKU 保质期归一化、邮箱校验、排序字段白名单防注入。

单元测试全部基于 Mockito mock，不依赖数据库。

## 目录结构

```
com.wms
├── WmsApplication.java
├── common/       Result 包络、PageResult、BizException、全局异常处理器、HealthController
├── entity/       与表结构逐列对应的实体
├── dto/          Query（分页查询）/ SaveRequest（增改请求）
├── vo/           响应对象
├── mapper/       MyBatis Mapper 接口
└── module/       warehouse / location / sku / customer，各含 controller + service
resources/mapper/*.xml   所有 SQL（无注解 SQL）
```

## 与前端的约定

- 统一响应包络 `{code, message, data}`，`code = 0` 成功
- 分页 `data` 为 `{list, total, page, size}`
- 错误码见契约第 9 节：`40001` 参数校验、`40002` 邮箱格式、`40003` 关联资源无效、
  `40401` 资源不存在、`40901` 编码重复、`40902` 存在关联资源无法删除、`50000` 服务端错误
- 前端 Vite dev server 把 `/api` 代理到 `http://localhost:8080`
