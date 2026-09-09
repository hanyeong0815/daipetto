import { useEffect, useRef, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './components/layout/MainLayout'
import DetailLayout from './components/layout/DetailLayout'
import ProtectedRoute from './components/auth/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import MyPetPage from './pages/MyPetPage'
import PetRegisterPage from './pages/PetRegisterPage'
import PetDetailPage from './pages/PetDetailPage'
import HealthRecordPage from './pages/HealthRecordPage'
import HospitalSearchPage from './pages/HospitalSearchPage'
import HospitalDetailPage from './pages/HospitalDetailPage'
import ReservationPage from './pages/ReservationPage'
import ReservationHistoryPage from './pages/ReservationHistoryPage'
import NotificationsPage from './pages/NotificationsPage'
import AdminReservationPage from './pages/admin/AdminReservationPage'
import AdminHospitalPage from './pages/admin/AdminHospitalPage'
import AdminUserPage from './pages/admin/AdminUserPage'
import { authApi } from './api/auth'
import { useAuthStore } from './stores/authStore'

export default function App() {
  // refreshTokenが永続化されていればページ再読み込み時にaccessTokenを復元する。
  // 復元対象が無ければ最初からisBootstrapping=falseにして無駄なスピナー表示を避ける。
  const [isBootstrapping, setIsBootstrapping] = useState(() => !!useAuthStore.getState().refreshToken)
  const hasBootstrapped = useRef(false)

  useEffect(() => {
    // React 18 StrictModeはeffectを2回実行する。refreshTokenはローテーション式で
    // 使用済みトークンが無効化されるため、2回目の呼び出しは失敗してしまう。
    if (hasBootstrapped.current) return
    hasBootstrapped.current = true

    const { refreshToken, setTokens, clearAuth } = useAuthStore.getState()
    if (!refreshToken) {
      setIsBootstrapping(false)
      return
    }

    authApi
      .refresh(refreshToken)
      .then((res) => {
        if (res.success && res.data) {
          setTokens(res.data.accessToken, res.data.refreshToken)
        } else {
          clearAuth()
        }
      })
      .catch(() => clearAuth())
      .finally(() => setIsBootstrapping(false))
  }, [])

  if (isBootstrapping) {
    return (
      <div className="h-dvh flex items-center justify-center bg-surface">
        <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
      </div>
    )
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* 認証不要 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* 要認証 */}
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/pets" element={<MyPetPage />} />
            <Route path="/hospitals" element={<HospitalSearchPage />} />
            <Route path="/reservations" element={<ReservationHistoryPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
          </Route>

          <Route element={<DetailLayout />}>
            <Route path="/pets/new" element={<PetRegisterPage />} />
            <Route path="/pets/:petId" element={<PetDetailPage />} />
            <Route path="/pets/:petId/health" element={<HealthRecordPage />} />
            <Route path="/hospitals/:hospitalId" element={<HospitalDetailPage />} />
            <Route path="/hospitals/:hospitalId/reserve" element={<ReservationPage />} />
          </Route>
        </Route>

        {/* 病院管理者 / システム管理者 */}
        <Route element={<ProtectedRoute allowedRoles={['ROLE_HOSPITAL_ADMIN', 'ROLE_SYSTEM_ADMIN']} />}>
          <Route element={<DetailLayout />}>
            <Route path="/admin/reservations" element={<AdminReservationPage />} />
            <Route path="/admin/hospitals" element={<AdminHospitalPage />} />
          </Route>
        </Route>

        {/* システム管理者のみ */}
        <Route element={<ProtectedRoute allowedRoles={['ROLE_SYSTEM_ADMIN']} />}>
          <Route element={<DetailLayout />}>
            <Route path="/admin/users" element={<AdminUserPage />} />
          </Route>
        </Route>

        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
