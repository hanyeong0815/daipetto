import { create } from 'zustand'
import type { HospitalBusinessHours, HospitalDetail, HospitalSchedule, HospitalSummary } from '../types'
import { hospitalApi } from '../api/hospital'

interface HospitalState {
  hospitals: HospitalSummary[]
  selectedHospital: HospitalDetail | null
  schedules: HospitalSchedule[]
  businessHours: HospitalBusinessHours[]
  isLoading: boolean
  error: string | null
  fetchHospitals: (keyword?: string, area?: string) => Promise<void>
  fetchHospitalDetail: (hospitalId: number) => Promise<void>
  fetchSchedules: (hospitalId: number) => Promise<void>
  fetchBusinessHours: (hospitalId: number) => Promise<void>
}

// 病院ごとのデータは、最後に要求した病院の応答だけを反映する。別の病院へ切り替えたときは
// 前の病院のデータをすぐ消し、遅れて届いた前の病院の応答でも上書きしない（REVIEW-001 F-02）
const latest = { hospitals: 0, detail: 0, schedules: 0, businessHours: 0 }
const loadedFor = { detail: 0, schedules: 0, businessHours: 0 }

export const useHospitalStore = create<HospitalState>()((set) => ({
  hospitals: [],
  selectedHospital: null,
  schedules: [],
  businessHours: [],
  isLoading: false,
  error: null,

  fetchHospitals: async (keyword, area) => {
    const request = ++latest.hospitals
    set({ isLoading: true, error: null })
    try {
      const res = await hospitalApi.getList(keyword, area)
      if (request === latest.hospitals && res.success) {
        set({ hospitals: res.data ?? [] })
      }
    } catch {
      if (request === latest.hospitals) set({ error: '病院一覧の取得に失敗しました。' })
    } finally {
      if (request === latest.hospitals) set({ isLoading: false })
    }
  },

  fetchHospitalDetail: async (hospitalId) => {
    const request = ++latest.detail
    if (loadedFor.detail !== hospitalId) {
      loadedFor.detail = hospitalId
      set({ selectedHospital: null })
    }
    set({ isLoading: true, error: null })
    try {
      const res = await hospitalApi.getDetail(hospitalId)
      if (request === latest.detail && res.success) {
        set({ selectedHospital: res.data })
      }
    } catch {
      if (request === latest.detail) set({ error: '病院情報の取得に失敗しました。' })
    } finally {
      if (request === latest.detail) set({ isLoading: false })
    }
  },

  fetchSchedules: async (hospitalId) => {
    const request = ++latest.schedules
    if (loadedFor.schedules !== hospitalId) {
      loadedFor.schedules = hospitalId
      set({ schedules: [] })
    }
    try {
      const res = await hospitalApi.getScheduleList(hospitalId)
      if (request === latest.schedules && res.success) {
        set({ schedules: res.data ?? [] })
      }
    } catch {
      if (request === latest.schedules) set({ error: '予約枠の取得に失敗しました。' })
    }
  },

  fetchBusinessHours: async (hospitalId) => {
    const request = ++latest.businessHours
    if (loadedFor.businessHours !== hospitalId) {
      loadedFor.businessHours = hospitalId
      set({ businessHours: [] })
    }
    try {
      const res = await hospitalApi.getBusinessHoursList(hospitalId)
      if (request === latest.businessHours && res.success) {
        set({ businessHours: res.data ?? [] })
      }
    } catch {
      if (request === latest.businessHours) set({ error: '営業時間の取得に失敗しました。' })
    }
  },
}))
