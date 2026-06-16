import axios from 'axios'
import type {AxiosError, AxiosInstance, AxiosResponse, CreateAxiosDefaults, InternalAxiosRequestConfig} from 'axios'
import {useLocalStorage} from '@/hooks/useStorage'
import type {IBaseResponse} from '@/typings/baseResponse.ts'

const httpAxios: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_AUTH_API_BASE_URL,
    timeout: 5000,
} as CreateAxiosDefaults)

export function getApiErrorMessage(error: unknown): string {
    if (isBaseResponse(error)) {
        return error.message || '请求失败，请稍后重试'
    }
    if (axios.isAxiosError(error)) {
        const data = error.response?.data
        if (isBaseResponse(data)) {
            return data.message
        }
        return error.message || '网络异常，请稍后重试'
    }
    if (error instanceof Error) {
        return error.message
    }
    return '请求失败，请稍后重试'
}

function isBaseResponse(value: unknown): value is IBaseResponse<unknown> {
    return (
        typeof value === 'object' &&
        value !== null &&
        'code' in value &&
        'message' in value
    )
}

httpAxios.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const {getItem} = useLocalStorage()
        config.headers['Content-Type'] = 'application/json;charset=UTF-8'

        const raw = getItem('user_login')
        if (raw) {
            try {
                const login = JSON.parse(raw) as { token?: string }
                if (login?.token) {
                    config.headers.Authorization = `Bearer ${login.token}`
                }
            } catch {
            }
        }
        return config
    },
    (error: unknown) => Promise.reject(error),
)

httpAxios.interceptors.response.use(
    (response: AxiosResponse) => {
        const data = response.data as IBaseResponse<unknown>
        if (isBaseResponse(data) && data.code !== 200) {
            return Promise.reject(data)
        }
        return response.data
    },
    (error: unknown) => {
        if (axios.isAxiosError(error)) {
            const axiosError = error as AxiosError<IBaseResponse<unknown>>
            const body = axiosError.response?.data
            if (isBaseResponse(body)) {
                return Promise.reject(body)
            }
        }
        return Promise.reject({
            code: axios.isAxiosError(error) ? (error.response?.status ?? 500) : 500,
            message: getApiErrorMessage(error),
            data: null,
        } satisfies IBaseResponse<null>)
    },
)

export default httpAxios
