export interface ApiResponse<T = null> {
  success: boolean
  data: T
  code?: string
  message?: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
