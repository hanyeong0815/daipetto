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
  userId: number
  email: string
  nickname: string
  role: 'ROLE_USER' | 'ROLE_HOSPITAL_ADMIN' | 'ROLE_SYSTEM_ADMIN'
  status: 'ACTIVE' | 'SUSPENDED'
}

// 管理者向けユーザー一覧（GET /api/v1/admin/users）の1件
export interface AdminUser {
  userId: number
  email: string
  nickname: string
  role: UserProfile['role']
  status: UserProfile['status']
  createdAt: string
}
