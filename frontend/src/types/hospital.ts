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
