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

export const useHospitalStore = create<HospitalState>()((set) => ({
  hospitals: [],
  selectedHospital: null,
  schedules: [],
  businessHours: [],
  isLoading: false,
  error: null,

  fetchHospitals: async (keyword, area) => {
    set({ isLoading: true, error: null })
    try {
      const res = await hospitalApi.getList(keyword, area)
      if (res.success) {
        set({ hospitals: res.data ?? [] })
      }
    } catch {
      set({ error: '病院一覧の取得に失敗しました。' })
    } finally {
      set({ isLoading: false })
    }
  },

  fetchHospitalDetail: async (hospitalId) => {
    set({ isLoading: true, error: null })
    try {
      const res = await hospitalApi.getDetail(hospitalId)
      if (res.success) {
        set({ selectedHospital: res.data })
      }
    } catch {
      set({ error: '病院情報の取得に失敗しました。' })
    } finally {
      set({ isLoading: false })
    }
  },

  fetchSchedules: async (hospitalId) => {
    try {
      const res = await hospitalApi.getScheduleList(hospitalId)
      if (res.success) {
        set({ schedules: res.data ?? [] })
      }
    } catch {
      set({ error: '予約枠の取得に失敗しました。' })
    }
  },

  fetchBusinessHours: async (hospitalId) => {
    try {
      const res = await hospitalApi.getBusinessHoursList(hospitalId)
      if (res.success) {
        set({ businessHours: res.data ?? [] })
      }
    } catch {
      set({ error: '営業時間の取得に失敗しました。' })
    }
  },
}))
