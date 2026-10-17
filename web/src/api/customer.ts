import request from './request'
import type { PageResult } from '@/types/common'
import type { Customer, CustomerPayload, CustomerQuery } from '@/types/customer'

export const customerApi = {
  page(query: CustomerQuery): Promise<PageResult<Customer>> {
    return request.get('/customers', { params: query }) as unknown as Promise<PageResult<Customer>>
  },
  detail(id: number): Promise<Customer> {
    return request.get(`/customers/${id}`) as unknown as Promise<Customer>
  },
  create(payload: CustomerPayload): Promise<Customer> {
    return request.post('/customers', payload) as unknown as Promise<Customer>
  },
  update(id: number, payload: CustomerPayload): Promise<Customer> {
    return request.put(`/customers/${id}`, payload) as unknown as Promise<Customer>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/customers/${id}`) as unknown as Promise<null>
  },
  /** 后端 batch-delete 接收裸 id 数组（契约未定义该请求体，见 README 已知待确认项） */
  batchRemove(ids: number[]): Promise<null> {
    return request.post('/customers/batch-delete', ids) as unknown as Promise<null>
  },
}
