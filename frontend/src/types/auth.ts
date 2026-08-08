export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  nickname: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

// バックエンドのRole enum（Role.java）はROLE_プレフィックス付きのname()をそのまま返す
export interface UserProfile {
  id: number
  email: string
  nickname: string
  role: 'ROLE_USER' | 'ROLE_HOSPITAL_ADMIN' | 'ROLE_SYSTEM_ADMIN'
}
