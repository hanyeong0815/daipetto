import { useState } from 'react'

type NotifType = 'reservation' | 'health' | 'system'

interface Notification {
  id: number
  type: NotifType
  title: string
  body: string
  time: string
  read: boolean
}

const typeConfig: Record<NotifType, { icon: string; color: string }> = {
  reservation: { icon: 'calendar_today', color: 'text-primary bg-primary/10' },
  health: { icon: 'favorite', color: 'text-error-red bg-error-red/10' },
  system: { icon: 'notifications', color: 'text-neutral-gray-600 bg-neutral-gray-100' },
}

const mockNotifications: Notification[] = [
  { id: 1, type: 'reservation', title: '予約リマインダー', body: '明日の午後2:00に幸せ動物病院の予約があります。ボリと一緒に準備しましょう！', time: '1時間前', read: false },
  { id: 2, type: 'health', title: '予防接種のお知らせ', body: 'ボリのフィラリア予防薬の投与日まであと3日です。', time: '3時間前', read: false },
  { id: 3, type: 'reservation', title: '予約が確定されました', body: '10月18日 愛情動物クリニックの予約が確定されました。', time: '昨日', read: true },
  { id: 4, type: 'system', title: 'サービスのお知らせ', body: '10月28日（月）メンテナンスのため、午前2:00〜4:00はサービスをご利用いただけません。', time: '2日前', read: true },
  { id: 5, type: 'health', title: '健康記録が保存されました', body: 'ボリの体重記録が保存されました。（28.5 kg）', time: '3日前', read: true },
]

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState(mockNotifications)

  const unreadCount = notifications.filter((n) => !n.read).length

  const markAllRead = () => setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  const markRead = (id: number) => setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)))

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">通知</h1>
          {unreadCount > 0 && (
            <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">未読の通知 {unreadCount}件</p>
          )}
        </div>
        {unreadCount > 0 && (
          <button onClick={markAllRead} className="font-label-md text-label-md text-primary hover:underline">
            すべて既読にする
          </button>
        )}
      </div>

      {notifications.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-xl gap-md text-center mt-xl">
          <div className="w-20 h-20 rounded-full bg-surface-container-low flex items-center justify-center">
            <span className="material-symbols-outlined text-neutral-gray-600 text-[40px]">notifications_off</span>
          </div>
          <div>
            <p className="font-headline-md text-headline-md text-neutral-gray-900 mb-xs">通知はありません</p>
            <p className="font-body-md text-body-md text-neutral-gray-600">新しい通知が届いたらここに表示されます。</p>
          </div>
        </div>
      ) : (
        <div className="flex flex-col gap-xs">
          {notifications.map((n) => {
            const config = typeConfig[n.type]
            return (
              <button key={n.id} onClick={() => markRead(n.id)}
                className={`w-full text-left rounded-xl border px-container-margin py-md flex items-start gap-3 transition-all ${n.read ? 'bg-surface-container-lowest border-neutral-gray-100' : 'bg-primary/5 border-primary/20'}`}>
                <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 ${config.color}`}>
                  <span className="material-symbols-outlined icon-fill text-[18px]">{config.icon}</span>
                </div>
                <div className="flex-grow min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <p className={`font-body-lg text-body-lg leading-tight ${!n.read ? 'text-neutral-gray-900 font-semibold' : 'text-neutral-gray-900'}`}>{n.title}</p>
                    <div className="flex items-center gap-1.5 flex-shrink-0">
                      {!n.read && <span className="w-2 h-2 rounded-full bg-pet-green-vibrant" />}
                      <span className="font-label-md text-label-md text-neutral-gray-600">{n.time}</span>
                    </div>
                  </div>
                  <p className="font-body-md text-body-md text-neutral-gray-600 mt-1 leading-relaxed">{n.body}</p>
                </div>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
