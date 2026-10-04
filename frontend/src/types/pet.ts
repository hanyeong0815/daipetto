export type PetSpecies = 'DOG' | 'CAT' | 'OTHER'
export type PetGender = 'MALE' | 'FEMALE'

export interface Pet {
  id: number
  userId: number
  name: string
  species: PetSpecies
  breed?: string
  birthDate?: string
  gender: PetGender
  weight?: number
  neutered: boolean
  microchipNumber?: string
  deletedAt?: string
}

export interface PetCreateRequest {
  name: string
  species: PetSpecies
  breed?: string
  birthDate?: string
  gender: PetGender
  weight?: number
  neutered: boolean
  microchipNumber?: string
}

export interface PetUpdateRequest {
  name?: string
  breed?: string
  birthDate?: string
  gender?: PetGender
  weight?: number
  neutered?: boolean
  microchipNumber?: string
}

// 一覧API（PetSummary）は品種・性別を返さないため、一覧では種別を表示する
export const PET_SPECIES_LABEL: Record<PetSpecies, string> = { DOG: '犬', CAT: '猫', OTHER: 'その他' }
