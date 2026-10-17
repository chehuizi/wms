<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ============ 到货单 ============ -->
      <el-tab-pane label="到货单" name="orders">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              placeholder="单号 / 供应商"
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
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
              <el-option
                v-for="o in INBOUND_STATUS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="createdRange"
              type="datetimerange"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DDTHH:mm:ssZ"
              style="width: 350px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="search">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetOrders">重置</el-button>
            <el-button type="primary" :icon="Plus" plain @click="openCreate">新建到货单</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loading"
          :data="rows"
          border
          stripe
          row-key="id"
          empty-text="暂无到货单"
        >
          <template #empty>
            <el-empty class="table-empty" description="暂无到货单" :image-size="80" />
          </template>

          <el-table-column prop="orderNo" label="单号" width="160" />
          <el-table-column prop="warehouseName" label="仓库" min-width="110" show-overflow-tooltip />
          <el-table-column prop="supplierName" label="供应商" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.supplierName || '-' }}</span>
              <span v-if="row.supplierCode" class="sub-text">（{{ row.supplierCode }}）</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="INBOUND_STATUS" />
            </template>
          </el-table-column>
          <el-table-column label="应收 / 已收" min-width="180">
            <template #default="{ row }">
              <div class="qty-progress-text">
                {{ formatQty(row.receivedQty) }} / {{ formatQty(row.expectedQty) }}
              </div>
              <el-progress
                :percentage="progressOf(row.expectedQty, row.receivedQty)"
                :stroke-width="6"
                :show-text="false"
              />
            </template>
          </el-table-column>
          <el-table-column prop="itemCount" label="明细" width="70" align="center" />
          <el-table-column prop="expectedAt" label="预计到货" width="170">
            <template #default="{ row }">{{ row.expectedAt || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="270" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 1" link type="primary" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-if="row.status === 1" link type="primary" @click="onSubmit(row)">
                提交
              </el-button>
              <el-button v-if="row.status === 2 || row.status === 3" link type="success" @click="openReceipt(row)">
                收货
              </el-button>
              <el-button
                v-if="row.status === 1 || row.status === 2 || row.status === 3"
                link
                type="warning"
                @click="onCancel(row)"
              >
                取消
              </el-button>
              <el-button v-if="row.status === 1" link type="danger" @click="onDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 上架任务 ============ -->
      <el-tab-pane label="上架任务" name="putaway">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="ptQuery.keyword"
              placeholder="任务号 / SKU"
              clearable
              style="width: 180px"
              @keyup.enter="searchPutaway"
              @clear="searchPutaway"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="ptQuery.status" placeholder="全部" clearable style="width: 130px">
              <el-option
                v-for="o in PUTAWAY_STATUS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchPutaway">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetPutaway">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" class="pt-tip">
          上架执行会同时产生「移库出 / 移库入」两条流水，源库位扣减、目标库位增加，批次与序列号随货移动。
        </el-alert>

        <el-table
          v-loading="loadingPutaway"
          :data="rowsPutaway"
          border
          stripe
          row-key="id"
          empty-text="暂无上架任务"
        >
          <template #empty>
            <el-empty class="table-empty" description="暂无上架任务" :image-size="80" />
          </template>

          <el-table-column prop="taskNo" label="任务号" width="160" />
          <el-table-column prop="inboundNo" label="来源单号" width="160">
            <template #default="{ row }">{{ row.inboundNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU" width="110" />
          <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="来源库位 → 目标库位" min-width="200">
            <template #default="{ row }">
              <span class="loc-from">{{ row.fromLocationCode }}</span>
              <span class="loc-arrow">→</span>
              <span :class="row.toLocationCode ? 'loc-to-done' : 'loc-to-empty'">
                {{ row.toLocationCode || '未指定' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="qty" label="数量" width="100" align="right">
            <template #default="{ row }">{{ formatQty(row.qty) }}</template>
          </el-table-column>
          <el-table-column prop="batchNo" label="批次" width="110">
            <template #default="{ row }">{{ row.batchNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="PUTAWAY_STATUS" />
            </template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="90">
            <template #default="{ row }">{{ row.operator || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="primary" @click="openExecute(row)">
                执行上架
              </el-button>
              <span v-else class="sub-text">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pagePutaway"
            v-model:page-size="sizePutaway"
            :total="totalPutaway"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onPutawayPageChange"
            @size-change="onPutawaySizeChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 新建 / 编辑到货单 ============ -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? `编辑到货单 ${editOrderNo}` : '新建到货单'"
      width="900px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select v-model="form.warehouseId" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="w in warehouses"
                  :key="w.id"
                  :label="warehouseLabel(w)"
                  :value="w.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商编码">
              <el-input v-model="form.supplierCode" placeholder="如 SUP001" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商名称">
              <el-input v-model="form.supplierName" placeholder="选填" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计到货">
              <el-date-picker
                v-model="form.expectedAt"
                type="datetime"
                placeholder="选填"
                value-format="YYYY-MM-DDTHH:mm:ssZ"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="选填" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="items-header">
        <span class="items-title">明细</span>
        <el-button size="small" type="primary" plain :icon="Plus" @click="addItem">添加明细</el-button>
      </div>

      <el-table :data="form.items" border size="small" empty-text="请至少添加一条明细">
        <el-table-column type="index" label="#" width="46" align="center" />
        <el-table-column label="SKU" min-width="260">
          <template #default="{ row }">
            <el-select
              v-model="row.skuId"
              placeholder="请选择 SKU"
              filterable
              style="width: 100%"
              @change="onItemSkuChange(row)"
            >
              <el-option
                v-for="s in skus"
                :key="s.id"
                :label="skuLabel(s)"
                :value="s.id"
                :disabled="isSkuUsed(s.id, row)"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="预期数量" width="170">
          <template #default="{ row }">
            <el-input-number v-model="row.expectedQty" :min="1" :step="1" :precision="0" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="批次管理" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.skuId && isBatchManaged(row.skuId)" size="small" type="primary" effect="plain">
              需批次
            </el-tag>
            <span v-else class="sub-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="序列号管理" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.skuId && isSerialManaged(row.skuId)" size="small" type="warning" effect="plain">
              收货逐条录
            </el-tag>
            <span v-else class="sub-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="" width="60" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmitForm">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 到货单详情抽屉 ============ -->
    <el-drawer v-model="detailVisible" :title="`到货单详情 ${detail?.orderNo ?? ''}`" size="880px">
      <div v-loading="loadingDetail" class="detail-body">
        <template v-if="detail">
          <BizSteps :steps="INBOUND_STEPS" :options="INBOUND_STATUS" :current="detail.status" />

          <el-descriptions :column="3" border size="small" class="detail-desc">
            <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="供应商">
              {{ detail.supplierName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <EnumTag :value="detail.status" :options="INBOUND_STATUS" />
            </el-descriptions-item>
            <el-descriptions-item label="应收数量">
              {{ formatQty(detail.expectedQty) }}
            </el-descriptions-item>
            <el-descriptions-item label="已收数量">
              {{ formatQty(detail.receivedQty) }}
            </el-descriptions-item>
            <el-descriptions-item label="预计到货">
              {{ detail.expectedAt || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="3">
              {{ detail.remark || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="section-title">到货明细</div>
          <el-table :data="detail.items" border size="small">
            <el-table-column prop="skuCode" label="SKU" width="110" />
            <el-table-column prop="skuName" label="商品名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="expectedQty" label="应收" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.expectedQty) }}</template>
            </el-table-column>
            <el-table-column prop="receivedQty" label="已收" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.receivedQty) }}</template>
            </el-table-column>
            <el-table-column label="未收" width="110" align="right">
              <template #default="{ row }">
                <span :class="{ 'qty-negative': remaining(row.expectedQty, row.receivedQty) > 0 }">
                  {{ formatQty(remaining(row.expectedQty, row.receivedQty)) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="70" align="center">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
          </el-table>

          <div class="section-title">
            收货记录
            <el-button
              v-if="detail.status === 2 || detail.status === 3"
              size="small"
              type="success"
              plain
              @click="openReceipt(detail)"
            >
              执行收货
            </el-button>
          </div>
          <el-table :data="detailReceipts" border size="small" empty-text="尚未收货">
            <el-table-column type="expand">
              <template #default="{ row }">
                <div class="receipt-items">
                  <div v-for="item in row.items" :key="item.id" class="receipt-item-line">
                    {{ item.skuCode }} {{ item.skuName }} × {{ formatQty(item.qty) }}
                    <span v-if="item.batchNo" class="sub-text">批次 {{ item.batchNo }}</span>
                    <span v-if="item.serialNo" class="sub-text">序列号 {{ item.serialNo }}</span>
                    <span v-if="item.expireDate" class="sub-text">效期 {{ item.expireDate }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="receiptNo" label="收货单号" width="160" />
            <el-table-column prop="locationCode" label="收货位" width="110" />
            <el-table-column prop="operator" label="操作人" width="90">
              <template #default="{ row }">{{ row.operator || '-' }}</template>
            </el-table-column>
            <el-table-column prop="receivedAt" label="收货时间" width="170" />
            <el-table-column label="明细数" width="80" align="center">
              <template #default="{ row }">{{ row.items?.length ?? 0 }}</template>
            </el-table-column>
          </el-table>

          <div class="section-title">上架任务</div>
          <el-table :data="detailPutaways" border size="small" empty-text="暂无上架任务">
            <el-table-column prop="taskNo" label="任务号" width="160" />
            <el-table-column prop="skuCode" label="SKU" width="110" />
            <el-table-column label="来源 → 目标" min-width="180">
              <template #default="{ row }">
                {{ row.fromLocationCode }} → {{ row.toLocationCode || '未指定' }}
              </template>
            </el-table-column>
            <el-table-column prop="qty" label="数量" width="90" align="right">
              <template #default="{ row }">{{ formatQty(row.qty) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <EnumTag :value="row.status" :options="PUTAWAY_STATUS" />
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button v-if="detail?.status === 1" type="primary" @click="onSubmit(detail)">
            提交
          </el-button>
          <el-button
            v-if="detail && (detail.status === 2 || detail.status === 3)"
            type="success"
            @click="openReceipt(detail)"
          >
            执行收货
          </el-button>
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- ============ 收货弹窗 ============ -->
    <el-dialog v-model="receiptVisible" title="执行收货" width="960px" destroy-on-close>
      <el-form ref="receiptRef" :model="receiptForm" :rules="receiptRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="收货暂存位" prop="locationId">
              <el-select
                v-model="receiptForm.locationId"
                placeholder="请选择暂存位"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="loc in locationsOf(receiptWarehouseId, 2)"
                  :key="loc.id"
                  :label="locationLabel(loc)"
                  :value="loc.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="操作人">
              <el-input v-model="receiptForm.operator" placeholder="如 张三" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="receiptForm.remark" placeholder="选填" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-alert type="info" :closable="false" class="receipt-tip">
        每次收货会生成上架任务，库存先落到收货暂存位。数量不得超过未收数（超收后端返回 40903）。
      </el-alert>

      <el-table :data="receiptForm.items" border size="small" empty-text="该单已全部收货">
        <el-table-column type="index" label="#" width="46" align="center" />
        <el-table-column prop="skuLabel" label="SKU" min-width="180" show-overflow-tooltip />
        <el-table-column prop="remaining" label="未收" width="100" align="right" />
        <el-table-column label="实收数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="row.qty"
              :min="row.serialManaged ? 1 : 0"
              :max="row.serialManaged ? 1 : row.remaining"
              :step="1"
              :precision="0"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="批次号" width="130">
          <template #default="{ row }">
            <el-input v-model="row.batchNo" :disabled="!row.batchManaged" placeholder="选填" maxlength="64" />
          </template>
        </el-table-column>
        <el-table-column label="序列号" width="140">
          <template #default="{ row }">
            <el-input
              v-model="row.serialNo"
              :disabled="!row.serialManaged"
              :placeholder="row.serialManaged ? '必填' : '不适用'"
              maxlength="64"
            />
          </template>
        </el-table-column>
        <el-table-column label="生产日期" width="150">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.producedDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选填"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column label="到期日期" width="150">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.expireDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选填"
              style="width: 100%"
            />
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="receiptVisible = false">取消</el-button>
        <el-button type="primary" :loading="receiptSubmitting" @click="onSubmitReceipt">
          确认收货
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 执行上架弹窗 ============ -->
    <el-dialog v-model="executeVisible" title="执行上架" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" class="receipt-tip">
        目标库位必须是该仓库下的存储位（locationType = 1）。
      </el-alert>
      <el-form ref="executeRef" :model="executeForm" :rules="executeRules" label-width="90px">
        <el-form-item label="商品">
          <span class="static-text">
            {{ executeTask?.skuCode }} {{ executeTask?.skuName }} × {{ formatQty(executeTask?.qty) }}
          </span>
        </el-form-item>
        <el-form-item label="来源库位">
          <span class="static-text">{{ executeTask?.fromLocationCode }}</span>
        </el-form-item>
        <el-form-item label="目标库位" prop="toLocationId">
          <el-select v-model="executeForm.toLocationId" placeholder="请选择存储位" filterable style="width: 100%">
            <el-option
              v-for="loc in locationsOf(executeWarehouseId, 1)"
              :key="loc.id"
              :label="locationLabel(loc)"
              :value="loc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="executeForm.operator" placeholder="如 李四" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="executeVisible = false">取消</el-button>
        <el-button type="primary" :loading="executeSubmitting" @click="onSubmitExecute">
          确认上架
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import BizSteps from '@/components/BizSteps.vue'
import EnumTag from '@/components/EnumTag.vue'
import { useTablePage } from '@/composables/useTablePage'
import { useOptions } from '@/composables/useOptions'
import { inboundApi, putawayApi } from '@/api/inbound'
import { formatQty } from '@/types/enum'
import {
  INBOUND_STATUS,
  INBOUND_STEPS,
  PUTAWAY_STATUS,
  type InboundOrder,
  type InboundOrderDetail,
  type InboundOrderPayload,
  type InboundOrderQuery,
  type InboundReceipt,
  type PutawayTask,
  type PutawayTaskQuery,
} from '@/types/inbound'

const {
  warehouses,
  locations,
  skus,
  loadWarehouses,
  loadLocations,
  loadSkus,
  warehouseLabel,
  locationLabel,
  skuLabel,
  locationsOf,
  isSerialManaged,
  isBatchManaged,
} = useOptions()

const activeTab = ref('orders')

function remaining(expected: number, received: number) {
  return Number(expected || 0) - Number(received || 0)
}

function progressOf(expected: number, received: number) {
  const exp = Number(expected || 0)
  if (exp <= 0) return 0
  return Math.min(100, Math.round((Number(received || 0) / exp) * 100))
}

/* ---------------- 到货单列表 ---------------- */

const query = reactive<{
  keyword: string
  warehouseId?: number
  status?: number
}>({
  keyword: '',
  warehouseId: undefined,
  status: undefined,
})

/** 后端按 created_at 过滤，所以筛选条件标为「创建时间」 */
const createdRange = ref<[string, string] | null>(null)

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
} = useTablePage<InboundOrder, InboundOrderQuery>({
  api: { page: (q: InboundOrderQuery) => inboundApi.page(q), remove: (id: number) => inboundApi.remove(id) },
  entityName: '到货单',
  labelOf: (row) => row.orderNo,
  buildQuery: (): InboundOrderQuery => ({
    page: page.value,
    size: size.value,
    keyword: query.keyword || undefined,
    warehouseId: query.warehouseId,
    status: query.status,
    startTime: createdRange.value?.[0],
    endTime: createdRange.value?.[1],
  }),
})

function onResetOrders() {
  query.keyword = ''
  query.warehouseId = undefined
  query.status = undefined
  createdRange.value = null
  search()
}

async function onSubmit(row: InboundOrder) {
  try {
    await ElMessageBox.confirm(`确定提交到货单「${row.orderNo}」吗？提交后进入待收货状态。`, '提交确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await inboundApi.submit(row.id)
    ElMessage.success('已提交')
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onCancel(row: InboundOrder) {
  try {
    await ElMessageBox.confirm(`确定取消到货单「${row.orderNo}」吗？`, '取消确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await inboundApi.cancel(row.id)
    ElMessage.success('已取消')
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

function onDelete(row: InboundOrder) {
  return handleDelete(row)
}

/* ---------------- 新建 / 编辑 ---------------- */

interface DraftItem {
  skuId: number | null
  expectedQty: number | null
}

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref<number | null>(null)
const editOrderNo = ref('')
const formRef = ref<FormInstance>()

const form = reactive<{
  warehouseId: number | null
  supplierCode: string
  supplierName: string
  expectedAt: string | null
  remark: string
  items: DraftItem[]
}>({
  warehouseId: null,
  supplierCode: '',
  supplierName: '',
  expectedAt: null,
  remark: '',
  items: [],
})

const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
}

function addItem() {
  form.items.push({ skuId: null, expectedQty: 1 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

/** 同一 SKU 只能出现一次，重复后端返回 40001，这里提前禁用 */
function isSkuUsed(skuId: number, current: DraftItem) {
  return form.items.some((item) => item !== current && item.skuId === skuId)
}

function onItemSkuChange(row: DraftItem) {
  if (!row.expectedQty || row.expectedQty < 1) row.expectedQty = 1
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, {
    warehouseId: null,
    supplierCode: '',
    supplierName: '',
    expectedAt: null,
    remark: '',
    items: [],
  })
  editId.value = null
  isEdit.value = false
  editOrderNo.value = ''
}

function openCreate() {
  resetForm()
  addItem()
  formVisible.value = true
}

async function openEdit(row: InboundOrder) {
  resetForm()
  try {
    const detail = await inboundApi.detail(row.id)
    Object.assign(form, {
      warehouseId: detail.warehouseId,
      supplierCode: detail.supplierCode ?? '',
      supplierName: detail.supplierName ?? '',
      expectedAt: detail.expectedAt ?? null,
      remark: detail.remark ?? '',
      items: detail.items.map((item) => ({
        skuId: item.skuId,
        expectedQty: Number(item.expectedQty),
      })),
    })
    editId.value = row.id
    editOrderNo.value = row.orderNo
    isEdit.value = true
    formVisible.value = true
  } catch {
    /* 拦截器已提示 */
  }
}

async function onSubmitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.warehouseId) return

  const filled = form.items.filter((i) => i.skuId && Number(i.expectedQty) > 0)
  if (filled.length === 0) {
    ElMessage.warning('请至少填写一条有效明细')
    return
  }
  if (new Set(filled.map((i) => i.skuId)).size !== filled.length) {
    ElMessage.warning('同一 SKU 不能重复出现')
    return
  }

  submitting.value = true
  try {
    const payload: InboundOrderPayload = {
      warehouseId: form.warehouseId,
      supplierCode: form.supplierCode.trim() || null,
      supplierName: form.supplierName.trim() || null,
      expectedAt: form.expectedAt || null,
      remark: form.remark.trim() || null,
      items: filled.map((i) => ({ skuId: i.skuId as number, expectedQty: Number(i.expectedQty) })),
    }
    if (isEdit.value && editId.value !== null) {
      await inboundApi.update(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await inboundApi.create(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

/* ---------------- 详情抽屉 ---------------- */

const detailVisible = ref(false)
const loadingDetail = ref(false)
const detail = ref<InboundOrderDetail | null>(null)
const detailReceipts = ref<InboundReceipt[]>([])
const detailPutaways = ref<PutawayTask[]>([])

async function openDetail(row: InboundOrder) {
  detailVisible.value = true
  loadingDetail.value = true
  detailReceipts.value = []
  detailPutaways.value = []
  try {
    detail.value = await inboundApi.detail(row.id)
    detailReceipts.value = (await inboundApi.receipts(row.id)) ?? []
    const pt = await putawayApi.page({ page: 1, size: 100, inboundId: row.id })
    detailPutaways.value = pt?.list ?? []
  } catch {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

/** 列表上的动作执行后，刷新详情与列表 */
async function refreshAfterAction() {
  await load()
  if (detail.value) {
    await openDetail(detail.value)
  }
}

/* ---------------- 收货 ---------------- */

const receiptVisible = ref(false)
const receiptSubmitting = ref(false)
const receiptRef = ref<FormInstance>()
const receiptId = ref<number | null>(null)
const receiptWarehouseId = ref<number | null>(null)
const executeWarehouseId = ref<number | null>(null)

interface ReceiptRow {
  inboundItemId: number
  skuLabel: string
  remaining: number
  qty: number
  batchManaged: boolean
  serialManaged: boolean
  batchNo: string
  serialNo: string
  producedDate: string | null
  expireDate: string | null
}

const receiptForm = reactive<{
  locationId: number | null
  operator: string
  remark: string
  items: ReceiptRow[]
}>({
  locationId: null,
  operator: '',
  remark: '',
  items: [],
})

const receiptRules: FormRules = {
  locationId: [{ required: true, message: '请选择收货暂存位', trigger: 'change' }],
}

async function openReceipt(row: InboundOrder) {
  try {
    const d = await inboundApi.detail(row.id)
    const openItems = d.items.filter((i) => remaining(i.expectedQty, i.receivedQty) > 0)
    if (openItems.length === 0) {
      ElMessage.warning('该单已全部收货，没有可收明细')
      return
    }
    receiptId.value = row.id
    receiptWarehouseId.value = row.warehouseId
    Object.assign(receiptForm, {
      locationId: null,
      operator: '',
      remark: '',
      items: openItems.map((item) => {
        const serialManaged = isSerialManaged(item.skuId)
        const rest = remaining(item.expectedQty, item.receivedQty)
        return {
          inboundItemId: item.id,
          skuLabel: `${item.skuCode} ${item.skuName}`,
          remaining: rest,
          // 序列号管理的 SKU 一次只能收 1 件（一条明细对应一个序列号）
          qty: serialManaged ? 1 : rest,
          batchManaged: isBatchManaged(item.skuId),
          serialManaged,
          batchNo: '',
          serialNo: '',
          producedDate: null,
          expireDate: null,
        }
      }),
    })
    detailVisible.value = false
    receiptVisible.value = true
  } catch {
    /* 拦截器已提示 */
  }
}

async function onSubmitReceipt() {
  const valid = await receiptRef.value?.validate().catch(() => false)
  if (!valid || receiptId.value === null) return

  const filled = receiptForm.items.filter((i) => Number(i.qty) > 0)
  if (filled.length === 0) {
    ElMessage.warning('请至少填写一条实收明细')
    return
  }
  const over = filled.find((i) => !i.serialManaged && Number(i.qty) > i.remaining)
  if (over) {
    ElMessage.warning(`「${over.skuLabel}」实收 ${over.qty} 超过未收数 ${over.remaining}`)
    return
  }
  const missingSerial = filled.find((i) => i.serialManaged && !i.serialNo.trim())
  if (missingSerial) {
    ElMessage.warning(`「${missingSerial.skuLabel}」开了序列号管理，必须逐条填写序列号`)
    return
  }

  receiptSubmitting.value = true
  try {
    await inboundApi.receipt(receiptId.value, {
      locationId: receiptForm.locationId as number,
      operator: receiptForm.operator.trim() || null,
      remark: receiptForm.remark.trim() || null,
      items: filled.map((i) => ({
        inboundItemId: i.inboundItemId,
        // 未开序列号管理时后端明确要求不能带序列号
        batchNo: i.batchNo.trim() || null,
        serialNo: i.serialManaged ? i.serialNo.trim() : null,
        producedDate: i.producedDate || null,
        expireDate: i.expireDate || null,
        qty: Number(i.qty),
      })),
    })
    ElMessage.success('收货成功，已生成上架任务')
    receiptVisible.value = false
    await refreshAfterAction()
    await reloadPutawayIfActive()
  } catch {
    /* 拦截器已提示 */
  } finally {
    receiptSubmitting.value = false
  }
}

/* ---------------- 上架任务 ---------------- */

const ptQuery = reactive<{ keyword: string; status?: number }>({
  keyword: '',
  status: undefined,
})

const {
  rows: rowsPutaway,
  total: totalPutaway,
  loading: loadingPutaway,
  page: pagePutaway,
  size: sizePutaway,
  load: loadPutaway,
  search: searchPutaway,
  onPageChange: onPutawayPageChange,
  onSizeChange: onPutawaySizeChange,
} = useTablePage<PutawayTask, PutawayTaskQuery>({
  api: { page: (q: PutawayTaskQuery) => putawayApi.page(q) },
  entityName: '上架任务',
  buildQuery: (): PutawayTaskQuery => ({
    page: pagePutaway.value,
    size: sizePutaway.value,
    keyword: ptQuery.keyword || undefined,
    status: ptQuery.status,
  }),
})

function onResetPutaway() {
  ptQuery.keyword = ''
  ptQuery.status = undefined
  searchPutaway()
}

async function reloadPutawayIfActive() {
  if (activeTab.value === 'putaway') {
    await loadPutaway()
  } else {
    loadedOnce.putaway = false
  }
}

const executeVisible = ref(false)
const executeSubmitting = ref(false)
const executeRef = ref<FormInstance>()
const executeTask = ref<PutawayTask | null>(null)

const executeForm = reactive<{ toLocationId: number | null; operator: string }>({
  toLocationId: null,
  operator: '',
})

const executeRules: FormRules = {
  toLocationId: [{ required: true, message: '请选择目标库位', trigger: 'change' }],
}

async function openExecute(row: PutawayTask) {
  executeTask.value = row
  Object.assign(executeForm, { toLocationId: null, operator: '' })
  // 任务对象里没有仓库字段，用来源暂存位反查所属仓库，好过滤出该仓的存储位
  const from = locations.value.find((loc) => loc.id === row.fromLocationId)
  executeWarehouseId.value = from?.warehouseId ?? null
  executeVisible.value = true
}

async function onSubmitExecute() {
  const valid = await executeRef.value?.validate().catch(() => false)
  if (!valid || executeTask.value === null) return
  executeSubmitting.value = true
  try {
    await putawayApi.execute(executeTask.value.id, {
      toLocationId: executeForm.toLocationId as number,
      operator: executeForm.operator.trim() || null,
    })
    ElMessage.success('上架完成')
    executeVisible.value = false
    await loadPutaway()
  } catch {
    /* 拦截器已提示 */
  } finally {
    executeSubmitting.value = false
  }
}

/* ---------------- 生命周期 ---------------- */

const loadedOnce = {
  orders: false,
  putaway: false,
}

async function onTabChange(name: string | number) {
  if (name === 'putaway' && !loadedOnce.putaway) {
    loadedOnce.putaway = true
    await loadPutaway()
  }
}

onMounted(async () => {
  await Promise.all([loadWarehouses(), loadLocations(), loadSkus()])
  loadedOnce.orders = true
  await load()
})
</script>

<style scoped>
.sub-text {
  font-size: 12px;
  color: #909399;
}

.qty-progress-text {
  font-size: 12px;
  color: #606266;
  margin-bottom: 2px;
}

.qty-negative {
  color: #f56c6c;
  font-weight: 600;
}

.items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.items-title {
  font-size: 14px;
  font-weight: 600;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 20px 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.detail-desc {
  margin-top: 12px;
}

.receipt-items {
  padding: 8px 12px;
  background: #fafafa;
}

.receipt-item-line {
  font-size: 13px;
  line-height: 1.9;
}

.receipt-tip {
  margin-bottom: 12px;
}

.pt-tip {
  margin-bottom: 12px;
}

.static-text {
  color: #606266;
}

.loc-from {
  color: #606266;
}

.loc-arrow {
  margin: 0 6px;
  color: #c0c4cc;
}

.loc-to-done {
  color: #16a34a;
  font-weight: 600;
}

.loc-to-empty {
  color: #c0c4cc;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
