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

export interface UserProfile {
  id: number
  email: string
  nickname: string
  role: 'USER' | 'HOSPITAL_ADMIN' | 'SYSTEM_ADMIN'
}
