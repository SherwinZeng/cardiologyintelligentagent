import axios from 'axios'
import type { AxiosInstance, AxiosResponse, CreateAxiosDefaults, InternalAxiosRequestConfig } from 'axios'

const httpAxios: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_AUTH_API_BASE_URL,
  timeout: 5000,
} as CreateAxiosDefaults)

httpAxios.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => config,
  (error: unknown) => Promise.reject(error),
)

httpAxios.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error: unknown) => Promise.reject(error),
)

export default httpAxios

export function getHttpErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    return (
      (error.response?.data as { message?: string } | undefined)?.message ??
      error.message ??
      '网络异常，请稍后重试'
    )
  }
  if (error instanceof Error) {
    return error.message
  }
  return '网络异常，请稍后重试'
}
