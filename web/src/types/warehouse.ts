import type { BaseEntity, PageQuery, StatusFlag } from './common'

/** 仓库，契约 §4 */
export interface Warehouse extends BaseEntity {
  code: string
  name: string
  address: string | null
  contactName: string | null
  contactPhone: string | null
  status: StatusFlag
  remark: string | null
}

/** 新建 / 整单更新请求体 */
export interface WarehousePayload {
  code: string
  name: string
  address?: string | null
  contactName?: string | null
  contactPhone?: string | null
  status?: StatusFlag
  remark?: string | null
}

export interface WarehouseQuery extends PageQuery {}
