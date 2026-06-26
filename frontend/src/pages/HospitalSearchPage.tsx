import { useState } from 'react'
import { Link } from 'react-router-dom'

const hospitals = [
  {
    id: 1,
    name: '幸せ動物病院',
    address: '東京都渋谷区道玄坂1-2-3',
    distance: '0.5km',
    rating: 4.8,
    reviews: 124,
    specialty: ['内科', '外科', '皮膚科'],
    hours: '09:00 - 20:00',
    open: true,
  },
  {
    id: 2,
    name: '愛情動物クリニック',
    address: '東京都渋谷区恵比寿4-5-6',
    distance: '1.2km',
    rating: 4.5,
    reviews: 87,
    specialty: ['内科', '歯科'],
    hours: '10:00 - 19:00',
    open: true,
  },
  {
    id: 3,
    name: '笑顔動物病院',
    address: '東京都渋谷区代官山7-8-9',
    distance: '2.1km',
    rating: 4.2,
    reviews: 45,
    specialty: ['内科', '眼科', '救急'],
    hours: '09:00 - 21:00',
    open: false,
  },
]

const specialties = ['内科', '外科', '皮膚科', '歯科', '眼科', '救急']

export default function HospitalSearchPage() {
  const [query, setQuery] = useState('')
  const [selectedSpecialty, setSelectedSpecialty] = useState<string | null>(null)

  const filtered = hospitals.filter((h) => {
    const matchQuery = h.name.includes(query) || h.address.includes(query)
    const matchSpecialty = !selectedSpecialty || h.specialty.includes(selectedSpecialty)
    return matchQuery && matchSpecialty
  })

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

      <div className="flex gap-2 overflow-x-auto pb-1 hide-scrollbar">
        <button
          onClick={() => setSelectedSpecialty(null)}
          className={`flex-shrink-0 px-4 py-2 rounded-full font-label-md text-label-md border transition-all ${
            !selectedSpecialty ? 'bg-pet-green-vibrant text-white border-pet-green-vibrant' : 'bg-surface-container-lowest text-neutral-gray-600 border-neutral-gray-100'
          }`}
        >
          すべて
        </button>
        {specialties.map((s) => (
          <button
            key={s}
            onClick={() => setSelectedSpecialty(selectedSpecialty === s ? null : s)}
            className={`flex-shrink-0 px-4 py-2 rounded-full font-label-md text-label-md border transition-all ${
              selectedSpecialty === s ? 'bg-pet-green-vibrant text-white border-pet-green-vibrant' : 'bg-surface-container-lowest text-neutral-gray-600 border-neutral-gray-100'
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      <div className="flex flex-col gap-sm">
        <p className="font-label-md text-label-md text-neutral-gray-600">{filtered.length}件の病院が見つかりました</p>
        {filtered.map((h) => (
          <Link
            key={h.id}
            to={`/hospitals/${h.id}`}
            className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 p-container-margin flex flex-col gap-sm shadow-[0px_4px_20px_rgba(0,0,0,0.05)] active:scale-[0.99] transition-transform"
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex-grow min-w-0">
                <div className="flex items-center gap-2">
                  <h2 className="font-headline-md text-headline-md text-neutral-gray-900">{h.name}</h2>
                  <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full flex-shrink-0 ${h.open ? 'bg-pet-green-vibrant/10 text-primary' : 'bg-neutral-gray-100 text-neutral-gray-600'}`}>
                    {h.open ? '営業中' : '営業終了'}
                  </span>
                </div>
                <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5 truncate">{h.address}</p>
              </div>
              <span className="font-label-md text-label-md text-neutral-gray-600 flex-shrink-0 bg-neutral-gray-50 px-2 py-1 rounded-lg border border-neutral-gray-100">{h.distance}</span>
            </div>
            <div className="flex items-center gap-1">
              <span className="material-symbols-outlined text-warning-yellow icon-fill text-[16px]">star</span>
              <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">{h.rating}</span>
              <span className="font-label-md text-label-md text-neutral-gray-600">（{h.reviews}件のレビュー）</span>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              {h.specialty.map((s) => (
                <span key={s} className="font-label-md text-label-md px-2.5 py-1 rounded-full bg-neutral-gray-50 border border-neutral-gray-100 text-neutral-gray-600">{s}</span>
              ))}
            </div>
            <div className="flex items-center justify-between text-neutral-gray-600">
              <div className="flex items-center gap-1">
                <span className="material-symbols-outlined text-[16px]">schedule</span>
                <span className="font-label-md text-label-md">{h.hours}</span>
              </div>
              <span className="font-label-md text-label-md text-primary flex items-center gap-0.5">
                詳細を見る <span className="material-symbols-outlined text-[16px]">chevron_right</span>
              </span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
