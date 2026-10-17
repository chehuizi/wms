import type { BaseEntity, PageQuery, StatusFlag } from './common'

/** 库位类型枚举，契约 §5 */
export const LOCATION_TYPE = {
  1: '存储位',
  2: '收货暂存',
  3: '发货暂存',
  4: '不良品',
  5: '退货位',
} as const

export type LocationTypeValue = keyof typeof LOCATION_TYPE

export const LOCATION_TYPE_OPTIONS = Object.entries(LOCATION_TYPE).map(([value, label]) => ({
  value: Number(value) as LocationTypeValue,
  label,
}))

/** 库位，契约 §5 */
export interface Location extends BaseEntity {
  warehouseId: number
  /** 所属仓库名称（仅响应返回） */
  warehouseName: string
  code: string
  zone: string
  locationType: LocationTypeValue
  capacity: number | null
  allowNegative: StatusFlag
  status: StatusFlag
  remark: string | null
}

export interface LocationPayload {
  warehouseId: number
  code: string
  zone: string
  locationType?: LocationTypeValue
  /** 不限制时不传 / null */
  capacity?: number | null
  allowNegative?: StatusFlag
  status?: StatusFlag
  remark?: string | null
}

export interface LocationQuery extends PageQuery {
  warehouseId?: number
  locationType?: LocationTypeValue
  zone?: string
}
