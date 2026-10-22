import { apiClient } from './client'
import type { AdminUser, ApiResponse, PageResponse, UserProfile } from '../types'

export interface AdminUserSearch {
  keyword?: string
  role?: UserProfile['role']
  status?: UserProfile['status']
  page: number
  size: number
}

export const adminUserApi = {
  search: (params: AdminUserSearch) =>
    apiClient.get<ApiResponse<PageResponse<AdminUser>>>('/api/v1/admin/users', { params }).then((r) => r.data),

  suspend: (userId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/users/${userId}/suspend`).then((r) => r.data),

  activate: (userId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/users/${userId}/activate`).then((r) => r.data),

  changeRole: (userId: number, role: UserProfile['role']) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/users/${userId}/role`, { role }).then((r) => r.data),
}
