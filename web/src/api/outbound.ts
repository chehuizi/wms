import request from './request'
import type { PageResult } from '@/types/common'
import type {
  Allocation,
  OutboundOrder,
  OutboundOrderDetail,
  OutboundOrderPayload,
  OutboundOrderQuery,
  PickPayload,
  PickTask,
  PickTaskQuery,
  Shipment,
  ShipPayload,
  Wave,
  WaveCreatePayload,
  WaveQuery,
} from '@/types/outbound'

/** 出库订单，契约 §16.1 / §16.5 / §16.6 */
export const outboundApi = {
  page(query: OutboundOrderQuery): Promise<PageResult<OutboundOrder>> {
    return request.get('/outbound-orders', { params: query }) as unknown as Promise<
      PageResult<OutboundOrder>
    >
  },
  detail(id: number): Promise<OutboundOrderDetail> {
    return request.get(`/outbound-orders/${id}`) as unknown as Promise<OutboundOrderDetail>
  },
  create(payload: OutboundOrderPayload): Promise<OutboundOrderDetail> {
    return request.post('/outbound-orders', payload) as unknown as Promise<OutboundOrderDetail>
  },
  update(id: number, payload: OutboundOrderPayload): Promise<OutboundOrderDetail> {
    return request.put(`/outbound-orders/${id}`, payload) as unknown as Promise<OutboundOrderDetail>
  },
  remove(id: number): Promise<null> {
    return request.delete(`/outbound-orders/${id}`) as unknown as Promise<null>
  },
  submit(id: number): Promise<OutboundOrderDetail> {
    return request.post(`/outbound-orders/${id}/submit`) as unknown as Promise<OutboundOrderDetail>
  },
  /** 分配库存（锁定）。可用量不足返回 40903，message 里写清缺多少 */
  allocate(id: number): Promise<OutboundOrderDetail> {
    return request.post(`/outbound-orders/${id}/allocate`) as unknown as Promise<OutboundOrderDetail>
  },
  /** 不走波次，直接生成拣货任务 */
  createPickTasks(id: number): Promise<OutboundOrderDetail> {
    return request.post(`/outbound-orders/${id}/pick-tasks`) as unknown as Promise<OutboundOrderDetail>
  },
  releaseAllocation(id: number): Promise<OutboundOrderDetail> {
    return request.post(`/outbound-orders/${id}/release-allocation`) as unknown as Promise<
      OutboundOrderDetail
    >
  },
  cancel(id: number): Promise<OutboundOrderDetail> {
    return request.post(`/outbound-orders/${id}/cancel`) as unknown as Promise<OutboundOrderDetail>
  },
  ship(id: number, payload: ShipPayload): Promise<Shipment> {
    return request.post(`/outbound-orders/${id}/ship`, payload) as unknown as Promise<Shipment>
  },
  shipments(id: number): Promise<Shipment[]> {
    return request.get(`/outbound-orders/${id}/shipments`) as unknown as Promise<Shipment[]>
  },
  allocations(id: number): Promise<Allocation[]> {
    return request.get(`/outbound-orders/${id}/allocations`) as unknown as Promise<Allocation[]>
  },
  pickTasks(id: number): Promise<PickTask[]> {
    return request.get(`/outbound-orders/${id}/pick-tasks`) as unknown as Promise<PickTask[]>
  },
}

/** 波次，契约 §16.3 */
export const waveApi = {
  page(query: WaveQuery): Promise<PageResult<Wave>> {
    return request.get('/waves', { params: query }) as unknown as Promise<PageResult<Wave>>
  },
  create(payload: WaveCreatePayload): Promise<Wave> {
    return request.post('/waves', payload) as unknown as Promise<Wave>
  },
  /** 只能加入 status = 3 已分配的订单 */
  addOrders(id: number, outboundIds: number[]): Promise<Wave> {
    return request.post(`/waves/${id}/orders`, { outboundIds }) as unknown as Promise<Wave>
  },
  /** 释放波次 → 按库位编码升序生成拣货任务 */
  release(id: number): Promise<Wave> {
    return request.post(`/waves/${id}/release`) as unknown as Promise<Wave>
  },
}

/** 拣货任务，契约 §16.4 */
export const pickTaskApi = {
  page(query: PickTaskQuery): Promise<PageResult<PickTask>> {
    return request.get('/pick-tasks', { params: query }) as unknown as Promise<PageResult<PickTask>>
  },
  pick(id: number, payload: PickPayload): Promise<PickTask> {
    return request.post(`/pick-tasks/${id}/pick`, payload) as unknown as Promise<PickTask>
  },
  cancel(id: number): Promise<PickTask> {
    return request.post(`/pick-tasks/${id}/cancel`) as unknown as Promise<PickTask>
  },
}
