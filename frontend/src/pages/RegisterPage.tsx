import { useState, FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [showPw, setShowPw] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPw, setConfirmPw] = useState('')
  const [nickname, setNickname] = useState('')
  const [allChecked, setAllChecked] = useState(false)
  const [termsChecked, setTermsChecked] = useState(false)
  const [privacyChecked, setPrivacyChecked] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const toggleAll = () => {
    const next = !allChecked
    setAllChecked(next)
    setTermsChecked(next)
    setPrivacyChecked(next)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (password !== confirmPw) {
      setError('パスワードが一致しません。')
      return
    }
    if (!termsChecked || !privacyChecked) {
      setError('必須項目に同意してください。')
      return
    }
    setIsLoading(true)
    try {
      const res = await authApi.register({ email, password, nickname })
      if (!res.success) {
        setError(res.message ?? '会員登録に失敗しました。')
        return
      }
      navigate('/login')
    } catch {
      setError('会員登録に失敗しました。もう一度お試しください。')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="bg-surface-container-lowest min-h-screen flex flex-col font-body-md w-full max-w-[430px] mx-auto shadow-2xl">
      <main className="w-full px-container-margin py-xl flex-1 flex flex-col">
        {/* ヘッダー */}
        <div className="text-center mb-lg mt-md">
          <h1 className="font-headline-lg-mobile text-headline-lg-mobile font-bold text-pet-green-vibrant mb-xs">
            Daipetto
          </h1>
          <p className="text-[17px] font-semibold tracking-wide text-neutral-gray-700">
            大事なペットをもっと大切に。
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-lg flex-1">
          {/* エラーメッセージ */}
          {error && (
            <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
              <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
              {error}
            </div>
          )}

          {/* メールアドレス */}
          <div className="space-y-xs">
            <label htmlFor="email" className="block font-label-md text-label-md text-on-surface-variant">
              メールアドレス
            </label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 pointer-events-none">mail</span>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@daipetto.com"
                required
                className="w-full h-[48px] pl-10 pr-4 bg-neutral-gray-100 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
              />
            </div>
          </div>

          {/* パスワード */}
          <div className="space-y-xs">
            <label htmlFor="password" className="block font-label-md text-label-md text-on-surface-variant">
              パスワード
            </label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 pointer-events-none">lock</span>
              <input
                id="password"
                type={showPw ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="英数字8文字以上"
                required
                className="w-full h-[48px] pl-10 pr-10 bg-neutral-gray-100 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
              />
              <button
                type="button"
                onClick={() => setShowPw((v) => !v)}
                className="absolute right-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 hover:text-on-surface"
              >
                <span className="material-symbols-outlined">{showPw ? 'visibility' : 'visibility_off'}</span>
              </button>
            </div>
          </div>

          {/* パスワード確認 */}
          <div className="space-y-xs">
            <label htmlFor="confirmPw" className="block font-label-md text-label-md text-on-surface-variant">
              パスワード確認
            </label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 pointer-events-none">lock_clock</span>
              <input
                id="confirmPw"
                type="password"
                value={confirmPw}
                onChange={(e) => setConfirmPw(e.target.value)}
                placeholder="パスワードをもう一度入力してください"
                required
                className="w-full h-[48px] pl-10 pr-4 bg-neutral-gray-100 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
              />
            </div>
          </div>

          {/* ニックネーム */}
          <div className="space-y-xs">
            <label htmlFor="nickname" className="block font-label-md text-label-md text-on-surface-variant">
              ニックネーム
            </label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 pointer-events-none">person</span>
              <input
                id="nickname"
                type="text"
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="使用するニックネームを入力してください"
                required
                className="w-full h-[48px] pl-10 pr-4 bg-neutral-gray-100 border border-neutral-gray-100 rounded-lg font-body-md text-body-md text-on-surface placeholder:text-neutral-gray-600 focus:outline-none focus:ring-2 focus:ring-pet-green-vibrant transition-all"
              />
            </div>
          </div>

          {/* 利用規約 */}
          <div className="bg-surface-container-low p-4 rounded-xl border border-neutral-gray-100 mt-xl space-y-xs">
            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={allChecked}
                onChange={toggleAll}
                className="mt-0.5 w-5 h-5 rounded accent-pet-green-vibrant cursor-pointer"
              />
              <span className="font-body-md text-body-md text-on-surface font-semibold">すべてに同意する</span>
            </label>
            <div className="pl-8 space-y-2">
              {[
                { id: 'terms', label: '利用規約への同意（必須）', checked: termsChecked, onChange: setTermsChecked },
                { id: 'privacy', label: 'プライバシーポリシーへの同意（必須）', checked: privacyChecked, onChange: setPrivacyChecked },
              ].map(({ id, label, checked, onChange }) => (
                <label key={id} className="flex items-center justify-between cursor-pointer">
                  <div className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={(e) => onChange(e.target.checked)}
                      className="w-4 h-4 rounded accent-pet-green-vibrant cursor-pointer"
                    />
                    <span className="font-body-md text-body-md text-on-surface-variant">{label}</span>
                  </div>
                  <button type="button" className="text-neutral-gray-600 hover:text-pet-green-vibrant">
                    <span className="material-symbols-outlined text-[18px]">chevron_right</span>
                  </button>
                </label>
              ))}
            </div>
          </div>

          {/* 登録ボタン */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full h-[52px] bg-pet-green-vibrant hover:bg-pet-green-dark disabled:opacity-60 text-white font-button-text text-button-text rounded-lg active:scale-[0.98] transition-all duration-200 mt-xl flex items-center justify-center gap-2 shadow-[0px_4px_20px_rgba(46,204,113,0.2)]"
          >
            {isLoading ? (
              <span className="material-symbols-outlined animate-spin text-[20px]">progress_activity</span>
            ) : (
              <>
                会員登録を完了する
                <span className="material-symbols-outlined">arrow_forward</span>
              </>
            )}
          </button>

          {/* 区切り線 */}
          <div className="relative flex items-center py-4">
            <div className="flex-grow border-t border-outline-variant" />
            <span className="mx-4 text-neutral-gray-600 font-label-md text-label-md">または</span>
            <div className="flex-grow border-t border-outline-variant" />
          </div>

          {/* ソーシャル登録 */}
          <div className="space-y-sm mb-lg">
            <button
              type="button"
              className="w-full h-[48px] bg-white border border-outline-variant hover:bg-neutral-gray-50 text-on-surface font-button-text text-button-text rounded-lg active:scale-[0.98] transition-all flex items-center justify-center gap-3"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
              </svg>
              Googleで登録
            </button>
            <button
              type="button"
              className="w-full h-[48px] bg-[#06C755] hover:bg-[#05b04b] text-white font-button-text text-button-text rounded-lg active:scale-[0.98] transition-all flex items-center justify-center gap-3"
            >
              LINEで登録
            </button>
          </div>
        </form>

        {/* ログインリンク */}
        <div className="mt-lg text-center pb-lg">
          <p className="font-body-md text-body-md text-on-surface-variant">
            すでにアカウントをお持ちですか？{' '}
            <Link to="/login" className="text-pet-green-vibrant font-semibold hover:underline">
              ログイン
            </Link>
          </p>
        </div>
      </main>
    </div>
  )
}
