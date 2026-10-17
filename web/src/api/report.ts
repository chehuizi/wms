import request from './request'
import type { Dashboard, InboundOutbound, InventorySummary, StockAging } from '@/types/report'

/** 报表，全部只读，契约 §18 */
export const reportApi = {
  dashboard(): Promise<Dashboard> {
    return request.get('/reports/dashboard') as unknown as Promise<Dashboard>
  },
  inventorySummary(params: {
    warehouseId?: number
    category?: string
    groupBy?: string
  }): Promise<InventorySummary> {
    return request.get('/reports/inventory-summary', { params }) as unknown as Promise<InventorySummary>
  },
  inboundOutbound(params: {
    warehouseId?: number
    /** yyyy-MM-dd */
    startDate?: string
    endDate?: string
    /** day | week | month */
    granularity?: string
  }): Promise<InboundOutbound> {
    return request.get('/reports/inbound-outbound', { params }) as unknown as Promise<InboundOutbound>
  },
  /** value 字段本版固定返回 0（SKU 无成本字段），前端只画数量 */
  stockAging(params: { warehouseId?: number; category?: string }): Promise<StockAging> {
    return request.get('/reports/stock-aging', { params }) as unknown as Promise<StockAging>
  },
}
