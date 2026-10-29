import type { BaseEntity, PageQuery, StatusFlag } from './common'

/** 库区，所属仓库下的分区。 */
export interface Zone extends BaseEntity {
  warehouseId: number
  /** 所属仓库名称（仅响应返回） */
  warehouseName: string
  code: string
  name: string
  status: StatusFlag
  remark: string | null
}

/** 新建 / 编辑库区请求体。 */
export interface ZonePayload {
  warehouseId: number
  code: string
  name: string
  remark?: string | null
}

/** 库区分页查询参数。 */
export interface ZoneQuery extends PageQuery {
  warehouseId?: number
}
