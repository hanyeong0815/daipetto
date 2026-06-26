import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePetStore } from '../stores/petStore'

type Step = 1 | 2 | 3

const services = ['健康診断', '予防接種', '皮膚科診察', '歯科診察', '外科手術', 'その他']
const timeSlots = ['09:00', '09:30', '10:00', '10:30', '11:00', '11:30', '14:00', '14:30', '15:00', '15:30', '16:00', '16:30']
const bookedSlots = ['10:00', '10:30', '15:00']

export default function ReservationPage() {
  const navigate = useNavigate()
  const { pets } = usePetStore()
  const [step, setStep] = useState<Step>(1)
  const [selectedPet, setSelectedPet] = useState<number | null>(null)
  const [selectedService, setSelectedService] = useState<string | null>(null)
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedTime, setSelectedTime] = useState<string | null>(null)
  const [note, setNote] = useState('')

  const canGoStep2 = selectedPet !== null && selectedService !== null
  const canGoStep3 = selectedDate !== '' && selectedTime !== null

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    navigate('/reservations')
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
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">診療目的</h2>
            <div className="grid grid-cols-2 gap-sm">
              {services.map((s) => (
                <button key={s} type="button" onClick={() => setSelectedService(s)}
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
            <input type="date" value={selectedDate} onChange={(e) => setSelectedDate(e.target.value)} min={new Date().toISOString().split('T')[0]}
              className="w-full h-[52px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-xl font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all" />
          </section>

          <section className="flex flex-col gap-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">時間を選択</h2>
            <div className="grid grid-cols-4 gap-sm">
              {timeSlots.map((t) => {
                const isBooked = bookedSlots.includes(t)
                const isSelected = selectedTime === t
                return (
                  <button key={t} type="button" disabled={isBooked} onClick={() => setSelectedTime(t)}
                    className={`py-2.5 rounded-lg font-body-md text-body-md transition-all border-2 ${isBooked ? 'border-neutral-gray-100 bg-neutral-gray-50 text-neutral-gray-600/50 cursor-not-allowed line-through' : isSelected ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary' : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-900 hover:border-pet-green-vibrant/50'}`}>
                    {t}
                  </button>
                )
              })}
            </div>
            <p className="font-label-md text-label-md text-neutral-gray-600 flex items-center gap-1">
              <span className="w-3 h-3 rounded bg-neutral-gray-50 border border-neutral-gray-100 inline-block" />
              打ち消し線 = 受付終了
            </p>
          </section>

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
                { label: '病院', value: '幸せ動物病院' },
                { label: 'ペット', value: pets.find((p) => p.id === selectedPet)?.name ?? '—' },
                { label: '診療目的', value: selectedService ?? '—' },
                { label: '予約日時', value: `${selectedDate} ${selectedTime}` },
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

          <div className="flex gap-sm">
            <button type="button" onClick={() => setStep(2)} className="flex-1 h-[52px] bg-surface-container-low text-on-surface font-button-text text-button-text rounded-xl border border-neutral-gray-100 flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
              <span className="material-symbols-outlined">arrow_back</span> 前へ
            </button>
            <button type="submit" className="flex-[2] h-[52px] bg-pet-green-vibrant text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] shadow-[0px_4px_12px_rgba(46,204,113,0.2)]">
              <span className="material-symbols-outlined">check</span> 予約を確定する
            </button>
          </div>
        </div>
      )}
    </form>
  )
}
