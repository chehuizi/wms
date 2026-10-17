import type { BaseEntity, PageQuery, StatusFlag } from './common'

/** 商品 SKU，契约 §6 */
export interface Sku extends BaseEntity {
  code: string
  name: string
  category: string | null
  spec: string | null
  unit: string | null
  barcode: string | null
  batchManaged: StatusFlag
  serialManaged: StatusFlag
  /** NULL = 无保质期 */
  shelfLifeDays: number | null
  weight: number | null
  volume: number | null
  length: number | null
  width: number | null
  height: number | null
  status: StatusFlag
  remark: string | null
}

export interface SkuPayload {
  code: string
  name: string
  category?: string | null
  spec?: string | null
  unit?: string | null
  barcode?: string | null
  batchManaged?: StatusFlag
  serialManaged?: StatusFlag
  shelfLifeDays?: number | null
  weight?: number | null
  volume?: number | null
  length?: number | null
  width?: number | null
  height?: number | null
  status?: StatusFlag
  remark?: string | null
}

export interface SkuQuery extends PageQuery {
  category?: string
  batchManaged?: StatusFlag
  serialManaged?: StatusFlag
}
