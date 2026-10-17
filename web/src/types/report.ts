/** 报表，契约 §18。全部只读，前端不做任何金额换算。 */

/** 看板汇总，契约 §18.1 */
export interface Dashboard {
  warehouseCount: number
  locationCount: number
  skuCount: number
  customerCount: number
  totalOnHandQty: number
  totalAvailableQty: number
  todayInboundOrders: number
  todayOutboundOrders: number
  pendingPutawayTasks: number
  pendingPickTasks: number
  stocktakeInProgress: number
  /** 30 天内到期 */
  expiringSoonSkuCount: number
}

/** 库存汇总行，契约 §18.2 */
export interface InventorySummaryRow {
  groupKey: string
  groupName: string
  skuCount: number
  onHandQty: number
  lockedQty: number
  availableQty: number
}

export interface InventorySummary {
  totalSkuCount: number
  totalOnHandQty: number
  totalLockedQty: number
  rows: InventorySummaryRow[]
}

export type SummaryGroupBy = 'warehouse' | 'category' | 'sku'

/** 出入库统计行，契约 §18.3 */
export interface InboundOutboundRow {
  period: string
  inboundQty: number
  inboundOrders: number
  outboundQty: number
  outboundOrders: number
}

export interface InboundOutbound {
  rows: InboundOutboundRow[]
  totalInboundQty: number
  totalOutboundQty: number
}

export type Granularity = 'day' | 'week' | 'month'

/** 库龄分析行，契约 §18.4 */
export interface StockAgingRow {
  /** 0-30天 / 31-60天 / 61-90天 / 90天以上 */
  bucket: string
  skuCount: number
  onHandQty: number
  /** 本版 SKU 无成本字段，后端固定返回 0，前端不展示金额 */
  value: number
}

export interface StockAging {
  rows: StockAgingRow[]
  totalValue: number
}
