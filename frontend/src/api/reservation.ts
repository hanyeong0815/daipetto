import { apiClient } from './client'
import type { AdminReservation, ApiResponse, ReservationCreateRequest, ReservationDetail, ReservationStatus, ReservationSummary } from '../types'

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

  // ---- 病院管理者向け（SC-013）
  getAdminList: (params: { hospitalId: number; status?: ReservationStatus; date?: string }) =>
    apiClient.get<ApiResponse<AdminReservation[]>>('/api/v1/admin/reservations', { params }).then((r) => r.data),

  approve: (reservationId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/reservations/${reservationId}/approve`).then((r) => r.data),

  reject: (reservationId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/reservations/${reservationId}/reject`).then((r) => r.data),

  complete: (reservationId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/reservations/${reservationId}/complete`).then((r) => r.data),
}
