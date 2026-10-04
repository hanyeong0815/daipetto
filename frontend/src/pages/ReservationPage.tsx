import { useEffect, useState, FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { usePetStore } from '../stores/petStore'
import { hospitalApi } from '../api/hospital'
import { reservationApi } from '../api/reservation'
import { extractErrorMessage } from '../api/client'
import { formatTime, isUpcoming } from '../utils/date'
import type { HospitalDetail, HospitalSchedule } from '../types'

type Step = 1 | 2 | 3

// 診療目的はAPIに項目が無いため、メモの先頭に添えて病院へ伝える
const services = ['健康診断', '予防接種', '皮膚科診察', '歯科診察', '外科手術', 'その他']

// 予約できる枠: AVAILABLE かつ開始がJSTの現在より後（バックエンドの RESERVATION-005 と同じ判定）
const isBookable = (s: HospitalSchedule) => s.status === 'AVAILABLE' && isUpcoming(s.availableDate, s.startTime)

// 病院が変わったら画面ごと作り直し、前の病院の枠・選択を持ち越さない（REVIEW-001 F-02）
export default function ReservationPage() {
  const { hospitalId } = useParams()
  return <ReservationForm key={hospitalId} hospitalId={Number(hospitalId)} />
}

function ReservationForm({ hospitalId }: { hospitalId: number }) {
  const navigate = useNavigate()
  const { pets, fetchPets } = usePetStore()
  // 病院情報・予約枠はこの病院専用のローカル状態で持つ（共有ストアだと別病院の応答が混ざる）
  const [hospital, setHospital] = useState<HospitalDetail | null>(null)
  const [schedules, setSchedules] = useState<HospitalSchedule[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [loadCount, setLoadCount] = useState(0)
  const [step, setStep] = useState<Step>(1)
  const [selectedPet, setSelectedPet] = useState<number | null>(null)
  const [selectedService, setSelectedService] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedScheduleId, setSelectedScheduleId] = useState<number | null>(null)
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    fetchPets()
  }, [fetchPets])

  useEffect(() => {
    // アンマウント後（別の病院へ移動後）に届いた応答は反映しない
    let active = true
    setLoadError(null)
    Promise.all([hospitalApi.getDetail(hospitalId), hospitalApi.getScheduleList(hospitalId)])
      .then(([detail, list]) => {
        if (!active) return
        setHospital(detail.data)
        setSchedules(list.data ?? [])
      })
      .catch((err) => {
        if (active) setLoadError(extractErrorMessage(err, '病院情報・予約枠の取得に失敗しました。'))
      })
    return () => {
      active = false
    }
  }, [hospitalId, loadCount])

  const reload = () => setLoadCount((c) => c + 1)
  // 現在の病院の情報が取れるまでは先へ進めない
  const isReady = hospital !== null

  const availableDates = [...new Set(schedules.filter(isBookable).map((s) => s.availableDate))].sort()
  const slots = schedules
    .filter((s) => s.availableDate === selectedDate)
    .sort((a, b) => a.startTime.localeCompare(b.startTime))
  const selectedSlot = schedules.find((s) => s.scheduleId === selectedScheduleId)

  const canGoStep2 = isReady && selectedPet !== null
  const canGoStep3 = selectedSlot !== undefined && isBookable(selectedSlot)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (selectedPet === null || !selectedSlot) return
    setError(null)
    // 確認画面にいる間に開始時刻を過ぎた枠は送らない
    if (!isBookable(selectedSlot)) {
      setError('選択した時間枠は受付を終了しました。日時を選び直してください。')
      setSelectedScheduleId(null)
      return
    }
    setIsSubmitting(true)
    try {
      const memo = [selectedService && `診療目的: ${selectedService}`, note.trim()].filter(Boolean).join('\n')
      await reservationApi.create({ petId: selectedPet, hospitalId, scheduleId: selectedSlot.scheduleId, memo: memo || undefined })
      navigate('/reservations')
    } catch (err) {
      setError(extractErrorMessage(err, '予約の申請に失敗しました。もう一度お試しください。'))
      // 重複予約などで枠の状態が変わっている可能性があるため取り直す
      reload()
    } finally {
      setIsSubmitting(false)
    }
  }

  const stepLabels: Record<Step, string> = {
    1: 'ペット＆診療を選択',
    2: '日時を選択',
    3: '予約内容を確認',
  }

  return (
    <form onSubmit={handleSubmit} className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      {/* ステップインジケーター */}
      <div className="flex items-center gap-2">
        {([1, 2, 3] as Step[]).map((s) => (
          <div key={s} className="flex items-center gap-2 flex-1">
            <div className={`w-7 h-7 rounded-full flex items-center justify-center font-label-md text-label-md flex-shrink-0 ${step >= s ? 'bg-pet-green-vibrant text-white' : 'bg-neutral-gray-100 text-neutral-gray-600'}`}>
              {step > s ? <span className="material-symbols-outlined text-[16px]">check</span> : s}
            </div>
            {s < 3 && <div className={`h-0.5 flex-1 rounded-full ${step > s ? 'bg-pet-green-vibrant' : 'bg-neutral-gray-100'}`} />}
          </div>
        ))}
      </div>
      <p className="font-body-md text-body-md text-neutral-gray-600 -mt-sm">{stepLabels[step]}</p>

      {loadError ? (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          <span className="flex-grow">{loadError}</span>
          <button type="button" onClick={reload} className="font-button-text text-button-text underline flex-shrink-0">再試行</button>
        </div>
      ) : !isReady && (
        <p className="flex items-center gap-2 font-body-md text-body-md text-neutral-gray-600">
          <span className="material-symbols-outlined animate-spin text-primary text-[18px]">progress_activity</span>
          病院情報と予約枠を読み込み中…
        </p>
      )}

      {step === 1 && (
        <div className="flex flex-col gap-lg">
          <section className="flex flex-col gap-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">ペットを選択</h2>
            {pets.length === 0 ? (
              <p className="font-body-md text-body-md text-neutral-gray-600">ペットが登録されていません。</p>
            ) : (
              pets.map((p) => (
                <button key={p.id} type="button" onClick={() => setSelectedPet(p.id)}
                  className={`flex items-center gap-4 p-container-margin rounded-xl border-2 transition-all ${selectedPet === p.id ? 'border-pet-green-vibrant bg-pet-green-vibrant/5' : 'border-neutral-gray-100 bg-surface-container-lowest'}`}>
                  <div className="w-12 h-12 rounded-full bg-secondary-container flex items-center justify-center flex-shrink-0">
                    <span className="material-symbols-outlined text-secondary">pets</span>
                  </div>
                  <div className="text-left">
                    <p className="font-body-lg text-body-lg text-neutral-gray-900 font-medium">{p.name}</p>
                    <p className="font-label-md text-label-md text-neutral-gray-600">{p.breed ?? '—'}</p>
                  </div>
                  <div className={`ml-auto w-5 h-5 rounded-full border-2 flex items-center justify-center flex-shrink-0 ${selectedPet === p.id ? 'border-pet-green-vibrant bg-pet-green-vibrant' : 'border-neutral-gray-100'}`}>
                    {selectedPet === p.id && <span className="material-symbols-outlined text-white text-[12px]">check</span>}
                  </div>
                </button>
              ))
            )}
          </section>

          <section className="flex flex-col gap-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">診療目的（任意）</h2>
            <div className="grid grid-cols-2 gap-sm">
              {services.map((s) => (
                <button key={s} type="button" onClick={() => setSelectedService(selectedService === s ? null : s)}
                  className={`py-3 px-4 rounded-xl border-2 font-body-md text-body-md text-left transition-all ${selectedService === s ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary' : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-900'}`}>
                  {s}
                </button>
              ))}
            </div>
          </section>

          <button type="button" disabled={!canGoStep2} onClick={() => setStep(2)}
            className="w-full h-[52px] bg-pet-green-vibrant disabled:bg-neutral-gray-100 disabled:text-neutral-gray-600 text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] mt-sm">
            次へ <span className="material-symbols-outlined">arrow_forward</span>
          </button>
        </div>
      )}

      {step === 2 && (
        <div className="flex flex-col gap-lg">
          <section className="flex flex-col gap-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">日付を選択</h2>
            {availableDates.length === 0 ? (
              <p className="font-body-md text-body-md text-neutral-gray-600">現在予約できる枠がありません。</p>
            ) : (
              <div className="flex gap-sm flex-wrap">
                {availableDates.map((d) => (
                  <button key={d} type="button" onClick={() => { setSelectedDate(d); setSelectedScheduleId(null) }}
                    className={`px-4 py-2.5 rounded-lg font-body-md text-body-md transition-all border-2 ${selectedDate === d ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary' : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-900 hover:border-pet-green-vibrant/50'}`}>
                    {d}
                  </button>
                ))}
              </div>
            )}
          </section>

          {selectedDate && (
            <section className="flex flex-col gap-sm">
              <h2 className="font-headline-md text-headline-md text-neutral-gray-900">時間を選択</h2>
              <div className="grid grid-cols-4 gap-sm">
                {slots.map((s) => {
                  const isBlocked = !isBookable(s)
                  const isSelected = selectedScheduleId === s.scheduleId
                  return (
                    <button key={s.scheduleId} type="button" disabled={isBlocked} onClick={() => setSelectedScheduleId(s.scheduleId)}
                      className={`py-2.5 rounded-lg font-body-md text-body-md transition-all border-2 ${isBlocked ? 'border-neutral-gray-100 bg-neutral-gray-50 text-neutral-gray-600/50 cursor-not-allowed line-through' : isSelected ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary' : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-900 hover:border-pet-green-vibrant/50'}`}>
                      {formatTime(s.startTime)}
                    </button>
                  )
                })}
              </div>
              <p className="font-label-md text-label-md text-neutral-gray-600 flex items-center gap-1">
                <span className="w-3 h-3 rounded bg-neutral-gray-50 border border-neutral-gray-100 inline-block" />
                打ち消し線 = 受付停止中・受付終了
              </p>
            </section>
          )}

          <div className="flex gap-sm">
            <button type="button" onClick={() => setStep(1)} className="flex-1 h-[52px] bg-surface-container-low text-on-surface font-button-text text-button-text rounded-xl border border-neutral-gray-100 flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
              <span className="material-symbols-outlined">arrow_back</span> 前へ
            </button>
            <button type="button" disabled={!canGoStep3} onClick={() => setStep(3)} className="flex-[2] h-[52px] bg-pet-green-vibrant disabled:bg-neutral-gray-100 disabled:text-neutral-gray-600 text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
              次へ <span className="material-symbols-outlined">arrow_forward</span>
            </button>
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="flex flex-col gap-lg">
          <section className="flex flex-col gap-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">予約内容の確認</h2>
            <div className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 shadow-[0px_4px_20px_rgba(0,0,0,0.05)] divide-y divide-neutral-gray-100">
              {[
                { label: '病院', value: hospital?.name ?? '—' },
                { label: 'ペット', value: pets.find((p) => p.id === selectedPet)?.name ?? '—' },
                { label: '診療目的', value: selectedService ?? '—' },
                { label: '予約日時', value: selectedSlot ? `${selectedSlot.availableDate} ${formatTime(selectedSlot.startTime)}〜${formatTime(selectedSlot.endTime)}` : '—' },
              ].map(({ label, value }) => (
                <div key={label} className="flex justify-between items-center px-container-margin py-md">
                  <span className="font-label-md text-label-md text-neutral-gray-600">{label}</span>
                  <span className="font-body-md text-body-md text-neutral-gray-900">{value}</span>
                </div>
              ))}
            </div>
          </section>

          <section className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">追加メモ（任意）</label>
            <textarea rows={3} value={note} onChange={(e) => setNote(e.target.value)} placeholder="医師に事前に伝えたいことを入力してください"
              className="w-full px-sm py-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-xl font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all resize-none" />
          </section>

          {error && (
            <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
              <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
              {error}
            </div>
          )}

          <div className="flex gap-sm">
            <button type="button" onClick={() => setStep(2)} className="flex-1 h-[52px] bg-surface-container-low text-on-surface font-button-text text-button-text rounded-xl border border-neutral-gray-100 flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
              <span className="material-symbols-outlined">arrow_back</span> 前へ
            </button>
            <button type="submit" disabled={isSubmitting} className="flex-[2] h-[52px] bg-pet-green-vibrant disabled:bg-neutral-gray-100 disabled:text-neutral-gray-600 text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] shadow-[0px_4px_12px_rgba(46,204,113,0.2)]">
              <span className="material-symbols-outlined">check</span> {isSubmitting ? '送信中…' : '予約を申請する'}
            </button>
          </div>
        </div>
      )}
    </form>
  )
}
