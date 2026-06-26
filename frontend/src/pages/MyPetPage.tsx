import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { usePetStore } from '../stores/petStore'

export default function MyPetPage() {
  const { pets, isLoading, fetchPets } = usePetStore()

  useEffect(() => {
    fetchPets()
  }, [fetchPets])

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-xl">
        <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
      </div>
    )
  }

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      <div className="flex justify-between items-center">
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">
          マイペット
        </h1>
        <Link
          to="/pets/new"
          className="flex items-center gap-1 bg-pet-green-vibrant text-white font-button-text text-button-text px-4 py-2.5 rounded-xl hover:bg-primary transition-colors active:scale-[0.98] shadow-[0px_4px_12px_rgba(46,204,113,0.2)]"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          ペット登録
        </Link>
      </div>

      {pets.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-xl gap-md text-center mt-xl">
          <div className="w-20 h-20 rounded-full bg-surface-container-low flex items-center justify-center">
            <span className="material-symbols-outlined text-neutral-gray-600 text-[40px]">pets</span>
          </div>
          <div>
            <p className="font-headline-md text-headline-md text-neutral-gray-900 mb-xs">
              まだペットが登録されていません
            </p>
            <p className="font-body-md text-body-md text-neutral-gray-600">
              大切なペットを登録して健康を管理しましょう。
            </p>
          </div>
          <Link
            to="/pets/new"
            className="flex items-center gap-2 bg-pet-green-vibrant text-white font-button-text text-button-text px-6 py-3 rounded-xl hover:bg-primary transition-colors"
          >
            <span className="material-symbols-outlined">add</span>
            最初のペットを登録
          </Link>
        </div>
      ) : (
        <div className="flex flex-col gap-sm">
          {pets.map((pet) => (
            <Link
              key={pet.id}
              to={`/pets/${pet.id}`}
              className="bg-surface-container-lowest rounded-[16px] p-container-margin shadow-[0px_4px_20px_rgba(0,0,0,0.05)] border border-neutral-gray-100 flex items-center gap-4 active:scale-[0.99] transition-transform"
            >
              <div className="w-16 h-16 rounded-full bg-secondary-container flex-shrink-0 flex items-center justify-center border-2 border-surface-container-low">
                <span className="material-symbols-outlined text-secondary text-[28px]">pets</span>
              </div>
              <div className="flex-grow min-w-0">
                <div className="flex items-center justify-between">
                  <h2 className="font-headline-md text-headline-md text-neutral-gray-900">{pet.name}</h2>
                  <span className="material-symbols-outlined text-neutral-gray-600">chevron_right</span>
                </div>
                <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5 truncate">
                  {pet.breed ?? '—'} · {pet.gender === 'MALE' ? 'オス' : 'メス'}
                </p>
                <div className="flex items-center gap-2 mt-3">
                  <span className="inline-flex items-center gap-1 text-primary border border-primary/20 bg-surface-bright font-label-md text-label-md px-2.5 py-1 rounded-full">
                    <span className="w-1.5 h-1.5 rounded-full bg-pet-green-vibrant" />
                    健康良好
                  </span>
                  {pet.weight && (
                    <span className="font-label-md text-label-md text-neutral-gray-600">{pet.weight} kg</span>
                  )}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
