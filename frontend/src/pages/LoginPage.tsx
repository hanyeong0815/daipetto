import { useState, FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/auth'
import { useAuthStore } from '../stores/authStore'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setTokens, setUser } = useAuthStore()
  const [showPw, setShowPw] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setIsLoading(true)
    try {
      const res = await authApi.login({ email, password })
      if (!res.success || !res.data) {
        setError(res.message ?? 'ログインに失敗しました。')
        return
      }
      setTokens(res.data.accessToken, res.data.refreshToken)
      const meRes = await authApi.getMe()
      if (meRes.success && meRes.data) setUser(meRes.data)
      navigate('/dashboard')
    } catch {
      setError('メールアドレスまたはパスワードが正しくありません。')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="bg-surface min-h-screen flex flex-col items-center justify-center relative overflow-hidden">
      {/* 背景装飾 */}
      <div className="absolute -top-1/5 -right-1/10 w-[60vw] h-[60vw] max-w-[600px] max-h-[600px] bg-primary-container/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-1/5 -left-1/10 w-[50vw] h-[50vw] max-w-[500px] max-h-[500px] bg-tertiary-container/10 rounded-full blur-3xl pointer-events-none" />

      <main className="w-full max-w-[430px] px-container-margin z-10">
        {/* ヘッダー */}
        <header className="text-center mb-xl mt-lg">
          <h1 className="font-headline-lg-mobile text-headline-lg-mobile md:font-headline-lg md:text-headline-lg text-pet-green-vibrant mb-sm">
            Daipetto
          </h1>
          <p className="text-[17px] font-semibold tracking-wide text-neutral-gray-700 mt-1">
            大事なペットをもっと大切に。
          </p>
        </header>

        {/* ログインカード */}
        <div className="bg-surface-container-lowest rounded-[24px] shadow-[0px_4px_20px_rgba(0,0,0,0.05)] p-lg border border-neutral-gray-100">
          <form onSubmit={handleSubmit} className="space-y-lg">
            {/* エラーメッセージ */}
            {error && (
              <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
                <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
                {error}
              </div>
            )}

            {/* メールアドレス */}
            <div className="space-y-xs">
              <label htmlFor="email" className="block font-label-md text-label-md text-neutral-gray-900">
                メールアドレス
              </label>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 text-[20px]">
                  mail
                </span>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="your@email.com"
                  required
                  className="block w-full pl-10 h-[48px] rounded-lg bg-neutral-gray-50 border border-neutral-gray-100 text-on-surface focus:ring-2 focus:ring-primary focus:border-primary transition-colors font-body-md text-body-md"
                />
              </div>
            </div>

            {/* パスワード */}
            <div className="space-y-xs">
              <div className="flex justify-between items-center">
                <label htmlFor="password" className="block font-label-md text-label-md text-neutral-gray-900">
                  パスワード
                </label>
                <a href="#" className="font-label-md text-label-md text-primary hover:underline">
                  パスワードをお忘れですか？
                </a>
              </div>
              <div className="relative">
                <span className="material-symbols-outlined absolute left-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 text-[20px]">
                  lock
                </span>
                <input
                  id="password"
                  type={showPw ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="block w-full pl-10 pr-10 h-[48px] rounded-lg bg-neutral-gray-50 border border-neutral-gray-100 text-on-surface focus:ring-2 focus:ring-primary focus:border-primary transition-colors font-body-md text-body-md"
                />
                <button
                  type="button"
                  onClick={() => setShowPw((v) => !v)}
                  className="absolute right-sm top-1/2 -translate-y-1/2 text-neutral-gray-600 hover:text-primary"
                >
                  <span className="material-symbols-outlined text-[20px]">
                    {showPw ? 'visibility' : 'visibility_off'}
                  </span>
                </button>
              </div>
            </div>

            {/* ログインボタン */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full h-[52px] bg-pet-green-vibrant hover:bg-primary disabled:opacity-60 text-white rounded-xl font-button-text text-button-text flex items-center justify-center gap-2 transition-all duration-200 active:scale-[0.98] shadow-sm"
            >
              {isLoading ? (
                <span className="material-symbols-outlined animate-spin text-[20px]">progress_activity</span>
              ) : (
                <>
                  ログイン
                  <span className="material-symbols-outlined text-[20px]">arrow_forward</span>
                </>
              )}
            </button>
          </form>

          {/* 区切り線 */}
          <div className="my-lg flex items-center">
            <div className="flex-grow border-t border-neutral-gray-100" />
            <span className="mx-sm font-label-md text-label-md text-neutral-gray-600">または</span>
            <div className="flex-grow border-t border-neutral-gray-100" />
          </div>

          {/* ソーシャルログイン */}
          <div className="space-y-sm">
            <button
              type="button"
              className="w-full h-[48px] bg-white border border-neutral-gray-100 rounded-lg font-button-text text-button-text text-neutral-gray-900 flex items-center justify-center gap-3 hover:bg-neutral-gray-50 active:scale-[0.98] transition-all"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
              </svg>
              Googleでログイン
            </button>
            <button
              type="button"
              className="w-full h-[48px] bg-[#06C755] hover:bg-[#05b04b] text-white border border-[#06C755] rounded-lg font-button-text text-button-text flex items-center justify-center gap-3 active:scale-[0.98] transition-all"
            >
              LINEでログイン
            </button>
          </div>
        </div>

        {/* 会員登録リンク */}
        <div className="mt-lg text-center mb-xl">
          <p className="font-body-md text-body-md text-on-surface-variant">
            アカウントをお持ちでないですか？{' '}
            <Link to="/register" className="font-button-text text-button-text text-primary hover:underline">
              会員登録
            </Link>
          </p>
        </div>
      </main>
    </div>
  )
}
