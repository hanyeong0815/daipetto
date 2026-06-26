import { apiClient } from './client'
import type { ApiResponse, Pet, PetCreateRequest, PetUpdateRequest } from '../types'

export const petApi = {
  create: (body: PetCreateRequest) =>
    apiClient.post<ApiResponse<Pet>>('/api/v1/pets', body).then((r) => r.data),

  getList: () =>
    apiClient.get<ApiResponse<Pet[]>>('/api/v1/pets').then((r) => r.data),

  getDetail: (petId: number) =>
    apiClient.get<ApiResponse<Pet>>(`/api/v1/pets/${petId}`).then((r) => r.data),

  update: (petId: number, body: PetUpdateRequest) =>
    apiClient.patch<ApiResponse<Pet>>(`/api/v1/pets/${petId}`, body).then((r) => r.data),

  remove: (petId: number) =>
    apiClient.delete<ApiResponse<null>>(`/api/v1/pets/${petId}`).then((r) => r.data),
}
