<template>
  <el-tag :type="flag === 1 ? 'success' : 'info'" size="small" effect="light">
    {{ flag === 1 ? '启用' : '停用' }}
  </el-tag>
</template>

<script setup lang="ts">
defineProps<{ flag: 0 | 1 }>()
</script>
