import type { BaseEntity, PageQuery, StatusFlag } from './common'

/** 客户，契约 §7 */
export interface Customer extends BaseEntity {
  code: string
  name: string
  contactName: string | null
  contactPhone: string | null
  contactEmail: string | null
  address: string | null
  status: StatusFlag
  remark: string | null
}

export interface CustomerPayload {
  code: string
  name: string
  contactName?: string | null
  contactPhone?: string | null
  contactEmail?: string | null
  address?: string | null
  status?: StatusFlag
  remark?: string | null
}

export interface CustomerQuery extends PageQuery {}
