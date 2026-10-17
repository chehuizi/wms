# WMS API 契约 v1

前后端唯一事实来源。后端按此实现 Controller，前端按此生成 API 调用与类型。
字段名一律 **小驼峰**（JSON），数据库列名一律 **小写下划线**。

- Base URL: `http://localhost:8080/api/v1`
- 前端开发服务器通过 Vite proxy 把 `/api` 转发到 `8080`
- 编码: `application/json;charset=UTF-8`

---

## 1. 统一响应包络

所有接口（除分页外）都返回这个结构。HTTP 状态码仍按语义给（400/404/409/500），
`code` 字段用于前端做细粒度分支，`code = 0` 表示成功。

```jsonc
// 成功 — 单对象
{ "code": 0, "message": "ok", "data": { /* 业务对象 */ } }

// 成功 — 空数据
{ "code": 0, "message": "ok", "data": null }

// 失败
{ "code": 40401, "message": "仓库不存在", "data": null }
```

`code` 规则：`0` 成功；`400xx` 参数/校验；`404xx` 资源不存在；`409xx` 唯一性或状态冲突；`500xx` 服务端异常。

## 2. 分页

分页接口的 `data` 结构：

```jsonc
{
  "list": [ /* 业务对象数组 */ ],
  "total": 128,   // 总条数
  "page": 1,      // 当前页码，从 1 开始
  "size": 20      // 每页条数
}
```

查询参数：

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `page` | int | 1 | 页码，从 1 开始 |
| `size` | int | 20 | 每页条数，最大 200 |
| `keyword` | string | — | 模糊搜索，匹配编码 + 名称 |
| `status` | int | — | 1=启用 0=停用，不传=全部 |
| `sortBy` | string | `id` | 排序字段（白名单校验） |
| `sortOrder` | string | `desc` | `asc` / `desc` |

### 1.1 批量删除请求体

四个模块的 `POST /{module}/batch-delete` 请求体**统一是裸 id 数组**，不是对象：

```jsonc
[1, 2, 3]
```

不是 `{"ids": [1,2,3]}`，也不是 `{"idList": [1,2,3]}`。

后端语义：

- 数组为空或为空数组 → 返回 `40001`
- 数组内含**已不存在或已删除**的 id → 整个请求失败，返回 `40401`，**不做部分删除**
- 存在业务约束冲突（如仓库下仍有库位）→ 返回 `40902`，**不做部分删除**

即**全成或全败**，调用方要么全删成功要么什么都没发生。

## 3. 通用实体字段

所有实体都有：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | int | 主键 |
| `createdAt` | string | ISO-8601 时间，如 `2026-10-17T02:30:00Z` |
| `updatedAt` | string | 同上 |

---

## 4. 仓库 warehouse

`/api/v1/warehouses`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/warehouses` | 分页列表，支持 `keyword`、`status` |
| GET | `/warehouses/{id}` | 详情 |
| POST | `/warehouses` | 新建 |
| PUT | `/warehouses/{id}` | 整单更新 |
| DELETE | `/warehouses/{id}` | 软删除 |
| POST | `/warehouses/batch-delete` | 批量软删除，body 为 id 数组，见第 1.1 节 |

请求体（新建/更新）：

```jsonc
{
  "code": "WH01",              // 必填，全局唯一，1-32 字符
  "name": "华东中心仓",         // 必填，1-128 字符
  "address": "上海市青浦区华徐公路 888 号",  // 选填，≤255
  "contactName": "张伟",        // 选填，≤64
  "contactPhone": "13800000001", // 选填，≤32
  "status": 1,                 // 选填，默认 1
  "remark": "主仓"             // 选填，≤255
}
```

响应对象在请求体基础上增加 `id`、`createdAt`、`updatedAt`。

**业务规则**：`code` 唯一，重复返回 409（`code: 40901`）。
**级联保护**：仓库下仍有库位时不允许删除，返回 409（`code: 40902`）。

## 5. 库位 location

`/api/v1/locations`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/locations` | 分页列表，支持 `keyword`、`status`、`warehouseId`、`locationType`、`zone` |
| GET | `/locations/{id}` | 详情（返回含 `warehouseName`） |
| POST | `/locations` | 新建 |
| PUT | `/locations/{id}` | 整单更新 |
| DELETE | `/locations/{id}` | 软删除 |
| POST | `/locations/batch-delete` | 批量软删除，body 为 id 数组，见第 1.1 节 |

请求体：

```jsonc
{
  "warehouseId": 1,      // 必填，须为存在的启用仓库
  "code": "A-01-01",     // 必填，同一仓库内唯一，1-32 字符
  "zone": "A",           // 必填，库区编码，1-32 字符
  "locationType": 1,     // 选填，默认 1
  "capacity": 100.0000,  // 选填 number，>0，NULL/不传=不限制
  "allowNegative": 0,    // 选填，默认 0
  "status": 1,           // 选填，默认 1
  "remark": "A区整箱位"   // 选填，≤255
}
```

响应对象额外含：`warehouseName`（所属仓库名称）、`createdAt`、`updatedAt`。

**业务规则**：
- `code` 在同一 `warehouseId` 下唯一，冲突返回 409（`code: 40901`）
- `warehouseId` 不存在或已停用，返回 400（`code: 40003`）
- 删除仓库时会检查其下库位，有则拒绝

**枚举 `locationType`**：

| 值 | 含义 | 前端标签 |
|---|---|---|
| 1 | 存储位 | 存储位 |
| 2 | 收货暂存位 | 收货暂存 |
| 3 | 发货暂存位 | 发货暂存 |
| 4 | 不良品位 | 不良品 |
| 5 | 退货位 | 退货位 |

## 6. 商品 SKU

`/api/v1/skus`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/skus` | 分页列表，支持 `keyword`、`status`、`category`、`batchManaged`、`serialManaged` |
| GET | `/skus/{id}` | 详情 |
| POST | `/skus` | 新建 |
| PUT | `/skus/{id}` | 整单更新 |
| DELETE | `/skus/{id}` | 软删除 |
| POST | `/skus/batch-delete` | 批量软删除，body 为 id 数组，见第 1.1 节 |
| GET | `/skus/categories` | 返回去重后的分类字符串数组 `["饮料","方便速食",...]` |

请求体：

```jsonc
{
  "code": "SKU-1001",        // 必填，全局唯一，1-64 字符
  "name": "农夫山泉 550ml 瓶装饮用水",  // 必填，1-128
  "category": "饮料",         // 选填，≤64
  "spec": "550ml × 24瓶/箱",   // 选填，≤64
  "unit": "箱",               // 选填，≤16
  "barcode": "6901285991219",  // 选填，≤64
  "batchManaged": 1,          // 选填 0/1，默认 0
  "serialManaged": 0,         // 选填 0/1，默认 0
  "shelfLifeDays": 365,       // 选填 int，>=0，NULL=无保质期
  "weight": 13.5,             // 选填 number，>=0
  "volume": 0.035,            // 选填 number，>=0
  "length": 0.4,              // 选填 number，>=0
  "width": 0.3,               // 选填 number，>=0
  "height": 0.27,             // 选填 number，>=0
  "status": 1,                // 选填，默认 1
  "remark": null              // 选填，≤255
}
```

**业务规则**：
- `code` 唯一，冲突返回 409（`code: 40901`）
- `serialManaged = 1` 且 `batchManaged = 1` 时允许（现代 WMS 常见），不报错
- `shelfLifeDays` 为 0 视同无保质期，后端存 NULL

## 7. 客户 customer

`/api/v1/customers`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/customers` | 分页列表，支持 `keyword`、`status` |
| GET | `/customers/{id}` | 详情 |
| POST | `/customers` | 新建 |
| PUT | `/customers/{id}` | 整单更新 |
| DELETE | `/customers/{id}` | 软删除 |
| POST | `/customers/batch-delete` | 批量软删除，body 为 id 数组，见第 1.1 节 |

请求体：

```jsonc
{
  "code": "CUS001",        // 必填，唯一，1-32
  "name": "上海永辉超市采购部",  // 必填，1-128
  "contactName": "陈明",    // 选填，≤64
  "contactPhone": "13911110001", // 选填，≤32
  "contactEmail": "chenming@example.com", // 选填，≤128，做邮箱格式校验
  "address": "上海市浦东新区张江路 1 号",  // 选填，≤255
  "status": 1,             // 选填，默认 1
  "remark": "商超渠道"     // 选填，≤255
}
```

**业务规则**：`code` 唯一；`contactEmail` 有值时必须符合邮箱格式，否则 400（`code: 40002`）。

## 8. 健康检查

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/v1/health` | 不走包络，直接返回 `{"status":"UP","database":"UP"}` |

---

## 9. 错误码表

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功 |
| 40001 | 400 | 参数校验失败，message 含具体字段原因 |
| 40002 | 400 | 邮箱格式非法 |
| 40003 | 400 | 关联资源不存在或已停用（如库位引用了不存在的仓库） |
| 40401 | 404 | 资源不存在或已删除 |
| 40901 | 409 | 唯一性冲突（编码重复） |
| 40902 | 409 | 存在关联资源，无法删除（如仓库下有库位） |
| 50000 | 500 | 服务端内部错误 |
