import { apiClient } from './client'
import type { ApiResponse, HealthRecord, HealthRecordRequest } from '../types'

export const healthRecordApi = {
  getList: (petId: number) =>
    apiClient.get<ApiResponse<HealthRecord[]>>(`/api/v1/pets/${petId}/health-records`).then((r) => r.data),

  create: (petId: number, body: HealthRecordRequest) =>
    apiClient
      .post<ApiResponse<{ healthRecordId: number }>>(`/api/v1/pets/${petId}/health-records`, body)
      .then((r) => r.data),

  update: (healthRecordId: number, body: HealthRecordRequest) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/health-records/${healthRecordId}`, body).then((r) => r.data),

  remove: (healthRecordId: number) =>
    apiClient.delete<ApiResponse<null>>(`/api/v1/health-records/${healthRecordId}`).then((r) => r.data),
}
