<template>
  <el-tag v-if="flag === 1" type="primary" size="small" effect="plain">是</el-tag>
  <el-tag v-else type="info" size="small" effect="plain">否</el-tag>
</template>

<script setup lang="ts">
defineProps<{ flag: 0 | 1 }>()
</script>
