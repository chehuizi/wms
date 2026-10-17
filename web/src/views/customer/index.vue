<template>
  <div class="page-card">
    <!-- 查询区 -->
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
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px" @change="search">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <el-button type="primary" :icon="Plus" plain @click="openCreate">新增客户</el-button>
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
        <el-empty class="table-empty" description="暂无客户数据" :image-size="80" />
      </template>

      <el-table-column type="selection" width="48" />
      <el-table-column prop="code" label="客户编码" min-width="120" />
      <el-table-column prop="name" label="客户名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" width="100">
        <template #default="{ row }">{{ row.contactName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="contactPhone" label="联系电话" width="130">
        <template #default="{ row }">{{ row.contactPhone || '-' }}</template>
      </el-table-column>
      <el-table-column prop="contactEmail" label="邮箱" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.contactEmail || '-' }}</template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.address || '-' }}</template>
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
      :title="isEdit ? '编辑客户' : '新增客户'"
      width="680px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户编码" prop="code">
              <el-input v-model="form.code" placeholder="如 CUS001" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="form.name" placeholder="如 上海永辉超市采购部" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="选填" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="选填" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="contactEmail">
              <el-input v-model="form.contactEmail" placeholder="选填，需符合邮箱格式" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址" prop="address">
              <el-input v-model="form.address" placeholder="选填" maxlength="255" />
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
import { useTablePage } from '@/composables/useTablePage'
import { customerApi } from '@/api/customer'
import type { Customer, CustomerPayload, CustomerQuery } from '@/types/customer'
import type { StatusFlag } from '@/types/common'

const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const query = reactive<{ keyword: string; status: StatusFlag | undefined }>({
  keyword: '',
  status: undefined,
})

const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const editId = ref<number | null>(null)

interface FormState {
  code: string
  name: string
  contactName: string
  contactPhone: string
  contactEmail: string
  address: string
  status: StatusFlag
  remark: string
}

function emptyForm(): FormState {
  return {
    code: '',
    name: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    address: '',
    status: 1,
    remark: '',
  }
}

const form = reactive<FormState>(emptyForm())

const rules: FormRules<FormState> = {
  code: [
    { required: true, message: '请输入客户编码', trigger: 'blur' },
    { min: 1, max: 32, message: '长度为 1-32 个字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { min: 1, max: 128, message: '长度为 1-128 个字符', trigger: 'blur' },
  ],
  contactName: [{ max: 64, message: '最多 64 个字符', trigger: 'blur' }],
  contactPhone: [{ max: 32, message: '最多 32 个字符', trigger: 'blur' }],
  contactEmail: [
    { max: 128, message: '最多 128 个字符', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value && !EMAIL_RE.test(value)) {
          callback(new Error('邮箱格式不正确'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  address: [{ max: 255, message: '最多 255 个字符', trigger: 'blur' }],
  remark: [{ max: 255, message: '最多 255 个字符', trigger: 'blur' }],
}

const { rows, total, loading, selection, page, size, load, search, onPageChange, onSizeChange, handleDelete, handleBatchDelete } =
  useTablePage<Customer, CustomerQuery>({
    api: customerApi,
    entityName: '客户',
    buildQuery: (): CustomerQuery => ({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      status: query.status,
    }),
  })

function onSelectionChange(val: Customer[]) {
  selection.value = val
}

function onReset() {
  query.keyword = ''
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

function openEdit(row: Customer) {
  Object.assign(form, {
    code: row.code,
    name: row.name,
    contactName: row.contactName ?? '',
    contactPhone: row.contactPhone ?? '',
    contactEmail: row.contactEmail ?? '',
    address: row.address ?? '',
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
    const payload: CustomerPayload = {
      code: form.code.trim(),
      name: form.name.trim(),
      contactName: form.contactName.trim() || null,
      contactPhone: form.contactPhone.trim() || null,
      contactEmail: form.contactEmail.trim() || null,
      address: form.address.trim() || null,
      status: form.status,
      remark: form.remark.trim() || null,
    }
    if (isEdit.value && editId.value !== null) {
      await customerApi.update(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await customerApi.create(payload)
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

onMounted(load)
</script>
