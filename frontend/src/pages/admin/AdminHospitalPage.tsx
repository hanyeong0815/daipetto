import { useState } from 'react'

interface HospitalInfo {
  name: string
  address: string
  phone: string
  hours: { weekday: string; saturday: string; sunday: string }
  specialty: string[]
}

const initialInfo: HospitalInfo = {
  name: '幸せ動物病院',
  address: '東京都渋谷区道玄坂1-2-3',
  phone: '03-1234-5678',
  hours: { weekday: '09:00 - 20:00', saturday: '09:00 - 17:00', sunday: '休診' },
  specialty: ['内科', '外科', '皮膚科'],
}

const allSpecialties = ['内科', '外科', '皮膚科', '歯科', '眼科', '救急', '整形外科', '神経科']

export default function AdminHospitalPage() {
  const [info, setInfo] = useState(initialInfo)
  const [saved, setSaved] = useState(false)

  const toggleSpecialty = (s: string) => {
    setInfo((prev) => ({
      ...prev,
      specialty: prev.specialty.includes(s) ? prev.specialty.filter((x) => x !== s) : [...prev.specialty, s],
    }))
  }

  const handleSave = () => {
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      <div>
        <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">病院情報管理</h1>
        <p className="font-body-md text-body-md text-neutral-gray-600 mt-0.5">登録されている病院情報を編集できます。</p>
      </div>

      <section className="flex flex-col gap-md">
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900">基本情報</h2>
        {[
          { label: '病院名', key: 'name' as keyof HospitalInfo, type: 'text' },
          { label: '住所', key: 'address' as keyof HospitalInfo, type: 'text' },
          { label: '電話番号', key: 'phone' as keyof HospitalInfo, type: 'tel' },
        ].map(({ label, key, type }) => (
          <div key={key} className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">{label}</label>
            <input type={type} value={info[key] as string} onChange={(e) => setInfo((prev) => ({ ...prev, [key]: e.target.value }))}
              className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all" />
          </div>
        ))}
      </section>

      <section className="flex flex-col gap-md">
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900">診療時間</h2>
        {[
          { label: '平日（月〜金）', key: 'weekday' as keyof typeof info.hours },
          { label: '土曜日', key: 'saturday' as keyof typeof info.hours },
          { label: '日曜日・祝日', key: 'sunday' as keyof typeof info.hours },
        ].map(({ label, key }) => (
          <div key={key} className="flex flex-col gap-xs">
            <label className="font-label-md text-label-md text-on-surface-variant">{label}</label>
            <input type="text" value={info.hours[key]} onChange={(e) => setInfo((prev) => ({ ...prev, hours: { ...prev.hours, [key]: e.target.value } }))}
              placeholder="例：09:00 - 18:00（休診の場合は「休診」と入力）"
              className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all" />
          </div>
        ))}
      </section>

      <section className="flex flex-col gap-sm">
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900">専門分野</h2>
        <div className="flex flex-wrap gap-sm">
          {allSpecialties.map((s) => (
            <button key={s} type="button" onClick={() => toggleSpecialty(s)}
              className={`px-4 py-2 rounded-full font-label-md text-label-md border-2 transition-all ${info.specialty.includes(s) ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary' : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-600'}`}>
              {s}
            </button>
          ))}
        </div>
      </section>

      <button onClick={handleSave}
        className={`w-full h-[52px] font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] ${saved ? 'bg-success-blue text-white' : 'bg-pet-green-vibrant text-white hover:bg-primary shadow-[0px_4px_12px_rgba(46,204,113,0.2)]'}`}>
        <span className="material-symbols-outlined">{saved ? 'check_circle' : 'save'}</span>
        {saved ? '保存しました！' : '変更を保存する'}
      </button>
    </div>
  )
}
