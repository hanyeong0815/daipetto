import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useHospitalStore } from '../stores/hospitalStore'
import type { DayOfWeek } from '../types'

const DAY_ORDER: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const DAY_LABEL: Record<DayOfWeek, string> = {
  MONDAY: '月曜日',
  TUESDAY: '火曜日',
  WEDNESDAY: '水曜日',
  THURSDAY: '木曜日',
  FRIDAY: '金曜日',
  SATURDAY: '土曜日',
  SUNDAY: '日曜日',
}

// バックエンドは "HH:mm:ss" 形式で返すため表示用に秒を落とす
const formatTime = (time: string) => time.slice(0, 5)

export default function HospitalDetailPage() {
  const { hospitalId } = useParams()
  const { selectedHospital, businessHours, isLoading, fetchHospitalDetail, fetchBusinessHours } = useHospitalStore()

  useEffect(() => {
    if (!hospitalId) return
    fetchHospitalDetail(Number(hospitalId))
    fetchBusinessHours(Number(hospitalId))
  }, [hospitalId, fetchHospitalDetail, fetchBusinessHours])

  const handleShare = async () => {
    if (!selectedHospital) return
    const shareData = { title: selectedHospital.name, text: selectedHospital.address, url: window.location.href }
    if (navigator.share) {
      try {
        await navigator.share(shareData)
      } catch {
        // ユーザーがキャンセルした場合は何もしない
      }
    } else {
      await navigator.clipboard.writeText(window.location.href)
    }
  }

  if (isLoading || !selectedHospital) {
    return (
      <div className="flex items-center justify-center py-xl">
        <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
      </div>
    )
  }

  const hospital = selectedHospital
  const isOpen = hospital.status === 'ACTIVE'
  const businessHoursByDay = new Map(businessHours.map((bh) => [bh.dayOfWeek, bh]))

  return (
    <div className="pb-24">
      <div className="bg-surface-container-lowest px-container-margin pt-lg pb-xl border-b border-neutral-gray-100">
        <div className="flex items-start gap-4">
          <div className="w-16 h-16 rounded-xl bg-error-red/10 flex items-center justify-center flex-shrink-0">
            <span className="material-symbols-outlined text-error-red icon-fill text-[32px]">local_hospital</span>
          </div>
          <div className="flex-grow min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">{hospital.name}</h1>
              <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full flex-shrink-0 ${isOpen ? 'bg-pet-green-vibrant/10 text-primary' : 'bg-neutral-gray-100 text-neutral-gray-600'}`}>
                {isOpen ? '営業中' : '休止中'}
              </span>
            </div>
            <p className="font-body-md text-body-md text-neutral-gray-600 mt-1">{hospital.address}</p>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-sm mt-lg">
          <a
            href={hospital.phoneNumber ? `tel:${hospital.phoneNumber}` : undefined}
            aria-disabled={!hospital.phoneNumber}
            className={`flex items-center justify-center gap-2 h-[44px] bg-surface-container-low rounded-xl font-button-text text-button-text text-on-surface border border-neutral-gray-100 transition-colors ${hospital.phoneNumber ? 'hover:bg-surface-container' : 'opacity-50 pointer-events-none'}`}
          >
            <span className="material-symbols-outlined text-[18px]">phone</span>
            電話する
          </a>
          <button onClick={handleShare} className="flex items-center justify-center gap-2 h-[44px] bg-surface-container-low rounded-xl font-button-text text-button-text text-on-surface border border-neutral-gray-100 hover:bg-surface-container transition-colors">
            <span className="material-symbols-outlined text-[18px]">share</span>
            共有
          </button>
        </div>
      </div>

      <div className="px-container-margin flex flex-col gap-lg pt-lg">
        <section className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 overflow-hidden shadow-[0px_4px_20px_rgba(0,0,0,0.05)]">
          <div className="px-container-margin py-md border-b border-neutral-gray-100">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">診療時間</h2>
          </div>
          <div className="divide-y divide-neutral-gray-100">
            {DAY_ORDER.map((day) => {
              const bh = businessHoursByDay.get(day)
              return (
                <div key={day} className="flex justify-between items-center px-container-margin py-sm">
                  <span className="font-label-md text-label-md text-neutral-gray-600">{DAY_LABEL[day]}</span>
                  {bh ? (
                    <span className="font-body-md text-body-md text-neutral-gray-900">
                      {formatTime(bh.openTime)} - {formatTime(bh.closeTime)}
                      {bh.breakStartTime && bh.breakEndTime && (
                        <span className="text-neutral-gray-600">（休憩 {formatTime(bh.breakStartTime)} - {formatTime(bh.breakEndTime)}）</span>
                      )}
                    </span>
                  ) : (
                    <span className="font-body-md text-body-md text-error-red">休診</span>
                  )}
                </div>
              )
            })}
            {businessHours.length === 0 && (
              <p className="px-container-margin py-md font-body-md text-body-md text-neutral-gray-600">診療時間はまだ登録されていません。</p>
            )}
          </div>
        </section>
      </div>

      <div className="fixed bottom-0 left-0 right-0 px-container-margin pb-lg pt-sm bg-surface-container-lowest border-t border-neutral-gray-100 max-w-[800px] mx-auto">
        <Link
          to={`/hospitals/${hospitalId}/reserve`}
          className="w-full h-[52px] bg-pet-green-vibrant hover:bg-primary text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] shadow-[0px_4px_12px_rgba(46,204,113,0.2)]"
        >
          <span className="material-symbols-outlined">calendar_add_on</span>
          予約する
        </Link>
      </div>
    </div>
  )
}
