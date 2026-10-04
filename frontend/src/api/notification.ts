import { apiClient } from './client'
import type { ApiResponse, AppNotification } from '../types'

export const notificationApi = {
  getList: () => apiClient.get<ApiResponse<AppNotification[]>>('/api/v1/notifications').then((r) => r.data),

  markAsRead: (notificationId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/notifications/${notificationId}/read`).then((r) => r.data),
}
