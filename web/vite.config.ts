import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * 部署子路径时（如挂在 /wms/ 下），通过 VITE_BASE 指定，例如 `VITE_BASE=/wms/`。
 * 不设时默认为根路径，本地开发无需任何额外配置。
 *
 * 注意：base 结尾的斜杠必须有，Vite 会据此拼接静态资源路径。
 */
const base = process.env.VITE_BASE || '/'

// 前端源码一律使用相对路径 /api/v1/...，由 dev server 代理到后端，避免跨域。
export default defineConfig({
  base,
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
