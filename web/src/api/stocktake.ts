import request from './request'
import type { PageResult } from '@/types/common'
import type {
  StocktakeApprovePayload,
  StocktakeCountPayload,
  StocktakeCreatePayload,
  StocktakeOrder,
  StocktakeOrderDetail,
  StocktakeOrderQuery,
} from '@/types/stocktake'

/** 盘点单，契约 §17 */
export const stocktakeApi = {
  page(query: StocktakeOrderQuery): Promise<PageResult<StocktakeOrder>> {
    return request.get('/stocktake-orders', { params: query }) as unknown as Promise<
      PageResult<StocktakeOrder>
    >
  },
  detail(id: number): Promise<StocktakeOrderDetail> {
    return request.get(`/stocktake-orders/${id}`) as unknown as Promise<StocktakeOrderDetail>
  },
  /** 创建并按范围内在库库存生成明细 */
  create(payload: StocktakeCreatePayload): Promise<StocktakeOrderDetail> {
    return request.post('/stocktake-orders', payload) as unknown as Promise<StocktakeOrderDetail>
  },
  /** 仅草稿态可删除 */
  remove(id: number): Promise<null> {
    return request.delete(`/stocktake-orders/${id}`) as unknown as Promise<null>
  },
  /** 开始盘点 → 进行中 */
  start(id: number): Promise<StocktakeOrderDetail> {
    return request.post(`/stocktake-orders/${id}/start`) as unknown as Promise<StocktakeOrderDetail>
  },
  /** 录入实盘，没货要显式录 0；一次可提交多条 */
  count(id: number, payload: StocktakeCountPayload): Promise<StocktakeOrderDetail> {
    return request.post(`/stocktake-orders/${id}/count`, payload) as unknown as Promise<
      StocktakeOrderDetail
    >
  },
  /** 结束盘点 → 无差异直接完成，有差异转待审批 */
  finish(id: number): Promise<StocktakeOrderDetail> {
    return request.post(`/stocktake-orders/${id}/finish`) as unknown as Promise<StocktakeOrderDetail>
  },
  /** 审批并调账 */
  approve(id: number, payload: StocktakeApprovePayload): Promise<StocktakeOrderDetail> {
    return request.post(`/stocktake-orders/${id}/approve`, payload) as unknown as Promise<
      StocktakeOrderDetail
    >
  },
  cancel(id: number): Promise<StocktakeOrderDetail> {
    return request.post(`/stocktake-orders/${id}/cancel`) as unknown as Promise<StocktakeOrderDetail>
  },
}
