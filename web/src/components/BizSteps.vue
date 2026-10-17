<template>
  <div class="biz-steps">
    <el-steps v-if="activeIndex >= 0" :active="activeIndex" align-center>
      <el-step v-for="step in steps" :key="step.value" :title="step.label" />
    </el-steps>
    <!-- 已取消不在主链上，单独用 tag 表达，避免把流程图显示成"回退" -->
    <div v-else class="biz-steps-cancelled">
      <el-tag type="danger" size="small" effect="light">{{ currentLabel }}</el-tag>
      <span class="biz-steps-cancelled-tip">该单据不在正常流转链上</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EnumOption } from '@/types/enum'

/**
 * 单据状态流转条。
 * steps 传主链状态（不含已取消等旁路状态），current 传当前状态值。
 */
const props = defineProps<{
  steps: EnumOption[]
  current: number
  options?: EnumOption[]
}>()

/** el-steps 的 active 是 0 基下标 */
const activeIndex = computed(() => props.steps.findIndex((s) => s.value === props.current))

const currentLabel = computed(
  () => (props.options ?? props.steps).find((o) => o.value === props.current)?.label ?? '未知状态',
)
</script>

<style scoped>
.biz-steps {
  padding: 8px 0 4px;
}

.biz-steps-cancelled {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
}

.biz-steps-cancelled-tip {
  font-size: 12px;
  color: #909399;
}
</style>
