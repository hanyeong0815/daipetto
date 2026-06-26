import React, { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { FaDog, FaCat, FaPaw } from 'react-icons/fa6'
import { petApi } from '../api/pet'
import { usePetStore } from '../stores/petStore'
import type { PetSpecies, PetGender } from '../types'

export default function PetRegisterPage() {
  const navigate = useNavigate()
  const fetchPets = usePetStore((s) => s.fetchPets)
  const [species, setSpecies] = useState<PetSpecies>('DOG')
  const [gender, setGender] = useState<PetGender>('MALE')
  const [name, setName] = useState('')
  const [breed, setBreed] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [weight, setWeight] = useState('')
  const [microchip, setMicrochip] = useState('')
  const [neutered, setNeutered] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setIsLoading(true)
    try {
      const res = await petApi.create({
        name,
        species,
        breed: breed || undefined,
        birthDate: birthDate || undefined,
        gender,
        weight: weight ? parseFloat(weight) : undefined,
        neutered,
        microchipNumber: microchip || undefined,
      })
      if (!res.success) {
        setError(res.message ?? 'ペットの登録に失敗しました。')
        return
      }
      await fetchPets()
      navigate('/pets')
    } catch {
      setError('ペットの登録に失敗しました。もう一度お試しください。')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="px-container-margin py-lg pb-xl flex flex-col gap-lg">
      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {/* 写真アップロード */}
      <section className="flex flex-col items-center gap-sm">
        <div className="relative">
          <div className="w-24 h-24 rounded-full bg-surface-container-low flex items-center justify-center border-2 border-dashed border-neutral-gray-100 overflow-hidden">
            <span className="material-symbols-outlined text-neutral-gray-600 text-[36px]">add_a_photo</span>
          </div>
          <button type="button" className="absolute bottom-0 right-0 w-7 h-7 bg-pet-green-vibrant rounded-full flex items-center justify-center shadow-md">
            <span className="material-symbols-outlined text-white text-[16px]">add</span>
          </button>
        </div>
        <p className="font-label-md text-label-md text-neutral-gray-600">写真を追加（任意）</p>
      </section>

      {/* 種類選択 */}
      <section className="flex flex-col gap-xs">
        <label className="font-label-md text-label-md text-on-surface-variant">ペットの種類</label>
        <div className="grid grid-cols-3 gap-sm">
          {([
            { value: 'DOG', label: '犬', Icon: FaDog },
            { value: 'CAT', label: '猫', Icon: FaCat },
            { value: 'OTHER', label: 'その他', Icon: FaPaw },
          ] as { value: PetSpecies; label: string; Icon: React.ComponentType<{ size?: number }> }[]).map(({ value, label, Icon }) => (
            <button
              key={value}
              type="button"
              onClick={() => setSpecies(value)}
              className={`flex flex-col items-center gap-2 py-4 rounded-xl border-2 transition-all ${
                species === value
                  ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary'
                  : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-600'
              }`}
            >
              <Icon size={28} />
              <span className="font-button-text text-button-text">{label}</span>
            </button>
          ))}
        </div>
      </section>

      {/* 基本情報 */}
      <section className="flex flex-col gap-md">
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900">基本情報</h2>

        <div className="flex flex-col gap-xs">
          <label className="font-label-md text-label-md text-on-surface-variant">名前 <span className="text-error-red">*</span></label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="ペットの名前"
            required
            className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
          />
        </div>

        <div className="flex flex-col gap-xs">
          <label className="font-label-md text-label-md text-on-surface-variant">品種</label>
          <input
            type="text"
            value={breed}
            onChange={(e) => setBreed(e.target.value)}
            placeholder="例：ゴールデン・レトリバー"
            className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
          />
        </div>

        <div className="flex flex-col gap-xs">
          <label className="font-label-md text-label-md text-on-surface-variant">生年月日</label>
          <input
            type="date"
            value={birthDate}
            onChange={(e) => setBirthDate(e.target.value)}
            className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
          />
        </div>

        <div className="flex flex-col gap-xs">
          <label className="font-label-md text-label-md text-on-surface-variant">性別</label>
          <div className="grid grid-cols-2 gap-sm">
            {([
              { value: 'MALE', label: 'オス' },
              { value: 'FEMALE', label: 'メス' },
            ] as { value: PetGender; label: string }[]).map(({ value, label }) => (
              <button
                key={value}
                type="button"
                onClick={() => setGender(value)}
                className={`flex items-center justify-center gap-2 h-[48px] rounded-lg border-2 font-button-text text-button-text transition-all ${
                  gender === value
                    ? 'border-pet-green-vibrant bg-pet-green-vibrant/5 text-primary'
                    : 'border-neutral-gray-100 bg-surface-container-lowest text-neutral-gray-600'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">{value === 'MALE' ? 'male' : 'female'}</span>
                {label}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-xs">
          <label className="font-label-md text-label-md text-on-surface-variant">体重（kg）</label>
          <div className="relative">
            <input
              type="number"
              step="0.1"
              min="0"
              max="200"
              value={weight}
              onChange={(e) => setWeight(e.target.value)}
              placeholder="0.0"
              className="w-full h-[48px] px-sm pr-14 bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
            />
            <span className="absolute right-sm top-1/2 -translate-y-1/2 font-body-md text-body-md text-neutral-gray-600">kg</span>
          </div>
        </div>
      </section>

      {/* 追加情報 */}
      <section className="flex flex-col gap-md">
        <h2 className="font-headline-md text-headline-md text-neutral-gray-900">追加情報</h2>

        <label className="flex items-center justify-between py-sm px-container-margin bg-surface-container-lowest rounded-xl border border-neutral-gray-100 cursor-pointer">
          <div>
            <p className="font-body-lg text-body-lg text-neutral-gray-900">不妊手術の有無</p>
            <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">完了している場合はチェックしてください</p>
          </div>
          <div
            onClick={() => setNeutered((v) => !v)}
            className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${neutered ? 'bg-pet-green-vibrant' : 'bg-neutral-gray-100'}`}
          >
            <span className={`absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform ${neutered ? 'translate-x-7' : 'translate-x-1'}`} />
          </div>
        </label>

        <div className="flex flex-col gap-xs">
          <label className="font-label-md text-label-md text-on-surface-variant">マイクロチップ番号（任意）</label>
          <input
            type="text"
            value={microchip}
            onChange={(e) => setMicrochip(e.target.value)}
            placeholder="15桁の番号を入力"
            maxLength={15}
            className="w-full h-[48px] px-sm bg-neutral-gray-50 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
          />
        </div>
      </section>

      <button
        type="submit"
        disabled={isLoading}
        className="w-full h-[52px] bg-pet-green-vibrant hover:bg-primary disabled:opacity-60 text-white font-button-text text-button-text rounded-xl flex items-center justify-center gap-2 transition-all active:scale-[0.98] shadow-[0px_4px_12px_rgba(46,204,113,0.2)] mt-sm"
      >
        {isLoading ? (
          <span className="material-symbols-outlined animate-spin text-[20px]">progress_activity</span>
        ) : (
          <>
            <span className="material-symbols-outlined">check</span>
            登録完了
          </>
        )}
      </button>
    </form>
  )
}
