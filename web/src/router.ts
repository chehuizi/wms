import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      // 作业：库存 / 入库 / 出库 / 盘点
      {
        path: 'inventory',
        name: 'Inventory',
        component: () => import('@/views/inventory/index.vue'),
        meta: { title: '库存台账' },
      },
      {
        path: 'inbound',
        name: 'Inbound',
        component: () => import('@/views/inbound/index.vue'),
        meta: { title: '入库管理' },
      },
      {
        path: 'outbound',
        name: 'Outbound',
        component: () => import('@/views/outbound/index.vue'),
        meta: { title: '出库管理' },
      },
      {
        path: 'stocktake',
        name: 'Stocktake',
        component: () => import('@/views/stocktake/index.vue'),
        meta: { title: '盘点管理' },
      },
      // 分析
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '看板报表' },
      },
      // 主数据
      {
        path: 'warehouse',
        name: 'Warehouse',
        component: () => import('@/views/warehouse/index.vue'),
        meta: { title: '仓库管理' },
      },
      {
        path: 'location',
        name: 'Location',
        component: () => import('@/views/location/index.vue'),
        meta: { title: '库位管理' },
      },
      {
        path: 'sku',
        name: 'Sku',
        component: () => import('@/views/sku/index.vue'),
        meta: { title: '商品管理' },
      },
      {
        path: 'customer',
        name: 'Customer',
        component: () => import('@/views/customer/index.vue'),
        meta: { title: '客户管理' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  // base 跟随 Vite 的 base：本地 '/' ，部署到子路径时 '/wms/'。
  // 不带这个参数的话，history 模式会把 /wms/dashboard 当成一条不存在的路由，
  // 页面能加载（nginx 正常返回 index.html）但渲染出的是应用自己的 404。
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.afterEach((to) => {
  const title = (to.meta.title as string) || ''
  document.title = title ? `${title} · WMS 仓储管理系统` : 'WMS 仓储管理系统'
})

export default router
