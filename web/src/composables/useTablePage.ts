import { ref, type Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

export interface TableApi<T, Q> {
  page: (query: Q) => Promise<{ list: T[]; total: number; page: number; size: number }>
  /** 业务模块（库存台账、库存流水、拣货任务…）本身没有删除能力，不实现即可 */
  remove?: (id: number) => Promise<unknown>
  batchRemove?: (ids: number[]) => Promise<unknown>
}

export interface TableOptions<T, Q> {
  /** 由页面把查询参数（含页面私有筛选项）转成请求参数 */
  buildQuery: () => Q
  api: TableApi<T, Q>
  /** 删除提示里的实体名，如「仓库」 */
  entityName: string
  /** 可选的删除前守卫，返回 false 阻止删除 */
  beforeDelete?: (row: T) => Promise<boolean> | boolean
  /** 确认框里显示的实体标识，默认取 name/id；单据类传 orderNo 这类单号更清楚 */
  labelOf?: (row: T) => string
}

/**
 * 列表页公共逻辑：分页 + 加载 + 选中 + 单条/批量删除。
 * 各页面自行负责表单弹窗与表格列定义。
 */
export function useTablePage<T extends { id: number }, Q>(options: TableOptions<T, Q>) {
  const rows = ref([]) as Ref<T[]>
  const total = ref(0)
  const loading = ref(false)
  const selection = ref<T[]>([])
  const page = ref(1)
  const size = ref(20)

  async function load() {
    loading.value = true
    try {
      const params = options.buildQuery()
      const result = await options.api.page(params)
      rows.value = result?.list ?? []
      total.value = result?.total ?? 0
      // 后端返回总页数变少时回退，避免停在空白页
      const maxPage = Math.max(1, Math.ceil(total.value / size.value))
      if (page.value > maxPage) {
        page.value = maxPage
        await load()
      }
    } catch {
      // 拦截器已提示，这里保证列表不白屏
      rows.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  function search() {
    page.value = 1
    return load()
  }

  function reset() {
    page.value = 1
    return load()
  }

  async function onPageChange(next: number) {
    page.value = next
    await load()
  }

  async function onSizeChange(next: number) {
    size.value = next
    page.value = 1
    await load()
  }

  async function handleDelete(row: T) {
    if (!options.api.remove) return
    const label = options.labelOf
      ? options.labelOf(row)
      : String((row as Record<string, unknown>).name ?? row.id)
    try {
      await ElMessageBox.confirm(
        `确定要删除${options.entityName}「${label}」吗？该操作为软删除。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
      )
    } catch {
      return // 用户取消
    }
    if (options.beforeDelete && !(await options.beforeDelete(row))) {
      return
    }
    try {
      await options.api.remove(row.id)
      ElMessage.success('删除成功')
      await load()
    } catch {
      /* 拦截器已提示 */
    }
  }

  async function handleBatchDelete() {
    if (!options.api.batchRemove) return
    const ids = selection.value.map((item) => item.id)
    if (ids.length === 0) {
      ElMessage.warning('请先勾选要删除的数据')
      return
    }
    try {
      await ElMessageBox.confirm(
        `确定要删除选中的 ${ids.length} 条${options.entityName}吗？该操作为软删除。`,
        '批量删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
    try {
      await options.api.batchRemove(ids)
      ElMessage.success(`已删除 ${ids.length} 条`)
      selection.value = []
      await load()
    } catch {
      /* 拦截器已提示 */
    }
  }

  return {
    rows,
    total,
    loading,
    selection,
    page,
    size,
    load,
    search,
    reset,
    onPageChange,
    onSizeChange,
    handleDelete,
    handleBatchDelete,
  }
}
