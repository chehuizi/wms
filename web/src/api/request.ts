import axios, { AxiosError, type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiEnvelope } from '@/types/common'

/**
 * 后端错误码，契约 §9。用于在提示之外保留可分支的 code。
 */
export const ERROR_CODE = {
  PARAM_INVALID: 40001,
  EMAIL_INVALID: 40002,
  RELATED_INVALID: 40003,
  NOT_FOUND: 40401,
  DUPLICATED: 40901,
  HAS_RELATION: 40902,
  SERVER_ERROR: 50000,
  /** 以下为契约 §19 补充的业务错误码 */
  /** 状态流转不合法（如对已取消的单据收货） */
  STATUS_INVALID: 40004,
  /** 盘点明细未录全就结束 */
  STOCKTAKE_INCOMPLETE: 40005,
  /** 超收或库存不足，message 会写清缺多少 */
  QTY_NOT_ENOUGH: 40903,
  /** 库位不允许负库存 */
  NEGATIVE_NOT_ALLOWED: 40904,
  /** 单据状态不允许该操作（如已发货订单不能取消） */
  ACTION_NOT_ALLOWED: 40905,
  /** 序列号重复或状态不允许 */
  SERIAL_INVALID: 40906,
} as const

/** 携带后端包络信息的错误对象，供页面按需分支（如 409 编码重复）。 */
export class ApiError extends Error {
  code: number
  httpStatus: number
  data: unknown

  constructor(message: string, code: number, httpStatus: number, data: unknown = null) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.httpStatus = httpStatus
    this.data = data
  }
}

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

/** 从错误响应里尽力解析出后端的 {code, message} 包络。 */
function extractEnvelope(error: AxiosError): { code: number; message: string; data: unknown } | null {
  const body = error.response?.data
  if (body && typeof body === 'object' && 'code' in body && 'message' in body) {
    const envelope = body as ApiEnvelope<unknown>
    return {
      code: typeof envelope.code === 'number' ? envelope.code : ERROR_CODE.SERVER_ERROR,
      message: String(envelope.message ?? ''),
      data: envelope.data ?? null,
    }
  }
  return null
}

service.interceptors.response.use(
  (response: AxiosResponse) => {
    // 拦截器统一拆包，调用方拿到的直接是 data。
    // axios 的类型签名固定为 AxiosResponse，这里用断言收敛，调用侧无需感知。
    // /health 不走包络，直接返回 {status, database}
    const body = response.data
    if (!body || typeof body !== 'object' || !('code' in body)) {
      return body as AxiosResponse
    }
    const envelope = body as ApiEnvelope<unknown>
    if (envelope.code === 0) {
      return envelope.data as AxiosResponse
    }
    const message = envelope.message || '操作失败'
    ElMessage.error(message)
    return Promise.reject(new ApiError(message, envelope.code, response.status, envelope.data))
  },
  (error: AxiosError) => {
    const envelope = extractEnvelope(error)
    if (envelope) {
      // 后端按语义给了 4xx/5xx，同时 body 里带包络 —— 优先用后端的 message
      ElMessage.error(envelope.message || '操作失败')
      return Promise.reject(
        new ApiError(envelope.message || '操作失败', envelope.code, error.response?.status ?? 0, envelope.data),
      )
    }

    let message = '网络异常，请稍后重试'
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      message = '请求超时，请检查后端服务是否已启动'
    } else if (!error.response) {
      message = '无法连接服务器，请确认后端已在 8080 端口启动'
    } else {
      const status = error.response.status
      if (status === 404) message = '接口不存在（404），请确认后端接口路径'
      else if (status >= 500) message = `服务端异常（${status}）`
      else message = `请求失败（${status}）`
    }
    ElMessage.error(message)
    return Promise.reject(new ApiError(message, -1, error.response?.status ?? 0))
  },
)

export default service
