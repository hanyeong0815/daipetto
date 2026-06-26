import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './components/layout/MainLayout'
import DetailLayout from './components/layout/DetailLayout'
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

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 인증 없이 접근 가능 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* 메인 레이아웃 (바텀 네비게이션 포함) */}
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/pets" element={<MyPetPage />} />
          <Route path="/hospitals" element={<HospitalSearchPage />} />
          <Route path="/reservations" element={<ReservationHistoryPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
        </Route>

        {/* 디테일 레이아웃 (뒤로가기 버튼 포함) */}
        <Route element={<DetailLayout />}>
          <Route path="/pets/new" element={<PetRegisterPage />} />
          <Route path="/pets/:petId" element={<PetDetailPage />} />
          <Route path="/pets/:petId/health" element={<HealthRecordPage />} />
          <Route path="/hospitals/:hospitalId" element={<HospitalDetailPage />} />
          <Route path="/hospitals/:hospitalId/reserve" element={<ReservationPage />} />
        </Route>

        {/* 관리자 페이지 */}
        <Route element={<DetailLayout />}>
          <Route path="/admin/reservations" element={<AdminReservationPage />} />
          <Route path="/admin/hospitals" element={<AdminHospitalPage />} />
          <Route path="/admin/users" element={<AdminUserPage />} />
        </Route>

        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
