export type NotificationType = 'RESERVATION_APPROVED' | 'RESERVATION_REJECTED' | 'TREATMENT_COMPLETED' | 'VACCINATION'

export interface AppNotification {
  notificationId: number
  type: NotificationType
  title: string
  message: string
  isRead: boolean
  createdAt: string
}
