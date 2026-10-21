package koh.portfolio.springapi.domain.notification.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Notification {
    private Long id;
    private Long userId;
    private Long reservationId;
    private Long vaccinationId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static Notification forReservation(Long userId, Long reservationId, NotificationType notificationType) {
        return new Notification(null, userId, reservationId, null, notificationType,
                notificationType.getTitle(), notificationType.getMessage(), false, LocalDateTime.now(), null);
    }

    // 本文にペット名・ワクチン名・予定日を含めるため、種別の既定文面ではなく呼び出し側の文面を使う
    public static Notification forVaccination(Long userId, Long vaccinationId, String message) {
        return new Notification(null, userId, null, vaccinationId, NotificationType.VACCINATION,
                NotificationType.VACCINATION.getTitle(), message, false, LocalDateTime.now(), null);
    }
}
