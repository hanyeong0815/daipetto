import { useState } from 'react'
import { Link } from 'react-router-dom'

type Status = 'upcoming' | 'completed' | 'cancelled'

interface Reservation {
  id: number
  hospitalName: string
  petName: string
  service: string
  date: string
  time: string
  status: Status
}

const reservations: Reservation[] = [
  { id: 1, hospitalName: '幸せ動物病院', petName: 'ボリ', service: 'フィラリア予防接種', date: '2024-10-25', time: '14:00', status: 'upcoming' },
  { id: 2, hospitalName: '愛情動物クリニック', petName: 'ナビ', service: '健康診断', date: '2024-10-18', time: '10:30', status: 'upcoming' },
  { id: 3, hospitalName: '幸せ動物病院', petName: 'ボリ', service: '皮膚科診察', date: '2024-09-30', time: '15:00', status: 'completed' },
  { id: 4, hospitalName: '笑顔動物病院', petName: 'ボリ', service: '外科手術', date: '2024-09-10', time: '11:00', status: 'cancelled' },
]

const statusConfig: Record<Status, { label: string; color: string }> = {
  upcoming: { label: '予約確定', color: 'text-primary bg-primary/10' },
  completed: { label: '診療完了', color: 'text-neutral-gray-600 bg-neutral-gray-50' },
  cancelled: { label: 'キャンセル', color: 'text-error-red bg-error-red/10' },
}

export default function ReservationHistoryPage() {
  const [filter, setFilter] = useState<'all' | Status>('all')

  const filtered = filter === 'all' ? reservations : reservations.filter((r) => r.status === filter)

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">予約履歴</h1>

      <div className="flex gap-sm border-b border-neutral-gray-100">
        {(['all', 'upcoming', 'completed', 'cancelled'] as const).map((f) => {
          const labels = { all: 'すべて', upcoming: '予定', completed: '完了', cancelled: 'キャンセル' }
          return (
            <button key={f} onClick={() => setFilter(f)}
              className={`pb-3 font-button-text text-button-text transition-all border-b-2 -mb-px ${filter === f ? 'border-pet-green-vibrant text-primary' : 'border-transparent text-neutral-gray-600 hover:text-on-surface'}`}>
              {labels[f]}
            </button>
          )
        })}
      </div>

      {filtered.length === 0 ? (
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
            return (
              <div key={r.id} className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-sm shadow-[0px_4px_20px_rgba(0,0,0,0.05)]">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h2 className="font-headline-md text-headline-md text-neutral-gray-900">{r.hospitalName}</h2>
                    <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">{r.service}</p>
                  </div>
                  <span className={`font-label-md text-label-md px-2.5 py-1 rounded-full flex-shrink-0 ${config.color}`}>{config.label}</span>
                </div>
                <div className="flex items-center gap-4 text-neutral-gray-600">
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">calendar_today</span>
                    <span className="font-body-md text-body-md">{r.date}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">schedule</span>
                    <span className="font-body-md text-body-md">{r.time}</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[16px]">pets</span>
                    <span className="font-body-md text-body-md">{r.petName}</span>
                  </div>
                </div>
                {r.status === 'upcoming' && (
                  <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                    <button className="flex-1 h-[40px] bg-error-red/10 text-error-red font-button-text text-button-text rounded-lg hover:bg-error-red/20 transition-colors">予約キャンセル</button>
                    <Link to={`/hospitals/${r.id}`} className="flex-1 h-[40px] bg-surface-container-low text-on-surface font-button-text text-button-text rounded-lg border border-neutral-gray-100 hover:bg-surface-container transition-colors flex items-center justify-center">
                      病院詳細
                    </Link>
                  </div>
                )}
                {r.status === 'completed' && (
                  <div className="pt-sm border-t border-neutral-gray-100">
                    <button className="w-full h-[40px] bg-surface-container-low text-primary font-button-text text-button-text rounded-lg border border-primary/20 hover:bg-primary/5 transition-colors">
                      レビューを書く
                    </button>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
