<template>
  <el-tag v-if="matched" :type="matched.type || 'info'" size="small" effect="light">
    {{ matched.label }}
  </el-tag>
  <!-- 后端返回了枚举外的值，不猜，直接把原始值显示出来便于排查 -->
  <el-tag v-else type="info" size="small" effect="plain">{{ value ?? '-' }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EnumOption } from '@/types/enum'

/**
 * 通用枚举标签：把契约里的数字枚举渲染成中文。
 * 各模块的选项数组定义在自己的 types 文件里（契约 §12 是事实来源）。
 */
const props = defineProps<{
  value: number | null | undefined
  options: EnumOption[]
}>()

const matched = computed(() => props.options.find((o) => o.value === props.value))
</script>
