import { apiClient } from './client'
import type { ApiResponse, ReservationCreateRequest, ReservationDetail, ReservationStatus, ReservationSummary } from '../types'

export const reservationApi = {
  create: (body: ReservationCreateRequest) =>
    apiClient
      .post<ApiResponse<{ reservationId: number; status: ReservationStatus }>>('/api/v1/reservations', body)
      .then((r) => r.data),

  getList: () => apiClient.get<ApiResponse<ReservationSummary[]>>('/api/v1/reservations').then((r) => r.data),

  getDetail: (reservationId: number) =>
    apiClient.get<ApiResponse<ReservationDetail>>(`/api/v1/reservations/${reservationId}`).then((r) => r.data),

  cancel: (reservationId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/reservations/${reservationId}/cancel`).then((r) => r.data),
}
