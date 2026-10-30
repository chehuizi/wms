<template>
  <div class="page-card">
    <!-- 查询区 -->
    <el-form :inline="true" class="page-toolbar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称"
          clearable
          style="width: 180px"
          @keyup.enter="search"
          @clear="search"
        />
      </el-form-item>
      <el-form-item label="所属仓库">
        <el-select
          v-model="query.warehouseId"
          placeholder="全部仓库"
          clearable
          filterable
          style="width: 180px"
          @change="search"
        >
          <el-option v-for="w in warehouseOptions" :key="w.id" :label="`${w.code} ${w.name}`" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="库位类型">
        <el-select
          v-model="query.locationType"
          placeholder="全部"
          clearable
          style="width: 140px"
          @change="search"
        >
          <el-option v-for="opt in LOCATION_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="库区">
        <el-input
          v-model="query.zone"
          placeholder="库区编码"
          clearable
          style="width: 120px"
          @keyup.enter="search"
          @clear="search"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px" @change="search">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <el-button type="primary" :icon="Plus" plain @click="openCreate">新增库位</el-button>
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :disabled="selection.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 表格区 -->
    <el-table
      v-loading="loading"
      :data="rows"
      border
      stripe
      row-key="id"
      @selection-change="onSelectionChange"
    >
      <template #empty>
        <el-empty class="table-empty" description="暂无库位数据" :image-size="80" />
      </template>

      <el-table-column type="selection" width="48" />
      <el-table-column prop="code" label="库位编码" min-width="120" />
      <el-table-column prop="warehouseName" label="所属仓库" min-width="140" show-overflow-tooltip />
      <el-table-column prop="zone" label="库区" width="90" />
      <el-table-column prop="locationType" label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ typeLabel(row.locationType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="capacity" label="容量" width="110" align="right">
        <template #default="{ row }">{{ row.capacity ?? '不限' }}</template>
      </el-table-column>
      <el-table-column prop="allowNegative" label="允许负库存" width="110" align="center">
        <template #default="{ row }">
          <FlagTag :flag="row.allowNegative" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :flag="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑库位' : '新增库位'"
      width="680px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属仓库" prop="warehouseId">
              <el-select
                v-model="form.warehouseId"
                placeholder="请选择启用中的仓库"
                filterable
                style="width: 100%"
                @change="onWarehouseChange"
              >
                <el-option v-for="w in warehouseOptions" :key="w.id" :label="`${w.code} ${w.name}`" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库位编码" prop="code">
              <el-input v-model="form.code" placeholder="如 A-01-01" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库区" prop="zone">
              <el-select
                v-model="form.zone"
                placeholder="请选择所属仓库下的库区"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="zone in zoneOptions"
                  :key="zone.id"
                  :label="`${zone.code} ${zone.name}`"
                  :value="zone.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库位类型" prop="locationType">
              <el-select v-model="form.locationType" style="width: 100%">
                <el-option v-for="opt in LOCATION_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量" prop="capacity">
              <el-input-number
                v-model="form.capacity"
                :min="0"
                :precision="4"
                :step="1"
                controls-position="right"
                style="width: 100%"
                placeholder="不限制请留空"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许负库存" prop="allowNegative">
              <el-switch
                v-model="form.allowNegative"
                :active-value="1"
                :inactive-value="0"
                active-text="允许"
                inactive-text="不允许"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="255" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import FlagTag from '@/components/FlagTag.vue'
import { useTablePage } from '@/composables/useTablePage'
import { locationApi } from '@/api/location'
import { warehouseApi } from '@/api/warehouse'
import { zoneApi } from '@/api/zone'
import type { Location, LocationPayload, LocationQuery, LocationTypeValue } from '@/types/location'
import type { Zone } from '@/types/zone'
import { LOCATION_TYPE, LOCATION_TYPE_OPTIONS } from '@/types/location'
import type { Warehouse } from '@/types/warehouse'
import type { StatusFlag } from '@/types/common'

const query = reactive<{
  keyword: string
  warehouseId: number | undefined
  locationType: LocationTypeValue | undefined
  zone: string
  status: StatusFlag | undefined
}>({
  keyword: '',
  warehouseId: undefined,
  locationType: undefined,
  zone: '',
  status: undefined,
})

/** 下拉数据源：只取启用中的仓库 */
const warehouseOptions = ref<Warehouse[]>([])
/** 当前仓库下可选择的库区，保存时提交库区编码。 */
const zoneOptions = ref<Zone[]>([])

async function loadWarehouses() {
  try {
    const res = await warehouseApi.page({ page: 1, size: 200, status: 1 })
    warehouseOptions.value = res?.list ?? []
  } catch {
    warehouseOptions.value = []
  }
}

async function loadZones(warehouseId: number | undefined, selectedZone?: string) {
  if (warehouseId === undefined) {
    zoneOptions.value = []
    return
  }
  try {
    const res = await zoneApi.page({ page: 1, size: 200, warehouseId })
    zoneOptions.value = res?.list ?? []
    if (selectedZone && !zoneOptions.value.some((zone) => zone.code === selectedZone)) {
      zoneOptions.value = []
    }
  } catch {
    zoneOptions.value = []
  }
}

async function onWarehouseChange() {
  form.zone = ''
  await loadZones(form.warehouseId)
}

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref<number | null>(null)

interface FormState {
  warehouseId: number | undefined
  code: string
  zone: string
  locationType: LocationTypeValue
  capacity: number | undefined
  allowNegative: StatusFlag
  status: StatusFlag
  remark: string
}

function emptyForm(): FormState {
  return {
    warehouseId: undefined,
    code: '',
    zone: '',
    locationType: 1,
    capacity: undefined,
    allowNegative: 0,
    status: 1,
    remark: '',
  }
}

const form = reactive<FormState>(emptyForm())

const rules: FormRules<FormState> = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  code: [
    { required: true, message: '请输入库位编码', trigger: 'blur' },
    { min: 1, max: 32, message: '长度为 1-32 个字符', trigger: 'blur' },
  ],
  zone: [
    { required: true, message: '请输入库区编码', trigger: 'blur' },
    { min: 1, max: 32, message: '长度为 1-32 个字符', trigger: 'blur' },
  ],
  capacity: [
    {
      validator: (_rule, value, callback) => {
        if (value !== undefined && value !== null && value <= 0) {
          callback(new Error('容量需大于 0，留空表示不限制'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  remark: [{ max: 255, message: '最多 255 个字符', trigger: 'blur' }],
}

const { rows, total, loading, selection, page, size, load, search, onPageChange, onSizeChange, handleDelete, handleBatchDelete } =
  useTablePage<Location, LocationQuery>({
    api: locationApi,
    entityName: '库位',
    buildQuery: (): LocationQuery => ({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      warehouseId: query.warehouseId,
      locationType: query.locationType,
      zone: query.zone || undefined,
      status: query.status,
    }),
  })

function onSelectionChange(val: Location[]) {
  selection.value = val
}

function typeLabel(value: unknown): string {
  return LOCATION_TYPE[value as LocationTypeValue] ?? String(value ?? '-')
}

function onReset() {
  query.keyword = ''
  query.warehouseId = undefined
  query.locationType = undefined
  query.zone = ''
  query.status = undefined
  search()
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, emptyForm())
  editId.value = null
  isEdit.value = false
}

function openCreate() {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
  void loadZones(form.warehouseId)
}

function openEdit(row: Location) {
  Object.assign(form, {
    warehouseId: row.warehouseId,
    code: row.code,
    zone: row.zone,
    locationType: row.locationType,
    capacity: row.capacity ?? undefined,
    allowNegative: row.allowNegative,
    status: row.status,
    remark: row.remark ?? '',
  })
  editId.value = row.id
  isEdit.value = true
  dialogVisible.value = true
  void loadZones(form.warehouseId, row.zone)
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.warehouseId === undefined) return

  submitting.value = true
  try {
    const payload: LocationPayload = {
      warehouseId: form.warehouseId,
      code: form.code.trim(),
      zone: form.zone.trim(),
      locationType: form.locationType,
      capacity: form.capacity ?? null,
      allowNegative: form.allowNegative,
      status: form.status,
      remark: form.remark.trim() || null,
    }
    if (isEdit.value && editId.value !== null) {
      await locationApi.update(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await locationApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadWarehouses()])
})
</script>
