import type { BaseEntity } from './common'
import type { BizPageQuery, EnumOption } from './enum'

/** 出库订单状态，契约 §12.2 */
export const OUTBOUND_STATUS: EnumOption[] = [
  { value: 1, label: '草稿', type: 'info' },
  { value: 2, label: '待分配', type: 'primary' },
  { value: 3, label: '已分配', type: 'primary' },
  { value: 4, label: '拣货中', type: 'warning' },
  { value: 5, label: '待发货', type: 'warning' },
  { value: 6, label: '已发货', type: 'success' },
  { value: 7, label: '已取消', type: 'danger' },
]

/** 订单状态主链，用于 el-steps（7 已取消是旁路） */
export const OUTBOUND_STEPS: EnumOption[] = OUTBOUND_STATUS.filter((o) => o.value !== 7)

export interface OutboundOrder extends BaseEntity {
  orderNo: string
  warehouseId: number
  warehouseName: string
  customerId: number | null
  customerName: string | null
  status: number
  /** 数值越大越优先 */
  priority: number
  expectedShipAt: string | null
  shippedAt: string | null
  remark: string | null
  itemCount: number
  orderedQty: number
  allocatedQty: number
  pickedQty: number
  shippedQty: number
}

export interface OutboundItem {
  id: number
  outboundId: number
  skuId: number
  skuCode: string
  skuName: string
  unit: string | null
  orderedQty: number
  allocatedQty: number
  pickedQty: number
  shippedQty: number
}

/** 分配记录，契约 §16.1 详情 */
export interface Allocation {
  id: number
  outboundId: number
  outboundItemId: number
  inventoryId: number
  warehouseId: number
  locationId: number
  locationCode: string
  skuId: number
  skuCode: string
  skuName: string
  batchNo: string | null
  allocatedQty: number
  pickedQty: number
  shippedQty: number
  status: number
}

export interface OutboundOrderDetail extends OutboundOrder {
  items: OutboundItem[]
  allocations: Allocation[]
}

/** 新建 / 修改出库订单请求，契约 §16.1 */
export interface OutboundOrderPayload {
  warehouseId: number
  customerId?: number | null
  priority?: number
  expectedShipAt?: string | null
  remark?: string | null
  items: Array<{ skuId: number; orderedQty: number }>
}

export interface OutboundOrderQuery extends BizPageQuery {
  warehouseId?: number
  customerId?: number
  /** 后端按 created_at 过滤（见 OutboundOrderMapper.pageWhere） */
  startTime?: string
  endTime?: string
}

/** 分配记录状态，契约 §12.4 */
export const ALLOCATION_STATUS: EnumOption[] = [
  { value: 1, label: '已锁定', type: 'primary' },
  { value: 2, label: '拣货中', type: 'warning' },
  { value: 3, label: '已拣货', type: 'success' },
  { value: 4, label: '已发货', type: 'success' },
  { value: 5, label: '已释放', type: 'info' },
]

/** 波次状态，契约 §12.4 */
export const WAVE_STATUS: EnumOption[] = [
  { value: 1, label: '创建中', type: 'info' },
  { value: 2, label: '已释放', type: 'primary' },
  { value: 3, label: '拣货中', type: 'warning' },
  { value: 4, label: '已完成', type: 'success' },
  { value: 5, label: '已关闭', type: 'danger' },
]

export interface Wave {
  id: number
  waveNo: string
  warehouseId: number
  warehouseName: string
  status: number
  orderCount: number
  releasedAt: string | null
  finishedAt: string | null
  operator: string | null
  createdAt: string
}

export interface WaveQuery extends BizPageQuery {
  warehouseId?: number
}

/** 创建波次，契约 §16.3 */
export interface WaveCreatePayload {
  warehouseId: number
  operator?: string | null
}

/** 拣货任务状态，契约 §12.4 */
export const PICK_STATUS: EnumOption[] = [
  { value: 1, label: '待拣货', type: 'warning' },
  { value: 2, label: '拣货中', type: 'primary' },
  { value: 3, label: '已完成', type: 'success' },
  { value: 4, label: '已取消', type: 'danger' },
]

/** 拣货任务，契约 §16.4 */
export interface PickTask {
  id: number
  taskNo: string
  waveId: number | null
  waveNo: string | null
  outboundId: number
  outboundNo: string | null
  allocationId: number
  skuId: number
  skuCode: string
  skuName: string
  fromLocationId: number
  fromLocationCode: string
  /** 释放波次时按库位编码升序递增 */
  sequence: number
  requiredQty: number
  pickedQty: number
  status: number
  operator: string | null
  pickedAt: string | null
  createdAt: string
}

export interface PickTaskQuery extends BizPageQuery {
  waveId?: number
  outboundId?: number
  skuId?: number
}

/** 拣货：pickedQty > 0 且 <= requiredQty，允许部分拣 */
export interface PickPayload {
  pickedQty: number
  operator: string
}

/** 发货记录，契约 §16.5 */
export interface Shipment {
  id: number
  shipmentNo: string
  outboundId: number
  carrier: string | null
  trackingNo: string | null
  shippedAt: string
  operator: string | null
  remark: string | null
}

/** 发货请求，契约 §16.5 */
export interface ShipPayload {
  carrier?: string | null
  trackingNo?: string | null
  operator: string
  remark?: string | null
}
