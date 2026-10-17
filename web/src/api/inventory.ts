import request from './request'
import type { PageResult } from '@/types/common'
import type {
  Inventory,
  InventoryAdjustPayload,
  InventoryQuery,
  MovementQuery,
  Serial,
  SerialQuery,
  StockMovement,
} from '@/types/inventory'

/** 库存台账 / 库存流水 / 序列号，契约 §13-§14 */
export const inventoryApi = {
  page(query: InventoryQuery): Promise<PageResult<Inventory>> {
    return request.get('/inventories', { params: query }) as unknown as Promise<PageResult<Inventory>>
  },
  detail(id: number): Promise<Inventory> {
    return request.get(`/inventories/${id}`) as unknown as Promise<Inventory>
  },
  /** 手工调整，写 movementType = 9 流水 */
  adjust(payload: InventoryAdjustPayload): Promise<Inventory> {
    return request.post('/inventories/adjust', payload) as unknown as Promise<Inventory>
  },
  movements(query: MovementQuery): Promise<PageResult<StockMovement>> {
    return request.get('/inventories/movements', { params: query }) as unknown as Promise<
      PageResult<StockMovement>
    >
  },
  serials(query: SerialQuery): Promise<PageResult<Serial>> {
    return request.get('/serials', { params: query }) as unknown as Promise<PageResult<Serial>>
  },
}
