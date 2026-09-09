import { Outlet, useLocation } from 'react-router-dom'
import TopAppBar from './TopAppBar'

const titleMap: Record<string, string> = {
  '/pets/new': 'ペット登録',
  '/admin/reservations': '予約管理',
  '/admin/hospitals': '病院情報管理',
  '/admin/users': 'ユーザー管理',
}

function getTitle(pathname: string): string {
  if (titleMap[pathname]) return titleMap[pathname]
  if (pathname.includes('/health')) return '健康記録'
  if (pathname.includes('/reserve')) return '予約する'
  if (pathname.includes('/hospitals/')) return '病院詳細'
  if (pathname.includes('/pets/')) return 'ペット詳細'
  return ''
}

export default function DetailLayout() {
  const { pathname } = useLocation()

  return (
    <div className="h-dvh flex flex-col overflow-hidden bg-neutral-gray-50">
      <TopAppBar variant="detail" title={getTitle(pathname)} />
      <main className="flex-1 overflow-y-auto max-w-[800px] mx-auto w-full">
        <Outlet />
      </main>
    </div>
  )
}
