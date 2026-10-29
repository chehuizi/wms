<template>
  <el-container class="layout-root">
    <el-aside width="220px" class="layout-aside">
      <div class="brand">
        <div class="brand-title">WMS 仓储管理</div>
        <div class="brand-sub">Warehouse Management</div>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="layout-menu"
        background-color="#20222a"
        text-color="#c8ccd4"
        active-text-color="#ffffff"
      >
        <el-sub-menu v-for="group in menuGroups" :key="group.title" :index="group.title">
          <template #title>
            <span class="menu-group-title">{{ group.title }}</span>
          </template>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon>
              <component :is="item.icon" />
            </el-icon>
            <div class="menu-text">
              <div class="menu-title">{{ item.title }}</div>
              <div class="menu-sub">{{ item.sub }}</div>
            </div>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header height="56px" class="layout-header">
        <div class="header-left">
          <span class="header-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <span class="header-hint">WMS v1.0</span>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="4">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Box,
  DataLine,
  Download,
  Goods,
  Location,
  OfficeBuilding,
  Tickets,
  Upload,
  User,
} from '@element-plus/icons-vue'

const route = useRoute()

// 菜单按业务分组：日常先看作业，其次看分析，最后维护主数据
const menuGroups = [
  {
    title: '作业',
    items: [
      { path: '/inventory', title: '库存台账', sub: 'Inventory', icon: Box },
      { path: '/inbound', title: '入库管理', sub: 'Inbound', icon: Download },
      { path: '/outbound', title: '出库管理', sub: 'Outbound', icon: Upload },
      { path: '/stocktake', title: '盘点管理', sub: 'Stocktake', icon: Tickets },
    ],
  },
  {
    title: '分析',
    items: [{ path: '/dashboard', title: '看板报表', sub: 'Dashboard', icon: DataLine }],
  },
  {
    title: '主数据',
    items: [
      { path: '/warehouse', title: '仓库管理', sub: 'Warehouses', icon: OfficeBuilding },
      { path: '/zone', title: '库区管理', sub: 'Zones', icon: Location },
      { path: '/location', title: '库位管理', sub: 'Locations', icon: Location },
      { path: '/sku', title: '商品管理', sub: 'SKUs', icon: Goods },
      { path: '/customer', title: '客户管理', sub: 'Customers', icon: User },
    ],
  },
]

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => (route.meta.title as string) || '')
</script>

<style scoped>
.layout-root {
  height: 100vh;
}

.layout-aside {
  background: #20222a;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.brand {
  height: 56px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid #2d3038;
  flex-shrink: 0;
}

.brand-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
}

.brand-sub {
  color: #7f8595;
  font-size: 11px;
  margin-top: 1px;
}

.layout-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

.menu-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  overflow: hidden;
}

.menu-title {
  font-size: 14px;
}

.menu-sub {
  font-size: 11px;
  color: var(--wms-menu-sub);
  margin-top: 1px;
}

.menu-group-title {
  font-size: 12px;
  color: #8b90a0;
  letter-spacing: 1px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background: #2f3442;
}

.layout-menu :deep(.el-menu-item.is-active) .menu-sub {
  color: #b8c0d0;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
}

.header-hint {
  color: #909399;
  font-size: 12px;
}

.layout-main {
  padding: 16px;
  overflow-y: auto;
}
</style>
