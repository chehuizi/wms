import type { BaseEntity } from './common'
import type { BizPageQuery, EnumOption } from './enum'

/** 库存台账，契约 §13 */
export interface Inventory extends BaseEntity {
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  zone: string | null
  skuId: number
  skuCode: string
  skuName: string
  category: string | null
  unit: string | null
  batchNo: string | null
  serialNo: string | null
  producedDate: string | null
  expireDate: string | null
  onHandQty: number
  lockedQty: number
  /** 计算字段 availableQty = onHandQty - lockedQty，后端算好返回，不落库 */
  availableQty: number
  /** 1=正常 0=冻结 */
  status: 0 | 1
}

export interface InventoryQuery extends BizPageQuery {
  warehouseId?: number
  locationId?: number
  skuId?: number
  category?: string
  batchNo?: string
  /** 只看在库量 > 0 的 */
  onlyAvailable?: boolean
  /** 是否包含已锁完（可用量为 0）的行，默认 true */
  includeLockedZero?: boolean
}

/** 手工调整请求，契约 §13 POST /inventories/adjust，流水 movementType = 9 */
export interface InventoryAdjustPayload {
  /** 已存在传 id；为 null 时按下面的键新建一条库存行 */
  inventoryId: number | null
  warehouseId?: number | null
  locationId?: number | null
  skuId?: number | null
  batchNo?: string | null
  serialNo?: string | null
  /** 正数=盘盈，负数=盘亏，0 会被后端拒绝 */
  qtyChange: number
  reason: string
  operator: string
}

/** 库存流水，契约 §13 */
export interface StockMovement {
  id: number
  movementNo: string
  movementType: number
  movementTypeName: string
  bizType: string | null
  bizNo: string | null
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  skuId: number
  skuCode: string
  skuName: string
  batchNo: string | null
  serialNo: string | null
  qtyChange: number
  lockedChange: number
  beforeQty: number
  afterQty: number
  operator: string | null
  remark: string | null
  createdAt: string
}

export interface MovementQuery extends BizPageQuery {
  warehouseId?: number
  locationId?: number
  skuId?: number
  movementType?: number
  bizType?: string
  bizNo?: string
  startTime?: string
  endTime?: string
}

/** 流水类型，契约 §12.3 */
export const MOVEMENT_TYPES: EnumOption[] = [
  { value: 1, label: '入库收货', type: 'success' },
  { value: 2, label: '出库发货', type: 'warning' },
  { value: 3, label: '盘盈', type: 'success' },
  { value: 4, label: '盘亏', type: 'danger' },
  { value: 5, label: '移库出', type: 'info' },
  { value: 6, label: '移库入', type: 'primary' },
  { value: 7, label: '锁定', type: 'warning' },
  { value: 8, label: '解锁', type: 'info' },
  { value: 9, label: '手工调整', type: 'primary' },
]

/** 序列号台账，契约 §14 */
export interface Serial {
  id: number
  serialNo: string
  skuId: number
  skuCode: string
  skuName: string
  warehouseId: number
  warehouseName: string
  locationId: number
  locationCode: string
  /** 1=在库 2=已锁定 3=已出库 4=已退货 */
  status: number
  inBoundAt: string | null
  outBoundAt: string | null
  createdAt: string
}

export interface SerialQuery extends BizPageQuery {
  skuId?: number
  warehouseId?: number
}

export const SERIAL_STATUS: EnumOption[] = [
  { value: 1, label: '在库', type: 'success' },
  { value: 2, label: '已锁定', type: 'warning' },
  { value: 3, label: '已出库', type: 'info' },
  { value: 4, label: '已退货', type: 'danger' },
]

/** 库存行状态，契约 §13 */
export const INVENTORY_STATUS: EnumOption[] = [
  { value: 1, label: '正常', type: 'success' },
  { value: 0, label: '冻结', type: 'info' },
]
