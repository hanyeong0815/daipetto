import { useCallback, useEffect, useState, FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { healthRecordApi } from '../api/healthRecord'
import { extractErrorMessage } from '../api/client'
import { todayInJapan } from '../utils/date'
import type { HealthRecord } from '../types'

const inputClass = 'w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all'

const emptyForm = () => ({ date: todayInJapan(), weight: '', symptom: '', memo: '' })

function recordIcon(r: HealthRecord) {
  if (r.symptom) return { icon: 'sick', color: 'text-warning-yellow bg-warning-yellow/10' }
  if (r.weight != null) return { icon: 'monitor_weight', color: 'text-success-blue bg-success-blue/10' }
  return { icon: 'note', color: 'text-neutral-gray-600 bg-neutral-gray-100' }
}

export default function HealthRecordPage() {
  const petId = Number(useParams().petId)
  const [records, setRecords] = useState<HealthRecord[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  // null = 新規登録、数値 = その記録の編集
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [isSaving, setIsSaving] = useState(false)

  const load = useCallback(async () => {
    try {
      const res = await healthRecordApi.getList(petId)
      setRecords(res.data ?? [])
    } catch (err) {
      setError(extractErrorMessage(err, '健康記録の取得に失敗しました。'))
    } finally {
      setIsLoading(false)
    }
  }, [petId])

  useEffect(() => {
    load()
  }, [load])

  const closeForm = () => {
    setShowForm(false)
    setEditingId(null)
    setForm(emptyForm())
  }

  const openEdit = (r: HealthRecord) => {
    setEditingId(r.healthRecordId)
    setForm({ date: r.recordedDate, weight: r.weight != null ? String(r.weight) : '', symptom: r.symptom ?? '', memo: r.memo ?? '' })
    setShowForm(true)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setIsSaving(true)
    // 体重はPATCHで消去できないため、空欄は「変更しない」として送らない。症状・メモは空文字で消去される
    const body = {
      recordedDate: form.date,
      weight: form.weight === '' ? undefined : Number(form.weight),
      symptom: editingId === null ? form.symptom || undefined : form.symptom,
      memo: editingId === null ? form.memo || undefined : form.memo,
    }
    try {
      if (editingId === null) {
        await healthRecordApi.create(petId, body)
      } else {
        await healthRecordApi.update(editingId, body)
      }
      closeForm()
      await load()
    } catch (err) {
      setError(extractErrorMessage(err, '健康記録の保存に失敗しました。'))
    } finally {
      setIsSaving(false)
    }
  }

  const handleDelete = async (healthRecordId: number) => {
    if (!window.confirm('この記録を削除してもよろしいですか？')) return
    setError(null)
    try {
      await healthRecordApi.remove(healthRecordId)
      await load()
    } catch (err) {
      setError(extractErrorMessage(err, '健康記録の削除に失敗しました。'))
    }
  }

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div className="flex justify-between items-center">
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">健康記録</h1>
        <button
          onClick={() => (showForm ? closeForm() : setShowForm(true))}
          className="flex items-center gap-1 bg-pet-green-vibrant text-white font-button-text text-button-text px-4 py-2.5 rounded-xl hover:bg-primary transition-colors active:scale-[0.98]"
        >
          <span className="material-symbols-outlined text-[18px]">{showForm ? 'close' : 'add'}</span>
          {showForm ? 'キャンセル' : '記録を追加'}
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {showForm && (
        <form
          onSubmit={handleSubmit}
          className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)]"
        >
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900">{editingId === null ? '新しい記録を追加' : '記録を編集'}</h2>

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">日付</label>
            <input type="date" required value={form.date} onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))} className={inputClass} />
          </div>

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">体重（kg・任意）</label>
            <div className="relative">
              <input
                type="number"
                step="0.01"
                min="0"
                max="999.99"
                value={form.weight}
                onChange={(e) => setForm((f) => ({ ...f, weight: e.target.value }))}
                placeholder="0.0"
                className={`${inputClass} pr-14`}
              />
              <span className="absolute right-sm top-1/2 -translate-y-1/2 font-body-md text-body-md text-neutral-gray-600">kg</span>
            </div>
          </div>

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">症状（任意）</label>
            <input type="text" maxLength={255} value={form.symptom} onChange={(e) => setForm((f) => ({ ...f, symptom: e.target.value }))} placeholder="例: 咳、食欲低下" className={inputClass} />
          </div>

          <div className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">メモ（任意）</label>
            <textarea
              rows={3}
              value={form.memo}
              onChange={(e) => setForm((f) => ({ ...f, memo: e.target.value }))}
              placeholder="観察した内容を入力してください"
              className="w-full px-sm py-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all resize-none"
            />
          </div>

          <button type="submit" disabled={isSaving} className="w-full h-[48px] bg-pet-green-vibrant hover:bg-primary disabled:bg-neutral-gray-100 disabled:text-neutral-gray-600 text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
            <span className="material-symbols-outlined">check</span>
            {isSaving ? '保存中…' : '保存'}
          </button>
        </form>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      ) : records.length === 0 ? (
        <p className="font-body-md text-body-md text-neutral-gray-600 text-center py-xl">健康記録はまだありません。</p>
      ) : (
        <div className="flex flex-col gap-sm">
          {records.map((r) => {
            const config = recordIcon(r)
            return (
              <div key={r.healthRecordId} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 px-container-margin py-md flex items-start gap-3 shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
                <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 ${config.color}`}>
                  <span className="material-symbols-outlined icon-fill text-[18px]">{config.icon}</span>
                </div>
                <div className="flex-grow min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-label-md text-label-md text-neutral-gray-600">{r.recordedDate}</span>
                    <div className="flex items-center gap-1 flex-shrink-0">
                      <button onClick={() => openEdit(r)} aria-label="編集" className="p-1 rounded-full text-neutral-gray-600 hover:bg-surface-container-low">
                        <span className="material-symbols-outlined text-[18px]">edit</span>
                      </button>
                      <button onClick={() => handleDelete(r.healthRecordId)} aria-label="削除" className="p-1 rounded-full text-error-red hover:bg-error-red/10">
                        <span className="material-symbols-outlined text-[18px]">delete</span>
                      </button>
                    </div>
                  </div>
                  {r.weight != null && <p className="font-body-lg text-body-lg text-neutral-gray-900 mt-1 font-medium">{r.weight} kg</p>}
                  {r.symptom && <p className="font-label-md text-label-md text-warning-yellow mt-1">症状: {r.symptom}</p>}
                  {r.memo && <p className="font-body-md text-body-md text-neutral-gray-900 mt-1 whitespace-pre-wrap">{r.memo}</p>}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
