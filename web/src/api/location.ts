import request from './request'
import type { PageResult } from '@/types/common'
import type { Location, LocationPayload, LocationQuery } from '@/types/location'

export const locationApi = {
  page(query: LocationQuery): Promise<PageResult<Location>> {
    return request.get('/locations', { params: query }) as unknown as Promise<PageResult<Location>>
  },
  detail(id: number): Promise<Location> {
    return request.get(`/locations/${id}`) as unknown as Promise<Location>
  },
  create(payload: LocationPayload): Promise<Location> {
    return request.post('/locations', payload) as unknown as Promise<Location>
  },
  update(id: number, payload: LocationPayload): Promise<Location> {
    return request.put(`/locations/${id}`, payload) as unknown as Promise<Location>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/locations/${id}`) as unknown as Promise<null>
  },
  /** 后端 batch-delete 接收裸 id 数组（契约未定义该请求体，见 README 已知待确认项） */
  batchRemove(ids: number[]): Promise<null> {
    return request.post('/locations/batch-delete', ids) as unknown as Promise<null>
  },
}
