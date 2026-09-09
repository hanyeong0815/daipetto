import { apiClient } from './client'
import type {
  ApiResponse,
  HospitalBusinessHours,
  HospitalBusinessHoursCreateRequest,
  HospitalBusinessHoursUpdateRequest,
  HospitalCreateRequest,
  HospitalDetail,
  HospitalSchedule,
  HospitalScheduleCreateRequest,
  HospitalSummary,
  HospitalUpdateRequest,
} from '../types'

export const hospitalApi = {
  getList: (keyword?: string, area?: string) =>
    apiClient
      .get<ApiResponse<HospitalSummary[]>>('/api/v1/hospitals', { params: { keyword, area } })
      .then((r) => r.data),

  getDetail: (hospitalId: number) =>
    apiClient.get<ApiResponse<HospitalDetail>>(`/api/v1/hospitals/${hospitalId}`).then((r) => r.data),

  getScheduleList: (hospitalId: number) =>
    apiClient.get<ApiResponse<HospitalSchedule[]>>(`/api/v1/hospitals/${hospitalId}/schedules`).then((r) => r.data),

  getBusinessHoursList: (hospitalId: number) =>
    apiClient
      .get<ApiResponse<HospitalBusinessHours[]>>(`/api/v1/hospitals/${hospitalId}/business-hours`)
      .then((r) => r.data),

  create: (body: HospitalCreateRequest) =>
    apiClient.post<ApiResponse<{ hospitalId: number }>>('/api/v1/admin/hospitals', body).then((r) => r.data),

  update: (hospitalId: number, body: HospitalUpdateRequest) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/hospitals/${hospitalId}`, body).then((r) => r.data),

  suspend: (hospitalId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/admin/hospitals/${hospitalId}/suspend`).then((r) => r.data),

  createSchedule: (hospitalId: number, body: HospitalScheduleCreateRequest) =>
    apiClient
      .post<ApiResponse<{ scheduleId: number }>>(`/api/v1/admin/hospitals/${hospitalId}/schedules`, body)
      .then((r) => r.data),

  blockSchedule: (hospitalId: number, scheduleId: number) =>
    apiClient
      .patch<ApiResponse<null>>(`/api/v1/admin/hospitals/${hospitalId}/schedules/${scheduleId}/block`)
      .then((r) => r.data),

  unblockSchedule: (hospitalId: number, scheduleId: number) =>
    apiClient
      .patch<ApiResponse<null>>(`/api/v1/admin/hospitals/${hospitalId}/schedules/${scheduleId}/unblock`)
      .then((r) => r.data),

  createBusinessHours: (hospitalId: number, body: HospitalBusinessHoursCreateRequest) =>
    apiClient
      .post<ApiResponse<{ businessHoursId: number }>>(`/api/v1/admin/hospitals/${hospitalId}/business-hours`, body)
      .then((r) => r.data),

  updateBusinessHours: (hospitalId: number, businessHoursId: number, body: HospitalBusinessHoursUpdateRequest) =>
    apiClient
      .patch<ApiResponse<null>>(`/api/v1/admin/hospitals/${hospitalId}/business-hours/${businessHoursId}`, body)
      .then((r) => r.data),

  deleteBusinessHours: (hospitalId: number, businessHoursId: number) =>
    apiClient
      .delete<ApiResponse<null>>(`/api/v1/admin/hospitals/${hospitalId}/business-hours/${businessHoursId}`)
      .then((r) => r.data),
}
