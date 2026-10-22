import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { UserProfile } from '../types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserProfile | null
  isAuthenticated: boolean
  // ログインセッションの世代。clearAuthのたびに増える（永続化しない）。
  // ユーザー固有のキャッシュはこれが変わったら破棄し、古い世代で始まった応答は反映しない
  session: number
  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: UserProfile) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      session: 0,

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken, isAuthenticated: true }),

      setUser: (user) => set({ user }),

      clearAuth: () =>
        set((state) => ({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false, session: state.session + 1 })),
    }),
    {
      name: 'daipetto-auth',
      // アクセストークンはメモリのみ、リフレッシュトークンのみ永続化
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    },
  ),
)
