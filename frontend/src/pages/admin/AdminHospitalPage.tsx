import { useEffect, useState, type FormEvent } from 'react'
import { useAuthStore } from '../../stores/authStore'
import { useHospitalStore } from '../../stores/hospitalStore'
import { hospitalApi } from '../../api/hospital'
import type { DayOfWeek, HospitalBusinessHours, HospitalSchedule } from '../../types'

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

interface BusinessHoursFormState {
  openTime: string
  closeTime: string
  breakStartTime: string
  breakEndTime: string
  slotDurationMinutes: string
}

const emptyBusinessHoursForm: BusinessHoursFormState = {
  openTime: '09:00',
  closeTime: '18:00',
  breakStartTime: '',
  breakEndTime: '',
  slotDurationMinutes: '30',
}

function extractErrorMessage(err: unknown, fallback: string): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
}

// バックエンドは "HH:mm:ss" 形式で返すため表示用に秒を落とす
const formatTime = (time: string) => time.slice(0, 5)

export default function AdminHospitalPage() {
  const { user } = useAuthStore()
  const isSystemAdmin = user?.role === 'ROLE_SYSTEM_ADMIN'
  const canManage = isSystemAdmin || user?.role === 'ROLE_HOSPITAL_ADMIN'

  const {
    hospitals,
    selectedHospital,
    schedules,
    businessHours,
    isLoading,
    fetchHospitals,
    fetchHospitalDetail,
    fetchSchedules,
    fetchBusinessHours,
  } = useHospitalStore()

  const [selectedHospitalId, setSelectedHospitalId] = useState<number | null>(null)
  const [basicInfoForm, setBasicInfoForm] = useState({ name: '', address: '', phoneNumber: '' })
  const [savedMessage, setSavedMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createForm, setCreateForm] = useState({ name: '', address: '', phoneNumber: '' })

  const [editingDay, setEditingDay] = useState<DayOfWeek | null>(null)
  const [businessHoursForm, setBusinessHoursForm] = useState<BusinessHoursFormState>(emptyBusinessHoursForm)

  const [scheduleForm, setScheduleForm] = useState({ availableDate: '', startTime: '', endTime: '' })

  useEffect(() => {
    fetchHospitals()
  }, [fetchHospitals])

  useEffect(() => {
    if (selectedHospitalId == null) return
    fetchHospitalDetail(selectedHospitalId)
    fetchSchedules(selectedHospitalId)
    fetchBusinessHours(selectedHospitalId)
    setEditingDay(null)
  }, [selectedHospitalId, fetchHospitalDetail, fetchSchedules, fetchBusinessHours])

  useEffect(() => {
    if (selectedHospital) {
      setBasicInfoForm({ name: selectedHospital.name, address: selectedHospital.address, phoneNumber: selectedHospital.phoneNumber ?? '' })
    }
  }, [selectedHospital])

  const flashSaved = (message: string) => {
    setSavedMessage(message)
    setTimeout(() => setSavedMessage(null), 2000)
  }

  const handleCreateHospital = async (e: FormEvent) => {
    e.preventDefault()
    setActionError(null)
    try {
      const res = await hospitalApi.create({
        name: createForm.name,
        address: createForm.address,
        phoneNumber: createForm.phoneNumber || undefined,
      })
      if (res.success) {
        setShowCreateForm(false)
        setCreateForm({ name: '', address: '', phoneNumber: '' })
        await fetchHospitals()
        if (res.data) setSelectedHospitalId(res.data.hospitalId)
        flashSaved('病院を登録しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '病院の登録に失敗しました。'))
    }
  }

  const handleSaveBasicInfo = async () => {
    if (!selectedHospitalId) return
    setActionError(null)
    try {
      const res = await hospitalApi.update(selectedHospitalId, {
        name: basicInfoForm.name,
        address: basicInfoForm.address,
        phoneNumber: basicInfoForm.phoneNumber || undefined,
      })
      if (res.success) {
        await fetchHospitalDetail(selectedHospitalId)
        flashSaved('保存しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '保存に失敗しました。'))
    }
  }

  const handleSuspend = async () => {
    if (!selectedHospitalId || !window.confirm('この病院を停止しますか？停止後は現時点でAPIから再開できません。')) return
    setActionError(null)
    try {
      const res = await hospitalApi.suspend(selectedHospitalId)
      if (res.success) {
        setSelectedHospitalId(null)
        await fetchHospitals()
        flashSaved('病院を停止しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '病院の停止に失敗しました。'))
    }
  }

  const openBusinessHoursForm = (day: DayOfWeek, existing?: HospitalBusinessHours) => {
    setEditingDay(day)
    setBusinessHoursForm(
      existing
        ? {
            openTime: existing.openTime,
            closeTime: existing.closeTime,
            breakStartTime: existing.breakStartTime ?? '',
            breakEndTime: existing.breakEndTime ?? '',
            slotDurationMinutes: String(existing.slotDurationMinutes),
          }
        : emptyBusinessHoursForm,
    )
  }

  const handleSaveBusinessHours = async (day: DayOfWeek, existing?: HospitalBusinessHours) => {
    if (!selectedHospitalId) return
    setActionError(null)
    const body = {
      openTime: businessHoursForm.openTime,
      closeTime: businessHoursForm.closeTime,
      breakStartTime: businessHoursForm.breakStartTime || undefined,
      breakEndTime: businessHoursForm.breakEndTime || undefined,
      slotDurationMinutes: Number(businessHoursForm.slotDurationMinutes),
    }
    try {
      const res = existing
        ? await hospitalApi.updateBusinessHours(selectedHospitalId, existing.businessHoursId, body)
        : await hospitalApi.createBusinessHours(selectedHospitalId, { dayOfWeek: day, ...body })
      if (res.success) {
        setEditingDay(null)
        await fetchBusinessHours(selectedHospitalId)
        flashSaved('診療時間を保存しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '診療時間の保存に失敗しました。'))
    }
  }

  const handleDeleteBusinessHours = async (businessHoursId: number) => {
    if (!selectedHospitalId || !window.confirm('この曜日の診療時間を削除しますか？削除すると休診扱いになります。')) return
    setActionError(null)
    try {
      const res = await hospitalApi.deleteBusinessHours(selectedHospitalId, businessHoursId)
      if (res.success) {
        await fetchBusinessHours(selectedHospitalId)
        flashSaved('削除しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '削除に失敗しました。'))
    }
  }

  const handleCreateSchedule = async (e: FormEvent) => {
    e.preventDefault()
    if (!selectedHospitalId) return
    setActionError(null)
    try {
      const res = await hospitalApi.createSchedule(selectedHospitalId, scheduleForm)
      if (res.success) {
        setScheduleForm({ availableDate: '', startTime: '', endTime: '' })
        await fetchSchedules(selectedHospitalId)
        flashSaved('予約枠を追加しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '予約枠の追加に失敗しました。'))
    }
  }

  const handleToggleSchedule = async (schedule: HospitalSchedule) => {
    if (!selectedHospitalId) return
    setActionError(null)
    try {
      const res =
        schedule.status === 'AVAILABLE'
          ? await hospitalApi.blockSchedule(selectedHospitalId, schedule.scheduleId)
          : await hospitalApi.unblockSchedule(selectedHospitalId, schedule.scheduleId)
      if (res.success) {
        await fetchSchedules(selectedHospitalId)
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '予約枠の状態変更に失敗しました。'))
    }
  }

  const inputClass =
    'w-full h-[44px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all'
  const businessHoursByDay = new Map(businessHours.map((bh) => [bh.dayOfWeek, bh]))

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div>
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">病院情報管理</h1>
        <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">登録されている病院情報を編集できます。</p>
      </div>

      {savedMessage && (
        <div className="bg-success-blue/10 text-success-blue font-label-md text-label-md px-4 py-2.5 rounded-lg flex items-center gap-2">
          <span className="material-symbols-outlined text-[18px]">check_circle</span>
          {savedMessage}
        </div>
      )}
      {actionError && (
        <div className="bg-error-red/10 text-error-red font-label-md text-label-md px-4 py-2.5 rounded-lg flex items-center gap-2">
          <span className="material-symbols-outlined text-[18px]">error</span>
          {actionError}
        </div>
      )}

      {/* 病院選択 */}
      <section className="flex flex-col gap-sm">
        <div className="flex items-center justify-between">
          <label className="font-label-md text-label-md text-on-surface-variant">管理する病院</label>
          {isSystemAdmin && (
            <button
              type="button"
              onClick={() => setShowCreateForm((v) => !v)}
              className="flex items-center gap-1 text-primary font-label-md text-label-md"
            >
              <span className="material-symbols-outlined text-[16px]">add_circle</span>
              新規病院登録
            </button>
          )}
        </div>
        <select
          value={selectedHospitalId ?? ''}
          onChange={(e) => setSelectedHospitalId(e.target.value ? Number(e.target.value) : null)}
          className={inputClass}
        >
          <option value="">病院を選択してください</option>
          {hospitals.map((h) => (
            <option key={h.id} value={h.id}>
              {h.name}
            </option>
          ))}
        </select>
        <p className="font-label-md text-label-md text-neutral-gray-600">
          ※ 現時点では病院と管理者の紐付け（hospital_admins）が未実装のため、一覧から選択する方式になっています。
        </p>
      </section>

      {showCreateForm && isSystemAdmin && (
        <form onSubmit={handleCreateHospital} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-container-margin flex flex-col gap-sm">
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900">新規病院登録</h2>
          <input required placeholder="病院名" value={createForm.name} onChange={(e) => setCreateForm((p) => ({ ...p, name: e.target.value }))} className={inputClass} />
          <input required placeholder="住所" value={createForm.address} onChange={(e) => setCreateForm((p) => ({ ...p, address: e.target.value }))} className={inputClass} />
          <input placeholder="電話番号（任意）" value={createForm.phoneNumber} onChange={(e) => setCreateForm((p) => ({ ...p, phoneNumber: e.target.value }))} className={inputClass} />
          <button type="submit" className="h-[44px] bg-pet-green-vibrant text-white font-button-text text-button-text rounded-lg hover:bg-primary transition-colors">
            登録する
          </button>
        </form>
      )}

      {isLoading && (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      )}

      {!isLoading && selectedHospital && (
        <>
          {!canManage && (
            <div className="bg-warning-yellow/10 text-neutral-gray-900 font-label-md text-label-md px-4 py-2.5 rounded-lg">
              この病院を編集する権限がありません。
            </div>
          )}

          {/* 基本情報 */}
          <section className="flex flex-col gap-md">
            <div className="flex items-center justify-between">
              <h2 className="font-headline-md text-headline-md text-neutral-gray-900">基本情報</h2>
              <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full ${selectedHospital.status === 'ACTIVE' ? 'bg-pet-green-vibrant/10 text-primary' : 'bg-neutral-gray-100 text-neutral-gray-600'}`}>
                {selectedHospital.status === 'ACTIVE' ? '営業中' : '停止中'}
              </span>
            </div>
            {[
              { label: '病院名', key: 'name' as const, type: 'text' },
              { label: '住所', key: 'address' as const, type: 'text' },
              { label: '電話番号', key: 'phoneNumber' as const, type: 'tel' },
            ].map(({ label, key, type }) => (
              <div key={key} className="flex flex-col gap-xs">
                <label className="font-label-md text-label-md text-on-surface-variant">{label}</label>
                <input
                  type={type}
                  disabled={!canManage}
                  value={basicInfoForm[key]}
                  onChange={(e) => setBasicInfoForm((prev) => ({ ...prev, [key]: e.target.value }))}
                  className={inputClass}
                />
              </div>
            ))}
            {canManage && (
              <button onClick={handleSaveBasicInfo} className="h-[48px] bg-pet-green-vibrant text-white font-button-text text-button-text rounded-xl hover:bg-primary transition-colors">
                基本情報を保存
              </button>
            )}
            {isSystemAdmin && selectedHospital.status === 'ACTIVE' && (
              <button onClick={handleSuspend} className="h-[44px] bg-error-red/10 text-error-red font-button-text text-button-text rounded-xl border border-error-red/20 hover:bg-error-red/20 transition-colors">
                この病院を停止する
              </button>
            )}
          </section>

          {/* 営業時間 */}
          <section className="flex flex-col gap-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">診療時間（曜日別）</h2>
            <div className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 divide-y divide-neutral-gray-100 overflow-hidden">
              {DAY_ORDER.map((day) => {
                const existing = businessHoursByDay.get(day)
                const isEditing = editingDay === day

                return (
                  <div key={day} className="px-container-margin py-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">{DAY_LABEL[day]}</span>
                      {existing ? (
                        <span className="font-label-md text-label-md text-neutral-gray-600">
                          {formatTime(existing.openTime)} - {formatTime(existing.closeTime)}
                        </span>
                      ) : (
                        <span className="font-label-md text-label-md text-error-red">休診</span>
                      )}
                      {canManage && (
                        <div className="flex items-center gap-2 ml-2">
                          <button onClick={() => openBusinessHoursForm(day, existing)} className="text-primary">
                            <span className="material-symbols-outlined text-[18px]">{existing ? 'edit' : 'add_circle'}</span>
                          </button>
                          {existing && (
                            <button onClick={() => handleDeleteBusinessHours(existing.businessHoursId)} className="text-error-red">
                              <span className="material-symbols-outlined text-[18px]">delete</span>
                            </button>
                          )}
                        </div>
                      )}
                    </div>

                    {isEditing && (
                      <div className="mt-sm grid grid-cols-2 gap-2">
                        <div className="flex flex-col gap-1">
                          <label className="font-label-md text-label-md text-on-surface-variant">開始</label>
                          <input type="time" value={businessHoursForm.openTime} onChange={(e) => setBusinessHoursForm((p) => ({ ...p, openTime: e.target.value }))} className={inputClass} />
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="font-label-md text-label-md text-on-surface-variant">終了</label>
                          <input type="time" value={businessHoursForm.closeTime} onChange={(e) => setBusinessHoursForm((p) => ({ ...p, closeTime: e.target.value }))} className={inputClass} />
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="font-label-md text-label-md text-on-surface-variant">休憩開始（任意）</label>
                          <input type="time" value={businessHoursForm.breakStartTime} onChange={(e) => setBusinessHoursForm((p) => ({ ...p, breakStartTime: e.target.value }))} className={inputClass} />
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="font-label-md text-label-md text-on-surface-variant">休憩終了（任意）</label>
                          <input type="time" value={businessHoursForm.breakEndTime} onChange={(e) => setBusinessHoursForm((p) => ({ ...p, breakEndTime: e.target.value }))} className={inputClass} />
                        </div>
                        <div className="flex flex-col gap-1 col-span-2">
                          <label className="font-label-md text-label-md text-on-surface-variant">予約枠単位（分）</label>
                          <input type="number" min={5} value={businessHoursForm.slotDurationMinutes} onChange={(e) => setBusinessHoursForm((p) => ({ ...p, slotDurationMinutes: e.target.value }))} className={inputClass} />
                        </div>
                        <div className="col-span-2 flex gap-2">
                          <button onClick={() => handleSaveBusinessHours(day, existing)} className="flex-1 h-[40px] bg-pet-green-vibrant text-white font-button-text text-button-text rounded-lg hover:bg-primary transition-colors">
                            保存
                          </button>
                          <button onClick={() => setEditingDay(null)} className="flex-1 h-[40px] bg-neutral-gray-50 text-neutral-gray-600 font-button-text text-button-text rounded-lg border border-neutral-gray-100">
                            キャンセル
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          </section>

          {/* 予約枠 */}
          {canManage && (
            <section className="flex flex-col gap-sm">
              <h2 className="font-headline-md text-headline-md text-neutral-gray-900">個別予約枠（臨時追加・修正用）</h2>
              <form onSubmit={handleCreateSchedule} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-container-margin grid grid-cols-3 gap-2">
                <input required type="date" value={scheduleForm.availableDate} onChange={(e) => setScheduleForm((p) => ({ ...p, availableDate: e.target.value }))} className={inputClass} />
                <input required type="time" value={scheduleForm.startTime} onChange={(e) => setScheduleForm((p) => ({ ...p, startTime: e.target.value }))} className={inputClass} />
                <input required type="time" value={scheduleForm.endTime} onChange={(e) => setScheduleForm((p) => ({ ...p, endTime: e.target.value }))} className={inputClass} />
                <button type="submit" className="col-span-3 h-[40px] bg-pet-green-vibrant text-white font-button-text text-button-text rounded-lg hover:bg-primary transition-colors">
                  枠を追加
                </button>
              </form>

              <div className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 divide-y divide-neutral-gray-100 max-h-[320px] overflow-y-auto">
                {schedules.map((s) => (
                  <div key={s.scheduleId} className="px-container-margin py-sm flex items-center justify-between">
                    <div>
                      <p className="font-body-md text-body-md text-neutral-gray-900">{s.availableDate}</p>
                      <p className="font-label-md text-label-md text-neutral-gray-600">{formatTime(s.startTime)} - {formatTime(s.endTime)}</p>
                    </div>
                    <button
                      onClick={() => handleToggleSchedule(s)}
                      className={`px-3 py-1.5 rounded-full font-label-md text-label-md ${s.status === 'AVAILABLE' ? 'bg-pet-green-vibrant/10 text-primary' : 'bg-neutral-gray-100 text-neutral-gray-600'}`}
                    >
                      {s.status === 'AVAILABLE' ? '予約可能' : 'ブロック中'}
                    </button>
                  </div>
                ))}
                {schedules.length === 0 && (
                  <p className="px-container-margin py-md font-body-md text-body-md text-neutral-gray-600">個別予約枠はまだ登録されていません。</p>
                )}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
