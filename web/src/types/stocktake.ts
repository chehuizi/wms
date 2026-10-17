import type { BaseEntity } from './common'
import type { BizPageQuery, EnumOption } from './enum'

/** 盘点类型，契约 §12.4 */
export const STOCKTAKE_TYPES: EnumOption[] = [
  { value: 1, label: '全盘', type: 'danger' },
  { value: 2, label: '循环盘', type: 'warning' },
  { value: 3, label: '抽盘', type: 'primary' },
]

/** 盘点单状态，契约 §12.4 */
export const STOCKTAKE_STATUS: EnumOption[] = [
  { value: 1, label: '草稿', type: 'info' },
  { value: 2, label: '进行中', type: 'primary' },
  { value: 3, label: '待审批', type: 'warning' },
  { value: 4, label: '已完成', type: 'success' },
  { value: 5, label: '已取消', type: 'danger' },
]

/** 盘点单状态主链，用于 el-steps（5 已取消是旁路） */
export const STOCKTAKE_STEPS: EnumOption[] = STOCKTAKE_STATUS.filter((o) => o.value !== 5)

/** 盘点明细状态，契约 §12.4 */
export const STOCKTAKE_ITEM_STATUS: EnumOption[] = [
  { value: 1, label: '待盘', type: 'info' },
  { value: 2, label: '已盘', type: 'warning' },
  { value: 3, label: '待审批', type: 'warning' },
  { value: 4, label: '已调账', type: 'success' },
]

export interface StocktakeOrder extends BaseEntity {
  orderNo: string
  warehouseId: number
  warehouseName: string
  /** 1=全盘 2=循环盘 3=抽盘 */
  stocktakeType: number
  status: number
  /** 全盘可空；循环盘/抽盘必填 */
  scopeZone: string | null
  totalLocations: number
  countedLocations: number
  totalItems: number
  countedItems: number
  diffItems: number
  /** 差异合计，盘盈盘亏相抵后的净值 */
  diffQty: number
  startedAt: string | null
  finishedAt: string | null
  approvedAt: string | null
  approver: string | null
  remark: string | null
}

export interface StocktakeItem {
  id: number
  stocktakeId: number
  locationId: number
  locationCode: string
  skuId: number
  skuCode: string
  skuName: string
  unit: string | null
  batchNo: string | null
  inventoryId: number
  /** 创建时的在库量快照 */
  bookQty: number
  /** 未录入时后端返回 null，前端要求显式录入，0 表示没货 */
  countedQty: number | null
  /** diffQty = countedQty - bookQty */
  diffQty: number | null
  status: number
  countedBy: string | null
  countedAt: string | null
  remark: string | null
}

/** 差异调整记录 */
export interface StocktakeAdjust {
  id: number
  stocktakeId: number
  stocktakeItemId: number
  inventoryId: number
  skuId: number
  skuCode: string
  skuName: string
  locationId: number
  locationCode: string
  diffQty: number
  approvedBy: string | null
  approvedAt: string | null
  remark: string | null
}

export interface StocktakeOrderDetail extends StocktakeOrder {
  items: StocktakeItem[]
  adjusts: StocktakeAdjust[]
}

export interface StocktakeOrderQuery extends BizPageQuery {
  warehouseId?: number
  stocktakeType?: number
}

/** 创建盘点单，契约 §17 */
export interface StocktakeCreatePayload {
  warehouseId: number
  stocktakeType: number
  /** stocktakeType != 1 时必填，否则后端返回 40001 */
  scopeZone?: string | null
  remark?: string | null
}

/**
 * 录入实盘，契约 §17。
 * countedQty 必填且 >= 0 —— 没货要显式录 0。
 * 已录入的 item 重复提交视为覆盖，所以前端一次提交多条。
 */
export interface StocktakeCountPayload {
  items: Array<{ itemId: number; countedQty: number; remark?: string | null }>
  operator: string
}

/** 审批并调账，契约 §17 */
export interface StocktakeApprovePayload {
  approver: string
  remark?: string | null
}
