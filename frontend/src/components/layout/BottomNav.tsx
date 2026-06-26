import { NavLink } from 'react-router-dom'

const navItems = [
  { to: '/dashboard', icon: 'dashboard', label: 'ホーム' },
  { to: '/pets', icon: 'pets', label: 'マイペット' },
  { to: '/hospitals', icon: 'local_hospital', label: '病院' },
  { to: '/reservations', icon: 'calendar_today', label: '予約' },
  { to: '/notifications', icon: 'notifications', label: '通知' },
]

export default function BottomNav() {
  return (
    <nav className="md:hidden fixed bottom-0 left-0 w-full h-[80px] flex justify-around items-center px-xs pb-safe bg-surface-container-lowest shadow-[0px_-4px_20px_0px_rgba(0,0,0,0.05)] border-t border-neutral-gray-100 z-50">
      {navItems.map(({ to, icon, label }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            `flex flex-col items-center justify-center px-3 py-1 rounded-xl transition-all duration-150 active:scale-[0.95] ${
              isActive
                ? 'text-primary bg-surface-container-high'
                : 'text-secondary hover:opacity-80'
            }`
          }
        >
          {({ isActive }) => (
            <>
              <span
                className={`material-symbols-outlined mb-1 ${isActive ? 'icon-fill' : ''}`}
              >
                {icon}
              </span>
              <span className="font-label-md text-[10px] tracking-tighter whitespace-nowrap">
                {label}
              </span>
            </>
          )}
        </NavLink>
      ))}
    </nav>
  )
}
