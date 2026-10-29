import request from './request'
import type { PageResult } from '@/types/common'
import type { Zone, ZonePayload, ZoneQuery } from '@/types/zone'

export const zoneApi = {
  page(query: ZoneQuery): Promise<PageResult<Zone>> {
    return request.get('/zones', { params: query }) as unknown as Promise<PageResult<Zone>>
  },
  detail(id: number): Promise<Zone> {
    return request.get(`/zones/${id}`) as unknown as Promise<Zone>
  },
  create(payload: ZonePayload): Promise<Zone> {
    return request.post('/zones', payload) as unknown as Promise<Zone>
  },
  update(id: number, payload: ZonePayload): Promise<Zone> {
    return request.put(`/zones/${id}`, payload) as unknown as Promise<Zone>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/zones/${id}`) as unknown as Promise<null>
  },
}
