import type { BaseEntity } from './common'
import type { BizPageQuery, EnumOption } from './enum'

/** 入库到货单状态，契约 §12.1 */
export const INBOUND_STATUS: EnumOption[] = [
  { value: 1, label: '草稿', type: 'info' },
  { value: 2, label: '待收货', type: 'primary' },
  { value: 3, label: '部分收货', type: 'warning' },
  { value: 4, label: '已完成', type: 'success' },
  { value: 5, label: '已取消', type: 'danger' },
]

/** 到货单状态流转的前四步，用于 el-steps 展示（5 已取消是旁路，不在主链上） */
export const INBOUND_STEPS: EnumOption[] = INBOUND_STATUS.filter((o) => o.value !== 5)

export interface InboundOrder extends BaseEntity {
  orderNo: string
  warehouseId: number
  warehouseName: string
  supplierCode: string | null
  supplierName: string | null
  status: number
  expectedAt: string | null
  finishedAt: string | null
  remark: string | null
  /** 明细条数 */
  itemCount: number
  expectedQty: number
  receivedQty: number
}

export interface InboundItem {
  id: number
  inboundId: number
  skuId: number
  skuCode: string
  skuName: string
  unit: string | null
  expectedQty: number
  receivedQty: number
}

/** 到货单详情 = 单头 + 明细 */
export interface InboundOrderDetail extends InboundOrder {
  items: InboundItem[]
}

export interface InboundReceiptItem {
  id: number
  receiptId: number
  inboundItemId: number
  skuId: number
  skuCode: string
  skuName: string
  batchNo: string | null
  serialNo: string | null
  producedDate: string | null
  expireDate: string | null
  qty: number
}

/** 收货记录，契约 §15.2 */
export interface InboundReceipt {
  id: number
  receiptNo: string
  inboundId: number
  locationId: number
  locationCode: string
  receivedAt: string
  operator: string | null
  remark: string | null
  items: InboundReceiptItem[]
}

/** 新建 / 修改到货单请求，契约 §15.1 */
export interface InboundOrderPayload {
  warehouseId: number
  supplierCode?: string | null
  supplierName?: string | null
  /** ISO 字符串，缺省不传 */
  expectedAt?: string | null
  remark?: string | null
  /** 至少 1 条，同一 SKU 只能出现一次（重复后端返回 40001） */
  items: Array<{ skuId: number; expectedQty: number }>
}

export interface InboundOrderQuery extends BizPageQuery {
  warehouseId?: number
  /** 后端按 created_at 过滤（见 InboundOrderMapper.pageWhere） */
  startTime?: string
  endTime?: string
}

/** 收货明细，契约 §15.2 */
export interface ReceiptItemPayload {
  inboundItemId: number
  batchNo?: string | null
  /** SKU 开序列号管理时必填；未开启时必须为空，否则后端报 40001 */
  serialNo?: string | null
  producedDate?: string | null
  expireDate?: string | null
  qty: number
}

export interface ReceiptPayload {
  /** 收货暂存位，必须是该仓库下 locationType=2 的启用库位 */
  locationId: number
  operator?: string | null
  remark?: string | null
  items: ReceiptItemPayload[]
}

/** 上架任务状态，契约 §12.4 */
export const PUTAWAY_STATUS: EnumOption[] = [
  { value: 1, label: '待上架', type: 'warning' },
  { value: 2, label: '已完成', type: 'success' },
  { value: 3, label: '已取消', type: 'danger' },
]

/** 上架任务，契约 §15.2 */
export interface PutawayTask {
  id: number
  taskNo: string
  receiptItemId: number
  inboundId: number
  inboundNo: string | null
  skuId: number
  skuCode: string
  skuName: string
  fromLocationId: number
  fromLocationCode: string
  toLocationId: number | null
  toLocationCode: string | null
  batchNo: string | null
  serialNo: string | null
  qty: number
  status: number
  operator: string | null
  executedAt: string | null
  createdAt: string
}

export interface PutawayTaskQuery extends BizPageQuery {
  inboundId?: number
  skuId?: number
}

/** 执行上架：toLocationId 必须是该仓库下 locationType=1 存储位的启用库位 */
export interface PutawayExecutePayload {
  toLocationId: number
  operator?: string | null
}
