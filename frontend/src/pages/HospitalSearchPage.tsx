import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useHospitalStore } from '../stores/hospitalStore'

export default function HospitalSearchPage() {
  const [query, setQuery] = useState('')
  const { hospitals, isLoading, fetchHospitals } = useHospitalStore()

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchHospitals(query || undefined)
    }, 300)
    return () => clearTimeout(timer)
  }, [query, fetchHospitals])

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">病院を探す</h1>

      <div className="relative">
        <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600">search</span>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="病院名または地域で検索"
          className="w-full h-[52px] pl-10 pr-sm bg-surface-container-lowest border border-neutral-gray-100 rounded-xl font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all shadow-[0px_2px_8px_rgba(0,0,0,0.04)]"
        />
        {query && (
          <button onClick={() => setQuery('')} className="absolute right-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 hover:text-on-surface">
            <span className="material-symbols-outlined">close</span>
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      ) : (
        <div className="flex flex-col gap-sm">
          <p className="font-label-md text-label-md text-neutral-gray-600">{hospitals.length}件の病院が見つかりました</p>
          {hospitals.map((h) => (
            <Link
              key={h.id}
              to={`/hospitals/${h.id}`}
              className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-sm shadow-[0px_4px_20px_rgba(0,0,0,0.05)] active:scale-[0.99] transition-transform"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex-grow min-w-0">
                  <h2 className="font-headline-md text-headline-md text-neutral-gray-900">{h.name}</h2>
                  <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5 truncate">{h.address}</p>
                </div>
              </div>
              {h.phoneNumber && (
                <div className="flex items-center gap-1 text-neutral-gray-600">
                  <span className="material-symbols-outlined text-[16px]">phone</span>
                  <span className="font-label-md text-label-md">{h.phoneNumber}</span>
                </div>
              )}
              <div className="flex items-center justify-end">
                <span className="font-label-md text-label-md text-primary flex items-center gap-0.5">
                  詳細を見る <span className="material-symbols-outlined text-[16px]">chevron_right</span>
                </span>
              </div>
            </Link>
          ))}
          {hospitals.length === 0 && (
            <p className="text-center py-xl font-body-md text-body-md text-neutral-gray-600">該当する病院が見つかりませんでした。</p>
          )}
        </div>
      )}
    </div>
  )
}
