import { useEffect, useState } from 'react'
import { hospitalApi } from '../../api/hospital'
import { reservationApi } from '../../api/reservation'
import { extractErrorMessage } from '../../api/client'
import { formatTime } from '../../utils/date'
import { PET_SPECIES_LABEL } from '../../types'
import type { AdminReservation, ManagedHospital, ReservationStatus } from '../../types'

const statusConfig: Record<ReservationStatus, { label: string; color: string }> = {
  REQUESTED: { label: '承認待ち', color: 'text-warning-yellow bg-warning-yellow/10' },
  APPROVED: { label: '予約確定', color: 'text-primary bg-primary/10' },
  COMPLETED: { label: '診療完了', color: 'text-neutral-gray-600 bg-neutral-gray-50' },
  REJECTED: { label: '却下', color: 'text-error-red bg-error-red/10' },
  CANCELLED: { label: 'キャンセル', color: 'text-error-red bg-error-red/10' },
}

const FILTERS = ['all', 'REQUESTED', 'APPROVED', 'COMPLETED', 'REJECTED', 'CANCELLED'] as const

// SC-013 予約管理（病院管理者・システム管理者）。管理対象の病院の予約を確認し、承認・却下・診療完了にする
export default function AdminReservationPage() {
  const [hospitals, setHospitals] = useState<ManagedHospital[] | null>(null)
  const [hospitalId, setHospitalId] = useState<number | null>(null)
  const [hospitalsFailed, setHospitalsFailed] = useState(false)
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>('all')
  const [date, setDate] = useState('')
  const [reservations, setReservations] = useState<AdminReservation[] | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [loadFailed, setLoadFailed] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [hospitalsKey, setHospitalsKey] = useState(0)

  useEffect(() => {
    hospitalApi
      .getManagedHospitals()
      .then((res) => {
        const list = res.data ?? []
        setHospitals(list)
        setHospitalId(list[0]?.hospitalId ?? null)
        setHospitalsFailed(false)
      })
      .catch((err) => {
        setError(extractErrorMessage(err, '管理対象の病院の取得に失敗しました。'))
        setHospitalsFailed(true)
      })
  }, [hospitalsKey])

  useEffect(() => {
    if (hospitalId == null) return
    // 病院・日付を変えた後に前の条件の応答が遅れて届いても反映しない
    let active = true
    setIsLoading(true)
    // 状態の絞り込みは件数表示のため画面側で行い、APIには病院と日付だけを渡す
    reservationApi
      .getAdminList({ hospitalId, date: date || undefined })
      .then((res) => {
        if (!active) return
        setReservations(res.data ?? [])
        setLoadFailed(false)
      })
      .catch((err) => {
        if (!active) return
        setError(extractErrorMessage(err, '予約一覧の取得に失敗しました。'))
        setLoadFailed(true)
      })
      .finally(() => active && setIsLoading(false))
    return () => {
      active = false
    }
  }, [hospitalId, date, reloadKey])

  const reload = () => {
    setError(null)
    setReloadKey((k) => k + 1)
  }

  const run = async (r: AdminReservation, message: string, action: () => Promise<unknown>, fallback: string) => {
    if (!window.confirm(`${r.availableDate} ${formatTime(r.startTime)} ${r.userNickname}様（${r.petName}）の予約を${message}`)) return
    setError(null)
    setBusyId(r.reservationId)
    try {
      await action()
    } catch (err) {
      setError(extractErrorMessage(err, fallback))
    } finally {
      setBusyId(null)
      // 成功・失敗どちらでも最新の状態を表示する（利用者のキャンセルや他の管理者の操作も反映）
      setReloadKey((k) => k + 1)
    }
  }

  const list = reservations ?? []
  const filtered = filter === 'all' ? list : list.filter((r) => r.status === filter)
  const count = (s: ReservationStatus) => list.filter((r) => r.status === s).length
  const hospitalName = hospitals?.find((h) => h.hospitalId === hospitalId)?.name

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div>
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">予約管理</h1>
        {hospitalName && <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">{hospitalName}</p>}
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {hospitals == null ? (
        hospitalsFailed ? (
          <button onClick={() => { setError(null); setHospitalsKey((k) => k + 1) }} className="self-center font-button-text text-button-text text-primary px-4 py-2 rounded-xl border border-primary/20 hover:bg-primary/10">
            再試行
          </button>
        ) : (
          <div className="flex items-center justify-center py-xl">
            <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
          </div>
        )
      ) : hospitals.length === 0 ? (
        <p className="font-body-md text-body-md text-neutral-gray-600 text-center py-xl">
          管理対象の病院がありません。システム管理者に病院への紐付けを依頼してください。
        </p>
      ) : (
        <>
          {hospitals.length > 1 && (
            <select value={hospitalId ?? ''} aria-label="病院"
              onChange={(e) => { setReservations(null); setHospitalId(Number(e.target.value)) }}
              className="h-[44px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant">
              {hospitals.map((h) => (
                <option key={h.hospitalId} value={h.hospitalId}>{h.name}{h.status === 'SUSPENDED' ? '（停止中）' : ''}</option>
              ))}
            </select>
          )}

          <div className="grid grid-cols-3 gap-sm">
            {(['REQUESTED', 'APPROVED', 'COMPLETED'] as const).map((s) => (
              <div key={s} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-md text-center shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
                <p className={`font-headline-lg-mobile text-headline-lg-mobile font-bold ${statusConfig[s].color.split(' ')[0]}`}>{count(s)}</p>
                <p className="font-label-md text-label-md text-neutral-gray-600 mt-1">{statusConfig[s].label}</p>
              </div>
            ))}
          </div>

          <div className="flex items-center gap-sm">
            <span className="material-symbols-outlined text-neutral-gray-600">calendar_today</span>
            <input type="date" value={date} onChange={(e) => setDate(e.target.value)} aria-label="予約日"
              className="flex-1 h-[44px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant" />
            {date && (
              <button onClick={() => setDate('')} className="flex-shrink-0 px-3 h-[44px] rounded-lg border border-neutral-gray-100 font-label-md text-label-md text-neutral-gray-600">
                全日程
              </button>
            )}
          </div>

          <div className="flex gap-sm overflow-x-auto pb-1 hide-scrollbar">
            {FILTERS.map((f) => (
              <button key={f} onClick={() => setFilter(f)}
                className={`flex-shrink-0 px-4 py-2 rounded-full font-label-md text-label-md border transition-all ${filter === f ? 'bg-pet-green-vibrant text-white border-pet-green-vibrant' : 'bg-surface-container-lowest text-neutral-gray-600 border-neutral-gray-100'}`}>
                {f === 'all' ? 'すべて' : statusConfig[f].label}
              </button>
            ))}
          </div>

          {isLoading && reservations == null ? (
            <div className="flex items-center justify-center py-xl">
              <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
            </div>
          ) : loadFailed ? (
            <button onClick={reload} className="self-center font-button-text text-button-text text-primary px-4 py-2 rounded-xl border border-primary/20 hover:bg-primary/10">
              再試行
            </button>
          ) : filtered.length === 0 ? (
            <p className="font-body-md text-body-md text-neutral-gray-600 text-center py-xl">該当する予約はありません。</p>
          ) : (
            <div className="flex flex-col gap-sm">
              {filtered.map((r) => {
                const isBusy = busyId === r.reservationId
                return (
                  <div key={r.reservationId} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-container-margin flex flex-col gap-sm shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <h2 className="font-body-lg text-body-lg text-neutral-gray-900 font-medium truncate">{r.userNickname} · {r.petName}</h2>
                        <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">{PET_SPECIES_LABEL[r.petType]}</p>
                      </div>
                      <span className={`font-label-md text-label-md px-2.5 py-1 rounded-full flex-shrink-0 ${statusConfig[r.status].color}`}>{statusConfig[r.status].label}</span>
                    </div>
                    <div className="flex items-center gap-1 text-neutral-gray-600">
                      <span className="material-symbols-outlined text-[14px]">schedule</span>
                      <span className="font-label-md text-label-md">{r.availableDate} {formatTime(r.startTime)}〜{formatTime(r.endTime)}</span>
                    </div>
                    {r.memo && <p className="font-body-md text-body-md text-on-surface whitespace-pre-wrap break-words">{r.memo}</p>}
                    {r.status === 'REQUESTED' && (
                      <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                        <button disabled={isBusy} onClick={() => run(r, '却下しますか？', () => reservationApi.reject(r.reservationId), '予約の却下に失敗しました。')}
                          className="flex-1 h-[36px] bg-error-red/10 text-error-red font-label-md text-label-md rounded-lg hover:bg-error-red/20 transition-colors disabled:opacity-50">却下</button>
                        <button disabled={isBusy} onClick={() => run(r, '承認しますか？', () => reservationApi.approve(r.reservationId), '予約の承認に失敗しました。')}
                          className="flex-1 h-[36px] bg-pet-green-vibrant text-white font-label-md text-label-md rounded-lg hover:bg-primary transition-colors disabled:opacity-50">承認</button>
                      </div>
                    )}
                    {r.status === 'APPROVED' && (
                      <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                        <button disabled={isBusy} onClick={() => run(r, '診療完了にしますか？', () => reservationApi.complete(r.reservationId), '診療完了の登録に失敗しました。')}
                          className="flex-1 h-[36px] bg-primary/10 text-primary font-label-md text-label-md rounded-lg hover:bg-primary/20 transition-colors disabled:opacity-50">診療完了にする</button>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </>
      )}
    </div>
  )
}
