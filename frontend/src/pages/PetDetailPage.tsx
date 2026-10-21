import { useEffect, useState } from 'react'
import { Link, useParams, useNavigate } from 'react-router-dom'
import { usePetStore } from '../stores/petStore'
import { vaccinationApi } from '../api/vaccination'
import { daysFromToday } from '../utils/date'
import type { Vaccination } from '../types'

export default function PetDetailPage() {
  const { petId } = useParams()
  const navigate = useNavigate()
  const { selectedPet, isLoading, fetchPetDetail, removePet } = usePetStore()

  // 予定（SCHEDULED）のうち接種予定日が近いもの。API は予定日の昇順で返す
  const [upcoming, setUpcoming] = useState<Vaccination[] | null>(null)
  // 取得失敗を「予定なし」と区別して表示し、再試行できるようにする
  const [upcomingFailed, setUpcomingFailed] = useState(false)
  const [upcomingReload, setUpcomingReload] = useState(0)

  useEffect(() => {
    if (petId) fetchPetDetail(Number(petId))
  }, [petId, fetchPetDetail])

  useEffect(() => {
    if (!petId) return
    let active = true
    setUpcoming(null)
    setUpcomingFailed(false)
    vaccinationApi
      .getList(Number(petId))
      .then((res) => active && setUpcoming((res.data ?? []).filter((v) => v.status === 'SCHEDULED').slice(0, 3)))
      .catch(() => active && setUpcomingFailed(true))
    return () => {
      active = false
    }
  }, [petId, upcomingReload])

  const handleDelete = async () => {
    if (!petId || !window.confirm('このペットを削除してもよろしいですか？')) return
    await removePet(Number(petId))
    navigate('/pets')
  }

  if (isLoading || !selectedPet) {
    return (
      <div className="flex items-center justify-center py-xl">
        <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
      </div>
    )
  }

  const pet = selectedPet

  return (
    <div className="pb-xl">
      {/* プロフィールヘッダー */}
      <div className="bg-surface-container-lowest px-container-margin pt-lg pb-xl flex flex-col items-center gap-md border-b border-neutral-gray-100">
        <div className="w-24 h-24 rounded-full bg-secondary-container flex items-center justify-center border-4 border-white shadow-lg">
          <span className="material-symbols-outlined text-secondary text-[44px]">pets</span>
        </div>
        <div className="text-center">
          <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">{pet.name}</h1>
          <p className="font-body-lg text-body-lg text-neutral-gray-600 mt-1">
            {pet.breed ?? '—'} · {pet.gender === 'MALE' ? 'オス' : 'メス'}
          </p>
        </div>
        <div className="flex gap-sm flex-wrap justify-center">
          <span className="inline-flex items-center gap-1 font-label-md text-label-md px-3 py-1.5 rounded-full border text-primary border-primary/20 bg-surface-bright">
            <span className="w-1.5 h-1.5 rounded-full bg-pet-green-vibrant" />
            健康良好
          </span>
          {pet.neutered && (
            <span className="inline-flex items-center gap-1 font-label-md text-label-md px-3 py-1.5 rounded-full border border-neutral-gray-100 bg-neutral-gray-50 text-neutral-gray-600">
              不妊手術済み
            </span>
          )}
        </div>
      </div>

      <div className="px-container-margin flex flex-col gap-lg pt-lg">
        {/* 基本情報 */}
        <section className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 overflow-hidden shadow-[0px_4px_20px_rgba(0,0,0,0.05)]">
          <div className="flex items-center justify-between px-container-margin py-md border-b border-neutral-gray-100">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">基本情報</h2>
            <button className="flex items-center gap-1 text-primary font-label-md text-label-md">
              <span className="material-symbols-outlined text-[16px]">edit</span>
              編集
            </button>
          </div>
          <div className="divide-y divide-neutral-gray-100">
            {[
              { label: '生年月日', value: pet.birthDate ?? '—' },
              { label: '体重', value: pet.weight ? `${pet.weight} kg` : '—' },
              { label: 'マイクロチップ', value: pet.microchipNumber ?? '未登録' },
            ].map(({ label, value }) => (
              <div key={label} className="flex justify-between items-center px-container-margin py-sm">
                <span className="font-label-md text-label-md text-neutral-gray-600">{label}</span>
                <span className="font-body-md text-body-md text-neutral-gray-900">{value}</span>
              </div>
            ))}
          </div>
        </section>

        {/* ワクチン予定（SC-006） */}
        <section className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 overflow-hidden shadow-[0px_4px_20px_rgba(0,0,0,0.05)]">
          <div className="flex items-center justify-between px-container-margin py-md border-b border-neutral-gray-100">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">ワクチン予定</h2>
            <Link to={`/pets/${petId}/vaccinations`} className="flex items-center gap-1 text-primary font-label-md text-label-md">
              すべて見る
              <span className="material-symbols-outlined text-[16px]">chevron_right</span>
            </Link>
          </div>
          {upcomingFailed ? (
            <div className="flex items-center justify-between gap-2 px-container-margin py-sm">
              <span className="font-body-md text-body-md text-error-red">ワクチン予定を取得できませんでした。</span>
              <button onClick={() => setUpcomingReload((n) => n + 1)} className="flex-shrink-0 font-label-md text-label-md text-primary px-2 py-1 rounded-lg hover:bg-primary/10">
                再試行
              </button>
            </div>
          ) : upcoming === null ? (
            <p className="px-container-margin py-sm font-body-md text-body-md text-neutral-gray-600">読み込み中…</p>
          ) : upcoming.length === 0 ? (
            <p className="px-container-margin py-sm font-body-md text-body-md text-neutral-gray-600">予定はありません。</p>
          ) : (
            <div className="divide-y divide-neutral-gray-100">
              {upcoming.map((v) => {
                const days = daysFromToday(v.scheduledDate)
                return (
                  <div key={v.vaccinationId} className="flex justify-between items-center px-container-margin py-sm">
                    <span className="font-body-md text-body-md text-neutral-gray-900">{v.vaccineName}</span>
                    <span className="font-label-md text-label-md text-neutral-gray-600">
                      {v.scheduledDate}
                      {days === 0 ? '（今日）' : days > 0 ? `（あと${days}日）` : ''}
                    </span>
                  </div>
                )
              })}
            </div>
          )}
        </section>

        {/* クイックアクション */}
        <section className="grid grid-cols-2 gap-sm">
          <Link
            to={`/pets/${petId}/health`}
            className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-md flex flex-col items-center gap-2 hover:shadow-sm transition-shadow"
          >
            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
              <span className="material-symbols-outlined text-primary text-[22px]">add_circle</span>
            </div>
            <span className="font-label-md text-label-md text-neutral-gray-900">健康記録を追加</span>
          </Link>
          <Link
            to="/hospitals"
            className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 p-md flex flex-col items-center gap-2 hover:shadow-sm transition-shadow"
          >
            <div className="w-10 h-10 rounded-full bg-error-red/10 flex items-center justify-center">
              <span className="material-symbols-outlined text-error-red text-[22px]">local_hospital</span>
            </div>
            <span className="font-label-md text-label-md text-neutral-gray-900">病院を予約</span>
          </Link>
        </section>

        {/* 削除ボタン */}
        <button
          onClick={handleDelete}
          className="w-full h-[44px] bg-error-red/10 text-error-red font-button-text text-button-text rounded-xl border border-error-red/20 hover:bg-error-red/20 transition-colors"
        >
          このペットを削除
        </button>
      </div>
    </div>
  )
}
