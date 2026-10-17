<template>
  <div class="page-card">
    <!-- ============ 盘点单列表 ============ -->
    <el-form :inline="true" class="page-toolbar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="盘点单号"
          clearable
          style="width: 180px"
          @keyup.enter="search"
          @clear="search"
        />
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="query.warehouseId" placeholder="全部" clearable style="width: 170px">
          <el-option
            v-for="w in warehouses"
            :key="w.id"
            :label="warehouseLabel(w)"
            :value="w.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.stocktakeType" placeholder="全部" clearable style="width: 130px">
          <el-option v-for="o in STOCKTAKE_TYPES" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option
            v-for="o in STOCKTAKE_STATUS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <el-button type="primary" :icon="Plus" plain @click="openCreate">创建盘点单</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" border stripe row-key="id" empty-text="暂无盘点单">
      <template #empty>
        <el-empty class="table-empty" description="暂无盘点单" :image-size="80" />
      </template>

      <el-table-column prop="orderNo" label="盘点单号" width="160" />
      <el-table-column prop="warehouseName" label="仓库" min-width="110" show-overflow-tooltip />
      <el-table-column prop="stocktakeType" label="类型" width="95" align="center">
        <template #default="{ row }">
          <EnumTag :value="row.stocktakeType" :options="STOCKTAKE_TYPES" />
        </template>
      </el-table-column>
      <el-table-column prop="scopeZone" label="范围库区" width="100" align="center">
        <template #default="{ row }">{{ row.scopeZone || '全仓' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="95" align="center">
        <template #default="{ row }">
          <EnumTag :value="row.status" :options="STOCKTAKE_STATUS" />
        </template>
      </el-table-column>
      <el-table-column label="盘点进度" min-width="180">
        <template #default="{ row }">
          <div class="progress-text">
            {{ row.countedItems }} / {{ row.totalItems }} 条 ·
            {{ row.countedLocations }} / {{ row.totalLocations }} 库位
          </div>
          <el-progress
            :percentage="progressOf(row.totalItems, row.countedItems)"
            :stroke-width="6"
            :show-text="false"
          />
        </template>
      </el-table-column>
      <el-table-column prop="diffItems" label="差异条数" width="90" align="center">
        <template #default="{ row }">
          <span :class="row.diffItems > 0 ? 'diff-warning' : ''">{{ row.diffItems }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="diffQty" label="差异合计" width="110" align="right">
        <template #default="{ row }">
          <span :class="diffClass(row.diffQty)">{{ signed(row.diffQty) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="approver" label="审批人" width="90">
        <template #default="{ row }">{{ row.approver || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="270" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">
            {{ row.status === 2 ? '录入' : '详情' }}
          </el-button>
          <el-button v-if="row.status === 1" link type="primary" @click="onStart(row)">开始</el-button>
          <el-button v-if="row.status === 2" link type="warning" @click="onFinish(row)">结束</el-button>
          <el-button v-if="row.status === 3" link type="success" @click="openApprove(row)">
            审批调账
          </el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- ============ 创建盘点单 ============ -->
    <el-dialog v-model="createVisible" title="创建盘点单" width="560px" destroy-on-close>
      <el-alert type="info" :closable="false" class="tip">
        创建时会抓取范围内所有启用库位下「在库量 &gt; 0」的库存行生成明细，并把当时的在库量记为账面数量。
      </el-alert>
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="createForm.warehouseId" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="warehouseLabel(w)"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型" prop="stocktakeType">
          <el-radio-group v-model="createForm.stocktakeType">
            <el-radio :value="1">全盘</el-radio>
            <el-radio :value="2">循环盘</el-radio>
            <el-radio :value="3">抽盘</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="范围库区" prop="scopeZone">
          <el-select
            v-model="createForm.scopeZone"
            :placeholder="createForm.stocktakeType === 1 ? '全盘可不选' : '循环盘 / 抽盘必填'"
            :disabled="createForm.stocktakeType === 1"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option v-for="z in zoneOptions" :key="z" :label="`${z} 区`" :value="z" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createForm.remark"
            type="textarea"
            :rows="2"
            placeholder="选填"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="onSubmitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- ============ 详情 / 录入抽屉 ============ -->
    <el-drawer v-model="detailVisible" :title="`盘点单 ${detail?.orderNo ?? ''}`" size="1080px">
      <div v-loading="loadingDetail" class="detail-body">
        <template v-if="detail">
          <BizSteps :steps="STOCKTAKE_STEPS" :options="STOCKTAKE_STATUS" :current="detail.status" />

          <el-descriptions :column="4" border size="small" class="detail-desc">
            <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="类型">
              <EnumTag :value="detail.stocktakeType" :options="STOCKTAKE_TYPES" />
            </el-descriptions-item>
            <el-descriptions-item label="范围库区">
              {{ detail.scopeZone || '全仓' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <EnumTag :value="detail.status" :options="STOCKTAKE_STATUS" />
            </el-descriptions-item>
            <el-descriptions-item label="明细 / 已盘">
              {{ detail.totalItems }} / {{ detail.countedItems }}
            </el-descriptions-item>
            <el-descriptions-item label="差异条数">{{ detail.diffItems }}</el-descriptions-item>
            <el-descriptions-item label="差异合计">
              <span :class="diffClass(detail.diffQty)">{{ signed(detail.diffQty) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="审批人">{{ detail.approver || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="4">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">
            盘点明细
            <div v-if="detail.status === 2" class="count-actions">
              <el-input
                v-model="countOperator"
                placeholder="盘点操作人"
                maxlength="64"
                style="width: 160px"
              />
              <el-button size="small" plain @click="fillByBook">按账面数填充</el-button>
              <el-button
                size="small"
                type="primary"
                :loading="countSubmitting"
                @click="onSubmitCount"
              >
                批量提交实盘（{{ pendingChanged }} 条）
              </el-button>
              <el-button size="small" type="warning" @click="onFinish(detail)">结束盘点</el-button>
            </div>
            <el-button
              v-else-if="detail.status === 3"
              size="small"
              type="success"
              @click="openApprove(detail)"
            >
              审批并调账
            </el-button>
          </div>

          <el-alert v-if="detail.status === 2" type="warning" :closable="false" class="tip">
            <template #title>
              实盘数量必须逐行显式录入，没货要填 0（留空视为未盘）。当前本地未录入
              {{ unCounted }} 行、未提交 {{ pendingChanged }} 行改动；未录满就点「结束盘点」会被后端拒绝（40005）。
            </template>
          </el-alert>

          <el-table :data="countRows" border size="small" empty-text="该范围内没有在库库存，未生成明细">
            <el-table-column type="index" label="#" width="46" align="center" />
            <el-table-column prop="locationCode" label="库位" width="110" />
            <el-table-column prop="skuCode" label="SKU" width="110" />
            <el-table-column prop="skuName" label="商品名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="batchNo" label="批次" width="110">
              <template #default="{ row }">{{ row.batchNo || '-' }}</template>
            </el-table-column>
            <el-table-column prop="bookQty" label="账面数量" width="110" align="right">
              <template #default="{ row }">{{ formatQty(row.bookQty) }}</template>
            </el-table-column>
            <el-table-column label="实盘数量" width="160">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.editCounted"
                  :min="0"
                  :step="1"
                  :precision="0"
                  :disabled="detail.status !== 2"
                  controls-position="right"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="差异" width="120" align="right">
              <template #default="{ row }">
                <span v-if="row.editCounted === null" class="diff-none">未录</span>
                <span v-else :class="diffClass(row.editCounted - row.bookQty)">
                  {{ signed(row.editCounted - row.bookQty) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="180">
              <template #default="{ row }">
                <el-input
                  v-model="row.editRemark"
                  :disabled="detail.status !== 2"
                  placeholder="差异原因，选填"
                  maxlength="255"
                />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <EnumTag :value="row.status" :options="STOCKTAKE_ITEM_STATUS" />
              </template>
            </el-table-column>
          </el-table>

          <template v-if="detail.status === 4">
            <div class="section-title">差异调账记录</div>
            <el-table :data="detail.adjusts" border size="small" empty-text="无差异，无需调账">
              <el-table-column prop="skuCode" label="SKU" width="110" />
              <el-table-column prop="locationCode" label="库位" width="110" />
              <el-table-column label="差异" width="120" align="right">
                <template #default="{ row }">
                  <span :class="diffClass(row.diffQty)">{{ signed(row.diffQty) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="approvedBy" label="审批人" width="100">
                <template #default="{ row }">{{ row.approvedBy || '-' }}</template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="180">
                <template #default="{ row }">{{ row.remark || '-' }}</template>
              </el-table-column>
            </el-table>
          </template>
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button v-if="detail?.status === 1" type="primary" @click="onStart(detail)">
            开始盘点
          </el-button>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- ============ 审批调账 ============ -->
    <el-dialog v-model="approveVisible" title="审批并调账" width="520px" destroy-on-close>
      <el-alert type="warning" :closable="false" class="tip">
        审批会在一个事务里按差异调整真实库存：盘盈写 3 号流水，盘亏写 4 号流水。该操作不可撤销。
      </el-alert>
      <el-form ref="approveRef" :model="approveForm" :rules="approveRules" label-width="90px">
        <el-form-item label="待调账">
          <span class="static-text">
            {{ approveTarget?.diffItems ?? 0 }} 条有差异，
            差异合计
            <span :class="diffClass(approveTarget?.diffQty)">{{ signed(approveTarget?.diffQty) }}</span>
          </span>
        </el-form-item>
        <el-form-item label="审批人" prop="approver">
          <el-input v-model="approveForm.approver" placeholder="如 周八" maxlength="64" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.remark"
            type="textarea"
            :rows="2"
            placeholder="选填"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="success" :loading="approveSubmitting" @click="onSubmitApprove">
          确认审批调账
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import BizSteps from '@/components/BizSteps.vue'
import EnumTag from '@/components/EnumTag.vue'
import { useTablePage } from '@/composables/useTablePage'
import { useOptions } from '@/composables/useOptions'
import { stocktakeApi } from '@/api/stocktake'
import { formatQty } from '@/types/enum'
import {
  STOCKTAKE_ITEM_STATUS,
  STOCKTAKE_STATUS,
  STOCKTAKE_STEPS,
  STOCKTAKE_TYPES,
  type StocktakeItem,
  type StocktakeOrder,
  type StocktakeOrderDetail,
  type StocktakeOrderQuery,
} from '@/types/stocktake'

const { warehouses, locations, loadWarehouses, loadLocations, warehouseLabel } = useOptions()

/* ---------------- 列表 ---------------- */

const query = reactive<{
  keyword: string
  warehouseId?: number
  stocktakeType?: number
  status?: number
}>({
  keyword: '',
  warehouseId: undefined,
  stocktakeType: undefined,
  status: undefined,
})

const {
  rows,
  total,
  loading,
  page,
  size,
  load,
  search,
  onPageChange,
  onSizeChange,
  handleDelete,
} = useTablePage<StocktakeOrder, StocktakeOrderQuery>({
  api: {
    page: (q: StocktakeOrderQuery) => stocktakeApi.page(q),
    remove: (id: number) => stocktakeApi.remove(id),
  },
  entityName: '盘点单',
  labelOf: (row) => row.orderNo,
  buildQuery: (): StocktakeOrderQuery => ({
    page: page.value,
    size: size.value,
    keyword: query.keyword || undefined,
    warehouseId: query.warehouseId,
    stocktakeType: query.stocktakeType,
    status: query.status,
  }),
})

function onReset() {
  query.keyword = ''
  query.warehouseId = undefined
  query.stocktakeType = undefined
  query.status = undefined
  search()
}

function progressOf(total: number, counted: number) {
  const t = Number(total || 0)
  if (t <= 0) return 0
  return Math.min(100, Math.round((Number(counted || 0) / t) * 100))
}

/** 差异配色：盘盈绿、盘亏红、无差异灰 */
function diffClass(value: number | null | undefined) {
  const num = Number(value || 0)
  if (num > 0) return 'diff-gain'
  if (num < 0) return 'diff-loss'
  return 'diff-zero'
}

function signed(value: number | null | undefined) {
  const num = Number(value || 0)
  return `${num > 0 ? '+' : ''}${formatQty(num)}`
}

/* ---------------- 创建 ---------------- */

const createVisible = ref(false)
const createSubmitting = ref(false)
const createRef = ref<FormInstance>()

const createForm = reactive<{
  warehouseId: number | null
  stocktakeType: number
  scopeZone: string | null
  remark: string
}>({
  warehouseId: null,
  stocktakeType: 1,
  scopeZone: null,
  remark: '',
})

/** 库区选项直接来自现有库位，避免用户填到不存在的库区导致生成 0 条明细 */
const zoneOptions = computed(() => {
  const set = new Set<string>()
  locations.value.forEach((loc) => {
    if (loc.zone) set.add(loc.zone)
  })
  return Array.from(set).sort()
})

const createRules = computed<FormRules>(() => ({
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  stocktakeType: [{ required: true, message: '请选择盘点类型', trigger: 'change' }],
  // 循环盘 / 抽盘必须限定库区，否则后端返回 40001
  scopeZone:
    createForm.stocktakeType === 1
      ? []
      : [{ required: true, message: '循环盘 / 抽盘必须选择范围库区', trigger: 'change' }],
}))

function openCreate() {
  Object.assign(createForm, {
    warehouseId: null,
    stocktakeType: 1,
    scopeZone: null,
    remark: '',
  })
  createVisible.value = true
}

watch(
  () => createForm.stocktakeType,
  (type) => {
    if (type === 1) createForm.scopeZone = null
  },
)

async function onSubmitCreate() {
  const valid = await createRef.value?.validate().catch(() => false)
  if (!valid || !createForm.warehouseId) return
  createSubmitting.value = true
  try {
    const created = await stocktakeApi.create({
      warehouseId: createForm.warehouseId,
      stocktakeType: createForm.stocktakeType,
      scopeZone: createForm.scopeZone,
      remark: createForm.remark.trim() || null,
    })
    createVisible.value = false
    if (created?.totalItems === 0) {
      ElMessage.warning('盘点单已创建，但范围内没有在库库存，未生成明细')
    } else {
      ElMessage.success(`盘点单已创建，生成 ${created.totalItems} 条明细`)
    }
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    createSubmitting.value = false
  }
}

/* ---------------- 详情 / 录入 ---------------- */

interface CountRow {
  item: StocktakeItem
  editCounted: number | null
  editRemark: string
  /** 后端已录的值，用于判断是否改动过 */
  original: number | null
}

const detailVisible = ref(false)
const loadingDetail = ref(false)
const detail = ref<StocktakeOrderDetail | null>(null)
const countRows = ref<CountRow[]>([])
const countOperator = ref('')
const countSubmitting = ref(false)

function buildCountRows(items: StocktakeItem[]): CountRow[] {
  return (items ?? []).map((item) => ({
    item,
    editCounted: item.countedQty === null ? null : Number(item.countedQty),
    editRemark: item.remark ?? '',
    original: item.countedQty === null ? null : Number(item.countedQty),
  }))
}

async function openDetail(row: StocktakeOrder) {
  detailVisible.value = true
  loadingDetail.value = true
  try {
    detail.value = await stocktakeApi.detail(row.id)
    countRows.value = buildCountRows(detail.value.items)
    countOperator.value = ''
  } catch {
    detail.value = null
    countRows.value = []
  } finally {
    loadingDetail.value = false
  }
}

/** 还没录或录完又改过的行 */
const pendingChanged = computed(
  () => countRows.value.filter((r) => r.editCounted !== null && r.editCounted !== r.original).length,
)

const unCounted = computed(() => countRows.value.filter((r) => r.editCounted === null).length)

function fillByBook() {
  countRows.value.forEach((r) => {
    if (r.editCounted === null) r.editCounted = Number(r.item.bookQty)
  })
}

async function onSubmitCount() {
  if (detail.value === null) return
  const changed = countRows.value.filter((r) => r.editCounted !== null)
  if (changed.length === 0) {
    ElMessage.warning('还没有录入任何实盘数量')
    return
  }
  if (!countOperator.value.trim()) {
    ElMessage.warning('请填写盘点操作人')
    return
  }
  countSubmitting.value = true
  try {
    // 一次提交所有已录行，已录过的行后端按覆盖处理
    detail.value = await stocktakeApi.count(detail.value.id, {
      operator: countOperator.value.trim(),
      items: changed.map((r) => ({
        itemId: r.item.id,
        countedQty: Number(r.editCounted),
        remark: r.editRemark.trim() || null,
      })),
    })
    countRows.value = buildCountRows(detail.value.items)
    ElMessage.success(`已提交 ${changed.length} 条实盘`)
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    countSubmitting.value = false
  }
}

async function onStart(row: StocktakeOrder) {
  try {
    await stocktakeApi.start(row.id)
    ElMessage.success('已开始盘点')
    detailVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

/** 以服务端已提交的状态为准判断是否录满（本地改了没提交的不算） */
const serverUnCounted = computed(() => {
  if (!detail.value) return 0
  return Math.max(0, Number(detail.value.totalItems || 0) - Number(detail.value.countedItems || 0))
})

async function onFinish(row: StocktakeOrder) {
  const unsaved = pendingChanged.value
  if (serverUnCounted.value > 0) {
    const ok = await confirmBox(
      `还有 ${serverUnCounted.value} 条明细未录入实盘，后端会拒绝结束（40005）。` +
        (unsaved > 0 ? `另外你本地还有 ${unsaved} 条改动没有提交。` : '') +
        '确定要提交未录全的盘点吗？',
      '明细未录全',
      { confirmButtonText: '仍然提交' },
    )
    if (!ok) return
  } else {
    const ok = await confirmBox(`确定结束盘点单「${row.orderNo}」吗？结束后不可再录入。`, '结束盘点')
    if (!ok) return
  }
  try {
    await stocktakeApi.finish(row.id)
    ElMessage.success('盘点已结束')
    detailVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

/** ElMessageBox 确认框的布尔化封装，取消返回 false */
async function confirmBox(
  message: string,
  title: string,
  options: { confirmButtonText?: string } = {},
): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: options.confirmButtonText ?? '确定',
      cancelButtonText: '取消',
    })
    return true
  } catch {
    return false
  }
}

function onDelete(row: StocktakeOrder) {
  return handleDelete(row)
}

/* ---------------- 审批 ---------------- */

const approveVisible = ref(false)
const approveSubmitting = ref(false)
const approveRef = ref<FormInstance>()
const approveId = ref<number | null>(null)
const approveTarget = ref<StocktakeOrderDetail | null>(null)

const approveForm = reactive<{ approver: string; remark: string }>({ approver: '', remark: '' })

const approveRules: FormRules = {
  approver: [
    { required: true, message: '请输入审批人', trigger: 'blur' },
    { max: 64, message: '最多 64 个字符', trigger: 'blur' },
  ],
}

async function openApprove(row: StocktakeOrder) {
  approveId.value = row.id
  approveTarget.value = detail.value?.id === row.id ? detail.value : null
  Object.assign(approveForm, { approver: '', remark: '' })
  approveVisible.value = true
}

async function onSubmitApprove() {
  const valid = await approveRef.value?.validate().catch(() => false)
  if (!valid || approveId.value === null) return
  approveSubmitting.value = true
  try {
    await stocktakeApi.approve(approveId.value, {
      approver: approveForm.approver.trim(),
      remark: approveForm.remark.trim() || null,
    })
    ElMessage.success('审批完成，差异已调账')
    approveVisible.value = false
    detailVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    approveSubmitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadWarehouses(), loadLocations()])
  await load()
})
</script>

<style scoped>
.tip {
  margin-bottom: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin: 20px 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.count-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 400;
}

.progress-text {
  font-size: 12px;
  color: #606266;
  margin-bottom: 2px;
}

.diff-gain {
  color: #16a34a;
  font-weight: 600;
}

.diff-loss {
  color: #f56c6c;
  font-weight: 600;
}

.diff-zero {
  color: #909399;
}

.diff-none {
  color: #c0c4cc;
  font-style: italic;
}

.diff-warning {
  color: #e6a23c;
  font-weight: 600;
}

.detail-desc {
  margin-top: 12px;
}

.static-text {
  color: #606266;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
