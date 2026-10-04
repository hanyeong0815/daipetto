import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'
import { usePetStore } from '../stores/petStore'
import { reservationApi } from '../api/reservation'
import { healthRecordApi } from '../api/healthRecord'
import { daysFromToday, formatTime, isUpcoming } from '../utils/date'
import { PET_SPECIES_LABEL, type HealthRecord, type ReservationSummary } from '../types'

const WEEKDAYS = ['日', '月', '火', '水', '木', '金', '土']

// "2026-10-25" → "10月25日（日）"。日付だけをUTCで解釈し、端末のタイムゾーンに依存させない
function formatDateJa(date: string) {
  const d = new Date(`${date}T00:00:00Z`)
  return `${d.getUTCMonth() + 1}月${d.getUTCDate()}日（${WEEKDAYS[d.getUTCDay()]}）`
}

const relativeDay = (date: string) => {
  const diff = -daysFromToday(date)
  return diff <= 0 ? '今日' : `${diff}日前`
}

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const { pets, fetchPets } = usePetStore()
  const [nextReservation, setNextReservation] = useState<ReservationSummary | null>(null)
  const [reservationError, setReservationError] = useState(false)
  const [records, setRecords] = useState<HealthRecord[]>([])
  const [recordsError, setRecordsError] = useState(false)
  const firstPetId = pets[0]?.id

  useEffect(() => {
    fetchPets()
    reservationApi
      .getList()
      .then((res) => {
        // 「次の予約」は開始がJSTの現在より後のもの（当日でも開始時刻を過ぎたものは含めない）
        const upcoming = (res.data ?? [])
          .filter((r) => (r.status === 'REQUESTED' || r.status === 'APPROVED') && isUpcoming(r.availableDate, r.startTime))
          .sort((a, b) => `${a.availableDate}${a.startTime}`.localeCompare(`${b.availableDate}${b.startTime}`))
        setNextReservation(upcoming[0] ?? null)
      })
      .catch(() => setReservationError(true))
  }, [fetchPets])

  useEffect(() => {
    if (firstPetId === undefined) return
    let active = true
    setRecordsError(false)
    healthRecordApi
      .getList(firstPetId)
      .then((res) => active && setRecords(res.data ?? []))
      .catch(() => active && setRecordsError(true))
    return () => {
      active = false
    }
  }, [firstPetId])

  // 一覧は記録日の新しい順（docs/07 §6-1）
  const weights = records.filter((r) => r.weight != null).slice(0, 5).map((r) => Number(r.weight)).reverse()
  const latestWeight = weights.length > 0 ? weights[weights.length - 1] : null
  const weightDiff = latestWeight !== null && weights.length >= 2 ? latestWeight - weights[weights.length - 2] : null
  // 最小〜最大を30〜100%に割り当てる（最大値比だと数百gの変化が見えない）
  const minWeight = Math.min(...weights)
  const maxWeight = Math.max(...weights)
  const barHeight = (w: number) => (maxWeight === minWeight ? 100 : 30 + (70 * (w - minWeight)) / (maxWeight - minWeight))
  const symptoms = records.filter((r) => r.symptom).slice(0, 2)

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      {/* ウェルカム */}
      <section className="mt-xs">
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">
          こんにちは、<span className="text-primary">{user?.nickname ?? '飼い主'}</span>さん！
        </h1>
        <p className="font-body-md text-body-md text-neutral-gray-600 mt-1">
          今日もペットと健やかな一日をお過ごしください。
        </p>
      </section>

      {/* 次の予約カード */}
      <section>
        <Link
          to="/reservations"
          className="bg-surface-container-lowest rounded-[16px] p-container-margin shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 relative overflow-hidden flex flex-col gap-sm active:scale-[0.99] transition-transform block"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-pet-green-vibrant" />
          <div className="flex justify-between items-start pl-2">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary icon-fill text-[20px]">calendar_today</span>
              <span className="font-label-md text-label-md text-primary bg-primary-container/10 px-2.5 py-1 rounded-full">
                次の予約
              </span>
            </div>
            {nextReservation && (
              <span className="font-label-md text-label-md text-neutral-gray-600 bg-surface-container-low px-2 py-1 rounded-lg">
                {daysFromToday(nextReservation.availableDate) === 0 ? '今日' : `あと${daysFromToday(nextReservation.availableDate)}日`}
              </span>
            )}
          </div>
          {nextReservation ? (
            <div className="pl-2">
              <h3 className="font-headline-md text-headline-md text-neutral-gray-900 mb-1">
                {formatDateJa(nextReservation.availableDate)} {formatTime(nextReservation.startTime)}
              </h3>
              <p className="font-body-lg text-body-lg text-on-surface-variant mb-4">
                {nextReservation.hospitalName} - {nextReservation.status === 'APPROVED' ? '予約確定' : '承認待ち'}
              </p>
              <div className="flex justify-between items-center bg-neutral-gray-50 p-3 rounded-lg border border-neutral-gray-100">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-secondary-container overflow-hidden flex items-center justify-center">
                    <span className="material-symbols-outlined text-secondary text-[16px]">pets</span>
                  </div>
                  <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">{nextReservation.petName}</span>
                </div>
                <span className="font-label-md text-label-md text-neutral-gray-600 flex items-center gap-1">
                  詳細を見る <span className="material-symbols-outlined text-[16px]">chevron_right</span>
                </span>
              </div>
            </div>
          ) : (
            <p className="pl-2 font-body-md text-body-md text-neutral-gray-600">{reservationError ? '予約情報を取得できませんでした。' : '予定されている予約はありません。'}</p>
          )}
        </Link>
      </section>

      {/* マイペット */}
      <section>
        <div className="flex justify-between items-end mb-sm">
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900">マイペット</h2>
          <Link to="/pets" className="font-label-md text-label-md text-neutral-gray-600 hover:text-primary transition-colors">
            すべて見る
          </Link>
        </div>

        {pets.length === 0 ? (
          <Link
            to="/pets/new"
            className="bg-surface-container-lowest rounded-[16px] p-container-margin border border-dashed border-neutral-gray-100 flex flex-col items-center gap-sm py-lg active:scale-[0.99] transition-transform"
          >
            <div className="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center">
              <span className="material-symbols-outlined text-neutral-gray-600 text-[24px]">add</span>
            </div>
            <p className="font-body-md text-body-md text-neutral-gray-600">ペットを登録する</p>
          </Link>
        ) : (
          <div className="flex flex-col gap-sm">
            {pets.slice(0, 2).map((pet) => (
              <Link
                key={pet.id}
                to={`/pets/${pet.id}`}
                className="bg-surface-container-lowest rounded-[16px] p-container-margin shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex items-center gap-4 active:scale-[0.99] transition-transform"
              >
                <div className="w-16 h-16 rounded-full bg-secondary-container flex-shrink-0 border-2 border-surface-container-low flex items-center justify-center">
                  <span className="material-symbols-outlined text-secondary text-[28px]">pets</span>
                </div>
                <div className="flex-grow">
                  <div className="flex items-center justify-between">
                    <h3 className="font-headline-md text-headline-md text-neutral-gray-900">{pet.name}</h3>
                    <span className="material-symbols-outlined text-neutral-gray-600">chevron_right</span>
                  </div>
                  <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">
                    {PET_SPECIES_LABEL[pet.species] ?? '—'}
                    {pet.weight ? ` · ${pet.weight}kg` : ''}
                  </p>
                  <div className="flex gap-2 mt-3">
                    <span className="inline-flex items-center gap-1 bg-surface-bright border border-primary/20 text-primary font-label-md text-label-md px-2.5 py-1 rounded-full">
                      <span className="w-1.5 h-1.5 rounded-full bg-pet-green-vibrant" />
                      健康良好
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* 健康サマリー（先頭のペットの健康記録から表示） */}
      {firstPetId !== undefined && (
      <section>
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900 mb-sm">健康サマリー（{pets[0].name}）</h2>
        <div className="grid grid-cols-2 gap-gutter">
          {/* 体重推移 */}
          <div className="bg-surface-container-lowest rounded-[16px] p-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex flex-col justify-between aspect-square">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <div className="w-6 h-6 rounded-full bg-success-blue/10 flex items-center justify-center">
                  <span className="material-symbols-outlined text-success-blue icon-fill text-[14px]">monitor_weight</span>
                </div>
                <span className="font-label-md text-label-md text-neutral-gray-600">体重変化</span>
              </div>
              <span className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">
                {latestWeight ?? '—'}<span className="font-body-md text-body-md text-neutral-gray-600 ml-0.5">kg</span>
              </span>
            </div>
            {/* 直近5件の体重（古い順） */}
            <div className="w-full h-12 mt-auto flex items-end gap-1.5 justify-between">
              {weights.map((w, i) => (
                <div
                  key={i}
                  className={`w-full rounded-t-md ${i === weights.length - 1 ? 'bg-success-blue' : 'bg-neutral-gray-100'}`}
                  style={{ height: `${barHeight(w)}%` }}
                />
              ))}
            </div>
            <p className="font-label-md text-label-md text-success-blue mt-2 flex items-center gap-0.5">
              {weightDiff === null ? (
                recordsError ? '健康記録を取得できませんでした' : '体重の記録がまだありません'
              ) : (
                <>
                  <span className="material-symbols-outlined text-[12px]">{weightDiff >= 0 ? 'trending_up' : 'trending_down'}</span>
                  {weightDiff >= 0 ? '+' : ''}{weightDiff.toFixed(2)}kg（前回比）
                </>
              )}
            </p>
          </div>

          {/* 最近の症状 */}
          <div className="bg-surface-container-lowest rounded-[16px] p-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex flex-col justify-between aspect-square">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-6 h-6 rounded-full bg-warning-yellow/10 flex items-center justify-center">
                  <span className="material-symbols-outlined text-warning-yellow icon-fill text-[14px]">medical_information</span>
                </div>
                <span className="font-label-md text-label-md text-neutral-gray-600">最近の症状</span>
              </div>
              {symptoms.length === 0 ? (
                <p className="font-body-md text-body-md text-neutral-gray-600">{recordsError ? '健康記録を取得できませんでした。' : '記録された症状はありません。'}</p>
              ) : (
                <ul className="flex flex-col gap-2.5">
                  {symptoms.map((r, i) => (
                    <li key={r.healthRecordId} className="flex items-start gap-2">
                      <div className={`w-1.5 h-1.5 rounded-full mt-1.5 flex-shrink-0 ${i === 0 ? 'bg-warning-yellow' : 'bg-neutral-gray-100'}`} />
                      <div className="min-w-0">
                        <p className={`font-body-md text-body-md leading-tight truncate ${i === 0 ? 'text-neutral-gray-900' : 'text-neutral-gray-600'}`}>{r.symptom}</p>
                        <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">{relativeDay(r.recordedDate)}</p>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <Link
              to={`/pets/${firstPetId}/health`}
              className="mt-auto font-label-md text-label-md text-primary w-full text-center flex items-center justify-center gap-1 bg-surface-container-low py-2 rounded-lg hover:bg-surface-container transition-colors"
            >
              記録を追加 <span className="material-symbols-outlined text-[14px]">add</span>
            </Link>
          </div>
        </div>
      </section>
      )}

      {/* クイックメニュー */}
      <section>
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900 mb-sm">クイックメニュー</h2>
        <div className="grid grid-cols-3 gap-sm">
          {[
            { to: '/hospitals', icon: 'local_hospital', label: '病院を探す' },
            { to: '/pets/new', icon: 'add_circle', label: 'ペット登録' },
            { to: '/notifications', icon: 'notifications', label: '通知' },
          ].map(({ to, icon, label }) => (
            <Link
              key={to}
              to={to}
              className="bg-surface-container-lowest rounded-xl p-md border border-neutral-gray-100 flex flex-col items-center gap-2 hover:shadow-[0px_4px_20px_rgba(0,0,0,0.05)] transition-shadow active:scale-[0.98]"
            >
              <div className="w-10 h-10 rounded-full bg-primary-container/10 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary">{icon}</span>
              </div>
              <span className="font-label-md text-label-md text-neutral-gray-900 text-center">{label}</span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}
