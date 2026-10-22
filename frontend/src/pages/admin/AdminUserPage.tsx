import { useEffect, useState, FormEvent } from 'react'
import { adminUserApi } from '../../api/adminUser'
import { extractErrorMessage } from '../../api/client'
import { useAuthStore } from '../../stores/authStore'
import type { AdminUser, PageResponse, UserProfile } from '../../types'

type Role = UserProfile['role']
type Status = UserProfile['status']

const ROLE_CONFIG: Record<Role, { label: string; color: string }> = {
  ROLE_USER: { label: '一般ユーザー', color: 'text-neutral-gray-600 bg-neutral-gray-50' },
  ROLE_HOSPITAL_ADMIN: { label: '病院管理者', color: 'text-primary bg-primary/10' },
  ROLE_SYSTEM_ADMIN: { label: 'システム管理者', color: 'text-error-red bg-error-red/10' },
}

const PAGE_SIZE = 20

// SC-015 ユーザー管理（システム管理者のみ）。検索・停止/停止解除・権限変更
export default function AdminUserPage() {
  const myId = useAuthStore((s) => s.user?.userId)
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [roleFilter, setRoleFilter] = useState<Role | 'all'>('all')
  const [statusFilter, setStatusFilter] = useState<Status | 'all'>('all')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<PageResponse<AdminUser> | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [busyUserId, setBusyUserId] = useState<number | null>(null)

  useEffect(() => {
    // 条件を変えた後に前の条件の応答が遅れて届いても反映しない
    let active = true
    setIsLoading(true)
    adminUserApi
      .search({
        keyword: keyword || undefined,
        role: roleFilter === 'all' ? undefined : roleFilter,
        status: statusFilter === 'all' ? undefined : statusFilter,
        page,
        size: PAGE_SIZE,
      })
      .then((res) => {
        if (!active) return
        // 停止・権限変更で最後のページの最後の1件が条件から外れると、現在のページが範囲外になる。
        // 空のページに留まらず、最後の有効なページを取り直す
        if (res.data && res.data.content.length === 0 && page > 0 && page >= res.data.totalPages) {
          setPage(Math.max(0, res.data.totalPages - 1))
          return
        }
        setResult(res.data)
        setLoadFailed(false)
      })
      .catch((err) => {
        if (!active) return
        setError(extractErrorMessage(err, 'ユーザー一覧の取得に失敗しました。'))
        setLoadFailed(true)
      })
      .finally(() => active && setIsLoading(false))
    return () => {
      active = false
    }
  }, [keyword, roleFilter, statusFilter, page, reloadKey])

  const handleSearch = (e: FormEvent) => {
    e.preventDefault()
    setPage(0)
    setKeyword(keywordInput.trim())
  }

  const run = async (userId: number, action: () => Promise<unknown>, fallback: string) => {
    setError(null)
    setBusyUserId(userId)
    try {
      await action()
    } catch (err) {
      setError(extractErrorMessage(err, fallback))
    } finally {
      setBusyUserId(null)
      // 成功・失敗どちらでも最新の状態を表示する（他の管理者による変更も反映）
      setReloadKey((k) => k + 1)
    }
  }

  const toggleStatus = (u: AdminUser) => {
    if (u.status === 'ACTIVE') {
      if (!window.confirm(`${u.nickname}（${u.email}）を停止しますか？\nログイン中のセッションも次の操作から無効になります。`)) return
      run(u.userId, () => adminUserApi.suspend(u.userId), 'アカウントの停止に失敗しました。')
    } else {
      if (!window.confirm(`${u.nickname}（${u.email}）の停止を解除しますか？`)) return
      run(u.userId, () => adminUserApi.activate(u.userId), '停止解除に失敗しました。')
    }
  }

  const changeRole = (u: AdminUser, role: Role) => {
    if (role === u.role) return
    if (!window.confirm(`${u.nickname} の権限を「${ROLE_CONFIG[role].label}」に変更しますか？\n対象ユーザーの次の操作から反映されます。`)) {
      setReloadKey((k) => k + 1)
      return
    }
    run(u.userId, () => adminUserApi.changeRole(u.userId, role), '権限の変更に失敗しました。')
  }

  const filterButton = (active: boolean) =>
    `flex-shrink-0 px-4 py-2 rounded-full font-label-md text-label-md border transition-all ${active ? 'bg-pet-green-vibrant text-white border-pet-green-vibrant' : 'bg-surface-container-lowest text-neutral-gray-600 border-neutral-gray-100'}`

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div>
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">ユーザー管理</h1>
        {result && (
          <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">該当 {result.totalElements}名</p>
        )}
      </div>

      <form onSubmit={handleSearch} className="relative">
        <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600">search</span>
        <input
          type="search"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="メールアドレスまたはニックネームで検索（Enter）"
          aria-label="メールアドレスまたはニックネームで検索"
          className="w-full h-[48px] pl-10 pr-sm bg-surface-container-lowest border border-neutral-gray-100 rounded-xl font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
        />
      </form>

      <div className="flex flex-col gap-xs">
        <div className="flex gap-2 overflow-x-auto pb-1 hide-scrollbar">
          {(['all', 'ROLE_USER', 'ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN'] as const).map((r) => (
            <button key={r} onClick={() => { setPage(0); setRoleFilter(r) }} className={filterButton(roleFilter === r)}>
              {r === 'all' ? 'すべての権限' : ROLE_CONFIG[r].label}
            </button>
          ))}
        </div>
        <div className="flex gap-2 overflow-x-auto pb-1 hide-scrollbar">
          {([['all', 'すべての状態'], ['ACTIVE', '利用中'], ['SUSPENDED', '停止中']] as const).map(([s, label]) => (
            <button key={s} onClick={() => { setPage(0); setStatusFilter(s) }} className={filterButton(statusFilter === s)}>
              {label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {isLoading && !result ? (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      ) : loadFailed ? (
        <button onClick={() => { setError(null); setReloadKey((k) => k + 1) }} className="self-center font-button-text text-button-text text-primary px-4 py-2 rounded-xl border border-primary/20 hover:bg-primary/10">
          再試行
        </button>
      ) : !result || result.content.length === 0 ? (
        <p className="font-body-md text-body-md text-neutral-gray-600 text-center py-xl">該当するユーザーはいません。</p>
      ) : (
        <div className="flex flex-col gap-sm">
          {result.content.map((u) => {
            const isSelf = u.userId === myId
            const isBusy = busyUserId === u.userId
            return (
              <div key={u.userId}
                className={`bg-surface-container-lowest rounded-xl border p-container-margin flex flex-col gap-sm shadow-[0px_2px_8px_rgba(0,0,0,0.03)] ${u.status === 'SUSPENDED' ? 'border-error-red/20' : 'border-neutral-gray-100'}`}>
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center flex-shrink-0">
                      <span className="material-symbols-outlined text-secondary">person</span>
                    </div>
                    <div className="min-w-0">
                      <p className="font-body-lg text-body-lg text-neutral-gray-900 font-medium truncate">
                        {u.nickname}{isSelf && <span className="font-label-md text-label-md text-neutral-gray-600">（自分）</span>}
                      </p>
                      <p className="font-label-md text-label-md text-neutral-gray-600 truncate">{u.email}</p>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                    <span className={`font-label-md text-label-md px-2.5 py-1 rounded-full ${ROLE_CONFIG[u.role].color}`}>{ROLE_CONFIG[u.role].label}</span>
                    {u.status === 'SUSPENDED' && (
                      <span className="font-label-md text-label-md text-error-red bg-error-red/10 px-2.5 py-1 rounded-full">停止中</span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-1 text-neutral-gray-600">
                  <span className="material-symbols-outlined text-[14px]">calendar_today</span>
                  <span className="font-label-md text-label-md">登録: {u.createdAt.slice(0, 10)}</span>
                </div>
                {/* 自分自身の停止・権限変更はAPIでも禁止（USER-005） */}
                {!isSelf && (
                  <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                    <button onClick={() => toggleStatus(u)} disabled={isBusy}
                      className={`flex-1 h-[36px] font-label-md text-label-md rounded-lg transition-colors disabled:opacity-50 ${u.status === 'ACTIVE' ? 'bg-error-red/10 text-error-red hover:bg-error-red/20' : 'bg-success-blue/10 text-success-blue hover:bg-success-blue/20'}`}>
                      {u.status === 'ACTIVE' ? 'アカウント停止' : '停止解除'}
                    </button>
                    <select value={u.role} disabled={isBusy} onChange={(e) => changeRole(u, e.target.value as Role)}
                      aria-label={`${u.nickname}の権限`}
                      className="flex-1 h-[36px] px-2 bg-surface-container-low text-on-surface font-label-md text-label-md rounded-lg border border-neutral-gray-100 disabled:opacity-50">
                      {(Object.keys(ROLE_CONFIG) as Role[]).map((r) => (
                        <option key={r} value={r}>{ROLE_CONFIG[r].label}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {result && result.totalPages > 1 && (
        <div className="flex items-center justify-center gap-md">
          <button onClick={() => setPage((p) => p - 1)} disabled={page === 0 || isLoading}
            className="px-4 py-2 rounded-xl border border-neutral-gray-100 font-label-md text-label-md disabled:opacity-40">前へ</button>
          <span className="font-label-md text-label-md text-neutral-gray-600">{result.number + 1} / {result.totalPages}</span>
          <button onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= result.totalPages || isLoading}
            className="px-4 py-2 rounded-xl border border-neutral-gray-100 font-label-md text-label-md disabled:opacity-40">次へ</button>
        </div>
      )}
    </div>
  )
}
