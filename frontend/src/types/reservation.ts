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
