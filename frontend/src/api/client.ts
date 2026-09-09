import axios, { type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { Capacitor } from '@capacitor/core'
import { useAuthStore } from '../stores/authStore'

// 実行環境（Web / Androidエミュレーター / iOSシミュレーター）ごとにAPIのURLが異なるため、
// Capacitorの実行時プラットフォーム判定で自動切り替えする（.env手動書き換えは不要）
function resolveBaseUrl(): string {
  const platform = Capacitor.getPlatform()

  if (platform === 'android') {
    return import.meta.env.VITE_API_BASE_URL_ANDROID ?? 'http://10.0.2.2:8080'
  }
  if (platform === 'ios') {
    return import.meta.env.VITE_API_BASE_URL_IOS ?? 'http://localhost:8080'
  }
  return import.meta.env.VITE_API_BASE_URL_WEB ?? 'http://localhost:8080'
}

export const BASE_URL = resolveBaseUrl()

export const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
  // JWT認証のためwithCredentialsは不要
  withCredentials: false,
})

// リクエストインターセプター: アクセストークンをヘッダーに付与
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// レスポンスインターセプター: 401時にトークンリフレッシュ
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    const { refreshToken, setTokens, clearAuth } = useAuthStore.getState()
    if (!refreshToken) {
      clearAuth()
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise<string>((resolve) => {
        pendingRequests.push(resolve)
      }).then((newToken) => {
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`
        }
        return apiClient(originalRequest)
      })
    }

    originalRequest._retry = true
    isRefreshing = true

    try {
      const { data } = await axios.post<{ success: boolean; data: { accessToken: string; refreshToken: string } }>(
        `${BASE_URL}/api/v1/auth/refresh`,
        { refreshToken },
      )
      const { accessToken, refreshToken: newRefreshToken } = data.data
      setTokens(accessToken, newRefreshToken)

      pendingRequests.forEach((resolve) => resolve(accessToken))
      pendingRequests = []

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
      }
      return apiClient(originalRequest)
    } catch {
      clearAuth()
      pendingRequests = []
      return Promise.reject(error)
    } finally {
      isRefreshing = false
    }
  },
)
