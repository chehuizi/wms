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
      <el-form-item label="分类">
        <el-select
          v-model="query.category"
          placeholder="全部分类"
          clearable
          filterable
          style="width: 150px"
          @change="search"
        >
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="批次管理">
        <el-select v-model="query.batchManaged" placeholder="全部" clearable style="width: 120px" @change="search">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="序列号管理">
        <el-select v-model="query.serialManaged" placeholder="全部" clearable style="width: 130px" @change="search">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
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
        <el-button type="primary" :icon="Plus" plain @click="openCreate">新增商品</el-button>
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
    <el-table v-loading="loading" :data="rows" border stripe row-key="id" @selection-change="onSelectionChange">
      <template #empty>
        <el-empty class="table-empty" description="暂无商品数据" :image-size="80" />
      </template>

      <el-table-column type="selection" width="48" />
      <el-table-column prop="code" label="商品编码" min-width="130" />
      <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="110">
        <template #default="{ row }">{{ row.category || '-' }}</template>
      </el-table-column>
      <el-table-column prop="spec" label="规格" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.spec || '-' }}</template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80">
        <template #default="{ row }">{{ row.unit || '-' }}</template>
      </el-table-column>
      <el-table-column prop="barcode" label="条码" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.barcode || '-' }}</template>
      </el-table-column>
      <el-table-column prop="batchManaged" label="批次" width="80" align="center">
        <template #default="{ row }">
          <FlagTag :flag="row.batchManaged" />
        </template>
      </el-table-column>
      <el-table-column prop="serialManaged" label="序列号" width="90" align="center">
        <template #default="{ row }">
          <FlagTag :flag="row.serialManaged" />
        </template>
      </el-table-column>
      <el-table-column prop="shelfLifeDays" label="保质期(天)" width="110" align="right">
        <template #default="{ row }">{{ row.shelfLifeDays ?? '无' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :flag="row.status" />
        </template>
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
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="760px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品编码" prop="code">
              <el-input v-model="form.code" placeholder="如 SKU-1001" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="如 农夫山泉 550ml" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select
                v-model="form.category"
                placeholder="可输入新分类"
                filterable
                allow-create
                default-first-option
                clearable
                style="width: 100%"
              >
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="form.spec" placeholder="如 550ml × 24瓶/箱" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如 箱" maxlength="16" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="条码" prop="barcode">
              <el-input v-model="form.barcode" placeholder="选填" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="批次管理" prop="batchManaged">
              <el-switch v-model="form.batchManaged" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="序列号管理" prop="serialManaged">
              <el-switch v-model="form.serialManaged" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保质期(天)" prop="shelfLifeDays">
              <el-input-number
                v-model="form.shelfLifeDays"
                :min="0"
                :precision="0"
                controls-position="right"
                style="width: 100%"
                placeholder="0 或留空表示无保质期"
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
          <el-col :span="24">
            <el-divider content-position="left">尺寸与重量（选填，单位 kg / m）</el-divider>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重量" prop="weight" label-width="60px">
              <el-input-number v-model="form.weight" :min="0" :precision="4" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="体积" prop="volume" label-width="60px">
              <el-input-number v-model="form.volume" :min="0" :precision="4" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="长度" prop="length" label-width="60px">
              <el-input-number v-model="form.length" :min="0" :precision="4" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="宽度" prop="width" label-width="60px">
              <el-input-number v-model="form.width" :min="0" :precision="4" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高度" prop="height" label-width="60px">
              <el-input-number v-model="form.height" :min="0" :precision="4" :controls="false" style="width: 100%" />
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
import { skuApi } from '@/api/sku'
import type { Sku, SkuPayload, SkuQuery } from '@/types/sku'
import type { StatusFlag } from '@/types/common'

const query = reactive<{
  keyword: string
  category: string | undefined
  batchManaged: StatusFlag | undefined
  serialManaged: StatusFlag | undefined
  status: StatusFlag | undefined
}>({
  keyword: '',
  category: undefined,
  batchManaged: undefined,
  serialManaged: undefined,
  status: undefined,
})

const categoryOptions = ref<string[]>([])

async function loadCategories() {
  try {
    const list = await skuApi.categories()
    categoryOptions.value = Array.isArray(list) ? list : []
  } catch {
    categoryOptions.value = []
  }
}

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref<number | null>(null)

interface FormState {
  code: string
  name: string
  category: string
  spec: string
  unit: string
  barcode: string
  batchManaged: StatusFlag
  serialManaged: StatusFlag
  shelfLifeDays: number | undefined
  weight: number | undefined
  volume: number | undefined
  length: number | undefined
  width: number | undefined
  height: number | undefined
  status: StatusFlag
  remark: string
}

function emptyForm(): FormState {
  return {
    code: '',
    name: '',
    category: '',
    spec: '',
    unit: '',
    barcode: '',
    batchManaged: 0,
    serialManaged: 0,
    shelfLifeDays: undefined,
    weight: undefined,
    volume: undefined,
    length: undefined,
    width: undefined,
    height: undefined,
    status: 1,
    remark: '',
  }
}

const form = reactive<FormState>(emptyForm())

const rules: FormRules<FormState> = {
  code: [
    { required: true, message: '请输入商品编码', trigger: 'blur' },
    { min: 1, max: 64, message: '长度为 1-64 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度为 1-128 个字符', trigger: 'blur' },
  ],
  category: [{ max: 64, message: '最多 64 个字符', trigger: 'blur' }],
  spec: [{ max: 64, message: '最多 64 个字符', trigger: 'blur' }],
  unit: [{ max: 16, message: '最多 16 个字符', trigger: 'blur' }],
  barcode: [{ max: 64, message: '最多 64 个字符', trigger: 'blur' }],
  remark: [{ max: 255, message: '最多 255 个字符', trigger: 'blur' }],
}

const { rows, total, loading, selection, page, size, load, search, onPageChange, onSizeChange, handleDelete, handleBatchDelete } =
  useTablePage<Sku, SkuQuery>({
    api: skuApi,
    entityName: '商品',
    buildQuery: (): SkuQuery => ({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      category: query.category,
      batchManaged: query.batchManaged,
      serialManaged: query.serialManaged,
      status: query.status,
    }),
  })

function onSelectionChange(val: Sku[]) {
  selection.value = val
}

function onReset() {
  query.keyword = ''
  query.category = undefined
  query.batchManaged = undefined
  query.serialManaged = undefined
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
}

function openEdit(row: Sku) {
  Object.assign(form, {
    code: row.code,
    name: row.name,
    category: row.category ?? '',
    spec: row.spec ?? '',
    unit: row.unit ?? '',
    barcode: row.barcode ?? '',
    batchManaged: row.batchManaged,
    serialManaged: row.serialManaged,
    shelfLifeDays: row.shelfLifeDays ?? undefined,
    weight: row.weight ?? undefined,
    volume: row.volume ?? undefined,
    length: row.length ?? undefined,
    width: row.width ?? undefined,
    height: row.height ?? undefined,
    status: row.status,
    remark: row.remark ?? '',
  })
  editId.value = row.id
  isEdit.value = true
  dialogVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: SkuPayload = {
      code: form.code.trim(),
      name: form.name.trim(),
      category: form.category.trim() || null,
      spec: form.spec.trim() || null,
      unit: form.unit.trim() || null,
      barcode: form.barcode.trim() || null,
      batchManaged: form.batchManaged,
      serialManaged: form.serialManaged,
      shelfLifeDays: form.shelfLifeDays || null,
      weight: form.weight ?? null,
      volume: form.volume ?? null,
      length: form.length ?? null,
      width: form.width ?? null,
      height: form.height ?? null,
      status: form.status,
      remark: form.remark.trim() || null,
    }
    if (isEdit.value && editId.value !== null) {
      await skuApi.update(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await skuApi.create(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    // 新分类可能刚被录入，刷新一下候选项
    await Promise.all([load(), loadCategories()])
  } catch {
    /* 拦截器已提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadCategories()])
})
</script>
