import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { reservationApi } from '../api/reservation'
import { extractErrorMessage } from '../api/client'
import { formatTime } from '../utils/date'
import type { ReservationStatus, ReservationSummary } from '../types'

type Filter = 'all' | 'upcoming' | 'completed' | 'cancelled'

const statusConfig: Record<ReservationStatus, { label: string; color: string }> = {
  REQUESTED: { label: '申請中', color: 'text-warning-yellow bg-warning-yellow/10' },
  APPROVED: { label: '予約確定', color: 'text-primary bg-primary/10' },
  COMPLETED: { label: '診療完了', color: 'text-neutral-gray-600 bg-neutral-gray-50' },
  REJECTED: { label: '却下', color: 'text-error-red bg-error-red/10' },
  CANCELLED: { label: 'キャンセル', color: 'text-error-red bg-error-red/10' },
}

const filterStatuses: Record<Exclude<Filter, 'all'>, ReservationStatus[]> = {
  upcoming: ['REQUESTED', 'APPROVED'],
  completed: ['COMPLETED'],
  cancelled: ['CANCELLED', 'REJECTED'],
}

export default function ReservationHistoryPage() {
  const [filter, setFilter] = useState<Filter>('all')
  const [reservations, setReservations] = useState<ReservationSummary[]>([])
  const [memos, setMemos] = useState<Record<number, string>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const res = await reservationApi.getList()
      setReservations(res.data ?? [])
    } catch (err) {
      setError(extractErrorMessage(err, '予約一覧の取得に失敗しました。'))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleCancel = async (reservationId: number) => {
    if (!window.confirm('この予約をキャンセルしてもよろしいですか？')) return
    setError(null)
    try {
      await reservationApi.cancel(reservationId)
    } catch (err) {
      setError(extractErrorMessage(err, '予約のキャンセルに失敗しました。'))
    }
    // 失敗時も他の操作で状態が変わっている可能性があるため取り直す
    await load()
  }

  const toggleDetail = async (reservationId: number) => {
    if (reservationId in memos) {
      setMemos(({ [reservationId]: _, ...rest }) => rest)
      return
    }
    try {
      const res = await reservationApi.getDetail(reservationId)
      setMemos((prev) => ({ ...prev, [reservationId]: res.data?.memo ?? '' }))
    } catch (err) {
      setError(extractErrorMessage(err, '予約詳細の取得に失敗しました。'))
    }
  }

  const filtered = filter === 'all' ? reservations : reservations.filter((r) => filterStatuses[filter].includes(r.status))

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">予約履歴</h1>

      <div className="flex gap-sm border-b border-neutral-gray-100">
        {(['all', 'upcoming', 'completed', 'cancelled'] as const).map((f) => {
          const labels = { all: 'すべて', upcoming: '予定', completed: '完了', cancelled: 'キャンセル・却下' }
          return (
            <button key={f} onClick={() => setFilter(f)}
              className={`pb-3 font-button-text text-button-text transition-all border-b-2 -mb-px ${filter === f ? 'border-pet-green-vibrant text-primary' : 'border-transparent text-neutral-gray-600 hover:text-on-surface'}`}>
              {labels[f]}
            </button>
          )
        })}
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-xl gap-md text-center mt-xl">
          <div className="w-20 h-20 rounded-full bg-surface-container-low flex items-center justify-center">
            <span className="material-symbols-outlined text-neutral-gray-600 text-[40px]">calendar_today</span>
          </div>
          <div>
            <p className="font-headline-md text-headline-md text-neutral-gray-900 mb-xs">予約履歴がありません</p>
            <p className="font-body-md text-body-md text-neutral-gray-600">近くの病院を探して予約しましょう。</p>
          </div>
          <Link to="/hospitals" className="flex items-center gap-2 bg-pet-green-vibrant text-white font-button-text text-button-text px-6 py-3 rounded-xl hover:bg-primary transition-colors">
            <span className="material-symbols-outlined">local_hospital</span>
            病院を探す
          </Link>
        </div>
      ) : (
        <div className="flex flex-col gap-sm">
          {filtered.map((r) => {
            const config = statusConfig[r.status]
            const isOpen = r.reservationId in memos
            return (
              <div key={r.reservationId} className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-sm shadow-[0px_4px_20px_rgba(0,0,0,0.05)]">
                <div className="flex items-start justify-between gap-2">
                  <h2 className="font-headline-md text-headline-md text-neutral-gray-900">{r.hospitalName}</h2>
                  <span className={`font-label-md text-label-md px-2.5 py-1 rounded-full flex-shrink-0 ${config.color}`}>{config.label}</span>
                </div>
                <div className="flex items-center gap-4 text-neutral-gray-600 flex-wrap">
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">calendar_today</span>
                    <span className="font-body-md text-body-md">{r.availableDate}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">schedule</span>
                    <span className="font-body-md text-body-md">{formatTime(r.startTime)}〜{formatTime(r.endTime)}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">pets</span>
                    <span className="font-body-md text-body-md">{r.petName}</span>
                  </div>
                </div>
                {isOpen && (
                  <p className="font-body-md text-body-md text-neutral-gray-900 whitespace-pre-wrap bg-neutral-gray-50 rounded-lg px-sm py-sm">
                    {memos[r.reservationId] || 'メモはありません。'}
                  </p>
                )}
                <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                  <button onClick={() => toggleDetail(r.reservationId)}
                    className="flex-1 h-[40px] bg-surface-container-low text-on-surface font-button-text text-button-text rounded-lg border border-neutral-gray-100 hover:bg-surface-container transition-colors">
                    {isOpen ? '詳細を閉じる' : '詳細'}
                  </button>
                  {(r.status === 'REQUESTED' || r.status === 'APPROVED') && (
                    <button onClick={() => handleCancel(r.reservationId)}
                      className="flex-1 h-[40px] bg-error-red/10 text-error-red font-button-text text-button-text rounded-lg hover:bg-error-red/20 transition-colors">
                      予約キャンセル
                    </button>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
