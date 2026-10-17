import request from './request'
import type { PageResult } from '@/types/common'
import type { Warehouse, WarehousePayload, WarehouseQuery } from '@/types/warehouse'

export const warehouseApi = {
  page(query: WarehouseQuery): Promise<PageResult<Warehouse>> {
    return request.get('/warehouses', { params: query }) as unknown as Promise<PageResult<Warehouse>>
  },
  detail(id: number): Promise<Warehouse> {
    return request.get(`/warehouses/${id}`) as unknown as Promise<Warehouse>
  },
  create(payload: WarehousePayload): Promise<Warehouse> {
    return request.post('/warehouses', payload) as unknown as Promise<Warehouse>
  },
  update(id: number, payload: WarehousePayload): Promise<Warehouse> {
    return request.put(`/warehouses/${id}`, payload) as unknown as Promise<Warehouse>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/warehouses/${id}`) as unknown as Promise<null>
  },
  /** 后端 batch-delete 接收裸 id 数组（契约未定义该请求体，见 README 已知待确认项） */
  batchRemove(ids: number[]): Promise<null> {
    return request.post('/warehouses/batch-delete', ids) as unknown as Promise<null>
  },
}
