<template>
  <div v-loading="loading" class="dashboard">
    <!-- ============ 顶部指标卡 ============ -->
    <div class="stat-grid">
      <div
        v-for="card in statCards"
        :key="card.label"
        class="stat-card"
        :class="{ clickable: card.to }"
        @click="card.to && router.push(card.to)"
      >
        <div class="stat-head">
          <span class="stat-label">{{ card.label }}</span>
          <el-icon class="stat-icon" :style="{ color: card.color, background: card.bg }">
            <component :is="card.icon" />
          </el-icon>
        </div>
        <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
        <div class="stat-hint">{{ card.hint }}</div>
      </div>
    </div>

    <!-- ============ 图表区 ============ -->
    <div class="chart-row">
      <div class="page-card chart-card">
        <div class="chart-head">
          <span class="chart-title">库存库龄分析</span>
          <div class="chart-actions">
            <el-select
              v-model="agingWarehouseId"
              placeholder="全部仓库"
              clearable
              style="width: 170px"
              @change="loadAging"
            >
              <el-option
                v-for="w in warehouses"
                :key="w.id"
                :label="warehouseLabel(w)"
                :value="w.id"
              />
            </el-select>
            <el-button :icon="Refresh" text @click="loadAging">刷新</el-button>
          </div>
        </div>
        <VChart :option="agingOption" autoresize style="height: 300px" />
        <div class="chart-note">
          按最后一次入库时间距今天数分桶。本版 SKU 没有成本字段，后端 amount 恒为 0，因此这里只画数量、不展示金额。
        </div>
      </div>

      <div class="page-card chart-card">
        <div class="chart-head">
          <span class="chart-title">出入库趋势</span>
          <div class="chart-actions">
            <el-radio-group v-model="granularity" size="small" @change="loadTrend">
              <el-radio-button value="day">按天</el-radio-button>
              <el-radio-button value="week">按周</el-radio-button>
              <el-radio-button value="month">按月</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-model="trendRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 240px"
              @change="loadTrend"
            />
            <el-select
              v-model="trendWarehouseId"
              placeholder="全部仓库"
              clearable
              style="width: 150px"
              @change="loadTrend"
            >
              <el-option
                v-for="w in warehouses"
                :key="w.id"
                :label="warehouseLabel(w)"
                :value="w.id"
              />
            </el-select>
          </div>
        </div>
        <VChart :option="trendOption" autoresize style="height: 300px" />
        <div class="chart-note">
          区间内入库合计 {{ formatQty(trend.totalInboundQty) }}，出库合计
          {{ formatQty(trend.totalOutboundQty) }}
        </div>
      </div>
    </div>

    <!-- ============ 库存汇总 ============ -->
    <div class="page-card summary-card">
      <div class="chart-head">
        <span class="chart-title">库存汇总</span>
        <div class="chart-actions">
          <el-radio-group v-model="summaryGroupBy" size="small" @change="loadSummary">
            <el-radio-button value="warehouse">按仓库</el-radio-button>
            <el-radio-button value="category">按分类</el-radio-button>
            <el-radio-button value="sku">按 SKU</el-radio-button>
          </el-radio-group>
          <el-button :icon="Refresh" text @click="loadSummary">刷新</el-button>
        </div>
      </div>

      <div class="summary-totals">
        <span>SKU 数 <b>{{ summary.totalSkuCount ?? 0 }}</b></span>
        <span>在库总量 <b>{{ formatQty(summary.totalOnHandQty) }}</b></span>
        <span>锁定总量 <b class="locked">{{ formatQty(summary.totalLockedQty) }}</b></span>
      </div>

      <el-table :data="summary.rows ?? []" border stripe max-height="360" empty-text="暂无库存汇总数据">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="groupName" label="分组" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.groupName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="skuCount" label="SKU 数" width="100" align="right" />
        <el-table-column prop="onHandQty" label="在库量" width="130" align="right">
          <template #default="{ row }">{{ formatQty(row.onHandQty) }}</template>
        </el-table-column>
        <el-table-column prop="lockedQty" label="锁定量" width="120" align="right">
          <template #default="{ row }">{{ formatQty(row.lockedQty) }}</template>
        </el-table-column>
        <el-table-column prop="availableQty" label="可用量" width="130" align="right">
          <template #default="{ row }">
            <span class="qty-available">{{ formatQty(row.availableQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可用占比" min-width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="ratioPercent(row.availableQty, row.onHandQty)"
              :stroke-width="8"
              :color="ratioColor(row.availableQty, row.onHandQty)"
            />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Box,
  Goods,
  Histogram,
  OfficeBuilding,
  Refresh,
  ShoppingCart,
  Timer,
  Upload,
  Van,
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { useOptions } from '@/composables/useOptions'
import { reportApi } from '@/api/report'
import { formatQty } from '@/types/enum'
import type {
  Dashboard,
  Granularity,
  InboundOutbound,
  InventorySummary,
  StockAging,
  SummaryGroupBy,
} from '@/types/report'

// 按需注册 echarts 模块，避免把整个 echarts 打进包里
use([
  BarChart,
  LineChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  CanvasRenderer,
])

const router = useRouter()
const { warehouses, loadWarehouses, warehouseLabel } = useOptions()

const loading = ref(false)

/* ---------------- 指标卡 ---------------- */

const data = ref<Dashboard | null>(null)

interface StatCard {
  label: string
  value: string | number
  hint: string
  icon: unknown
  color: string
  bg: string
  to?: string
}

const statCards = computed<StatCard[]>(() => {
  const d = data.value
  return [
    {
      label: '仓库数',
      value: d?.warehouseCount ?? 0,
      hint: '主数据',
      icon: OfficeBuilding,
      color: '#409eff',
      bg: '#ecf5ff',
      to: '/warehouse',
    },
    {
      label: '库位数',
      value: d?.locationCount ?? 0,
      hint: '主数据',
      icon: Box,
      color: '#909399',
      bg: '#f4f4f5',
      to: '/location',
    },
    {
      label: 'SKU 数',
      value: d?.skuCount ?? 0,
      hint: '主数据',
      icon: Goods,
      color: '#67c23a',
      bg: '#f0f9eb',
      to: '/sku',
    },
    {
      label: '客户数',
      value: d?.customerCount ?? 0,
      hint: '主数据',
      icon: ShoppingCart,
      color: '#67c23a',
      bg: '#f0f9eb',
      to: '/customer',
    },
    {
      label: '库存总量',
      value: formatQty(d?.totalOnHandQty),
      hint: '在库合计',
      icon: Histogram,
      color: '#e6a23c',
      bg: '#fdf6ec',
      to: '/inventory',
    },
    {
      label: '可用总量',
      value: formatQty(d?.totalAvailableQty),
      hint: '在库 − 锁定',
      icon: Goods,
      color: '#16a34a',
      bg: '#f0f9eb',
      to: '/inventory',
    },
    {
      label: '待上架任务',
      value: d?.pendingPutawayTasks ?? 0,
      hint: '收货后待上架',
      icon: Upload,
      color: '#e6a23c',
      bg: '#fdf6ec',
      to: '/inbound',
    },
    {
      label: '待拣货任务',
      value: d?.pendingPickTasks ?? 0,
      hint: '待走位拣货',
      icon: Van,
      color: '#409eff',
      bg: '#ecf5ff',
      to: '/outbound',
    },
    {
      label: '今日入库单',
      value: d?.todayInboundOrders ?? 0,
      hint: '到货单数',
      icon: Upload,
      color: '#409eff',
      bg: '#ecf5ff',
      to: '/inbound',
    },
    {
      label: '今日出库单',
      value: d?.todayOutboundOrders ?? 0,
      hint: '订单数',
      icon: ShoppingCart,
      color: '#409eff',
      bg: '#ecf5ff',
      to: '/outbound',
    },
    {
      label: '盘点进行中',
      value: d?.stocktakeInProgress ?? 0,
      hint: '未结束盘点单',
      icon: Timer,
      color: '#909399',
      bg: '#f4f4f5',
      to: '/stocktake',
    },
    {
      label: '30 天内到期 SKU',
      value: d?.expiringSoonSkuCount ?? 0,
      hint: '临期提醒',
      icon: Timer,
      color: '#f56c6c',
      bg: '#fef0f0',
      to: '/inventory',
    },
  ]
})

/* ---------------- 库龄分析 ---------------- */

const aging = ref<StockAging>({ rows: [], totalValue: 0 })
const agingWarehouseId = ref<number | undefined>(undefined)

const agingOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 50, right: 20, top: 30, bottom: 30 },
  xAxis: {
    type: 'category',
    data: (aging.value.rows ?? []).map((r) => r.bucket),
    axisTick: { alignWithLabel: true },
  },
  yAxis: { type: 'value', name: '在库数量', splitLine: { lineStyle: { type: 'dashed' } } },
  series: [
    {
      name: '在库数量',
      type: 'bar',
      barMaxWidth: 52,
      data: (aging.value.rows ?? []).map((r) => ({
        value: Number(r.onHandQty ?? 0),
        skuCount: r.skuCount,
      })),
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top', formatter: (p: { value: number }) => formatQty(p.value) },
    },
  ],
}))

/* ---------------- 出入库趋势 ---------------- */

const trend = ref<InboundOutbound>({ rows: [], totalInboundQty: 0, totalOutboundQty: 0 })
const granularity = ref<Granularity>('day')
const trendWarehouseId = ref<number | undefined>(undefined)

function isoDate(date: Date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function daysAgo(n: number) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return isoDate(d)
}

/** 默认看最近 30 天 */
const trendRange = ref<[string, string]>([daysAgo(29), isoDate(new Date())])

const trendOption = computed(() => {
  const rows = trend.value.rows ?? []
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库数量', '出库数量'], right: 0 },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map((r) => r.period) },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } } },
    series: [
      {
        name: '入库数量',
        type: 'line',
        smooth: true,
        showSymbol: rows.length <= 31,
        data: rows.map((r) => Number(r.inboundQty ?? 0)),
        itemStyle: { color: '#67c23a' },
        areaStyle: { color: 'rgba(103,194,58,0.12)' },
      },
      {
        name: '出库数量',
        type: 'line',
        smooth: true,
        showSymbol: rows.length <= 31,
        data: rows.map((r) => Number(r.outboundQty ?? 0)),
        itemStyle: { color: '#f56c6c' },
        areaStyle: { color: 'rgba(245,108,108,0.12)' },
      },
    ],
  }
})

/* ---------------- 库存汇总 ---------------- */

const summary = ref<InventorySummary>({
  totalSkuCount: 0,
  totalOnHandQty: 0,
  totalLockedQty: 0,
  rows: [],
})
const summaryGroupBy = ref<SummaryGroupBy>('warehouse')

function ratioPercent(available: number, onHand: number) {
  const o = Number(onHand || 0)
  if (o <= 0) return 0
  return Math.round((Number(available || 0) / o) * 100)
}

function ratioColor(available: number, onHand: number) {
  const p = ratioPercent(available, onHand)
  if (p >= 80) return '#67c23a'
  if (p >= 50) return '#e6a23c'
  return '#f56c6c'
}

/* ---------------- 加载 ---------------- */

async function loadDashboard() {
  data.value = await reportApi.dashboard()
}

async function loadAging() {
  try {
    aging.value = await reportApi.stockAging({ warehouseId: agingWarehouseId.value })
  } catch {
    aging.value = { rows: [], totalValue: 0 }
  }
}

async function loadTrend() {
  try {
    trend.value = await reportApi.inboundOutbound({
      warehouseId: trendWarehouseId.value,
      startDate: trendRange.value?.[0],
      endDate: trendRange.value?.[1],
      granularity: granularity.value,
    })
  } catch {
    trend.value = { rows: [], totalInboundQty: 0, totalOutboundQty: 0 }
  }
}

async function loadSummary() {
  try {
    summary.value = await reportApi.inventorySummary({ groupBy: summaryGroupBy.value })
  } catch {
    summary.value = { totalSkuCount: 0, totalOnHandQty: 0, totalLockedQty: 0, rows: [] }
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadWarehouses()
    await Promise.all([loadDashboard(), loadAging(), loadTrend(), loadSummary()])
  } catch {
    /* 拦截器已提示，页面各区块显示空态 */
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 4px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  transition: box-shadow 0.2s;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 13px;
  color: #606266;
}

.stat-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  font-size: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin: 6px 0 2px;
  line-height: 1.2;
}

.stat-hint {
  font-size: 12px;
  color: #909399;
}

.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 1200px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}

.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
}

.chart-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.chart-note {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.summary-totals {
  display: flex;
  gap: 24px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
}

.summary-totals b {
  font-size: 15px;
  color: #303133;
  margin-left: 4px;
}

.summary-totals b.locked {
  color: #e6a23c;
}

.qty-available {
  font-weight: 700;
  color: #16a34a;
}
</style>
