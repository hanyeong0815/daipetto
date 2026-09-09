import { useState } from 'react'

type Role = 'USER' | 'HOSPITAL_ADMIN' | 'SYSTEM_ADMIN'
type UserStatus = 'active' | 'suspended'

interface User {
  id: number
  email: string
  nickname: string
  role: Role
  status: UserStatus
  joinedAt: string
  petCount: number
}

const roleConfig: Record<Role, { label: string; color: string }> = {
  USER: { label: '一般ユーザー', color: 'text-neutral-gray-600 bg-neutral-gray-50' },
  HOSPITAL_ADMIN: { label: '病院管理者', color: 'text-primary bg-primary/10' },
  SYSTEM_ADMIN: { label: 'システム管理者', color: 'text-error-red bg-error-red/10' },
}

const mockUsers: User[] = [
  { id: 1, email: 'tanaka@example.com', nickname: 'ボリパパ', role: 'USER', status: 'active', joinedAt: '2024-01-15', petCount: 2 },
  { id: 2, email: 'sato@example.com', nickname: 'ナビママ', role: 'USER', status: 'active', joinedAt: '2024-02-20', petCount: 1 },
  { id: 3, email: 'hospital@happy.com', nickname: '幸せ病院', role: 'HOSPITAL_ADMIN', status: 'active', joinedAt: '2024-03-01', petCount: 0 },
  { id: 4, email: 'suzuki@example.com', nickname: 'コンイ飼い主', role: 'USER', status: 'suspended', joinedAt: '2024-04-10', petCount: 1 },
]

export default function AdminUserPage() {
  const [query, setQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState<Role | 'all'>('all')
  const [users, setUsers] = useState(mockUsers)

  const filtered = users.filter((u) => {
    const matchQuery = u.email.includes(query) || u.nickname.includes(query)
    const matchRole = roleFilter === 'all' || u.role === roleFilter
    return matchQuery && matchRole
  })

  const toggleStatus = (id: number) => {
    setUsers((prev) => prev.map((u) => u.id === id ? { ...u, status: u.status === 'active' ? 'suspended' : 'active' } : u))
  }

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div>
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">ユーザー管理</h1>
        <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">
          全ユーザー {users.length}名 · アクティブ {users.filter((u) => u.status === 'active').length}名
        </p>
      </div>

      <div className="relative">
        <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600">search</span>
        <input type="text" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="メールアドレスまたはニックネームで検索"
          className="w-full h-[48px] pl-10 pr-sm bg-surface-container-lowest border border-neutral-gray-100 rounded-xl font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all" />
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1 hide-scrollbar">
        {(['all', 'USER', 'HOSPITAL_ADMIN', 'SYSTEM_ADMIN'] as const).map((r) => {
          const labels = { all: 'すべて', USER: 'ユーザー', HOSPITAL_ADMIN: '病院', SYSTEM_ADMIN: '管理者' }
          return (
            <button key={r} onClick={() => setRoleFilter(r)}
              className={`flex-shrink-0 px-4 py-2 rounded-full font-label-md text-label-md border transition-all ${roleFilter === r ? 'bg-pet-green-vibrant text-white border-pet-green-vibrant' : 'bg-surface-container-lowest text-neutral-gray-600 border-neutral-gray-100'}`}>
              {labels[r]}
            </button>
          )
        })}
      </div>

      <div className="flex flex-col gap-sm">
        {filtered.map((u) => {
          const roleConf = roleConfig[u.role]
          return (
            <div key={u.id}
              className={`bg-surface-container-lowest rounded-xl border p-container-margin flex flex-col gap-sm shadow-[0px_2px_8px_rgba(0,0,0,0.03)] ${u.status === 'suspended' ? 'border-error-red/20 opacity-70' : 'border-neutral-gray-100'}`}>
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center flex-shrink-0">
                    <span className="material-symbols-outlined text-secondary">person</span>
                  </div>
                  <div className="min-w-0">
                    <p className="font-body-lg text-body-lg text-neutral-gray-900 font-medium truncate">{u.nickname}</p>
                    <p className="font-label-md text-label-md text-neutral-gray-600 truncate">{u.email}</p>
                  </div>
                </div>
                <div className="flex flex-col items-end gap-1.5 flex-shrink-0">
                  <span className={`font-label-md text-label-md px-2.5 py-1 rounded-full ${roleConf.color}`}>{roleConf.label}</span>
                  {u.status === 'suspended' && (
                    <span className="font-label-md text-label-md text-error-red bg-error-red/10 px-2.5 py-1 rounded-full">停止中</span>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-4 text-neutral-gray-600">
                <div className="flex items-center gap-1">
                  <span className="material-symbols-outlined text-[14px]">pets</span>
                  <span className="font-label-md text-label-md">ペット {u.petCount}匹</span>
                </div>
                <div className="flex items-center gap-1">
                  <span className="material-symbols-outlined text-[14px]">calendar_today</span>
                  <span className="font-label-md text-label-md">登録: {u.joinedAt}</span>
                </div>
              </div>
              <div className="flex gap-sm pt-sm border-t border-neutral-gray-100">
                <button onClick={() => toggleStatus(u.id)}
                  className={`flex-1 h-[36px] font-label-md text-label-md rounded-lg transition-colors ${u.status === 'active' ? 'bg-error-red/10 text-error-red hover:bg-error-red/20' : 'bg-success-blue/10 text-success-blue hover:bg-success-blue/20'}`}>
                  {u.status === 'active' ? 'アカウント停止' : '停止解除'}
                </button>
                <button className="flex-1 h-[36px] bg-surface-container-low text-on-surface font-label-md text-label-md rounded-lg border border-neutral-gray-100 hover:bg-surface-container transition-colors">
                  詳細を見る
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
