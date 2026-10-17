
---

# 第二部分：核心业务模块

> 本部分在 v1 基础主数据之上补充库存、入库、出库、盘点四大模块。
> 数据库结构见 `server/src/main/resources/db/migration/V3__business_modules.sql`（22 张表）。

## 10. 库存核心不变式

这 8 条是整个 WMS 的地基，**后端所有涉及库存的写操作都必须满足**，前端展示也要按此口径：

1. **可用量不落库**：`availableQty = onHandQty - lockedQty`，永远实时计算，不要当字段存
2. **锁定量 ≤ 在库量**：DB 有 CHECK 约束 `ck_inventory_not_over_locked`
3. **不允许负库存**：库位 `allowNegative = 0` 时，`onHandQty` 不得为负；违反返回 `40904`
4. **有账必有流水**：每一次 `onHandQty` 或 `lockedQty` 的变化，**同一事务内**必须写一条 `wms_stock_movement`
5. **并发安全**：库存行更新必须 `SELECT ... FOR UPDATE` 悲观加锁，避免并发超卖
6. **流水不可变**：`wms_stock_movement` 只增不改不删，`beforeQty` / `afterQty` 必须记录快照
7. **移库是两条流水**：同一事务内先写 `5=移库出` 再写 `6=移库入`
8. **过期与冻结**：库位 `allowNegative=1` 只放宽负库存，不放宽过期校验

## 11. 单据号规则

后端统一生成，格式为「前缀 + yyyyMMdd + 4 位日内序号」，前端只读不填：

| 单据 | 前缀 | 示例 |
|---|---|---|
| 入库到货单 | `IN` | `IN202610170001` |
| 收货记录 | `RC` | `RC202610170001` |
| 上架任务 | `PT` | `PT202610170001` |
| 出库订单 | `OUT` | `OUT202610170001` |
| 拣货任务 | `PK` | `PK202610170001` |
| 发货单 | `SH` | `SH202610170001` |
| 拣货波次 | `WV` | `WV202610170001` |
| 盘点单 | `ST` | `ST202610170001` |
| 库存流水 | `MV` | `MV2026101700001` |

## 12. 通用状态枚举

### 12.1 入库到货单 `inboundOrder.status`

| 值 | 含义 | 允许的下一状态 |
|---|---|---|
| 1 | 草稿 | 2, 5 |
| 2 | 待收货 | 3, 4, 5 |
| 3 | 部分收货 | 3, 4, 5 |
| 4 | 已完成 | — |
| 5 | 已取消 | — |

### 12.2 出库订单 `outboundOrder.status`

| 值 | 含义 | 允许的下一状态 |
|---|---|---|
| 1 | 草稿 | 2, 7 |
| 2 | 待分配 | 3, 7 |
| 3 | 已分配 | 4, 7 |
| 4 | 拣货中 | 5, 7 |
| 5 | 待发货 | 6, 7 |
| 6 | 已发货 | — |
| 7 | 已取消 | — |

### 12.3 库存流水 `movement.movementType`

| 值 | 含义 | qtyChange | lockedChange |
|---|---|---|---|
| 1 | 入库收货 | 正 | 0 |
| 2 | 出库发货 | 负 | 负（同时扣减） |
| 3 | 盘盈 | 正 | 0 |
| 4 | 盘亏 | 负 | 0 |
| 5 | 移库出 | 负 | 0 |
| 6 | 移库入 | 正 | 0 |
| 7 | 锁定 | 0 | 正 |
| 8 | 解锁 | 0 | 负 |
| 9 | 手工调整 | 正或负 | 0 |

### 12.4 其他

| 字段 | 取值 |
|---|---|
| `putawayTask.status` | 1=待上架 2=已完成 3=已取消 |
| `pickTask.status` | 1=待拣货 2=拣货中 3=已完成 4=已取消 |
| `allocation.status` | 1=已锁定 2=拣货中 3=已拣货 4=已发货 5=已释放 |
| `wave.status` | 1=创建中 2=已释放 3=拣货中 4=已完成 5=已关闭 |
| `shipment` | 无状态，发货即终态 |
| `stocktakeOrder.stocktakeType` | 1=全盘 2=循环盘 3=抽盘 |
| `stocktakeOrder.status` | 1=草稿 2=进行中 3=待审批 4=已完成 5=已取消 |
| `stocktakeItem.status` | 1=待盘 2=已盘 3=待审批 4=已调账 |

---

## 13. 库存台账 `/api/v1/inventories`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/inventories` | 库存查询 |
| GET | `/inventories/{id}` | 单条库存详情 |
| POST | `/inventories/adjust` | 手工调整（盘点外的紧急调整） |
| GET | `/inventories/movements` | 库存流水查询 |

**`GET /inventories` 查询参数**：

| 参数 | 类型 | 说明 |
|---|---|---|
| `page` / `size` | int | 分页 |
| `keyword` | string | 匹配 SKU 编码 / 名称 / 条码 |
| `warehouseId` | int | 仓库 |
| `locationId` | int | 库位 |
| `skuId` | int | SKU |
| `category` | string | 商品分类 |
| `batchNo` | string | 批次号 |
| `status` | int | 1=正常 0=冻结 |
| `onlyAvailable` | boolean | 只看在库量 > 0 的，默认 false |
| `includeLockedZero` | boolean | 是否包含已锁完的，默认 true |

响应对象：

```jsonc
{
  "id": 1,
  "warehouseId": 1, "warehouseName": "华东中心仓",
  "locationId": 7, "locationCode": "A-01-01", "zone": "A",
  "skuId": 1, "skuCode": "SKU-1001", "skuName": "农夫山泉 550ml 瓶装饮用水",
  "category": "饮料", "unit": "箱",
  "batchNo": "B20261001", "serialNo": null,
  "producedDate": "2026-10-01", "expireDate": "2027-10-01",
  "onHandQty": 128.0000,
  "lockedQty": 32.0000,
  "availableQty": 96.0000,   // ← 计算字段，后端算好返回
  "status": 1,
  "createdAt": "...", "updatedAt": "..."
}
```

**`POST /inventories/adjust`**：

```jsonc
{
  "inventoryId": 1,        // 已存在则传；传 null 则按下面的键新建一条库存行
  "warehouseId": 1,       // inventoryId 为 null 时必填
  "locationId": 7,        // 同上
  "skuId": 1,             // 同上
  "batchNo": null,        // 可空
  "serialNo": null,       // 可空
  "qtyChange": -5,        // 必填，正数=盘盈，负数=盘亏，0 拒绝
  "reason": "货损",        // 必填，≤255
  "operator": "张三"       // 必填，≤64
}
```

返回更新后的库存对象。流水 `movementType = 9`。

**`GET /inventories/movements`** 查询参数：`page`/`size`/`keyword`（SKU 编码名称）/`warehouseId`/`locationId`/`skuId`/`movementType`/`bizType`/`bizNo`/`startTime`/`endTime`。

响应：

```jsonc
{
  "id": 12,
  "movementNo": "MV2026101700001",
  "movementType": 2, "movementTypeName": "出库发货",
  "bizType": "OUTBOUND", "bizNo": "OUT202610170001",
  "warehouseId": 1, "warehouseName": "华东中心仓",
  "locationId": 7, "locationCode": "A-01-01",
  "skuId": 1, "skuCode": "SKU-1001", "skuName": "农夫山泉 550ml 瓶装饮用水",
  "batchNo": null, "serialNo": null,
  "qtyChange": -10.0000, "lockedChange": -10.0000,
  "beforeQty": 138.0000, "afterQty": 128.0000,
  "operator": "张三", "remark": null,
  "createdAt": "2026-10-17T02:30:00Z"
}
```

## 14. 序列号 `/api/v1/serials`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/serials` | 序列号查询，支持 `keyword`（序列号或 SKU）/`skuId`/`status`/`warehouseId` |
| GET | `/serials/{id}` | 详情 |

`status`：1=在库 2=已锁定 3=已出库 4=已退货。

> 后端规则：当 SKU 的 `serialManaged = 1` 时，收货必须逐个录入序列号；出库按序列号逐个扣减；
> `serialManaged = 0` 时忽略本模块，`serialNo` 一律为 NULL。

---

## 15. 入库 `/api/v1`

### 15.1 到货单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/inbound-orders` | 分页，支持 `keyword`/`warehouseId`/`status`/`startTime`/`endTime` |
| GET | `/inbound-orders/{id}` | 详情（含明细） |
| POST | `/inbound-orders` | 新建草稿 |
| PUT | `/inbound-orders/{id}` | 修改（仅草稿态） |
| DELETE | `/inbound-orders/{id}` | 删除（仅草稿态） |
| POST | `/inbound-orders/{id}/submit` | 提交 → 待收货 |
| POST | `/inbound-orders/{id}/cancel` | 取消 |

请求体：

```jsonc
{
  "warehouseId": 1,
  "supplierCode": "SUP001",
  "supplierName": "农夫山泉华东经销商",
  "expectedAt": "2026-10-20T08:00:00Z",
  "remark": "季度补货",
  "items": [
    { "skuId": 1, "expectedQty": 500 },
    { "skuId": 2, "expectedQty": 200 }
  ]
}
```

> `items` 必填且至少 1 条，同一 SKU 只能出现一次（重复返回 `40001`）。

### 15.2 收货

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/inbound-orders/{id}/receipts` | 执行收货 |
| GET | `/inbound-orders/{id}/receipts` | 该单的收货记录 |
| GET | `/putaway-tasks` | 上架任务列表，支持 `inboundId`/`skuId`/`status` |

收货请求体：

```jsonc
{
  "locationId": 12,          // 收货暂存位，必填，须为该仓库下 locationType=2 的启用库位
  "operator": "张三",
  "remark": null,
  "items": [
    {
      "inboundItemId": 1,    // 必填，对应到货单明细
      "batchNo": "B20261001",
      "serialNo": null,      // SKU 开序列号管理时必填
      "producedDate": "2026-10-01",
      "expireDate": "2027-10-01",
      "qty": 500
    }
  ]
}
```

**收货的业务规则（后端必须全部实现）**：

1. 累加 `inboundItem.receivedQty`，**不得超过 `expectedQty`**，超收返回 `40903`
2. 库存 `onHandQty += qty` 落到 `locationId`，同时写流水 `movementType = 1`
3. SKU `serialManaged = 1` 时，`items` 必须逐条带 `serialNo`，且序列号台账中不得已存在
4. 自动生成上架任务（`fromLocationId = 收货位`），`status = 1`
5. 明细全部收满 → 到货单 `status = 4 已完成`，写 `finishedAt`；部分收 → `3 部分收货`

**上架执行**：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/putaway-tasks/{id}/execute` | 执行上架 |

```jsonc
{ "toLocationId": 7, "operator": "李四" }
```

- `toLocationId` 必填，须为该仓库下 `locationType = 1 存储位` 的启用库位
- 同事务：源库位 `onHandQty -= qty` 写流水 `5`，目标库位 `onHandQty += qty` 写流水 `6`
- 批次和序列号随货移动
- 源库位扣完后任务 `status = 2 已完成`

## 16. 出库 `/api/v1`

### 16.1 出库订单

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/outbound-orders` | 分页，支持 `keyword`/`warehouseId`/`customerId`/`status`/`startTime`/`endTime` |
| GET | `/outbound-orders/{id}` | 详情（含明细、分配记录） |
| POST | `/outbound-orders` | 新建草稿 |
| PUT | `/outbound-orders/{id}` | 修改（仅草稿态） |
| DELETE | `/outbound-orders/{id}` | 删除（仅草稿态） |
| POST | `/outbound-orders/{id}/submit` | 提交 → 待分配 |
| POST | `/outbound-orders/{id}/allocate` | 分配库存（锁定） |
| POST | `/outbound-orders/{id}/pick-tasks` | 直接生成拣货任务（不走波次） |
| POST | `/outbound-orders/{id}/release-allocation` | 释放全部未发货锁定 |
| POST | `/outbound-orders/{id}/cancel` | 取消订单 |
| POST | `/outbound-orders/{id}/ship` | 发货 |

请求体：

```jsonc
{
  "warehouseId": 1,
  "customerId": 1,
  "priority": 5,
  "expectedShipAt": "2026-10-22T10:00:00Z",
  "remark": null,
  "items": [
    { "skuId": 1, "orderedQty": 50 },
    { "skuId": 2, "orderedQty": 20 }
  ]
}
```

### 16.2 分配（锁定库存）

`POST /outbound-orders/{id}/allocate` —— 无请求体。

后端规则：

1. 按 **FIFO** 策略取可用库存：同一 SKU 下先按 `expireDate` 早的（临期先出），再按 `producedDate` 早的，最后按 `id` 早的
2. 只取 `status = 1` 且 `deleted = 0` 的库存行，且 `onHandQty - lockedQty >= 剩余需求`
3. 每取一行：`lockedQty += qty`，写流水 `movementType = 7`
4. 生成 `wms_allocation` 记录，`status = 1`
5. 累加 `outboundItem.allocatedQty`
6. **全部明细分配成功** → 订单 `status = 3 已分配`
7. **可用量不足** → 返回 `40903`，message 写清「SKU 编码 需 X，可用 Y，缺 Z」，且**不得留下任何已锁定的部分**（要么全成要么全回滚）

### 16.3 波次

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/waves` | 分页，支持 `keyword`/`warehouseId`/`status` |
| POST | `/waves` | 创建波次 |
| POST | `/waves/{id}/orders` | 加入订单 |
| POST | `/waves/{id}/release` | 释放波次 → 生成拣货任务 |

创建波次请求体：

```jsonc
{ "warehouseId": 1, "operator": "王五" }
```

加入订单请求体：`{ "outboundIds": [1, 2, 3] }`

释放时后端规则：

1. 按**库位编码**升序（同库位合并），生成拣货任务，`sequence` 递增
2. 任务 `status = 1 待拣货`，订单 `status = 4 拣货中`，波次 `status = 3 拣货中`
3. 只能加入 `status = 3 已分配` 的订单

### 16.4 拣货

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/pick-tasks` | 任务列表，支持 `waveId`/`outboundId`/`skuId`/`status`/`keyword` |
| POST | `/pick-tasks/{id}/pick` | 拣货 |
| POST | `/pick-tasks/{id}/cancel` | 取消任务 |

```jsonc
{ "pickedQty": 10, "operator": "赵六" }
```

- `pickedQty` 必填，`> 0` 且 `<= requiredQty`（拣不满允许，部分拣）
- 累加 `pickTask.pickedQty`、`allocation.pickedQty`、`outboundItem.pickedQty`
- 任务完成 → `status = 3`
- **订单全部明细拣满** → 订单 `status = 5 待发货`
- 波次全部任务完成 → 波次 `status = 4 已完成`

### 16.5 发货

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/outbound-orders/{id}/ship` | 发货 |
| GET | `/outbound-orders/{id}/shipments` | 发货记录 |

```jsonc
{
  "carrier": "顺丰速运",
  "trackingNo": "SF1234567890",
  "operator": "孙七"
}
```

后端规则：

1. 校验订单 `status = 5 待发货`，否则拒绝
2. 对每条 allocation：`onHandQty -= shippedQty`、`lockedQty -= shippedQty`，写流水 `movementType = 2`
3. 序列号台账状态置 `3 已出库`，写 `outBoundAt`
4. 累加 `outboundItem.shippedQty`，allocation `status = 4`
5. 订单 `status = 6 已发货`，写 `shippedAt`，生成发货单

### 16.6 释放与取消

`POST /outbound-orders/{id}/release-allocation`：

- 仅 `status ∈ {3, 4}` 可释放
- 释放所有 `status ∈ {1, 2}` 的 allocation：`lockedQty -= allocatedQty - pickedQty`，写流水 `movementType = 8`
- 累减 `outboundItem.allocatedQty` 与 `pickedQty`
- 订单回 `status = 2 待分配`

`POST /outbound-orders/{id}/cancel`：

- `status = 6 已发货` 拒绝（`40905`）
- 先自动释放锁定，再置 `status = 7 已取消`
- `status ∈ {1, 5}` 直接取消

---

## 17. 盘点 `/api/v1/stocktake-orders`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/stocktake-orders` | 分页，支持 `keyword`/`warehouseId`/`status`/`stocktakeType` |
| GET | `/stocktake-orders/{id}` | 详情（含明细） |
| POST | `/stocktake-orders` | 创建并生成明细 |
| DELETE | `/stocktake-orders/{id}` | 删除（仅草稿态） |
| POST | `/stocktake-orders/{id}/start` | 开始盘点 → 进行中 |
| POST | `/stocktake-orders/{id}/count` | 录入实盘 |
| POST | `/stocktake-orders/{id}/finish` | 结束盘点 → 待审批 |
| POST | `/stocktake-orders/{id}/approve` | 审批并调账 → 已完成 |
| POST | `/stocktake-orders/{id}/cancel` | 取消 |

创建请求体：

```jsonc
{
  "warehouseId": 1,
  "stocktakeType": 1,       // 1=全盘 2=循环盘 3=抽盘
  "scopeZone": null,        // 全盘可空；循环盘/抽盘必填
  "remark": "月末全盘"
}
```

**创建时后端规则**：

1. 抓取范围内**所有未删除的启用库位**
2. 对每个库位下**每条在库量 > 0 的库存行**生成一条 `wms_stocktake_item`
3. `bookQty` = 生成时刻的 `onHandQty` 快照
4. `status = 1 待盘`，回写 `totalLocations`
5. `stocktakeType ≠ 1` 时 `scopeZone` 必填，否则返回 `40001`

开始盘点：`POST /{id}/start` → `status = 2 进行中`，写 `startedAt`。

录入实盘：

```jsonc
{
  "items": [
    { "itemId": 1, "countedQty": 98, "remark": "少 2 箱，疑似破损" },
    { "itemId": 2, "countedQty": 0,  "remark": null }
  ],
  "operator": "李四"
}
```

- `diffQty = countedQty - bookQty`，可为负可为正
- `countedQty` 必填且 `>= 0`（**没货要显式录 0**，不允许不填）
- 已录入的 item 重复提交视为覆盖

结束盘点：`POST /{id}/finish`
- 必须全部明细都已录入，否则 `40004`
- 无差异 → 直接 `status = 4 已完成`，写 `finishedAt`
- 有差异 → `status = 3 待审批`，写 `finishedAt`

审批并调账：

```jsonc
{ "approver": "周八", "remark": "差异已核实，按实盘调整" }
```

后端规则（**必须逐条在一个事务里做**）：

1. 校验 `status = 3 待审批`
2. 对每条 `diffQty ≠ 0` 的明细：
   - `diffQty > 0` → 库存 `onHandQty += diffQty`，流水 `movementType = 3 盘盈`
   - `diffQty < 0` → 库存 `onHandQty += diffQty`（负数即扣减），流水 `movementType = 4 盘亏`
   - 写 `wms_stocktake_adjust`
   - item `status = 4 已调账`
3. `diffQty = 0` 的明细 → `status = 4 已调账`，不写流水
4. 订单 `status = 4 已完成`，写 `finishedAt` 与 `approvedAt`

取消：`status ∈ {1, 2}` 可取消 → `5 已取消`，已录入数据不写流水。

---

## 18. 报表 `/api/v1/reports`

报表一律**只读**，不新建表，全部基于 `wms_stock_movement` 与 `wms_inventory` 聚合。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/reports/dashboard` | 看板汇总 |
| GET | `/reports/inventory-summary` | 库存汇总（按仓库 / 分类 / SKU 聚合） |
| GET | `/reports/inbound-outbound` | 出入库统计（按时间维度 + 仓库） |
| GET | `/reports/stock-aging` | 库龄分析 |

### 18.1 `GET /reports/dashboard`

```jsonc
{
  "warehouseCount": 3,
  "locationCount": 9,
  "skuCount": 6,
  "customerCount": 4,
  "totalOnHandQty": 1520.0000,
  "totalAvailableQty": 1380.0000,
  "todayInboundOrders": 2,
  "todayOutboundOrders": 5,
  "pendingPutawayTasks": 3,
  "pendingPickTasks": 12,
  "stocktakeInProgress": 1,
  "expiringSoonSkuCount": 2      // 30 天内到期
}
```

### 18.2 `GET /reports/inventory-summary`

参数：`warehouseId`、`category`、`groupBy`（`warehouse` \| `category` \| `sku`）。

```jsonc
{
  "totalSkuCount": 6,
  "totalOnHandQty": 1520.0000,
  "totalLockedQty": 140.0000,
  "rows": [
    {
      "groupKey": "1", "groupName": "华东中心仓",
      "skuCount": 5, "onHandQty": 1200.0000,
      "lockedQty": 140.0000, "availableQty": 1060.0000
    }
  ]
}
```

### 18.3 `GET /reports/inbound-outbound`

参数：`warehouseId`、`startDate`、`endDate`、`granularity`（`day` \| `week` \| `month`）。

```jsonc
{
  "rows": [
    {
      "period": "2026-10-15",
      "inboundQty": 500.0000, "inboundOrders": 3,
      "outboundQty": 320.0000, "outboundOrders": 8
    }
  ],
  "totalInboundQty": 1520.0000,
  "totalOutboundQty": 980.0000
}
```

### 18.4 `GET /reports/stock-aging`

按**最后一次入库时间**距今天数分桶。

参数：`warehouseId`、`category`。

```jsonc
{
  "rows": [
    { "bucket": "0-30天",   "skuCount": 3, "onHandQty": 800.0000,  "value": 10800.00 },
    { "bucket": "31-60天",  "skuCount": 2, "onHandQty": 520.0000,  "value": 7020.00 },
    { "bucket": "61-90天",  "skuCount": 1, "onHandQty": 200.0000,  "value": 2700.00 },
    { "bucket": "90天以上", "skuCount": 0, "onHandQty": 0.0000,     "value": 0.00 }
  ],
  "totalValue": 20520.00
}
```

> `value` 需要单价。本版本 SKU 无成本字段，**先按数量计算并把 `value` 固定返回 0**，
> 前端展示数量即可。不要为了填这个字段去改 SKU 表。

---

## 19. 补充错误码

在第一部分错误码表基础上新增：

| code | HTTP | 含义 |
|---|---|---|
| 40004 | 400 | 状态流转不合法（如对已取消的单据收货） |
| 40005 | 400 | 盘点明细未录全就结束 |
| 40903 | 409 | 超收或库存不足，message 必须写清缺多少 |
| 40904 | 409 | 库位不允许负库存 |
| 40905 | 409 | 单据状态不允许该操作（如已发货订单不能取消） |
| 40906 | 409 | 序列号重复或状态不允许 |
