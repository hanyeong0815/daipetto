import { useState } from 'react'

type Status = 'pending' | 'confirmed' | 'completed' | 'cancelled'

interface Reservation {
  id: number
  userName: string
  petName: string
  service: string
  date: string
  time: string
  status: Status
}

const statusConfig: Record<Status, { label: string; color: string }> = {
  pending: { label: '承認待ち', color: 'text-warning-yellow bg-warning-yellow/10' },
  confirmed: { label: '予約確定', color: 'text-primary bg-primary/10' },
  completed: { label: '診療完了', color: 'text-neutral-gray-600 bg-neutral-gray-50' },
  cancelled: { label: 'キャンセル', color: 'text-error-red bg-error-red/10' },
}

const reservations: Reservation[] = [
  { id: 1, userName: '田中 太郎', petName: 'ボリ', service: '健康診断', date: '2024-10-25', time: '09:00', status: 'pending' },
  { id: 2, userName: '佐藤 花子', petName: 'ナビ', service: '予防接種', date: '2024-10-25', time: '10:00', status: 'confirmed' },
  { id: 3, userName: '鈴木 次郎', petName: 'コンイ', service: '皮膚科診察', date: '2024-10-25', time: '11:00', status: 'pending' },
  { id: 4, userName: '高橋 直子', petName: 'モカ', service: '健康診断', date: '2024-10-24', time: '14:30', status: 'completed' },
  { id: 5, userName: '伊藤 健一', petName: 'ハル', service: '歯科診察', date: '2024-10-24', time: '16:00', status: 'cancelled' },
]

export default function AdminReservationPage() {
  const [filter, setFilter] = useState<'all' | Status>('all')
  const [date, setDate] = useState('2024-10-25')

  const filtered = filter === 'all' ? reservations : reservations.filter((r) => r.status === filter)
  const stats = {
    pending: reservations.filter((r) => r.status === 'pending').length,
    confirmed: reservations.filter((r) => r.status === 'confirmed').length,
    completed: reservations.filter((r) => r.status === 'completed').length,
  }

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div>
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">予約管理</h1>
        <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">幸せ動物病院</p>
      </div>

      <div className="grid grid-cols-3 gap-sm">
        {[
          { label: '承認待ち', count: stats.pending, color: 'text-warning-yellow' },
          { label: '予約確定', count: stats.confirmed, color: 'text-primary' },
          { label: '診療完了', count: stats.completed, color: 'text-neutral-gray-600' },
        ].map(({ label, count, color }) => (
          <div key={label} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-md text-center shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
            <p className={`font-headline-lg-mobile text-headline-lg-mobile font-bold ${color}`}>{count}</p>
            <p className="font-label-md text-label-md text-neutral-gray-600 mt-1">{label}</p>
          </div>
        ))}
      </div>

      <div className="flex items-center gap-sm">
        <span className="material-symbols-outlined text-neutral-gray-600">calendar_today</span>
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)}
          className="flex-1 h-[44px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant" />
      </div>

      <div className="flex gap-sm overflow-x-auto pb-1 hide-scrollbar">
        {(['all', 'pending', 'confirmed', 'completed', 'cancelled'] as const).map((f) => {
          const labels = { all: 'すべて', pending: '待ち', confirmed: '確定', completed: '完了', cancelled: 'キャンセル' }
          return (
            <button key={f} onClick={() => setFilter(f)}
              className={`flex-shrink-0 px-4 py-2 rounded-full font-label-md text-label-md border transition-all ${filter === f ? 'bg-pet-green-vibrant text-white border-pet-green-vibrant' : 'bg-surface-container-lowest text-neutral-gray-600 border-neutral-gray-100'}`}>
              {labels[f]}
            </button>
          )
        })}
      </div>

      <div className="flex flex-col gap-sm">
        {filtered.map((r) => {
          const config = statusConfig[r.status]
          return (
            <div key={r.id} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-container-margin flex flex-col gap-sm shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <h2 className="font-body-lg text-body-lg text-neutral-gray-900 font-medium">{r.userName} · {r.petName}</h2>
                  <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">{r.service}</p>
                </div>
                <span className={`font-label-md text-label-md px-2.5 py-1 rounded-full flex-shrink-0 ${config.color}`}>{config.label}</span>
              </div>
              <div className="flex items-center gap-1 text-neutral-gray-600">
                <span className="material-symbols-outlined text-[14px]">schedule</span>
                <span className="font-label-md text-label-md">{r.time}</span>
              </div>
              {r.status === 'pending' && (
                <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                  <button className="flex-1 h-[36px] bg-error-red/10 text-error-red font-label-md text-label-md rounded-lg hover:bg-error-red/20 transition-colors">拒否</button>
                  <button className="flex-1 h-[36px] bg-pet-green-vibrant text-white font-label-md text-label-md rounded-lg hover:bg-primary transition-colors">承認</button>
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
