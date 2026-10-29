<template>
  <div class="page-card">
    <el-form :inline="true" class="page-toolbar" @submit.prevent>
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称"
          clearable
          style="width: 200px"
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
          style="width: 200px"
          @change="search"
        >
          <el-option
            v-for="warehouse in warehouseOptions"
            :key="warehouse.id"
            :label="`${warehouse.code} ${warehouse.name}`"
            :value="warehouse.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="query.status"
          placeholder="全部"
          clearable
          style="width: 140px"
          @change="search"
        >
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <el-button type="primary" :icon="Plus" plain @click="openCreate">新增库区</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows" border stripe row-key="id">
      <template #empty>
        <el-empty class="table-empty" description="暂无库区数据" :image-size="80" />
      </template>
      <el-table-column prop="code" label="库区编码" min-width="120" />
      <el-table-column prop="name" label="库区名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="所属仓库" min-width="160" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <StatusTag :flag="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑库区' : '新增库区'"
      width="600px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select
            v-model="form.warehouseId"
            placeholder="请选择启用中的仓库"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="warehouse in warehouseOptions"
              :key="warehouse.id"
              :label="`${warehouse.code} ${warehouse.name}`"
              :value="warehouse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="库区编码" prop="code">
          <el-input v-model="form.code" placeholder="如 A" maxlength="32" />
        </el-form-item>
        <el-form-item label="库区名称" prop="name">
          <el-input v-model="form.name" placeholder="如 A区" maxlength="128" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="选填"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
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
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { useTablePage } from '@/composables/useTablePage'
import { zoneApi } from '@/api/zone'
import { warehouseApi } from '@/api/warehouse'
import type { Zone, ZonePayload, ZoneQuery } from '@/types/zone'
import type { Warehouse } from '@/types/warehouse'
import type { StatusFlag } from '@/types/common'

const query = reactive<{
  keyword: string
  warehouseId: number | undefined
  status: StatusFlag | undefined
}>({
  keyword: '',
  warehouseId: undefined,
  status: undefined,
})

const warehouseOptions = ref<Warehouse[]>([])

async function loadWarehouses() {
  try {
    const response = await warehouseApi.page({ page: 1, size: 200, status: 1 })
    warehouseOptions.value = response?.list ?? []
  } catch {
    warehouseOptions.value = []
  }
}

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref<number | null>(null)

interface FormState {
  warehouseId: number | undefined
  code: string
  name: string
  remark: string
}

function emptyForm(): FormState {
  return {
    warehouseId: undefined,
    code: '',
    name: '',
    remark: '',
  }
}

const form = reactive<FormState>(emptyForm())

const rules: FormRules<FormState> = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  code: [
    { required: true, message: '请输入库区编码', trigger: 'blur' },
    { min: 1, max: 32, message: '长度为 1-32 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入库区名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度为 1-128 个字符', trigger: 'blur' },
  ],
  remark: [{ max: 255, message: '最多 255 个字符', trigger: 'blur' }],
}

const { rows, total, loading, page, size, load, search, onPageChange, onSizeChange, handleDelete } =
  useTablePage<Zone, ZoneQuery>({
    api: zoneApi,
    entityName: '库区',
    buildQuery: (): ZoneQuery => ({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      warehouseId: query.warehouseId,
      status: query.status,
    }),
  })

function onReset() {
  query.keyword = ''
  query.warehouseId = undefined
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

function openEdit(row: Zone) {
  Object.assign(form, {
    warehouseId: row.warehouseId,
    code: row.code,
    name: row.name,
    remark: row.remark ?? '',
  })
  editId.value = row.id
  isEdit.value = true
  dialogVisible.value = true
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.warehouseId === undefined) return

  submitting.value = true
  try {
    const payload: ZonePayload = {
      warehouseId: form.warehouseId,
      code: form.code.trim(),
      name: form.name.trim(),
      remark: form.remark.trim() || null,
    }
    if (isEdit.value && editId.value !== null) {
      await zoneApi.update(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await zoneApi.create(payload)
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
