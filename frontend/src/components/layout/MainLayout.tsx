import { Outlet } from 'react-router-dom'
import TopAppBar from './TopAppBar'
import BottomNav from './BottomNav'

export default function MainLayout() {
  return (
    <div className="bg-neutral-gray-50 min-h-screen">
      <TopAppBar variant="main" />
      <main className="pt-[64px] pb-[96px] md:pb-8 max-w-[1200px] mx-auto w-full">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  )
}
