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
