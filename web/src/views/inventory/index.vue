<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ============ 库存台账 ============ -->
      <el-tab-pane label="库存台账" name="ledger">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              placeholder="SKU 编码 / 名称 / 条码"
              clearable
              style="width: 190px"
              @keyup.enter="searchLedger"
              @clear="searchLedger"
            />
          </el-form-item>
          <el-form-item label="仓库">
            <el-select
              v-model="query.warehouseId"
              placeholder="全部"
              clearable
              style="width: 170px"
              @change="onLedgerWarehouseChange"
            >
              <el-option
                v-for="w in warehouses"
                :key="w.id"
                :label="warehouseLabel(w)"
                :value="w.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="库位">
            <el-select
              v-model="query.locationId"
              placeholder="全部"
              clearable
              filterable
              style="width: 170px"
            >
              <el-option
                v-for="loc in locationsOf(query.warehouseId)"
                :key="loc.id"
                :label="locationLabel(loc)"
                :value="loc.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分类">
            <el-select
              v-model="query.category"
              placeholder="全部"
              clearable
              style="width: 140px"
            >
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
          <el-form-item label="批次号">
            <el-input
              v-model="query.batchNo"
              placeholder="精确匹配"
              clearable
              style="width: 140px"
              @keyup.enter="searchLedger"
              @clear="searchLedger"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select
              v-model="query.status"
              placeholder="全部"
              clearable
              style="width: 120px"
            >
              <el-option v-for="o in INVENTORY_STATUS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="只看">
            <el-checkbox v-model="query.onlyAvailable" @change="searchLedger">有库存的</el-checkbox>
            <el-checkbox v-model="query.includeLockedZero" @change="searchLedger">
              含可用为 0
            </el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchLedger">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetLedger">重置</el-button>
            <el-button type="warning" :icon="EditPen" plain @click="openAdjust(null)">
              手工新增库存
            </el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loadingLedger"
          :data="rowsLedger"
          border
          stripe
          row-key="id"
          show-summary
          :summary-method="ledgerSummary"
          sum-text="本页合计"
          empty-text="暂无库存数据"
        >
          <template #empty>
            <el-empty class="table-empty" description="暂无库存数据" :image-size="80" />
          </template>

          <el-table-column prop="warehouseName" label="仓库" min-width="110" show-overflow-tooltip />
          <el-table-column prop="locationCode" label="库位" width="110">
            <template #default="{ row }">
              <span>{{ row.locationCode }}</span>
              <el-tag v-if="row.zone" size="small" effect="plain" class="zone-tag">{{ row.zone }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU" width="110">
            <template #default="{ row }">{{ row.skuCode }}</template>
          </el-table-column>
          <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column prop="batchNo" label="批次" width="110">
            <template #default="{ row }">{{ row.batchNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="serialNo" label="序列号" width="130">
            <template #default="{ row }">{{ row.serialNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="onHandQty" label="在库量" width="110" align="right">
            <template #default="{ row }">{{ formatQty(row.onHandQty) }}</template>
          </el-table-column>
          <el-table-column prop="lockedQty" label="锁定量" width="100" align="right">
            <template #default="{ row }">
              <span :class="{ 'qty-locked': Number(row.lockedQty) > 0 }">
                {{ formatQty(row.lockedQty) }}
              </span>
            </template>
          </el-table-column>
          <!-- 可用量 = 在库量 - 锁定量，是履约真正能用的量，视觉上重点突出 -->
          <el-table-column prop="availableQty" label="可用量" width="120" align="right">
            <template #default="{ row }">
              <span class="qty-available" :class="availableClass(row.availableQty)">
                {{ formatQty(row.availableQty) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="INVENTORY_STATUS" />
            </template>
          </el-table-column>
          <el-table-column prop="expireDate" label="有效期至" width="110">
            <template #default="{ row }">{{ row.expireDate || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAdjust(row)">手工调整</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pageLedger"
            v-model:page-size="sizeLedger"
            :total="totalLedger"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onLedgerPageChange"
            @size-change="onLedgerSizeChange"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 库存流水 ============ -->
      <el-tab-pane label="库存流水" name="movements">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="mvQuery.keyword"
              placeholder="SKU 编码 / 名称"
              clearable
              style="width: 180px"
              @keyup.enter="searchMovements"
              @clear="searchMovements"
            />
          </el-form-item>
          <el-form-item label="仓库">
            <el-select v-model="mvQuery.warehouseId" placeholder="全部" clearable style="width: 160px">
              <el-option
                v-for="w in warehouses"
                :key="w.id"
                :label="warehouseLabel(w)"
                :value="w.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="流水类型">
            <el-select
              v-model="mvQuery.movementType"
              placeholder="全部"
              clearable
              style="width: 140px"
            >
              <el-option
                v-for="o in MOVEMENT_TYPES"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="业务单号">
            <el-input
              v-model="mvQuery.bizNo"
              placeholder="如 IN2026… / OUT2026…"
              clearable
              style="width: 180px"
              @keyup.enter="searchMovements"
              @clear="searchMovements"
            />
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="mvRange"
              type="datetimerange"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DDTHH:mm:ssZ"
              style="width: 360px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchMovements">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetMovements">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" class="movement-tip">
          流水只增不改不删（契约 §10.6），是账实一致的审计依据，不提供任何编辑入口。
        </el-alert>

        <el-table
          v-loading="loadingMovements"
          :data="rowsMovements"
          border
          stripe
          row-key="id"
          empty-text="暂无流水记录"
        >
          <template #empty>
            <el-empty class="table-empty" description="暂无流水记录" :image-size="80" />
          </template>

          <el-table-column prop="movementNo" label="流水号" width="160" />
          <el-table-column prop="movementType" label="类型" width="100" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.movementType" :options="MOVEMENT_TYPES" />
            </template>
          </el-table-column>
          <el-table-column prop="bizNo" label="业务单号" width="160">
            <template #default="{ row }">{{ row.bizNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="warehouseName" label="仓库" min-width="100" show-overflow-tooltip />
          <el-table-column prop="locationCode" label="库位" width="100" />
          <el-table-column prop="skuCode" label="SKU" width="110" />
          <el-table-column prop="skuName" label="商品名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="qtyChange" label="在库变动" width="110" align="right">
            <template #default="{ row }">
              <span :class="changeClass(row.qtyChange)">
                {{ Number(row.qtyChange) > 0 ? '+' : '' }}{{ formatQty(row.qtyChange) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="lockedChange" label="锁定变动" width="110" align="right">
            <template #default="{ row }">
              <span :class="changeClass(row.lockedChange)">
                {{ Number(row.lockedChange) > 0 ? '+' : '' }}{{ formatQty(row.lockedChange) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="在库快照" width="150" align="right">
            <template #default="{ row }">
              {{ formatQty(row.beforeQty) }} → {{ formatQty(row.afterQty) }}
            </template>
          </el-table-column>
          <el-table-column prop="batchNo" label="批次" width="110">
            <template #default="{ row }">{{ row.batchNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="90">
            <template #default="{ row }">{{ row.operator || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pageMovements"
            v-model:page-size="sizeMovements"
            :total="totalMovements"
            :page-sizes="[20, 50, 100, 200]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onMovementPageChange"
            @size-change="onMovementSizeChange"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 序列号台账 ============ -->
      <el-tab-pane label="序列号台账" name="serials">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="snQuery.keyword"
              placeholder="序列号 / SKU"
              clearable
              style="width: 200px"
              @keyup.enter="searchSerials"
              @clear="searchSerials"
            />
          </el-form-item>
          <el-form-item label="仓库">
            <el-select v-model="snQuery.warehouseId" placeholder="全部" clearable style="width: 160px">
              <el-option
                v-for="w in warehouses"
                :key="w.id"
                :label="warehouseLabel(w)"
                :value="w.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="snQuery.status" placeholder="全部" clearable style="width: 130px">
              <el-option
                v-for="o in SERIAL_STATUS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchSerials">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetSerials">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loadingSerials"
          :data="rowsSerials"
          border
          stripe
          row-key="id"
          empty-text="暂无序列号"
        >
          <template #empty>
            <el-empty class="table-empty" description="暂无序列号记录" :image-size="80" />
          </template>

          <el-table-column prop="serialNo" label="序列号" width="180" />
          <el-table-column prop="skuCode" label="SKU" width="110" />
          <el-table-column prop="skuName" label="商品名称" min-width="170" show-overflow-tooltip />
          <el-table-column prop="warehouseName" label="仓库" min-width="110" show-overflow-tooltip />
          <el-table-column prop="locationCode" label="库位" width="110" />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="SERIAL_STATUS" />
            </template>
          </el-table-column>
          <el-table-column prop="inBoundAt" label="入库时间" width="170">
            <template #default="{ row }">{{ row.inBoundAt || '-' }}</template>
          </el-table-column>
          <el-table-column prop="outBoundAt" label="出库时间" width="170">
            <template #default="{ row }">{{ row.outBoundAt || '-' }}</template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pageSerials"
            v-model:page-size="sizeSerials"
            :total="totalSerials"
            :page-sizes="[20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onSerialPageChange"
            @size-change="onSerialSizeChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 手工调整 / 新增库存行 -->
    <el-dialog
      v-model="adjustVisible"
      :title="adjustForm.inventoryId === null ? '手工新增库存' : '手工调整库存'"
      width="620px"
      destroy-on-close
      @closed="onAdjustClosed"
    >
      <el-alert
        v-if="adjustForm.inventoryId === null"
        type="warning"
        :closable="false"
        title="新增库存行会直接抬高在库量，请确认是期初建账 / 盘盈补录，而不是正常入库"
        class="adjust-alert"
      />
      <el-alert
        v-else-if="adjustContext"
        type="info"
        :closable="false"
        class="adjust-alert"
      >
        <template #title>
          {{ adjustContext.skuCode }} {{ adjustContext.skuName }} ·
          {{ adjustContext.locationCode }} · 当前可用 {{ formatQty(adjustContext.availableQty) }}
          （在库 {{ formatQty(adjustContext.onHandQty) }} / 锁定
          {{ formatQty(adjustContext.lockedQty) }}）
        </template>
      </el-alert>

      <el-form ref="adjustRef" :model="adjustForm" :rules="adjustRules" label-width="96px">
        <template v-if="adjustForm.inventoryId === null">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="仓库" prop="warehouseId">
                <el-select
                  v-model="adjustForm.warehouseId"
                  placeholder="请选择"
                  style="width: 100%"
                  @change="onAdjustWarehouseChange"
                >
                  <el-option
                    v-for="w in warehouses"
                    :key="w.id"
                    :label="warehouseLabel(w)"
                    :value="w.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="库位" prop="locationId">
                <el-select
                  v-model="adjustForm.locationId"
                  placeholder="请选择"
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="loc in locationsOf(adjustForm.warehouseId)"
                    :key="loc.id"
                    :label="locationLabel(loc)"
                    :value="loc.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SKU" prop="skuId">
                <el-select v-model="adjustForm.skuId" placeholder="请选择" filterable style="width: 100%">
                  <el-option
                    v-for="s in skus"
                    :key="s.id"
                    :label="skuLabel(s)"
                    :value="s.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="批次">
                <el-input v-model="adjustForm.batchNo" placeholder="选填" maxlength="64" />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="序列号">
                <el-input
                  v-model="adjustForm.serialNo"
                  placeholder="选填"
                  maxlength="64"
                  :disabled="!isSerialManaged(adjustForm.skuId)"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="调整数量" prop="qtyChange">
              <el-input-number
                v-model="adjustForm.qtyChange"
                :step="1"
                :precision="0"
                controls-position="right"
                style="width: 100%"
              />
              <div class="field-hint">正数=盘盈，负数=盘亏，不能为 0</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="操作人" prop="operator">
              <el-input v-model="adjustForm.operator" placeholder="如 张三" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="调整原因" prop="reason">
              <el-input
                v-model="adjustForm.reason"
                type="textarea"
                :rows="2"
                placeholder="如 货损 / 错收 / 期初建账"
                maxlength="255"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustSubmitting" @click="onAdjustSubmit">
          确认调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type TableColumnCtx } from 'element-plus'
import { EditPen, RefreshLeft, Search } from '@element-plus/icons-vue'
import EnumTag from '@/components/EnumTag.vue'
import { useTablePage } from '@/composables/useTablePage'
import { useOptions } from '@/composables/useOptions'
import { inventoryApi } from '@/api/inventory'
import { skuApi } from '@/api/sku'
import { formatQty } from '@/types/enum'
import {
  INVENTORY_STATUS,
  MOVEMENT_TYPES,
  SERIAL_STATUS,
  type Inventory,
  type InventoryAdjustPayload,
  type InventoryQuery,
  type MovementQuery,
  type Serial,
  type SerialQuery,
  type StockMovement,
} from '@/types/inventory'

const {
  warehouses,
  skus,
  loadWarehouses,
  loadLocations,
  loadSkus,
  warehouseLabel,
  locationLabel,
  skuLabel,
  locationsOf,
  isSerialManaged,
} = useOptions()

const activeTab = ref('ledger')

/* ---------------- 库存台账 ---------------- */

const query = reactive<{
  keyword: string
  warehouseId?: number
  locationId?: number
  category?: string
  batchNo: string
  status?: number
  onlyAvailable: boolean
  includeLockedZero: boolean
}>({
  keyword: '',
  warehouseId: undefined,
  locationId: undefined,
  category: undefined,
  batchNo: '',
  status: undefined,
  onlyAvailable: true,
  includeLockedZero: true,
})

const categories = ref<string[]>([])

const {
  rows: rowsLedger,
  total: totalLedger,
  loading: loadingLedger,
  page: pageLedger,
  size: sizeLedger,
  load: loadLedger,
  search: searchLedger,
  onPageChange: onLedgerPageChange,
  onSizeChange: onLedgerSizeChange,
} = useTablePage<Inventory, InventoryQuery>({
  api: { page: (q: InventoryQuery) => inventoryApi.page(q) },
  entityName: '库存',
  buildQuery: (): InventoryQuery => ({
    page: pageLedger.value,
    size: sizeLedger.value,
    keyword: query.keyword || undefined,
    warehouseId: query.warehouseId,
    locationId: query.locationId,
    category: query.category,
    batchNo: query.batchNo || undefined,
    status: query.status,
    onlyAvailable: query.onlyAvailable,
    includeLockedZero: query.includeLockedZero,
  }),
})

function onLedgerWarehouseChange() {
  // 换仓库后原库位不再属于该仓库，清掉避免查出空结果
  query.locationId = undefined
  searchLedger()
}

function onResetLedger() {
  query.keyword = ''
  query.warehouseId = undefined
  query.locationId = undefined
  query.category = undefined
  query.batchNo = ''
  query.status = undefined
  query.onlyAvailable = true
  query.includeLockedZero = true
  searchLedger()
}

function availableClass(qty: number) {
  const num = Number(qty)
  if (num > 0) return 'qty-available-positive'
  if (num < 0) return 'qty-available-negative'
  return ''
}

/** 合计只统计当前页数据，sum-text 已注明，避免被误读成全量合计 */
function ledgerSummary(param: { columns: TableColumnCtx<Inventory>[]; data: Inventory[] }) {
  const { columns, data } = param
  const onHand = data.reduce((sum, r) => sum + Number(r.onHandQty || 0), 0)
  const locked = data.reduce((sum, r) => sum + Number(r.lockedQty || 0), 0)
  const available = data.reduce((sum, r) => sum + Number(r.availableQty || 0), 0)
  return columns.map((col) => {
    if (col.property === 'onHandQty') return formatQty(onHand)
    if (col.property === 'lockedQty') return formatQty(locked)
    if (col.property === 'availableQty') return formatQty(available)
    return ''
  })
}

/* ---------------- 库存流水 ---------------- */

const mvQuery = reactive<{
  keyword: string
  warehouseId?: number
  movementType?: number
  bizNo: string
}>({
  keyword: '',
  warehouseId: undefined,
  movementType: undefined,
  bizNo: '',
})

/** el-date-picker 的 datetimerange 值，[开始, 结束] */
const mvRange = ref<[string, string] | null>(null)

const {
  rows: rowsMovements,
  total: totalMovements,
  loading: loadingMovements,
  page: pageMovements,
  size: sizeMovements,
  load: loadMovements,
  search: searchMovements,
  onPageChange: onMovementPageChange,
  onSizeChange: onMovementSizeChange,
} = useTablePage<StockMovement, MovementQuery>({
  api: { page: (q: MovementQuery) => inventoryApi.movements(q) },
  entityName: '流水',
  buildQuery: (): MovementQuery => ({
    page: pageMovements.value,
    size: sizeMovements.value,
    keyword: mvQuery.keyword || undefined,
    warehouseId: mvQuery.warehouseId,
    movementType: mvQuery.movementType,
    bizNo: mvQuery.bizNo || undefined,
    startTime: mvRange.value?.[0],
    endTime: mvRange.value?.[1],
  }),
})

function changeClass(value: number | null | undefined) {
  const num = Number(value || 0)
  if (num > 0) return 'qty-positive'
  if (num < 0) return 'qty-negative'
  return 'qty-zero'
}

function onResetMovements() {
  mvQuery.keyword = ''
  mvQuery.warehouseId = undefined
  mvQuery.movementType = undefined
  mvQuery.bizNo = ''
  mvRange.value = null
  searchMovements()
}

/* ---------------- 序列号台账 ---------------- */

const snQuery = reactive<{
  keyword: string
  warehouseId?: number
  status?: number
}>({
  keyword: '',
  warehouseId: undefined,
  status: undefined,
})

const {
  rows: rowsSerials,
  total: totalSerials,
  loading: loadingSerials,
  page: pageSerials,
  size: sizeSerials,
  load: loadSerials,
  search: searchSerials,
  onPageChange: onSerialPageChange,
  onSizeChange: onSerialSizeChange,
} = useTablePage<Serial, SerialQuery>({
  api: { page: (q: SerialQuery) => inventoryApi.serials(q) },
  entityName: '序列号',
  buildQuery: (): SerialQuery => ({
    page: pageSerials.value,
    size: sizeSerials.value,
    keyword: snQuery.keyword || undefined,
    warehouseId: snQuery.warehouseId,
    status: snQuery.status,
  }),
})

function onResetSerials() {
  snQuery.keyword = ''
  snQuery.warehouseId = undefined
  snQuery.status = undefined
  searchSerials()
}

/* ---------------- 手工调整 ---------------- */

const adjustVisible = ref(false)
const adjustSubmitting = ref(false)
const adjustRef = ref<FormInstance>()
const adjustContext = ref<Inventory | null>(null)

const adjustForm = reactive<InventoryAdjustPayload>({
  inventoryId: null,
  warehouseId: null,
  locationId: null,
  skuId: null,
  batchNo: null,
  serialNo: null,
  qtyChange: 0,
  reason: '',
  operator: '',
})

const adjustRules = computed<FormRules<InventoryAdjustPayload>>(() => ({
  warehouseId:
    adjustForm.inventoryId === null
      ? [{ required: true, message: '请选择仓库', trigger: 'change' }]
      : [],
  locationId:
    adjustForm.inventoryId === null
      ? [{ required: true, message: '请选择库位', trigger: 'change' }]
      : [],
  skuId:
    adjustForm.inventoryId === null ? [{ required: true, message: '请选择 SKU', trigger: 'change' }] : [],
  qtyChange: [
    { required: true, message: '请输入调整数量', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (Number(value) === 0) callback(new Error('调整数量不能为 0'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  operator: [
    { required: true, message: '请输入操作人', trigger: 'blur' },
    { max: 64, message: '最多 64 个字符', trigger: 'blur' },
  ],
  reason: [
    { required: true, message: '请输入调整原因', trigger: 'blur' },
    { max: 255, message: '最多 255 个字符', trigger: 'blur' },
  ],
}))

function resetAdjustForm() {
  adjustRef.value?.clearValidate()
  adjustContext.value = null
  Object.assign(adjustForm, {
    inventoryId: null,
    warehouseId: null,
    locationId: null,
    skuId: null,
    batchNo: null,
    serialNo: null,
    qtyChange: 0,
    reason: '',
    operator: '',
  })
}

function openAdjust(row: Inventory | null) {
  resetAdjustForm()
  if (row) {
    adjustForm.inventoryId = row.id
    adjustContext.value = row
  }
  adjustVisible.value = true
}

function onAdjustClosed() {
  resetAdjustForm()
}

function onAdjustWarehouseChange() {
  adjustForm.locationId = null
}

async function onAdjustSubmit() {
  const valid = await adjustRef.value?.validate().catch(() => false)
  if (!valid) return
  if (Number(adjustForm.qtyChange) === 0) {
    ElMessage.warning('调整数量不能为 0')
    return
  }
  adjustSubmitting.value = true
  try {
    const payload: InventoryAdjustPayload = {
      inventoryId: adjustForm.inventoryId,
      warehouseId: adjustForm.warehouseId ?? null,
      locationId: adjustForm.locationId ?? null,
      skuId: adjustForm.skuId ?? null,
      batchNo: adjustForm.batchNo?.trim() || null,
      serialNo: adjustForm.serialNo?.trim() || null,
      qtyChange: Number(adjustForm.qtyChange),
      reason: adjustForm.reason.trim(),
      operator: adjustForm.operator.trim(),
    }
    await inventoryApi.adjust(payload)
    ElMessage.success('调整成功，已写入库存流水')
    adjustVisible.value = false
    await loadLedger()
  } catch {
    /* 拦截器已提示 */
  } finally {
    adjustSubmitting.value = false
  }
}

/* ---------------- 生命周期 ---------------- */

const loadedOnce = {
  ledger: false,
  movements: false,
  serials: false,
}

async function onTabChange(name: string | number) {
  // 三个 Tab 各自分页，首次切入时才拉数据
  if (name === 'movements' && !loadedOnce.movements) {
    loadedOnce.movements = true
    await loadMovements()
  } else if (name === 'serials' && !loadedOnce.serials) {
    loadedOnce.serials = true
    await loadSerials()
  }
}

onMounted(async () => {
  await Promise.all([loadWarehouses(), loadLocations(), loadSkus()])
  try {
    categories.value = (await skuApi.categories()) ?? []
  } catch {
    categories.value = []
  }
  loadedOnce.ledger = true
  await loadLedger()
})
</script>

<style scoped>
.zone-tag {
  margin-left: 4px;
}

.qty-available {
  font-weight: 700;
  font-size: 14px;
}

.qty-available-positive {
  color: #16a34a;
}

.qty-available-negative {
  color: #f56c6c;
}

.qty-locked {
  color: #e6a23c;
}

.qty-positive {
  color: #16a34a;
  font-weight: 600;
}

.qty-negative {
  color: #f56c6c;
  font-weight: 600;
}

.qty-zero {
  color: #909399;
}

.movement-tip {
  margin-bottom: 12px;
}

.adjust-alert {
  margin-bottom: 16px;
}

.field-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
