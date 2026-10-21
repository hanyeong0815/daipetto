import { apiClient } from './client'
import type { ApiResponse, Vaccination, VaccinationCreateRequest } from '../types'

export const vaccinationApi = {
  getList: (petId: number) =>
    apiClient.get<ApiResponse<Vaccination[]>>(`/api/v1/pets/${petId}/vaccinations`).then((r) => r.data),

  create: (petId: number, body: VaccinationCreateRequest) =>
    apiClient
      .post<ApiResponse<{ vaccinationId: number }>>(`/api/v1/pets/${petId}/vaccinations`, body)
      .then((r) => r.data),

  complete: (vaccinationId: number, completedDate: string) =>
    apiClient
      .patch<ApiResponse<null>>(`/api/v1/vaccinations/${vaccinationId}/complete`, { completedDate })
      .then((r) => r.data),

  cancel: (vaccinationId: number) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/vaccinations/${vaccinationId}/cancel`).then((r) => r.data),

  reschedule: (vaccinationId: number, scheduledDate: string) =>
    apiClient
      .patch<ApiResponse<null>>(`/api/v1/vaccinations/${vaccinationId}/reschedule`, { scheduledDate })
      .then((r) => r.data),

  remove: (vaccinationId: number) =>
    apiClient.delete<ApiResponse<null>>(`/api/v1/vaccinations/${vaccinationId}`).then((r) => r.data),
}
