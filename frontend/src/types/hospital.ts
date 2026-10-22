export type HospitalStatus = 'ACTIVE' | 'SUSPENDED'
export type HospitalScheduleStatus = 'AVAILABLE' | 'BLOCKED'
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface HospitalSummary {
  id: number
  name: string
  address: string
  phoneNumber?: string
}

export interface HospitalDetail {
  id: number
  name: string
  address: string
  phoneNumber?: string
  status: HospitalStatus
  createdAt: string
}

export interface HospitalCreateRequest {
  name: string
  address: string
  phoneNumber?: string
}

export interface HospitalUpdateRequest {
  name: string
  address: string
  phoneNumber?: string
}

export interface HospitalSchedule {
  scheduleId: number
  availableDate: string
  startTime: string
  endTime: string
  status: HospitalScheduleStatus
}

export interface HospitalScheduleCreateRequest {
  availableDate: string
  startTime: string
  endTime: string
}

export interface HospitalBusinessHours {
  businessHoursId: number
  dayOfWeek: DayOfWeek
  openTime: string
  closeTime: string
  breakStartTime?: string
  breakEndTime?: string
  slotDurationMinutes: number
}

export interface HospitalBusinessHoursCreateRequest {
  dayOfWeek: DayOfWeek
  openTime: string
  closeTime: string
  breakStartTime?: string
  breakEndTime?: string
  slotDurationMinutes: number
}

export interface HospitalBusinessHoursUpdateRequest {
  openTime: string
  closeTime: string
  breakStartTime?: string
  breakEndTime?: string
  slotDurationMinutes: number
}

// 管理画面で操作できる病院（GET /api/v1/admin/hospitals/mine、停止中も含む）
export interface ManagedHospital {
  hospitalId: number
  name: string
  address: string
  phoneNumber?: string
  status: HospitalStatus
}

// 病院に紐付いた病院管理者（GET /api/v1/admin/hospitals/{id}/admins）
export interface HospitalAdminUser {
  userId: number
  email: string
  nickname: string
}

// 予約枠の自動生成結果（POST /api/v1/admin/hospitals/{id}/schedules/generate）
export interface ScheduleGenerationResult {
  createdSlots: number
  generatedDates: number
  skippedDates: number
}
