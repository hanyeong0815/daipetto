import { create } from 'zustand'
import type { Pet } from '../types'
import { petApi } from '../api/pet'

interface PetState {
  pets: Pet[]
  selectedPet: Pet | null
  isLoading: boolean
  error: string | null
  fetchPets: () => Promise<void>
  fetchPetDetail: (petId: number) => Promise<void>
  setSelectedPet: (pet: Pet | null) => void
  removePet: (petId: number) => Promise<void>
}

export const usePetStore = create<PetState>()((set, get) => ({
  pets: [],
  selectedPet: null,
  isLoading: false,
  error: null,

  fetchPets: async () => {
    set({ isLoading: true, error: null })
    try {
      const res = await petApi.getList()
      if (res.success) {
        set({ pets: res.data ?? [] })
      }
    } catch {
      set({ error: 'ペット一覧の取得に失敗しました。' })
    } finally {
      set({ isLoading: false })
    }
  },

  fetchPetDetail: async (petId) => {
    set({ isLoading: true, error: null })
    try {
      const res = await petApi.getDetail(petId)
      if (res.success) {
        set({ selectedPet: res.data })
      }
    } catch {
      set({ error: 'ペット情報の取得に失敗しました。' })
    } finally {
      set({ isLoading: false })
    }
  },

  setSelectedPet: (pet) => set({ selectedPet: pet }),

  removePet: async (petId) => {
    try {
      const res = await petApi.remove(petId)
      if (res.success) {
        set({ pets: get().pets.filter((p) => p.id !== petId) })
      }
    } catch {
      set({ error: 'ペットの削除に失敗しました。' })
    }
  },
}))
