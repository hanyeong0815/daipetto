import { useEffect, useState } from 'react'
import { notificationApi } from '../api/notification'
import { extractErrorMessage } from '../api/client'
import type { AppNotification, NotificationType } from '../types'

const typeConfig: Record<NotificationType, { icon: string; color: string }> = {
  RESERVATION_APPROVED: { icon: 'event_available', color: 'text-primary bg-primary/10' },
  RESERVATION_REJECTED: { icon: 'event_busy', color: 'text-error-red bg-error-red/10' },
  TREATMENT_COMPLETED: { icon: 'medical_services', color: 'text-success-blue bg-success-blue/10' },
  VACCINATION: { icon: 'vaccines', color: 'text-warning-yellow bg-warning-yellow/10' },
}

// createdAt はオフセットなしのJST日時（docs/07 §2-7）。端末のタイムゾーンで解釈し直さずそのまま表示する
const formatDateTime = (dateTime: string) => dateTime.slice(0, 16).replace('T', ' ')

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<AppNotification[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    notificationApi
      .getList()
      .then((res) => setNotifications(res.data ?? []))
      .catch((err) => setError(extractErrorMessage(err, '通知の取得に失敗しました。')))
      .finally(() => setIsLoading(false))
  }, [])

  const unread = notifications.filter((n) => !n.isRead)

  const markRead = async (ids: number[]) => {
    setError(null)
    try {
      // ponytail: 一括既読APIが無いため1件ずつ。件数が増えたら一括APIを追加する
      await Promise.all(ids.map((id) => notificationApi.markAsRead(id)))
      setNotifications((prev) => prev.map((n) => (ids.includes(n.notificationId) ? { ...n, isRead: true } : n)))
    } catch (err) {
      setError(extractErrorMessage(err, '既読への変更に失敗しました。'))
    }
  }

  return (
    <div className="px-container-margin pt-lg pb-xl flex flex-col gap-lg">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="font-headline-lg-mobile text-headline-lg-mobile text-neutral-gray-900">通知</h1>
          {unread.length > 0 && (
            <p className="font-label-md text-label-md text-neutral-gray-600 mt-0.5">未読の通知 {unread.length}件</p>
          )}
        </div>
        {unread.length > 0 && (
          <button onClick={() => markRead(unread.map((n) => n.notificationId))} className="font-label-md text-label-md text-primary hover:underline">
            すべて既読にする
          </button>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-2 bg-error-red/10 border border-error-red/20 text-error-red px-sm py-3 rounded-lg font-body-md text-body-md">
          <span className="material-symbols-outlined text-[18px] flex-shrink-0">error</span>
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="flex items-center justify-center py-xl">
          <span className="material-symbols-outlined animate-spin text-primary text-[32px]">progress_activity</span>
        </div>
      ) : notifications.length === 0 ? (
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
              <button key={n.notificationId} onClick={() => !n.isRead && markRead([n.notificationId])}
                className={`w-full text-left rounded-xl border px-container-margin py-md flex items-start gap-3 transition-all ${n.isRead ? 'bg-surface-container-lowest border-neutral-gray-100' : 'bg-primary/5 border-primary/20'}`}>
                <div className={`w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5 ${config.color}`}>
                  <span className="material-symbols-outlined icon-fill text-[18px]">{config.icon}</span>
                </div>
                <div className="flex-grow min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <p className={`font-body-lg text-body-lg leading-tight ${!n.isRead ? 'text-neutral-gray-900 font-semibold' : 'text-neutral-gray-900'}`}>{n.title}</p>
                    <div className="flex items-center gap-1.5 flex-shrink-0">
                      {!n.isRead && <span className="w-2 h-2 rounded-full bg-pet-green-vibrant" />}
                      <span className="font-label-md text-label-md text-neutral-gray-600">{formatDateTime(n.createdAt)}</span>
                    </div>
                  </div>
                  <p className="font-body-md text-body-md text-neutral-gray-600 mt-1 leading-relaxed">{n.message}</p>
                </div>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
