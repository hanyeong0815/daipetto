import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuthStore } from '../stores/authStore'
import { usePetStore } from '../stores/petStore'

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user)
  const { pets, fetchPets } = usePetStore()

  useEffect(() => {
    fetchPets()
  }, [fetchPets])

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      {/* ウェルカム */}
      <section className="mt-xs">
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">
          こんにちは、<span className="text-primary">{user?.nickname ?? '飼い主'}</span>さん！
        </h1>
        <p className="font-body-md text-body-md text-neutral-gray-600 mt-1">
          今日もペットと健やかな一日をお過ごしください。
        </p>
      </section>

      {/* 次の予約カード */}
      <section>
        <Link
          to="/reservations"
          className="bg-surface-container-lowest rounded-[16px] p-container-margin shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 relative overflow-hidden flex flex-col gap-sm active:scale-[0.99] transition-transform block"
        >
          <div className="absolute top-0 left-0 w-1.5 h-full bg-pet-green-vibrant" />
          <div className="flex justify-between items-start pl-2">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary icon-fill text-[20px]">calendar_today</span>
              <span className="font-label-md text-label-md text-primary bg-primary-container/10 px-2.5 py-1 rounded-full">
                次の予約
              </span>
            </div>
            <span className="font-label-md text-label-md text-neutral-gray-600 bg-surface-container-low px-2 py-1 rounded-lg">
              あと2日
            </span>
          </div>
          <div className="pl-2">
            <h3 className="font-headline-md text-headline-md text-neutral-gray-900 mb-1">
              10月25日（水）午後2:00
            </h3>
            <p className="font-body-lg text-body-lg text-on-surface-variant mb-4">
              幸せ動物病院 - フィラリア予防接種
            </p>
            <div className="flex justify-between items-center bg-neutral-gray-50 p-3 rounded-lg border border-neutral-gray-100">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-secondary-container overflow-hidden flex items-center justify-center">
                  <span className="material-symbols-outlined text-secondary text-[16px]">pets</span>
                </div>
                <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">ボリ</span>
              </div>
              <span className="font-label-md text-label-md text-neutral-gray-600 flex items-center gap-1">
                詳細を見る <span className="material-symbols-outlined text-[16px]">chevron_right</span>
              </span>
            </div>
          </div>
        </Link>
      </section>

      {/* マイペット */}
      <section>
        <div className="flex justify-between items-end mb-sm">
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900">マイペット</h2>
          <Link to="/pets" className="font-label-md text-label-md text-neutral-gray-600 hover:text-primary transition-colors">
            すべて見る
          </Link>
        </div>

        {pets.length === 0 ? (
          <Link
            to="/pets/new"
            className="bg-surface-container-lowest rounded-[16px] p-container-margin border border-dashed border-neutral-gray-100 flex flex-col items-center gap-sm py-lg active:scale-[0.99] transition-transform"
          >
            <div className="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center">
              <span className="material-symbols-outlined text-neutral-gray-600 text-[24px]">add</span>
            </div>
            <p className="font-body-md text-body-md text-neutral-gray-600">ペットを登録する</p>
          </Link>
        ) : (
          <div className="flex flex-col gap-sm">
            {pets.slice(0, 2).map((pet) => (
              <Link
                key={pet.id}
                to={`/pets/${pet.id}`}
                className="bg-surface-container-lowest rounded-[16px] p-container-margin shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex items-center gap-4 active:scale-[0.99] transition-transform"
              >
                <div className="w-16 h-16 rounded-full bg-secondary-container flex-shrink-0 border-2 border-surface-container-low flex items-center justify-center">
                  <span className="material-symbols-outlined text-secondary text-[28px]">pets</span>
                </div>
                <div className="flex-grow">
                  <div className="flex items-center justify-between">
                    <h3 className="font-headline-md text-headline-md text-neutral-gray-900">{pet.name}</h3>
                    <span className="material-symbols-outlined text-neutral-gray-600">chevron_right</span>
                  </div>
                  <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">
                    {pet.breed ?? '—'} · {pet.gender === 'MALE' ? 'オス' : 'メス'}
                    {pet.weight ? ` · ${pet.weight}kg` : ''}
                  </p>
                  <div className="flex gap-2 mt-3">
                    <span className="inline-flex items-center gap-1 bg-surface-bright border border-primary/20 text-primary font-label-md text-label-md px-2.5 py-1 rounded-full">
                      <span className="w-1.5 h-1.5 rounded-full bg-pet-green-vibrant" />
                      健康良好
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* 健康サマリー */}
      <section>
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900 mb-sm">健康サマリー</h2>
        <div className="grid grid-cols-2 gap-gutter">
          {/* 体重推移 */}
          <div className="bg-surface-container-lowest rounded-[16px] p-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex flex-col justify-between aspect-square">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <div className="w-6 h-6 rounded-full bg-success-blue/10 flex items-center justify-center">
                  <span className="material-symbols-outlined text-success-blue icon-fill text-[14px]">monitor_weight</span>
                </div>
                <span className="font-label-md text-label-md text-neutral-gray-600">体重変化</span>
              </div>
              <span className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">
                28.5<span className="font-body-md text-body-md text-neutral-gray-600 ml-0.5">kg</span>
              </span>
            </div>
            <div className="w-full h-12 mt-auto flex items-end gap-1.5 justify-between">
              {[40, 45, 42, 55, 60].map((h, i) => (
                <div
                  key={i}
                  className={`w-full rounded-t-md ${i === 4 ? 'bg-success-blue' : 'bg-neutral-gray-100'}`}
                  style={{ height: `${h}%` }}
                />
              ))}
            </div>
            <p className="font-label-md text-label-md text-success-blue mt-2 flex items-center gap-0.5">
              <span className="material-symbols-outlined text-[12px]">trending_up</span>
              +0.2kg（先月比）
            </p>
          </div>

          {/* 最近の症状 */}
          <div className="bg-surface-container-lowest rounded-[16px] p-md shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex flex-col justify-between aspect-square">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-6 h-6 rounded-full bg-warning-yellow/10 flex items-center justify-center">
                  <span className="material-symbols-outlined text-warning-yellow icon-fill text-[14px]">medical_information</span>
                </div>
                <span className="font-label-md text-label-md text-neutral-gray-600">最近の症状</span>
              </div>
              <ul className="flex flex-col gap-2.5">
                <li className="flex items-start gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-warning-yellow mt-1.5 flex-shrink-0" />
                  <div>
                    <p className="font-body-md text-body-md text-neutral-gray-900 leading-tight">軽い咳</p>
                    <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">2日前</p>
                  </div>
                </li>
                <li className="flex items-start gap-2">
                  <div className="w-1.5 h-1.5 rounded-full bg-neutral-gray-100 mt-1.5 flex-shrink-0" />
                  <div>
                    <p className="font-body-md text-body-md text-neutral-gray-600 leading-tight">食欲不振</p>
                    <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">1週間前</p>
                  </div>
                </li>
              </ul>
            </div>
            <Link
              to="/pets/1/health"
              className="mt-auto font-label-md text-label-md text-primary w-full text-center flex items-center justify-center gap-1 bg-surface-container-low py-2 rounded-lg hover:bg-surface-container transition-colors"
            >
              記録を追加 <span className="material-symbols-outlined text-[14px]">add</span>
            </Link>
          </div>
        </div>
      </section>

      {/* クイックメニュー */}
      <section>
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900 mb-sm">クイックメニュー</h2>
        <div className="grid grid-cols-3 gap-sm">
          {[
            { to: '/hospitals', icon: 'local_hospital', label: '病院を探す' },
            { to: '/pets/new', icon: 'add_circle', label: 'ペット登録' },
            { to: '/notifications', icon: 'notifications', label: '通知' },
          ].map(({ to, icon, label }) => (
            <Link
              key={to}
              to={to}
              className="bg-surface-container-lowest rounded-xl p-md border border-neutral-gray-100 flex flex-col items-center gap-2 hover:shadow-[0px_4px_20px_rgba(0,0,0,0.05)] transition-shadow active:scale-[0.98]"
            >
              <div className="w-10 h-10 rounded-full bg-primary-container/10 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary">{icon}</span>
              </div>
              <span className="font-label-md text-label-md text-neutral-gray-900 text-center">{label}</span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  )
}
