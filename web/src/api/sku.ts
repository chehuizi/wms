import request from './request'
import type { PageResult } from '@/types/common'
import type { Sku, SkuPayload, SkuQuery } from '@/types/sku'

export const skuApi = {
  page(query: SkuQuery): Promise<PageResult<Sku>> {
    return request.get('/skus', { params: query }) as unknown as Promise<PageResult<Sku>>
  },
  detail(id: number): Promise<Sku> {
    return request.get(`/skus/${id}`) as unknown as Promise<Sku>
  },
  create(payload: SkuPayload): Promise<Sku> {
    return request.post('/skus', payload) as unknown as Promise<Sku>
  },
  update(id: number, payload: SkuPayload): Promise<Sku> {
    return request.put(`/skus/${id}`, payload) as unknown as Promise<Sku>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/skus/${id}`) as unknown as Promise<null>
  },
  /** 后端 batch-delete 接收裸 id 数组（契约未定义该请求体，见 README 已知待确认项） */
  batchRemove(ids: number[]): Promise<null> {
    return request.post('/skus/batch-delete', ids) as unknown as Promise<null>
  },
  /** 去重后的分类字符串数组 */
  categories(): Promise<string[]> {
    return request.get('/skus/categories') as unknown as Promise<string[]>
  },
}
