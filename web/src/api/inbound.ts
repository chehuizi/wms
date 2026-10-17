import request from './request'
import type { PageResult } from '@/types/common'
import type {
  InboundOrder,
  InboundOrderDetail,
  InboundOrderPayload,
  InboundOrderQuery,
  InboundReceipt,
  PutawayExecutePayload,
  PutawayTask,
  PutawayTaskQuery,
  ReceiptPayload,
} from '@/types/inbound'

/** 入库到货单 / 收货 / 上架任务，契约 §15 */
export const inboundApi = {
  page(query: InboundOrderQuery): Promise<PageResult<InboundOrder>> {
    return request.get('/inbound-orders', { params: query }) as unknown as Promise<
      PageResult<InboundOrder>
    >
  },
  detail(id: number): Promise<InboundOrderDetail> {
    return request.get(`/inbound-orders/${id}`) as unknown as Promise<InboundOrderDetail>
  },
  create(payload: InboundOrderPayload): Promise<InboundOrderDetail> {
    return request.post('/inbound-orders', payload) as unknown as Promise<InboundOrderDetail>
  },
  update(id: number, payload: InboundOrderPayload): Promise<InboundOrderDetail> {
    return request.put(`/inbound-orders/${id}`, payload) as unknown as Promise<InboundOrderDetail>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/inbound-orders/${id}`) as unknown as Promise<null>
  },
  submit(id: number): Promise<InboundOrderDetail> {
    return request.post(`/inbound-orders/${id}/submit`) as unknown as Promise<InboundOrderDetail>
  },
  cancel(id: number): Promise<InboundOrderDetail> {
    return request.post(`/inbound-orders/${id}/cancel`) as unknown as Promise<InboundOrderDetail>
  },
  /** 执行收货，收货明细逐条带 inboundItemId */
  receipt(id: number, payload: ReceiptPayload): Promise<InboundReceipt> {
    return request.post(`/inbound-orders/${id}/receipts`, payload) as unknown as Promise<InboundReceipt>
  },
  /** 该单的收货记录，非分页，直接返回数组 */
  receipts(id: number): Promise<InboundReceipt[]> {
    return request.get(`/inbound-orders/${id}/receipts`) as unknown as Promise<InboundReceipt[]>
  },
}

/** 上架任务，契约 §15.2 */
export const putawayApi = {
  page(query: PutawayTaskQuery): Promise<PageResult<PutawayTask>> {
    return request.get('/putaway-tasks', { params: query }) as unknown as Promise<PageResult<PutawayTask>>
  },
  /** 执行上架：源位减、目标位加，产生 5/6 两条流水 */
  execute(id: number, payload: PutawayExecutePayload): Promise<PutawayTask> {
    return request.post(`/putaway-tasks/${id}/execute`, payload) as unknown as Promise<PutawayTask>
  },
}
