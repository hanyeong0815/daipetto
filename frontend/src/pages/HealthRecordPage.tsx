import { useState, FormEvent } from 'react'
import { useParams } from 'react-router-dom'

type RecordType = 'symptom' | 'weight' | 'vaccine' | 'medicine' | 'note'

const typeConfig: Record<RecordType, { label: string; icon: string; color: string }> = {
  symptom: { label: '症状', icon: 'sick', color: 'text-warning-yellow bg-warning-yellow/10' },
  weight: { label: '体重', icon: 'monitor_weight', color: 'text-success-blue bg-success-blue/10' },
  vaccine: { label: '予防接種', icon: 'vaccines', color: 'text-primary bg-primary/10' },
  medicine: { label: '投薬', icon: 'medication', color: 'text-secondary bg-secondary-container' },
  note: { label: 'メモ', icon: 'note', color: 'text-neutral-gray-600 bg-neutral-gray-100' },
}

const mockRecords = [
  { id: 1, type: 'symptom' as RecordType, date: '2024-10-20', note: '軽い咳がある。2日間続いている。', weight: null },
  { id: 2, type: 'weight' as RecordType, date: '2024-10-14', note: '', weight: 28.5 },
  { id: 3, type: 'vaccine' as RecordType, date: '2024-10-01', note: '混合ワクチン（DHPP）3回目接種完了', weight: null },
  { id: 4, type: 'medicine' as RecordType, date: '2024-09-28', note: 'フィラリア予防薬投与（1錠）', weight: null },
  { id: 5, type: 'note' as RecordType, date: '2024-09-15', note: '食欲が平常より低下。フードの種類変更を検討。', weight: null },
]

export default function HealthRecordPage() {
  useParams()
  const [showForm, setShowForm] = useState(false)
  const [recordType, setRecordType] = useState<RecordType>('symptom')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [note, setNote] = useState('')
  const [weight, setWeight] = useState('')

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    setShowForm(false)
    setNote('')
    setWeight('')
  }

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div className="flex justify-between items-center">
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">健康記録</h1>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="flex items-center gap-1 bg-pet-green-vibrant text-white font-button-text text-button-text px-4 py-2.5 rounded-xl hover:bg-primary transition-colors active:scale-[0.98]"
        >
          <span className="material-symbols-outlined text-[18px]">{showForm ? 'close' : 'add'}</span>
          {showForm ? 'キャンセル' : '記録を追加'}
        </button>
      </div>

      {showForm && (
        <form
          onSubmit={handleSubmit}
          className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)]"
        >
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900">新しい記録を追加</h2>

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">記録の種類</label>
            <div className="flex gap-sm flex-wrap">
              {(Object.keys(typeConfig) as RecordType[]).map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setRecordType(t)}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full border font-label-md text-label-md transition-all ${
                    recordType === t ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary' : 'border-neutral-gray-100 text-neutral-gray-600'
                  }`}
                >
                  <span className="material-symbols-outlined text-[14px]">{typeConfig[t].icon}</span>
                  {typeConfig[t].label}
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">日付</label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
            />
          </div>

          {recordType === 'weight' && (
            <div className="flex flex-col gap-xs">
              <label className="font-label-md text-label-md text-on-surface-variant">体重（kg）</label>
              <div className="relative">
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  value={weight}
                  onChange={(e) => setWeight(e.target.value)}
                  placeholder="0.0"
                  className="w-full h-[48px] px-sm pr-14 bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
                />
                <span className="absolute right-sm top-1/2 -translate-y-1/2 font-body-md text-body-md text-neutral-gray-600">kg</span>
              </div>
            </div>
          )}

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">メモ</label>
            <textarea
              rows={3}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="観察した内容を入力してください"
              className="w-full px-sm py-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all resize-none"
            />
          </div>

          <button type="submit" className="w-full h-[48px] bg-pet-green-vibrant hover:bg-primary text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
            <span className="material-symbols-outlined">check</span>
            保存
          </button>
        </form>
      )}

      <div className="flex flex-col gap-sm">
        {mockRecords.map((r) => {
          const config = typeConfig[r.type]
          return (
            <div key={r.id} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 px-container-margin py-md flex items-start gap-3 shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
              <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 ${config.color}`}>
                <span className="material-symbols-outlined icon-fill text-[18px]">{config.icon}</span>
              </div>
              <div className="flex-grow min-w-0">
                <div className="flex items-center justify-between gap-2">
                  <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full ${config.color}`}>{config.label}</span>
                  <span className="font-label-md text-label-md text-neutral-gray-600 flex-shrink-0">{r.date}</span>
                </div>
                {r.weight != null && <p className="font-body-lg text-body-lg text-neutral-gray-900 mt-1 font-medium">{r.weight} kg</p>}
                {r.note && <p className="font-body-md text-body-md text-neutral-gray-900 mt-1">{r.note}</p>}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
