import { computed, ref } from 'vue'
import { warehouseApi } from '@/api/warehouse'
import { locationApi } from '@/api/location'
import { skuApi } from '@/api/sku'
import { customerApi } from '@/api/customer'
import type { Location, LocationTypeValue } from '@/types/location'
import type { Warehouse } from '@/types/warehouse'
import type { Sku } from '@/types/sku'
import type { Customer } from '@/types/customer'

/**
 * 下拉选项的共享加载器。
 *
 * 入库 / 出库 / 盘点 / 库存四个模块都要用「仓库、库位、SKU、客户」下拉，
 * 各自再请求一遍既浪费也没必要，所以这里做模块级单例缓存：
 * 首次调用时拉一次（一次性取全量启用数据），之后直接命中。
 *
 * 主数据量级是几十到几百条，一次性取比按仓库分页拉更简单可靠；
 * 如果将来主数据上万，这里改成按 warehouseId 分段拉取即可，调用方无感。
 */

/** 后端 size 上限 200（见 BaseQuery.getResolvedSize） */
const ALL_SIZE = 200

const warehouses = ref<Warehouse[]>([])
const locations = ref<Location[]>([])
const skus = ref<Sku[]>([])
const customers = ref<Customer[]>([])

const loadedFlags = {
  warehouses: false,
  locations: false,
  skus: false,
  customers: false,
}

export function useOptions() {
  async function ensure(kind: keyof typeof loadedFlags) {
    if (loadedFlags[kind]) return
    const params = { page: 1, size: ALL_SIZE, status: 1 } as const
    try {
      if (kind === 'warehouses') {
        const res = await warehouseApi.page({ ...params })
        warehouses.value = res?.list ?? []
      } else if (kind === 'locations') {
        const res = await locationApi.page({ ...params })
        locations.value = res?.list ?? []
      } else if (kind === 'skus') {
        const res = await skuApi.page({ ...params })
        skus.value = res?.list ?? []
      } else {
        const res = await customerApi.page({ ...params })
        customers.value = res?.list ?? []
      }
      loadedFlags[kind] = true
    } catch {
      // 拦截器已提示，选项保持空列表，页面其他部分仍可用
    }
  }

  /** 下拉里只展示名称，编码跟在后面更好认 */
  function warehouseLabel(row: Warehouse) {
    return `${row.name}（${row.code}）`
  }
  function locationLabel(row: Location) {
    return `${row.code}${row.zone ? ` · ${row.zone}区` : ''}`
  }
  function skuLabel(row: Sku) {
    return `${row.code} ${row.name}`
  }
  function customerLabel(row: Customer) {
    return `${row.name}（${row.code}）`
  }

  /** 某仓库下指定类型的启用库位，用于收货暂存位 / 存储位选择 */
  function locationsOf(warehouseId: number | undefined | null, locationType?: LocationTypeValue) {
    return locations.value.filter((loc) => {
      if (loc.status !== 1) return false
      if (warehouseId && loc.warehouseId !== warehouseId) return false
      if (locationType && loc.locationType !== locationType) return false
      return true
    })
  }

  const skuById = computed(() => new Map(skus.value.map((s) => [s.id, s])))

  /** 收货时要判断 SKU 是否开序列号管理，决定要不要填序列号 */
  function isSerialManaged(skuId: number | undefined | null) {
    if (!skuId) return false
    return skuById.value.get(skuId)?.serialManaged === 1
  }

  function isBatchManaged(skuId: number | undefined | null) {
    if (!skuId) return false
    return skuById.value.get(skuId)?.batchManaged === 1
  }

  return {
    warehouses,
    locations,
    skus,
    customers,
    loadWarehouses: () => ensure('warehouses'),
    loadLocations: () => ensure('locations'),
    loadSkus: () => ensure('skus'),
    loadCustomers: () => ensure('customers'),
    warehouseLabel,
    locationLabel,
    skuLabel,
    customerLabel,
    locationsOf,
    isSerialManaged,
    isBatchManaged,
  }
}
