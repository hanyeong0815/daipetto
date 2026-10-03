package koh.portfolio.springapi.application.notification.usecase;

import koh.portfolio.springapi.domain.notification.model.NotificationType;

// 予約の承認・却下・診療完了で通知を生成する（08_State_Design §6-6）。
// 公開APIは持たず、Reservation の状態遷移サービスから呼ばれる内部契約
public interface NotifyReservationEventUseCase {
    void execute(Long userId, Long reservationId, NotificationType notificationType);
}
