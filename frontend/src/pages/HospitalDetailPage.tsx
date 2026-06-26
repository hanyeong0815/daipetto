import { Link, useParams } from 'react-router-dom'

const mockHospital = {
  id: 1,
  name: '幸せ動物病院',
  address: '東京都渋谷区道玄坂1-2-3',
  phone: '03-1234-5678',
  distance: '0.5km',
  rating: 4.8,
  reviews: 124,
  specialty: ['内科', '外科', '皮膚科'],
  hours: { weekday: '09:00 - 20:00', saturday: '09:00 - 17:00', sunday: '休診' },
  open: true,
  doctors: [
    { name: '田中 誠', role: '院長', specialty: '内科', experience: '10年' },
    { name: '佐藤 美咲', role: '獣医師', specialty: '皮膚科', experience: '5年' },
  ],
  reviews_list: [
    { author: 'ボリパパ', rating: 5, date: '2024-10-15', comment: '丁寧に診察してくださいました。待ち時間も短かったです。' },
    { author: 'ナビママ', rating: 4, date: '2024-10-10', comment: '施設が清潔で先生も優しかったです。' },
  ],
}

export default function HospitalDetailPage() {
  const { hospitalId } = useParams()

  return (
    <div className="pb-24">
      <div className="bg-surface-container-lowest px-container-margin pt-lg pb-xl border-b border-neutral-gray-100">
        <div className="flex items-start gap-4">
          <div className="w-16 h-16 rounded-xl bg-error-red/10 flex items-center justify-center flex-shrink-0">
            <span className="material-symbols-outlined text-error-red icon-fill text-[32px]">local_hospital</span>
          </div>
          <div className="flex-grow min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">{mockHospital.name}</h1>
              <span className={`font-label-md text-label-md px-2 py-0.5 rounded-full flex-shrink-0 ${mockHospital.open ? 'bg-pet-green-vibrant/10 text-primary' : 'bg-neutral-gray-100 text-neutral-gray-600'}`}>
                {mockHospital.open ? '営業中' : '営業終了'}
              </span>
            </div>
            <p className="font-body-md text-body-md text-neutral-gray-600 mt-1">{mockHospital.address}</p>
            <div className="flex items-center gap-1 mt-2">
              <span className="material-symbols-outlined text-warning-yellow icon-fill text-[16px]">star</span>
              <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">{mockHospital.rating}</span>
              <span className="font-label-md text-label-md text-neutral-gray-600">（{mockHospital.reviews}件のレビュー）</span>
              <span className="text-neutral-gray-100 mx-1">·</span>
              <span className="font-label-md text-label-md text-neutral-gray-600">{mockHospital.distance}</span>
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-sm mt-lg">
          <a href={`tel:${mockHospital.phone}`} className="flex items-center justify-center gap-2 h-[44px] bg-surface-container-low rounded-xl font-button-text text-button-text text-on-surface border border-neutral-gray-100 hover:bg-surface-container transition-colors">
            <span className="material-symbols-outlined text-[18px]">phone</span>
            電話する
          </a>
          <button className="flex items-center justify-center gap-2 h-[44px] bg-surface-container-low rounded-xl font-button-text text-button-text text-on-surface border border-neutral-gray-100 hover:bg-surface-container transition-colors">
            <span className="material-symbols-outlined text-[18px]">share</span>
            共有
          </button>
        </div>
      </div>

      <div className="px-container-margin flex flex-col gap-lg pt-lg">
        <section>
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900 mb-sm">専門分野</h2>
          <div className="flex gap-2 flex-wrap">
            {mockHospital.specialty.map((s) => (
              <span key={s} className="font-label-md text-label-md px-3 py-1.5 rounded-full bg-primary/10 text-primary border border-primary/20">{s}</span>
            ))}
          </div>
        </section>

        <section className="bg-surface-container-lowest rounded-[16px] border border-neutral-gray-100 overflow-hidden shadow-[0px_4px_20px_rgba(0,0,0,0.05)]">
          <div className="px-container-margin py-md border-b border-neutral-gray-100">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">診療時間</h2>
          </div>
          <div className="divide-y divide-neutral-gray-100">
            {[
              { label: '平日（月〜金）', value: mockHospital.hours.weekday },
              { label: '土曜日', value: mockHospital.hours.saturday },
              { label: '日曜日・祝日', value: mockHospital.hours.sunday },
            ].map(({ label, value }) => (
              <div key={label} className="flex justify-between items-center px-container-margin py-sm">
                <span className="font-label-md text-label-md text-neutral-gray-600">{label}</span>
                <span className={`font-body-md text-body-md ${value === '休診' ? 'text-error-red' : 'text-neutral-gray-900'}`}>{value}</span>
              </div>
            ))}
          </div>
        </section>

        <section>
          <h2 className="font-headline-md text-headline-md text-neutral-gray-900 mb-sm">医療スタッフ</h2>
          <div className="flex flex-col gap-sm">
            {mockHospital.doctors.map((d) => (
              <div key={d.name} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 px-container-margin py-md flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-secondary text-[20px]">person</span>
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-body-lg text-body-lg text-neutral-gray-900 font-medium">{d.name}</p>
                    <span className="font-label-md text-label-md text-neutral-gray-600 bg-neutral-gray-50 px-2 py-0.5 rounded-full border border-neutral-gray-100">{d.role}</span>
                  </div>
                  <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">{d.specialty} · 経歴 {d.experience}</p>
                </div>
              </div>
            ))}
          </div>
        </section>

        <section>
          <div className="flex items-center justify-between mb-sm">
            <h2 className="font-headline-md text-headline-md text-neutral-gray-900">レビュー</h2>
            <div className="flex items-center gap-1">
              <span className="material-symbols-outlined text-warning-yellow icon-fill text-[18px]">star</span>
              <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">{mockHospital.rating}</span>
            </div>
          </div>
          <div className="flex flex-col gap-sm">
            {mockHospital.reviews_list.map((r) => (
              <div key={`${r.author}-${r.date}`} className="bg-surface-container-lowest rounded-xl border border-neutral-gray-100 px-container-margin py-md">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-full bg-secondary-container flex items-center justify-center">
                      <span className="material-symbols-outlined text-secondary text-[14px]">person</span>
                    </div>
                    <span className="font-body-md text-body-md text-neutral-gray-900 font-medium">{r.author}</span>
                  </div>
                  <div className="flex items-center gap-0.5">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <span key={i} className={`material-symbols-outlined text-[14px] ${i < r.rating ? 'text-warning-yellow icon-fill' : 'text-neutral-gray-100'}`}>star</span>
                    ))}
                  </div>
                </div>
                <p className="font-body-md text-body-md text-neutral-gray-900">{r.comment}</p>
                <p className="font-label-md text-label-md text-neutral-gray-600 mt-1">{r.date}</p>
              </div>
            ))}
          </div>
        </section>
      </div>

      <div className="fixed bottom-0 left-0 right-0 px-container-margin pb-lg pt-sm bg-surface-container-lowest border-t border-neutral-gray-100 max-w-[800px] mx-auto">
        <Link
          to={`/hospitals/${hospitalId}/reserve`}
          className="w-full h-[52px] bg-pet-green-vibrant hover:bg-primary text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] shadow-[0px_4px_12px_rgba(46,204,113,0.2)]"
        >
          <span className="material-symbols-outlined">calendar_add_on</span>
          予約する
        </Link>
      </div>
    </div>
  )
}
