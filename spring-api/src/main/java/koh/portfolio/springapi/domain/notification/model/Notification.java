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
}
