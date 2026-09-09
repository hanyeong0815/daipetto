import { apiClient } from './client'
import type { ApiResponse, Pet, PetCreateRequest, PetUpdateRequest } from '../types'

// Spring API は petType フィールドを使用するため、送受信時にマッピングする
const toApiBody = ({ species, ...rest }: PetCreateRequest) => ({ ...rest, petType: species })
const fromApiPet = (p: Record<string, unknown>): Pet => ({ ...p, species: p.petType } as Pet)

export const petApi = {
  create: (body: PetCreateRequest) =>
    apiClient.post<ApiResponse<{ petId: number }>>('/api/v1/pets', toApiBody(body)).then((r) => r.data),

  getList: () =>
    apiClient.get<ApiResponse<Record<string, unknown>[]>>('/api/v1/pets').then((r) => ({
      ...r.data,
      data: r.data.data?.map(fromApiPet) ?? [],
    })),

  getDetail: (petId: number) =>
    apiClient.get<ApiResponse<Record<string, unknown>>>(`/api/v1/pets/${petId}`).then((r) => ({
      ...r.data,
      data: r.data.data ? fromApiPet(r.data.data) : null,
    })),

  update: (petId: number, body: PetUpdateRequest) =>
    apiClient.patch<ApiResponse<null>>(`/api/v1/pets/${petId}`, body).then((r) => r.data),

  remove: (petId: number) =>
    apiClient.delete<ApiResponse<null>>(`/api/v1/pets/${petId}`).then((r) => r.data),
}
