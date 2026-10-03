package koh.portfolio.springapi.infrastructure.persistence.notification;

import jakarta.persistence.*;
import koh.portfolio.springapi.domain.notification.model.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long reservationId;
    private Long vaccinationId;
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
    private String title;
    private String message;
    @Column(name = "is_read")
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
