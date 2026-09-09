import { NavLink, useNavigate } from 'react-router-dom'

interface TopAppBarProps {
  variant?: 'main' | 'detail'
  title?: string
}

const mainNavItems = [
  { to: '/dashboard', label: 'ホーム' },
  { to: '/pets', label: 'マイペット' },
  { to: '/hospitals', label: '病院を探す' },
  { to: '/reservations', label: '予約履歴' },
  { to: '/notifications', label: '通知' },
]

export default function TopAppBar({ variant = 'main', title }: TopAppBarProps) {
  const navigate = useNavigate()

  if (variant === 'detail') {
    return (
      <header className="bg-surface-container-lowest border-b border-neutral-gray-100 z-40 pt-safe shrink-0">
        <div className="flex items-center px-container-margin h-[64px]">
          <button
            onClick={() => navigate(-1)}
            className="p-2 -ml-2 text-on-surface-variant hover:bg-surface-container-low rounded-full transition-colors active:scale-[0.98]"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md ml-4 text-neutral-gray-900">
            {title}
          </h1>
        </div>
      </header>
    )
  }

  return (
    <header className="w-full z-50 bg-surface border-b border-neutral-gray-100 pt-safe shrink-0">
      <div className="flex justify-between items-center px-container-margin h-[64px] max-w-[1200px] mx-auto">
        <div className="flex items-center gap-sm">
          <span className="material-symbols-outlined text-primary icon-fill text-[28px]">pets</span>
          <span className="font-headline-lg-mobile text-headline-lg-mobile font-bold text-primary">
            Daipetto
          </span>
        </div>

        <nav className="hidden md:flex items-center gap-1">
          {mainNavItems.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `px-4 py-2 rounded-lg font-button-text text-button-text transition-colors ${
                  isActive
                    ? 'text-primary bg-primary-container/10'
                    : 'text-on-surface-variant hover:bg-surface-container-low'
                }`
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <NavLink
            to="/notifications"
            className={({ isActive }) =>
              `p-2 rounded-full transition-colors ${
                isActive ? 'text-primary' : 'text-primary hover:bg-surface-container-low'
              }`
            }
          >
            {({ isActive }) => (
              <span className={`material-symbols-outlined ${isActive ? 'icon-fill' : ''}`}>
                notifications
              </span>
            )}
          </NavLink>
          <NavLink
            to="/profile"
            className="w-9 h-9 rounded-full overflow-hidden border border-outline-variant flex items-center justify-center bg-secondary-container"
          >
            <span className="material-symbols-outlined text-[20px] text-secondary">person</span>
          </NavLink>
        </div>
      </div>
    </header>
  )
}
