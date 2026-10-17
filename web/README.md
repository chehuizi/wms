# WMS 前端管理界面

Vue 3 + TypeScript + Vite + Element Plus 的仓储管理后台。四个模块：仓库、库位、商品、客户。

接口契约以 [`../docs/API_CONTRACT.md`](../docs/API_CONTRACT.md) 为唯一事实来源，字段名与枚举值均按契约实现。

## 环境要求

- Node.js >= 20（当前开发环境 v24.10.0）
- npm >= 10
- 后端服务运行在 `http://localhost:8080`（联调时需要）

## 安装依赖

```bash
cd web
npm install
```

> 国内网络下 `registry.npmjs.org` 可能连不上（表现为长时间无响应）。若卡住，改用镜像源：
>
> ```bash
> npm install --registry=https://registry.npmmirror.com
> ```
>
> 本仓库的依赖就是用该镜像安装的。

## 启动开发服务器

```bash
npm run dev
```

默认监听 `http://localhost:5173`。

### 后端地址与跨域

前端代码里所有请求都走相对路径 `/api/v1/...`，由 Vite dev server 代理到后端，因此**不需要处理跨域**。

代理配置在 `vite.config.ts`：

```ts
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

要改后端地址，改 `target` 一处即可。若后端没有启动，页面会提示「无法连接服务器，请确认后端已在 8080 端口启动」，属于预期行为。

## 构建

```bash
npm run build      # vue-tsc 类型检查 + vite 打包，产物在 dist/
npm run preview    # 本地预览 dist 产物
npm run type-check # 只跑类型检查
```

## 目录结构

```
src/
├── api/          # 按模块拆分的请求方法 + axios 实例
│   ├── request.ts    # 统一拆包 {code, message, data}，错误统一提示
│   ├── warehouse.ts
│   ├── location.ts
│   ├── sku.ts
│   └── customer.ts
├── types/        # 与契约严格对齐的 TS 类型
├── components/   # StatusTag（启用/停用）、FlagTag（是/否）
├── composables/  # useTablePage：分页 / 加载 / 批量删除公共逻辑
├── layouts/      # BasicLayout：左侧菜单 + 顶栏
├── views/        # 四个 CRUD 页面 + 404
├── router.ts
└── main.ts
```

## 实现要点

- **响应包络**：响应拦截器统一处理。`code === 0` 直接返回 `data`；`code !== 0` 弹 `ElMessage.error(message)` 并 reject。
  HTTP 4xx/5xx 时会尝试从响应体里取后端的 `{code, message}` 来提示，而不是笼统的「请求失败」。
  失败对象是 `ApiError`，带 `code` / `httpStatus`，页面可按需分支（例如编码重复 `40901`）。
- **布尔字段**：契约里 `status`、`batchManaged` 等是 `0/1`，前端统一用 `StatusTag` / `FlagTag` 渲染成「启用/停用」「是/否」。
- **库位类型**：枚举在 `src/types/location.ts` 的 `LOCATION_TYPE`，中文标签随契约 §5。
- **商品分类**：下拉数据来自 `GET /skus/categories`，`allow-create` 支持直接录入新分类；保存成功后刷新候选列表。
- **列表容错**：请求失败时表格清空而不是白屏，错误由拦截器统一提示。

## 已知待确认项

1. **批量删除请求体契约缺失**：`POST /*/batch-delete` 在契约里只列了路径，没有定义请求体。
   当前前端按后端 Controller 的实际签名 `@RequestBody List<Long> ids` 发送**裸 id 数组**：

   ```json
   [1, 2, 3]
   ```

   建议把这一条补进 `docs/API_CONTRACT.md`，否则后续改动容易再次跑偏。
2. **`GET /warehouses` 等列表接口当前返回 500**：验证时后端 `/api/v1/health` 返回 `UP`、`database: UP`，
   但列表接口返回 `{"code":50000,"message":"服务端内部错误"}`。属后端侧问题，前端已能正确解析该包络并提示。

