import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useAuthStore } from '../../stores/authStore'
import { useHospitalStore } from '../../stores/hospitalStore'
import { hospitalApi } from '../../api/hospital'
import { adminUserApi } from '../../api/adminUser'
import { extractErrorMessage } from '../../api/client'
import { formatTime } from '../../utils/date'
import type { AdminUser, DayOfWeek, HospitalAdminUser, HospitalBusinessHours, HospitalSchedule, ManagedHospital } from '../../types'

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


export default function AdminHospitalPage() {
  const { user } = useAuthStore()
  const isSystemAdmin = user?.role === 'ROLE_SYSTEM_ADMIN'
  const canManage = isSystemAdmin || user?.role === 'ROLE_HOSPITAL_ADMIN'

  const {
    selectedHospital,
    schedules,
    businessHours,
    isLoading,
    fetchHospitalDetail,
    fetchSchedules,
    fetchBusinessHours,
  } = useHospitalStore()

  // 選択肢はログイン中の管理者が操作できる病院だけ（システム管理者は停止中を含む全病院、病院管理者は紐付いた病院）
  const [hospitals, setHospitals] = useState<ManagedHospital[] | null>(null)
  const [hospitalsFailed, setHospitalsFailed] = useState(false)
  const [selectedHospitalId, setSelectedHospitalId] = useState<number | null>(null)
  // 更新後の再取得は、応答を待つ間に別の病院を選んでいたら行わない。遅れた再取得がstoreの最新要求になり、
  // 選択中とは別の病院を表示・保存対象にしてしまうため（REVIEW-001 R-01）
  const selectedRef = useRef<number | null>(null)
  const selectHospital = useCallback((hospitalId: number | null) => {
    selectedRef.current = hospitalId
    setSelectedHospitalId(hospitalId)
  }, [])
  const refreshIfSelected = (hospitalId: number, ...fetches: Array<(id: number) => Promise<void>>) =>
    selectedRef.current === hospitalId ? Promise.all(fetches.map((fetch) => fetch(hospitalId))) : Promise.resolve()
  const [basicInfoForm, setBasicInfoForm] = useState({ name: '', address: '', phoneNumber: '' })
  const [savedMessage, setSavedMessage] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createForm, setCreateForm] = useState({ name: '', address: '', phoneNumber: '' })

  const [editingDay, setEditingDay] = useState<DayOfWeek | null>(null)
  const [businessHoursForm, setBusinessHoursForm] = useState<BusinessHoursFormState>(emptyBusinessHoursForm)

  const [scheduleForm, setScheduleForm] = useState({ availableDate: '', startTime: '', endTime: '' })

  const fetchHospitals = useCallback(async () => {
    try {
      const res = await hospitalApi.getManagedHospitals()
      const list = res.data ?? []
      setHospitals(list)
      setHospitalsFailed(false)
      // 1件だけなら選択の手間を省く（病院管理者は通常1病院）
      if (selectedRef.current == null && list.length === 1) selectHospital(list[0].hospitalId)
    } catch (err) {
      // 取得失敗を「管理対象なし」と表示しない（再試行ボタンを出す。REVIEW-001 R-02）
      setActionError(extractErrorMessage(err, '管理対象の病院の取得に失敗しました。'))
      setHospitalsFailed(true)
    }
  }, [selectHospital])

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
        if (res.data) selectHospital(res.data.hospitalId)
        flashSaved('病院を登録しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '病院の登録に失敗しました。'))
    }
  }

  const handleSaveBasicInfo = async () => {
    if (!selectedHospitalId) return
    const hospitalId = selectedHospitalId
    setActionError(null)
    try {
      const res = await hospitalApi.update(hospitalId, {
        name: basicInfoForm.name,
        address: basicInfoForm.address,
        phoneNumber: basicInfoForm.phoneNumber || undefined,
      })
      if (res.success) {
        await Promise.all([refreshIfSelected(hospitalId, fetchHospitalDetail), fetchHospitals()])
        flashSaved('保存しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '保存に失敗しました。'))
    }
  }

  const handleSuspend = async () => {
    if (!selectedHospitalId || !window.confirm('この病院を停止しますか？停止後は現時点でAPIから再開できません。')) return
    const hospitalId = selectedHospitalId
    setActionError(null)
    try {
      const res = await hospitalApi.suspend(hospitalId)
      if (res.success) {
        // 停止中の病院も管理一覧に残るため、選択したまま状態表示を更新する
        await Promise.all([refreshIfSelected(hospitalId, fetchHospitalDetail), fetchHospitals()])
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
    const hospitalId = selectedHospitalId
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
        ? await hospitalApi.updateBusinessHours(hospitalId, existing.businessHoursId, body)
        : await hospitalApi.createBusinessHours(hospitalId, { dayOfWeek: day, ...body })
      if (res.success) {
        setEditingDay(null)
        await refreshIfSelected(hospitalId, fetchBusinessHours)
        flashSaved('診療時間を保存しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '診療時間の保存に失敗しました。'))
    }
  }

  const handleDeleteBusinessHours = async (businessHoursId: number) => {
    if (!selectedHospitalId || !window.confirm('この曜日の診療時間を削除しますか？削除すると休診扱いになります。')) return
    const hospitalId = selectedHospitalId
    setActionError(null)
    try {
      const res = await hospitalApi.deleteBusinessHours(hospitalId, businessHoursId)
      if (res.success) {
        await refreshIfSelected(hospitalId, fetchBusinessHours)
        flashSaved('削除しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '削除に失敗しました。'))
    }
  }

  const handleCreateSchedule = async (e: FormEvent) => {
    e.preventDefault()
    if (!selectedHospitalId) return
    const hospitalId = selectedHospitalId
    setActionError(null)
    try {
      const res = await hospitalApi.createSchedule(hospitalId, scheduleForm)
      if (res.success) {
        setScheduleForm({ availableDate: '', startTime: '', endTime: '' })
        await refreshIfSelected(hospitalId, fetchSchedules)
        flashSaved('予約枠を追加しました')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, '予約枠の追加に失敗しました。'))
    }
  }

  const handleToggleSchedule = async (schedule: HospitalSchedule) => {
    if (!selectedHospitalId) return
    const hospitalId = selectedHospitalId
    setActionError(null)
    try {
      const res =
        schedule.status === 'AVAILABLE'
          ? await hospitalApi.blockSchedule(hospitalId, schedule.scheduleId)
          : await hospitalApi.unblockSchedule(hospitalId, schedule.scheduleId)
      if (res.success) {
        await refreshIfSelected(hospitalId, fetchSchedules)
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
          onChange={(e) => selectHospital(e.target.value ? Number(e.target.value) : null)}
          className={inputClass}
        >
          <option value="">病院を選択してください</option>
          {(hospitals ?? []).map((h) => (
            <option key={h.hospitalId} value={h.hospitalId}>
              {h.name}{h.status === 'SUSPENDED' ? '（停止中）' : ''}
            </option>
          ))}
        </select>
        {hospitals == null && hospitalsFailed && (
          <button type="button" onClick={() => { setActionError(null); fetchHospitals() }}
            className="self-start font-button-text text-button-text text-primary px-4 py-2 rounded-xl border border-primary/20 hover:bg-primary/10">
            再試行
          </button>
        )}
        {hospitals?.length === 0 && (
          <p className="font-label-md text-label-md text-neutral-gray-600">
            {isSystemAdmin
              ? '登録されている病院はありません。「新規病院登録」から追加してください。'
              : '管理対象の病院がありません。システム管理者に病院への紐付けを依頼してください。'}
          </p>
        )}
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

      {/* 表示・編集するのは選択中の病院の詳細だけ（R-01の防御。保存先は常に選択中の病院） */}
      {!isLoading && selectedHospital && selectedHospital.id === selectedHospitalId && (
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

          {/* 病院管理者の紐付け（システム管理者のみ）。病院ごとに作り直して前の病院の表示を残さない */}
          {isSystemAdmin && <HospitalAdminSection key={selectedHospital.id} hospitalId={selectedHospital.id} />}

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

// ponytail: 候補は利用中の病院管理者権限ユーザーの先頭100名まで。超えるようならキーワード検索を足す
const CANDIDATE_LIMIT = 100

// SC-014 病院管理者の紐付け（システム管理者のみ）。紐付いたユーザーだけがこの病院を管理できる
function HospitalAdminSection({ hospitalId }: { hospitalId: number }) {
  const [admins, setAdmins] = useState<HospitalAdminUser[] | null>(null)
  const [candidates, setCandidates] = useState<AdminUser[]>([])
  const [selectedUserId, setSelectedUserId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [isBusy, setIsBusy] = useState(false)

  useEffect(() => {
    let active = true
    Promise.all([
      hospitalApi.getAdmins(hospitalId),
      adminUserApi.search({ role: 'ROLE_HOSPITAL_ADMIN', status: 'ACTIVE', page: 0, size: CANDIDATE_LIMIT }),
    ])
      .then(([adminRes, userRes]) => {
        if (!active) return
        setAdmins(adminRes.data ?? [])
        setCandidates(userRes.data?.content ?? [])
      })
      .catch((err) => active && setError(extractErrorMessage(err, '病院管理者の取得に失敗しました。')))
    return () => {
      active = false
    }
  }, [hospitalId, reloadKey])

  const run = async (action: () => Promise<unknown>, fallback: string) => {
    setError(null)
    setIsBusy(true)
    try {
      await action()
      setSelectedUserId('')
    } catch (err) {
      setError(extractErrorMessage(err, fallback))
    } finally {
      setIsBusy(false)
      // 成功・失敗どちらでも最新の紐付けを表示する（他の管理者による変更や権限変更による解除も反映）
      setReloadKey((k) => k + 1)
    }
  }

  const unassign = (admin: HospitalAdminUser) => {
    if (!window.confirm(`${admin.nickname}（${admin.email}）をこの病院の管理者から外しますか？`)) return
    run(() => hospitalApi.unassignAdmin(hospitalId, admin.userId), '病院管理者の解除に失敗しました。')
  }

  const linked = new Set((admins ?? []).map((a) => a.userId))
  const options = candidates.filter((u) => !linked.has(u.userId))

  return (
    <section className="flex flex-col gap-sm">
      <h2 className="font-headline-md text-headline-md text-neutral-gray-900">病院管理者</h2>
      <p className="font-label-md text-label-md text-neutral-gray-600">
        紐付けたユーザーだけが、この病院の予約・診療時間・予約枠を管理できます。候補はユーザー管理で「病院管理者」権限にした利用中のユーザーです。
      </p>

      {error && (
        <div className="bg-error-red/10 text-error-red font-label-md text-label-md px-4 py-2.5 rounded-lg flex items-center gap-2">
          <span className="material-symbols-outlined text-[18px]">error</span>
          {error}
        </div>
      )}

      {admins == null ? (
        error ? (
          <button onClick={() => { setError(null); setReloadKey((k) => k + 1) }} className="self-center font-button-text text-button-text text-primary px-4 py-2 rounded-xl border border-primary/20 hover:bg-primary/10">
            再試行
          </button>
        ) : (
          <div className="flex items-center justify-center py-md">
            <span className="material-symbols-outlined animate-spin text-primary text-[24px]">progress_activity</span>
          </div>
        )
      ) : (
        <>
          <div className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 divide-y divide-neutral-gray-100">
            {admins.map((a) => (
              <div key={a.userId} className="px-container-margin py-sm flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="font-body-md text-body-md text-neutral-gray-900 truncate">{a.nickname}</p>
                  <p className="font-label-md text-label-md text-neutral-gray-600 truncate">{a.email}</p>
                </div>
                <button onClick={() => unassign(a)} disabled={isBusy}
                  className="flex-shrink-0 px-3 py-1.5 rounded-full bg-error-red/10 text-error-red font-label-md text-label-md disabled:opacity-50">
                  解除
                </button>
              </div>
            ))}
            {admins.length === 0 && (
              <p className="px-container-margin py-md font-body-md text-body-md text-neutral-gray-600">まだ病院管理者が紐付けられていません。</p>
            )}
          </div>
          <div className="flex gap-2">
            <select value={selectedUserId} onChange={(e) => setSelectedUserId(e.target.value)} disabled={isBusy} aria-label="追加する病院管理者"
              className="flex-1 min-w-0 h-[44px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant">
              <option value="">{options.length === 0 ? '追加できるユーザーはいません' : '追加するユーザーを選択'}</option>
              {options.map((u) => (
                <option key={u.userId} value={u.userId}>{u.nickname}（{u.email}）</option>
              ))}
            </select>
            <button disabled={!selectedUserId || isBusy}
              onClick={() => run(() => hospitalApi.assignAdmin(hospitalId, Number(selectedUserId)), '病院管理者の追加に失敗しました。')}
              className="flex-shrink-0 px-4 h-[44px] bg-pet-green-vibrant text-white font-button-text text-button-text rounded-lg hover:bg-primary transition-colors disabled:opacity-50">
              追加
            </button>
          </div>
        </>
      )}
    </section>
  )
}
