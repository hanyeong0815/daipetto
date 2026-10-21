import { useEffect, useState, FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import { vaccinationApi } from '../api/vaccination'
import { extractErrorMessage } from '../api/client'
import { daysFromToday, todayInJapan } from '../utils/date'
import type { Vaccination, VaccinationStatus } from '../types'

const inputClass = 'w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all'

const STATUS_CONFIG: Record<VaccinationStatus, { label: string; color: string; icon: string }> = {
  SCHEDULED: { label: '予定', color: 'text-success-blue bg-success-blue/10', icon: 'event' },
  COMPLETED: { label: '接種済み', color: 'text-primary bg-primary/10', icon: 'check_circle' },
  CANCELLED: { label: '中止', color: 'text-neutral-gray-600 bg-neutral-gray-100', icon: 'event_busy' },
}

function scheduleLabel(v: Vaccination): string {
  if (v.status === 'COMPLETED') return `接種日 ${v.completedDate ?? '—'}`
  if (v.status === 'CANCELLED') return `予定日 ${v.scheduledDate}`
  const days = daysFromToday(v.scheduledDate)
  if (days === 0) return `${v.scheduledDate}（今日）`
  return days > 0 ? `${v.scheduledDate}（あと${days}日）` : `${v.scheduledDate}（${-days}日経過）`
}

// 接種完了・再登録は日付の入力が必要なため、対象の行にだけ入力欄を開く
type DateAction = { vaccinationId: number; kind: 'complete' | 'reschedule'; date: string }

// ペットごとに作り直し、前のペットの一覧・入力中の操作を次のペットへ持ち越さない（取得失敗時も含む）
export default function VaccinationPage() {
  const { petId } = useParams()
  return <VaccinationList key={petId} petId={Number(petId)} />
}

function VaccinationList({ petId }: { petId: number }) {
  const [items, setItems] = useState<Vaccination[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // 最新の一覧取得が失敗したか。失敗を「予定なし」と区別し、古い一覧の操作ボタンも出さない
  const [loadFailed, setLoadFailed] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ vaccineName: '', scheduledDate: todayInJapan() })
  const [dateAction, setDateAction] = useState<DateAction | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    // 再読み込みの前の応答が遅れて届いても反映しない
    let active = true
    setIsLoading(true)
    vaccinationApi
      .getList(petId)
      .then((res) => {
        if (!active) return
        setItems(res.data ?? [])
        setLoadFailed(false)
      })
      .catch((err) => {
        if (!active) return
        setError(extractErrorMessage(err, 'ワクチン予定の取得に失敗しました。'))
        setLoadFailed(true)
      })
      .finally(() => active && setIsLoading(false))
    return () => {
      active = false
    }
  }, [petId, reloadKey])

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setError(null)
    setIsSaving(true)
    try {
      await action()
      setDateAction(null)
      setReloadKey((k) => k + 1)
      return true
    } catch (err) {
      setError(extractErrorMessage(err, fallback))
      // 別の端末などで状態が変わっていた場合（409/404）に、画面の古い状態を最新に戻す
      setReloadKey((k) => k + 1)
      return false
    } finally {
      setIsSaving(false)
    }
  }

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault()
    const ok = await run(() => vaccinationApi.create(petId, { vaccineName: form.vaccineName.trim(), scheduledDate: form.scheduledDate }), 'ワクチン予定の登録に失敗しました。')
    if (ok) {
      setShowForm(false)
      setForm({ vaccineName: '', scheduledDate: todayInJapan() })
    }
  }

  const handleDateAction = (e: FormEvent) => {
    e.preventDefault()
    if (!dateAction) return
    const { vaccinationId, kind, date } = dateAction
    if (kind === 'complete') {
      run(() => vaccinationApi.complete(vaccinationId, date), '接種完了の登録に失敗しました。')
    } else {
      run(() => vaccinationApi.reschedule(vaccinationId, date), '再登録に失敗しました。')
    }
  }

  const handleCancel = (v: Vaccination) => {
    if (!window.confirm(`「${v.vaccineName}」の予定を中止しますか？`)) return
    run(() => vaccinationApi.cancel(v.vaccinationId), '予定の中止に失敗しました。')
  }

  const handleDelete = (v: Vaccination) => {
    if (!window.confirm(`「${v.vaccineName}」を削除してもよろしいですか？`)) return
    run(() => vaccinationApi.remove(v.vaccinationId), 'ワクチン予定の削除に失敗しました。')
  }

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div className="flex justify-between items-center">
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">ワクチン予定</h1>
        <button
          onClick={() => setShowForm((s) => !s)}
          className="flex items-center gap-1 bg-pet-green-vibrant text-white font-button-text text-button-text px-4 py-2.5 rounded-xl hover:bg-primary transition-colors active:scale-[0.98]"
        >
          <span className="material-symbols-outlined text-[18px]">{showForm ? 'close' : 'add'}</span>
          {showForm ? 'キャンセル' : '予定を追加'}
        </button>
      </div>

      <p className="font-body-md text-body-md text-neutral-gray-600">接種予定日が7日以内になると、朝9時に一度だけ通知でお知らせします。予定日を変更した場合は、新しい予定日で改めてお知らせします。</p>

      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {showForm && (
        <form
          onSubmit={handleCreate}
          className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)]"
        >
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900">新しい予定を追加</h2>
          <div className="flex flex-col gap-xs">
            <label htmlFor="vaccine-name" className="font-label-md text-label-md text-on-surface-variant">ワクチン名</label>
            <input
              id="vaccine-name"
              type="text"
              required
              maxLength={100}
              value={form.vaccineName}
              onChange={(e) => setForm((f) => ({ ...f, vaccineName: e.target.value }))}
              placeholder="例: 混合ワクチン、狂犬病"
              className={inputClass}
            />
          </div>
          <div className="flex flex-col gap-xs">
            <label htmlFor="vaccine-date" className="font-label-md text-label-md text-on-surface-variant">接種予定日</label>
            <input id="vaccine-date" type="date" required value={form.scheduledDate} onChange={(e) => setForm((f) => ({ ...f, scheduledDate: e.target.value }))} className={inputClass} />
          </div>
          <button type="submit" disabled={isSaving || !form.vaccineName.trim()} className="w-full h-[48px] bg-pet-green-vibrant hover:bg-primary disabled:bg-neutral-gray-100 disabled:text-neutral-gray-600 text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98]">
            <span className="material-symbols-outlined">check</span>
            {isSaving ? '保存中…' : '登録'}
          </button>
        </form>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      ) : loadFailed ? (
        <button onClick={() => { setError(null); setReloadKey((k) => k + 1) }} className="self-center font-button-text text-button-text text-primary px-4 py-2 rounded-xl border border-primary/20 hover:bg-primary/10">
          再試行
        </button>
      ) : items.length === 0 ? (
        <p className="font-body-md text-body-md text-neutral-gray-600 text-center py-xl">ワクチン予定はまだありません。</p>
      ) : (
        <div className="flex flex-col gap-sm">
          {items.map((v) => {
            const config = STATUS_CONFIG[v.status]
            const isOpen = dateAction?.vaccinationId === v.vaccinationId
            return (
              <div key={v.vaccinationId} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 px-container-margin py-md flex flex-col gap-sm shadow-[0px_2px_8px_rgba(0,0,0,0.03)]">
                <div className="flex items-start gap-3">
                  <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 ${config.color}`}>
                    <span className="material-symbols-outlined icon-fill text-[18px]">{config.icon}</span>
                  </div>
                  <div className="flex-grow min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <p className="font-body-lg text-body-lg text-neutral-gray-900 font-medium truncate">{v.vaccineName}</p>
                      <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full flex-shrink-0 ${config.color}`}>{config.label}</span>
                    </div>
                    <p className="font-label-md text-label-md text-neutral-gray-600 mt-1">{scheduleLabel(v)}</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-xs justify-end">
                  {v.status === 'SCHEDULED' && (
                    <>
                      <button onClick={() => setDateAction({ vaccinationId: v.vaccinationId, kind: 'complete', date: todayInJapan() })} disabled={isSaving} className="font-label-md text-label-md px-3 py-1.5 rounded-lg bg-primary/10 text-primary hover:bg-primary/20 disabled:opacity-50">
                        接種完了
                      </button>
                      <button onClick={() => handleCancel(v)} disabled={isSaving} className="font-label-md text-label-md px-3 py-1.5 rounded-lg bg-neutral-gray-100 text-neutral-gray-600 hover:bg-neutral-gray-50 disabled:opacity-50">
                        中止
                      </button>
                    </>
                  )}
                  {v.status === 'CANCELLED' && (
                    <button onClick={() => setDateAction({ vaccinationId: v.vaccinationId, kind: 'reschedule', date: todayInJapan() })} disabled={isSaving} className="font-label-md text-label-md px-3 py-1.5 rounded-lg bg-success-blue/10 text-success-blue hover:bg-success-blue/20 disabled:opacity-50">
                      再登録
                    </button>
                  )}
                  <button onClick={() => handleDelete(v)} disabled={isSaving} aria-label={`${v.vaccineName}を削除`} className="font-label-md text-label-md px-3 py-1.5 rounded-lg text-error-red hover:bg-error-red/10 disabled:opacity-50">
                    削除
                  </button>
                </div>

                {isOpen && dateAction && (
                  <form onSubmit={handleDateAction} className="flex items-end gap-xs border-t border-neutral-gray-100 pt-sm">
                    <div className="flex-grow flex flex-col gap-xs">
                      <label htmlFor={`action-date-${v.vaccinationId}`} className="font-label-md text-label-md text-on-surface-variant">
                        {dateAction.kind === 'complete' ? '接種日' : '新しい接種予定日'}
                      </label>
                      <input
                        id={`action-date-${v.vaccinationId}`}
                        type="date"
                        required
                        // 接種日は未来日不可（API側も@PastOrPresentで検証）
                        max={dateAction.kind === 'complete' ? todayInJapan() : undefined}
                        value={dateAction.date}
                        onChange={(e) => setDateAction({ ...dateAction, date: e.target.value })}
                        className={inputClass}
                      />
                    </div>
                    <button type="submit" disabled={isSaving} className="h-[48px] px-4 bg-pet-green-vibrant hover:bg-primary disabled:bg-neutral-gray-100 text-white font-button-text text-button-text rounded-lg">
                      確定
                    </button>
                    <button type="button" onClick={() => setDateAction(null)} className="h-[48px] px-3 text-neutral-gray-600 font-button-text text-button-text rounded-lg hover:bg-neutral-gray-50">
                      戻る
                    </button>
                  </form>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
