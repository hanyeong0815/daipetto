export type VaccinationStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED'

export interface Vaccination {
  vaccinationId: number
  vaccineName: string
  scheduledDate: string
  completedDate?: string
  status: VaccinationStatus
}

export interface VaccinationCreateRequest {
  vaccineName: string
  scheduledDate: string
}
