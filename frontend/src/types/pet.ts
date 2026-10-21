export type PetSpecies = 'DOG' | 'CAT' | 'OTHER'
export type PetGender = 'MALE' | 'FEMALE'

export interface Pet {
  id: number
  userId: number
  name: string
  species: PetSpecies
  breed?: string
  birthDate?: string
  gender?: PetGender
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
  gender?: PetGender
  weight?: number
  neutered: boolean
  microchipNumber?: string
}

// 更新は全項目置換（docs/07 §5-4）。省略した任意項目はNULL、neuteredはfalseになるため、常にフォーム全体を送る
export type PetUpdateRequest = PetCreateRequest

// 一覧API（PetSummary）は性別を返さないため、一覧では種別と品種を表示する
export const PET_SPECIES_LABEL: Record<PetSpecies, string> = { DOG: '犬', CAT: '猫', OTHER: 'その他' }
