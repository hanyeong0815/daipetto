import { apiClient } from './client'
import type { ApiResponse, AuthTokens, LoginRequest, RegisterRequest, UserProfile } from '../types'

export const authApi = {
  login: (body: LoginRequest) =>
    apiClient.post<ApiResponse<AuthTokens>>('/api/v1/auth/login', body).then((r) => r.data),

  register: (body: RegisterRequest) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/register', body).then((r) => r.data),

  logout: (refreshToken: string) =>
    apiClient.post<ApiResponse<null>>('/api/v1/auth/logout', { refreshToken }).then((r) => r.data),

  getMe: () =>
    apiClient.get<ApiResponse<UserProfile>>('/api/v1/users/me').then((r) => r.data),
}
