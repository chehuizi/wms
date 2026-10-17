import type { PageQuery } from './common'

/** el-tag 的语义色 */
export type TagType = 'success' | 'info' | 'warning' | 'danger' | 'primary'

/**
 * 枚举选项，供 EnumTag.vue 展示、各处筛选下拉复用。
 * 业务模块的枚举集中在各自 types 文件里维护，契约 §12 是事实来源。
 */
export interface EnumOption {
  value: number
  label: string
  type?: TagType
}

/**
 * 业务单据分页查询公共参数。
 *
 * 注意：基础主数据的 status 是启用/停用（0|1），而业务单据的 status 是
 * 1-7 的多态流转（草稿/待收货/已取消…），两者语义不同。
 * 所以这里 Omit 掉 PageQuery.status 后重新声明为 number，
 * 避免把入库单的 status=4（已完成）误当成「启用」。
 */
export interface BizPageQuery extends Omit<PageQuery, 'status'> {
  /** 各模块含义不同，见契约 §12 对应小节 */
  status?: number
}

/** 把后端返回的数量按 4 位小数的口径展示，去掉无意义的尾零 */
export function formatQty(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-'
  const num = Number(value)
  if (Number.isNaN(num)) return '-'
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}
