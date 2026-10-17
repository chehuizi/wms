<template>
  <div class="page-card">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- ============ 出库订单 ============ -->
      <el-tab-pane label="出库订单" name="orders">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              placeholder="单号 / 客户"
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
          <el-form-item label="客户">
            <el-select v-model="query.customerId" placeholder="全部" clearable filterable style="width: 180px">
              <el-option
                v-for="c in customers"
                :key="c.id"
                :label="customerLabel(c)"
                :value="c.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
              <el-option
                v-for="o in OUTBOUND_STATUS"
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
            <el-button type="primary" :icon="Plus" plain @click="openCreate">新建订单</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="rows" border stripe row-key="id" empty-text="暂无出库订单">
          <template #empty>
            <el-empty class="table-empty" description="暂无出库订单" :image-size="80" />
          </template>

          <el-table-column prop="orderNo" label="单号" width="160" />
          <el-table-column prop="warehouseName" label="仓库" min-width="110" show-overflow-tooltip />
          <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.customerName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="85" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.priority >= 5" size="small" type="danger" effect="plain">
                {{ row.priority }}
              </el-tag>
              <span v-else>{{ row.priority }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="95" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="OUTBOUND_STATUS" />
            </template>
          </el-table-column>
          <el-table-column label="订购 / 已配 / 已拣 / 已发" min-width="210">
            <template #default="{ row }">
              <div class="qty-chain">
                {{ formatQty(row.orderedQty) }} / {{ formatQty(row.allocatedQty) }} /
                {{ formatQty(row.pickedQty) }} / {{ formatQty(row.shippedQty) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="expectedShipAt" label="预计发货" width="170">
            <template #default="{ row }">{{ row.expectedShipAt || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="290" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 1" link type="primary" @click="openEdit(row)">
                编辑
              </el-button>
              <el-button v-if="row.status === 1" link type="primary" @click="onSubmit(row)">
                提交
              </el-button>
              <el-button v-if="row.status === 2" link type="warning" @click="onAllocate(row)">
                分配库存
              </el-button>
              <el-button v-if="row.status === 3" link type="success" @click="onCreatePickTasks(row)">
                生成拣货
              </el-button>
              <el-button v-if="row.status === 5" link type="danger" @click="openShip(row)">
                发货
              </el-button>
              <el-button
                v-if="row.status === 1 || row.status === 2 || row.status === 3 || row.status === 4 || row.status === 5"
                link
                type="info"
                @click="onCancel(row)"
              >
                取消
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

      <!-- ============ 拣货波次 ============ -->
      <el-tab-pane label="拣货波次" name="waves">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="wvQuery.keyword"
              placeholder="波次号"
              clearable
              style="width: 180px"
              @keyup.enter="searchWaves"
              @clear="searchWaves"
            />
          </el-form-item>
          <el-form-item label="仓库">
            <el-select v-model="wvQuery.warehouseId" placeholder="全部" clearable style="width: 170px">
              <el-option
                v-for="w in warehouses"
                :key="w.id"
                :label="warehouseLabel(w)"
                :value="w.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="wvQuery.status" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="o in WAVE_STATUS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchWaves">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetWaves">重置</el-button>
            <el-button type="primary" :icon="Plus" plain @click="openCreateWave">新建波次</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loadingWaves" :data="rowsWaves" border stripe row-key="id" empty-text="暂无波次">
          <template #empty>
            <el-empty class="table-empty" description="暂无波次" :image-size="80" />
          </template>

          <el-table-column prop="waveNo" label="波次号" width="160" />
          <el-table-column prop="warehouseName" label="仓库" min-width="110" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="WAVE_STATUS" />
            </template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" width="90" align="center" />
          <el-table-column prop="operator" label="操作人" width="90">
            <template #default="{ row }">{{ row.operator || '-' }}</template>
          </el-table-column>
          <el-table-column prop="releasedAt" label="释放时间" width="170">
            <template #default="{ row }">{{ row.releasedAt || '-' }}</template>
          </el-table-column>
          <el-table-column prop="finishedAt" label="完成时间" width="170">
            <template #default="{ row }">{{ row.finishedAt || '-' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="primary" @click="openAddOrders(row)">
                加入订单
              </el-button>
              <el-button v-if="row.status === 2" link type="warning" @click="onReleaseWave(row)">
                释放波次
              </el-button>
              <span v-if="row.status !== 1 && row.status !== 2" class="sub-text">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pageWaves"
            v-model:page-size="sizeWaves"
            :total="totalWaves"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onWavePageChange"
            @size-change="onWaveSizeChange"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 拣货任务 ============ -->
      <el-tab-pane label="拣货任务" name="pick">
        <el-form :inline="true" class="page-toolbar" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="pkQuery.keyword"
              placeholder="任务号 / SKU"
              clearable
              style="width: 180px"
              @keyup.enter="searchPick"
              @clear="searchPick"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="pkQuery.status" placeholder="全部" clearable style="width: 130px">
              <el-option v-for="o in PICK_STATUS" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="searchPick">查询</el-button>
            <el-button :icon="RefreshLeft" @click="onResetPick">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" class="pk-tip">
          任务默认按 sequence 升序展示（同库位已合并），按该顺序走位拣货可少走路。允许部分拣，但拣满才推进订单状态。
        </el-alert>

        <el-table v-loading="loadingPick" :data="rowsPick" border stripe row-key="id" empty-text="暂无拣货任务">
          <template #empty>
            <el-empty class="table-empty" description="暂无拣货任务" :image-size="80" />
          </template>

          <el-table-column prop="sequence" label="序号" width="70" align="center">
            <template #default="{ row }">
              <span class="seq-badge">{{ row.sequence }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="taskNo" label="任务号" width="160" />
          <el-table-column prop="waveNo" label="波次" width="150">
            <template #default="{ row }">{{ row.waveNo || '直接生成' }}</template>
          </el-table-column>
          <el-table-column prop="outboundNo" label="出库单" width="150">
            <template #default="{ row }">{{ row.outboundNo || '-' }}</template>
          </el-table-column>
          <el-table-column prop="fromLocationCode" label="拣货位" width="110" />
          <el-table-column prop="skuCode" label="SKU" width="110" />
          <el-table-column prop="skuName" label="商品名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="requiredQty" label="应拣" width="100" align="right">
            <template #default="{ row }">{{ formatQty(row.requiredQty) }}</template>
          </el-table-column>
          <el-table-column prop="pickedQty" label="已拣" width="100" align="right">
            <template #default="{ row }">
              <span :class="Number(row.pickedQty) >= Number(row.requiredQty) ? 'qty-positive' : ''">
                {{ formatQty(row.pickedQty) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="95" align="center">
            <template #default="{ row }">
              <EnumTag :value="row.status" :options="PICK_STATUS" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="center" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="primary" @click="openPick(row)">
                拣货
              </el-button>
              <el-button v-if="row.status === 1" link type="info" @click="onCancelPick(row)">
                取消
              </el-button>
              <span v-if="row.status !== 1" class="sub-text">-</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pagePick"
            v-model:page-size="sizePick"
            :total="totalPick"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="onPickPageChange"
            @size-change="onPickSizeChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 新建 / 编辑订单 ============ -->
    <el-dialog
      v-model="formVisible"
      :title="isEdit ? `编辑订单 ${editOrderNo}` : '新建出库订单'"
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
            <el-form-item label="客户">
              <el-select v-model="form.customerId" placeholder="选填" filterable clearable style="width: 100%">
                <el-option
                  v-for="c in customers"
                  :key="c.id"
                  :label="customerLabel(c)"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="0" :max="100" :step="1" style="width: 100%" />
              <div class="field-hint">数值越大越优先</div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计发货">
              <el-date-picker
                v-model="form.expectedShipAt"
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
        <el-table-column label="SKU" min-width="280">
          <template #default="{ row }">
            <el-select v-model="row.skuId" placeholder="请选择 SKU" filterable style="width: 100%">
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
        <el-table-column label="订购数量" width="170">
          <template #default="{ row }">
            <el-input-number v-model="row.orderedQty" :min="1" :step="1" :precision="0" style="width: 100%" />
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

    <!-- ============ 订单详情抽屉 ============ -->
    <el-drawer v-model="detailVisible" :title="`出库订单详情 ${detail?.orderNo ?? ''}`" size="900px">
      <div v-loading="loadingDetail" class="detail-body">
        <!-- 分配失败时后端会写清缺了多少，这里原样展示，不要二次加工 -->
        <el-alert
          v-if="shortageMessage"
          type="error"
          :closable="true"
          title="库存不足，分配未生效"
          class="shortage-alert"
          @close="shortageMessage = ''"
        >
          <div class="shortage-text">{{ shortageMessage }}</div>
        </el-alert>

        <template v-if="detail">
          <BizSteps :steps="OUTBOUND_STEPS" :options="OUTBOUND_STATUS" :current="detail.status" />

          <div class="detail-actions">
            <el-button v-if="detail.status === 1" type="primary" size="small" @click="onSubmit(detail)">
              提交
            </el-button>
            <el-button
              v-if="detail.status === 2"
              type="warning"
              size="small"
              @click="onAllocate(detail)"
            >
              分配库存
            </el-button>
            <el-button
              v-if="detail.status === 3"
              type="success"
              size="small"
              @click="onCreatePickTasks(detail)"
            >
              直接生成拣货任务
            </el-button>
            <el-button
              v-if="detail.status === 3 || detail.status === 4"
              size="small"
              @click="onReleaseAllocation(detail)"
            >
              释放锁定
            </el-button>
            <el-button v-if="detail.status === 5" type="danger" size="small" @click="openShip(detail)">
              发货
            </el-button>
          </div>

          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="仓库">{{ detail.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="客户">{{ detail.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <EnumTag :value="detail.status" :options="OUTBOUND_STATUS" />
            </el-descriptions-item>
            <el-descriptions-item label="优先级">{{ detail.priority }}</el-descriptions-item>
            <el-descriptions-item label="订购 / 已配 / 已拣 / 已发">
              {{ formatQty(detail.orderedQty) }} / {{ formatQty(detail.allocatedQty) }} /
              {{ formatQty(detail.pickedQty) }} / {{ formatQty(detail.shippedQty) }}
            </el-descriptions-item>
            <el-descriptions-item label="预计发货">
              {{ detail.expectedShipAt || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="3">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">订单明细</div>
          <el-table :data="detail.items" border size="small">
            <el-table-column prop="skuCode" label="SKU" width="110" />
            <el-table-column prop="skuName" label="商品名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="orderedQty" label="订购" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.orderedQty) }}</template>
            </el-table-column>
            <el-table-column prop="allocatedQty" label="已分配" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.allocatedQty) }}</template>
            </el-table-column>
            <el-table-column prop="pickedQty" label="已拣" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.pickedQty) }}</template>
            </el-table-column>
            <el-table-column prop="shippedQty" label="已发" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.shippedQty) }}</template>
            </el-table-column>
          </el-table>

          <div class="section-title">分配记录</div>
          <el-table :data="detail.allocations" border size="small" empty-text="尚未分配库存">
            <el-table-column prop="skuCode" label="SKU" width="110" />
            <el-table-column prop="locationCode" label="库位" width="110" />
            <el-table-column prop="batchNo" label="批次" width="110">
              <template #default="{ row }">{{ row.batchNo || '-' }}</template>
            </el-table-column>
            <el-table-column prop="allocatedQty" label="分配量" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.allocatedQty) }}</template>
            </el-table-column>
            <el-table-column prop="pickedQty" label="已拣" width="100" align="right">
              <template #default="{ row }">{{ formatQty(row.pickedQty) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="95" align="center">
              <template #default="{ row }">
                <EnumTag :value="row.status" :options="ALLOCATION_STATUS" />
              </template>
            </el-table-column>
          </el-table>

          <div class="section-title">发货记录</div>
          <el-table :data="detailShipments" border size="small" empty-text="尚未发货">
            <el-table-column prop="shipmentNo" label="发货单号" width="160" />
            <el-table-column prop="carrier" label="快递公司" width="120">
              <template #default="{ row }">{{ row.carrier || '-' }}</template>
            </el-table-column>
            <el-table-column prop="trackingNo" label="运单号" width="180" />
            <el-table-column prop="shippedAt" label="发货时间" width="170" />
          </el-table>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-drawer>

    <!-- ============ 波次：加入订单 ============ -->
    <el-dialog v-model="addOrdersVisible" title="加入订单到波次" width="820px" destroy-on-close>
      <el-alert type="info" :closable="false" class="pk-tip">
        只能加入「已分配」的订单。下拉里只列出当前仓库下已分配的订单。
      </el-alert>
      <el-form label-width="90px">
        <el-form-item label="可加入订单">
          <el-select
            v-model="addOrderIds"
            multiple
            filterable
            placeholder="请选择订单"
            style="width: 100%"
            :loading="loadingEligibleOrders"
          >
            <el-option
              v-for="o in eligibleOrders"
              :key="o.id"
              :label="`${o.orderNo} · ${o.customerName || '无客户'} · ${formatQty(o.orderedQty)}`"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addOrdersVisible = false">取消</el-button>
        <el-button type="primary" :loading="addOrdersSubmitting" @click="onSubmitAddOrders">
          确认加入
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 波次：新建 ============ -->
    <el-dialog v-model="waveVisible" title="新建拣货波次" width="480px" destroy-on-close>
      <el-form ref="waveRef" :model="waveForm" :rules="waveRules" label-width="90px">
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="waveForm.warehouseId" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="warehouseLabel(w)"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="waveForm.operator" placeholder="如 王五" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="waveVisible = false">取消</el-button>
        <el-button type="primary" :loading="waveSubmitting" @click="onSubmitWave">创建</el-button>
      </template>
    </el-dialog>

    <!-- ============ 发货弹窗 ============ -->
    <el-dialog v-model="shipVisible" title="发货" width="520px" destroy-on-close>
      <el-form ref="shipRef" :model="shipForm" :rules="shipRules" label-width="90px">
        <el-form-item label="快递公司">
          <el-input v-model="shipForm.carrier" placeholder="如 顺丰速运" maxlength="64" />
        </el-form-item>
        <el-form-item label="运单号">
          <el-input v-model="shipForm.trackingNo" placeholder="选填" maxlength="64" />
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="shipForm.operator" placeholder="如 孙七" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="shipForm.remark" type="textarea" :rows="2" maxlength="255" />
        </el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false" class="pk-tip">
        发货会扣减在库量与锁定量，并写入「出库发货」流水，该操作不可撤销。
      </el-alert>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="danger" :loading="shipSubmitting" @click="onSubmitShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- ============ 拣货弹窗 ============ -->
    <el-dialog v-model="pickVisible" title="拣货" width="480px" destroy-on-close>
      <el-alert v-if="pickTask" type="info" :closable="false" class="pk-tip">
        <template #title>
          拣货位 {{ pickTask.fromLocationCode }} · {{ pickTask.skuCode }} {{ pickTask.skuName }} ·
          应拣 {{ formatQty(pickTask.requiredQty) }}（已拣 {{ formatQty(pickTask.pickedQty) }}）
        </template>
      </el-alert>
      <el-form ref="pickRef" :model="pickForm" :rules="pickRules" label-width="90px">
        <el-form-item label="实拣数量" prop="pickedQty">
          <el-input-number
            v-model="pickForm.pickedQty"
            :min="1"
            :max="pickRemaining"
            :step="1"
            :precision="0"
            style="width: 100%"
          />
          <div class="field-hint">本次最多可拣 {{ formatQty(pickRemaining) }}，允许部分拣</div>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="pickForm.operator" placeholder="如 赵六" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pickVisible = false">取消</el-button>
        <el-button type="primary" :loading="pickSubmitting" @click="onSubmitPick">确认拣货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import BizSteps from '@/components/BizSteps.vue'
import EnumTag from '@/components/EnumTag.vue'
import { useTablePage } from '@/composables/useTablePage'
import { useOptions } from '@/composables/useOptions'
import { ApiError, ERROR_CODE } from '@/api/request'
import { outboundApi, pickTaskApi, waveApi } from '@/api/outbound'
import { formatQty } from '@/types/enum'
import {
  ALLOCATION_STATUS,
  OUTBOUND_STATUS,
  OUTBOUND_STEPS,
  PICK_STATUS,
  WAVE_STATUS,
  type OutboundOrder,
  type OutboundOrderDetail,
  type OutboundOrderPayload,
  type OutboundOrderQuery,
  type PickTask,
  type PickTaskQuery,
  type Shipment,
  type Wave,
  type WaveQuery,
} from '@/types/outbound'

const {
  warehouses,
  skus,
  customers,
  loadWarehouses,
  loadSkus,
  loadCustomers,
  warehouseLabel,
  skuLabel,
  customerLabel,
} = useOptions()

const activeTab = ref('orders')

/* ---------------- 订单列表 ---------------- */

const query = reactive<{
  keyword: string
  warehouseId?: number
  customerId?: number
  status?: number
}>({
  keyword: '',
  warehouseId: undefined,
  customerId: undefined,
  status: undefined,
})

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
} = useTablePage<OutboundOrder, OutboundOrderQuery>({
  api: { page: (q: OutboundOrderQuery) => outboundApi.page(q) },
  entityName: '出库订单',
  labelOf: (row) => row.orderNo,
  buildQuery: (): OutboundOrderQuery => ({
    page: page.value,
    size: size.value,
    keyword: query.keyword || undefined,
    warehouseId: query.warehouseId,
    customerId: query.customerId,
    status: query.status,
    startTime: createdRange.value?.[0],
    endTime: createdRange.value?.[1],
  }),
})

function onResetOrders() {
  query.keyword = ''
  query.warehouseId = undefined
  query.customerId = undefined
  query.status = undefined
  createdRange.value = null
  search()
}

/** 40903 的提示文案：后端已经写清缺多少，原样透出 */
const shortageMessage = ref('')

async function handleBizError(e: unknown) {
  if (e instanceof ApiError && e.code === ERROR_CODE.QTY_NOT_ENOUGH) {
    shortageMessage.value = e.message
    if (detailVisible.value && detail.value) {
      // 抽屉已开着，直接在抽屉里显示
      return
    }
    await ElMessageBox.alert(e.message, '库存不足，分配未生效', {
      type: 'error',
      confirmButtonText: '知道了',
    }).catch(() => undefined)
    return
  }
  // 其余错误拦截器已提示
}

async function onSubmit(row: OutboundOrder) {
  try {
    await ElMessageBox.confirm(`确定提交订单「${row.orderNo}」吗？提交后进入待分配。`, '提交确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await outboundApi.submit(row.id)
    ElMessage.success('已提交')
    await refreshAfterAction()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onAllocate(row: OutboundOrder) {
  shortageMessage.value = ''
  try {
    await outboundApi.allocate(row.id)
    ElMessage.success('分配成功，库存已锁定')
    await refreshAfterAction()
  } catch (e) {
    await handleBizError(e)
    await load()
  }
}

async function onCreatePickTasks(row: OutboundOrder) {
  try {
    await outboundApi.createPickTasks(row.id)
    ElMessage.success('已生成拣货任务')
    await refreshAfterAction()
    loadedOnce.pick = false
    if (activeTab.value === 'pick') await loadPick()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onReleaseAllocation(row: OutboundOrder) {
  try {
    await ElMessageBox.confirm(
      `确定释放订单「${row.orderNo}」的全部锁定吗？释放后订单回到待分配。`,
      '释放锁定',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await outboundApi.releaseAllocation(row.id)
    ElMessage.success('锁定已释放')
    await refreshAfterAction()
  } catch {
    /* 拦截器已提示 */
  }
}

async function onCancel(row: OutboundOrder) {
  try {
    await ElMessageBox.confirm(`确定取消订单「${row.orderNo}」吗？已锁定的库存会自动释放。`, '取消确认', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await outboundApi.cancel(row.id)
    ElMessage.success('已取消')
    await refreshAfterAction()
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 新建 / 编辑 ---------------- */

interface DraftItem {
  skuId: number | null
  orderedQty: number | null
}

const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref<number | null>(null)
const editOrderNo = ref('')
const formRef = ref<FormInstance>()

const form = reactive<{
  warehouseId: number | null
  customerId: number | null
  priority: number
  expectedShipAt: string | null
  remark: string
  items: DraftItem[]
}>({
  warehouseId: null,
  customerId: null,
  priority: 0,
  expectedShipAt: null,
  remark: '',
  items: [],
})

const rules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
}

function addItem() {
  form.items.push({ skuId: null, orderedQty: 1 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

function isSkuUsed(skuId: number, current: DraftItem) {
  return form.items.some((item) => item !== current && item.skuId === skuId)
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, {
    warehouseId: null,
    customerId: null,
    priority: 0,
    expectedShipAt: null,
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

async function openEdit(row: OutboundOrder) {
  resetForm()
  try {
    const detail = await outboundApi.detail(row.id)
    Object.assign(form, {
      warehouseId: detail.warehouseId,
      customerId: detail.customerId,
      priority: detail.priority ?? 0,
      expectedShipAt: detail.expectedShipAt ?? null,
      remark: detail.remark ?? '',
      items: detail.items.map((item) => ({
        skuId: item.skuId,
        orderedQty: Number(item.orderedQty),
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

  const filled = form.items.filter((i) => i.skuId && Number(i.orderedQty) > 0)
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
    const payload: OutboundOrderPayload = {
      warehouseId: form.warehouseId,
      customerId: form.customerId,
      priority: form.priority,
      expectedShipAt: form.expectedShipAt || null,
      remark: form.remark.trim() || null,
      items: filled.map((i) => ({ skuId: i.skuId as number, orderedQty: Number(i.orderedQty) })),
    }
    if (isEdit.value && editId.value !== null) {
      await outboundApi.update(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await outboundApi.create(payload)
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
const detail = ref<OutboundOrderDetail | null>(null)
const detailShipments = ref<Shipment[]>([])

async function openDetail(row: OutboundOrder) {
  detailVisible.value = true
  loadingDetail.value = true
  detailShipments.value = []
  try {
    detail.value = await outboundApi.detail(row.id)
    detailShipments.value = (await outboundApi.shipments(row.id)) ?? []
  } catch {
    detail.value = null
  } finally {
    loadingDetail.value = false
  }
}

async function refreshAfterAction() {
  await load()
  if (detail.value) {
    await openDetail(detail.value)
  }
}

/* ---------------- 波次 ---------------- */

const wvQuery = reactive<{ keyword: string; warehouseId?: number; status?: number }>({
  keyword: '',
  warehouseId: undefined,
  status: undefined,
})

const {
  rows: rowsWaves,
  total: totalWaves,
  loading: loadingWaves,
  page: pageWaves,
  size: sizeWaves,
  load: loadWaves,
  search: searchWaves,
  onPageChange: onWavePageChange,
  onSizeChange: onWaveSizeChange,
} = useTablePage<Wave, WaveQuery>({
  api: { page: (q: WaveQuery) => waveApi.page(q) },
  entityName: '波次',
  labelOf: (row) => row.waveNo,
  buildQuery: (): WaveQuery => ({
    page: pageWaves.value,
    size: sizeWaves.value,
    keyword: wvQuery.keyword || undefined,
    warehouseId: wvQuery.warehouseId,
    status: wvQuery.status,
  }),
})

function onResetWaves() {
  wvQuery.keyword = ''
  wvQuery.warehouseId = undefined
  wvQuery.status = undefined
  searchWaves()
}

const waveVisible = ref(false)
const waveSubmitting = ref(false)
const waveRef = ref<FormInstance>()
const waveForm = reactive<{ warehouseId: number | null; operator: string }>({
  warehouseId: null,
  operator: '',
})
const waveRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
}

function openCreateWave() {
  Object.assign(waveForm, { warehouseId: null, operator: '' })
  waveVisible.value = true
}

async function onSubmitWave() {
  const valid = await waveRef.value?.validate().catch(() => false)
  if (!valid || !waveForm.warehouseId) return
  waveSubmitting.value = true
  try {
    await waveApi.create({
      warehouseId: waveForm.warehouseId,
      operator: waveForm.operator.trim() || null,
    })
    ElMessage.success('波次已创建')
    waveVisible.value = false
    await loadWaves()
  } catch {
    /* 拦截器已提示 */
  } finally {
    waveSubmitting.value = false
  }
}

const addOrdersVisible = ref(false)
const addOrdersSubmitting = ref(false)
const loadingEligibleOrders = ref(false)
const addOrderIds = ref<number[]>([])
const eligibleOrders = ref<OutboundOrder[]>([])
const currentWaveId = ref<number | null>(null)

async function openAddOrders(row: Wave) {
  addOrderIds.value = []
  eligibleOrders.value = []
  currentWaveId.value = row.id
  addOrdersVisible.value = true
  loadingEligibleOrders.value = true
  try {
    // 后端只允许加入 status = 3 已分配的订单，直接按状态查，避免用户挑到无效单
    const res = await outboundApi.page({
      page: 1,
      size: 200,
      warehouseId: row.warehouseId,
      status: 3,
    })
    eligibleOrders.value = res?.list ?? []
    if (eligibleOrders.value.length === 0) {
      ElMessage.warning('该仓库下没有「已分配」的订单，请先完成库存分配')
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    loadingEligibleOrders.value = false
  }
}

async function onSubmitAddOrders() {
  if (addOrderIds.value.length === 0) {
    ElMessage.warning('请先选择要加入的订单')
    return
  }
  addOrdersSubmitting.value = true
  try {
    await waveApi.addOrders(currentWaveId.value as number, addOrderIds.value)
    ElMessage.success('已加入订单')
    addOrdersVisible.value = false
    await loadWaves()
  } catch {
    /* 拦截器已提示 */
  } finally {
    addOrdersSubmitting.value = false
  }
}

async function onReleaseWave(row: Wave) {  try {
    await ElMessageBox.confirm(
      `确定释放波次「${row.waveNo}」吗？释放后会按库位编码升序生成拣货任务。`,
      '释放波次',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await waveApi.release(row.id)
    ElMessage.success('波次已释放，拣货任务已生成')
    await loadWaves()
    loadedOnce.pick = false
    if (activeTab.value === 'pick') await loadPick()
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 拣货任务 ---------------- */

const pkQuery = reactive<{ keyword: string; status?: number }>({
  keyword: '',
  status: undefined,
})

const {
  rows: rowsPick,
  total: totalPick,
  loading: loadingPick,
  page: pagePick,
  size: sizePick,
  load: loadPick,
  search: searchPick,
  onPageChange: onPickPageChange,
  onSizeChange: onPickSizeChange,
} = useTablePage<PickTask, PickTaskQuery>({
  api: { page: (q: PickTaskQuery) => pickTaskApi.page(q) },
  entityName: '拣货任务',
  labelOf: (row) => row.taskNo,
  buildQuery: (): PickTaskQuery => ({
    page: pagePick.value,
    size: sizePick.value,
    keyword: pkQuery.keyword || undefined,
    status: pkQuery.status,
    // 按拣货顺序展示（契约里 pick-tasks 支持 waveId/outboundId，没有 waveNo 查询参数）
    sortBy: 'sequence',
    sortOrder: 'asc',
  }),
})

function onResetPick() {
  pkQuery.keyword = ''
  pkQuery.status = undefined
  searchPick()
}

const pickVisible = ref(false)
const pickSubmitting = ref(false)
const pickRef = ref<FormInstance>()
const pickTask = ref<PickTask | null>(null)

const pickForm = reactive<{ pickedQty: number; operator: string }>({
  pickedQty: 1,
  operator: '',
})

const pickRules: FormRules = {
  pickedQty: [{ required: true, message: '请输入实拣数量', trigger: 'blur' }],
  operator: [
    { required: true, message: '请输入操作人', trigger: 'blur' },
    { max: 64, message: '最多 64 个字符', trigger: 'blur' },
  ],
}

const pickRemaining = computed(() => {
  if (!pickTask.value) return 1
  return Math.max(0, Number(pickTask.value.requiredQty) - Number(pickTask.value.pickedQty))
})

function openPick(row: PickTask) {
  pickTask.value = row
  Object.assign(pickForm, { pickedQty: 1, operator: '' })
  pickVisible.value = true
}

async function onSubmitPick() {
  const valid = await pickRef.value?.validate().catch(() => false)
  if (!valid || pickTask.value === null) return
  if (Number(pickForm.pickedQty) < 1 || Number(pickForm.pickedQty) > pickRemaining.value) {
    ElMessage.warning(`实拣数量需在 1 ~ ${pickRemaining.value} 之间`)
    return
  }
  pickSubmitting.value = true
  try {
    await pickTaskApi.pick(pickTask.value.id, {
      pickedQty: Number(pickForm.pickedQty),
      operator: pickForm.operator.trim(),
    })
    ElMessage.success('拣货成功')
    pickVisible.value = false
    await loadPick()
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    pickSubmitting.value = false
  }
}

async function onCancelPick(row: PickTask) {
  try {
    await ElMessageBox.confirm(`确定取消拣货任务「${row.taskNo}」吗？`, '取消确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await pickTaskApi.cancel(row.id)
    ElMessage.success('已取消')
    await loadPick()
  } catch {
    /* 拦截器已提示 */
  }
}

/* ---------------- 发货 ---------------- */

const shipVisible = ref(false)
const shipSubmitting = ref(false)
const shipRef = ref<FormInstance>()
const shipId = ref<number | null>(null)

const shipForm = reactive<{
  carrier: string
  trackingNo: string
  operator: string
  remark: string
}>({
  carrier: '',
  trackingNo: '',
  operator: '',
  remark: '',
})

const shipRules: FormRules = {
  operator: [
    { required: true, message: '请输入操作人', trigger: 'blur' },
    { max: 64, message: '最多 64 个字符', trigger: 'blur' },
  ],
}

function openShip(row: OutboundOrder) {
  shipId.value = row.id
  Object.assign(shipForm, { carrier: '', trackingNo: '', operator: '', remark: '' })
  detailVisible.value = false
  shipVisible.value = true
}

async function onSubmitShip() {
  const valid = await shipRef.value?.validate().catch(() => false)
  if (!valid || shipId.value === null) return
  shipSubmitting.value = true
  try {
    await outboundApi.ship(shipId.value, {
      carrier: shipForm.carrier.trim() || null,
      trackingNo: shipForm.trackingNo.trim() || null,
      operator: shipForm.operator.trim(),
      remark: shipForm.remark.trim() || null,
    })
    ElMessage.success('发货成功')
    shipVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    shipSubmitting.value = false
  }
}

/* ---------------- 生命周期 ---------------- */

const loadedOnce = {
  orders: false,
  waves: false,
  pick: false,
}

async function onTabChange(name: string | number) {
  if (name === 'waves' && !loadedOnce.waves) {
    loadedOnce.waves = true
    await loadWaves()
  } else if (name === 'pick' && !loadedOnce.pick) {
    loadedOnce.pick = true
    await loadPick()
  }
}

onMounted(async () => {
  await Promise.all([loadWarehouses(), loadSkus(), loadCustomers()])
  loadedOnce.orders = true
  await load()
})
</script>

<style scoped>
.sub-text {
  font-size: 12px;
  color: #909399;
}

.qty-chain {
  font-size: 12px;
  color: #606266;
}

.qty-positive {
  color: #16a34a;
  font-weight: 600;
}

.seq-badge {
  display: inline-block;
  min-width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 4px;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.section-title {
  margin: 20px 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.shortage-alert {
  margin-bottom: 12px;
}

.shortage-text {
  white-space: pre-wrap;
  font-size: 13px;
  line-height: 1.7;
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

.field-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.pk-tip {
  margin-bottom: 12px;
}
</style>
