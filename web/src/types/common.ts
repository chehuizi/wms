/**
 * 通用类型：与 docs/API_CONTRACT.md §1-§3 对齐
 */

/** 统一响应包络 */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** 分页结构 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 状态：1=启用 0=停用；布尔型字段契约里也是 0/1 */
export type StatusFlag = 0 | 1

/** 分页查询公共参数 */
export interface PageQuery {
  page: number
  size: number
  keyword?: string
  /** 不传 = 全部 */
  status?: StatusFlag
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

/** 通用实体字段 */
export interface BaseEntity {
  id: number
  createdAt: string
  updatedAt: string
}
