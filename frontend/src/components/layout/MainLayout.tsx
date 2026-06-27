import { Outlet } from 'react-router-dom'
import TopAppBar from './TopAppBar'
import BottomNav from './BottomNav'

export default function MainLayout() {
  return (
    <div className="h-dvh flex flex-col overflow-hidden bg-neutral-gray-50">
      <TopAppBar variant="main" />
      <main className="flex-1 overflow-y-auto max-w-[1200px] mx-auto w-full">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  )
}
