import { create } from 'zustand'
import type { Pet } from '../types'
import { petApi } from '../api/pet'
import { useAuthStore } from './authStore'

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

const initialState = { pets: [], selectedPet: null, isLoading: false, error: null }

// 応答を反映してよいのは、要求したときと同じログインセッションの間だけ。
// ログアウト・別アカウントでのログイン後に前のユーザーの応答が届いても捨てる（REVIEW-001 F-01）
const currentSession = () => useAuthStore.getState().session

export const usePetStore = create<PetState>()((set, get) => ({
  ...initialState,

  fetchPets: async () => {
    const session = currentSession()
    set({ isLoading: true, error: null })
    try {
      const res = await petApi.getList()
      if (session === currentSession() && res.success) {
        set({ pets: res.data ?? [] })
      }
    } catch {
      if (session === currentSession()) set({ error: 'ペット一覧の取得に失敗しました。' })
    } finally {
      if (session === currentSession()) set({ isLoading: false })
    }
  },

  fetchPetDetail: async (petId) => {
    const session = currentSession()
    set({ isLoading: true, error: null })
    try {
      const res = await petApi.getDetail(petId)
      if (session === currentSession() && res.success) {
        set({ selectedPet: res.data })
      }
    } catch {
      if (session === currentSession()) set({ error: 'ペット情報の取得に失敗しました。' })
    } finally {
      if (session === currentSession()) set({ isLoading: false })
    }
  },

  setSelectedPet: (pet) => set({ selectedPet: pet }),

  removePet: async (petId) => {
    const session = currentSession()
    try {
      const res = await petApi.remove(petId)
      if (session === currentSession() && res.success) {
        set({ pets: get().pets.filter((p) => p.id !== petId) })
      }
    } catch {
      if (session === currentSession()) set({ error: 'ペットの削除に失敗しました。' })
    }
  },
}))

// セッションが変わったら前のユーザーのペット情報（一覧・選択中）を破棄する
useAuthStore.subscribe((state, prev) => {
  if (state.session !== prev.session) usePetStore.setState(initialState)
})
