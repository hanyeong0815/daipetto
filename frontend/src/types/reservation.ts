import type { PetSpecies } from './pet'

export type ReservationStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | 'CANCELLED'

export interface ReservationSummary {
  reservationId: number
  hospitalName: string
  petName: string
  availableDate: string
  startTime: string
  endTime: string
  status: ReservationStatus
}

export interface ReservationDetail extends ReservationSummary {
  memo?: string
}

export interface ReservationCreateRequest {
  petId: number
  hospitalId: number
  scheduleId: number
  memo?: string
}

// 病院管理者向け予約一覧（GET /api/v1/admin/reservations、SC-013）の1件
export interface AdminReservation {
  reservationId: number
  status: ReservationStatus
  availableDate: string
  startTime: string
  endTime: string
  userId: number
  userNickname: string
  petName: string
  petType: PetSpecies
  memo?: string
  createdAt: string
}
